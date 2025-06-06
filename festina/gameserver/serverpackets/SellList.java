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

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.festina.Config;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2MerchantInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 * 
 * @version $Revision: 1.4.2.3.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class SellList extends ServerBasePacket
{
	private static final String _S__10_SELLLIST = "[S] 10 SellList";
	private static Logger _log = Logger.getLogger(SellList.class.getName());
	private final L2PcInstance _char;
	private final L2MerchantInstance _lease;
	private int _money;
	private List<L2ItemInstance> _selllist = new FastList<L2ItemInstance>();
	
	public SellList(L2PcInstance player)
	{
		_char = player;
		_lease = null;
		_money = _char.getAdena();
	}
	
	public SellList(L2PcInstance player, L2MerchantInstance lease)
	{
		_char = player;
		_lease = lease;
		_money = _char.getAdena();
	}
	
	final void runImpl()
	{
		if (_lease == null)
		{
			for (L2ItemInstance item : _char.getInventory().getItems())
			{
				if (!item.isEquipped() &&                                                      // Not equipped 
                        item.getItem().isSellable() &&                                         // Item is sellable
                        (_char.getPet() == null ||                                             // Pet not summoned or
                                item.getObjectId() != _char.getPet().getControlItemId()))      // Pet is summoned and not the item that summoned the pet
				{
					_selllist.add(item);
					if (Config.DEBUG) _log.fine("item added to selllist: " + item.getItem().getName());
				}
			}
		}
	}
	
	final void writeImpl()
	{
		writeC(0x10);
		writeD(_money);
		writeD(_lease == null ? 0x00 : 1000000 + _lease.getTemplate().npcId);
		
		writeH(_selllist.size());
		
		for (L2ItemInstance item : _selllist)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(0x00);
			writeH(0x00);
			
			if (_lease == null)
				writeD(item.getItem().getReferencePrice()/2);
		}
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__10_SELLLIST;
	}
}
