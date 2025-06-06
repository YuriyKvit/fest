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
package com.festina.gameserver.handler.usercommandhandlers;

import com.festina.Config;
import com.festina.gameserver.GameTimeController;
import com.festina.gameserver.MapRegionTable;
import com.festina.gameserver.ThreadPoolManager;
import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.handler.IUserCommandHandler;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.serverpackets.SetupGauge;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.util.Broadcast;

/**
 * 
 *
 */
public class Escape implements IUserCommandHandler
{
    private static final int[] COMMAND_IDS = { 52 }; 
    private static final int REQUIRED_LEVEL = Config.GM_ESCAPE;

    /* (non-Javadoc)
     * @see com.festina.gameserver.handler.IUserCommandHandler#useUserCommand(int, com.festina.gameserver.model.L2PcInstance)
     */
    public boolean useUserCommand(int id, L2PcInstance activeChar)
    {   
        if (activeChar.isCastingNow() || activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isAlikeDead() ||
                activeChar.isInOlympiadMode())
            return false;
        
        int unstuckTimer = (activeChar.getAccessLevel() >=REQUIRED_LEVEL? 5000 : Config.UNSTUCK_INTERVAL*1000 );
        
        // Check to see if the player is in a festival.
        if (activeChar.isFestivalParticipant()) 
        {
        	activeChar.sendMessage("You may not use an escape command in a festival.");
        	return false;
        }
        
        // Check to see if player is in jail
        if (activeChar.isInJail())
        {
        	activeChar.sendMessage("You can not escape from jail.");
            return false;
        }
        
        SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
        sm.addString("After " + unstuckTimer/60000 + " min. you be returned to near village.");
        
        activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        //SoE Animation section
        activeChar.setTarget(activeChar);
        activeChar.disableAllSkills();

        MagicSkillUser msk = new MagicSkillUser(activeChar, 1050, 1, unstuckTimer, 0);
        Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 810000/*900*/);
        SetupGauge sg = new SetupGauge(0, unstuckTimer);
        activeChar.sendPacket(sg);
        //End SoE Animation section

        EscapeFinalizer ef = new EscapeFinalizer(activeChar);
        // continue execution later
        activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
        activeChar.setSkillCastEndTime(10+GameTimeController.getGameTicks()+unstuckTimer/GameTimeController.MILLIS_IN_TICK);
        
        return true;
    }

    static class EscapeFinalizer implements Runnable
    {
        private L2PcInstance _activeChar;
        
        EscapeFinalizer(L2PcInstance activeChar)
        {
            _activeChar = activeChar;
        }
        
        public void run()
        {
            if (_activeChar.isDead()) 
                return; 
            
            _activeChar.setIsIn7sDungeon(false);
            
            _activeChar.enableAllSkills();
            
            try 
            {
                _activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            } catch (Throwable e) { if (Config.DEBUG) e.printStackTrace(); }
        }
    }
    
    /* (non-Javadoc)
     * @see com.festina.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}