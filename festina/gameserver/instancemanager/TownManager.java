package com.festina.gameserver.instancemanager;

import java.util.logging.Logger;

import javolution.util.FastList;
import com.festina.Config;
import com.festina.gameserver.MapRegionTable;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.entity.Castle;
import com.festina.gameserver.model.zone.type.L2TownZone;
import com.festina.status.LoginStatusThread;

public class TownManager
{
	private static final Logger _log = Logger.getLogger(LoginStatusThread.class.getName());
    // =========================================================
    private static TownManager _Instance;
    public static final TownManager getInstance()
    {
        if (_Instance == null)
        {
		if(Config.DEBUG)
			_log.info("Initializing TownManager");
        	_Instance = new TownManager();
        }
        return _Instance;
    }
    // =========================================================

    
    // =========================================================
    // Data Field
    private FastList<L2TownZone> _towns;
    
    // =========================================================
    // Constructor
    public TownManager()
    {
    }

    public void addTown(L2TownZone arena)
    {
        if (_towns == null)
                       _towns = new FastList<L2TownZone>();
                   
                   _towns.add(arena);
    }

    public final L2TownZone getClosestTown(L2Object activeObject)
    {   switch (MapRegionTable.getInstance().getMapRegion(activeObject.getPosition().getX(), activeObject.getPosition().getY()))
		{
			case 0:
				return getTown(2); // TI
			case 1:
				return getTown(3); // Elven
			case 2:
				return getTown(1); // DE
			case 3:
				return getTown(4); // Orc
			case 4:
				return getTown(6); // Dwarven
			case 5:
				return getTown(7); // Gludio
			case 6:
				return getTown(5); // Gludin
			case 7:
				return getTown(8); // Dion
			case 8:
				return getTown(9); // Giran
			case 9:
				return getTown(10); // Oren
			case 10:
				return getTown(12); // Aden
			case 11:
				return getTown(11); // HV
			case 12:
				return getTown(16); // Floran
			case 13:
				return getTown(15); // Heine
			case 14:
				return getTown(14); // Rune
			case 15:
				return getTown(13); // Goddard
		}

        return getTown(16); // Default to floran
    }
    public final boolean townHasCastleInSeige(int townId)
    {
    	int[] castleidarray = {0,0,0,0,0,0,0,1,2,3,4,0,5,0,0,6,0};
    	int castleIndex= castleidarray[townId] ;
     
    	if ( castleIndex > 0 )
        {
           	Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
           	if (castle != null)
           		return castle.getSiege().getIsInProgress();
        }
        return false;
    }

    public final boolean townHasCastleInSeige(int x, int y)
    {
        int curtown= (MapRegionTable.getInstance().getMapRegion(x, y));
        int[] castleidarray = {0,0,0,0,0,1,0,2,3,4,5,0,0,6,0,0,0};
        //find an instance of the castle for this town.
        int castleIndex = castleidarray[curtown];
        if ( castleIndex > 0 )
        {
        	Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
        	if (castle != null)
        		return castle.getSiege().getIsInProgress();
        }
        return false;
    }

    public final L2TownZone getTown(int townId)
    {
        for (L2TownZone temp : _towns)
            if (temp.getTownId() == townId) return temp;
        return null;
    }

    public final L2TownZone getTown(int x, int y, int z)
    {
        for (L2TownZone temp : _towns)
            if (temp.isInsideZone(x, y, z)) return temp;
        return null;
    }
}
