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

import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
/**
 * This class ...
 * 
 * @version $Revision: 1.1.2.2.2.1 $ $Date: 2005/03/02 15:38:36 $
 */

public class CombatPointHeal implements ISkillHandler
{
    //private static Logger _log = Logger.getLogger(CombatPointHeal.class.getName());
    
    /* (non-Javadoc)
     * @see com.festina.gameserver.handler.IItemHandler#useItem(com.festina.gameserver.model.L2PcInstance, com.festina.gameserver.model.L2ItemInstance)
     */
    private static SkillType[] _skillIds = {SkillType.COMBATPOINTHEAL};
    
    /* (non-Javadoc)
     * @see com.festina.gameserver.handler.IItemHandler#useItem(com.festina.gameserver.model.L2PcInstance, com.festina.gameserver.model.L2ItemInstance)
     */
    public void useSkill(@SuppressWarnings("unused") L2Character actChar, L2Skill skill, L2Object[] targets)
    {
//      L2Character activeChar = actChar;

        L2Character target = null;
        
        for(int index = 0;index < targets.length;index++)
        {
            target = (L2Character)targets[index];
            
            double cp = skill.getPower(); 
            //int cLev = activeChar.getLevel();
            //hp += skill.getPower()/*+(Math.sqrt(cLev)*cLev)+cLev*/; 
            SystemMessage sm = new SystemMessage(SystemMessage.S1_CP_WILL_BE_RESTORED);  
            sm.addNumber((int)cp);  
            target.sendPacket(sm);
            target.setCurrentCp(cp+target.getCurrentCp()); 
            StatusUpdate sump = new StatusUpdate(target.getObjectId()); 
            sump.addAttribute(StatusUpdate.CUR_CP, (int)target.getCurrentCp()); 
            target.sendPacket(sump);
        }
    }
    
    
    public SkillType[] getSkillIds()
    {
        return _skillIds;
    }
}