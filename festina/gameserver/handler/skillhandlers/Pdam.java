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

import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.lib.Log;
import com.festina.gameserver.SkillTable;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2RaidBossInstance;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.skills.Formulas;
import com.festina.gameserver.skills.EffectCharge;
import com.festina.gameserver.templates.L2WeaponType;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.2.7.2.16 $ $Date: 2005/04/06 16:13:49 $
 */

public class Pdam implements ISkillHandler
{
    // all the items ids that this handler knowns
    private static Logger _log = Logger.getLogger(Pdam.class.getName());

    /* (non-Javadoc)
     * @see com.festina.gameserver.handler.IItemHandler#useItem(com.festina.gameserver.model.L2PcInstance, com.festina.gameserver.model.L2ItemInstance)
     */
    private static SkillType[] _skillIds = {SkillType.PDAM,
    /* SkillType.CHARGEDAM */
    };

    /* (non-Javadoc)
     * @see com.festina.gameserver.handler.IItemHandler#useItem(com.festina.gameserver.model.L2PcInstance, com.festina.gameserver.model.L2ItemInstance)
     */
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
    {
        if (activeChar.isAlikeDead()) return;

        int damage = 0;

        if (Config.DEBUG)
            if (Config.DEBUG) _log.fine("Begin Skill processing in Pdam.java " + skill.getSkillType());

        for (int index = 0; index < targets.length; index++)
        {
            L2Character target = (L2Character) targets[index];
            L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
            if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance
                && target.isAlikeDead() && target.isFakeDeath())
            {
                target.stopFakeDeath(null);
            }
            else if (target.isAlikeDead()) continue;

            boolean dual = activeChar.isUsingDualWeapon();
            boolean shld = Formulas.getInstance().calcShldUse(activeChar, target);
            boolean crit = Formulas.getInstance().calcCrit(activeChar.getCriticalHit(target, skill));
            boolean soul = (weapon != null
                && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

            if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0) damage = 0;
            else damage = (int) Formulas.getInstance().calcPhysDam(activeChar, target, skill, shld,
                                                                   crit, dual, soul);

            if (damage > 5000 && activeChar instanceof L2PcInstance)
            {
                String name = "";
                if (target instanceof L2RaidBossInstance) name = "RaidBoss ";
                if (target instanceof L2NpcInstance)
                    name += target.getName() + "(" + ((L2NpcInstance) target).getTemplate().npcId
                        + ")";
                if (target instanceof L2PcInstance)
                    name = target.getName() + "(" + target.getObjectId() + ") ";
                name += target.getLevel() + " lvl";
                Log.add(activeChar.getName() + "(" + activeChar.getObjectId() + ") "
                    + activeChar.getLevel() + " lvl did damage " + damage + " with skill "
                    + skill.getName() + "(" + skill.getId() + ") to " + name, "damage_pdam");
            }
            // Why are we trying to reduce the current target HP here?
            // Why not inside the below "if" condition, after the effects processing as it should be?
            // It doesn't seem to make sense for me. I'm moving this line inside the "if" condition, right after the effects processing...
            // [changed by nexus - 2006-08-15]
            //target.reduceCurrentHp(damage, activeChar);
            if (soul && weapon != null) weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);

            if (damage > 0)
            {
                if (activeChar instanceof L2PcInstance)
                {
                    if (crit) activeChar.sendPacket(new SystemMessage(SystemMessage.CRITICAL_HIT));
                    
                    SystemMessage sm = new SystemMessage(SystemMessage.YOU_DID_S1_DMG);
                    sm.addNumber(damage);
                    activeChar.sendPacket(sm);
                }

                if (skill.hasEffects())
                {
                    // activate attacked effects, if any
                    target.stopEffect(skill.getId());
                    if (target.getEffect(skill.getId()) != null)
                        target.removeEffect(target.getEffect(skill.getId()));
                    if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, false, false, false))
                    {
                        skill.getEffects(activeChar, target);
                        
                        SystemMessage sm = new SystemMessage(SystemMessage.YOU_FEEL_S1_EFFECT);
                        sm.addSkillName(skill.getId());
                        target.sendPacket(sm);
                    }
                    else
                    {
                        SystemMessage sm = new SystemMessage(139);
                        sm.addString(target.getName());
                        sm.addSkillName(skill.getId());
                        activeChar.sendPacket(sm);
                    }
                }
                
                target.reduceCurrentHp(damage, activeChar);
            }
            else activeChar.sendPacket(new SystemMessage(SystemMessage.ATTACK_FAILED));
        }
        if (skill.getId() == 345 || skill.getId() == 346) // Sonic Rage or Raging Force
        {
            EffectCharge effect = (EffectCharge)activeChar.getEffect(L2Effect.EffectType.CHARGE);
            if (effect != null) 
            {
                int effectcharge = effect.getLevel();
                if (effectcharge < 7)
                {
                    effectcharge++;
                    effect.addNumCharges(1);
                    activeChar.updateEffectIcons();
                	//������� ��������� � �������.
                    //SystemMessage sm = new SystemMessage(SystemMessage.FORCE_INCREASED_TO_S1);
                    //sm.addNumber(effectcharge);
                    //activeChar.sendPacket(sm);
                }
                else
                {
                	//������� ��������� � ��� ��� ������ ����. ����� ������
                    //SystemMessage sm = new SystemMessage(SystemMessage.FORCE_MAXLEVEL_REACHED);
                    //activeChar.sendPacket(sm);
                }
            }
            else
            {
                if (skill.getId() == 345) // Sonic Rage
                {
                    L2Skill dummy = SkillTable.getInstance().getInfo(8, 7); // Lv7 Sonic Focus
                    dummy.getEffects(activeChar, activeChar);
                }
                else if (skill.getId() == 346) // Raging Force
                {
                    L2Skill dummy = SkillTable.getInstance().getInfo(50, 7); // Lv7 Focused Force
                    dummy.getEffects(activeChar, activeChar);
                }
            }
        }       
        
        if (skill.isSuicideAttack())
        {
        	activeChar.doDie(null);
        	activeChar.setCurrentHp(0);
        }
    }

    public SkillType[] getSkillIds()
    {
        return _skillIds;
    }
}
