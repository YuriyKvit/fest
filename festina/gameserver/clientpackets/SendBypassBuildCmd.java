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
package com.festina.gameserver.clientpackets;

import java.nio.ByteBuffer;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.handler.AdminCommandHandler;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.util.Util;

/**
 * This class handles all GM commands triggered by //command
 * 
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:29 $
 */
public class SendBypassBuildCmd extends ClientBasePacket
{
	private static final String _C__5B_SENDBYPASSBUILDCMD = "[C] 5b SendBypassBuildCmd";
	public final static int GM_MESSAGE = 9;
	public final static int ANNOUNCEMENT = 10;

	private final String _command;
	
	public SendBypassBuildCmd(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_command = readS();
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
        if(activeChar == null)
            return;
		
        if (Config.ALT_PRIVILEGES_ADMIN && !AdminCommandHandler.getInstance().checkPrivileges(activeChar,"admin_"+_command))
            return;
        
        if(!activeChar.isGM() && !"gm".equalsIgnoreCase(_command))
        {
        	Util.handleIllegalPlayerAction(activeChar,"Warning!! Non-gm character "+activeChar.getName()+" requests gm bypass handler, hack?", Config.DEFAULT_PUNISH);
        	return;
        }
        
		IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler("admin_"+_command);
        
		if (ach != null) 
		{
			ach.useAdminCommand("admin_"+_command, activeChar);
		} 
	}
		
	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__5B_SENDBYPASSBUILDCMD;
	}
}
