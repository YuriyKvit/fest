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

import javolution.util.FastList;

public class ExEnchantSkillInfo extends ServerBasePacket
{
    private static final String _S__FE_18_EXENCHANTSKILLINFO = "[S] FE:18 ExEnchantSkillInfo";
    private FastList<Req> _reqs;
    private int _id;
    private int _level;
    private int _spCost;
    private int _xpCost;
    private int _rate;

    class Req
    {
        public int id;
        public int count;
        public int type;
        public int unk;

        Req(int pType, int pId, int pCount, int pUnk)
        {
            this.id = pId;
            this.type = pType;
            this.count = pCount;
            this.unk = pUnk;
        }
    }

    public ExEnchantSkillInfo(int id, int level, int spCost, int xpCost, int rate)
    {
        _reqs = new FastList<Req>();
        _id = id;
        _level = level;
        _spCost = spCost;
        _xpCost = xpCost;
        _rate = rate;
    }

    public void addRequirement(int type, int id, int count, int unk)
    {
        _reqs.add(new Req(type, id, count, unk));
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.serverpackets.ServerBasePacket#runImpl()
     */
    @Override
    void runImpl()
    {
        // --
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    @Override
    void writeImpl()
    {
        writeC(0xfe);
        writeH(0x18);

        writeD(_id);
        writeD(_level);
        writeD(_spCost);
        writeD(_xpCost);
        writeD(_rate);

        writeD(_reqs.size());

        for (Req temp : _reqs)
        {
            writeD(temp.type);
            writeD(temp.id);
            writeD(temp.count);
            writeD(temp.unk);
        }

    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _S__FE_18_EXENCHANTSKILLINFO;
    }

}