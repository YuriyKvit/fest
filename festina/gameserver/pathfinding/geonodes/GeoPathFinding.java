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
package com.festina.gameserver.pathfinding.geonodes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.festina.Config;
import com.festina.gameserver.pathfinding.AbstractNodeLoc;
import com.festina.gameserver.pathfinding.Node;
import com.festina.gameserver.pathfinding.PathFinding;

/**
 *
 * @author -Nemesiss-
 */
public class GeoPathFinding extends PathFinding
{
	private static Logger _log = Logger.getLogger(GeoPathFinding.class.getName());
	private static GeoPathFinding _instance;
	private static Map<Short, ByteBuffer> PathNodes = new FastMap<Short, ByteBuffer>();
	private static Map<Short, IntBuffer> PathNodes_index = new FastMap<Short, IntBuffer>();
	
	public static GeoPathFinding getInstance()
	{
		if (_instance == null)
			_instance = new GeoPathFinding();
		return _instance;
	}
	
	/**
	 * @see com.festina.gameserver.pathfinding.PathFinding#PathNodesExist(short)
	 */
	@Override
	public boolean PathNodesExist(short regionoffset)
	{
		return PathNodes_index.containsKey(regionoffset);
	}
	
	/**
	 * @see com.festina.gameserver.pathfinding.PathFinding#FindPath(int, int, short, int, int, short)
	 */
	@Override
	public List<AbstractNodeLoc> FindPath(int gx, int gy, short z, int gtx, int gty, short tz)
	{
		Node start = readNode(gx,gy,z);
		//_log.warning("startx"+start.getLoc().getNodeX());
		//_log.warning("starty"+start.getLoc().getNodeY());
		//_log.warning("startx"+start.getLoc().getX());
		//_log.warning("starty"+start.getLoc().getY());
		Node end = readNode(gtx,gty,tz);
		//_log.warning("end"+end.getLoc().getNodeX());
		//_log.warning("end"+end.getLoc().getNodeY());
		if(start == null || end == null) return null;
		return search(start, end);
	}
	
	/**
	 * @see com.festina.gameserver.pathfinding.PathFinding#ReadNeighbors(short, short)
	 */
	@Override
	public Node[] ReadNeighbors(short node_x,short node_y, int idx)
	{
		short regoffset = getRegionOffset(getRegionX(node_x),getRegionY(node_y));
		ByteBuffer pn = PathNodes.get(regoffset);
		
		List<Node> Neighbors = new FastList<Node>(8);
		
		short new_node_x, new_node_y;
		
		//Region for sure will change, we must read from correct file
		byte neighbor = pn.get(idx); //N
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = node_x;
			new_node_y = (short)(node_y-1);
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		neighbor = pn.get(idx); //NE
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x+1);
			new_node_y = (short)(node_y-1);
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		neighbor = pn.get(idx); //E
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x+1);
			new_node_y = node_y;
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		neighbor = pn.get(idx); //SE
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x+1);
			new_node_y = (short)(node_y+1);
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		neighbor = pn.get(idx); //S
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = node_x;
			new_node_y = (short)(node_y+1);
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		neighbor = pn.get(idx); //SW
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x-1);
			new_node_y = (short)(node_y+1);
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		neighbor = pn.get(idx); //W
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x-1);
			new_node_y = node_y;
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		neighbor = pn.get(idx); //NW
		idx++;
		if(neighbor > 0)
		{
			neighbor--;
			new_node_x = (short)(node_x-1);
			new_node_y = (short)(node_y-1);
			Neighbors.add(readNode(new_node_x,new_node_y,neighbor));
		}
		Node[] result = new Node[Neighbors.size()];
		return Neighbors.toArray(result);
	}
	
	//Private
	
	private Node readNode(short node_x, short node_y, byte layer)
	{
		short regoffset = getRegionOffset(getRegionX(node_x),getRegionY(node_y));
		short nbx = getNodeBlock(node_x);
		short nby = getNodeBlock(node_y);
		int idx = PathNodes_index.get(regoffset).get((nby << 8)+nbx);
		ByteBuffer pn = PathNodes.get(regoffset);
		//reading
		byte nodes = pn.get(idx);
		idx += layer*10+1;//byte + layer*10byte
		if (nodes < layer)
		{
			_log.warning("SmthWrong!");
		}
		short node_z = pn.getShort(idx);
		idx += 2;
		return new Node(new GeoNodeLoc(node_x,node_y,node_z), idx);
	}
	
	private Node readNode(int gx, int gy, short z)
	{
		short node_x = getNodePos(gx);
		short node_y = getNodePos(gy);
		short regoffset = getRegionOffset(getRegionX(node_x),getRegionY(node_y));
		short nbx = getNodeBlock(node_x);
		short nby = getNodeBlock(node_y);
		int idx = PathNodes_index.get(regoffset).get((nby << 8)+nbx);
		ByteBuffer pn = PathNodes.get(regoffset);
		//reading
		byte nodes = pn.get(idx);
		idx++;//byte
		int idx2 = 0; //create index to nearlest node by z
		short last_z = Short.MIN_VALUE;
		while (nodes > 0)
		{
			short node_z = pn.getShort(idx);
			if (Math.abs(last_z - z) >  Math.abs(node_z -z))
			{
				last_z = node_z;
				idx2 = idx;
			}
			idx += 10; //short + 8 byte
			nodes--;
		}		
		return new Node(new GeoNodeLoc(node_x,node_y,last_z), idx2);
	}

	private GeoPathFinding()
	{
		LineNumberReader lnr = null;
		try
		{
			_log.info("PathFinding Engine: - Loading Path Nodes...");			
			File Data = new File("./data/pathnode/pn_index.txt");
			if (!Data.exists())
				return;
			
			lnr = new LineNumberReader(new BufferedReader(new FileReader(Data)));	
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Failed to Load pn_index File.");	
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
				LoadPathNodeFile(rx,ry);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Failed to Read pn_index File.");
		}
	}
	
	private void LoadPathNodeFile(byte rx,byte ry)
	{
		String fname = "./data/pathnode/"+rx+"_"+ry+".pn";
		short regionoffset = getRegionOffset(rx,ry);
		_log.info("PathFinding Engine: - Loading: "+fname+" -> region offset: "+regionoffset+"X: "+rx+" Y: "+ry);		
		File Pn = new File(fname);
		int node = 0,size, index = 0;
		try {
	        // Create a read-only memory-mapped file
	        FileChannel roChannel = new RandomAccessFile(Pn, "r").getChannel();
			size = (int)roChannel.size();
			MappedByteBuffer nodes;
			if (Config.FORCE_GEODATA) //Force O/S to Loads this buffer's content into physical memory.
				//it is not guarantee, because the underlying operating system may have paged out some of the buffer's data
				nodes = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size).load();
			else
				nodes = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);	

			// Indexing pathnode files, so we will know where each block starts
			IntBuffer indexs = IntBuffer.allocate(65536);
			
			while(node < 65536)
			{
				byte layer = nodes.get(index);
		        indexs.put(node, index);
				node++;
				index += layer*10+1;
			}
			PathNodes_index.put(regionoffset, indexs);
			PathNodes.put(regionoffset, nodes);
		} catch (Exception e)
		{
			e.printStackTrace();
			_log.warning("Failed to Load PathNode File: "+fname+"\n");
	    }
		
	}
}
