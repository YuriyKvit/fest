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

import java.util.logging.Logger;

import com.festina.gameserver.Olympiad;
import com.festina.gameserver.model.actor.instance.L2PcInstance;


/**
 * This class ...
 * 
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 * 
 * @author godson
 */
public class ExOlympiadUserInfo extends ServerBasePacket
{
    private static final String _S__FE_29_OLYMPIADUSERINFO = "[S] FE:29 OlympiadUserInfo";
    private int _side;
    private L2PcInstance _player;


    /**
     * @param _player
     * @param _side (1 = right, 2 = left)
     */

    public ExOlympiadUserInfo(L2PcInstance player)
    {
            _player = player;
    }


    @Override
    protected final void writeImpl()
    {
            writeC(0xfe);
            writeH(0x29);
            writeC(1);
            writeD(_player.getObjectId());
            writeS(_player.getName());
            writeD(_player.getClassId().getId());
            writeD((int)_player.getCurrentHp());
            writeD(_player.getMaxHp());
            writeD((int)_player.getCurrentCp());
            writeD(_player.getMaxCp());
    }

    /* (non-Javadoc)
     * @see ru.dw.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
    public String getType()
    {
            return _S__FE_29_OLYMPIADUSERINFO;
    }


	@Override
	void runImpl() {
		// TODO Auto-generated method stub
		
	}
}