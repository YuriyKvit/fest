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

import javolution.util.FastList;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.instancemanager.CastleManorManager;
import com.festina.gameserver.instancemanager.CastleManorManager.SeedProduction;

/**
 * Format: (ch) dd [ddd]
 * @author Zombie_Killer
 *
 */
public class RequestSetSeed extends ClientBasePacket
{
	private static final String _C__D0_0A_REQUESTSETSEED = "[C] D0:0A RequestSetSeed";
	private int _size; 

	private int _manorId; 

	private int[] _items; // _size*3
	
	/**
	 * @param buf
	 * @param client
	 */
	public RequestSetSeed(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
        _manorId = readD();
        _size = readD();
        if (_size * 12 > _buf.remaining() || _size > 500)
        {
            _size = 0;
            return;
        }
        _items = new int[_size * 3];
        for (int i = 0; i < _size; i++)
        {
            int itemId = readD();
            _items[i * 3 + 0] = itemId;
            int sales = readD();
            _items[i * 3 + 1] = sales;
            int price = readD();
            _items[i * 3 + 2] = price;
        }
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	void runImpl()
	{
	    if (_size < 1)
            return;

        FastList<SeedProduction> seeds = new FastList<SeedProduction>();
        for (int i = 0; i < _size; i++)
        {
            int id = _items[i * 3 + 0];
            int sales = _items[i * 3 + 1];
            int price = _items[i * 3 + 2];
            if (id > 0)
            {
                SeedProduction s = CastleManorManager.getInstance()
                        .getNewSeedProduction(id, sales, price, sales);
                seeds.add(s);
            }
        }

        CastleManager.getInstance().getCastleById(_manorId).setSeedProduction(seeds, CastleManorManager.PERIOD_NEXT);
        if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
            CastleManager.getInstance().getCastleById(_manorId).saveSeedData(CastleManorManager.PERIOD_NEXT);

	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_0A_REQUESTSETSEED;
	}

}
