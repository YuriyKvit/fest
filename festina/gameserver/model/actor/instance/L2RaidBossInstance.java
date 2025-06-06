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

import com.festina.gameserver.NpcTable;
import com.festina.gameserver.idfactory.IdFactory;
import com.festina.gameserver.instancemanager.RaidBossSpawnManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Spawn;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2NpcTemplate;
import com.festina.gameserver.ThreadPoolManager;
import com.festina.util.Rnd;

/**
 * This class manages all RaidBoss. 
 * In a group mob, there are one master called RaidBoss and several slaves called Minions.
 * 
 * @version $Revision: 1.20.4.6 $ $Date: 2005/04/06 16:13:39 $
 */
public final class L2RaidBossInstance extends L2MonsterInstance
{
	//protected static Logger _log = Logger.getLogger(L2RaidBossInstance.class.getName());
    
	private static final int RAIDBOSS_MAINTENANCE_INTERVAL = 420000; // 7 min
    
    private RaidBossSpawnManager.StatusEnum _raidStatus;

	/**
	 * Constructor of L2RaidBossInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 *  
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2RaidBossInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2RaidBossInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 * 
	 * @param objectId Identifier of the object to initialized
	 * @param L2NpcTemplate Template to apply to the NPC
	 */
	public L2RaidBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
    
    public boolean isRaid()
    {
        return true; 
    }

    protected int getMaintenanceInterval() { return RAIDBOSS_MAINTENANCE_INTERVAL; }
    /**
     * Spawn all minions at a regular interval
     * Also if boss is too far from home location at the time of this check, teleport it home
     *
     */

    protected void manageMinions()
    {
        minionList.spawnMinions();
        _minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() {
    
			public void run()
            {
                // teleport raid boss home if it's too far from home location
                L2Spawn bossSpawn = getSpawn();
                if(!isInsideRadius(bossSpawn.getLocx(),bossSpawn.getLocy(),bossSpawn.getLocz(), 5000, true, false))
                {
                    teleToLocation(bossSpawn.getLocx(),bossSpawn.getLocy(),bossSpawn.getLocz());
                    healFull(); // prevents minor exploiting with it
                }
                minionList.maintainMinions();
            }
        }, 60000, getMaintenanceInterval()+Rnd.get(5000));
    }
    public void doDie(L2Character killer)
    {
        if(killer instanceof L2PlayableInstance)
        {
        	SystemMessage msg = new SystemMessage(1209);
        	broadcastPacket(msg);
        }
        
        RaidBossSpawnManager.getInstance().updateStatus(this, true);
        for(L2MinionInstance minion : minionList.getSpawnedMinions())
        {
        	minion.deleteMe();
        }
        if (getNpcId() == 10035 || getNpcId() == 10054 || getNpcId() == 10126
                || getNpcId() == 10220)
        {
            int boxId = 0;
            switch(getNpcId())
            {
                case 10035: // Shilen�s Messenger Cabrio
                    boxId = 8027;
                    break;
                case 10054: // Demon Kernon
                    boxId = 8028;
                    break;
                case 10126: // Golkonda, the Longhorn General
                    boxId = 8029;
                    break;
                case 10220: // Death Lord Hallate
                    boxId = 8030;
                    break;
            }
            
            L2NpcTemplate boxTemplate = NpcTable.getInstance().getTemplate(boxId);
            final L2NpcInstance box = new L2NpcInstance(IdFactory.getInstance().getNextId(), boxTemplate);
            box.spawnMe(this.getX(), this.getY(), this.getZ());
            box.doRevive();
            
            ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
                public void run()
                {
                    box.deleteMe();
                }
            }, 60000);
        }
        
        super.doDie(killer);
        
    }
    
    public void onSpawn()
    {
    	RaidBossSpawnManager.getInstance().updateStatus(this, false);
    	super.OnSpawn();
        if (getNpcId() == 10286 || getNpcId() == 10283)
            return;
        else
            getSpawn().stopRespawn();
    }
    
    public void setRaidStatus (RaidBossSpawnManager.StatusEnum status)
    {
    	_raidStatus = status;
    }
    
    public RaidBossSpawnManager.StatusEnum getRaidStatus()
    {
    	return _raidStatus;
    }
    
    /**
     * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.<BR><BR> 
     * 
     */
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
        super.reduceCurrentHp(damage, attacker, awake);
    }
    public void healFull()
    {
        super.setCurrentHp(super.getMaxHp());
        super.setCurrentMp(super.getMaxMp());
    }
    
    
}