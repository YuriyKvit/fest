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
import com.festina.gameserver.serverpackets.JoinParty;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 *  sample
 *  2a 
 *  01 00 00 00
 * 
 *  format  cdd
 * 
 * 
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestAnswerJoinParty extends ClientBasePacket
{
	private static final String _C__2A_REQUESTANSWERPARTY = "[C] 2A RequestAnswerJoinParty";
	//private static Logger _log = Logger.getLogger(RequestAnswerJoinParty.class.getName());
	
	private final int _response;
	
	public RequestAnswerJoinParty(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_response = readD();
	}

	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
        if(player != null)
        {
    		L2PcInstance requestor = player.getActiveRequester();
		if (requestor == null)
		    return;
    		
    		JoinParty join = new JoinParty(_response);
    		requestor.sendPacket(join);	
    			
    		if (_response == 1) 
            {
    			player.joinParty(requestor.getParty());
    		} else
            {
    			SystemMessage msg = new SystemMessage(SystemMessage.PLAYER_DECLINED);
    			requestor.sendPacket(msg);
                
    			//activate garbage collection if there are no other members in party (happens when we were creating new one) 
    			if (requestor.getParty() != null && requestor.getParty().getMemberCount() == 1) requestor.setParty(null);
    		}
    		if (requestor.getParty() != null)
    			requestor.getParty().decreasePendingInvitationNumber(); // if party is null, there is no need of decreasing
            
    		player.setActiveRequester(null);
    		requestor.onTransactionResponse();
        }
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__2A_REQUESTANSWERPARTY;
	}
}
