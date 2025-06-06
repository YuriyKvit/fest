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

import com.festina.gameserver.model.actor.instance.L2BoatInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Maktakien
 *
 */
public class GetOffVehicle extends ServerBasePacket
{
	private int _x;
	private int _y;
	private int _z;
	private L2PcInstance _pci;
	private L2BoatInstance _boat;

	/**
	 * @param activeChar
	 * @param boat
	 * @param x
	 * @param y
	 * @param z
	 */
	public GetOffVehicle(L2PcInstance activeChar, L2BoatInstance boat, int x, int y, int z)
	{
		_pci = activeChar;
		_boat = boat;
		_x = x;
		_y = y;
		_z = z;
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#runImpl()
	 */
	@Override
	void runImpl()
	{
		if (_pci == null) return;
		
		_pci.setInBoat(false);
		_pci.setBoat(null);
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	void writeImpl()
	{
		if (_boat == null || _pci == null) return;

		writeC(0x5d);
		writeD(_pci.getObjectId());
		writeD(_boat.getObjectId());
		writeD(_x);
		writeD(_y);
		writeD(_z);

	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return "[S] 5d GetOffVehicle";
	}

}
