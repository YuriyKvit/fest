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

/**
 * This class ...
 * 
 * @version $Revision: $ $Date: $
 * @author  Luca Baldi
 */
public class ExQuestInfo extends ServerBasePacket
{
    private static final String _S__FE_19_EXQUESTINFO = "[S] FE:19 EXQUESTINFO";
    
    /* (non-Javadoc)
     * @see com.festina.gameserver.serverpackets.ServerBasePacket#runImpl()
     */
    @Override
    void runImpl()
    {
        // no long running
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    @Override
    void writeImpl()
    {
        writeC(0xfe);
        writeH(0x19);       
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _S__FE_19_EXQUESTINFO;
    }
}
