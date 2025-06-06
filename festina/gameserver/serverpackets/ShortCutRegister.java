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
/**
 * 
 *
 * sample
 *  
 * 56 
 * 01000000 04000000 dd9fb640 01000000
 * 
 * 56 
 * 02000000 07000000 38000000 03000000 01000000
 * 
 * 56 
 * 03000000 00000000 02000000 01000000
 * 
 * format   dd d/dd/d d
 * 
 * 
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class ShortCutRegister extends ServerBasePacket
{
	private static final String _S__56_SHORTCUTREGISTER = "[S] 44 ShortCutRegister";
	
    private L2ShortCut _shortcut;

	/**
	 * Register new skill shortcut
	 * @param slot
	 * @param type
	 * @param typeId
	 * @param level
	 * @param dat2
	 */
	public ShortCutRegister(L2ShortCut shortcut)
	{
		_shortcut = shortcut;
	}	
	
	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0x44);
        
		writeD(_shortcut.getType());
		writeD(_shortcut.getSlot() + _shortcut.getPage() * 12); // C4 Client
		writeD(_shortcut.getId());
        
		if (_shortcut.getLevel() > -1)
			writeD(_shortcut.getLevel());
        
		writeD(1);			
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__56_SHORTCUTREGISTER;
	}
}
