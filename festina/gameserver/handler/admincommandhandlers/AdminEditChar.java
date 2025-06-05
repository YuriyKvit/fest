/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.festina.gameserver.handler.admincommandhandlers;

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.lang.TextBuilder;

import com.festina.Config;
import com.festina.gameserver.ClanTable;
import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.datatables.NobleTable;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.model.GMAudit;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;
import com.festina.gameserver.model.base.ClassId;
import com.festina.gameserver.serverpackets.CharInfo;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.serverpackets.UserInfo;
import com.festina.gameserver.util.Util;

/**
 * This class handles following admin commands:
 * - edit_character
 * - current_player
 * - character_list
 * - show_characters
 * - find_character
 * - save_modifications
 * 
 * @version $Revision: 1.3.2.1.2.10 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminEditChar implements IAdminCommandHandler 
{
	private static Logger _log = Logger.getLogger(AdminEditChar.class.getName());
	
	private static String[] _adminCommands = 
    {
			"admin_edit_character",
			"admin_current_player",
			"admin_nokarma", // this is to remove karma from selected char...
			"admin_setkarma", // sets karma of target char to any amount. //setkarma <karma>
			"admin_character_list",
			"admin_show_characters",
			"admin_find_character",
			"admin_save_modifications",
			"admin_rec",
            "admin_settitle",
            "admin_setname",
            "admin_setsex",
            "admin_setcolor",
            "admin_fullfood",
            "admin_setclass"
			};
    
	private static final int REQUIRED_LEVEL = Config.GM_CHAR_EDIT;
	private static final int REQUIRED_LEVEL2 = Config.GM_CHAR_EDIT_OTHER;
	private static final int REQUIRED_LEVEL_VIEW = Config.GM_CHAR_VIEW;

	public boolean useAdminCommand(String command, L2PcInstance activeChar) 
    {
        if (!((checkLevel(activeChar.getAccessLevel()) || checkLevel2(activeChar.getAccessLevel())) && activeChar.isGM())) 
            return false;
        
		if (command.equals("admin_current_player"))
		{
			showCharacterList(activeChar, null);
		}
		else if (command.startsWith("admin_character_list"))
		{
			try
			{
				String val = command.substring(21); 
				L2PcInstance target = L2World.getInstance().getPlayer(val);
                
				if (target != null)
				{
					showCharacterList(activeChar, target);
					GMAudit.auditGMAction(activeChar.getName(), command, target.getName(), "");
				}
				else
				{
					activeChar.sendMessage("Player not found.");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				//Case of empty character name
			}
		}
		else if (command.startsWith("admin_show_characters"))
		{
			try
			{   
				String val = command.substring(22);
				int page = Integer.parseInt(val);
				listCharacters(activeChar, page);
		        GMAudit.auditGMAction(activeChar.getName(), command, "no-target", "");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				//Case of empty page
			}
		}
		else if (command.startsWith("admin_find_character"))
		{
			try
			{
				String val = command.substring(21); 
				findCharacter(activeChar, val);
		        GMAudit.auditGMAction(activeChar.getName(), command, val, "");
			}
			catch (StringIndexOutOfBoundsException e)
			{	//Case of empty character name
				SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
				sm.addString("You didnt enter a character name to find.");
				activeChar.sendPacket(sm);
				
				listCharacters(activeChar, 0);
			}			
		}
		else if (command.equals("admin_edit_character"))
		{
	        GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
			editCharacter(activeChar);
		}
		// Karma control commands
		else if (command.equals("admin_nokarma"))
		{
	        GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
			setTargetKarma(activeChar, 0);
		}
		else if (command.startsWith("admin_setkarma"))
		{
			try
			{   
				String val = command.substring(15);
				int karma = Integer.parseInt(val);
				if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel()>=REQUIRED_LEVEL2)
			    GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
				setTargetKarma(activeChar, karma);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if ( Config.DEVELOPER ) 
					_log.info("Set karma error: " + e);
				SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
				sm.addString("Please specify new karma value.");
				activeChar.sendPacket(sm);
			}
		}
		else if (command.startsWith("admin_save_modifications"))
		{
			try
			{
				String val = command.substring(24); 
				if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel()>=REQUIRED_LEVEL2)
			        GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
				adminModifyCharacter(activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{	//Case of empty character name
				SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
				sm.addString("Error while modifying character.");
				activeChar.sendPacket(sm);
				listCharacters(activeChar, 0);
			}			
		}
		else if (command.equals("admin_rec"))
		{
			if (activeChar != activeChar.getTarget() && activeChar.getAccessLevel()<REQUIRED_LEVEL2)
			    return false;
            
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
            
			if (target instanceof L2PcInstance) 
				player = (L2PcInstance)target;
			else
				return false;
            
			player.setRecomHave(player.getRecomHave() + 1);
			SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
			sm.addString("You have been recommended by a GM");
			player.sendPacket(sm);
			player.broadcastUserInfo();
		}
		else if (command.startsWith("admin_rec"))
		{
			try
			{
				String val = command.substring(10);
				int recVal = Integer.parseInt(val);
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				if (activeChar != target && activeChar.getAccessLevel()<REQUIRED_LEVEL2)
				    return false;
				if (target instanceof L2PcInstance) {
					player = (L2PcInstance)target;
				} else {
					return false;
				}
	            player.setRecomHave(player.getRecomHave() + recVal);
				SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
				sm.addString("You have been recommended by a GM");
				player.sendPacket(sm);
				player.broadcastUserInfo();
			} catch (NumberFormatException nfe)
			{
                activeChar.sendMessage("You must specify the number of recommendations to add.");
			}
		}
        else if (command.startsWith("admin_settitle"))
        {
            try
            {
                String val = command.substring(15); 
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
		if (activeChar != target && activeChar.getAccessLevel()<REQUIRED_LEVEL2)
		    return false;
                if (target instanceof L2PcInstance) {
                    player = (L2PcInstance)target;
                } else {
                    return false;
                }
                player.setTitle(val);
                SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
                sm.addString("Your title has been changed by a GM");
                player.sendPacket(sm);
                player.broadcastUserInfo();
            }
            catch (StringIndexOutOfBoundsException e)
            {   //Case of empty character title
                SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
                sm.addString("You need to specify the new title.");
                activeChar.sendPacket(sm);
            }           
        }
        else if (command.startsWith("admin_setname"))
        {
            try
            {
                String val = command.substring(14); 
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
		if (activeChar != target && activeChar.getAccessLevel()<REQUIRED_LEVEL2)
		    return false;
                if (target instanceof L2PcInstance) {
                    player = (L2PcInstance)target;
                } else {
                    return false;
                }
                player.setName(val);
                SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
                sm.addString("Your name has been changed by a GM");
                player.sendPacket(sm);
                player.broadcastUserInfo();
            }
            catch (StringIndexOutOfBoundsException e)
            {   //Case of empty character name
                SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
                sm.addString("You need to specify the new name.");
                activeChar.sendPacket(sm);
            }           
        }	
        else if (command.startsWith("admin_setsex"))
        {
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
		if (activeChar != target && activeChar.getAccessLevel()<REQUIRED_LEVEL2)
		    return false;
                if (target instanceof L2PcInstance) {
                    player = (L2PcInstance)target;
                } else {
                    return false;
                }
                player.changeSex();
                SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
                sm.addString("Your gender has been changed by a GM");
                player.sendPacket(sm);
                player.broadcastUserInfo();  
        }   
        else if (command.startsWith("admin_setcolor"))
        {
            try
            {
                String val          = command.substring(15); 
                L2Object target     = activeChar.getTarget();
                L2PcInstance player = null;
		if (activeChar != target && activeChar.getAccessLevel()<REQUIRED_LEVEL2)
		    return false;
                if (target instanceof L2PcInstance) {
                    player = (L2PcInstance)target;
                } else {
                    return false;
                }
                player.setNameColor(Integer.decode("0x"+val));
                player.sendMessage("Your name color has been changed by a GM");
                player.broadcastUserInfo();
            }
            catch (StringIndexOutOfBoundsException e)
            {   //Case of empty color
                activeChar.sendMessage("You need to specify the new color.");
            }
        }
		else if (command.startsWith("admin_setclass"))
		{
			try
			{
				String val = command.substring(15).trim();
				int classidval = Integer.parseInt(val);
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					return false;
				boolean valid = false;
				for (ClassId classid : ClassId.values())
					if (classidval == classid.getId())
						valid = true;
				if (valid && (player.getClassId().getId() != classidval))
				{
					player.setClassId(classidval);
					if (!player.isSubClassActive())
						player.setBaseClass(classidval);
					String newclass = player.getTemplate().className;
					player.store();
					player.sendMessage("A GM changed your class to " + newclass);
					player.broadcastUserInfo();
					activeChar.sendMessage(player.getName() + " is a " + newclass);
				}
				else
					activeChar.sendMessage("Usage: //setclass <valid_new_classid>");
			}
			catch (StringIndexOutOfBoundsException e)
			{
			    AdminHelpPage.showHelpPage(activeChar, "charclasses.htm");
			}
			catch (NumberFormatException e)
			{
			activeChar.sendMessage("Usage: //setclass <valid_new_classid>");
			}
		}
        else if (command.startsWith("admin_fullfood"))
        {
        	L2Object target = activeChar.getTarget();
        	
        	if (target instanceof L2PetInstance)
        	{
        		L2PetInstance targetPet = (L2PetInstance)target;
        		targetPet.setCurrentFed(targetPet.getMaxFed());
        	}
        	else {
        		activeChar.sendPacket(new SystemMessage(SystemMessage.INCORRECT_TARGET));
        	}
        }
		
		return true;
	}
	
	public String[] getAdminCommandList() {
		return _adminCommands;
	}
	
	private boolean checkLevel(int level) {
		return (level >= REQUIRED_LEVEL);
	}
	private boolean checkLevel2(int level) {
		return (level >= REQUIRED_LEVEL_VIEW);
	}
	
	private void listCharacters(L2PcInstance activeChar, int page)
	{	
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		int MaxCharactersPerPage = 20;
		int MaxPages = players.length / MaxCharactersPerPage;
		
        if (players.length > MaxCharactersPerPage * MaxPages)
			MaxPages++;
		
		//Check if number of users changed
		if (page>MaxPages)
		{
			page=MaxPages;
		}

		int CharactersStart = MaxCharactersPerPage*page;
		int CharactersEnd = players.length;		
		if (CharactersEnd - CharactersStart > MaxCharactersPerPage)
		    CharactersEnd = CharactersStart + MaxCharactersPerPage;
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);		

		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td width=270>You can find a character by writing his name and</td></tr>");
		replyMSG.append("<tr><td width=270>clicking Find bellow.<br></td></tr>");
		replyMSG.append("<tr><td width=270>Note: Names should be written case sensitive.</td></tr>");
		replyMSG.append("</table><br>");
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<edit var=\"character_name\" width=80></td><td><button value=\"Find\" action=\"bypass -h admin_find_character $character_name\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</td></tr></table></center><br><br>");
		
		for (int x=0; x<MaxPages; x++)
		{
			int pagenr = x + 1;
			replyMSG.append("<center><a action=\"bypass -h admin_show_characters " + x + "\">Page " + pagenr + "</a></center>");	
		}			
		replyMSG.append("<br>");

		//List Players in a Table
		replyMSG.append("<table width=270>");		
		replyMSG.append("<tr><td width=80>Name:</td><td width=110>Class:</td><td width=40>Level:</td></tr>");
		for (int i = CharactersStart; i < CharactersEnd; i++)
		{	//Add player info into new Table row
			replyMSG.append("<tr><td width=80>" + "<a action=\"bypass -h admin_character_list " + players[i].getName() + "\">" + players[i].getName() + "</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
		}
		replyMSG.append("</table>");
		replyMSG.append("</body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);			
	}
	
	private void showCharacterList(L2PcInstance activeChar, L2PcInstance player)
    {
        if(player == null)
        {
            L2Object target = activeChar.getTarget();

            if(target instanceof L2PcInstance)
            {
                player = (L2PcInstance) target;
            }
            else
                return;

            target = null;
        }
        else
        {
            activeChar.setTarget(player);
        }

        gatherCharacterInfo(activeChar, player, "charinfo.htm");
    }
    public static void gatherCharacterInfo(L2PcInstance activeChar, L2PcInstance player, String filename)
    {
        String account = activeChar.getAccountName().toString();
       
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        adminReply.setFile("data/html/admin/" + filename);
        adminReply.replace("%name%", player.getName());
        adminReply.replace("%level%", String.valueOf(player.getLevel()));
        try {
            adminReply.replace("%clan%", String.valueOf(ClanTable.getInstance().getClan(player.getClanId())));
        } catch(Exception e) {
            adminReply.replace("%clan%", "none");
        }
        adminReply.replace("%xp%", String.valueOf(player.getExp()));
        adminReply.replace("%sp%", String.valueOf(player.getSp()));
        adminReply.replace("%class%", player.getTemplate().className);
        adminReply.replace("%ordinal%", String.valueOf(player.getClassId().ordinal()));
        adminReply.replace("%classid%", String.valueOf(player.getClassId()));
        adminReply.replace("%x%", String.valueOf(player.getX()));
        adminReply.replace("%y%", String.valueOf(player.getY()));
        adminReply.replace("%z%", String.valueOf(player.getZ()));
        adminReply.replace("%currenthp%", String.valueOf((int) player.getCurrentHp()));
        adminReply.replace("%maxhp%", String.valueOf(player.getMaxHp()));
        adminReply.replace("%karma%", String.valueOf(player.getKarma()));
        adminReply.replace("%currentmp%", String.valueOf((int) player.getCurrentMp()));
        adminReply.replace("%maxmp%", String.valueOf(player.getMaxMp()));
        adminReply.replace("%pvpflag%", String.valueOf(player.getPvpFlag()));
        adminReply.replace("%currentcp%", String.valueOf((int) player.getCurrentCp()));
        adminReply.replace("%maxcp%", String.valueOf(player.getMaxCp()));
        adminReply.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
        adminReply.replace("%pkkills%", String.valueOf(player.getPkKills()));
        adminReply.replace("%currentload%", String.valueOf(player.getCurrentLoad()));
        adminReply.replace("%maxload%", String.valueOf(player.getMaxLoad()));
        adminReply.replace("%percent%", String.valueOf(Util.roundTo((float) player.getCurrentLoad() / (float) player.getMaxLoad() * 100, 2)));
        adminReply.replace("%patk%", String.valueOf(player.getPAtk(null)));
        adminReply.replace("%matk%", String.valueOf(player.getMAtk(null, null)));
        adminReply.replace("%pdef%", String.valueOf(player.getPDef(null)));
        adminReply.replace("%mdef%", String.valueOf(player.getMDef(null, null)));
        adminReply.replace("%accuracy%", String.valueOf(player.getAccuracy()));
        adminReply.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
        adminReply.replace("%critical%", String.valueOf(player.getCriticalHit(null, null)));
        adminReply.replace("%runspeed%", String.valueOf(player.getRunSpeed()));
        adminReply.replace("%patkspd%", String.valueOf(player.getPAtkSpd()));
        adminReply.replace("%matkspd%", String.valueOf(player.getMAtkSpd()));
        adminReply.replace("%access%", String.valueOf(player.getAccessLevel()));
        adminReply.replace("%account%", account);
        adminReply.replace("%baseclass%", String.valueOf(player.getBaseClass()));
        adminReply.replace("%noblesse%", String.valueOf(NobleTable.isNoble(player.getObjectId())));
        activeChar.sendPacket(adminReply);

        adminReply = null;
        account = null;
    }	
	private void setTargetKarma(L2PcInstance activeChar, int newKarma) {
		// function to change karma of selected char
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance) {
			player = (L2PcInstance)target;
		} else {
			return;
		}
		
		if ( newKarma >= 0 ) {
			// for display
			int oldKarma = player.getKarma();			
			
			// update karma
			player.setKarma(newKarma);
			
			StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.KARMA, newKarma);
			player.sendPacket(su);
			
			/*
			StatusUpdate statu = new StatusUpdate(player.getObjectId());
			statu.addAttribute(StatusUpdate.KARMA, newKarma);
			activeChar.sendPacket(statu);
			*/
			
		    CharInfo info1 = new CharInfo(player);
		    player.broadcastPacket(info1);
		    UserInfo info2 = new UserInfo(player);
		    player.sendPacket(info2);
			
			//Common character information
			SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
			sm.addString("Admin has changed your karma from " + oldKarma + " to " + newKarma + ".");
			player.sendPacket(sm);
			
			//Admin information	
			SystemMessage smA = new SystemMessage(SystemMessage.S1_S2);
			smA.addString("Successfully Changed karma for "+player.getName()+" from (" + oldKarma + ") to (" + newKarma + ").");		
			activeChar.sendPacket(smA);			
			
			if (Config.DEBUG) 
				_log.fine("[SET KARMA] [GM]"+activeChar.getName()+" Changed karma for "+player.getName()+" from (" + oldKarma + ") to (" + newKarma + ").");
		}
		else {
			// tell admin of mistake 
			SystemMessage smA = new SystemMessage(SystemMessage.S1_S2);
			smA.addString("You must enter a value for karma greater than or equal to 0.");		
			activeChar.sendPacket(smA);
			
			if (Config.DEBUG) 
				_log.fine("[SET KARMA] ERROR: [GM]"+activeChar.getName()+" entered an incorrect value for new karma: " + newKarma + " for "+player.getName()+".");			
		}
	}
		
	private void adminModifyCharacter(L2PcInstance activeChar, String modifications)
	{
		L2Object target = activeChar.getTarget();
        
		if (!(target instanceof L2PcInstance))
            return;

        L2PcInstance player = (L2PcInstance)target;
		StringTokenizer st = new StringTokenizer(modifications);
        
		if (st.countTokens() != 8) {
			editCharacter(player);
            return;
        }

        String hp = st.nextToken();
        String mp = st.nextToken();
        String cp = st.nextToken();
        String karma = st.nextToken();
        String pvpflag = st.nextToken();
        String pvpkills = st.nextToken();
        String pkkills = st.nextToken();
        String classid = st.nextToken();
            
        int hpval = Integer.parseInt(hp);
        int mpval = Integer.parseInt(mp);
        int cpval = Integer.parseInt(cp);
        int karmaval = Integer.parseInt(karma);
        int pvpflagval = Integer.parseInt(pvpflag);
        int pvpkillsval = Integer.parseInt(pvpkills);
        int pkkillsval = Integer.parseInt(pkkills);
        int classidval = Integer.parseInt(classid);
		    
        //Common character information
        player.sendMessage("Admin has changed your stats." +
                           "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + 
                           "  Karma: " + karmaval + "  PvP Flag: " + pvpflagval + " PvP/PK " + pvpkillsval + "/" + pkkillsval + 
                           "  ClassID: " + ClassId.values()[classidval] + " (" + classidval + ")");
		    
        player.setCurrentHp(hpval);
        player.setCurrentMp(mpval);
        player.setCurrentCp(cpval);
        player.setKarma(karmaval);
        player.setPvpFlag(pvpflagval);
        player.setPvpKills(pvpkillsval);
        player.setPkKills(pkkillsval);
        player.setClassId(classidval);
            
        // Update the base class also if this character is not on a sub-class.
        if (!player.isSubClassActive())
            player.setBaseClass(classidval);
            
        // Save the changed parameters to the database.
        player.store();
		    
        StatusUpdate su = new StatusUpdate(player.getObjectId());
        su.addAttribute(StatusUpdate.CUR_HP, hpval);
        su.addAttribute(StatusUpdate.MAX_HP, player.getMaxHp());
        su.addAttribute(StatusUpdate.CUR_MP, mpval);
        su.addAttribute(StatusUpdate.MAX_MP, player.getMaxMp());
        su.addAttribute(StatusUpdate.CUR_CP, cpval);
        su.addAttribute(StatusUpdate.MAX_CP, player.getMaxCp());
        su.addAttribute(StatusUpdate.KARMA, karmaval);
        su.addAttribute(StatusUpdate.PVP_FLAG, pvpflagval);
        player.sendPacket(su);
		    
        //Admin information	
        player.sendMessage("Changed stats of " + player.getName() + "." +
                           "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + 
                           "  Karma: " + karmaval + "  PvP: " + pvpflagval + " / " + pvpkillsval + 
                           "  ClassID: " + ClassId.values()[classidval] + " (" + classidval + ")");
            
        if (Config.DEBUG) 
            _log.fine("[GM]"+activeChar.getName()+" changed stats of "+player.getName()+". " +
                      " HP: "+hpval+" MP: "+mpval+" CP: " + cpval + " Karma: "+karmaval+
                      " PvP: "+pvpflagval+" / "+pvpkillsval+ " ClassID: "+classidval);
		    
        showCharacterList(activeChar, null); //Back to start
            
        player.broadcastPacket(new CharInfo(player));
        player.sendPacket(new UserInfo(player));
            
        player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        
        player.decayMe();
        player.spawnMe(activeChar.getX(),activeChar.getY(),activeChar.getZ());
	}

	private void editCharacter(L2PcInstance activeChar)
	{
		///FIXME Made it so that you have to enter all values to 'prevent' abuses...
		L2Object target = activeChar.getTarget();
        
		if (!(target instanceof L2PcInstance)) 
			return;

        L2PcInstance player = (L2PcInstance)target;
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5); 
		float loadPercent = ((float)player.getCurrentLoad() / (float)player.getMaxLoad()) * 100;
        
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<center>Editing <font color=\"LEVEL\">" + player.getName() + "</font></center><br>");
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td width=40></td><td width=70>Current:</td><td width=70>Maximum:</td><td width=90></td></tr>");
		replyMSG.append("<tr><td width=40>HP:</td><td width=70>" + (int)player.getCurrentHp() + "</td><td width=70>" + player.getMaxHp() + "</td><td width=70>Karma: " + player.getKarma() + "</td><td width=20></td></tr>");
		replyMSG.append("<tr><td width=40>MP:</td><td width=70>" + (int)player.getCurrentMp() + "</td><td width=70>" + player.getMaxMp() + "</td><td width=70>PvP Flag: " + player.getPvpFlag() + "</td><td width=20></td></tr>");
        replyMSG.append("<tr><td width=40>CP:</td><td width=70>" + (int)player.getCurrentCp() + "</td><td width=70>" + player.getMaxCp() + "</td><td width=90>PvP/PK: " + player.getPvpKills() +  "/" + player.getPkKills() +  "</td></tr>");
		replyMSG.append("<tr><td width=40>Load:</td><td width=70>" + player.getCurrentLoad() + "</td><td width=70>" + player.getMaxLoad() + "</td><td width=90>(" + Util.roundTo(loadPercent, 2) + "%)</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<table width=270><tr><td>Class Template ID: " + player.getClassId().ordinal() + " (" + player.getClassId() + ")</td></tr></table><br>");
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td>Note: Fill all values before saving the modifications.</td></tr>");
		replyMSG.append("</table><br>");
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td width=50>Current HP:</td><td><edit var=\"hp\" width=50></td><td width=50>Karma:</td><td><edit var=\"karma\" width=50></td></tr>");
		replyMSG.append("<tr><td width=50>Current MP:</td><td><edit var=\"mp\" width=50></td><td width=50>PvP Flag:</td><td><edit var=\"pvpflag\" width=50></td></tr>");
        replyMSG.append("<tr><td width=50>Current CP:</td><td><edit var=\"cp\" width=50></td><td width=50>PvP Kills:</td><td><edit var=\"pvpkills\" width=50></td></tr>");
        replyMSG.append("<tr><td width=50>Class ID:</td><td><edit var=\"classid\" width=50 valu=\"" + player.getClassId().ordinal() + "\"></td><td width=50>PK Kills:</td><td><edit var=\"pkkills\" width=50></td></tr>");
        replyMSG.append("</table><br>");
		replyMSG.append("<center><button value=\"Save Changes\" action=\"bypass -h" + " admin_save_modifications $hp $mp $cp $karma $pvpflag $pvpkills $pkkills $classid\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center><br>");
		replyMSG.append("</body></html>");

        adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	//FIXME: needs removal, whole thing needs to use getTarget()
	private void findCharacter(L2PcInstance activeChar, String CharacterToFind)
	{
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		int CharactersFound = 0;
		
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_show_characters 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		
		for (int i = 0; i < players.length; i++)
		{	//Add player info into new Table row
			
			if (players[i].getName().startsWith((CharacterToFind)))
			{
				CharactersFound = CharactersFound+1;
				replyMSG.append("<table width=270>");		
				replyMSG.append("<tr><td width=80>Name</td><td width=110>Class</td><td width=40>Level</td></tr>");
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + players[i].getName() + "\">" + players[i].getName() + "</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
				replyMSG.append("</table>");
			}	
		}
		
		if (CharactersFound==0)
    	{
			replyMSG.append("<table width=270>");
			replyMSG.append("<tr><td width=270>Your search did not find any characters.</td></tr>");
			replyMSG.append("<tr><td width=270>Please try again.<br></td></tr>");
			replyMSG.append("</table><br>");
			replyMSG.append("<center><table><tr><td>");
			replyMSG.append("<edit var=\"character_name\" width=80></td><td><button value=\"Find\" action=\"bypass -h admin_find_character $character_name\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</td></tr></table></center>");
		}
		else
		{
			replyMSG.append("<center><br>Found " + CharactersFound + " character");
			
			if (CharactersFound==1)
			{
				replyMSG.append(".");
			}
			else 
			{
				if (CharactersFound>1)
				{
					replyMSG.append("s.");
				}
			}

		}
		
		replyMSG.append("</center></body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);			
	}
}
