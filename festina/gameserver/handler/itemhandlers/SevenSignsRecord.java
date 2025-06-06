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
package com.festina.gameserver.handler.itemhandlers;

import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.SSQStatus;

/**
 * Item Handler for Seven Signs Record
 * 
 * @author Tempy
 */
public class SevenSignsRecord implements IItemHandler {
private static int[] _itemIds = {5707};
	
	public void useItem(L2PlayableInstance playable, @SuppressWarnings("unused") L2ItemInstance item)
	{
		L2PcInstance activeChar;
		
		if (playable instanceof L2PcInstance)
			activeChar = (L2PcInstance)playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance)playable).getOwner();
		else
			return;
		
		SSQStatus ssqs = new SSQStatus(activeChar, 1);
		activeChar.sendPacket(ssqs);
	}
		
	public int[] getItemIds()
	{
       return _itemIds;
	}
}