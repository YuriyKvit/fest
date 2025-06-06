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

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.PrivateStoreManageListSell;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestPrivateStoreManageSell extends ClientBasePacket
{
	private static final String _C__73_REQUESTPRIVATESTOREMANAGESELL = "[C] 73 RequestPrivateStoreManageSell";
	//private static Logger _log = Logger.getLogger(RequestPrivateStoreManage.class.getName());
	
	
	public RequestPrivateStoreManageSell(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
	}

	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null) return;
        
        // Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
        if (player.isAlikeDead())
        {
            sendPacket(new ActionFailed());
            return;
        }
        
        if (player.isInOlympiadMode())
        {
            sendPacket(new ActionFailed());
            return;
        }
        
        if (player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL +1)
        	player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);

        if (player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
        {
			if (player.isSitting()) player.standUp();
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_SELL +1);
			player.sendPacket(new PrivateStoreManageListSell(player));
        }
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__73_REQUESTPRIVATESTOREMANAGESELL;
	}
}
