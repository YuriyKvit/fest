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
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.serverpackets.UserInfo;

/**
 * Itemhhandler for Character Appearance Change Potions
 * 
 * @author Tempy
  */
public class CharChangePotions implements IItemHandler {
	private static int[] _itemIds = { 
                                      5235, 5236, 5237,							// Face
	                                  5238, 5239, 5240, 5241, 					// Hair Color
	                                  5242, 5243, 5244, 5245, 5246, 5247, 5248 	// Hair Style
		                            };
	
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();
		
		L2PcInstance activeChar;
		
		if (playable instanceof L2PcInstance)
			activeChar = (L2PcInstance)playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance)playable).getOwner();
		else
			return;
		
		if (activeChar.isAllSkillsDisabled())
		{
            ActionFailed af = new ActionFailed();
            activeChar.sendPacket(af);
			return;
		}
		
		switch (itemId) {
		case 5235:
			activeChar.setFace(0);
			break;
		case 5236:
			activeChar.setFace(1);
			break;
		case 5237:
			activeChar.setFace(2);
			break;
		case 5238:
			activeChar.setHairColor(0);
			break;
		case 5239:
			activeChar.setHairColor(1);
			break;
		case 5240:
			activeChar.setHairColor(2);
			break;
		case 5241:
			activeChar.setHairColor(3);
			break;
		case 5242:
			activeChar.setHairStyle(0);
			break;
		case 5243:
			activeChar.setHairStyle(1);
			break;
		case 5244:
			activeChar.setHairStyle(2);
			break;
		case 5245:
			activeChar.setHairStyle(3);
			break;
		case 5246:
			activeChar.setHairStyle(4);
			break;
		case 5247:
			activeChar.setHairStyle(5);
			break;
		case 5248:
			activeChar.setHairStyle(6);
			break;
		}
		
        // Create a summon effect!
        MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2003, 1, 1, 0);
        activeChar.broadcastPacket(MSU);
        
		// Update the changed stat for the character in the DB.
		activeChar.store();
		
        // Remove the item from inventory.
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
        
		// Broadcast the changes to the char and all those nearby.
		UserInfo ui = new UserInfo(activeChar);
		activeChar.broadcastPacket(ui);
	}
		
	public int[] getItemIds()
	{
       return _itemIds;
	}
}