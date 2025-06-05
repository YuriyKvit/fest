package com.festina.gameserver.instancemanager;

import java.util.logging.Logger;

import javolution.util.FastList;

import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.zone.type.L2OlympiadStadiumZone;

public class OlympiadStadiaManager
{
    protected static Logger _log = Logger.getLogger(OlympiadStadiaManager.class.getName());

    // =========================================================
    private static OlympiadStadiaManager _instance;
    public static final OlympiadStadiaManager getInstance()
    {
        if (_instance == null)
        {
            _instance = new OlympiadStadiaManager();
        }
        return _instance;
    }
    // =========================================================

    
    // =========================================================
    // Data Field
    private FastList<L2OlympiadStadiumZone> _olympiadStadias;
    
    // =========================================================
    // Constructor
    public OlympiadStadiaManager()
    {
    }

    // =========================================================
    // Method - Public


    public void addStadium(L2OlympiadStadiumZone arena)
    {
        if (_olympiadStadias == null)
            _olympiadStadias = new FastList<L2OlympiadStadiumZone>();
            
            _olympiadStadias.add(arena);
    }

    // =========================================================
    public final L2OlympiadStadiumZone getStadium(L2Character character)
    {
        for (L2OlympiadStadiumZone temp : _olympiadStadias)
            if (temp.isCharacterInZone(character)) return temp;
            
            return null;
    }

    // =========================================================
    // Property - Public
    public final L2OlympiadStadiumZone getOlympiadStadiumById(int olympiadStadiumId)
    {
        for (L2OlympiadStadiumZone temp : _olympiadStadias)
            if (temp.getStadiumId() == olympiadStadiumId) return temp;
        return null;
    }

    
}
