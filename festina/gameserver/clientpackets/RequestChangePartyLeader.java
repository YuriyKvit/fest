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

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestChangePartyLeader extends ClientBasePacket{
	
	private static final String _C__EE_REQUESTCHANGEPARTYLEADER = "[C] EE RequestChangePartyLeader";
	//private static Logger _log = Logger.getLogger(RequestJoinParty.class.getName());

	private final String _name;
	
	public RequestChangePartyLeader(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_name = readS();
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		    return;
		
		if (activeChar.isInParty() && activeChar.getParty().isLeader(activeChar)) 
			activeChar.getParty().changePartyLeader(_name);
	}
	
	
	public String getType()
	{
		return _C__EE_REQUESTCHANGEPARTYLEADER;
	}
}
