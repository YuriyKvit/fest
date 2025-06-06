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
import com.festina.gameserver.model.L2Party;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.AskJoinParty;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 *  sample
 *  29 
 *  42 00 00 10 
 *  01 00 00 00
 * 
 *  format  cdd
 * 
 * 
 * @version $Revision: 1.7.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestJoinParty extends ClientBasePacket
{
	private static final String _C__29_REQUESTJOINPARTY = "[C] 29 RequestJoinParty";
	private static Logger _log = Logger.getLogger(RequestJoinParty.class.getName());
	
	private final String _name;
	private final int _itemDistribution;

    public RequestJoinParty(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);

        _name = readS();
        _itemDistribution = readD();
	}

	void runImpl()
	{
        L2PcInstance requestor = getClient().getActiveChar();
        L2PcInstance target = L2World.getInstance().getPlayer(_name);
        
		if (requestor == null)
		    return;
        
        if (target == null)
        {
            requestor.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
            return;
        }
        
        SystemMessage msg;
        
		if (target.isInParty()) 
        {
			msg = new SystemMessage(SystemMessage.S1_IS_ALREADY_IN_PARTY);
			msg.addString(target.getName());
			requestor.sendPacket(msg);
			return;
		}

		if (target == requestor) 
        {
			msg = new SystemMessage(SystemMessage.INCORRECT_TARGET);
			msg.addString(target.getName());
			requestor.sendPacket(msg);
			return;
		}
        
        if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
            return;
		
		if (!requestor.isInParty())
            //asker has no party
			createNewParty(target, requestor);
		else if (requestor.getParty().isInDimensionalRift()) 
		{
			requestor.sendMessage("You can't invite a player when in Dimensional Rift.");
		}
		else
            //asker has a party
			addTargetToParty(target, requestor);
	}
	
	/**
	 * @param client
	 * @param itemDistribution
	 * @param target
	 * @param requestor
	 */
	private void addTargetToParty(L2PcInstance target, L2PcInstance requestor)
	{
		SystemMessage msg;

       // summary of ppl already in party and ppl that get invitation
        if (requestor.getParty().getMemberCount() + requestor.getParty().getPendingInvitationNumber() >= 9 ) 
        {
			requestor.sendPacket(new SystemMessage(SystemMessage.PARTY_FULL));
			return;
		}
		
		if (!requestor.getParty().isLeader(requestor)) 
        {
			requestor.sendPacket(new SystemMessage(SystemMessage.ONLY_LEADER_CAN_INVITE));
			return;
		}
		
		if (!target.isProcessingRequest()) 
		{
		    requestor.onTransactionRequest(target);
		    target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
		    requestor.getParty().increasePendingInvitationNumber();
		    
		    if (Config.DEBUG) 
		        _log.fine("sent out a party invitation to:"+target.getName());
		    
		    msg = new SystemMessage(SystemMessage.YOU_INVITED_S1_TO_PARTY);
		    msg.addString(target.getName());
		    requestor.sendPacket(msg);
		}
		else
		{
		    msg = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
		    requestor.sendPacket(msg);
		    
		    if (Config.DEBUG)
		        _log.warning(requestor.getName() + " already received a party invitation");
		}
	}


	/**
	 * @param client
	 * @param itemDistribution
	 * @param target
	 * @param requestor
	 */
	private void createNewParty(L2PcInstance target, L2PcInstance requestor)
	{
		SystemMessage msg;
        
		if (!target.isProcessingRequest())
		{
		    requestor.setParty(new L2Party(requestor, _itemDistribution));
		    
		    requestor.onTransactionRequest(target);
		    target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
		    requestor.getParty().increasePendingInvitationNumber();
		    
		    if (Config.DEBUG)
		        _log.fine("sent out a party invitation to:"+target.getName());
		    
		    msg = new SystemMessage(SystemMessage.YOU_INVITED_S1_TO_PARTY);
		    msg.addString(target.getName());
		    requestor.sendPacket(msg);
		}
		else
		{
		    msg = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
		    msg.addString(target.getName());
		    requestor.sendPacket(msg);
		    
		    if (Config.DEBUG)
		        _log.warning(requestor.getName() + " already received a party invitation");
		}
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__29_REQUESTJOINPARTY;
	}
}
