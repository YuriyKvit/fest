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
package com.festina.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.L2DatabaseFactory;
import com.festina.L2JFestina;
import com.festina.Server;
import com.festina.gameserver.cache.CrestCache;
import com.festina.gameserver.cache.HtmCache;
import com.festina.gameserver.communitybbs.Manager.*;
import com.festina.gameserver.datatables.ZoneData;
import com.festina.gameserver.handler.HandlerLists;
import com.festina.gameserver.idfactory.IdFactory;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.instancemanager.CastleManorManager; 
import com.festina.gameserver.instancemanager.ClanHallManager;
import com.festina.gameserver.instancemanager.DayNightSpawnManager;
import com.festina.gameserver.instancemanager.DimensionalRiftManager;
import com.festina.gameserver.instancemanager.GrandBossManager;
import com.festina.gameserver.instancemanager.Manager;
import com.festina.gameserver.instancemanager.RaidBossSpawnManager;
import com.festina.gameserver.instancemanager.SiegeManager;
import com.festina.gameserver.model.AutoChatHandler;
import com.festina.gameserver.model.AutoSpawnHandler;
import com.festina.gameserver.model.L2Manor;
import com.festina.gameserver.model.L2PetDataTable;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.entity.Hero;
import com.festina.gameserver.model.quest.engine.CompiledScriptCache;
import com.festina.gameserver.model.quest.engine.L2ScriptEngineManager;
import com.festina.gameserver.pathfinding.geonodes.GeoPathFinding;
import com.festina.gameserver.taskmanager.TaskManager;
import com.festina.gameserver.util.DynamicExtension;
import com.festina.gameserver.util.FloodProtector;
import com.festina.status.Status;

/**
 * This class ...
 *
 * @version $Revision: 1.29.2.15.2.19 $ $Date: 2005/04/05 19:41:23 $
 */
public class GameServer
{
	private static final Logger _log = Logger.getLogger(GameServer.class.getName());
	private final SelectorThread _selectorThread;
	private final SkillTable _skillTable;
	private final ItemTable _itemTable;
	private final NpcTable _npcTable;
	private final HennaTable _hennaTable;
	private final IdFactory _idFactory;
	public static GameServer gameServer;

	private final Shutdown _shutdownHandler;
    private final DoorTable _doorTable;
    private final SevenSigns _sevenSignsEngine;
    private final AutoChatHandler _autoChatHandler;
	private final AutoSpawnHandler _autoSpawnHandler;
	private LoginServerThread _loginThread;
    private final HelperBuffTable _helperBuffTable;

	public static Status statusServer;
	@SuppressWarnings("unused")
	private final ThreadPoolManager _threadpools;

    public static final Calendar DateTimeServerStarted = Calendar.getInstance();
    
    public long getUsedMemoryMB()
	{
    	return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1048576; // 1024 * 1024 = 1048576;
	}

    public SelectorThread getSelectorThread()
    {
    	return _selectorThread;
    }

	public GameServer() throws Exception
	{
        gameServer = this;
		_log.finest("used mem:" + getUsedMemoryMB()+"MB" );
		_idFactory = IdFactory.getInstance();
        if (!_idFactory.isInitialized())
        {
            _log.severe("Could not read object IDs from DB. Please Check Your Data.");
            throw new Exception("Could not initialize the ID factory");
        }

        _threadpools = ThreadPoolManager.getInstance();

		new File(Config.DATAPACK_ROOT, "data/clans").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();
        new File(Config.DATAPACK_ROOT, "data/pathnode").mkdirs();

		// start game time control early
		GameTimeController.getInstance();
		FloodProtector.getInstance();
		// keep the references of Singletons to prevent garbage collection
		CharNameTable.getInstance();

		_itemTable = ItemTable.getInstance();
		if (!_itemTable.isInitialized())
		{
		    _log.severe("Could not find the extraced files. Please Check Your Data.");
		    throw new Exception("Could not initialize the item table");
		}
		
		TradeController.getInstance();
		ArmorSetsTable.getInstance();
		_skillTable = SkillTable.getInstance();
		if (!_skillTable.isInitialized())
		{
		    _log.severe("Could not find the extraced files. Please Check Your Data.");
		    throw new Exception("Could not initialize the skill table");
		}
		GrandBossManager.getInstance();	
		RecipeController.getInstance();

		SkillTreeTable.getInstance();
		FishTable.getInstance();
		SkillSpellbookTable.getInstance();
		CharTemplateTable.getInstance();
        
        //Call to load caches
        HtmCache.getInstance();
        CrestCache.getInstance();
        
		_npcTable = NpcTable.getInstance();
        
		if (!_npcTable.isInitialized())
		{
		    _log.severe("Could not find the extraced files. Please Check Your Data.");
		    throw new Exception("Could not initialize the npc table");
		}
        
		_hennaTable = HennaTable.getInstance();
        
		if (!_hennaTable.isInitialized())
		{
		   throw new Exception("Could not initialize the Henna Table");
		}
        
		HennaTreeTable.getInstance();
        
		if (!_hennaTable.isInitialized())
		{
		   throw new Exception("Could not initialize the Henna Tree Table");
		}
        
        _helperBuffTable = HelperBuffTable.getInstance();
        
        if (!_helperBuffTable.isInitialized())
        {
           throw new Exception("Could not initialize the Helper Buff Table");
        }
        

		TeleportLocationTable.getInstance();
		LevelUpData.getInstance();
		L2World.getInstance();
		ClanHallManager.getInstance();
		ZoneData.getInstance();
        SpawnTable.getInstance();
        RaidBossSpawnManager.getInstance();
        CastleManorManager.getInstance(); 
        DayNightSpawnManager.getInstance().notifyChangeMode();
        DimensionalRiftManager.getInstance();
		Announcements.getInstance();
		MapRegionTable.getInstance();
		EventDroplist.getInstance();
        
		/* Load Manor data */
	    L2Manor.getInstance();
		
        if (Config.AUTODESTROY_ITEM_AFTER > 0)
    	    ItemsAutoDestroy.getInstance();
        
        MonsterRace.getInstance();
        
		_doorTable = DoorTable.getInstance();
		_doorTable.parseData();
        StaticObjects.getInstance();
        
		_sevenSignsEngine = SevenSigns.getInstance();
        SevenSignsFestival.getInstance();
		_autoSpawnHandler = AutoSpawnHandler.getInstance();
		_autoChatHandler = AutoChatHandler.getInstance();

        // Spawn the Orators/Preachers if in the Seal Validation period.
        _sevenSignsEngine.spawnSevenSignsNPC();
        GeoData.getInstance();
        if (Config.GEODATA == 2)  
        	GeoPathFinding.getInstance();
     	// Load clan hall data before zone data
		//_cHManager = ClanHallManager.getInstance();
	   CastleManager.getInstance();
	   SiegeManager.getInstance();
        Olympiad.getInstance();
        Hero.getInstance();
		if(Config.DEBUG)
		{
		_log.config("AutoChatHandler: Loaded " + _autoChatHandler.size() + " handlers in total.");
		_log.config("AutoSpawnHandler: Loaded " + _autoSpawnHandler.size() + " handlers in total.");
		}

		HandlerLists.registerHandlers();
		L2ScriptEngineManager.getInstance();
		try
				{
					_log.info("Loading Server Scripts");
					File scripts = new File(Config.DATAPACK_ROOT + "/data/scripts.cfg");
					L2ScriptEngineManager.getInstance().executeScriptList(scripts);
				}
				catch (IOException ioe)
				{
					_log.severe("Failed loading scripts.cfg, no script going to be loaded");
				}
				try
				{
					CompiledScriptCache compiledScriptCache = L2ScriptEngineManager.getInstance().getCompiledScriptCache();
					if (compiledScriptCache == null)
					{
						_log.info("Compiled Scripts Cache is disabled.");
					}
					else
					{
						compiledScriptCache.purge();
						
						if (compiledScriptCache.isModified())
						{
							compiledScriptCache.save();
							_log.info("Compiled Scripts Cache was saved.");
						}
						else
						{
							_log.info("Compiled Scripts Cache is up-to-date.");
						}
					}
					
				}
				catch (IOException e)
				{
					_log.log(Level.SEVERE, "Failed to store Compiled Scripts Cache.", e);
				}
        
		TaskManager.getInstance();
        
		GmListTable.getInstance();

        // read pet stats from db
        L2PetDataTable.getInstance().loadPetsData(); 

        Universe.getInstance();

        Manager.loadAll();
        
		_shutdownHandler = Shutdown.getInstance();
		Runtime.getRuntime().addShutdownHook(_shutdownHandler);

		try
        {
            _doorTable.getDoor(24190001).openMe();
            _doorTable.getDoor(24190002).openMe();
            _doorTable.getDoor(24190003).openMe();
            _doorTable.getDoor(24190004).openMe();
            _doorTable.getDoor(23180001).openMe();
            _doorTable.getDoor(23180002).openMe();
            _doorTable.getDoor(23180003).openMe();
            _doorTable.getDoor(23180004).openMe();
            _doorTable.getDoor(23180005).openMe();
            _doorTable.getDoor(23180006).openMe();
            
            _doorTable.checkAutoOpen();
        } 
        catch (NullPointerException e)
        {
            _log.warning("Door.csv does not contain the right door info. Update door.csv");
            e.printStackTrace();
        }
        ClanTable.getInstance();
        ForumsBBSManager.getInstance();
		_log.info("CustomBBSManager loading");
		_log.info("CustomBBSManager: " + Config.COMMUNITY_TYPE);
		if (Config.COMMUNITY_TYPE.equals("Full")) {
			CustomBBSManager.init();
			_log.info("CustomBBSManager loaded");
		}
		if(Config.DEBUG) {
			_log.config("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());
		}
        // initialize the dynamic extension loader
        try {
            DynamicExtension.getInstance();
        } catch (Exception ex) {
            _log.log(Level.WARNING, "DynamicExtension could not be loaded and initialized", ex);
        }

		System.gc();
		// maxMemory is the upper limit the jvm can use, totalMemory the size of the current allocation pool, freeMemory the unused memory in the allocation pool
		long freeMem = (Runtime.getRuntime().maxMemory()-Runtime.getRuntime().totalMemory()+Runtime.getRuntime().freeMemory()) / 1048576; // 1024 * 1024 = 1048576; 
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		_log.info("GameServer Started, free memory "+freeMem+" Mb of "+totalMem+" Mb");
		
		_loginThread = LoginServerThread.getInstance();
		_loginThread.start();
		
		_selectorThread = SelectorThread.getInstance();
		_selectorThread.start();
		if(Config.DEBUG) {
			_log.config("Maximum Numbers of Connected Players: " + Config.MAXIMUM_ONLINE_USERS);
		}
		L2JFestina.info();
	}
	
	public static void main(String[] args) throws Exception
    {
		Server.SERVER_MODE = Server.MODE_GAMESERVER;
		final String LOG_FOLDER = "log"; // Name of folder for log file
		final String LOG_NAME   = "./log.cfg"; // Name of log file
		
		/* Main ***/
		// Create log folder
		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER); 
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory
		InputStream is =  new FileInputStream(new File(LOG_NAME));  
		LogManager.getLogManager().readConfiguration(is);
		is.close();
		
		// Initialize config 
		Config.load();
		L2DatabaseFactory.getInstance();
		gameServer = new GameServer();
		
		if ( Config.IS_TELNET_ENABLED ) {
		    statusServer = new Status(Server.SERVER_MODE);
		    statusServer.start();
		}
		else {
			_log.info("Telnet server is currently disabled.");
		}
    }
}
