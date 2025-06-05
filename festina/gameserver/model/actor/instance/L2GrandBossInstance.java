/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.festina.gameserver.model.actor.instance;

import com.festina.gameserver.instancemanager.GrandBossManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2NpcTemplate;

/**
 * This class manages all Grand Bosses.
 *
 * @version $Revision: 1.0.0.0 $ $Date: 2006/06/16 $
 */
public final class L2GrandBossInstance extends L2MonsterInstance
{
    private static final int BOSS_MAINTENANCE_INTERVAL = 10000;

     /**
     * Constructor for L2GrandBossInstance. This represent all grandbosses.
     * 
     * @param objectId ID of the instance
     * @param template L2NpcTemplate of the instance
     */
	public L2GrandBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}


    @Override
	protected int getMaintenanceInterval() 
    { 
        return BOSS_MAINTENANCE_INTERVAL; 
    }


    @Override
	public void OnSpawn()
    {
    	super.OnSpawn();
        GrandBossManager.getInstance().addBoss(this);
    }

    /**
     * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.<BR><BR>
     *
     */

    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
        super.reduceCurrentHp(damage, attacker, awake);
    }


    @Override
	public boolean isRaid()
    {
        return true;
    }
    
    protected boolean _isInSocialAction = false;

    public boolean IsInSocialAction()
    {
        return _isInSocialAction;
    }

    public void setIsInSocialAction(boolean value)
    {
        _isInSocialAction = value;
    }
    public void doDie(L2Character killer) 
    {
    	if ((killer instanceof L2PlayableInstance))
    	{
    		SystemMessage msg = new SystemMessage(1209);
    		broadcastPacket(msg);
    	}
    	super.doDie(killer);
    }
}