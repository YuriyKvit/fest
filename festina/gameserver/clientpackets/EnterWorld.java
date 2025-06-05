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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.festina.Base64;
import com.festina.Config;
import com.festina.L2DatabaseFactory;
// import com.festina.License;
import com.festina.gameserver.Announcements;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.GmListTable;
import com.festina.gameserver.LoginServerThread;
import com.festina.gameserver.MapRegionTable;
import com.festina.gameserver.Olympiad;
import com.festina.gameserver.SevenSigns;
import com.festina.gameserver.TaskPriority;
import com.festina.gameserver.cache.HtmCache;
import com.festina.gameserver.handler.AdminCommandHandler;
import com.festina.gameserver.instancemanager.ClanHallManager;
import com.festina.gameserver.instancemanager.DimensionalRiftManager;
import com.festina.gameserver.instancemanager.PetitionManager;
import com.festina.gameserver.model.entity.ClanHall;
import com.festina.gameserver.serverpackets.ClanHallDecoration;
import com.festina.gameserver.instancemanager.SiegeManager;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.entity.Hero;
import com.festina.gameserver.model.entity.L2Event;
import com.festina.gameserver.model.entity.Siege;
import com.festina.gameserver.model.quest.Quest;
import com.festina.gameserver.serverpackets.Die;
import com.festina.gameserver.serverpackets.ExStorageMaxCount;
import com.festina.gameserver.serverpackets.FriendList;
import com.festina.gameserver.serverpackets.GameGuardQuery;
import com.festina.gameserver.serverpackets.HennaInfo;
import com.festina.gameserver.serverpackets.ItemList;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.serverpackets.PledgeShowMemberListAdd;
import com.festina.gameserver.serverpackets.PledgeShowMemberListAll;
import com.festina.gameserver.serverpackets.PledgeStatusChanged;
import com.festina.gameserver.serverpackets.ShortCutInit;
import com.festina.gameserver.serverpackets.SignsSky;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.serverpackets.UserInfo;
import com.festina.gameserver.util.FloodProtector;
/**
 * Enter World Packet Handler<p>
 * <p>
 * 0000: 03 <p>
 * packet format rev656 cbdddd  
 * <p>
 * 
 * @version $Revision: 1.16.2.1.2.7 $ $Date: 2005/03/29 23:15:33 $
 */
public class EnterWorld extends ClientBasePacket
{
	private static final String _C__03_ENTERWORLD = "[C] 03 EnterWorld";
	private static Logger _log = Logger.getLogger(EnterWorld.class.getName());
    
	public TaskPriority getPriority() { return TaskPriority.PR_URGENT; }
	
	/**
	 * @param decrypt
	 */
	public EnterWorld(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		// this is just a trigger packet. it has no content
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			_log.warning("EnterWorld failed! activeChar is null...");
		    return;
		}
		FloodProtector.getInstance().registerNewPlayer(activeChar.getObjectId());
		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
		{
			if(Config.DEBUG)
				_log.warning("User already exist in OID map! User "+activeChar.getName()+" is character clone");
			//activeChar.closeNetConnection();
		}
        
        if (activeChar.isGM())
        {
        	if (Config.GM_STARTUP_INVULNERABLE
        			&& (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_GODMODE
        			  || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invul")))
        		activeChar.setIsInvul(true);
        	
            if (Config.GM_STARTUP_INVISIBLE 
                    && (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_GODMODE
                      || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invisible")))
                activeChar.setInvisible();

            if (Config.GM_STARTUP_SILENCE 
                    && (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_MENU
                      || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_silence")))
                activeChar.setMessageRefusal(true);
            
            if (Config.GM_STARTUP_AUTO_LIST 
                    && (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_MENU
                      || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_gmliston")))
            	GmListTable.getInstance().addGm(activeChar);
            
            if (Config.GM_NAME_COLOR_ENABLED)
            {
                if (activeChar.getAccessLevel() >= 100)
                    activeChar.setNameColor(Config.ADMIN_NAME_COLOR);
                else if (activeChar.getAccessLevel() >= 75)
                    activeChar.setNameColor(Config.GM_NAME_COLOR);
            }
        }
        
        if (Config.PLAYER_SPAWN_PROTECTION > 0)
            activeChar.setProtection(true);
        
		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		
		if (L2Event.active && L2Event.connectionLossData.containsKey(activeChar.getName()) && L2Event.isOnEvent(activeChar))
            L2Event.restoreChar(activeChar);
        else if (L2Event.connectionLossData.containsKey(activeChar.getName()))
            L2Event.restoreAndTeleChar(activeChar);

		if (SevenSigns.getInstance().isSealValidationPeriod())
			sendPacket(new SignsSky());
		
        if (Config.STORE_SKILL_COOLTIME)
            activeChar.restoreEffects();
        
        if (activeChar.getAllEffects() != null)
        {
            for (L2Effect e : activeChar.getAllEffects())
            {
                if (e.getEffectType() == L2Effect.EffectType.HEAL_OVER_TIME)
                {
                    activeChar.stopEffects(L2Effect.EffectType.HEAL_OVER_TIME);
                    activeChar.removeEffect(e);
                }
                
                if (e.getEffectType() == L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME)
                {
                    activeChar.stopEffects(L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME);
                    activeChar.removeEffect(e);
                }
            }
        }
        
        //Expand Skill		
        ExStorageMaxCount esmc = new ExStorageMaxCount(activeChar);  
        activeChar.sendPacket(esmc);        
        
        activeChar.getMacroses().sendUpdate();
        
        sendPacket(new ItemList(activeChar, false));

        sendPacket(new UserInfo(activeChar));

		sendPacket(new ShortCutInit(activeChar));

        sendPacket(new HennaInfo(activeChar));
        
        sendPacket(new FriendList(activeChar, false, null));
        
        if (Config.SHOW_ONLINE_ON_LOGIN)
        	activeChar.sendMessage("Online players: " + L2World.getInstance().getAllPlayersCount());
        if ((activeChar.isClanLeader()) && (Config.CLAN_LEADER_COLOR_ENABLE))
        	activeChar.setNameColor(Config.CLAN_LEADER_COLOR);
        	
        SystemMessage sm = new SystemMessage(34);
        sendPacket(sm);
	

        SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
        Announcements.getInstance().showAnnouncements(activeChar);
//         if(License.isFree)
//         activeChar.sendMessage("You use Festina-Project server. Free Version.");

		Quest.playerEnter(activeChar);

		String serverNews = HtmCache.getInstance().getHtm("data/html/servnews.htm");
		
		if (serverNews != null)
		{
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			htmlMsg.setHtml(serverNews);
			sendPacket(htmlMsg);
		}
		
		PetitionManager.getInstance().checkPetitionMessages(activeChar);
		
        // send user info again .. just like the real client
        //sendPacket(ui);

        if (activeChar.getClanId() != 0 && activeChar.getClan() != null)
        {
        	sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
        	sendPacket(new PledgeStatusChanged(activeChar.getClan()));
        }
        if (activeChar.getClan() != null) 
        { 

         	for (Siege siege : SiegeManager.getInstance().getSieges()) 
         	{ 
         		if (!siege.getIsInProgress()) continue; 
         		if (siege.checkIsAttacker(activeChar.getClan())) 
         			activeChar.setSiegeState((byte)1); 
         		else if (siege.checkIsDefender(activeChar.getClan())) 
         			activeChar.setSiegeState((byte)2); 
         	} 
        } 
	
		if (activeChar.isAlikeDead())
		{
			// no broadcast needed since the player will already spawn dead to others
			sendPacket(new Die(activeChar));
		}

		if (Config.ALLOW_WATER)
		    activeChar.checkWaterState();
        
		//add char to online characters
		activeChar.setOnlineStatus(true);
		
		activeChar.notifyFriends(false);
		notifyClanMembers(activeChar);
        
        activeChar.onPlayerEnter();
        
        if (Olympiad.getInstance().playerInStadia(activeChar))
        {
            activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            activeChar.sendMessage("You have been teleported to the nearest town due to you being in an Olympiad Stadium");
        }
        
        if (Hero.getInstance().getHeroes() != null &&
                Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
            activeChar.setHero(true);
        //16 - κυ
         if (activeChar.isInsideZone(16)){ 
                 ClanHall clanHall = ClanHallManager.getInstance().getNearbyClanHall(activeChar.getX(), activeChar.getY(), activeChar.getZ()); 
                 if(clanHall != null){ 
                         ClanHallDecoration bl = new ClanHallDecoration(clanHall); 
                         activeChar.sendPacket(bl); 
                 } 
         }
         if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
         {
        	 DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
         }
        if(Config.GAMEGUARD_ENFORCE)
            activeChar.sendPacket(new GameGuardQuery());
	}
    
	/**
	 * @param activeChar
	 */
	/*private void notifyFriends(final L2PcInstance cha)
	{
		java.sql.Connection con = null;
		
		try {
		    con = L2DatabaseFactory.getInstance().getConnection();
		    PreparedStatement statement;
		    statement = con.prepareStatement("SELECT char_id FROM character_friends WHERE friend_id=?");
		    statement.setInt(1, cha.getObjectId());
		    ResultSet rset = statement.executeQuery();

            L2PcInstance friend;
            int objectId;
            
            SystemMessage sm = new SystemMessage(SystemMessage.FRIEND_S1_HAS_LOGGED_IN);
            sm.addString(cha.getName());

            while (rset.next())
            {
                objectId = rset.getInt("char_id");

                friend = (L2PcInstance)L2World.getInstance().findObject(objectId);

                if (friend != null) //friend logged in.
                {
                	friend.sendPacket(new FriendList(friend));
                    friend.sendPacket(sm);
                }
		    }
        } 
		catch (Exception e) {
            _log.warning("could not restore friend data:"+e);
        } 
		finally {
            try {con.close();} catch (Exception e){}
        }
	}
    */
	/**
	 * @param activeChar
	 */
	private void notifyClanMembers(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			clan.getClanMember(activeChar.getName()).setPlayerInstance(activeChar);
			SystemMessage msg = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_LOGGED_IN);
			msg.addString(activeChar.getName());

            if(activeChar.isClanLeader()) 
                activeChar.setClanPrivileges(L2Clan.CP_ALL);

			L2PcInstance[] clanMembers = clan.getOnlineMembers(activeChar.getName());
            PledgeShowMemberListAdd ps = new PledgeShowMemberListAdd(activeChar);
			for (int i = 0; i < clanMembers.length; i++)
			{
				clanMembers[i].sendPacket(ps);
				clanMembers[i].sendPacket(msg);
			}
		}
	}

	/**
	 * @param string
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String getText(String string)
	{
		try {
			String result = new String(Base64.decode(string), "UTF-8"); 
			return result;
		} catch (UnsupportedEncodingException e) {
			// huh, UTF-8 is not supported? :)
			return null;
		}
	}
    
    /* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__03_ENTERWORLD;
	}
}
