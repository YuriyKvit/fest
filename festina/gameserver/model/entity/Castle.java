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
package com.festina.gameserver.model.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.festina.L2DatabaseFactory;
import com.festina.gameserver.Announcements;
import com.festina.gameserver.CastleUpdater;
import com.festina.gameserver.ClanTable;
import com.festina.gameserver.DoorTable;
import com.festina.gameserver.ThreadPoolManager;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.instancemanager.CastleManorManager;
import com.festina.gameserver.instancemanager.CastleManorManager.CropProcure;
import com.festina.gameserver.instancemanager.CastleManorManager.SeedProduction;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.L2ClanMember;
import com.festina.gameserver.model.L2Manor;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2DoorInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.zone.type.L2CastleZone;
import com.festina.gameserver.serverpackets.PledgeShowMemberListAll;

public class Castle
{
    protected static Logger _log = Logger.getLogger(Castle.class.getName());
    
	// =========================================================
    // Data Field
    private FastList<CropProcure>    _procure        = new FastList<CropProcure>(); 
    private FastList<SeedProduction> _production     = new FastList<SeedProduction>(); 
    private FastList<CropProcure>    _procureNext    = new FastList<CropProcure>(); 
    private FastList<SeedProduction> _productionNext = new FastList<SeedProduction>(); 
    private boolean _isNextPeriodApproved = false; 

    private static final String CASTLE_MANOR_DELETE_PRODUCTION =  
            "DELETE FROM castle_manor_production WHERE castle_id=?;"; 
    private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD =  
            "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;"; 
    private static final String CASTLE_MANOR_DELETE_PROCURE =  
            "DELETE FROM castle_manor_procure WHERE castle_id=?;"; 
    private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD =  
            "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;"; 
    private static final String CASTLE_UPDATE_CROP = 
            "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?"; 
    private static final String CASTLE_UPDATE_SEED = 
            "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";

	// =========================================================
    // Data Field
	private int _CastleId                      = 0;
	private List<L2DoorInstance> _Doors        = new FastList<L2DoorInstance>();
	private List<String> _DoorDefault          = new FastList<String>();
	private String _Name                       = "";
	private int _OwnerId                       = 0;
	private Siege _Siege                       = null;
	private Calendar _SiegeDate;
	private int _SiegeDayOfWeek                = 7; // Default to saturday
	private int _SiegeHourOfDay                = 20; // Default to 8 pm server time
	private int _TaxPercent                    = 0;
	private double _TaxRate                    = 0;
	private int _Treasury                      = 0;
	private L2CastleZone _zone;
    private L2Clan _formerOwner                            = null;

	// =========================================================
	// Constructor
	public Castle(int castleId)
	{
		_CastleId = castleId;
        this.load();
		this.loadDoor();
	}

	// =========================================================
	// Method - Public
	// This method add to the treasury
    /** Add amount to castle instance's treasury (warehouse). */
	public void addToTreasury(int amount)
	{
        if (getOwnerId() <= 0) return;

        if (!_Name.equalsIgnoreCase("aden"))    // If current castle instance is not Aden
        {
            Castle aden = CastleManager.getInstance().getCastle("aden");
            if (aden != null)
            {
                int adenTax = (int)(amount * aden.getTaxRate());        // Find out what Aden gets from the current castle instance's income
                if (aden.getOwnerId() > 0) aden.addToTreasury(adenTax); // Only bother to really add the tax to the treasury if not npc owned
                    
                amount -= adenTax; // Subtract Aden's income from current castle instance's income
            }
        }
	    
        addToTreasuryNoTax(amount); 
        } 

    /** Add amount to castle instance's treasury (warehouse), no tax paying. */ 
    public void addToTreasuryNoTax(int amount) 
    { 
        if (getOwnerId() <= 0) return; 

        if (Integer.MAX_VALUE - amount < _Treasury) 
            return; 

        _Treasury += amount; // Add to the current treasury total.  Use "-" to substract from treasury
	    
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("Update castle set treasury = ? where id = ?");
            statement.setInt(1, getTreasury());
            statement.setInt(2, getCastleId());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {} 
        finally {try { con.close(); } catch (Exception e) {}}
	}
	
	/**
	 * Move non clan members off castle area and to nearest town.<BR><BR>
	 */
    public void banishForeigners()
    {
        _zone.banishForeigners(getOwnerId());
    }

    /**
     * Return true if object is inside the zone
     */
    public boolean checkIfInZone(int x, int y, int z)
    {
        return _zone.isInsideZone(x, y, z);
    }

    /**
     * Return true if object is inside the zone
     */
    public void setZone(L2CastleZone zone)
    {
        _zone = zone;
    }
     
    public L2CastleZone getZone()
    {
        return _zone;
    }

    public double getDistance(L2Object obj)
    {
        return _zone.getDistanceToZone(obj);
    }	
    
	public void closeDoor(L2PcInstance activeChar, int doorId)
	{
	    openCloseDoor(activeChar, doorId, false);
	}

	public void openDoor(L2PcInstance activeChar, int doorId)
	{
	    openCloseDoor(activeChar, doorId, true);
	}

	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
	    if (activeChar.getClanId() != getOwnerId())
	        return;

	    L2DoorInstance door = getDoor(doorId);
        if (door != null)
        {
            if (open)
                door.openMe();
            else
                door.closeMe();
        }
	}
	
	// This method is used to begin removing all castle upgrades
	public void removeUpgrade()
	{
	    removeDoorUpgrade();
	}
	
	// This method updates the castle tax rate
	public void setOwner(L2Clan clan)
	{
		// Remove old owner
	    if (getOwnerId() > 0 && (clan == null || clan.getClanId() != getOwnerId()))
	    {
	        L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId());			// Try to find clan instance 
			if (oldOwner != null)
			{
			    if (_formerOwner == null)
			        _formerOwner = oldOwner;
			    oldOwner.setHasCastle(0);												// Unset has castle flag for old owner
        		new Announcements().announceToAll(oldOwner.getName() + " has lost " + getName() + " castle!");
			}							
	    }

	    updateOwnerInDB(clan);															// Update in database

	    if (getSiege().getIsInProgress())												// If siege in progress
        	getSiege().midVictory();													// Mid victory phase of siege
	}

	// This method updates the castle tax rate
	public void setTaxPercent(L2PcInstance activeChar, int taxPercent)
	{
	    if (taxPercent < 0 || taxPercent > 15)
	    {
	        activeChar.sendMessage("Tax value must be between 1 and 15.");
	        return;
	    }
	    
        _TaxPercent = taxPercent;
        _TaxRate = _TaxPercent / 100.0;

        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("Update castle set taxPercent = ? where id = ?");
            statement.setInt(1, taxPercent);
            statement.setInt(2, getCastleId());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {} 
        finally {try { con.close(); } catch (Exception e) {}}

        activeChar.sendMessage(getName() + " castle tax changed to " + taxPercent + "%.");
	}
    
	/**
	 * Respawn all doors on castle grounds<BR><BR>
	 */
	public void spawnDoor()
    {
	    spawnDoor(false);
    }
    
	/**
	 * Respawn all doors on castle grounds<BR><BR>
	 */
	public void spawnDoor(boolean isDoorWeak)
    {
	    for (int i = 0; i < getDoors().size(); i++)
        {
            L2DoorInstance door = getDoors().get(i);
            if (door.getCurrentHp() <= 0)
            {
                door.decayMe();	// Kill current if not killed already
                door = DoorTable.parseList(_DoorDefault.get(i));
                if (isDoorWeak)
                    door.setCurrentHp(door.getMaxHp() / 2);
    			door.spawnMe(door.getX(), door.getY(),door.getZ());
    			getDoors().set(i, door);
            }
            else if (door.getOpen() == 0)
                door.closeMe();
        }
		loadDoorUpgrade(); // Check for any upgrade the doors may have
    }

	// This method upgrade door
	public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
	{
        L2DoorInstance door = getDoor(doorId);
	    if (door == null)
	        return;
	    
        if (door != null && door.getDoorId() == doorId)
        {
        	door.setCurrentHp(door.getMaxHp() + hp);

        	saveDoorUpgrade(doorId, hp, pDef, mDef);
            return;
        }
	}
	
	// =========================================================
	// Method - Private
	// This method loads castle
	private void load()
	{
        java.sql.Connection con = null;
        try
        {
            PreparedStatement statement;
            ResultSet rs;

            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("Select * from castle where id = ?");
            statement.setInt(1, getCastleId());
            rs = statement.executeQuery();

            while (rs.next())
            {
        	    _Name = rs.getString("name");
        	    //_OwnerId = rs.getInt("ownerId");

        	    _SiegeDate = Calendar.getInstance();
        	    _SiegeDate.setTimeInMillis(rs.getLong("siegeDate"));
        	    
        	    _SiegeDayOfWeek = rs.getInt("siegeDayOfWeek");
        	    if (_SiegeDayOfWeek < 1 || _SiegeDayOfWeek > 7)
        	        _SiegeDayOfWeek = 7;

        	    _SiegeHourOfDay = rs.getInt("siegeHourOfDay");
        	    if (_SiegeHourOfDay < 0 || _SiegeHourOfDay > 23)
        	        _SiegeHourOfDay = 20;

        	    _TaxPercent = rs.getInt("taxPercent");
        	    _Treasury = rs.getInt("treasury");
            }

            statement.close();

            _TaxRate = _TaxPercent / 100.0;

            statement = con.prepareStatement("Select clan_id from clan_data where hasCastle = ?");
            statement.setInt(1, getCastleId());
            rs = statement.executeQuery();

            while (rs.next())
            {
        	    _OwnerId = rs.getInt("clan_id");
            }

            if (getOwnerId() > 0)
            {
                L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());                        // Try to find clan instance 
                ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000);     // Schedule owner tasks to start running 
            }

            statement.close();
        }
        catch (Exception e)
        {
        	_log.info("Exception: loadCastleData(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
	}

	// This method loads castle door data from database
	private void loadDoor()
	{
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("Select * from castle_door where castleId = ?");
            statement.setInt(1, getCastleId());
            ResultSet rs = statement.executeQuery();

            while (rs.next())
            {
                // Create list of the door default for use when respawning dead doors
                _DoorDefault.add(rs.getString("name") 
                        + ";" + rs.getInt("id") 
                        + ";" + rs.getInt("x") 
                        + ";" + rs.getInt("y") 
                        + ";" + rs.getInt("z") 
                        + ";" + rs.getInt("hp") 
                        + ";" + rs.getInt("pDef") 
                        + ";" + rs.getInt("mDef"));

                L2DoorInstance door = DoorTable.parseList(_DoorDefault.get(_DoorDefault.size() - 1));
				door.spawnMe(door.getX(), door.getY(),door.getZ());				
                _Doors.add(door);
            }

            statement.close();
        }
        catch (Exception e)
        {
        	_log.info("Exception: loadCastleDoor(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
	}

	// This method loads castle door upgrade data from database
	private void loadDoorUpgrade()
	{
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("Select * from castle_doorupgrade where doorId in (Select Id from castle_door where castleId = ?)");
            statement.setInt(1, getCastleId());
            ResultSet rs = statement.executeQuery();

            while (rs.next())
            {
                upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
            }

            statement.close();
        }
        catch (Exception e)
        {
        	_log.info("Exception: loadCastleDoorUpgrade(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
	}

	private void removeDoorUpgrade()
	{
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("delete from castle_doorupgrade where doorId in (select id from castle_door where castleId=?)");
            statement.setInt(1, getCastleId());
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
        	_log.info("Exception: removeDoorUpgrade(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
	}

	private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
	{
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("INSERT INTO castle_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)");
            statement.setInt(1, doorId);
            statement.setInt(2, hp);
            statement.setInt(3, pDef);
            statement.setInt(4, mDef);
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
        	_log.info("Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage());
            e.printStackTrace();
        } 
        finally 
        {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	private void updateOwnerInDB(L2Clan clan)
	{
		if (clan != null)
		    _OwnerId = clan.getClanId();	// Update owner id property
		else
			_OwnerId = 0;					// Remove owner

	    java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;

            // ============================================================================
            // NEED TO REMOVE HAS CASTLE FLAG FROM CLAN_DATA
            // SHOULD BE CHECKED FROM CASTLE TABLE
            statement = con.prepareStatement("UPDATE clan_data SET hasCastle=0 WHERE hasCastle=?");
            statement.setInt(1, getCastleId());
            statement.execute();
            statement.close();   

            statement = con.prepareStatement("UPDATE clan_data SET hasCastle=? WHERE clan_id=?");
            statement.setInt(1, getCastleId());
            statement.setInt(2, getOwnerId());
            statement.execute();
            statement.close();   
            // ============================================================================
            
            // Announce to clan memebers
            if (clan != null)
            {
    		    clan.setHasCastle(getCastleId()); // Set has castle flag for new owner
    		    new Announcements().announceToAll(clan.getName() + " has taken " + getName() + " castle!");

    		    for (L2ClanMember member : clan.getMembers())
        		{
        			if (member.isOnline() && member.getPlayerInstance() != null)
        			{
        				member.getPlayerInstance().sendPacket(new PledgeShowMemberListAll(clan, member.getPlayerInstance()));
        			}
        		}

    		    ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000);	// Schedule owner tasks to start running 
            }
        }
        catch (Exception e)
        {
        	_log.info("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	// =========================================================
	// Proeprty
	public final int getCastleId()
	{
		return _CastleId;
	}

	public final L2DoorInstance getDoor(int doorId)
	{
	    if (doorId <= 0)
	        return null;
	    
        for (int i = 0; i < getDoors().size(); i++)
        {
            L2DoorInstance door = getDoors().get(i);
            if (door.getDoorId() == doorId)
                return door;
        }
		return null;
	}

	public final List<L2DoorInstance> getDoors()
	{
		return _Doors;
	}

	public final String getName()
	{
	    return _Name;
	}

	public final int getOwnerId()
	{
		return _OwnerId;
	}

	public final Siege getSiege()
	{
        if (_Siege == null) _Siege = new Siege(new Castle[] {this});
		return _Siege;
	}

	public final Calendar getSiegeDate() { return _SiegeDate; }

	public final int getSiegeDayOfWeek() { return _SiegeDayOfWeek; }

	public final int getSiegeHourOfDay() { return _SiegeHourOfDay; }

	public final int getTaxPercent()
	{
		return _TaxPercent;
	}

	public final double getTaxRate()
	{
		return _TaxRate;
	}

	public final int getTreasury()
	{
		return _Treasury;
	}



    /**
     * Manor specific code
     */
        
    public FastList<SeedProduction> getSeedProduction(int period) 
    { 
        return (period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext); 
    } 

    public FastList<CropProcure> getCropProcure(int period) 
    { 
        return (period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext); 
    } 

    public void setSeedProduction(FastList<SeedProduction> seed, int period) 
    { 
        if (period == CastleManorManager.PERIOD_CURRENT) 
            _production = seed; 
        else 
            _productionNext = seed; 
    } 

    public void setCropProcure(FastList<CropProcure> crop, int period) 
    { 
        if (period == CastleManorManager.PERIOD_CURRENT) 
            _procure = crop; 
        else 
            _procureNext = crop; 
        } 

    public synchronized SeedProduction getSeed(int seedId, int period) 
    { 
        for (SeedProduction seed : getSeedProduction(period)) 
            { 
            if (seed.getId() == seedId) 
                { 
                return seed; 
                } 
            } 
        return null; 
        } 

    public synchronized CropProcure getCrop(int cropId, int period) 
    { 
        for (CropProcure crop : getCropProcure(period) ) 
            { 
            if (crop.getId() == cropId) 
                { 
                return crop; 
                } 
            } 
        return null; 
        } 

    public int getManorCost (int period) 
    { 
        FastList<CropProcure> procure; 
        FastList<SeedProduction> production; 

        if (period == CastleManorManager.PERIOD_CURRENT) 
            { 
            procure = _procure; 
            production = _production; 
            } else {  
                procure = _procureNext; 
                production = _productionNext; 
                } 

        int total = 0; 
        if (production != null) 
            { 
            for (SeedProduction seed : production) 
                { 
                total += L2Manor.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce(); 
                } 
            } 
        if (procure != null) 
            { 
            for (CropProcure crop: procure) 
                { 
                total += crop.getPrice() * crop.getStartAmount(); 
                } 
            } 
        return total; 
        } 

    //save manor production data 
    public void saveSeedData()
    {
        java.sql.Connection con = null;
        PreparedStatement statement;
        
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            
            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION);
            statement.setInt(1, getCastleId());

            statement.execute();
            statement.close();
            
            if (_production != null)
            {
                int count = 0;
                String query = "INSERT INTO castle_manor_production VALUES ";
                String values[] = new String[_production.size()];
                for(SeedProduction s : _production)
                {
                    values[count] = "("+getCastleId()+","+s.getId()+","+s.getCanProduce()+","+s.getStartProduce()+","+s.getPrice()+","+CastleManorManager.PERIOD_CURRENT+")";
                    count++;
                }
                if (values.length > 0)
                {
                    query += values[0];
                    for (int i=1; i<values.length; i++)
                    {
                        query += "," + values[i];
                    }
                    statement = con.prepareStatement(query);
                    statement.execute();
                    statement.close();
                }
            }
            
            if (_productionNext != null)
            {
                int count = 0;
                String query = "INSERT INTO castle_manor_production VALUES ";
                String values[] = new String[_productionNext.size()];
                for(SeedProduction s : _productionNext)
                {
                    values[count] = "("+getCastleId()+","+s.getId()+","+s.getCanProduce()+","+s.getStartProduce()+","+s.getPrice()+","+CastleManorManager.PERIOD_NEXT+")";
                    count++;
                }
                if (values.length > 0)
                {
                    query += values[0];
                    for (int i=1;i<values.length;i++)
                    {
                        query += "," + values[i];
                    }
                    statement = con.prepareStatement(query);
                    statement.execute();
                    statement.close();
                }
            }
        } catch (Exception e)
        {
            _log.info("Error adding seed production data for castle " + getName() +": " + e.getMessage());
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
    }
    
    //save manor production data for specified period
    public void saveSeedData(int period)
    {
        java.sql.Connection con = null;
        PreparedStatement statement;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            
            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
            statement.setInt(1, getCastleId());
            statement.setInt(2, period);
            statement.execute();
            statement.close();
            
            FastList<SeedProduction> prod = null;
            prod = getSeedProduction(period);
            
            if (prod != null)
            {
                int count = 0;
                String query = "INSERT INTO castle_manor_production VALUES ";
                String values[] = new String[prod.size()];
                for(SeedProduction s : prod)
                {
                    values[count] = "("+getCastleId()+","+s.getId()+","+s.getCanProduce()+","+s.getStartProduce()+","+s.getPrice()+","+period+")";
                    count++;
                }
                if (values.length > 0)
                {
                    query += values[0];
                    for (int i=1;i<values.length;i++)
                    {
                        query += "," + values[i];
                    }
                    statement = con.prepareStatement(query);
                    statement.execute();
                    statement.close();
                }
            }
        } catch (Exception e)
        {
            _log.info("Error adding seed production data for castle " + getName() +": " + e.getMessage());
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
    }
    //save crop procure data
    public void saveCropData()
    {
        java.sql.Connection con = null;
        PreparedStatement statement;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            
            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE);
            statement.setInt(1, getCastleId());
            statement.execute();
            statement.close();
            if (_procure != null)
            {
                int count = 0;
                String query = "INSERT INTO castle_manor_procure VALUES ";
                String values[] = new String[_procure.size()];
                for (CropProcure cp : _procure)
                {
                    values[count] = "("+getCastleId()+","+cp.getId()+","+cp.getAmount()+","+cp.getStartAmount()+","+cp.getPrice()+","+cp.getReward()+","+CastleManorManager.PERIOD_CURRENT+")";
                    count++;
                }
                if (values.length > 0)
                {
                    query += values[0];
                    for (int i=1;i<values.length;i++)
                    {
                        query += "," + values[i];
                    }
                    statement = con.prepareStatement(query);
                    statement.execute();
                    statement.close();
                }
            }
            if (_procureNext != null)
            {
                int count = 0;
                String query = "INSERT INTO castle_manor_procure VALUES ";
                String values[] = new String[_procureNext.size()];
                for (CropProcure cp : _procureNext)
                {
                    values[count] = "("+getCastleId()+","+cp.getId()+","+cp.getAmount()+","+cp.getStartAmount()+","+cp.getPrice()+","+cp.getReward()+","+CastleManorManager.PERIOD_NEXT+")";
                    count++;
                }
                if (values.length > 0)
                {
                    query += values[0];
                    for (int i=1;i<values.length;i++)
                    {
                        query += "," + values[i];
                    }
                    statement = con.prepareStatement(query);
                    statement.execute();
                    statement.close();
                }
            }
        } catch (Exception e) {
            _log.info("Error adding crop data for castle " + getName() +": " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (Exception e) { }
        }
    }
    
    //  save crop procure data for specified period
    public void saveCropData(int period) {
        java.sql.Connection con = null;
        PreparedStatement statement;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            
            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
            statement.setInt(1, getCastleId());
            statement.setInt(2, period);
            statement.execute();
            statement.close();
            
            FastList<CropProcure> proc = null;
            proc = getCropProcure(period);
            
            if (proc != null)
            {
                int count = 0;
                String query = "INSERT INTO castle_manor_procure VALUES ";
                String values[] = new String[proc.size()];
                
                for (CropProcure cp : proc)
                {
                    values[count] = "("+getCastleId()+","+cp.getId()+","+cp.getAmount()+","+cp.getStartAmount()+","+cp.getPrice()+","+cp.getReward()+","+period+")";
                    count++;
                }
                if (values.length > 0)
                {
                    query += values[0];
                    for (int i=1;i<values.length;i++)
                    {
                        query += "," + values[i];
                    }
                    statement = con.prepareStatement(query);
                    statement.execute();
                    statement.close();
                }
            }
        } catch (Exception e) {
            _log.info("Error adding crop data for castle " + getName() +": " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (Exception e) { }
        }
    }
    
    public void updateCrop (int cropId, int amount, int period)
    {
        java.sql.Connection con = null;
        PreparedStatement statement;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            
            statement = con.prepareStatement(CASTLE_UPDATE_CROP);
            statement.setInt(1, amount);
            statement.setInt(2, cropId);
            statement.setInt(3, getCastleId());
            statement.setInt(4, period);
            statement.execute();
            statement.close();
        } catch (Exception e) {
            _log.info("Error adding crop data for castle " + getName() +": " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (Exception e) { }
        }
    }
    
    public void updateSeed (int seedId, int amount, int period)
    {
        java.sql.Connection con = null;
        PreparedStatement statement;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            
            statement = con.prepareStatement(CASTLE_UPDATE_SEED);
            statement.setInt(1, amount);
            statement.setInt(2, seedId);
            statement.setInt(3, getCastleId());
            statement.setInt(4, period);
            statement.execute();
            statement.close();
        } catch (Exception e) {
            _log.info("Error adding seed production data for castle " + getName() +": " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (Exception e) { }
        }
    }
    
    public boolean isNextPeriodApproved()
    {
        return _isNextPeriodApproved;
    }
    
    public void setNextPeriodApproved(boolean val)
    {
        _isNextPeriodApproved = val;
    }
    }