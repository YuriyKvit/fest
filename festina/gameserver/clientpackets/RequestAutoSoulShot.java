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
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ExAutoSoulShot;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.0.0.0 $ $Date: 2005/07/11 15:29:30 $
 */
public class RequestAutoSoulShot extends ClientBasePacket
{
    private static final String _C__CF_REQUESTAUTOSOULSHOT = "[C] CF RequestAutoSoulShot";
    private static Logger _log = Logger.getLogger(RequestAutoSoulShot.class.getName());

    // format  cd
    private final int _itemId;
    private final int _type; // 1 = on : 0 = off;

    /**
     * packet type id 0xcf
     * format:		chdd
     * @param decrypt
     */
    public RequestAutoSoulShot(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _itemId = readD();
        _type = readD();
    }

    void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        
        if (activeChar == null) 
            return;

        if (activeChar.getPrivateStoreType() == 0 && 
                activeChar.getActiveRequester() == null &&
                !activeChar.isDead())
        {
            if (Config.DEBUG) 
                _log.fine("AutoSoulShot:" + _itemId);

            L2ItemInstance item = activeChar.getInventory().getItemByItemId(_itemId);
            
            if (item != null)
            {
                if (_type == 1)
                {

                    activeChar.addAutoSoulShot(_itemId);
                    ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
                    activeChar.sendPacket(atk);

                    //start the auto soulshot use
                    SystemMessage sm = new SystemMessage(SystemMessage.USE_OF_S1_WILL_BE_AUTO);
                    sm.addString(item.getItemName());
                    activeChar.sendPacket(sm);

                    // Attempt to charge first shot on activation
                    if (_itemId == 6645 || _itemId == 6646 || _itemId == 6647)
                        activeChar.rechargeAutoSoulShot(true, true, true);
                    else
                        activeChar.rechargeAutoSoulShot(true, true, false);
                }
                else if (_type == 0)
                {
                    activeChar.removeAutoSoulShot(_itemId);
                    ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
                    activeChar.sendPacket(atk);

                    //cancel the auto soulshot use
                    SystemMessage sm = new SystemMessage(SystemMessage.AUTO_USE_OF_S1_CANCELLED);
                    sm.addString(item.getItemName());
                    activeChar.sendPacket(sm);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__CF_REQUESTAUTOSOULSHOT;
    }
}