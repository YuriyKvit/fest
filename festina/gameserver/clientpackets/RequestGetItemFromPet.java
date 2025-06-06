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
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;
import com.festina.gameserver.util.Util;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.4 $ $Date: 2005/03/29 23:15:33 $
 */
public class RequestGetItemFromPet extends ClientBasePacket
{
	private static final String REQUESTGETITEMFROMPET__C__8C = "[C] 8C RequestGetItemFromPet";
	private static Logger _log = Logger.getLogger(RequestGetItemFromPet.class.getName());

	private final int _objectId;
	private final int _amount;
	@SuppressWarnings("unused")
    private final int _unknown;
	
	public RequestGetItemFromPet(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_objectId = readD();
		_amount   = readD();
		_unknown  = readD();// = 0 for most trades
	}

	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar(); 
        if (player == null || player.getPet() == null || !(player.getPet() instanceof L2PetInstance)) return;
        L2PetInstance pet = (L2PetInstance)player.getPet(); 

        if(_amount < 0)
        {
        	Util.handleIllegalPlayerAction(player,"[RequestGetItemFromPet] count < 0! ban! oid: "+_objectId+" owner: "+player.getName(),Config.DEFAULT_PUNISH);
        	return;
        }
        else if(_amount == 0)
        	return;
        
		if (pet.transferItem("Transfer", _objectId, _amount, player.getInventory(), player, pet) == null)
			_log.warning("Invalid Item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
	}

	public String getType()
	{
		return REQUESTGETITEMFROMPET__C__8C;
	}
}
