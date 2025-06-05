package com.festina.gameserver.instancemanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.festina.L2DatabaseFactory;
import com.festina.Config;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.entity.Castle;
import com.festina.status.LoginStatusThread;


public class CastleManager
{
	private static final Logger _log = Logger.getLogger(LoginStatusThread.class.getName());
    // =========================================================
    private static CastleManager _Instance;
    public static final CastleManager getInstance()
    {
        if (_Instance == null)
        {	
		if(Config.DEBUG)
			_log.info("Initializing CastleManager");
            _Instance = new CastleManager();
            _Instance.load();
        }
        return _Instance;
    }
    // =========================================================

    
    // =========================================================
    // Data Field
    private List<Castle> _Castles;
    
    // =========================================================
    // Constructor
    public CastleManager()
    {
    }

    // =========================================================
    // Method - Public
    public final int findNearestCastleIndex(L2Object obj)
    {
    int index = getCastleIndex(obj);
        if (index < 0)
        {
            double closestDistance = 99999999;
            double distance;
            Castle castle;
            for (int i = 0; i < getCastles().size(); i++)
            {
                castle = getCastles().get(i);
                if (castle == null) continue;
                distance = castle.getDistance(obj);
                if (closestDistance > distance)
                {
                    closestDistance = distance;
                    index = i;
                }
            }
        }
        return index;
    }

    // =========================================================
    // Method - Private
    private final void load()
    {
        java.sql.Connection con = null;
        try
        {
            PreparedStatement statement;
            ResultSet rs;

            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("Select id from castle order by id");
            rs = statement.executeQuery();

            while (rs.next())
            {
                getCastles().add(new Castle(rs.getInt("id")));
            }

            statement.close();
	if(Config.DEBUG)
		_log.info("Loaded: " + getCastles().size() + " castles");
        }
        catch (Exception e)
        {
        	_log.info("Exception: loadCastleData(): " + e.getMessage());
            e.printStackTrace();
        }
        
        finally {try { con.close(); } catch (Exception e) {}}
    }
    public final Castle getCastleById(int castleId)
        {
           for (Castle temp : getCastles())
           {
               if (temp.getCastleId() == castleId)
                   return temp;
           }
            return null;
        }
        
        public final Castle getCastleByOwner(L2Clan clan)
        {
           for (Castle temp : getCastles())
           {
               if (temp.getOwnerId() == clan.getClanId())
                   return temp;
          }
            return null;
        }
    public final Castle getCastle(String name)
    {
        for (Castle temp : getCastles())
                   {
                       if (temp.getName().equalsIgnoreCase(name.trim()))
                           return temp;
                   }
                    return null;
                }
                
                public final Castle getCastle(int x, int y, int z)
                {
                   for (Castle temp : getCastles())
                  {
                       if (temp.checkIfInZone(x, y, z))
                           return temp;
                   }
                    return null;
                }
            
                public final Castle getCastle(L2Object activeObject) { return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ()); }

    public final int getCastleIndex(int castleId)
    {
        Castle castle;
        for (int i = 0; i < getCastles().size(); i++)
        {
            castle = getCastles().get(i);
            if (castle != null && castle.getCastleId() == castleId) return i;
        }
        return -1;
    }

    public final int getCastleIndex(L2Object activeObject)
        {
           return getCastleIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
        }
    
        public final int getCastleIndex(int x, int y, int z)
    {
        Castle castle;
        for (int i = 0; i < getCastles().size(); i++)
        {
            castle = getCastles().get(i);
            if (castle != null && castle.checkIfInZone(x, y, z)) return i;
        }
        return -1;
    }

    public final List<Castle> getCastles()
    {
        if (_Castles == null) _Castles = new FastList<Castle>();
        return _Castles;
    }
}