/* This program is free software; you can redistribute it and/or modify
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastMap;
import com.festina.Config;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.Location;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2DoorInstance;
import com.festina.gameserver.model.actor.instance.L2SiegeGuardInstance;

/**
 *
 * @author -Nemesiss-
 */
public class GeoEngine extends GeoData
{
	private static Logger _log = Logger.getLogger(GeoData.class.getName());
	private static GeoEngine _instance;
    private final static byte E = 1;
    private final static byte W = 2;
    private final static byte S = 4;
    private final static byte N = 8;
	private static Map<Short, MappedByteBuffer> Geodata = new FastMap<Short, MappedByteBuffer>();
	private static Map<Short, IntBuffer> Geodata_index = new FastMap<Short, IntBuffer>();
	private static BufferedOutputStream _geo_bugs_out;
	
	public static GeoEngine getInstance()
    {
        if(_instance == null)
            _instance = new GeoEngine();
        return _instance;
    }
    public GeoEngine()
    {
        NinitGeodata();            
    }
	
    //Public Methods
    /**
     * @see com.festina.gameserver.GeoData#getType(int, int)
     */
    @Override
    public short getType(int x, int y)         
    {
        return NgetType((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4);        
    }
    /**
     * @see com.festina.gameserver.GeoData#getHeight(int, int, int)
     */
    @Override
    public short getHeight(int x, int y, int z)
    {
        return NgetHeight((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z);        
    }
    /**
     * @see com.festina.gameserver.GeoData#getSpawnHeight(int, int, int, int, int)
     */
    @Override
    public short getSpawnHeight(int x, int y, int zmin, int zmax, int spawnid)
    {
    	return NgetSpawnHeight((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,zmin,zmax,spawnid);        
    }
    /**
     * @see com.festina.gameserver.GeoData#geoPosition(int, int)
     */
    @Override
    public String geoPosition(int x, int y)
    {
    	int gx = (x - L2World.MAP_MIN_X) >> 4;
    	int gy = (y - L2World.MAP_MIN_Y) >> 4;
    	return "bx: "+getBlock(gx)+" by: "+getBlock(gy)+" cx: "+getCell(gx)+" cy: "+getCell(gy)+"  region offset: "+getRegionOffset(gx,gy);
    }
    /**
     * @see com.festina.gameserver.GeoData#canSeeTarget(com.festina.gameserver.model.L2Object, com.festina.gameserver.model.L2Object)
     */
    @Override
    public boolean canSeeTarget(L2Object cha, L2Object target)
    {
    	// To be able to see over fences and give the player the viewpoint
    	// game client has, all coordinates are lifted 45 from ground.
    	// Because of layer selection in LOS algorithm (it selects -45 there
    	// and some layers can be very close...) do not change this without 
    	// changing the LOS code.
    	// Basically the +45 is character height. Raid bosses are naturally higher,
    	// dwarves shorter, but this should work relatively well.
    	// If this is going to be improved, use e.g. 
    	// ((L2Character)cha).getTemplate().collisionHeight
    	
    	int z = cha.getZ()+45;
    	if(cha instanceof L2SiegeGuardInstance) z += 30; // well they don't move closer to balcony fence at the moment :(
    	int z2 = target.getZ()+45;
    	if(target instanceof L2DoorInstance) return true; // door coordinates are hinge coords..
    	if(target instanceof L2SiegeGuardInstance) z2 += 30; // well they don't move closer to balcony fence at the moment :(
    	if(cha.getZ() >= target.getZ())
    		return canSeeTarget(cha.getX(),cha.getY(),z,target.getX(),target.getY(),z2);
    	else
    		return canSeeTarget(target.getX(),target.getY(),z2, cha.getX(),cha.getY(),z);
    }
    /**
     * @see com.festina.gameserver.GeoData#canSeeTargetDebug(com.festina.gameserver.model.actor.instance.L2PcInstance, com.festina.gameserver.model.L2Object)
     */
    @Override
    public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
    {
    	// comments: see above
    	int z = gm.getZ()+45;
    	int z2 = target.getZ()+45;
    	if(target instanceof L2DoorInstance) 
    	{
    		gm.sendMessage("door always true");
    		return true; // door coordinates are hinge coords..
    	}
    	
    	if(gm.getZ() >= target.getZ())
    		return canSeeDebug(gm,(gm.getX() - L2World.MAP_MIN_X) >> 4,(gm.getY() - L2World.MAP_MIN_Y) >> 4,z,(target.getX() - L2World.MAP_MIN_X) >> 4,(target.getY() - L2World.MAP_MIN_Y) >> 4,z2);
    	else
    		return canSeeDebug(gm,(target.getX() - L2World.MAP_MIN_X) >> 4,(target.getY() - L2World.MAP_MIN_Y) >> 4,z2,(gm.getX() - L2World.MAP_MIN_X) >> 4,(gm.getY() - L2World.MAP_MIN_Y) >> 4,z);
    }
    /**
     * @see com.festina.gameserver.GeoData#getNSWE(int, int, int)
     */
    @Override
    public short getNSWE(int x, int y, int z)  
    {
        return NgetNSWE((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z);
    }
    /**
     * @see com.festina.gameserver.GeoData#moveCheck(int, int, int, int, int, int)
     */
    @Override
    public Location moveCheck(int x, int y, int z, int tx, int ty, int tz)
    {
    	Location destiny = new Location(tx,ty,tz);
        return MoveCheck(destiny,(x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z,(tx - L2World.MAP_MIN_X) >> 4,(ty - L2World.MAP_MIN_Y) >> 4,tz);
    }    
    /**
     * @see com.festina.gameserver.GeoData#addGeoDataBug(com.festina.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
     */
    @Override
    public void addGeoDataBug(L2PcInstance gm, String comment)
    {
    	int gx = (gm.getX() - L2World.MAP_MIN_X) >> 4;
    	int gy = (gm.getY() - L2World.MAP_MIN_Y) >> 4;
    	int bx = getBlock(gx);
    	int by = getBlock(gy);
    	int cx = getCell(gx);
    	int cy = getCell(gy);
    	int rx = (gx >> 11) + 16;
	    int ry = (gy >> 11) + 10;
    	String out = rx+";"+ry+";"+bx+";"+by+";"+cx+";"+cy+";"+gm.getZ()+";"+comment+"\n";
    	try
		{
    		_geo_bugs_out.write(out.getBytes());
    		_geo_bugs_out.flush();
    		gm.sendMessage("GeoData bug saved!");
		} catch (Exception e) {
			e.printStackTrace();
			gm.sendMessage("GeoData bug save Failed!");
		}
    }    
    
    // Private Methods
    private boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
    {
        return canSee((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4,z,(tx - L2World.MAP_MIN_X) >> 4,(ty - L2World.MAP_MIN_Y) >> 4,tz);
    }
    
    private static boolean canSee(int x, int y, double z, int tx, int ty, int tz)
    {
    	int dx = (tx - x);
        int dy = (ty - y);
        final double dz = (tz - z);
        final int distance2 = dx*dx+dy*dy;

        if (distance2 > 90000) // (300*300) 300*16 = 4800 in world coord
        {
            //Avoid too long check
            return false;
        }
        // very short checks: 9 => 144 world distance
        // this ensures NLOS function has enough points to calculate,
        // it might not work when distance is small and path vertical
        else if (distance2 < 10)
        {
        	if(dz*dz > 40000) // 200 too deep/high. This value should be in sync with NLOS
        		return false;
        	else return true;
        }

        // Increment in Z coordinate when moving along X or Y axis 
        // and not straight to the target. This is done because
        // calculation moves either in X or Y direction.
        final int inc_x = sign(dx);
        final int inc_y = sign(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        final double inc_z_directionx = dz*dx / (distance2);
        final double inc_z_directiony = dz*dy / (distance2);

        // next_* are used in NLOS check from x,y
        int next_x = x;
        int next_y = y;
        
        // creates path to the target
        // calculation stops when next_* == target
        if (dx >= dy)// dy/dx <= 1
        {
        	int delta_A = 2*dy;
        	int d = delta_A - dx;
            int delta_B = delta_A - 2*dx;
            
            for (int i = 0; i < dx; i++) 
            {
            	x = next_x;
            	y = next_y;
            	if (d > 0)
            	{
            		d += delta_B;
            		next_x += inc_x;
            		z += inc_z_directionx;
            		next_y += inc_y;
            		z += inc_z_directiony;
            		//_log.warning("1: next_x:"+next_x+" next_y"+next_y);
            		if (!NLOS(x,y,(int)z,inc_x,inc_y,tz,false))
            			return false;
            	}
            	else
            	{
            		d += delta_A;
            		next_x += inc_x;
            		//_log.warning("2: next_x:"+next_x+" next_y"+next_y);
            		z += inc_z_directionx;
            		if (!NLOS(x,y,(int)z,inc_x,0,tz,false))
            			return false;
            	}
            }
        }
        else
        {
        	int delta_A = 2*dx;
        	int d = delta_A - dy;
            int delta_B = delta_A - 2*dy;
            for (int i = 0; i < dy; i++)
            {
            	x = next_x;
            	y = next_y;
            	if (d > 0)
            	{
            		d += delta_B;
            		next_y += inc_y;
            		z += inc_z_directiony;
            		next_x += inc_x;
            		z += inc_z_directionx;
            		//_log.warning("3: next_x:"+next_x+" next_y"+next_y);
            		if (!NLOS(x,y,(int)z,inc_x,inc_y,tz,false))
            			return false;
            	}
            	else
            	{
            		d += delta_A;
            		next_y += inc_y;
            		//_log.warning("4: next_x:"+next_x+" next_y"+next_y);
            		z += inc_z_directiony;
            		if (!NLOS(x,y,(int)z,0,inc_y,tz,false))
            			return false;
            	}
            }
        }
        return true;
    }
    /*
     * Debug function for checking if there's a line of sight between
     * two coordinates.
     * 
     * Creates points for line of sight check (x,y,z towards target) and 
     * in each point, layer and movement checks are made with NLOS function.
     * 
     * Coordinates here are geodata x,y but z coordinate is world coordinate
     */
    private static boolean canSeeDebug(L2PcInstance gm, int x, int y, double z, int tx, int ty, int tz)
    {
    	int dx = (tx - x);
        int dy = (ty - y);
        final double dz = (tz - z);
        final int distance2 = dx*dx+dy*dy;

        if (distance2 > 90000) // (300*300) 300*16 = 4800 in world coord
        {
            //Avoid too long check
        	gm.sendMessage("dist > 300");
            return false;
        }
        // very short checks: 9 => 144 world distance
        // this ensures NLOS function has enough points to calculate,
        // it might not work when distance is small and path vertical
        else if (distance2 < 10)
        {
        	if(dz*dz > 40000) // 200 too deep/high. This value should be in sync with NLOS
        		return false;
        	else return true;
        }

        // Increment in Z coordinate when moving along X or Y axis 
        // and not straight to the target. This is done because
        // calculation moves either in X or Y direction.
        final int inc_x = sign(dx);
        final int inc_y = sign(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        final double inc_z_directionx = dz*dx / (distance2);
        final double inc_z_directiony = dz*dy / (distance2);

        gm.sendMessage("Los: from X: "+x+ "Y: "+y+ "--->> X: "+tx+" Y: "+ty);

        // next_* are used in NLOS check from x,y
        int next_x = x;
        int next_y = y;
        
        // creates path to the target
        // calculation stops when next_* == target
        if (dx >= dy)// dy/dx <= 1
        {
        	int delta_A = 2*dy;
        	int d = delta_A - dx;
            int delta_B = delta_A - 2*dx;
            
            for (int i = 0; i < dx; i++) 
            {
            	x = next_x;
            	y = next_y;
            	if (d > 0)
            	{
            		d += delta_B;
            		next_x += inc_x;
            		z += inc_z_directionx;
            		next_y += inc_y;
            		z += inc_z_directiony;
            		//_log.warning("1: next_x:"+next_x+" next_y"+next_y);
            		if (!NLOS(x,y,(int)z,inc_x,inc_y,tz,true))
            			return false;
            	}
            	else
            	{
            		d += delta_A;
            		next_x += inc_x;
            		//_log.warning("2: next_x:"+next_x+" next_y"+next_y);
            		z += inc_z_directionx;
            		if (!NLOS(x,y,(int)z,inc_x,0,tz,true))
            			return false;
            	}
            }
        }
        else
        {
        	int delta_A = 2*dx;
        	int d = delta_A - dy;
            int delta_B = delta_A - 2*dy;
            for (int i = 0; i < dy; i++)
            {
            	x = next_x;
            	y = next_y;
            	if (d > 0)
            	{
            		d += delta_B;
            		next_y += inc_y;
            		z += inc_z_directiony;
            		next_x += inc_x;
            		z += inc_z_directionx;
            		//_log.warning("3: next_x:"+next_x+" next_y"+next_y);
            		if (!NLOS(x,y,(int)z,inc_x,inc_y,tz,true))
            			return false;
            	}
            	else
            	{
            		d += delta_A;
            		next_y += inc_y;
            		//_log.warning("4: next_x:"+next_x+" next_y"+next_y);
            		z += inc_z_directiony;
            		if (!NLOS(x,y,(int)z,0,inc_y,tz,true))
            			return false;
            	}
            }
        }
        return true;
    }
    /*
     *  MoveCheck
     */
    private static Location MoveCheck(Location destiny, int x, int y, double z, int tx, int ty, int tz)
    {
    	int dx = (tx - x);
        int dy = (ty - y);
        //final double dz = (tz - z);
        final int distance2 = dx*dx+dy*dy;

        if (distance2 > 15625) // 150*150*16 = 2000 world coord
        {
            //Avoid too long check
            return destiny;
        }
        if (distance2 == 0)
        	return destiny;
     
        // Increment in Z coordinate when moving along X or Y axis 
        // and not straight to the target. This is done because
        // calculation moves either in X or Y direction.
        final int inc_x = sign(dx);
        final int inc_y = sign(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);

        //gm.sendMessage("MoveCheck: from X: "+x+ "Y: "+y+ "--->> X: "+tx+" Y: "+ty);

        // next_* are used in NcanMoveNext check from x,y
        int next_x = x;
        int next_y = y;
        double tempz = z;
        
        // creates path to the target, using only x or y direction
        // calculation stops when next_* == target
        if (dx >= dy)// dy/dx <= 1
        {
        	int delta_A = 2*dy;
        	int d = delta_A - dx;
            int delta_B = delta_A - 2*dx;
            
            for (int i = 0; i < dx; i++) 
            {
            	x = next_x;
            	y = next_y;
            	if (d > 0)
            	{
            		d += delta_B;
            		next_x += inc_x;
            		next_y += inc_y;
            		//_log.warning("2: next_x:"+next_x+" next_y"+next_y);
            		tempz = NcanMoveNext(x,y,(int)z,next_x,next_y,tz);
            		if (tempz == Double.MIN_VALUE) 
            			return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
            		else z = tempz;
            	}
            	else
            	{
            		d += delta_A;
            		next_x += inc_x;
            		//_log.warning("3: next_x:"+next_x+" next_y"+next_y);
            		tempz = NcanMoveNext(x,y,(int)z,next_x,next_y,tz);
            		if (tempz == Double.MIN_VALUE) 
            			return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
            		else z = tempz;
            	}
            }
        }
        else
        {
        	int delta_A = 2*dx;
        	int d = delta_A - dy;
            int delta_B = delta_A - 2*dy;
            for (int i = 0; i < dy; i++)
            {
            	x = next_x;
            	y = next_y;
            	if (d > 0)
            	{
            		d += delta_B;
            		next_y += inc_y;
            		next_x += inc_x;
            		//_log.warning("5: next_x:"+next_x+" next_y"+next_y);
            		tempz = NcanMoveNext(x,y,(int)z,next_x,next_y,tz);
            		if (tempz == Double.MIN_VALUE) 
            			return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
            		else z = tempz;
            	}
            	else
            	{
            		d += delta_A;
            		next_y += inc_y;
            		//_log.warning("6: next_x:"+next_x+" next_y"+next_y);
            		tempz = NcanMoveNext(x,y,(int)z,next_x,next_y,tz);
            		if (tempz == Double.MIN_VALUE) 
            			return new Location((x << 4) + L2World.MAP_MIN_X,(y << 4) + L2World.MAP_MIN_Y,(int)z);
            		else z = tempz;
            	}
            }
        }
        return destiny;
    }
    
    private static byte sign(int x)
    {    	
    	if (x >= 0)
    		return +1;
        else
        	return -1;
    }
	
	//GeoEngine
	private static void NinitGeodata()
	{
		LineNumberReader lnr = null;
		try
		{
			_log.info("Geo Engine: - Loading Geodata...");			
			File Data = new File("./data/geodata/geo_index.txt");
			if (!Data.exists())
				return;
			
			lnr = new LineNumberReader(new BufferedReader(new FileReader(Data)));	
		} catch (Exception e) {
			e.printStackTrace();		
			throw new Error("Failed to Load geo_index File.");	
		}
		String line;
		try
		{
			while ((line = lnr.readLine()) != null) {
				if (line.trim().length() == 0)
					continue;
				StringTokenizer st = new StringTokenizer(line, "_");
				byte rx = Byte.parseByte(st.nextToken());
				byte ry = Byte.parseByte(st.nextToken());
				LoadGeodataFile(rx,ry);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Failed to Read geo_index File.");
		}
		try
		{			
			File geo_bugs = new File("./data/geodata/geo_bugs.txt");			
			
			_geo_bugs_out = new BufferedOutputStream(new FileOutputStream(geo_bugs,true));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Failed to Load geo_bugs.txt File.");	
		}
	}
	public static void unloadGeodata(byte rx, byte ry)
	{
		short regionoffset = (short)((rx << 5) + ry);
		Geodata_index.remove(regionoffset);
		Geodata.remove(regionoffset);
	}
	public static boolean LoadGeodataFile(byte rx, byte ry)
	{
		String fname = "./data/geodata/"+rx+"_"+ry+".l2j";
		short regionoffset = (short)((rx << 5) + ry);
		_log.info("Geo Engine: - Loading: "+fname+" -> region offset: "+regionoffset+"X: "+rx+" Y: "+ry);		
		File Geo = new File(fname);
		int size, index = 0, block = 0, flor = 0;
		try {
	        // Create a read-only memory-mapped file
	        FileChannel roChannel = new RandomAccessFile(Geo, "r").getChannel();
			size = (int)roChannel.size();
			MappedByteBuffer geo;
			if (Config.FORCE_GEODATA) //Force O/S to Loads this buffer's content into physical memory.
				//it is not guarantee, because the underlying operating system may have paged out some of the buffer's data
				geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size).load();
			else
				geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
			geo.order(ByteOrder.LITTLE_ENDIAN);
			
			if (size > 196608)
			{
				// Indexing geo files, so we will know where each block starts
				IntBuffer indexs = IntBuffer.allocate(65536);
				while(block < 65536)
			    {	        
					byte type = geo.get(index);
			        indexs.put(block,index);
					block++;
					index++;
			        if(type == 0)
			        	index += 2; // 1x short
			        else if(type == 1)
			        	index += 128; // 64 x short
			        else
			        {
			            int b;
			            for(b=0;b<64;b++)
			            {
			                byte layers = geo.get(index);
			                index += (layers << 1) + 1;
			                if (layers > flor)
			                     flor = layers;
			            }
			        }
			    }
				Geodata_index.put(regionoffset, indexs);
			}
			Geodata.put(regionoffset,geo);
			
			_log.info("Geo Engine: - Max Layers: "+flor+" Size: "+size+" Loaded: "+index);
	    } catch (Exception e)
		{
			e.printStackTrace();
			_log.warning("Failed to Load GeoFile at block: "+block+"\n");
			return false;
	    }
	    return true;
	}
	
	//Geodata Methods
	/**
	 * @param x
	 * @param y
	 * @return Region Offset
	 */
	private static short getRegionOffset(int x, int y)
	{
	    int rx = x >> 11; // =/(256 * 8)
	    int ry = y >> 11;
	    return (short)(((rx+16) << 5) + (ry+10));
	}

	/**
	 * @param pos
	 * @return Block Index: 0-255
	 */
	private  static int getBlock(int geo_pos)
	{
	    return (geo_pos >> 3) % 256;
	}
	
	/**
	 * @param pos
	 * @return Cell Index: 0-7
	 */
	private static int getCell(int geo_pos)
	{
	    return geo_pos % 8;
	}
	
	//Geodata Functions
	
	/**
	 * @param x
	 * @param y
	 * @return Type of geo_block: 0-2
	 */
	private static short NgetType(int x, int y)
	{
	    short region = getRegionOffset(x,y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int index = 0;
		//Geodata without index - it is just empty so index can be calculated on the fly
		if(Geodata_index.get(region) == null) index = ((blockX << 8) + blockY)*3;
		//Get Index for current block of current geodata region
		else index = Geodata_index.get(region).get((blockX << 8) + blockY);
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = Geodata.get(region);
		if(geo == null)
		{
			if(Config.DEBUG)
				_log.warning("Geo Region - Region Offset: "+region+" dosnt exist!!");
			return 0;
		}
		return geo.get(index);
	}
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return Nearlest Z
	 */
	private static short NgetHeight(int geox, int geoy, int z)
	{
	    short region = getRegionOffset(geox,geoy);
	    int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		//Geodata without index - it is just empty so index can be calculated on the fly
		if(Geodata_index.get(region) == null) index = ((blockX << 8) + blockY)*3;		
		//Get Index for current block of current region geodata
		else index = Geodata_index.get(region).get(((blockX << 8))+(blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = Geodata.get(region);
		if(geo == null)
		{
			if(Config.DEBUG)
				_log.warning("Geo Region - Region Offset: "+region+" dosnt exist!!");
			return (short)z;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
	    if(type == 0)//flat
	        return geo.getShort(index);	    
	    else if(type == 1)//complex
	    {
	    	cellX = getCell(geox);
			cellY = getCell(geoy);
	        index += ((cellX << 3) + cellY) << 1;
	        short height = geo.getShort(index);
			height = (short)(height&0x0fff0);
			height = (short)(height >> 1); //height / 2
			return height;
	    }
	    else //multilevel
	    {
	    	cellX = getCell(geox);
			cellY = getCell(geoy);
	        int offset = (cellX << 3) + cellY;
	        while(offset > 0)
	        {
	            byte lc = geo.get(index);
	            index += (lc << 1) + 1;
	            offset--;
	        }
	        byte layers = geo.get(index);
	        index++;
	        short height=-1;
			if(layers <= 0 || layers > 125)
			{
				_log.warning("Geo Engine: - invalid layers count: "+layers+" at: "+geox+" "+geoy);				
	            return (short)z;
			}
	        short temph = Short.MIN_VALUE;
	        while(layers > 0)
	        {	            
	            height = geo.getShort(index);
	            height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
	            if ((z-temph)*(z-temph) > (z-height)*(z-height))
	                temph = height;            
	            layers--;
	            index += 2;
	        }	        
		 return temph;
	    }
	}
	/**
	 * @param x
	 * @param y
	 * @param zmin
	 * @param zmax
	 * @return Z betwen zmin and zmax
	 */
	private static short NgetSpawnHeight(int geox, int geoy, int zmin, int zmax, int spawnid)
	{    
	    short region = getRegionOffset(geox,geoy);
	    int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		short temph = Short.MIN_VALUE;
		//Geodata without index - it is just empty so index can be calculated on the fly
		if(Geodata_index.get(region) == null) index = ((blockX << 8) + blockY)*3;		
		//Get Index for current block of current region geodata
		else index = Geodata_index.get(region).get(((blockX << 8))+(blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = Geodata.get(region);
		if(geo == null)
		{
			if(Config.DEBUG)
				_log.warning("Geo Region - Region Offset: "+region+" dosnt exist!!");
			return (short)zmin;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
	    if(type == 0)//flat	    
	    	temph = geo.getShort(index);	    
	    else if(type == 1)//complex
	    {
	    	cellX = getCell(geox);
			cellY = getCell(geoy);
	        index += ((cellX << 3) + cellY) << 1;
	        short height = geo.getShort(index);
			height = (short)(height&0x0fff0);
			height = (short)(height >> 1); //height / 2
            temph = height;
	    }
	    else//multilevel
	    {
	    	cellX = getCell(geox);
			cellY = getCell(geoy);
			short height;
	        int offset = (cellX << 3) + cellY;
	        while(offset > 0)
	        {
	            byte lc = geo.get(index);		                
	            index += (lc << 1) + 1;
	            offset--;
	        }
	        //Read current block type: 0-flat,1-complex,2-multilevel
	        byte layers = geo.get(index);
	        index++;
			if(layers <= 0 || layers > 125)
			{
				_log.warning("Geo Engine: - invalid layers count: "+layers+" at: "+geox+" "+geoy);				
	            return (short)zmin;
			}			
	        while(layers > 0)
	        {	            
	            height = geo.getShort(index);
	            height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
	            if ((zmin-temph)*(zmin-temph) > (zmin-height)*(zmin-height))
	                temph = height;            
	            layers--;
	            index += 2;
	        }
	        if (temph > zmax + 150 || temph < zmin - 150)        
	        {
	        	//Just log error - we trust GeoData and spawn NPC on nearlest GeoData Z
	        	if(Config.DEBUG)
	        		_log.warning("SpawnHeight Error - Couldnt find correct layer to spawn NPC - GeoData or Spawnlist Bug!: zmin: "+zmin+" zmax: "+zmax+" value: "+temph+" SpawnId: "+spawnid+" at: "+geox+" : "+geoy);
	        	return temph;
	        }
	    }
	    if (temph > zmax + 600 || temph < zmin - 600) 
	    {
	    	//Just log error - we trust GeoData and spawn NPC on Z given by GeoData
	    	if(Config.DEBUG)
	    		_log.warning("SpawnHeight Error - Spawnlist z value is wrong or GeoData error: zmin: "+zmin+" zmax: "+zmax+" value: "+temph+" SpawnId: "+spawnid+" at: "+geox+" : "+geoy);	        
        }
	    return temph;
	}
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param tx
	 * @param ty
	 * @param tz
	 * @return True if char can move to (tx,ty,tz)
	 */
	private static double NcanMoveNext(int x, int y, int z, int tx, int ty, int tz)
	{
	    short region = getRegionOffset(x,y);
	    int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
	    short NSWE = 0;	    
	    
		int index = 0;
		//Geodata without index - it is just empty so index can be calculated on the fly
		if(Geodata_index.get(region) == null) index = ((blockX << 8) + blockY)*3;		
		//Get Index for current block of current region geodata
		else index = Geodata_index.get(region).get(((blockX << 8))+(blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = Geodata.get(region);
		if(geo == null)
		{
			if(Config.DEBUG)
				_log.warning("Geo Region - Region Offset: "+region+" dosnt exist!!");
			return z;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
	    if(type == 0) //flat
	        return z;
	    else if(type == 1) //complex
	    {
	    	cellX = getCell(x);
			cellY = getCell(y);
	        index += ((cellX << 3) + cellY) << 1;
	        short height = geo.getShort(index);
			NSWE = (short)(height&0x0F);
			height = (short)(height&0x0fff0);
			height = (short)(height >> 1); //height / 2
			if(CheckNSWE(NSWE,x,y,tx,ty)) return height;
			else return Double.MIN_VALUE;
	    }
	    else //multilevel, type == 2
	    {		 
	    	cellX = getCell(x);
			cellY = getCell(y);
	        int offset = (cellX << 3) + cellY;
	        while(offset > 0) // iterates (too many times?) to get to layer count
	        {
	            byte lc = geo.get(index);		                 
	            index += (lc << 1) + 1;
	            offset--;
	        }
	        byte layers = geo.get(index);
	        //_log.warning("layers"+layers);
	        index++;
	        short height=-1;		 
	        if(layers <= 0 || layers > 125)		 
	        {		     
                _log.warning("Geo Engine: - invalid layers count: "+layers+" at: "+x+" "+y);
	            return z;
	        }		
	        short tempz = Short.MIN_VALUE;
	        while(layers > 0)
	        {	            
	            height = geo.getShort(index);
	            height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2

				// searches the closest layer to current z coordinate
	            if ((z-tempz)*(z-tempz) > (z-height)*(z-height))
	            {
	                //layercurr = layers;
	            	tempz = height;
	                NSWE = geo.getShort(index);
	                NSWE = (short)(NSWE&0x0F);                           
	            }
	            layers--;
	            index += 2;
	        }
	        if(CheckNSWE(NSWE,x,y,tx,ty)) return tempz;
	        else return Double.MIN_VALUE;
	    }
	}
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param inc_x
	 * @param inc_y
	 * @param tz
	 * @return True if Char can see target
	 */
	private static boolean NLOS(int x, int y, int z, int inc_x, int inc_y, int tz, boolean debug)
	{
	    short region = getRegionOffset(x,y);
	    int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
	    short NSWE = 0;
	    
		int index;
		//Geodata without index - it is just empty so index can be calculated on the fly
		if(Geodata_index.get(region) == null) index = ((blockX << 8) + blockY)*3;		
		//Get Index for current block of current region geodata
		else index = Geodata_index.get(region).get(((blockX << 8))+(blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = Geodata.get(region);
		if(geo == null)
		{
			if(Config.DEBUG)
				_log.warning("Geo Region - Region Offset: "+region+" dosnt exist!!");
			return true;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
	    if(type == 0) //flat, movement and sight always possible
	    {
	    	if(debug) _log.warning("flatheight:"+geo.getShort(index));	 
	    	return true;
	    }
	    else if(type == 1) //complex
	    {
	    	cellX = getCell(x);
			cellY = getCell(y);
	        index += ((cellX << 3) + cellY) << 1;
	        short height = geo.getShort(index);
	        NSWE = (short)(height&0x0F);
	        height = (short)(height&0x0fff0);
			height = (short)(height >> 1); //height / 2
			if(debug) {
				_log.warning("height:"+height+" z"+z);
				if(!CheckNSWE(NSWE,x,y,x+inc_x,y+inc_y)) _log.warning("would block");
			}
			if(z - height > 50) return true; // this value is just an approximate
	    }
	    else//multilevel, type == 2
	    {		 
	    	cellX = getCell(x);
			cellY = getCell(y);
	        int offset = (cellX << 3) + cellY;
	        while(offset > 0) // iterates (too many times?) to get to layer count
	        {
	            byte lc = geo.get(index);		                 
	            index += (lc << 1) + 1;
	            offset--;
	        }
	        byte layers = geo.get(index);
	        if (debug) _log.warning("layers"+layers);
	        index++;
	        short height=-1;		 
	        if(layers <= 0 || layers > 125)
	        {
	        	_log.warning("Geo Engine: - invalid layers count: "+layers+" at: "+x+" "+y);
	            return false;
	        }
	        short tempz = Short.MIN_VALUE; // big negative value
	        byte temp_layers = layers;
	        boolean highestlayer = true;
	        
	        z -= 25; // lowering level temporarily to avoid selecting ceiling
	        while(temp_layers > 0)
	        {	            
	            // reads height for current layer, result in world z coordinate
	        	height = geo.getShort(index);
	            height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2
				//height -= 8; // old geo files had -8 around giran, new data seems better
				
				// searches the closest layer to current z coordinate
				if ((z-tempz)*(z-tempz) > (z-height)*(z-height))
	            {
					if(tempz > Short.MIN_VALUE) highestlayer = false;
					tempz = height;
					if (debug) _log.warning("z"+(z+45)+" tempz"+tempz+" dz"+(z-tempz));
	                NSWE = geo.getShort(index);
	                NSWE = (short)(NSWE&0x0F);
	            }
				temp_layers--;
	            index += 2;
	        }
	        z += 25; // level rises back
	        
	        // Check if LOS goes under a layer/floor  
	        if((z-tempz) < -20) return false; // -20 => clearly under, approximates also fence width
	        
	        // this helps in some cases (occasional under-highest-layer block which isn't wall) 
	        // but might also create problems in others (passes walls when you're standing high)
	        if((z-tempz) > 250) return true; 
	        
	        // or there's a fence/wall ahead when we're not on highest layer
	        // this part of the check is problematic
	        if(!highestlayer)
	        {
	        	//a probable wall, there's movement block and layers above you
	        	if(!CheckNSWE(NSWE,x,y,x+inc_x,y+inc_y)) // cannot move
	        	{
	        		// the height after 2 inc_x,inc_y
	        		short nextheight = NgetHeight(x+2*inc_x,y+2*inc_y,z-50);
	        		if(debug) 
	        		{
	        			_log.warning("0: z:"+z+" tz"+NgetHeight(x,y,z-60));
	        			_log.warning("1: z:"+z+" tz"+NgetHeight(x+inc_x,y+inc_y,z-60));
	        			_log.warning("2: z:"+z+" tz"+NgetHeight(x+2*inc_x,y+2*inc_y,z-60));
	        			_log.warning("3: z:"+z+" tz"+NgetHeight(x+3*inc_x,y+3*inc_y,z-60));
	        		}
	        		// Probably a very thin fence (e.g. castle fences above artefact),
	        		// where height instantly drops after 1-2 cells and layer ends. 
	        		if(z-nextheight>100) return true;
	        		// layer continues so close we can see over it
	        		if(nextheight-tempz>5 && nextheight-tempz<20) return true;
	        		return false;
	        	}
	        	else return true;
	        }
	        else return true;
	    }
	    return CheckNSWE(NSWE,x,y,x+inc_x,y+inc_y);
	}
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return NSWE: 0-15
	 */
	private short NgetNSWE(int x, int y, int z)
	{
		short region = getRegionOffset(x,y);
	    int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
	    short NSWE = 0;

		int index = 0;
		//Geodata without index - it is just empty so index can be calculated on the fly
		if(Geodata_index.get(region) == null) index = ((blockX << 8) + blockY)*3;		
		//Get Index for current block of current region geodata
		else index = Geodata_index.get(region).get(((blockX << 8))+(blockY));
		//Buffer that Contains current Region GeoData
		ByteBuffer geo = Geodata.get(region);
		if(geo == null)
		{
			if(Config.DEBUG)
				_log.warning("Geo Region - Region Offset: "+region+" dosnt exist!!");
			return 15;
		}
		//Read current block type: 0-flat,1-complex,2-multilevel
		byte type = geo.get(index);
		index++;
	    if(type == 0)//flat
	        return 15;
	    else if(type == 1)//complex
	    {
	    	cellX = getCell(x);
			cellY = getCell(y);
	        index += ((cellX << 3) + cellY) << 1;
	        short height = geo.getShort(index);
			NSWE = (short)(height&0x0F);		
	    }
	    else//multilevel
	    {		 
	    	cellX = getCell(x);
			cellY = getCell(y);
	        int offset = (cellX << 3) + cellY;
	        while(offset > 0)
	        {
	        	byte lc = geo.get(index);		                 
	            index += (lc << 1) + 1;
	            offset--;
	        }
	        byte layers = geo.get(index);
	        index++;
	        short height=-1;		 
	        if(layers <= 0 || layers > 125)		 
	        {		     
	        	_log.warning("Geo Engine: - invalid layers count: "+layers+" at: "+x+" "+y);
	            return 15;
	        }
	        short tempz = Short.MIN_VALUE;
	        while(layers > 0)
	        {
	            height = geo.getShort(index);
	            height = (short)(height&0x0fff0);
				height = (short)(height >> 1); //height / 2	                      

	            if ((z-tempz)*(z-tempz) > (z-height)*(z-height))
	            {
	                tempz = height;
	                NSWE = geo.get(index);
	                NSWE = (short)(NSWE&0x0F);                           
	            }
	            layers--;
	            index += 2;
	        }
	    }
	    return NSWE;	    
	}
	
	/**
	 * @param NSWE
	 * @param x
	 * @param y
	 * @param tx
	 * @param ty
	 * @return True if NSWE dont block given direction
	 */
	private static boolean CheckNSWE(short NSWE, int x, int y, int tx, int ty)
    {
        //Check NSWE
	    if(NSWE == 15)
	       return true;
	    if(tx > x)//E
	    {
	    	if ((NSWE & E) == 0)
	            return false;
	    }
	    else if (tx < x)//W
	    {
	    	if ((NSWE & W) == 0)
	            return false;
	    }
	    if (ty > y)//S
	    {
	    	if ((NSWE & S) == 0)
	            return false;
	    }
	    else if (ty < y)//N
	    {
	    	if ((NSWE & N) == 0)
	            return false;
	    }
	    return true;
    }
}
