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
package com.festina.gameserver.handler.skillhandlers; 

import com.festina.Config;
import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.model.L2Attackable;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.InventoryUpdate;
import com.festina.gameserver.serverpackets.ItemList;
import com.festina.gameserver.serverpackets.SystemMessage;

/** 
 * @author _drunk_ 
 * 
 * TODO To change the template for this generated type comment go to 
 * Window - Preferences - Java - Code Style - Code Templates 
 */ 
public class Sweep implements ISkillHandler 
{ 
    //private static Logger _log = Logger.getLogger(Sweep.class.getName()); 
    protected SkillType[] _skillIds = {SkillType.SWEEP}; 
    
    public void useSkill(L2Character activeChar, @SuppressWarnings("unused") L2Skill skill, L2Object[] targets) 
    { 
        if (!(activeChar instanceof L2PcInstance))
        {
            return;
        }
        
        L2PcInstance player = (L2PcInstance)activeChar;
		InventoryUpdate iu = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		boolean send = false;
		
        for(int index = 0;index < targets.length;index++) 
        { 
            if (!(targets[index] instanceof L2Attackable)) 
            	continue;
	        L2Attackable target = (L2Attackable)targets[index];
            
            if (target.isSweepActive())
            {
            	L2Attackable.RewardItem[] items = target.takeSweep();
				if (items == null || items.length == 0) 
					continue;
				for (L2Attackable.RewardItem ritem : items)
				{
					if (player.isInParty())
						player.getParty().distributeItem(player, ritem, true, target);
					else
					{
						L2ItemInstance item = player.getInventory().addItem("Sweep", ritem.getItemId(), ritem.getCount(), player, target);
						if (iu != null) iu.addItem(item);
						send = true;

						SystemMessage smsg;
						if (ritem.getCount() > 1)
						{
							smsg = new SystemMessage(SystemMessage.EARNED_S2_S1_s); // earned $s2$s1
							smsg.addItemName(ritem.getItemId());
                            smsg.addNumber(ritem.getCount());
						}
						else
						{
							smsg = new SystemMessage(SystemMessage.EARNED_ITEM); // earned $s1
							smsg.addItemName(ritem.getItemId());
						}
						player.sendPacket(smsg);
					}
				}
            }
            target.endDecayTask();
            
    		if (send)
    		{
                if (iu != null) 
                	player.sendPacket(iu);
        		else 
        			player.sendPacket(new ItemList(player, false));
    		}
        }
    } 
    
    public SkillType[] getSkillIds() 
    { 
        return _skillIds; 
    } 
}
