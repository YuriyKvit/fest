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
/**
 * Format chS
 * c (id) 0xD0
 * h (subid) 0x0C
 * S the hero's words :)
 * @author -Wooden-
 *
 */
public class RequestWriteHeroWords extends ClientBasePacket
{
	private static final String _C__FE_0C_REQUESTWRITEHEROWORDS = "[C] D0:0C RequestWriteHeroWords";
	@SuppressWarnings("unused")
	private String _heroWords;

	/**
	 * @param buf
	 * @param client
	 */
	public RequestWriteHeroWords(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_heroWords = readS();
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	void runImpl()
	{
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__FE_0C_REQUESTWRITEHEROWORDS;
	}
	
}