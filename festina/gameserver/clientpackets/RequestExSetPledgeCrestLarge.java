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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.festina.L2DatabaseFactory;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.cache.CrestCache;
import com.festina.gameserver.idfactory.BitSetIDFactory;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * Format : chdb
 * c (id) 0xD0
 * h (subid) 0x11
 * d data size
 * b raw data (picture i think ;) )
 * @author -Wooden-
 *
 */
public class RequestExSetPledgeCrestLarge extends ClientBasePacket
{
	private static final String _C__D0_11_REQUESTEXSETPLEDGECRESTLARGE = "[C] D0:11 RequestExSetPledgeCrestLarge";
	static Logger _log = Logger.getLogger(RequestExSetPledgeCrestLarge.class.getName());
	private int _size;
	private byte[] _data;

	/**
	 * @param buf
	 * @param client
	 */
	public RequestExSetPledgeCrestLarge(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_size = readD();
		if(_size > 0) // client CAN send a RequestExSetPledgeCrestLarge with the size set to 0 then format is just chd
		{
			_data = readB(_size);
		}
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null) return;
		
		L2Clan clan = activeChar.getClan();
		if (clan == null) return;
		
		if (_data == null)
		{
			CrestCache.getInstance().removePledgeCrestLarge(clan.getCrestId());
			
            clan.setHasCrestLarge(false);
            activeChar.sendMessage("The insignia has been removed.");
            
            for (L2PcInstance member : clan.getOnlineMembers(""))
                member.broadcastUserInfo();
            
            return;
		}
		
		if (_data.length > 2176)
        {
        	activeChar.sendMessage("The insignia file size is greater than 2176 bytes.");
        	return;
        }
		
		if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{	
			if(clan.getHasCastle() == 0 && clan.getHasHideout() == 0)
			{
				activeChar.sendMessage("Only a clan that owns a clan hall or a castle can get their emblem displayed on clan related items"); //there is a system message for that but didnt found the id
				return;
			}
			
			CrestCache crestCache = CrestCache.getInstance();
            
			int newId = BitSetIDFactory.getInstance().getNextId();
            
            if (!crestCache.savePledgeCrestLarge(newId,_data))
            {
                _log.log(Level.INFO, "Error loading large crest of clan:" + clan.getName());
                return;
            }
            
            if (clan.hasCrestLarge())
            {
                crestCache.removePledgeCrestLarge(clan.getCrestLargeId());
            }
            
            java.sql.Connection con = null;
            
            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
                statement.setInt(1, newId);
                statement.setInt(2, clan.getClanId());
                statement.executeUpdate();
                statement.close();
            }
            catch (SQLException e)
            {
                _log.warning("could not update the large crest id:"+e.getMessage());
            }
            finally
            {
                try { con.close(); } catch (Exception e) {}
            }
            
            clan.setCrestLargeId(newId);
            clan.setHasCrestLarge(true);
            
            activeChar.sendPacket(new SystemMessage(SystemMessage.CLAN_EMBLEM_WAS_SUCCESSFULLY_REGISTERED));
            
            for (L2PcInstance member : clan.getOnlineMembers(""))
                member.broadcastUserInfo();
            
		}
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_11_REQUESTEXSETPLEDGECRESTLARGE;
	}
	
}