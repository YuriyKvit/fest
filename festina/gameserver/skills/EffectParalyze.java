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

final class EffectParalyze extends L2Effect {

	public EffectParalyze(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public EffectType getEffectType()
	{
		return EffectType.PARALYZE;
	}
	
	public void onStart() {
		 getEffected().startAbnormalEffect((short)0x0800);
       	 getEffected().setIsParalyzed(true);
	}
	
	public void onExit() {
		 getEffected().stopAbnormalEffect((short)0x0800);
		 getEffected().setIsParalyzed(false);
	}
	
    public boolean onActionTime()
    {
    	return false;
    }
}