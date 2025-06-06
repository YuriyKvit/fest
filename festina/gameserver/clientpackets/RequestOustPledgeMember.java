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

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.L2ClanMember;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.PledgeShowMemberListDelete;
import com.festina.gameserver.serverpackets.PledgeShowMemberListDeleteAll;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestOustPledgeMember extends ClientBasePacket
{
	private static final String _C__27_REQUESTOUSTPLEDGEMEMBER = "[C] 27 RequestOustPledgeMember";
	static Logger _log = Logger.getLogger(RequestOustPledgeMember.class.getName());

	private final String _target;
	
	public RequestOustPledgeMember(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_target  = readS();
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
	                //privilege check 
	                if (activeChar == null || !((activeChar.getClanPrivileges() & L2Clan.CP_CL_DISMISS) == L2Clan.CP_CL_DISMISS)) 
		{	
			return;
		}
		
		L2Clan clan = activeChar.getClan();
		if (clan == null) return;
		
		L2ClanMember member = clan.getClanMember(_target);
		if (member == null)
		{
			_log.warning("target is not member of the clan");
			return;
		}
		
        // this also updates the database
		clan.removeClanMember(_target);

		SystemMessage msg = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_EXPELLED);
		msg.addString(member.getName());
		clan.broadcastToOnlineMembers(msg);
		
		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(_target));
		
		if (member.isOnline())
		{
			L2PcInstance player = member.getPlayerInstance();
			player.setClan(null);
			player.setTitle("");
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_MEMBERSHIP_TERMINATED));
			player.setDeleteClanCurTime();
			
			player.broadcastUserInfo();
			
			// disable clan tab
			player.sendPacket(new PledgeShowMemberListDeleteAll());
		}
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__27_REQUESTOUSTPLEDGEMEMBER;
	}
}
