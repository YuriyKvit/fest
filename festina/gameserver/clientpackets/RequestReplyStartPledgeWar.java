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

import com.festina.gameserver.ClanTable;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestReplyStartPledgeWar extends ClientBasePacket
{
    private static final String _C__4e_REQUESTREPLYSTARTPLEDGEWAR = "[C] 4e RequestReplyStartPledgeWar";
	//private static Logger _log = Logger.getLogger(RequestReplyStartPledgeWar.class.getName());
    
    private final int _answer;
            
    public RequestReplyStartPledgeWar(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        @SuppressWarnings("unused") String _reqName = readS();
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
            ClanTable.getInstance().storeclanswars(requestor.getClanId(), activeChar.getClanId());
        } 
        else
        {
            requestor.sendPacket(new SystemMessage(SystemMessage.WAR_PROCLAMATION_HAS_BEEN_REFUSED));
        }
        activeChar.setActiveRequester(null);
        requestor.onTransactionResponse();
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__4e_REQUESTREPLYSTARTPLEDGEWAR;
    }
}