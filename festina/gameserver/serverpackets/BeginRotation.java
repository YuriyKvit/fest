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
package com.festina.gameserver.serverpackets;

import com.festina.gameserver.model.actor.instance.L2PcInstance;

public class BeginRotation extends ServerBasePacket
{
	private static final String _S__77_BEGINROTATION = "[S] 62 BeginRotation";
	private int _charId;
	private int _degree;
	private int _side;
	
	public BeginRotation(L2PcInstance player, int degree, int side)
	{
		_charId = player.getObjectId();
		_degree = degree;
		_side = side;
	}
	
	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0x62);
		writeD(_charId);
		writeD(_degree);
		writeD(_side);
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__77_BEGINROTATION;
	}
}
