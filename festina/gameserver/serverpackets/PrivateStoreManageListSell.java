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

import com.festina.gameserver.model.TradeList;
import com.festina.gameserver.model.actor.instance.L2PcInstance;


/**
 * 3 section to this packet
 * 1)playerinfo which is always sent
 * dd
 *
 * 2)list of items which can be added to sell
 * d(hhddddhhhd)
 *
 * 3)list of items which have already been setup 
 * for sell in previous sell private store sell manageent 
 * d(hhddddhhhdd) * 
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PrivateStoreManageListSell  extends ServerBasePacket
{
	private static final String _S__B3_PRIVATESELLLISTSELL = "[S] 9a PrivateSellListSell";
	private L2PcInstance _player;
	private int _playerAdena;
	private TradeList.TradeItem[] _itemList;
	private TradeList.TradeItem[] _sellList;
	
	public PrivateStoreManageListSell(L2PcInstance player)
	{
		_player = player;
	}
	
	final void runImpl()
	{
		_playerAdena = _player.getAdena();
		_player.getSellList().updateItems();
		_itemList = _player.getInventory().getAvailableItems(_player.getSellList());
		_sellList = _player.getSellList().getItems(); 
	}
	
	final void writeImpl()
	{
		writeC(0x9a);
		//section 1 
		writeD(_player.getObjectId());
		writeD(0); // Package sell
		writeD(_playerAdena);

		//section2 
		writeD(_itemList.length); //for potential sells
		for (TradeList.TradeItem item : _itemList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0);
			writeH(item.getEnchant());//enchant lvl
			writeH(0);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice()); //store price
		}	
		//section 3
		writeD(_sellList.length); //count for any items already added for sell
		for (TradeList.TradeItem item : _sellList)
		{
			writeD(item.getItem().getType2()); 
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0);	
			writeH(item.getEnchant());//enchant lvl
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice());//your price
			writeD(item.getItem().getReferencePrice()); //store price
		}	
	}
	
	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__B3_PRIVATESELLLISTSELL;
	}
}
