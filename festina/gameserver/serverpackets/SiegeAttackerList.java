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
package com.festina.gameserver.serverpackets;

//import java.util.Calendar; //signed time related
//import java.util.logging.Logger;

import com.festina.gameserver.ClanTable;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.L2SiegeClan;
import com.festina.gameserver.model.entity.Castle;
/**
 * Populates the Siege Attacker List in the SiegeInfo Window<BR>
 * <BR>
 * packet type id 0xca<BR>
 * format: cddddddd + dSSdddSSd<BR>
 * <BR>
 * c = ca<BR>
 * d = CastleID<BR>
 * d = unknow (0x00)<BR>
 * d = unknow (0x01)<BR>
 * d = unknow (0x00)<BR>
 * d = Number of Attackers Clans?<BR>
 * d = Number of Attackers Clans<BR>
 * { //repeats<BR>
 * d = ClanID<BR>
 * S = ClanName<BR>
 * S = ClanLeaderName<BR>
 * d = ClanCrestID<BR>
 * d = signed time (seconds)<BR>
 * d = AllyID<BR>
 * S = AllyName<BR>
 * S = AllyLeaderName<BR>
 * d = AllyCrestID<BR>
 * 
 * @author KenM
 */
public class SiegeAttackerList extends ServerBasePacket
{
    private static final String _S__CA_SiegeAttackerList = "[S] ca SiegeAttackerList";
    //private static Logger _log = Logger.getLogger(SiegeAttackerList.class.getName());
    private Castle _Castle;
    
    public SiegeAttackerList(Castle castle)
    {
        _Castle = castle;   
    }

    final void runImpl()
    {
        // no long-running tasks
    }

    final void writeImpl()
    {
        writeC(0xca);
        writeD(_Castle.getCastleId());
        writeD(0x00); //0 
        writeD(0x01); //1
        writeD(0x00); //0
        int size = _Castle.getSiege().getAttackerClans().size();
        if (size > 0)
        {
            L2Clan clan;

            writeD(size);
            writeD(size);
            for(L2SiegeClan siegeclan : _Castle.getSiege().getAttackerClans())
            {
                clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
                if (clan == null) continue;

                writeD(clan.getClanId());
                writeS(clan.getName());
                writeS(clan.getLeaderName());
                writeD(clan.getCrestId());
                writeD(0x00); //signed time (seconds) (not storated by L2J)
                writeD(clan.getAllyId());
                writeS(clan.getAllyName());
                writeS(""); //AllyLeaderName
                writeD(clan.getAllyCrestId());
            }
        }
        else
        {
            writeD(0x00);
            writeD(0x00);
        }
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__CA_SiegeAttackerList;
    }

}
