package com.festina.gameserver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import com.festina.Config;
import com.festina.gameserver.model.L2CharPosition;

public class Universe implements java.io.Serializable
{

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = -2040223695811104704L;
    public static final int MIN_X = -127900;
    public static final int MAX_X = 194327;
    public static final int MIN_Y = -30000;
    public static final int MAX_Y = 259536;
    public static final int MIN_Z = -17000;
    public static final int MAX_Z = 17000;
    public static final int MIN_X_GRID = 60;
    public static final int MIN_Y_GRID = 60;
    public static final int MIN_Z_GRID = 60;
    public static final int MIN_GRID = 360;
    private static Universe _instance;
    protected static Logger _log = Logger.getLogger(Universe.class.getName());

    public static void main(String[] args)
    {
        Universe u = new Universe();
        u.load();
        //u.removeDoubles();
        u.implode(false);
    }

    private class Position implements Comparable, java.io.Serializable
    {
        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = -8798746764450022287L;
        int _x;
        int _flag;
        int _y;
        int _z;

        public Position(int x, int y, int z, int flag)
        {
            _x = x;
            _y = y;
            _z = z;
            _flag = flag;
        }

        public Position(L2CharPosition pos)
        {
            _x = pos.x;
            _y = pos.y;
            _z = pos.z;
            _flag = 0;
        }

        public L2CharPosition L2CP()
        {
            return new L2CharPosition(_x, _y, _z, 0);
        }

        public int compareTo(Object obj)
        {
            Position o = (Position) obj;
            int res = Integer.valueOf(_x).compareTo(o._x);
            if (res != 0) return res;
            res = Integer.valueOf(_y).compareTo(o._y);
            if (res != 0) return res;
            res = Integer.valueOf(_z).compareTo(o._z);
            return res;
        }

        public String toString()
        {
            return String.valueOf(_x) + " " + _y + " " + _z + " " + _flag;
        }
    }

    private class Coord implements Comparable, java.io.Serializable
    {
        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = -558060332886829552L;
        int _x;
        int _y;
        int _z;

        public Coord(int x, int y, int z)
        {
            _x = x;
            _y = y;
            _z = z;
        }

        public Coord(L2CharPosition pos)
        {
            _x = pos.x;
            _y = pos.y;
            _z = pos.z;
        }

        public int compareTo(Object obj)
        {
            Position o = (Position) obj;
            int res = Integer.valueOf(_x).compareTo(o._x);
            if (res != 0) return res;
            res = Integer.valueOf(_y).compareTo(o._y);
            if (res != 0) return res;
            res = Integer.valueOf(_z).compareTo(o._z);
            return res;
        }

        public String toString()
        {
            return String.valueOf(_x) + " " + _y + " " + _z;
        }
    }

    protected List<Coord> coordList;

    private HashSet<Integer> _logPlayers;
    private boolean _logAll = true;

    public static Universe getInstance()
    {
        if (_instance == null && Config.ACTIVATE_POSITION_RECORDER)
        {
            _instance = new Universe();
        }
        return _instance;
    }

    private Universe()
    {
        coordList = new LinkedList<Coord>();
        _logPlayers = new HashSet<Integer>();

        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new UniverseDump(), 30000, 30000);
    }

    public void registerHeight(int x, int y, int z)
    {
        // don't overwrite obstacle entries
        //Position p  = new Position(x, y, z, 0);
        //_map.add(p);
        coordList.add(new Coord(x, y, z));
        //if (Config.USE_3D_MAP) insertInto3DMap(p);
    }

    public void registerObstacle(int x, int y, int z)
    {
        //Position p = new Position(x, y, z, -1);
        //_map.add(p);
        coordList.add(new Coord(x, y, z));
        //if (Config.USE_3D_MAP) insertInto3DMap(p);
    }

    public boolean shouldLog(Integer id)
    {
        return (_logPlayers.contains(id) || _logAll);
    }

    public void setLogAll(boolean flag)
    {
        _logAll = flag;
    }

    public void addLogPlayer(Integer id)
    {
        _logPlayers.add(id);
        _logAll = false;
    }

    public void removeLogPlayer(Integer id)
    {
        _logPlayers.remove(id);
    }

    public void loadAscii()
    {
        int initialSize = coordList.size();
        try
        {
            BufferedReader r = new BufferedReader(new FileReader("data/universe.txt"));
            String line;
            while ((line = r.readLine()) != null)
            {
                StringTokenizer st = new StringTokenizer(line);
                String x1 = st.nextToken();
                String y1 = st.nextToken();
                String z1 = st.nextToken();
                //                String f1 = st.nextToken();
                int x = Integer.parseInt(x1);
                int y = Integer.parseInt(y1);
                int z = Integer.parseInt(z1);
                //                int f = Integer.parseInt(f1);
                coordList.add(new Coord(x, y, z));
            }
            r.close();
            _log.info((coordList.size() - initialSize) + " additional nodes loaded from text file.");
        }
        catch (Exception e)
        {
            _log.info("could not read text file universe.txt");
        }
    }

    public void createMap()
    {
        int zoom = 100;
        int w = (MAX_X - MIN_X) / zoom;
        int h = (MAX_Y - MIN_Y) / zoom;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        Graphics2D gr = bi.createGraphics();
        int min_z = 0, max_z = 0;
        for (Coord pos : coordList)
        {
            if (pos == null) continue;

            if (pos._z < min_z) min_z = pos._z;
            if (pos._z > max_z) max_z = pos._z;
        }
        for (Coord pos : coordList)
        {
            if (pos == null) continue;

            int x = (pos._x - MIN_X) / zoom;
            int y = (pos._y - MIN_Y) / zoom;
            int color = (int) (((long) pos._z - MIN_Z) * 0xFFFFFF / (MAX_Z - MIN_Z));
            gr.setColor(new Color(color));
            gr.drawLine(x, y, x, y);
        }
        try
        {
            ImageIO.write(bi, "png", new File("universe.png"));
        }
        catch (Exception e)
        {
            _log.warning("cannot create universe.png: " + e);
        }
    }

    public class UniverseFilter implements FilenameFilter
    {
        String ext = "";

        public UniverseFilter(String pExt)
        {
            this.ext = pExt;
        }

        /* (non-Javadoc)
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File arg0, String name)
        {
            return name.startsWith("universe") && name.endsWith("." + ext);
        }

    }

    public void load()
    {
        int total = 0;
        if (coordList == null)
        {
            coordList = new LinkedList<Coord>();
        }
        try
        {
            loadBinFiles();

            loadHexFiles();

            loadFinFiles();

            _log.info(coordList.size() + " map vertices loaded in total.");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        _log.info("Total: " + total);
    }

    /**
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void loadFinFiles() throws FileNotFoundException, IOException
    {
        FilenameFilter filter = new UniverseFilter("fin");
        File directory = new File("data");
        File[] files = directory.listFiles(filter);
        for (File file : files)
        {
            FileInputStream fos = new FileInputStream(file); // Save to file 
            DataInputStream data = new DataInputStream(fos);
            int count = data.readInt();
            List<Coord> newMap = new LinkedList<Coord>();
            for (int i = 0; i < count; i++)
            {
                newMap.add(new Coord(data.readInt(), data.readInt(), data.readInt()));
            }
            data.close(); // Close the stream.

            _log.info(newMap.size() + " map vertices loaded from file " + file.getName());

            coordList.addAll(newMap);
        }
    }

    /**
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void loadHexFiles() throws FileNotFoundException, IOException
    {
        FilenameFilter filter = new UniverseFilter("hex");
        File directory = new File("data");
        File[] files = directory.listFiles(filter);
        for (File file : files)
        {
            FileInputStream fos = new FileInputStream(file); // Save to file 
            GZIPInputStream gzos = new GZIPInputStream(fos);
            DataInputStream data = new DataInputStream(gzos);
            int count = data.readInt();
            List<Coord> newMap = new LinkedList<Coord>();
            for (int i = 0; i < count; i++)
            {
                newMap.add(new Coord(data.readInt(), data.readInt(), data.readInt()));
                data.readInt();
            }
            data.close(); // Close the stream.

            _log.info(newMap.size() + " map vertices loaded from file " + file.getName());

            coordList.addAll(newMap);
        }
    }

    /**
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings(value = {"unchecked"})
    private void loadBinFiles() throws FileNotFoundException, IOException, ClassNotFoundException
    {
        FilenameFilter filter = new UniverseFilter("bin");
        File directory = new File("data");
        File[] files = directory.listFiles(filter);
        for (File file : files)
        {
            //Create necessary input streams 
            FileInputStream fis = new FileInputStream(file); // Read from file 
            GZIPInputStream gzis = new GZIPInputStream(fis); // Uncompress 
            ObjectInputStream in = new ObjectInputStream(gzis); // Read objects 
            // Read in an object. It should be a vector of scribbles

            TreeSet<Position> temp = (TreeSet<Position>) in.readObject();
            _log.info(temp.size() + " map vertices loaded from file " + file.getName());
            in.close(); // Close the stream.
            for (Position p : temp)
            {
                coordList.add(new Coord(p._x, p._y, p._z));
            }
        }
    }

    public class UniverseDump implements Runnable
    {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            int size = coordList.size();
            //System.out.println("Univere Map has " + _map.size() + " nodes.");
            if (size > 100000)
            {
                flush();
            }
        }
    }

    public void flush()
    {
        //System.out.println("Size of dump: "+coordList.size());
        List<Coord> oldMap = coordList;
        coordList = new LinkedList<Coord>();
        int size = oldMap.size();
        dump(oldMap, true);
        _log.info("Universe Map : Dumped " + size + " vertices.");
    }

    public int size()
    {
        int size = 0;
        if (coordList != null) size = coordList.size();
        return size;
    }

    public void dump(List<Coord> _map, boolean b)
    {
        try
        {
            String pad = "";
            if (b) pad = "" + System.currentTimeMillis();
            FileOutputStream fos = new FileOutputStream("data/universe" + pad + ".fin"); // Save to file 
            DataOutputStream data = new DataOutputStream(fos);
            int count = _map.size();
            //System.out.println("Size of dump: "+count);
            data.writeInt(count);

            if (_map != null)
            {
                for (Coord p : _map)
                {
                    if (p != null)
                    {
                        data.writeInt(p._x);
                        data.writeInt(p._y);
                        data.writeInt(p._z);
                    }
                }
            }
            data.flush();
            data.close();
            _log.info("Universe Map saved to: " + "data/universe" + pad + ".fin");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // prepare for shutdown
    public void implode(boolean b)
    {
        createMap();
        dump(coordList, b);
    }
}
