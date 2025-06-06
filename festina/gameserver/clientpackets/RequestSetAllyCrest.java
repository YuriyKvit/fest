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
import com.festina.gameserver.ClanTable;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.cache.CrestCache;
import com.festina.gameserver.idfactory.BitSetIDFactory;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestSetAllyCrest extends ClientBasePacket
{
    private static final String _C__87_REQUESTSETALLYCREST = "[C] 87 RequestSetAllyCrest";
    static Logger _log = Logger.getLogger(RequestSetAllyCrest.class.getName());
            
    private final int _length;
    private final byte[] _data;
    
    public RequestSetAllyCrest(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _length  = readD();
        _data = readB(_length);
    }

    void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
        	return;
        
        if (_data.length > 192)
        {
        	activeChar.sendMessage("Its more than 192 bytes");
        	return;
        }
        
        if (activeChar.getAllyId() != 0)
        {   
            L2Clan leaderclan = ClanTable.getInstance().getClan(activeChar.getAllyId());
            
            if (activeChar.getClanId() != leaderclan.getClanId() || !activeChar.isClanLeader())
            {   
                return;
            }
            
            CrestCache crestCache = CrestCache.getInstance();
            
            int newId = BitSetIDFactory.getInstance().getNextId();
            
            if (!crestCache.saveAllyCrest(newId,_data))
            {
                _log.log(Level.INFO, "Error loading crest of ally:" + leaderclan.getAllyName());
                return;
            }
            
            if (leaderclan.getAllyCrestId() != 0)
            {
                crestCache.removeAllyCrest(leaderclan.getAllyCrestId());
            }
            
            java.sql.Connection con = null;
            
            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?");
                statement.setInt(1, newId);
                statement.setInt(2, leaderclan.getAllyId());
                statement.executeUpdate();
                statement.close();
            }
            catch (SQLException e)
            {
                _log.warning("could not update the ally crest id:"+e.getMessage());
            }
            finally
            {
                try { con.close(); } catch (Exception e) {}
            }
            
            
            for (L2Clan clan : ClanTable.getInstance().getClans())
            {
                if (clan.getAllyId() == activeChar.getAllyId())
                {
                    clan.setAllyCrestId(newId);
                    for (L2PcInstance member : clan.getOnlineMembers(""))
                        member.broadcastUserInfo();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__87_REQUESTSETALLYCREST;
    }
}
