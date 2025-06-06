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
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
final class ConditionUsingItemType extends Condition {

	final int _mask;
	
	ConditionUsingItemType(int mask)
	{
		_mask = mask;
	}
	
	public boolean testImpl(Env env)
	{
		if (!(env._player instanceof L2PcInstance))
			return false;
		Inventory inv = ((L2PcInstance)env._player).getInventory();
		return (_mask & inv.getWearedMask()) != 0;
	}
}
