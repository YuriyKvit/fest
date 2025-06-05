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
package com.festina.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import com.festina.Config;
import com.festina.gameserver.TeleportLocationTable;
import com.festina.gameserver.datatables.NobleTable;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.instancemanager.SiegeManager;
import com.festina.gameserver.instancemanager.TownManager;
import com.festina.gameserver.model.L2TeleportLocation;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2NpcTemplate;

/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 *
 */
public final class L2TeleporterInstance extends L2FolkInstance
{
    //private static Logger _log = Logger.getLogger(L2TeleporterInstance.class.getName());

    private static int Cond_All_False = 0;
    private static int Cond_Busy_Because_Of_Siege = 1;
    private static int Cond_Owner = 2;
    private static int Cond_Regular = 3;

    /**
     * @param template
     */
    public L2TeleporterInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    public void onBypassFeedback(L2PcInstance player, String command)
    {
        player.sendPacket(new ActionFailed());

        int condition = validateCondition(player);

        StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command

        if (actualCommand.equalsIgnoreCase("goto"))
        {
            int npcId = getTemplate().npcId;

            switch (npcId)
            {
                case 8095: //
                case 8096: //
                case 8097: //
                case 8098: // Enter Necropolises
                case 8099: //
                case 8100: //
                case 8101: // 
                case 8102: //

                case 8114: //
                case 8115: //
                case 8116: // Enter Catacombs
                case 8117: //
                case 8118: //
                case 8119: //
                    player.setIsIn7sDungeon(true);
                    break;
                case 8103: //
                case 8104: //
                case 8105: //
                case 8106: // Exit Necropolises
                case 8107: //
                case 8108: //
                case 8109: // 
                case 8110: //

                case 8120: //
                case 8121: //
                case 8122: // Exit Catacombs
                case 8123: //
                case 8124: //
                case 8125: //
                    player.setIsIn7sDungeon(false);
                    break;
            }

            if (st.countTokens() <= 0)
            {
                return;
            }
            int whereTo = Integer.parseInt(st.nextToken());
            if (condition == Cond_Regular)
            {
                doTeleport(player, whereTo);
                return;
            }
            else if (condition == Cond_Owner)
            {
                int minPrivilegeLevel = 0; // NOTE: Replace 0 with highest level when privilege level is implemented
                if (st.countTokens() >= 1)
                {
                    minPrivilegeLevel = Integer.parseInt(st.nextToken());
                }
                if (10 >= minPrivilegeLevel) // NOTE: Replace 10 with privilege level of player
                doTeleport(player, whereTo);
                else player.sendMessage("You do not sufficient access level to teleport there.");
                return;
            }
        }

        super.onBypassFeedback(player, command);
    }

    public String getHtmlPath(int npcId, int val)
    {
        String pom = "";
        if (val == 0)
        {
            pom = "" + npcId;
        }
        else
        {
            pom = npcId + "-" + val;
        }

        return "data/html/teleporter/" + pom + ".htm";
    }

    public void showChatWindow(L2PcInstance player)
    {
        String filename = "data/html/teleporter/castleteleporter-no.htm";

        int condition = validateCondition(player);
        if (condition == Cond_Regular)
        {
            super.showChatWindow(player);
            return;
        }
        else if (condition > Cond_All_False)
        {
            if (condition == Cond_Busy_Because_Of_Siege) filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
            else if (condition == Cond_Owner) // Clan owns castle
                filename = getHtmlPath(getNpcId(), 0); // Owner message window
        }

        NpcHtmlMessage html = new NpcHtmlMessage(1);
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    private void doTeleport(L2PcInstance player, int val)
    {
        L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
        if (list != null)
        {
            //you cannot teleport to village that is in siege
            if (SiegeManager.getInstance().getSiege(list.getLocX(), list.getLocY(), list.getLocZ()) != null)
            {
                player.sendPacket(new SystemMessage(707));
                return;
            }
            else if (TownManager.getInstance().townHasCastleInSeige(list.getLocX(), list.getLocY()))
            {
                player.sendPacket(new SystemMessage(707));
                return;
            }
            if ((list.getIsForNoble()) && (!NobleTable.isNoble(player.getCharId())))
            {
             String filename = "data/html/teleporter/nobleteleporter-no.htm";
             NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
             html.setFile(filename);
             html.replace("%objectId%", String.valueOf(getObjectId()));
             html.replace("%npcname%", getName());
             player.sendPacket(html);
             return;
           }
           if (Config.ALT_GAME_FREE_TELEPORT)
           {
             player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
           }
           else if (player.reduceAdena("Teleport", list.getPrice(), this, true))
           {
            
                if (Config.DEBUG)
                    _log.fine("Teleporting player " + player.getName() + " to new location: "
                        + list.getLocX() + ":" + list.getLocY() + ":" + list.getLocZ());
                player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
            }
        }
        else
        {
            _log.warning("No teleport destination with id:" + val);
        }
        player.sendPacket(new ActionFailed());
    }

    private int validateCondition(L2PcInstance player)
    {
        if (CastleManager.getInstance().getCastleIndex(this) < 0) // Teleporter isn't on castle ground
        return Cond_Regular; // Regular access
        else if (getCastle().getSiege().getIsInProgress()) // Teleporter is on castle ground and siege is in progress
        return Cond_Busy_Because_Of_Siege; // Busy because of siege
        else if (player.getClan() != null) // Teleporter is on castle ground and player is in a clan
        {
            if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
                return Cond_Owner; // Owner
        }

        return Cond_All_False;
    }
}
