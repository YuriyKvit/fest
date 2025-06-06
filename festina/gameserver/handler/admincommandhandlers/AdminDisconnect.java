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

import com.festina.Config;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.model.GMAudit;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.LeaveWorld;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - character_disconnect = disconnects target player
 * 
 * @version $Revision: 1.2.4.4 $ $Date: 2005/04/11 10:06:00 $
 */
public class AdminDisconnect implements IAdminCommandHandler {

	private static String[] _adminCommands = {"admin_character_disconnect"};
	private static final int REQUIRED_LEVEL = Config.GM_KICK;

	public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;

		if (command.equals("admin_character_disconnect"))
		{ 
			disconnectCharacter(activeChar);			
		}		

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		return true;
	}
	
	public String[] getAdminCommandList() {
		return _adminCommands;
	}
	
	private boolean checkLevel(int level) {
		return (level >= REQUIRED_LEVEL);
	}
	
	private void disconnectCharacter(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance) {
			player = (L2PcInstance)target;
		} else {
			return;
		}
		
		if (player.getObjectId() == activeChar.getObjectId())
		{		
			SystemMessage sm = new SystemMessage(614);
			sm.addString("You cannot logout your character.");
			activeChar.sendPacket(sm);
		}
		else
		{				
			SystemMessage sm = new SystemMessage(614);
			sm.addString("Character " + player.getName() + " disconnected from server.");
			activeChar.sendPacket(sm);
			
			//Logout Character
			LeaveWorld ql = new LeaveWorld();
			player.sendPacket(ql);
			
			player.closeNetConnection();
		}
	}
}
