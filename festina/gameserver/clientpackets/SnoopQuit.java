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
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author -Wooden-
 * 
 */
public class SnoopQuit extends ClientBasePacket
{
	private static final String _C__AB_SNOOPQUIT = "[C] AB SnoopQuit";

	private int _snoopID;

	/**
	 * @param buf
	 * @param client
	 */
	public SnoopQuit(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_snoopID = readD();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	void runImpl()
	{
		L2PcInstance player = (L2PcInstance) L2World.getInstance().findObject(
				_snoopID);
		if (player == null)
			return;
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		player.removeSnooper(activeChar);
		activeChar.removeSnooped(player);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.festina.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__AB_SNOOPQUIT;
	}

}
