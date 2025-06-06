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

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.serverpackets.ItemList;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestItemList extends ClientBasePacket
{
	private static final String _C__0F_REQUESTITEMLIST = "[C] 0F RequestItemList";
	/**
	 * packet type id 0x0f
	 * format:		c
	 * @param rawPacket
	 */
	public RequestItemList(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
	}

	void runImpl()
	{
        if (getClient() != null && getClient().getActiveChar() != null && !getClient().getActiveChar().isInvetoryDisabled())
        {
    		ItemList il = new ItemList(getClient().getActiveChar(), true);
    		sendPacket(il);
        }
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__0F_REQUESTITEMLIST;
	}
}
