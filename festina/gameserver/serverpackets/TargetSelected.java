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

/**
 * format   ddddd
 * 
 * sample
 * 0000: 39  0b 07 10 48  3e 31 10 48  3a f6 00 00  91 5b 00    9...H>1.H:....[.
 * 0010: 00  4c f1 ff ff                                     .L...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class TargetSelected extends ServerBasePacket
{
	private static final String _S__39_TARGETSELECTED = "[S] 29 TargetSelected";
	private int _objectId;
	private int _targetId;
	private int _x;
	private int _y;
	private int _z;
	

	/**
	 * @param _characters
	 */
	public TargetSelected(int objectId, int targetId, int x, int y, int z)
	{
		_objectId = objectId;
		_targetId = targetId;
		_x = x;
		_y = y;
		_z = z;
	}


	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0x29);
		writeD(_objectId);
		writeD(_targetId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__39_TARGETSELECTED;
	}
}
