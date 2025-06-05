package com.festina.gameserver.instancemanager;


import java.util.logging.Logger;

import javolution.util.FastList;
import com.festina.Config;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.zone.type.L2ArenaZone;
import com.festina.status.LoginStatusThread;

public class ArenaManager
{
	private static final Logger _log = Logger.getLogger(LoginStatusThread.class.getName());
    // =========================================================
    private static ArenaManager _Instance;
    public static final ArenaManager getInstance()
    {
        if (_Instance == null)
        {
		if(Config.DEBUG)
			_log.info("Initializing ArenaManager");
        	_Instance = new ArenaManager();
        }
        return _Instance;
    }
    // =========================================================

    
    // =========================================================
    // Data Field
    private FastList<L2ArenaZone> _arenas;
    
    // =========================================================
    // Constructor
    public ArenaManager()
    {
    }

    public void addArena(L2ArenaZone arena)
    {
        if (_arenas == null)
            _arenas = new FastList<L2ArenaZone>();
                   
            _arenas.add(arena);
    }

    // =========================================================
    // Method - Private
    public final L2ArenaZone getArena(L2Character character)
    {
        for (L2ArenaZone temp : _arenas)
        if (temp.isCharacterInZone(character)) return temp;
        
           return null;
    }
}
