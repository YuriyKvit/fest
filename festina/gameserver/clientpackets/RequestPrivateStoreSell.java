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
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.TradeList;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.util.Util;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestPrivateStoreSell extends ClientBasePacket
{
//	private static final String _C__96_SENDPRIVATESTOREBUYBUYLIST = "[C] 96 SendPrivateStoreBuyBuyList";
	private static final String _C__96_REQUESTPRIVATESTORESELL = "[C] 96 RequestPrivateStoreSell";
	private static Logger _log = Logger.getLogger(RequestPrivateStoreSell.class.getName());
	
	private final int _storePlayerId;
	private int _count;
	private int _price;
	private ItemRequest[] _items;
    
	public RequestPrivateStoreSell (ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_storePlayerId = readD();
		_count = readD();
        if (_count < 0)
            _count = 0;
		_items = new ItemRequest[_count];

		long priceTotal = 0;
		for (int i = 0; i < _count; i++)
		{
			int objectId = readD(); 
			int itemId = readD(); 
            readH(); //TODO analyse this
            readH(); //TODO analyse this
			long count   = readD(); 
			int price    = readD(); 
			
			if (count > Integer.MAX_VALUE || count < 0)
			{
	            String msgErr = "[RequestPrivateStoreSell] player "+getClient().getActiveChar().getName()+" tried an overflow exploit, ban this player!";
	            Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
			    _count = 0; 
			    _items = null;
			    return;
			}
			_items[i] = new ItemRequest(objectId, itemId, (int)count, price);
			priceTotal += price * count;
		}
        
		if(priceTotal < 0 || priceTotal > Integer.MAX_VALUE)
        {
            String msgErr = "[RequestPrivateStoreSell] player "+getClient().getActiveChar().getName()+" tried an overflow exploit, ban this player!";
            Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
		    _count = 0; 
		    _items = null;
            return;
        }

		_price = (int)priceTotal;
	}

	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null) return;
		L2Object object = L2World.getInstance().findObject(_storePlayerId);
		if (object == null || !(object instanceof L2PcInstance)) return;
		L2PcInstance storePlayer = (L2PcInstance)object;
		if (storePlayer.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_BUY) return;
		TradeList storeList = storePlayer.getBuyList();
		if (storeList == null) return;
        
        if (Config.GM_DISABLE_TRANSACTION && player.getAccessLevel() >= Config.GM_TRANSACTION_MIN && player.getAccessLevel() <= Config.GM_TRANSACTION_MAX)
        {
        	player.sendMessage("Transactions are disable for your Access Level");
            sendPacket(new ActionFailed());
            return;
        }
        
        if (storePlayer.getAdena() < _price)
		{
			sendPacket(new ActionFailed());
        	storePlayer.sendMessage("You have not enough adena, canceling PrivateBuy.");
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();
			return;
		}
        
        if (!storeList.PrivateStoreSell(player, _items, _price))
        {
            sendPacket(new ActionFailed());
            _log.warning("PrivateStore sell has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
            return;
        }

        if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();
		}
	}
    
	public String getType()
	{
		return _C__96_REQUESTPRIVATESTORESELL;
	}
}
