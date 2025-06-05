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
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.JoinPledge;
import com.festina.gameserver.serverpackets.PledgeShowInfoUpdate;
import com.festina.gameserver.serverpackets.PledgeShowMemberListAdd;
import com.festina.gameserver.serverpackets.PledgeShowMemberListAll;
import com.festina.gameserver.serverpackets.PledgeStatusChanged;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestAnswerJoinPledge extends ClientBasePacket
{
	private static final String _C__25_REQUESTANSWERJOINPLEDGE = "[C] 25 RequestAnswerJoinPledge";
	//private static Logger _log = Logger.getLogger(RequestAnswerJoinPledge.class.getName());
	
	private final int _answer;
			
	public RequestAnswerJoinPledge(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_answer  = readD();
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
        
		if (activeChar == null)
		    return;
        
		L2PcInstance requestor = activeChar.getActiveRequester();
        
        if (requestor == null)
            return;
		
		if (_answer == 1)
		{
		    if (activeChar.canJoinClan())
		    {
		        //not used ?_?
		        JoinPledge jp = new JoinPledge(requestor.getClanId());
		        activeChar.sendPacket(jp);
		        
		        L2Clan clan = requestor.getClan();
		        
//		      L2ClanMember[] members = clan.getMembers();
		        PledgeShowMemberListAdd la = new PledgeShowMemberListAdd(activeChar);
		        clan.broadcastToOnlineMembers(la);
		        
		        
		        // this also updates the database
		        clan.addClanMember(activeChar);
		        activeChar.setClan(clan);
		        activeChar.setClanPrivileges(0);
		        
		        //should be update packet only
		        activeChar.sendPacket(new PledgeShowInfoUpdate(clan, activeChar));
		        activeChar.broadcastUserInfo();
		        requestor.broadcastUserInfo();
		        requestor.sendPacket(new PledgeShowInfoUpdate(clan, activeChar));
		        
		        SystemMessage sm = new SystemMessage(SystemMessage.ENTERED_THE_CLAN);
		        clan.broadcastToOnlineMembers(sm);
		        activeChar.sendPacket(sm);
		        
		        
		        sm = new SystemMessage(SystemMessage.S1_HAS_JOINED_CLAN);
		        sm.addString(activeChar.getName());

		        
	              clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(activeChar), activeChar);
	                clan.broadcastToOtherOnlineMembers(sm, activeChar);
		        clan.broadcastToOnlineMembers(sm);
		        requestor.sendPacket(sm);
		        requestor.broadcastUserInfo();
		        requestor.sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
		        requestor.sendPacket(new PledgeStatusChanged(activeChar.getClan()));

		        
		        // this activates the clan tab on the new member
		        activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
		        activeChar.setDeleteClanTime(0);
		    } 
            else 
            {
		        requestor.sendPacket(new SystemMessage(231));
		        activeChar.sendPacket(new SystemMessage(232));
		    }
		} 
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessage.S1_REFUSED_TO_JOIN_CLAN);
			sm.addString(activeChar.getName());
			requestor.sendPacket(sm);
		}
		
		activeChar.setActiveRequester(null);
		requestor.onTransactionResponse();
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__25_REQUESTANSWERJOINPLEDGE;
	}
}
