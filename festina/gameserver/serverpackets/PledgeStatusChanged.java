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

import com.festina.gameserver.model.L2Clan;

/**
 * 
 *
 * sample
 * 0000: cd b0 98 a0 48 1e 01 00 00 00 00 00 00 00 00 00    ....H...........
 * 0010: 00 00 00 00 00                                     .....
 *  
 * format   ddddd 
 * 
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PledgeStatusChanged extends ServerBasePacket
{
	private static final String _S__CD_PLEDGESTATUS_CHANGED = "[S] CD PledgeStatusChanged";
	private L2Clan _clan;
	
	public PledgeStatusChanged(L2Clan clan)
	{
		_clan = clan;
	}	
	
	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0xcd);
		writeD(_clan.getLeaderId());
		writeD(_clan.getClanId());
        writeD(0);
        writeD(_clan.getLevel());
        writeD(0);
	}
	
	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__CD_PLEDGESTATUS_CHANGED;
	}
}
