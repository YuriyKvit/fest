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
package com.festina.gameserver.model.actor.instance;

import com.festina.gameserver.model.L2Attackable;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.actor.knownlist.FriendlyMobKnownList;
import com.festina.gameserver.templates.L2NpcTemplate;

/**
 * This class represents Friendly Mobs lying over the world.
 * These friendly mobs should ony attack players with karma > 0
 * and it is always aggro, since it just attacks players with karma
 * 
 * @version $Revision: 1.20.4.6 $ $Date: 2005/07/23 16:13:39 $
 */
public class L2FriendlyMobInstance extends L2Attackable {

	public L2FriendlyMobInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
        super.setKnownList(new FriendlyMobKnownList(new L2FriendlyMobInstance[] {this}));
	}	

    public final FriendlyMobKnownList getKnownList() { return (FriendlyMobKnownList)super.getKnownList(); }

	public boolean isAutoAttackable(L2Character attacker) {
		if (attacker instanceof L2PcInstance)
			return ((L2PcInstance)attacker).getKarma() > 0;
		return false;
	}
	
	public boolean isAggressive()
	{
		return true;
	}

	public boolean hasRandomAnimation()
	{
		return false;
	}

}
