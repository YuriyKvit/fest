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
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2ArtefactInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.entity.Castle;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.util.Util;

/** 
 * @author _drunk_ 
 * 
 * TODO To change the template for this generated type comment go to 
 * Window - Preferences - Java - Code Style - Code Templates 
 */ 
public class TakeCastle implements ISkillHandler 
{ 
    //private static Logger _log = Logger.getLogger(TakeCastle.class.getName()); 
    protected SkillType[] _skillIds = {SkillType.TAKECASTLE}; 
    
    public void useSkill(L2Character activeChar, @SuppressWarnings("unused") L2Skill skill, @SuppressWarnings("unused") L2Object[] targets)
    {
        if (activeChar == null || !(activeChar instanceof L2PcInstance)) return;

        L2PcInstance player = (L2PcInstance)activeChar;

        if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId()) return;

        Castle castle = CastleManager.getInstance().getCastle(player);
        if (castle == null || !checkIfOkToCastSealOfRule(player, castle, true)) return;

    	castle.setOwner(player.getClan());
    } 
    
    public SkillType[] getSkillIds() 
    { 
        return _skillIds; 
    } 

    /**
     * Return true if character clan place a flag<BR><BR>
     * 
     * @param activeChar The L2Character of the character placing the flag
     * 
     */
    public static boolean checkIfOkToCastSealOfRule(L2Character activeChar, boolean isCheckOnly)
    {
        return checkIfOkToCastSealOfRule(activeChar, CastleManager.getInstance().getCastle(activeChar), isCheckOnly);
    }

    public static boolean checkIfOkToCastSealOfRule(L2Character activeChar, Castle castle, boolean isCheckOnly)
    {
        if (activeChar == null || !(activeChar instanceof L2PcInstance))
            return false;

        SystemMessage sm = new SystemMessage(614);
        L2PcInstance player = (L2PcInstance)activeChar;

        if (castle == null || castle.getCastleId() <= 0)
            sm.addString("You must be on castle ground to use this skill");
        else if (player.getTarget() == null && !(player.getTarget() instanceof L2ArtefactInstance))
            sm.addString("You can only use this skill on an artifact");
        else if (!castle.getSiege().getIsInProgress())
            sm.addString("You can only use this skill during a siege.");
        else if (!Util.checkIfInRange(200, player, player.getTarget(), true))
            sm.addString("You are not in range of the artifact.");
        else if (castle.getSiege().getAttackerClan(player.getClan()) == null)
            sm.addString("You must be an attacker to use this skill");
        else
        {
            if (!isCheckOnly) castle.getSiege().announceToPlayer("Clan " + player.getClan().getName() + " has begun to engrave the ruler.", true);                
            return true;
        }

        if (!isCheckOnly) {player.sendPacket(sm);}
        return false;
    }
}
