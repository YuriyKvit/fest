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
import com.festina.gameserver.communitybbs.CommunityBoard;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestShowBoard extends ClientBasePacket
{
	private static final String _C__57_REQUESTSHOWBOARD = "[C] 57 RequestShowBoard";

	@SuppressWarnings("unused")
    private final int _unknown;
	/**
	 * packet type id 0x57
	 * 
	 * sample
	 * 
	 * 57
	 * 01 00 00 00		// unknown (always 1?)
	 * 
	 * format:		cd
	 * @param decrypt
	 */
	public RequestShowBoard(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_unknown = readD();
	}

	void runImpl()
	{
		CommunityBoard.getInstance().handleCommands(getClient(), Config.BBS_DEFAULT);
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__57_REQUESTSHOWBOARD;
	}
}
