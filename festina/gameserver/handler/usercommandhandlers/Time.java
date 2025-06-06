/* * This program is free software; you can redistribute it and/or modify
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
package com.festina.gameserver.handler.usercommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.festina.gameserver.GameTimeController;
import com.festina.gameserver.handler.IUserCommandHandler;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * 
 *
 */
public class Time implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS = { 77 }; 
    /* (non-Javadoc)
     * 
     */
    public boolean useUserCommand(int id, L2PcInstance activeChar)
    {
        if (COMMAND_IDS[0] != id) return false;
        
		int t = GameTimeController.getInstance().getGameTime();
		int h = t/60;
		int m = t%60;
		Calendar gamt = Calendar.getInstance();
		gamt.set(Calendar.HOUR_OF_DAY, h);
		gamt.set(Calendar.MINUTE, m);
		
		String RealTime = (new SimpleDateFormat("H:mm")).format(new Date());
		String GameTime = (new SimpleDateFormat("H:mm")).format(gamt.getTime());

		SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
        sm.addString("Game Time: "+GameTime+", Real Time: "+RealTime);
        activeChar.sendPacket(sm);
        return true;
    }

    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}
