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

import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;

/**
 * This class ...
 * 
 * @version $Revision: 1.4.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class PetItemList extends ServerBasePacket 
{
	private static Logger _log = Logger.getLogger(PetItemList.class.getName());
	private static final String _S__cb_PETITEMLIST = "[S] b2  PetItemList";
	private L2PetInstance _cha;

	public PetItemList(L2PetInstance cha)
	{
		_cha = cha;
		
	}	
	
	final void runImpl()
	{
		if (Config.DEBUG)
		{
			L2ItemInstance[] items = _cha.getInventory().getItems();
			for (L2ItemInstance temp : items)
			{
				_log.fine("item:" + temp.getItem().getName() +
						" type1:" + temp.getItem().getType1() + " type2:" + temp.getItem().getType2());
			}
		}
	}
	
	final void writeImpl()
	{
		writeC(0xB2);
		
		L2ItemInstance[] items = _cha.getInventory().getItems();
		int count = items.length; 
		writeH(count);
		
		for (L2ItemInstance temp : items)
		{
			writeH(temp.getItem().getType1()); // item type1
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(temp.getItem().getType2());	// item type2
			writeH(0xff);	// ?
			if (temp.isEquipped())
			{
				writeH(0x01);
			}
			else
			{
				writeH(0x00);
			}
			writeD(temp.getItem().getBodyPart());	// rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
//			writeH(temp.getItem().getBodyPart());	// rev 377  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			writeH(temp.getEnchantLevel());	// enchant level
			writeH(0x00);	// ?
		}
	}
	
	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__cb_PETITEMLIST;
	}
}
