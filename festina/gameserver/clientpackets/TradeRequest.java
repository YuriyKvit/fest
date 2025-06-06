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
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.SendTradeRequest;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * 
 * This class ...
 * 
 * @version $Revision: 1.2.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class TradeRequest extends ClientBasePacket
{
	private static final String TRADEREQUEST__C__15 = "[C] 15 TradeRequest";
	private static Logger _log = Logger.getLogger(TradeRequest.class.getName());
	
	private final int _objectId;
	
	public TradeRequest(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_objectId = readD();
	}

	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
        if (player == null) return;
        
        if (Config.GM_DISABLE_TRANSACTION && player.getAccessLevel() >= Config.GM_TRANSACTION_MIN && player.getAccessLevel() <= Config.GM_TRANSACTION_MAX)
        {
        	player.sendMessage("Transactions are disable for your Access Level");
            sendPacket(new ActionFailed());
            return;
        }
        
		L2Object target = L2World.getInstance().findObject(_objectId);
        if (target == null || !player.getKnownList().knowsObject(target) 
        		|| !(target instanceof L2PcInstance) || (target.getObjectId() == player.getObjectId()))
		{
			player.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
        if(player.getPet() != null && Config.DONT_ALLOW_TRADE_WITH_PET)
        {
        	player.sendMessage("Don't allow trade while you have summon!");
        	return;
        }
		
        L2PcInstance partner = (L2PcInstance)target;
        
        if (partner.isInOlympiadMode() || player.isInOlympiadMode())
        {
            player.sendMessage("You or your target cant request trade in Olympiad mode");
            return;
        }
        if(player.isMounted() || partner.isMounted()) //�������
        {
            player.sendMessage("Don't allow trade from wywern");
            return;
        }
        // Alt game - Karma punishment
        if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && (player.getKarma() > 0 || partner.getKarma() > 0))
        {
        	player.sendMessage("Chaotic players can't use Trade.");
            return;
        }

        if (player.getPrivateStoreType() != 0 || partner.getPrivateStoreType() != 0)
        {
            player.sendPacket(new SystemMessage(SystemMessage.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
            return;
        }

        if (player.isProcessingTransaction())
		{
			if (Config.DEBUG) _log.fine("already trading with someone");
			player.sendPacket(new SystemMessage(SystemMessage.ALREADY_TRADING));
			return;
		}

		if (partner.isProcessingRequest() || partner.isProcessingTransaction()) 
		{
			_log.info("transaction already in progress.");
			SystemMessage sm = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
			sm.addString(partner.getName());
			player.sendPacket(sm);
            return;
		}

        if (partner.getTradeRefusal())
        {
            player.sendMessage("Target is in trade refusal mode");
            return;
        }
        
		player.onTransactionRequest(partner);
		partner.sendPacket(new SendTradeRequest(player.getObjectId()));
		SystemMessage sm = new SystemMessage(SystemMessage.REQUEST_S1_FOR_TRADE);
		sm.addString(partner.getName());
		player.sendPacket(sm);
	} 
	
	public String getType()
	{
		return TRADEREQUEST__C__15;
	}
}
