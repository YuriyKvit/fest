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

import com.festina.gameserver.NpcTable;
import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.idfactory.IdFactory;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.instancemanager.SiegeManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.festina.gameserver.model.entity.Castle;
import com.festina.gameserver.serverpackets.SystemMessage;

/** 
 * @author _drunk_ 
 * 
 * TODO To change the template for this generated type comment go to 
 * Window - Preferences - Java - Code Style - Code Templates 
 */ 
public class SiegeFlag implements ISkillHandler 
{ 
    //private static Logger _log = Logger.getLogger(SiegeFlag.class.getName()); 
    protected SkillType[] _skillIds = {SkillType.SIEGEFLAG}; 
    
    public void useSkill(L2Character activeChar, @SuppressWarnings("unused") L2Skill skill, @SuppressWarnings("unused") L2Object[] targets)
    {
        if (activeChar == null || !(activeChar instanceof L2PcInstance)) return;

        L2PcInstance player = (L2PcInstance)activeChar;

        if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId()) return;

        Castle castle = CastleManager.getInstance().getCastle(player);

        if (castle == null || !checkIfOkToPlaceFlag(player, castle, true)) return;
        
        try
        {
            L2ItemInstance itemToTake = player.getInventory().getItemByItemId(SiegeManager.getInstance().getFlagBuyItemId());
            if(!player.destroyItem("Consume", itemToTake.getObjectId(), SiegeManager.getInstance().getFlagBuyCost(), null, true)) return;

            // Spawn a new flag
            L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(12024));
            flag.setTitle(player.getClan().getName());
            flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
            flag.setHeading(player.getHeading());
            flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
            castle.getSiege().getFlag(player.getClan()).add(flag);
        }
        catch (Exception e)
        {
            player.sendMessage("Error placing flag:" + e);
        }
    } 
    
    public SkillType[] getSkillIds() 
    { 
        return _skillIds; 
    }

    /**
     * Return true if character clan place a flag<BR><BR>
     * 
     * @param activeChar The L2Character of the character placing the flag
     * @param isCheckOnly if false, it will send a notification to the player telling him
     * why it failed
     */
    public static boolean checkIfOkToPlaceFlag(L2Character activeChar, boolean isCheckOnly)
    {
        return checkIfOkToPlaceFlag(activeChar, CastleManager.getInstance().getCastle(activeChar), isCheckOnly);
    }

    public static boolean checkIfOkToPlaceFlag(L2Character activeChar, Castle castle, boolean isCheckOnly)
    {
        if (activeChar == null || !(activeChar instanceof L2PcInstance))
            return false;
        
        SystemMessage sm = new SystemMessage(614);
        L2PcInstance player = (L2PcInstance)activeChar;
        L2ItemInstance itemToTake = player.getInventory().getItemByItemId(SiegeManager.getInstance().getFlagBuyItemId());

        if (castle == null || castle.getCastleId() <= 0)
            sm.addString("You must be on castle ground to place a flag");
        else if (!castle.getSiege().getIsInProgress())
            sm.addString("You can only place a flag during a siege.");
        else if (castle.getSiege().getAttackerClan(player.getClan()) == null)
            sm.addString("You must be an attacker to place a flag");
        else if (player.getClan() == null || !player.isClanLeader())
            sm.addString("You must be a clan leader to place a flag");
        else if (SiegeManager.getInstance().getFlagBuyItemId() > 0 && itemToTake == null)
            sm.addString("You do not have the required construction items");
        else if (itemToTake != null && itemToTake.getCount() < SiegeManager.getInstance().getFlagBuyCost())
            sm.addString("You do not have enough items");
        else if (castle.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= SiegeManager.getInstance().getFlagMaxCount())
        	sm.addString("You have already placed the maximum number of flags possible");
        else
            return true;
        
        if (!isCheckOnly) {player.sendPacket(sm);}
        return false;
    }
}
