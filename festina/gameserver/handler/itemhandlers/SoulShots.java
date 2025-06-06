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

import com.festina.Config;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.ExAutoSoulShot;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.skills.Stats;
import com.festina.gameserver.templates.L2Item;
import com.festina.gameserver.templates.L2Weapon;
import com.festina.gameserver.util.Broadcast;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.4.4 $ $Date: 2005/03/27 15:30:07 $
 */

public class SoulShots implements IItemHandler
{
    // All the item IDs that this handler knows.
	private static int[] _itemIds = {5789, 1835, 1463, 1464, 1465, 1466, 1467 };
	private static int[] _skillIds = {2039, 2150, 2151, 2152, 2153, 2154 };

	/* (non-Javadoc)
	 * @see com.festina.gameserver.handler.IItemHandler#useItem(com.festina.gameserver.model.L2PcInstance, com.festina.gameserver.model.L2ItemInstance)
	 */
	public synchronized void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{  // why is this synchronized? according to user repoorts it leads to a deadlock (#562)
		if (!(playable instanceof L2PcInstance)) return;
        
		L2PcInstance activeChar = (L2PcInstance)playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
        int itemId = item.getItemId();
		
        // Check if Soulshot can be used
		if (weaponInst == null || weaponItem.getSoulShotCount() == 0)
		{
            if(!activeChar.getAutoSoulShot().contains(itemId)) 
                activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_USE_SOULSHOTS));
			return;
		}

        // Check if Soulshot is already active
        if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE) return;

		// Check for correct grade
        int weaponGrade = weaponItem.getCrystalType();
        if ((weaponGrade == L2Item.CRYSTAL_NONE && itemId != 5789 && itemId != 1835) || 
			(weaponGrade == L2Item.CRYSTAL_D && itemId != 1463) || 
			(weaponGrade == L2Item.CRYSTAL_C && itemId != 1464) || 
			(weaponGrade == L2Item.CRYSTAL_B && itemId != 1465) || 
			(weaponGrade == L2Item.CRYSTAL_A && itemId != 1466) || 
			(weaponGrade == L2Item.CRYSTAL_S && itemId != 1467))
		{
            if(!activeChar.getAutoSoulShot().contains(itemId)) 
                activeChar.sendPacket(new SystemMessage(SystemMessage.SOULSHOTS_GRADE_MISMATCH));
			return;
		}
		
        // Consume Soulshots if player has enough of them 
        int saSSCount = (int)activeChar.getStat().calcStat(Stats.SOULSHOT_COUNT, 0, null, null);
        int SSCount = saSSCount == 0 ? weaponItem.getSoulShotCount() : saSSCount; 
        if(!Config.DONT_DESTROY_SS)
        {
		if (!activeChar.destroyItem("Consume", item.getObjectId(), SSCount, null, false))
		{
            if(activeChar.getAutoSoulShot().contains(itemId))
            {
                activeChar.removeAutoSoulShot(itemId);
                activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));
                
                SystemMessage sm = new SystemMessage(SystemMessage.AUTO_USE_OF_S1_CANCELLED); 
                sm.addString(item.getItem().getName());
                activeChar.sendPacket(sm);
            }
            else activeChar.sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_SOULSHOTS));
			return;
		}
        }
        
        // Charge soulshot
        weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);
        
		// Send message to client
        activeChar.sendPacket(new SystemMessage(SystemMessage.ENABLED_SOULSHOT));
        Broadcast.toSelfAndKnownPlayersInRadius(activeChar, new MagicSkillUser(activeChar, activeChar, _skillIds[weaponGrade], 1, 0, 0), 360000/*600*/);
	}
	
	public int[] getItemIds()
	{
		return _itemIds;
	}
}
