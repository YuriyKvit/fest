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

import com.festina.gameserver.model.L2ShortCut;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * ShortCutInit
 * format   d *(1dddd)/(2ddddd)/(3dddd)
 * 
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class ShortCutInit extends ServerBasePacket
{
	private static final String _S__57_SHORTCUTINIT = "[S] 45 ShortCutInit";
    
	private L2ShortCut[] _shortCuts;
    private L2PcInstance _activeChar;

	public ShortCutInit(L2PcInstance activeChar)
	{
        _activeChar = activeChar;
	}	
	
	final void runImpl()
	{
		if (_activeChar == null)
			return;
        
		_shortCuts = _activeChar.getAllShortCuts();
	}
	
	final void writeImpl()
	{
		writeC(0x45);
		writeD(_shortCuts.length);

		for (int i = 0; i < _shortCuts.length; i++)
		{
		    L2ShortCut sc = _shortCuts[i];

            writeD(sc.getType());	
		    writeD(sc.getSlot() + sc.getPage() * 12);
		    writeD(sc.getId());
            
		    if (sc.getLevel() > -1)
		        writeD(sc.getLevel());
            
		    writeD(1);
		}
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__57_SHORTCUTINIT;
	}
}
