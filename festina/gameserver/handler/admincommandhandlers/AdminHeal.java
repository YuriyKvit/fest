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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package com.festina.gameserver.handler.admincommandhandlers;

import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - res = resurrects target L2Character
 * 
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminHeal implements IAdminCommandHandler {
    private static Logger _log = Logger.getLogger(AdminRes.class.getName());
    private static String[] _adminCommands = {"admin_heal"};
    private static final int REQUIRED_LEVEL = Config.GM_HEAL;
    
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) 
            	return false;
        
        if (command.equals("admin_heal")) handleRes(activeChar);
        else if (command.startsWith("admin_heal")) {            
            try
            {   
                String healTarget = command.substring(11);
                handleRes(activeChar, healTarget);
            }
            catch (StringIndexOutOfBoundsException e)
            {
                if ( Config.DEVELOPER ) 
                	_log.info("Heal error: " + e);
                SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
                sm.addString("Incorrect target/radius specified.");
                activeChar.sendPacket(sm);
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
    
    private void handleRes(L2PcInstance activeChar)
    {
        handleRes(activeChar, null);
    }
    
    private void handleRes(L2PcInstance activeChar, String player) {
        
        L2Object obj = activeChar.getTarget();
        if (player != null) {
            L2PcInstance plyr = L2World.getInstance().getPlayer(player);
            
            if (plyr != null) obj = plyr;            
            else {
                try
                {
                    int radius  = Integer.parseInt(player);
                    for (L2Object object : activeChar.getKnownList().getKnownObjects())
                    {
                        if (object instanceof L2Character)
                        {
                            L2Character character = (L2Character) object;
                            character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
                            if ( object instanceof L2PcInstance ) character.setCurrentCp(character.getMaxCp());
                        }
                    }
                    activeChar.sendMessage("Healed within " + radius + " unit radius.");
                    return;
                } catch (NumberFormatException nbe) {}
            }
        }
        
        if (obj == null) obj = activeChar;
        
        if ((obj != null) && (obj instanceof L2Character)) {
            L2Character target = (L2Character)obj;
            target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
            if ( target instanceof L2PcInstance ) target.setCurrentCp(target.getMaxCp());
            if (Config.DEBUG) 
                _log.fine("GM: "+activeChar.getName()+"("+activeChar.getObjectId()+") healed character "+target.getName());
        } 
        else {
            SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
            sm.addString("Incorrect target.");
            activeChar.sendPacket(sm);
        }
    }
}
