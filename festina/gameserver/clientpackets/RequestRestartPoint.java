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
package com.festina.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.festina.gameserver.instancemanager.ClanHallManager;
import com.festina.gameserver.model.entity.ClanHall;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.MapRegionTable;
import com.festina.gameserver.ThreadPoolManager;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.model.L2SiegeClan;
import com.festina.gameserver.model.Location;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.entity.Castle;
import com.festina.gameserver.serverpackets.Revive;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.util.IllegalPlayerAction;
import com.festina.gameserver.util.Util;


/**
 * This class ...
 * 
 * @version $Revision: 1.7.2.3.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestRestartPoint extends ClientBasePacket
{
    private static final String _C__6d_REQUESTRESTARTPOINT = "[C] 6d RequestRestartPoint";
    private static Logger _log = Logger.getLogger(RequestRestartPoint.class.getName());	
    
    protected int     requestedPointType;
    protected boolean continuation;
    
    /**
     * packet type id 0x6d
     * format:		c
     * @param decrypt
     */
    public RequestRestartPoint(ByteBuffer buf, ClientThread client)
   {
        super(buf,client);
       requestedPointType = readD();
   }
   
   class DeathTask implements Runnable
   {
       L2PcInstance activeChar;
       DeathTask (L2PcInstance _activeChar)
       {
           activeChar = _activeChar;
       }
       
       public void run()
       {
           //_log.warning(activeChar.getName()+" request restartpoint "+requestedPointType);
           try
           {
               Location loc = null;
               Castle castle=null;
               
               if (activeChar.isInJail()) // to jail
                   loc = new Location(-114356, -249645, -2984);
               
               if (activeChar.isFestivalParticipant()) requestedPointType = 4;
               
               switch (requestedPointType)
               {
                   case 1: // to clanhall
                       if (activeChar.getClan().getHasHideout() == 0)
                       {
                           //cheater
                           activeChar.sendMessage("You may not use this respawn point!");
                           Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
                           return;
                       }
                       loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.ClanHall);
                       
                       if (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan())!= null &&
                               ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
                       {
                           activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
                       }
                       break;
                       
                   case 2: // to castle
                       Boolean isInDefense = false;
                       castle = CastleManager.getInstance().getCastle(activeChar);                 
                       if (castle != null && castle.getSiege().getIsInProgress())
                       {
                           //siege in progress             
                           if (castle.getSiege().checkIsDefender(activeChar.getClan()))
                               isInDefense = true;
                       }
                       if (activeChar.getClan().getHasCastle() == 0 && !isInDefense)
                       {
                           //cheater
                           activeChar.sendMessage("You may not use this respawn point!");
                           Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
                           return;
                       }
                       loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Castle);
                       break;
                       
                   case 3: // to siege HQ
                       L2SiegeClan siegeClan = null;
                       castle = CastleManager.getInstance().getCastle(activeChar);
                       
                       if (castle != null && castle.getSiege().getIsInProgress())
                           siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());
                       
                       if (siegeClan == null || siegeClan.getFlag().size() == 0)
                       {
                           //cheater
                           activeChar.sendMessage("You may not use this respawn point!");
                           Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
                           return;
                       }
                       loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.SiegeFlag);
                       break;
                       
                   case 4: // Fixed or Player is a festival participant
                       if (!activeChar.isGM() && !activeChar.isFestivalParticipant())
                       {
                           //cheater
                           activeChar.sendMessage("You may not use this respawn point!");
                           Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
                           return;
                       }
                       loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ()); // spawn them where they died
                       break;
                       
                   default:
                       loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
                   break;
               }
               
               //Teleport and revive
               activeChar.setIsPendingRevive(true);
               activeChar.teleToLocation(loc);
           } catch (Throwable e) {
               //_log.log(Level.SEVERE, "", e);
           }
       }
   }    
   @Override
   protected void runImpl()
   {
       L2PcInstance activeChar = getClient().getActiveChar();
       
       if (activeChar == null)
           return;
       
       //SystemMessage sm2 = new SystemMessage(SystemMessage.S1_S2);
       //sm2.addString("type:"+requestedPointType);
       //activeChar.sendPacket(sm2);
        if (activeChar.isFakeDeath())
           {
               activeChar.stopFakeDeath(null);
               activeChar.broadcastPacket(new Revive(activeChar));
               return;
           }
           else if(!activeChar.isAlikeDead())
           {
               _log.warning("Living player ["+activeChar.getName()+"] called RestartPointPacket! Ban this player!");
               return;
           }
           
           Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
           if (castle != null && castle.getSiege().getIsInProgress())
           {
               //DeathFinalizer df = new DeathFinalizer(10000);
               SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
               if (activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan()))
               {
                   // Schedule respawn delay for attacker
                   ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), castle.getSiege().getAttackerRespawnDelay());
                   sm.addString("You will be re-spawned in " + castle.getSiege().getAttackerRespawnDelay()/1000 + " seconds");
                   activeChar.sendPacket(sm);
               }
               else
               {
                   // Schedule respawn delay for defender with penalty for CT lose
                   ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), castle.getSiege().getDefenderRespawnDelay());
                   sm.addString("You will be re-spawned in " + castle.getSiege().getDefenderRespawnDelay()/1000 + " seconds");
                   activeChar.sendPacket(sm);
               }
               sm = null;
               return;
           }
           
           ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), 1);
       }
    
    
    
    /* (non-Javadoc)
     * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__6d_REQUESTRESTARTPOINT;
    }
}
