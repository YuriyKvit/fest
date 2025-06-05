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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.serverpackets.CharDeleteOk;
import com.festina.gameserver.serverpackets.CharSelectInfo;

/**
 * This class ...
 * 
 * @version $Revision: 1.8.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class CharacterDelete extends ClientBasePacket
{
	private static final String _C__0C_CHARACTERDELETE = "[C] 0C CharacterDelete";
	private static Logger _log = Logger.getLogger(CharacterDelete.class.getName());

	// cd
	private final int _charSlot;

	/**
	 * @param decrypt
	 */
	public CharacterDelete(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_charSlot = readD();
	}

	void runImpl()
	{
		if (Config.DEBUG) _log.fine("deleting slot:" + _charSlot);

		try
		{
		    if (Config.DELETE_DAYS == 0)
		    	getClient().deleteChar(_charSlot);
		    else
		    	getClient().markToDeleteChar(_charSlot);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error:", e);
		}
		
		CharDeleteOk cco = new CharDeleteOk();
		sendPacket(cco);

		CharSelectInfo cl = new CharSelectInfo(getClient().getLoginName(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__0C_CHARACTERDELETE;
	}
}
