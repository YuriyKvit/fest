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

import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/** 
 * @author _drunk_ 
 * 
 * TODO To change the template for this generated type comment go to 
 * Window - Preferences - Java - Code Style - Code Templates 
 */ 
public class DrainSoul implements ISkillHandler 
{ 
    private static Logger _log = Logger.getLogger(DrainSoul.class.getName()); 
    protected SkillType[] _skillIds = {SkillType.DRAIN_SOUL};
    
    public void useSkill(L2Character activeChar, L2Skill skill, @SuppressWarnings("unused") L2Object[] targets)
    { 
        if (!(activeChar instanceof L2PcInstance))
			return;

		L2Object[] targetList = skill.getTargetList(activeChar);
        
        if (targetList == null)
        {
            return;
        }

        _log.fine("Soul Crystal casting succeded.");
        
        // This is just a dummy skill handler for the soul crystal skill,
        // since the Soul Crystal item handler already does everything.

    } 
    
    public SkillType[] getSkillIds() 
    { 
        return _skillIds; 
    } 
}
