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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package com.festina.gameserver.handler.itemhandlers; 


import com.festina.gameserver.SkillTable;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.skills.L2SkillCharge;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.skills.EffectCharge;

public class EnergyStone implements IItemHandler 
{
    private static int[] _itemIds = { 5589 };
    private EffectCharge effect;
    private L2SkillCharge skill;

    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
      
    	L2PcInstance activeChar;
        if (playable instanceof L2PcInstance)
        {
        	activeChar = (L2PcInstance)playable;
        }
        else if (playable instanceof L2PetInstance)
        {
        	activeChar = ((L2PetInstance)playable).getOwner();
        }
        else return;

        if (item.getItemId() != 5589) return;
        int classid = activeChar.getClassId().getId();
        
        if (classid == 2 || classid == 48 || classid == 88 || classid == 114)
        {
      
        	if (activeChar.isAllSkillsDisabled())
        	{
        		ActionFailed af = new ActionFailed();
        		activeChar.sendPacket(af);
        		return;
            }

            if (activeChar.isSitting())
            {
                     SystemMessage sm = new SystemMessage(614);
                     sm.addString("You cannot use that while sitting.");
                     activeChar.sendPacket(sm);
                     return;
            }
     
            skill = getChargeSkill(activeChar);
            if (skill == null)
            {
                     SystemMessage sm = new SystemMessage(614);
                     sm.addString("You dont have the knowledge.");
                     activeChar.sendPacket(sm);
                     return;
            }
     
            effect = getChargeEffect(activeChar);
        
            if (effect == null)
            {
                L2Skill dummy = SkillTable.getInstance().getInfo(skill.getId(),skill.getLevel());
                if (dummy != null) 
                {
                	dummy.getEffects(null, activeChar);
                	activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
                	return;
                }
                return;
            }
    
            if (effect.getLevel() < 2)
            {
                MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, skill.getId(), 1, 1, 0);
                activeChar.sendPacket(MSU);
                activeChar.broadcastPacket(MSU);
                effect.addNumCharges(1);
                activeChar.updateEffectIcons();
                activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
            }
            else if (effect.getLevel() == 2)
            {
                SystemMessage sm2 = new SystemMessage(614);
                sm2.addString("Already charged to maximum.");
                activeChar.sendPacket(sm2);
            }
            SystemMessage sm = new SystemMessage(614);
            sm.addString("Charged to " + effect.getLevel() + ".");
            activeChar.sendPacket(sm);
            return;
        }
        else
        {
             SystemMessage sm = new SystemMessage(614);
             sm.addString("Only Gladiators or Tyrants can use Energy Stone.");
             activeChar.sendPacket(sm);
             return;
        }
    }
    
    private EffectCharge getChargeEffect(L2PcInstance activeChar)
    {
    L2Effect[] effects = activeChar.getAllEffects();
    for (L2Effect e : effects)
    {
        if (e.getSkill().getSkillType() == L2Skill.SkillType.CHARGE)
        {
            return (EffectCharge)e;    
        }
    }
    return null;
    }
    private L2SkillCharge getChargeSkill(L2PcInstance activeChar)
    {     
    L2Skill[] skills = activeChar.getAllSkills();
    for (L2Skill s : skills) {
        if (s.getId() == 50 || s.getId() == 8) {
            return (L2SkillCharge)s;
        }
    }
    return null;
    }

    public int[] getItemIds()
    {
        return _itemIds;
    }
}