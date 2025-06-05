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
package com.festina.gameserver.model.quest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.festina.Config;
import com.festina.L2DatabaseFactory;
import com.festina.gameserver.NpcTable;
import com.festina.gameserver.ThreadPoolManager;
import com.festina.gameserver.cache.HtmCache;
import com.festina.gameserver.instancemanager.QuestManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Spawn;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.zone.L2ZoneType;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2NpcTemplate;
import com.festina.util.Rnd;

/**
 * @author Luis Arias
 *
 */
public abstract class Quest
{
	protected static Logger _log = Logger.getLogger(Quest.class.getName());

	/** HashMap containing events from String value of the event */
	private static Map<String, Quest> allEventsS = new FastMap<String, Quest>();

	private final int _questId;
	private final String _name;
	private final String _descr;
	 private final boolean _party;
    private State initialState;
    private Map<String, State> states;
	private static Map<String, FastList<QuestTimer>> _allEventTimers = new FastMap<String, FastList<QuestTimer>>();
	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();
	/**
	 * Return collection view of the values contains in the allEventS
	 * @return Collection<Quest>
	 */
	public static Collection<Quest> findAllEvents() {
		return allEventsS.values();
	}
	
    /**
     * (Constructor)Add values to class variables and put the quest in HashMaps. 
     * @param questId : int pointing out the ID of the quest
     * @param name : String corresponding to the name of the quest
     * @param descr : String for the description of the quest
     */
	public Quest(int questId, String name, String descr)
    {
		this(questId, name, descr, false);
    }
	
	public Quest(int questId, String name, String descr, boolean party)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
		_party = party;
        states = new FastMap<String, State>();
		if (questId != 0) {
            QuestManager.getInstance().getQuests().add(Quest.this);
		} else {
			allEventsS.put(name, this);
		}
    }
	
	public boolean isParty() {
		return _party;
	}
	
	public static enum QuestEventType
	{
		ON_FIRST_TALK(false), // control the first dialog shown by NPCs when they are clicked (some quests must override the default npc action)
		QUEST_START(true), // onTalk action from start npcs
		ON_TALK(true), // onTalk action from npcs participating in a quest
		ON_ATTACK(true), // onAttack action triggered when a mob gets attacked by someone
		ON_KILL(true), // onKill action triggered when a mob gets killed.
		ON_SPAWN(true), // onSpawn action triggered when an NPC is spawned or respawned.
		ON_SKILL_SEE(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_FACTION_CALL(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_AGGRO_RANGE_ENTER(true), // a person came within the Npc/Mob's range
		ON_SPELL_FINISHED(true), // on spell finished action when npc finish casting skill
		ON_SKILL_LEARN(false), // control the AcquireSkill dialog from quest script
		ON_ENTER_ZONE(true), // on zone enter
		ON_EXIT_ZONE(true); // on zone exit
		// control whether this event type is allowed for the same npc template in multiple quests
		// or if the npc must be registered in at most one quest for the specified event
		private boolean _allowMultipleRegistration;

		QuestEventType(boolean allowMultipleRegistration)
		{
			_allowMultipleRegistration = allowMultipleRegistration;
		}

		public boolean isMultipleRegistrationAllowed()
		{
			return _allowMultipleRegistration;
		}
	}
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for the specified Event type.<BR>
	 * <BR>
	 *
	 * @param npcId
	 *            : id of the NPC to register
	 * @param eventType
	 *            : type of event being registered
	 * @return L2NpcTemplate : Npc Template corresponding to the npcId, or null if the id is invalid
	 */
	public L2NpcTemplate addEventId(int npcId, QuestEventType eventType)
	{
		try
		{
			L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
			if (t != null)
				t.addQuestEvent(eventType, this);
			return t;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Return ID of the quest
	 * @return int
	 */
	public int getQuestIntId() {
		return _questId;
	}
	
	/**
	 * Set the initial state of the quest with parameter "state"
	 * @param state
	 */
	public void setInitialState(State state) {
		this.initialState = state;
	}
	
	/**
	 * Add a new QuestState to the database and return it.
	 * @param player
	 * @return QuestState : QuestState created
	 */
	public QuestState newQuestState(L2PcInstance player) {
		QuestState qs = new QuestState(this, player, getInitialState(), false);
		Quest.createQuestInDb(qs);
		return qs;
	}
	
	/**
	 * Return initial state of the quest
	 * @return State
	 */
	public State getInitialState() {
		return initialState;
	}
    
	/**
	 * Return name of the quest
	 * @return String
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * Return description of the quest
	 * @return String
	 */
	public String getDescr() {
		return _descr;
	}
    
	/**
	 * Add a state to the quest
	 * @param state
	 * @return state added
	 */
    public State addState(State state)
    {
        states.put(state.getName(), state);
		return state;
    }
    
    /**
     * Add the quest to the NPC's startQuest
     * @param npcId
     * @return L2NpcTemplate : Start NPC
     */
    public L2NpcTemplate addStartNpc(int npcId)
    {
		L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
		if (t != null) {
			t.addStartQuests(this);
		}
		return t;
    }
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.<BR><BR>
	 * @param talkId : ID of the NPC
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate addTalkId(int talkId)
	{
		return addEventId(talkId, Quest.QuestEventType.ON_TALK);
	}
    
	// these are methods to call from java
    public final boolean notifyAttack(L2NpcInstance npc, QuestState qs) {
        String res = null;
        try { res = onAttack(npc, qs); } catch (Exception e) { return showError(qs, e); }
        return showResult(qs, res);
    } 
    public final boolean notifyDeath(L2NpcInstance npc, L2Character character, QuestState qs) {
        String res = null;
        try { res = onDeath(npc, character, qs); } catch (Exception e) { return showError(qs, e); }
        return showResult(qs, res);
    } 
    public final boolean notifyEvent(String event, QuestState qs) {
        String res = null;
        try { res = onEvent(event, qs); } catch (Exception e) { return showError(qs, e); }
        return showResult(qs, res);
    } 
	public final boolean notifyKill (L2NpcInstance npc, QuestState qs) {
		String res = null;
		try { res = onKill(npc, qs); } catch (Exception e) { return showError(qs, e); }
		return showResult(qs, res);
	}
	public final boolean notifyTalk (L2NpcInstance npc, QuestState qs) {
		String res = null;
		try { res = onTalk(npc, qs); } catch (Exception e) { return showError(qs, e); }
        qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());
		return showResult(qs, res);
	}

	// these are methods that java calls to invoke scripts
    public String onAttack(L2NpcInstance npc, QuestState qs) { return onEvent("", qs); } 
    public String onDeath (L2NpcInstance npc, L2Character character, QuestState qs) { return onEvent("", qs); }
    public String onEvent(String event, QuestState qs) { return null; } 
    public String onKill (L2NpcInstance npc, QuestState qs) { return onEvent("", qs); }
    public String onTalk (L2NpcInstance npc, QuestState qs) { return onEvent("", qs); }
	
	/**
	 * Show message error to player who has an access level greater than 0
	 * @param qs : QuestState
	 * @param t : Throwable
	 * @return boolean
	 */
	private boolean showError(QuestState qs, Throwable t) {
		_log.log(Level.WARNING, "", t);
		if (qs.getPlayer().getAccessLevel() > 0) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			String res = "<html><body><title>Script error</title>"+sw.toString()+"</body></html>";
			return showResult(qs, res);
		}
		return false;
	}
	
	/**
	 * Show a message to player.<BR><BR>
	 * <U><I>Concept : </I></U><BR>
	 * 3 cases are managed according to the value of the parameter "res" :<BR>
	 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
	 * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
	 * <LI><U>otherwise :</U> the message hold in "res" is shown in chat box</LI>
	 * @param qs : QuestState 
	 * @param res : String pointing out the message to show at the player
	 * @return boolean
	 */
	private boolean showResult(QuestState qs, String res) {
		if (res == null)
			return true;
		if (res.endsWith(".htm")) {
			qs.showHtmlFile(res);
		}
		else if (res.startsWith("<html>")) {
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(res);
			qs.getPlayer().sendPacket(npcReply);
		}
		else {
			SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
			sm.addString(res);
			qs.getPlayer().sendPacket(sm);
		}
		return false;
	}
	
	/**
	 * Add quests to the L2PCInstance of the player.<BR><BR>
	 * <U><I>Action : </U></I><BR>
	 * Add state of quests, drops and variables for quests in the HashMap _quest of L2PcInstance
	 * @param player : Player who is entering the world
	 */
	public static void playerEnter(L2PcInstance player) {

        java.sql.Connection con = null;
        try
        {
	    // Get list of quests owned by the player from database
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            
            PreparedStatement invalidQuestData      = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? and name=?");
            PreparedStatement invalidQuestDataVar   = con.prepareStatement("delete FROM character_quests WHERE char_id=? and name=? and var=?");
            
            statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE char_id=? AND var=?");
            statement.setInt(1, player.getObjectId());
            statement.setString(2, "<state>");
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				
				// Get ID of the quest and ID of its state
				String questId = rs.getString("name");
				String stateId = rs.getString("value");
				
				// Search quest associated with the ID
				Quest q = QuestManager.getInstance().getQuest(questId);
				if (q == null) {
					_log.finer("Unknown quest "+questId+" for player "+player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA){
                        invalidQuestData.setInt(1, player.getObjectId());
                        invalidQuestData.setString(2, questId);
                        invalidQuestData.executeUpdate();
					}
					continue;
				}
				
				// Identify the state of the quest for the player
				boolean completed = false;
				if (stateId.length() > 0 && stateId.charAt(0) == '*') {
					completed = true;
					stateId = stateId.substring(1);
				}
				// Create an object State containing the state of the quest
				State state = q.states.get(stateId);
				if (state == null) {
					_log.finer("Unknown state "+state+" in quest "+questId+" for player "+player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA){
					    invalidQuestData.setInt(1, player.getObjectId());
                        invalidQuestData.setString(2, questId);
                        invalidQuestData.executeUpdate();
					}
					continue;
				}
				// Create a new QuestState for the player that will be added to the player's list of quests
				new QuestState(q, player, state, completed);
			}
			rs.close();
            invalidQuestData.close();
            statement.close();

            // Get list of quests owned by the player from the DB in order to add variables used in the quest.
            statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE char_id=?");
            statement.setInt(1,player.getObjectId());
			rs = statement.executeQuery();
			while (rs.next()) {
				String questId = rs.getString("name");
				String var     = rs.getString("var");
				String value   = rs.getString("value");
				// Get the QuestState saved in the loop before
				QuestState qs = player.getQuestState(questId);
				if (qs == null) {
					_log.finer("Lost variable "+var+" in quest "+questId+" for player "+player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA){
					    invalidQuestDataVar.setInt   (1,player.getObjectId());
                        invalidQuestDataVar.setString(2,questId);
                        invalidQuestDataVar.setString(3,var);
                        invalidQuestDataVar.executeUpdate();
					}
					continue;
				}
				// Add parameter to the quest
				qs.setInternal(var, value);
			}
			rs.close();
            invalidQuestDataVar.close();
            statement.close();
			
		} catch (Exception e) {
			_log.log(Level.WARNING, "could not insert char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
		
		// events
		for (String name : allEventsS.keySet()) {
			player.processQuestEvent(name, "enter");
		}
	}


	/**
	 * Insert in the database the quest for the player.
	 * @param qs : QuestState pointing out the state of the quest
	 * @param var : String designating the name of the variable for the quest
	 * @param value : String designating the value of the variable for the quest
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("INSERT INTO character_quests (char_id,name,var,value) VALUES (?,?,?,?)");
            statement.setInt   (1, qs.getPlayer().getObjectId());
            statement.setString(2, qs.getQuest().getName());
            statement.setString(3, var);
            statement.setString(4, value);
	    statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not insert char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	/**
	 * Update the value of the variable "var" for the quest.<BR><BR>
	 * <U><I>Actions :</I></U><BR>
	 * The selection of the right record is made with :
	 * <LI>char_id = qs.getPlayer().getObjectID()</LI>
	 * <LI>name = qs.getQuest().getName()</LI>
	 * <LI>var = var</LI>
	 * <BR><BR>
	 * The modification made is :
	 * <LI>value = parameter value</LI>
	 * @param qs : Quest State
	 * @param var : String designating the name of the variable for quest
	 * @param value : String designating the value of the variable for quest
	 */
    public static void updateQuestVarInDb(QuestState qs, String var, String value) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE char_id=? AND name=? AND var = ?");
            statement.setString(1, value);
            statement.setInt   (2, qs.getPlayer().getObjectId());
            statement.setString(3, qs.getQuest().getName());
            statement.setString(4, var);
			statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not update char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
    /**
     * Delete a variable of player's quest from the database.
     * @param qs : object QuestState pointing out the player's quest
     * @param var : String designating the variable characterizing the quest
     */
	public static void deleteQuestVarInDb(QuestState qs, String var) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=? AND var=?");
            statement.setInt   (1, qs.getPlayer().getObjectId());
            statement.setString(2, qs.getQuest().getName());
            statement.setString(3, var);
	    statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not delete char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	/**
	 * Delete the player's quest from database.
	 * @param qs : QuestState pointing out the player's quest
	 */
	public static void deleteQuestInDb(QuestState qs) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=?");
            statement.setInt   (1, qs.getPlayer().getObjectId());
            statement.setString(2, qs.getQuest().getName());
			statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not delete char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	/**
	 * Create a record in database for quest.<BR><BR>
	 * <U><I>Actions :</I></U><BR>
	 * Use fucntion createQuestVarInDb() with following parameters :<BR>
	 * <LI>QuestState : parameter sq that puts in fields of database :
	 * 	 <UL type="square">
	 *     <LI>char_id : ID of the player</LI>
	 *     <LI>name : name of the quest</LI>
	 *   </UL>
	 * </LI>
	 * <LI>var : string "&lt;state&gt;" as the name of the variable for the quest</LI>
	 * <LI>val : string corresponding at the ID of the state (in fact, initial state)</LI>
	 * @param qs : QuestState
	 */
	public static void createQuestInDb(QuestState qs) {
		createQuestVarInDb(qs, "<state>", qs.getStateId());
	}
	
	/**
	 * Update informations regarding quest in database.<BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Get ID state of the quest recorded in object qs</LI>
	 * <LI>Test if quest is completed. If true, add a star (*) before the ID state</LI>
	 * <LI>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</LI>
	 * @param qs : QuestState
	 */
	public static void updateQuestInDb(QuestState qs) {
		String val = qs.getStateId();
		if (qs.isCompleted())
			val = "*" + val;
		updateQuestVarInDb(qs, "<state>", val);
	}

	// these are methods to call from java
		public final boolean notifyAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
		{
			String res = null;
			try
			{
				res = onAttack(npc, attacker, damage, isPet);
			}
			catch (Exception e)
			{
				return showError(attacker, e);
			}
			return showResult(attacker, res);
		}

		// these are methods that java calls to invoke scripts
		public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
		{
			return null;
		}

		public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
		{
			return onAttack(npc, attacker, damage, isPet);
		}
		/**
		 * Show message error to player who has an access level greater than 0
		 *
		 * @param player
		 *            : L2PcInstance
		 * @param t
		 *            : Throwable
		 * @return boolean
		 */
		public boolean showError(L2PcInstance player, Throwable t)
		{
			//_log.log(Level.WARNING, this.getScriptFile().getAbsolutePath(), t);
			if (player.isGM())
			{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				pw.close();
				String res = "<html><body><title>Script error</title>" + sw.toString() + "</body></html>";
				return showResult(player, res);
			}
			return false;
		}

		/**
		 * Show a message to player.<BR>
		 * <BR>
		 * <U><I>Concept : </I></U><BR>
		 * 3 cases are managed according to the value of the parameter "res" :<BR>
		 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI> <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI> <LI><U>otherwise :</U> the message held in "res" is shown in chat box</LI>
		 *
		 * @param qs
		 *            : QuestState
		 * @param res
		 *            : String pointing out the message to show at the player
		 * @return boolean
		 */
		public boolean showResult(L2PcInstance player, String res)
		{
			if (res == null || res.isEmpty() || player == null)
				return true;
			if (res.endsWith(".htm"))
				showHtmlFile(player, res);
			else if (res.startsWith("<html>"))
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(res);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
				player.sendPacket(new ActionFailed());
			} else
				player.sendMessage(res);
			return false;
		}

		/**
		 * Show HTML file to client
		 *
		 * @param fileName
		 * @return String : message sent to client
		 */
		public String showHtmlFile(L2PcInstance player, String fileName)
		{
			String questId = getName();
			// Create handler to file linked to the quest
			String directory = getDescr().toLowerCase();
			String content = HtmCache.getInstance().getHtm("data/scripts/" + directory + "/" + questId + "/" + fileName);
			if (content == null)
				content = HtmCache.getInstance().getHtmForce("data/scripts/quests/" + questId + "/" + fileName);
			if (player != null && player.getTarget() != null)
				content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
			// Send message to client if message not empty
			if (content != null)
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
				player.sendPacket(new ActionFailed());
			}
			return content;
		}

		public final boolean notifyEnterZone(L2Character character, L2ZoneType zone)
		{
			L2PcInstance player = (L2PcInstance)character;
			String res = null;
			try
			{
				res = this.onEnterZone(character, zone);
			}
			catch (Exception e)
			{
				if (player != null)
					return showError(player, e);
			}
			if (player != null)
				return showResult(player, res);
			return true;
		}
		
		public String onEnterZone(L2Character character, L2ZoneType zone)
		{
			return null;
		}

		public final boolean notifyExitZone(L2Character character, L2ZoneType zone)
		{
			L2PcInstance player = (L2PcInstance)character;
			String res = null;
			try
			{
				res = this.onExitZone(character, zone);
			}
			catch (Exception e)
			{
				if (player != null)
					return showError(player, e);
			}
			if (player != null)
				return showResult(player, res);
			return true;
		}

		public String onExitZone(L2Character character, L2ZoneType zone)
		{
			return null;
		}
		// Method - Public
	    /**
	     * Add a temporary (quest) spawn
	     * Return instance of newly spawned npc
	     */
		public L2NpcInstance addSpawn(int npcId, L2Character cha)
		{
		    return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0);
		}
		
		/**
		 * Add a temporary (quest) spawn Return instance of newly spawned npc with summon animation
		 */
		public L2NpcInstance addSpawn(int npcId, L2Character cha, boolean isSummonSpawn)
		{
			return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, isSummonSpawn);
		}

		public L2NpcInstance addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffSet, int despawnDelay)
		{
			return addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay, false);
		}
		
	    public L2NpcInstance addSpawn(int npcId, int x, int y, int z,int heading, boolean randomOffset, int despawnDelay, boolean isSummonSpawn)
	    {
	    	L2NpcInstance result = null;
	        try 
	        {
	            L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
	            if (template != null)
	            {
	                // Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
	            	// reaches here, xyz have become 0!  Also, a questdev might have purposely set xy to 0,0...however,
	            	// the spawn code is coded such that if x=y=0, it looks into location for the spawn loc!  This will NOT work
	            	// with quest spawns!  For both of the above cases, we need a fail-safe spawn.  For this, we use the 
	                // default spawn location, which is at the player's loc.
	                if ((x == 0) && (y == 0))
	                {
	                	_log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
	                	return null;
	                }
	                if (randomOffset)
	                {
	                    int offset;

	                    offset = Rnd.get(2); // Get the direction of the offset
	                    if (offset == 0) {offset = -1;} // make offset negative
	                    offset *= Rnd.get(50, 100);
	                    x += offset;

	                    offset = Rnd.get(2); // Get the direction of the offset
	                    if (offset == 0) {offset = -1;} // make offset negative
	                    offset *= Rnd.get(50, 100); 
	                    y += offset;
	                }       
	                L2Spawn spawn = new L2Spawn(template);
	                spawn.setHeading(heading);
	                spawn.setLocx(x);
	                spawn.setLocy(y);
	                spawn.setLocz(z+20);
	                spawn.stopRespawn();
	                result = spawn.spawnOne();

		            if (despawnDelay > 0)
			            ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);
		            
		            return result;
	            }
	        }
	        catch (Exception e1)
	        {
	        	_log.warning("Could not spawn Npc " + npcId);
	        	e1.printStackTrace();
	        }
	          
	        return null;
	    }
	    public class DeSpawnScheduleTimerTask implements Runnable
	    {
	        L2NpcInstance _npc = null;
	        public DeSpawnScheduleTimerTask(L2NpcInstance npc)
	        {
	        	_npc = npc;
	        }
	        

			public void run()
	        {
	           _npc.onDecay();
	        }
	    }
	    public void startQuestTimer(String name, long time, L2NpcInstance npc, L2PcInstance player)
		{
			startQuestTimer(name, time, npc, player, false);
		}
	    
	    public void startQuestTimer(String name, long time, L2NpcInstance npc, L2PcInstance player, boolean repeating)
	    {
	        // Add quest timer if timer doesn't already exist
	    	FastList<QuestTimer> timers = getQuestTimers(name);
	    	// no timer exists with the same name, at all 
	        if (timers == null)
	        {
	        	timers = new FastList<QuestTimer>();
	            timers.add(new QuestTimer(this, name, time, npc, player, repeating));
	        	_allEventTimers.put(name, timers);
	        }
	        // a timer with this name exists, but may not be for the same set of npc and player
	        else
	        {
	        	// if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
	        	// nulls act as wildcards
	        	if(getQuestTimer(name, npc, player)==null)
	        	{
	        		try
	    			{
	    				_rwLock.writeLock().lock();
	    				timers.add(new QuestTimer(this, name, time, npc, player, repeating));
	    			}
	    			finally
	    			{
	    				_rwLock.writeLock().unlock();
	    			}
	        	}
	        }
	    }
	    
	    public QuestTimer getQuestTimer(String name, L2NpcInstance npc, L2PcInstance player)
	    {
	    	FastList<QuestTimer> qt = getQuestTimers(name);
	    	if (qt == null || qt.isEmpty())
				return null;
	    	
	    	try
			{
				_rwLock.readLock().lock();
				for (QuestTimer timer : qt)
					if (timer != null)
						if (timer.isMatch(this, name, npc, player))
							return timer;
			}
			finally
			{
				_rwLock.readLock().unlock();
			}
			return null;
	    }

	    public FastList<QuestTimer> getQuestTimers(String name)
	    {
	    	return _allEventTimers.get(name);
	    }
	    
	    public void cancelQuestTimers(String name)
		{
			FastList<QuestTimer> timers = getQuestTimers(name);
			if (timers == null)
				return;
			try
			{
				_rwLock.writeLock().lock();
				for (QuestTimer timer : timers)
					if (timer != null)
						timer.cancel();
			}
			finally
			{
				_rwLock.writeLock().unlock();
			}
		}
	    
	    public void cancelQuestTimer(String name, L2NpcInstance npc, L2PcInstance player)
	    {
	    	QuestTimer timer = getQuestTimer(name, npc, player);
	    	if (timer != null)
	    		timer.cancel();
	    }

	    public void removeQuestTimer(QuestTimer timer)
	    {
	    	if (timer == null)
	    		return;
	    	FastList<QuestTimer> timers = getQuestTimers(timer.getName());
	    	if (timers == null)
	    		return;
	    	
	    	try
			{
				_rwLock.writeLock().lock();
				timers.remove(timer);
			}
			finally
			{
				_rwLock.writeLock().unlock();
			}  		
	    }

		public final boolean notifyEvent(String event, L2NpcInstance npc, L2PcInstance player)
		{
			String res = null;
			try
			{
				res = onAdvEvent(event, npc, player);
			}
			catch(Exception e)
			{
				return showError(player, e);
			}

			return showResult(player, res);
		}
	    public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player) 
	    {
	    	// if not overriden by a subclass, then default to the returned value of the simpler (and older) onEvent override
	    	// if the player has a state, use it as parameter in the next call, else return null
	    	QuestState qs = player.getQuestState(getName());
	    	if (qs != null)
	    		return onEvent(event, qs);
	    	return null; 
	    } 
		public final boolean notifyFactionCall(L2NpcInstance npc, L2NpcInstance caller, L2PcInstance attacker, boolean isPet)
		{
			String res = null;
			try
			{
				res = onFactionCall(npc, caller, attacker, isPet);
			}
			catch(Exception e)
			{
				return showError(attacker, e);
			}
			return showResult(attacker, res);
		}
		public String onFactionCall(L2NpcInstance npc, L2NpcInstance caller, L2PcInstance attacker, boolean isPet)
		{
			return null;
		}
}
