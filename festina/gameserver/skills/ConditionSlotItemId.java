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
package com.festina.gameserver.skills;

import com.festina.gameserver.model.Inventory;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;


/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
final class ConditionSlotItemId extends ConditionInventory {

	final int _itemId;
	final int _enchantLevel;
	
	ConditionSlotItemId(int slot, int itemId, int enchantLevel)
	{
		super(slot);
		_itemId = itemId;
		_enchantLevel = enchantLevel;
	}
	
	public boolean testImpl(Env env)
	{
		if (!(env._player instanceof L2PcInstance))
			return false;
		Inventory inv = ((L2PcInstance)env._player).getInventory();
		L2ItemInstance item = inv.getPaperdollItem(_slot);
		if (item == null)
			return _itemId == 0;
		return item.getItemId() == _itemId && item.getEnchantLevel() >= _enchantLevel;
	}
}
