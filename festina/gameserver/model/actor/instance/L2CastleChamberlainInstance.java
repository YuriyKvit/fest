package com.festina.gameserver.model.actor.instance;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javolution.lang.TextBuilder;

import com.festina.Config;
import com.festina.gameserver.ItemTable;
import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.instancemanager.CastleManorManager;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.serverpackets.ExShowCropInfo; 
import com.festina.gameserver.serverpackets.ExShowCropSetting; 
import com.festina.gameserver.serverpackets.ExShowManorDefaultInfo; 
import com.festina.gameserver.serverpackets.ExShowSeedInfo; 
import com.festina.gameserver.serverpackets.ExShowSeedSetting;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.MyTargetSelected;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.serverpackets.ValidateLocation;
import com.festina.gameserver.serverpackets.WareHouseDepositList;
import com.festina.gameserver.serverpackets.WareHouseWithdrawalList;
import com.festina.gameserver.templates.L2NpcTemplate;
import com.festina.gameserver.util.Util;

/**
 * Castle Chamberlains implementation
 * used for:
 * - tax rate control
 * - regional manor system control
 * - castle treasure control
 * - ...
 */
public class L2CastleChamberlainInstance extends L2MerchantInstance
{
    //private static Logger _log = Logger.getLogger(L2CastleChamberlainInstance.class.getName());

	protected static int Cond_All_False = 0;
	protected static int Cond_Busy_Because_Of_Siege = 1;
	protected static int Cond_Owner = 2;

    public L2CastleChamberlainInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }
    public void onAction(L2PcInstance player)
    {
        player.setLastFolkNPC(this);
        
        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget())
        {
            // Set the target of the L2PcInstance player
            player.setTarget(this);
            
            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            // The player.getLevel() - getLevel() permit to display the correct color in the select window
            MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
            player.sendPacket(my);
            
            // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        }
        else
        {
            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            // The player.getLevel() - getLevel() permit to display the correct color
            MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
            player.sendPacket(my);
            
            // Calculate the distance between the L2PcInstance and the L2NpcInstance
            if (!isInsideRadius(player, INTERACTION_DISTANCE, false, false))
            {
                // player.setCurrentState(L2Character.STATE_INTERACT);
                // player.setInteractTarget(this);
                // player.moveTo(this.getX(), this.getY(), this.getZ(), INTERACTION_DISTANCE);

                // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);                    
                
                // Send a Server->Client packet ActionFailed (target is out of interaction range) to the L2PcInstance player
                player.sendPacket(new ActionFailed());
            } 
            else 
            {
                showMessageWindow(player);

                // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
                player.sendPacket(new ActionFailed());                  
                // player.setCurrentState(L2Character.STATE_IDLE);
            }
        }
    }
    public void onBypassFeedback(L2PcInstance player, String command)
    {
		player.sendPacket( new ActionFailed() );

		int condition = validateCondition(player);
		if (condition <= Cond_All_False)
            return;

		if (condition == Cond_Busy_Because_Of_Siege)
            return;
		else if (condition == Cond_Owner)
		{
	        StringTokenizer st = new StringTokenizer(command, " ");
            String actualCommand = st.nextToken(); // Get actual command

	        String val = "";
	        if (st.countTokens() >= 1) {val = st.nextToken();}
	 
			if (actualCommand.equalsIgnoreCase("banish_foreigner"))
            {
                getCastle().banishForeigners();                                                      // Move non-clan members off castle area                return;
            }
			else if (actualCommand.equalsIgnoreCase("list_siege_clans"))
            {
                getCastle().getSiege().listRegisterClan(player);                                    // List current register clan
                return;
            }
			else if (actualCommand.equalsIgnoreCase("manage_siege_defender"))
			{
                getCastle().getSiege().listRegisterClan(player);
                return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_vault")) {
                String filename = "data/html/chamberlain/chamberlain-vault.htm";
                int amount = 0;
                if (val.equalsIgnoreCase("deposit")) {
                    try {
                        amount = Integer.parseInt(st.nextToken());
                    } catch(NoSuchElementException e) {}
                    if (amount > 0 && getCastle().getTreasury() + amount < Integer.MAX_VALUE) {
                        if (player.reduceAdena("Castle", amount, this, true)) {
                            getCastle().addToTreasuryNoTax(amount);
                        } else {
                            sendPacket(new SystemMessage(SystemMessage.YOU_NOT_ENOUGH_ADENA));
                        }
                    }
                } else if (val.equalsIgnoreCase("withdraw")) {
                    try {
                        amount = Integer.parseInt(st.nextToken());
                    } catch(NoSuchElementException e) {}
                    if (amount > 0) {
                        if (getCastle().getTreasury() < amount) {
                            filename = "data/html/chamberlain/chamberlain-vault-no.htm";
                        } else {
                            player.addAdena("Castle", amount, this, true);
                            getCastle().addToTreasuryNoTax((-1)*amount);
                        }
                    }
                }
                
                NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                html.setFile(filename);
                html.replace("%objectId%", String.valueOf(getObjectId()));
                html.replace("%npcname%", getName());
                html.replace("%tax_income%", Util.formatAdena(getCastle().getTreasury()));
                html.replace("%withdraw_amount%", Util.formatAdena(amount));
                player.sendPacket(html);
                    
                return;
            }
			else if(actualCommand.equalsIgnoreCase("manor")) {
                String filename = "";
                if (CastleManorManager.getInstance().isDisabled()) {
                    filename = "data/html/npcdefault.htm";
                } else {
                    int cmd = Integer.parseInt(val);
                    switch(cmd) {
                        case 0:
                            filename = "data/html/chamberlain/manor/manor.htm";
                            break;
                        // TODO: correct in html's to 1
                        case 4:
                            filename = "data/html/chamberlain/manor/manor_help00"+st.nextToken()+".htm";
                            break;
                        default:
                            filename = "data/html/chamberlain/chamberlain-no.htm";
                            break;
                    }
                }
                
                if (!filename.isEmpty()) {
                    NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                    html.setFile(filename);
                    html.replace("%objectId%", String.valueOf(getObjectId()));
                    html.replace("%npcname%", getName());
                    player.sendPacket(html);
                }
                return;
            } else if (command.startsWith("manor_menu_select")) { // input string format: 
                                                                    // manor_menu_select?ask=X&state=Y&time=X
                if (CastleManorManager.getInstance().isUnderMaintenance()) {
                    player.sendPacket(new ActionFailed());
                    //player.sendPacket(new SystemMessage(SystemMessage.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
                    player.sendMessage("The manor system is currently under maintenance");
                    return;
                }
                
                String params = command.substring(command.indexOf("?")+1);
                StringTokenizer str = new StringTokenizer(params, "&");
                int ask   = Integer.parseInt(str.nextToken().split("=")[1]);
                int state = Integer.parseInt(str.nextToken().split("=")[1]);
                int time  = Integer.parseInt(str.nextToken().split("=")[1]);
                
                int castleId;
                if (state == -1) // info for current manor
                    castleId = getCastle().getCastleId();
                else             // info for requested manor
                    castleId = state;
                
                switch (ask) { // Main action
                case 3: // Current seeds (Manor info)
                    if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
                        player.sendPacket(new ExShowSeedInfo(castleId, null));
                    else
                        player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
                    break;
                case 4: // Current crops (Manor info)
                    if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
                        player.sendPacket(new ExShowCropInfo(castleId, null));
                    else
                        player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
                    break;
                case 5: // Basic info (Manor info)
                    player.sendPacket(new ExShowManorDefaultInfo());
                    break;
                case 7: // Edit seed setup
                    if (getCastle().isNextPeriodApproved()) {
                        //player.sendPacket(new SystemMessage(SystemMessage.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
                        player.sendMessage("A manor cannot be set up between 6 and 8 PM");
                    } else {
                        player.sendPacket(new ExShowSeedSetting(getCastle().getCastleId()));
                    }
                    break;
                case 8: // Edit crop setup
                    if (getCastle().isNextPeriodApproved()) {
                        //player.sendPacket(new SystemMessage(SystemMessage.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
                        player.sendMessage("A manor cannot be set up between 6 and 8 PM");
                    } else {
                        player.sendPacket(new ExShowCropSetting(getCastle().getCastleId()));
                    }
                    break;
                }
            }
	        else if(actualCommand.equalsIgnoreCase("operate_door")) // door control
	        {
	            if (val != "")
	            {
		            boolean open = (Integer.parseInt(val) == 1);
		            while (st.hasMoreTokens())
		           	{
		           	    getCastle().openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
		           	}
	            }

	            NpcHtmlMessage html = new NpcHtmlMessage(1);
	    		html.setFile("data/html/chamberlain/" + getTemplate().npcId + "-d.htm");
	    		html.replace("%objectId%", String.valueOf(getObjectId()));
	    		html.replace("%npcname%", getName());
	    		player.sendPacket(html);
                return;
	        }
	        else if (actualCommand.equalsIgnoreCase("items"))
			{
	        	int npcId = getNpcId();
	        	showBuyWindow(player, npcId);
			}
			else if (actualCommand.equalsIgnoreCase("products"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-products.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcId%", String.valueOf(getNpcId()));
				player.sendPacket(html);
				return;
			}
	        else if(actualCommand.equalsIgnoreCase("tax_set")) // tax rates control
	        {
	            if (val != "")
	                getCastle().setTaxPercent(player, Integer.parseInt(val));

                TextBuilder msg = new TextBuilder("<html><body>");
	        	msg.append(getName() + ":<br>");
	        	msg.append("Current tax rate: " + getCastle().getTaxPercent() + "%<br>");
	        	msg.append("<table>");
	        	msg.append("<tr>");
	        	msg.append("<td>Change tax rate to:</td>");
	        	msg.append("<td><edit var=\"value\" width=40><br>");
	        	msg.append("<button value=\"Adjust\" action=\"bypass -h npc_%objectId%_tax_set $value\" width=80 height=15></td>");
	        	msg.append("</tr>");
	        	msg.append("</table>");
	        	msg.append("</center>");
	        	msg.append("</body></html>");

		        this.sendHtmlMessage(player, msg.toString());
                return;
	        }
		}

        super.onBypassFeedback(player, command);
    }
	private void sendHtmlMessage(L2PcInstance player, String htmlMessage)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(1);
        html.setHtml(htmlMessage);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
        player.sendPacket(html);
    }
    
    private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket( new ActionFailed() );
		String filename = "data/html/chamberlain/chamberlain-no.htm";
		
		int condition = validateCondition(player);
		if (condition > Cond_All_False)
		{
	        if (condition == Cond_Busy_Because_Of_Siege)
	            filename = "data/html/chamberlain/chamberlain-busy.htm";					// Busy because of siege
	        else if (condition == Cond_Owner)												// Clan owns castle
	            filename = "data/html/chamberlain/chamberlain.htm";							// Owner message window
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
    /*
	private void showVaultWindowDeposit(L2PcInstance player)
	{
		player.sendPacket(new ActionFailed());
		player.setActiveWarehouse(player.getClan().getWarehouse());
        player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.Clan)); //Or Castle ??
	}

	private void showVaultWindowWithdraw(L2PcInstance player)
	{
		player.sendPacket(new ActionFailed());
		player.setActiveWarehouse(player.getClan().getWarehouse());
        player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.Clan)); //Or Castle ??
	}
	*/
	protected int validateCondition(L2PcInstance player)
	{
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
		    if (player.getClan() != null)
		    {
		        if (getCastle().getSiege().getIsInProgress())
		            return Cond_Busy_Because_Of_Siege;										// Busy because of siege
		        else if (getCastle().getOwnerId() == player.getClanId()						// Clan owns castle
		                && player.isClanLeader())			                                // Leader of clan
		            return Cond_Owner;	// Owner
		    }
		}
		
		return Cond_All_False;
	}
}
