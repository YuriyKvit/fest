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

import javolution.util.FastList;

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ExSendManorList;

/**
 * Format: ch
 * c (id) 0xD0
 * h (subid) 0x08
 * @author Zombie_Killer
 *
 */
public class RequestManorList extends ClientBasePacket
{
	private static final String _C__FE_08_REQUESTMANORLIST = "[S] FE:08 RequestManorList";
	public RequestManorList(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
	}
	@Override
	void runImpl()
	{
	    L2PcInstance player = getClient().getActiveChar(); 
	    FastList<String> manorsName = new FastList<String>(); 
	    manorsName.add("gludio"); 
	    manorsName.add("dion"); 
	    manorsName.add("giran"); 
	    manorsName.add("oren"); 
	    manorsName.add("aden"); 
	    manorsName.add("innadril"); 
	    manorsName.add("goddard"); 
	    manorsName.add("rune"); 
	    ExSendManorList manorlist = new ExSendManorList(manorsName); 
	    player.sendPacket(manorlist); 
		
	}
	@Override
	public String getType()
	{
		return _C__FE_08_REQUESTMANORLIST;
	}
}