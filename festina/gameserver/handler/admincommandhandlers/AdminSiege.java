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

import java.util.StringTokenizer;

import javolution.lang.TextBuilder;

import com.festina.Config;
import com.festina.gameserver.ClanTable;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.instancemanager.ClanHallManager;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.entity.Castle;
import com.festina.gameserver.model.entity.ClanHall;
import com.festina.gameserver.model.zone.type.L2ClanHallZone;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class handles all siege commands:
 * Todo: change the class name, and neaten it up
 * 
 *
 */
public class AdminSiege implements IAdminCommandHandler
{
    //private static Logger _log = Logger.getLogger(AdminSiege.class.getName());

    private static String[] _adminCommands = {"admin_siege",
            "admin_add_attacker", "admin_add_defender", "admin_add_guard",
            "admin_list_siege_clans", "admin_clear_siege_list",
            "admin_move_defenders", "admin_spawn_doors",
            "admin_endsiege", "admin_startsiege",
            "admin_setcastle", 
            "admin_clanhall","admin_clanhallset","admin_clanhalldel",
	    "admin_clanhallopendoors","admin_clanhallclosedoors",
	    "admin_clanhallteleportself"
	    };
    private static final int REQUIRED_LEVEL = Config.GM_NPC_EDIT;

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (activeChar.getAccessLevel() < REQUIRED_LEVEL || !activeChar.isGM()) {return false;}

        StringTokenizer st = new StringTokenizer(command, " ");
        command = st.nextToken(); // Get actual command


        // Get castle
        Castle castle = null;
	ClanHall clanhall = null;
        if (command.startsWith("admin_clanhall"))
	{
            clanhall = ClanHallManager.getInstance().getClanHallById(Integer.parseInt(st.nextToken()));
	} else if (st.hasMoreTokens()) {
	    castle = CastleManager.getInstance().getCastle(st.nextToken());
	}

        // Get castle
        String val = "";
        if (st.hasMoreTokens()) {val = st.nextToken();}

        if ((castle == null  || castle.getCastleId() < 0) && clanhall == null)
            // No castle specified
            showCastleSelectPage(activeChar);
        else
        {
            L2Object target = activeChar.getTarget();
            L2PcInstance player = null;
            if (target instanceof L2PcInstance)
                player = (L2PcInstance)target;

            if (command.equalsIgnoreCase("admin_add_attacker"))
            {
                if (player == null)
                    activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
                else
                    castle.getSiege().registerAttacker(player,true);
            }
            else if (command.equalsIgnoreCase("admin_add_defender"))
            {
                if (player == null)
                    activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
                else
                    castle.getSiege().registerDefender(player,true);
            }
            else if (command.equalsIgnoreCase("admin_add_guard"))
            {
                if (val != "")
                {
                    try
                    {
                        int npcId = Integer.parseInt(val);
                        castle.getSiege().getSiegeGuardManager().addSiegeGuard(activeChar, npcId);
                    }
                    catch (Exception e)
                    {
                        activeChar.sendMessage("Value entered for Npc Id wasn't an integer");
                    }
                }
                else
                    activeChar.sendMessage("Missing Npc Id");
            }
            else if (command.equalsIgnoreCase("admin_clear_siege_list"))
            {
                castle.getSiege().clearSiegeClan();
            }
            else if (command.equalsIgnoreCase("admin_endsiege"))
            {
                castle.getSiege().endSiege();
            }
            else if (command.equalsIgnoreCase("admin_list_siege_clans"))
            {
                castle.getSiege().listRegisterClan(activeChar);
                return true;
            }
            else if (command.equalsIgnoreCase("admin_move_defenders"))
            {
                activeChar.sendPacket(SystemMessage.sendString("Not implemented yet."));
            }
            else if (command.equalsIgnoreCase("admin_setcastle"))
            {
                if (player == null)
                    activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
                else
                    castle.setOwner(player.getClan());
            }
            else if (command.equalsIgnoreCase("admin_clanhallset"))
            {
                if (player == null || player.getClan() == null)
                    activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
                else
                    clanhall.setOwner(player.getClan());
            }
            else if (command.equalsIgnoreCase("admin_clanhalldel"))
            {
                    clanhall.setOwner(null);
            }
            else if (command.equalsIgnoreCase("admin_clanhallopendoors"))
            {
                    clanhall.openCloseDoors(true);
            }
            else if (command.equalsIgnoreCase("admin_clanhallclosedoors"))
            {
                    clanhall.openCloseDoors(false);
            }
            else if (command.equalsIgnoreCase("admin_clanhallteleportself"))
            {
                L2ClanHallZone zone = clanhall.getZone();
            		if (zone != null)
            		{
            		    activeChar.teleToLocation(zone.getSpawn()); 
			}
            }
            else if (command.equalsIgnoreCase("admin_spawn_doors"))
            {
                castle.spawnDoor();
            }
            else if (command.equalsIgnoreCase("admin_startsiege"))
            {
                castle.getSiege().startSiege();
            }

	    if (clanhall != null)
        	showClanHallPage(activeChar, clanhall);
	    else
        	showSiegePage(activeChar, castle.getName());
        }

        return true;
    }

    private void showCastleSelectPage(L2PcInstance activeChar)
    {
        int i=0;
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        adminReply.setFile("data/html/admin/castles.htm");
        TextBuilder cList = new TextBuilder();
        for (Castle castle: CastleManager.getInstance().getCastles())
        {
            if (castle != null)
            {
                String name=castle.getName();
                cList.append("<td fixwidth=90><a action=\"bypass -h admin_siege "+name+"\">"+name+"</a></td>");
                i++;
            }
            if (i>2)
            {
                cList.append("</tr><tr>");
                i=0;
            }
        }
        adminReply.replace("%castles%", cList.toString());
        //cList.clear();
        i=0;
        for (ClanHall clanhall: ClanHallManager.getInstance().getClanHalls().values())
        {
            if (clanhall != null)
            {
                cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall "+clanhall.getId()+"\">");
                cList.append(clanhall.getName()+"</a></td>");
                i++;
            }
            if (i>1)
            {
                cList.append("</tr><tr>");
                i=0;
            }
        }
        adminReply.replace("%clanhalls%", cList.toString());
        //cList.clear();
        i=0;
        for (ClanHall clanhall: ClanHallManager.getInstance().getFreeClanHalls().values())
        {
            if (clanhall != null)
            {
                cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall "+clanhall.getId()+"\">");
                cList.append(clanhall.getName()+"</a></td>");
                i++;
            }
            if (i>1)
            {
                cList.append("</tr><tr>");
                i=0;
            }
        }
        adminReply.replace("%freeclanhalls%", cList.toString());
        activeChar.sendPacket(adminReply);
    }

    
    public void showSiegePage(L2PcInstance activeChar, String castleName)
    {
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        
        TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Siege Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_siege\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
        replyMSG.append("<center>");
        replyMSG.append("<br><br><br>Castle: " + castleName + "<br><br>");
        replyMSG.append("<table>");
        replyMSG.append("<tr><td><button value=\"Add Attacker\" action=\"bypass -h admin_add_attacker " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("<td><button value=\"Add Defender\" action=\"bypass -h admin_add_defender " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("<tr><td><button value=\"List Clans\" action=\"bypass -h admin_list_siege_clans " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("<td><button value=\"Clear List\" action=\"bypass -h admin_clear_siege_list " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("<br>");
        replyMSG.append("<table>");
        replyMSG.append("<tr><td><button value=\"Move Defenders\" action=\"bypass -h admin_move_defenders " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("<td><button value=\"Spawn Doors\" action=\"bypass -h admin_spawn_doors " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("<br>");
        replyMSG.append("<table>");
        replyMSG.append("<tr><td><button value=\"Start Siege\" action=\"bypass -h admin_startsiege " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("<td><button value=\"End Siege\" action=\"bypass -h admin_endsiege " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("<br>");
        replyMSG.append("<table>");
        replyMSG.append("<tr><td><button value=\"Give Castle\" action=\"bypass -h admin_setcastle " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("<br>");
        replyMSG.append("<table>");
        replyMSG.append("<tr><td>NpcId: <edit var=\"value\" width=40>");
        replyMSG.append("<td><button value=\"Add Guard\" action=\"bypass -h admin_add_guard " + castleName + " $value\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("</center>");
        replyMSG.append("</body></html>");
        
        adminReply.setHtml(replyMSG.toString());
        activeChar.sendPacket(adminReply);
    }

    public void showClanHallPage(L2PcInstance activeChar, ClanHall clanhall)
    {
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        
        TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Siege Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_siege\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
        replyMSG.append("<center>");
        replyMSG.append("<br><br><br>ClanHall: " + clanhall.getName() + "<br>");

        L2Clan owner = ClanTable.getInstance().getClan(clanhall.getOwnerId()); 
        if (owner == null)
        	replyMSG.append("ClanHall Owner: none<br><br>");
        else	
    	    replyMSG.append("ClanHall Owner: " + owner.getName() + "<br><br>");

        //replyMSG.append("<table>");
        //replyMSG.append("<tr><td><button value=\" Owner\" action=\"bypass -h admin_clanhallset " + clanhall.getId() + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        //replyMSG.append("</table>");
        replyMSG.append("<br>");
        //replyMSG.append("<td><button value=\"Add Defender\" action=\"bypass -h admin_add_defender " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        //replyMSG.append("<tr><td><button value=\"List Clans\" action=\"bypass -h admin_list_siege_clans " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        //replyMSG.append("<td><button value=\"Clear List\" action=\"bypass -h admin_clear_siege_list " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        //replyMSG.append("<table>");
        //replyMSG.append("<tr><td><button value=\"Move Defenders\" action=\"bypass -h admin_move_defenders " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        //replyMSG.append("<td><button value=\"Spawn Doors\" action=\"bypass -h admin_spawn_doors " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        //replyMSG.append("</table>");
        replyMSG.append("<br>");
        //replyMSG.append("<table>");
        //replyMSG.append("<tr><td><button value=\"Start Siege\" action=\"bypass -h admin_startsiege " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        //replyMSG.append("<td><button value=\"End Siege\" action=\"bypass -h admin_endsiege " + castleName + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        //replyMSG.append("</table>");
        replyMSG.append("<table>");
        replyMSG.append("<tr><td><button value=\"Open Doors\" action=\"bypass -h admin_clanhallopendoors " + clanhall.getId() + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("<td><button value=\"Close Doors\" action=\"bypass -h admin_clanhallclosedoors " + clanhall.getId() + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("<br>");
        replyMSG.append("<table>");
        replyMSG.append("<tr><td><button value=\"Give ClanHall\" action=\"bypass -h admin_clanhallset " + clanhall.getId() + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("<td><button value=\"Take ClanHall\" action=\"bypass -h admin_clanhalldel " + clanhall.getId() + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("<br>");
        replyMSG.append("<table><tr>");
        //replyMSG.append("<tr><td>NpcId: <edit var=\"value\" width=40>");
        replyMSG.append("<td><button value=\"Teleport self\" action=\"bypass -h admin_clanhallteleportself " + clanhall.getId() + " \" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table>");
        replyMSG.append("</center>");
        replyMSG.append("</body></html>");
        
        adminReply.setHtml(replyMSG.toString());
        activeChar.sendPacket(adminReply);
    }

    public String[] getAdminCommandList() {
        return _adminCommands;
    }
    
}
