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

import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.serverpackets.SystemMessage;


public class EffectCharge extends L2Effect
{
	int num_charges;
	
	public EffectCharge(Env env, EffectTemplate template)
	{
		super(env, template);
		num_charges = 1;
		env._target.updateEffectIcons();
        SystemMessage sm = new SystemMessage(614);
        sm.addString("Charged to 1");
        getEffected().sendPacket(sm);
	}

	public EffectType getEffectType()
	{
		return EffectType.CHARGE;
	}

	public boolean onActionTime()
    {
    	// ignore
    	return true;
    }

	public int getLevel() { return num_charges; }
	public void addNumCharges(int i) { num_charges = num_charges + i; }
}
