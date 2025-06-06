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
package com.festina.gameserver.ai;

import com.festina.gameserver.ThreadPoolManager;
import com.festina.gameserver.model.L2CharPosition;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.actor.instance.L2DoorInstance;
import com.festina.gameserver.model.actor.instance.L2SiegeGuardInstance;

/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class L2DoorAI extends L2CharacterAI {

	public L2DoorAI(L2DoorInstance.AIAccessor accessor)
	{
		super(accessor);
	}
	
	// rather stupid AI... well,  it's for doors :D
	protected void onIntentionIdle() {}
	protected void onIntentionActive() {}
	protected void onIntentionRest() {}
    @SuppressWarnings("unused")
	protected void onIntentionAttack(L2Character target) {}
    @SuppressWarnings("unused")
	protected void onIntentionCast(L2Skill skill, L2Object target) {}
    @SuppressWarnings("unused")
	protected void onIntentionMoveTo(L2CharPosition destination) {}
    @SuppressWarnings("unused")
	protected void onIntentionFollow(L2Character target) {}
    @SuppressWarnings("unused")
	protected void onIntentionPickUp(L2Object item) {}
    @SuppressWarnings("unused")
	protected void onIntentionInteract(L2Object object) {}

	protected void onEvtThink() {}
    @SuppressWarnings("unused")
	protected void onEvtAttacked(L2Character attacker)
    {
    	L2DoorInstance me = (L2DoorInstance)_actor;
        ThreadPoolManager.getInstance().executeTask(new onEventAttackedDoorTask(me, attacker));
	}
    @SuppressWarnings("unused") 
	protected void onEvtAggression(L2Character target, int aggro) {}
    @SuppressWarnings("unused") 
	protected void onEvtStunned(L2Character attacker) {}
    @SuppressWarnings("unused")
	protected void onEvtSleeping(L2Character attacker) {}
    @SuppressWarnings("unused")
	protected void onEvtRooted(L2Character attacker) {}
	protected void onEvtReadyToAct() {}
    @SuppressWarnings("unused")
	protected void onEvtUserCmd(Object arg0, Object arg1) {}
	protected void onEvtArrived() {}
	protected void onEvtArrivedRevalidate() {}
    @SuppressWarnings("unused")
	protected void onEvtArrivedBlocked(L2CharPosition blocked_at_pos) {}
    @SuppressWarnings("unused")
	protected void onEvtForgetObject(L2Object object) {}
	protected void onEvtCancel() {}
	protected void onEvtDead() {}
	
	private class onEventAttackedDoorTask implements Runnable
	{
		private L2DoorInstance _door;
		private L2Character _attacker;

		public onEventAttackedDoorTask(L2DoorInstance door, L2Character attacker)
		{
			_door = door;
			_attacker = attacker;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			_door.getKnownList().updateKnownObjects();
	        
			for (L2SiegeGuardInstance guard : _door.getKnownSiegeGuards()) {
	            if (_actor.isInsideRadius(guard, guard.getFactionRange(), false, true) 
	                    && Math.abs(_attacker.getZ()-guard.getZ()) < 200)
	            {
	                guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 15);
	            }
			}		
		}
	}

}
