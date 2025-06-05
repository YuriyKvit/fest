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
import com.festina.gameserver.model.L2Summon;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.ExAutoSoulShot;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.serverpackets.PetInfo;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2Weapon;
import com.festina.gameserver.util.Broadcast;

/**
 * Beast SpiritShot Handler
 * 
 * @author Tempy
 */
public class BeastSpiritShot implements IItemHandler
{
    // All the item IDs that this handler knows.
    private static int[] _itemIds = {6646, 6647};
    
    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
    	if (playable == null) return; 
        L2PcInstance activeOwner = null;;
        
        if (playable instanceof L2Summon)
        {
            activeOwner = ((L2Summon)playable).getOwner();
            activeOwner.sendPacket(new SystemMessage(SystemMessage.PET_CANNOT_USE_ITEM));
            return;
        }
        else if (playable instanceof L2PcInstance) 
        { 
        		activeOwner = (L2PcInstance)playable; 
        } 
        
        L2Summon activePet = activeOwner.getPet();
        
        if (activePet == null)
        {
            activeOwner.sendPacket(new SystemMessage(574));
            return;
        }
        
        if (activePet.isDead())
        {
            activeOwner.sendPacket(new SystemMessage(1598));
            return;
        }

        int itemId = item.getItemId();
        boolean isBlessed = (itemId == 6647);
        int shotConsumption = 1;
        
        L2ItemInstance weaponInst = null;
        L2Weapon weaponItem = null;
        
        if (activePet instanceof L2PetInstance)
        {
            weaponInst = ((L2PetInstance)activePet).getActiveWeaponInstance();
            weaponItem = ((L2PetInstance)activePet).getActiveWeaponItem();
            
            if (weaponInst == null)
            {
                activeOwner.sendPacket(new SystemMessage(SystemMessage.CANNOT_USE_SPIRITSHOTS));
                return;
            }
            
            if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
            {
                // SpiritShots are already active.
                return;
            }
        
            int shotCount = item.getCount();
            shotConsumption = weaponItem.getSpiritShotCount();
            
            if (shotConsumption == 0)
            {
                activeOwner.sendPacket(new SystemMessage(SystemMessage.CANNOT_USE_SPIRITSHOTS));
                return;
            }   
            
            if (!(shotCount > shotConsumption))
            {
                // Not enough SpiritShots to use.
                activeOwner.sendPacket(new SystemMessage(1700));
                return;
            }

            if (isBlessed)
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
            else
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);
        }
        else
        {
            if (activePet.getChargedSpiritShot() != L2ItemInstance.CHARGED_NONE)
                return;
             
            if (isBlessed)
                activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
            else
                activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_SPIRITSHOT);
        }
        
        if (!activeOwner.destroyItem("Consume", item.getObjectId(), shotConsumption, null, false))
        {
            if (activeOwner.getAutoSoulShot().contains(itemId))
            {
                activeOwner.removeAutoSoulShot(itemId);
                activeOwner.sendPacket(new ExAutoSoulShot(itemId, 0));
                
                SystemMessage sm = new SystemMessage(SystemMessage.AUTO_USE_OF_S1_CANCELLED); 
                sm.addString(item.getItem().getName());
                activeOwner.sendPacket(sm);
                return;
            }

            activeOwner.sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_SPIRITSHOTS));
            return;     
        }
        
        // Update used spiritshot count and send a Server->Client update.
        activePet.increaseUsedSpiritShots(1);
        activeOwner.sendPacket(new PetInfo(activePet));
        
        // Pet uses the power of spirit.
        activeOwner.sendPacket(new SystemMessage(1576));
        
        Broadcast.toSelfAndKnownPlayersInRadius(activeOwner, new MagicSkillUser(activePet, activePet, isBlessed? 2009:2008, 1, 0, 0), 360000/*600*/);
    }
    
    public int[] getItemIds()
    {
        return _itemIds;
    }
}
