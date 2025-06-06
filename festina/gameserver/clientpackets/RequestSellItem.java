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
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2MercManagerInstance;
import com.festina.gameserver.model.actor.instance.L2MerchantInstance;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.InventoryUpdate;
import com.festina.gameserver.serverpackets.ItemList;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.util.Util;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestSellItem extends ClientBasePacket
{
	private static final String _C__1E_REQUESTSELLITEM = "[C] 1E RequestSellItem";
	//private static Logger _log = Logger.getLogger(RequestSellItem.class.getName());

	private final int _listId;
	private int _count;
	private int[] _items; // count*3
	/**
	 * packet type id 0x1e
	 * 
	 * sample
	 * 
	 * 1e
	 * 00 00 00 00		// list id
	 * 02 00 00 00		// number of items
	 * 
	 * 71 72 00 10		// object id
	 * ea 05 00 00		// item id
	 * 01 00 00 00		// item count
	 * 
	 * 76 4b 00 10		// object id
	 * 2e 0a 00 00		// item id
	 * 01 00 00 00		// item count
	 * 
	 * format:		cdd (ddd)
	 * @param decrypt
	 */
	public RequestSellItem(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_listId = readD();
		_count = readD();
		if (_count <= 0)
		{
		    _count = 0; _items = null;
		    return;
		}
		_items = new int[_count * 3];
		for (int i = 0; i < _count; i++)
		{
			int objectId = readD(); _items[i * 3 + 0] = objectId;
			int itemId   = readD(); _items[i * 3 + 1] = itemId;
			long cnt      = readD(); 
			if (cnt > Integer.MAX_VALUE || cnt <= 0)
			{
			    _count = 0; _items = null;
			    return;
			}
			_items[i * 3 + 2] = (int)cnt;
		}
	}

	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
        if (player == null) return;

        // Alt game - Karma punishment
        if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0) return;

        L2Object target = player.getTarget();
        if (!player.isGM() && (target == null								// No target (ie GM Shop)
        		|| !(target instanceof L2MerchantInstance || target instanceof L2MercManagerInstance)	// Target not a merchant and not mercmanager
			    || !player.isInsideRadius(target, L2NpcInstance.INTERACTION_DISTANCE, false, false) 	// Distance is too far
			        )) return;

		L2MerchantInstance merchant = (target != null && target instanceof L2MerchantInstance) ? (L2MerchantInstance)target : null;

		if (_listId > 1000000) // lease
		{
			if (merchant.getTemplate().npcId != _listId-1000000)
			{
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		long totalPrice = 0;
		// Proceed the sell
		InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			int objectId = _items[i * 3 + 0];
			@SuppressWarnings("unused")
			int itemId   = _items[i * 3 + 1];
			int count   = _items[i * 3 + 2];

			if (count < 0 || count > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase over "+Integer.MAX_VALUE+" items at the same time.",  Config.DEFAULT_PUNISH);
				SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				return;
			}

			L2ItemInstance item = player.checkItemManipulation(objectId, count, "sell");
        	if (item == null || (!item.getItem().isSellable())) continue;

            totalPrice += item.getReferencePrice() * count /2;
            if (totalPrice > Integer.MAX_VALUE)
            {
                Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase over "+Integer.MAX_VALUE+" adena worth of goods.",  Config.DEFAULT_PUNISH);
                return;
            }

			item = player.getInventory().destroyItem("Sell", objectId, count, player, null);
			if (playerIU != null) playerIU.addItem(item);
			
/* TODO: Disabled until Leaseholders are rewritten ;-)
			int price = item.getReferencePrice()*(int)count/2;
			L2ItemInstance li = null;
			L2ItemInstance la = null;
			if (_listId > 1000000) {
				li = merchant.findLeaseItem(item.getItemId(),item.getEnchantLevel());
				la = merchant.getLeaseAdena();
				if (li == null || la == null) continue;
				price = li.getPriceToBuy()*(int)count; // player sells, thus merchant buys.
				if (price > la.getCount()) continue;
			}
*/
/* TODO: Disabled until Leaseholders are rewritten ;-)
				if (item != null && _listId > 1000000) {
					li.setCount(li.getCount()+(int)count);
					li.updateDatabase();
					la.setCount(la.getCount()-price);
					la.updateDatabase();
				}
*/
		}
		player.addAdena("Sell", (int)totalPrice, merchant, false);

    	// Send inventory update packet
		if (playerIU != null) player.sendPacket(playerIU);
		else player.sendPacket(new ItemList(player, false));

    	// Update current load as well
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__1E_REQUESTSELLITEM;
	}
}
