package com.festina.gameserver.instancemanager;

import com.festina.Config;
import com.festina.L2DatabaseFactory;
import com.festina.gameserver.NpcTable;
import com.festina.gameserver.SpawnTable;
import com.festina.gameserver.instancemanager.DimensionalRiftManager.DimensionalRiftRoom;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Party;
import com.festina.gameserver.model.L2Spawn;
import com.festina.gameserver.model.PcInventory;
import com.festina.gameserver.model.SpawnListener;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2RiftInvaderInstance;
import com.festina.gameserver.model.entity.DimensionalRift;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.templates.L2NpcTemplate;
import com.festina.gameserver.util.Util;
import com.festina.util.Rnd;
import java.awt.Polygon;
import java.awt.Shape;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DimensionalRiftManager
{
  private static Logger _log = Logger.getLogger(DimensionalRiftManager.class.getName());
  private static DimensionalRiftManager _instance;
  private FastMap<Byte, FastMap<Byte, DimensionalRiftRoom>> _rooms = new FastMap<Byte, FastMap<Byte, DimensionalRiftRoom>>();
  private final short DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;

  public static DimensionalRiftManager getInstance()
  {
    if (_instance == null) {
      _instance = new DimensionalRiftManager();
    }
    return _instance;
  }

  public DimensionalRiftManager()
  {
    loadRooms();
    loadSpawns();
  }

  public DimensionalRiftRoom getRoom(byte _type, byte room)
  {
    return (DimensionalRiftRoom)(_rooms.get(Byte.valueOf(_type))).get(Byte.valueOf(room));
  }

  private void loadRooms()
  {
    _log.info("Loading DimensionalRiftManager");
    Connection con = null;
    try
    {
      con = L2DatabaseFactory.getInstance().getConnection();
      PreparedStatement s = con.prepareStatement("SELECT * FROM dimensional_rift");
      ResultSet rs = s.executeQuery();

      while (rs.next())
      {
        byte type = rs.getByte("type");
        byte room_id = rs.getByte("room_id");

        int xMin = rs.getInt("xMin");
        int xMax = rs.getInt("xMax");
        int yMin = rs.getInt("yMin");
        int yMax = rs.getInt("yMax");
        int z1 = rs.getInt("zMin");
        int z2 = rs.getInt("zMax");
        int xT = rs.getInt("xT");
        int yT = rs.getInt("yT");
        int zT = rs.getInt("zT");
        boolean isBossRoom = rs.getByte("boss") > 0;

        if (!_rooms.containsKey(type)) {
          _rooms.put((type), new FastMap<Byte, DimensionalRiftRoom>());
        }
        (_rooms.get(Byte.valueOf(type))).put(Byte.valueOf(room_id), new DimensionalRiftRoom(type, room_id, xMin, xMax, yMin, yMax, z1, z2, xT, yT, zT, isBossRoom));
      }

      s.close();
      con.close();
    }
    catch (Exception e)
    {
      _log.warning("Can't load Dimension Rift zones. " + e);
    }
    finally
    {
      try
      {
        con.close();
      }
      catch (Exception e)
      {
      }
    }
    int typeSize = _rooms.keySet().size();
    int roomSize = 0;

    for (Byte b : _rooms.keySet()) {
      roomSize += (_rooms.get(b)).keySet().size();
    }
    _log.info("Loaded: " + typeSize + " room types with " + roomSize + " rooms.");
  }

  public void loadSpawns()
  {
    int countGood = 0; int countBad = 0;
    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setIgnoringComments(true);

      File file = new File(Config.DATAPACK_ROOT + "/data/dimensionalRift.xml");
      if (!file.exists()) {
        throw new IOException();
      }
      Document doc = factory.newDocumentBuilder().parse(file);

      for (Node rift = doc.getFirstChild(); rift != null; rift = rift.getNextSibling())
      {
        if ("rift".equalsIgnoreCase(rift.getNodeName()))
        {
          for (Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
          {
            if ("area".equalsIgnoreCase(area.getNodeName()))
            {
              NamedNodeMap attrs = area.getAttributes();
              byte type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());

              for (Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
              {
                if ("room".equalsIgnoreCase(room.getNodeName()))
                {
                  attrs = room.getAttributes();
                  byte roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());

                  for (Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
                  {
                    if ("spawn".equalsIgnoreCase(spawn.getNodeName()))
                    {
                      attrs = spawn.getAttributes();
                      int mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());

                      int delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
                      int count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());

                      L2NpcTemplate template = NpcTable.getInstance().getTemplate(mobId);
                      if (template == null) _log.warning("Template " + mobId + " not found!");
                      if (!_rooms.containsKey(Byte.valueOf(type))) _log.warning("Type " + type + " not found!");
                      else if (!(_rooms.get(Byte.valueOf(type))).containsKey(Byte.valueOf(roomId))) _log.warning("Room " + roomId + " in Type " + type + " not found!");

                      for (int i = 0; i < count; i++)
                      {
                        DimensionalRiftRoom riftRoom = (DimensionalRiftRoom)(_rooms.get(Byte.valueOf(type))).get(Byte.valueOf(roomId));
                        int x = riftRoom.getRandomX();
                        int y = riftRoom.getRandomY();
                        int z = riftRoom.getTeleportCoords()[2];

                        if ((template != null) && (_rooms.containsKey(Byte.valueOf(type))) && ((_rooms.get(Byte.valueOf(type))).containsKey(Byte.valueOf(roomId))))
                        {
                          L2Spawn spawnDat = new L2Spawn(template);
                          spawnDat.setAmount(1);
                          spawnDat.setLocx(x);
                          spawnDat.setLocy(y);
                          spawnDat.setLocz(z);
                          spawnDat.setHeading(-1);
                          spawnDat.setRespawnDelay(delay);
                          L2Spawn.addSpawnListener(new RiftSpawnListener(type, roomId));

                          SpawnTable.getInstance().addNewSpawn(spawnDat, false);
                          ((DimensionalRiftRoom)(_rooms.get(Byte.valueOf(type))).get(Byte.valueOf(roomId))).getSpawns().add(spawnDat);
                          countGood++;
                        }
                        else
                        {
                          countBad++;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      _log.warning("Error on loading dimensional rift spawns: " + e);
      e.printStackTrace();
    }
    _log.info("DimensionalRiftManager: Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
  }

  public void reload()
  {
    for (Byte b : _rooms.keySet())
    {
      for (Iterator<?> i$ = (_rooms.get(b)).keySet().iterator(); i$.hasNext(); ) { int i = ((Byte)i$.next()).byteValue();

        ((DimensionalRiftRoom)(_rooms.get(b)).get(Integer.valueOf(i))).getSpawns().clear();
      }
      _rooms.get(b).clear();
    }
    _rooms.clear();
    loadRooms();
    loadSpawns();
  }

  public boolean checkIfInRiftZone(int x, int y, int z, boolean ignorePeaceZone)
  {
    if (ignorePeaceZone) {
      return ((DimensionalRiftRoom)(_rooms.get(Byte.valueOf((byte)0))).get(Byte.valueOf((byte)1))).checkIfInZone(x, y, z);
    }
    return (((DimensionalRiftRoom)(_rooms.get(Byte.valueOf((byte)0))).get(Byte.valueOf((byte)1))).checkIfInZone(x, y, z)) && (!((DimensionalRiftRoom)(_rooms.get(Byte.valueOf((byte)0))).get(Byte.valueOf((byte)0))).checkIfInZone(x, y, z));
  }

  public boolean checkIfInPeaceZone(int x, int y, int z)
  {
    return ((DimensionalRiftRoom)(_rooms.get(Byte.valueOf((byte)0))).get(Byte.valueOf((byte)0))).checkIfInZone(x, y, z);
  }

  public void teleportToWaitingRoom(L2PcInstance player)
  {
    int[] coords = getRoom((byte)0, (byte)0).getTeleportCoords();
    player.teleToLocation(coords[0], coords[1], coords[2]);
  }

  public void start(L2PcInstance player, byte type, L2NpcInstance npc)
  {
    boolean canPass = true;
    if (!player.isInParty())
    {
      showHtmlFile(player, "data/html/seven_signs/rift/NoParty.htm", npc);
      return;
    }

    if (player.getParty().getPartyLeaderOID() != player.getObjectId())
    {
      showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
      return;
    }

    if (player.getParty().isInDimensionalRift())
    {
      handleCheat(player, npc);
      return;
    }

    if (player.getParty().getMemberCount() < Config.RIFT_MIN_PARTY_SIZE)
    {
      NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
      html.setFile("data/html/seven_signs/rift/SmallParty.htm");
      html.replace("%npc_name%", npc.getName());
      html.replace("%count%", new Integer(Config.RIFT_MIN_PARTY_SIZE).toString());
      player.sendPacket(html);
      return;
    }

    for (L2PcInstance p : player.getParty().getPartyMembers()) {
      if (!checkIfInPeaceZone(p.getX(), p.getY(), p.getZ()))
        canPass = false;
    }
    if (!canPass)
    {
      showHtmlFile(player, "data/html/seven_signs/rift/NotInWaitingRoom.htm", npc);
      return;
    }

    for (L2PcInstance p : player.getParty().getPartyMembers())
    {
      L2ItemInstance i = p.getInventory().getItemByItemId(7079);

      if (i == null)
      {
        canPass = false;
        break;
      }

      if ((i.getCount() > 0) && 
        (i.getCount() < getNeededItems(type))) {
        canPass = false;
      }
    }
    if (!canPass)
    {
      NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
      html.setFile("data/html/seven_signs/rift/NoFragments.htm");
      html.replace("%npc_name%", npc.getName());
      html.replace("%count%", new Integer(getNeededItems(type)).toString());
      player.sendPacket(html);
      return;
    }

    for (L2PcInstance p : player.getParty().getPartyMembers())
    {
      L2ItemInstance i = p.getInventory().getItemByItemId(7079);
      p.destroyItem("RiftEntrance", i.getObjectId(), getNeededItems(type), null, false);
    }

    new DimensionalRift(player.getParty(), type, (byte)Rnd.get(1, 9));
  }

  public void killRift(DimensionalRift d)
  {
    if (d.getTeleportTimerTask() != null) d.getTeleportTimerTask().cancel();
    d.setTeleportTimerTask(null);

    if (d.getTeleportTimer() != null) d.getTeleportTimer().cancel();
    d.setTeleportTimer(null);

    if (d.getSpawnTimerTask() != null) d.getSpawnTimerTask().cancel();
    d.setSpawnTimerTask(null);

    if (d.getSpawnTimer() != null) d.getSpawnTimer().cancel();
    d.setSpawnTimer(null);
  }

  private int getNeededItems(byte type)
  {
    switch (type)
    {
    case 1:
      return Config.RIFT_ENTER_COST_RECRUIT;
    case 2:
      return Config.RIFT_ENTER_COST_SOLDIER;
    case 3:
      return Config.RIFT_ENTER_COST_OFFICER;
    case 4:
      return Config.RIFT_ENTER_COST_CAPTAIN;
    case 5:
      return Config.RIFT_ENTER_COST_COMMANDER;
    case 6:
      return Config.RIFT_ENTER_COST_HERO;
    }
    return 999999;
  }

  public void showHtmlFile(L2PcInstance player, String file, L2NpcInstance npc)
  {
    NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
    html.setFile(file);
    html.replace("%npc_name%", npc.getName());
    player.sendPacket(html);
  }

  public void handleCheat(L2PcInstance player, L2NpcInstance npc)
  {
    showHtmlFile(player, "data/html/seven_signs/rift/Cheater.htm", npc);
    if (!player.isGM())
    {
      _log.warning("Player " + player.getName() + "(" + player.getObjectId() + ") was cheating in dimension rift area!");
      Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " tried to cheat in dimensional rift.", Config.DEFAULT_PUNISH);
    }
  }

  private class RiftSpawnListener
    implements SpawnListener
  {
    private byte _type;
    private byte _room;

    public RiftSpawnListener(byte type, byte room)
    {
      _type = type;
      _room = room;
    }

    public void npcSpawned(L2NpcInstance mob)
    {
      if ((mob instanceof L2RiftInvaderInstance))
      {
        L2RiftInvaderInstance rMob = (L2RiftInvaderInstance) mob;
        rMob.setType(_type);
        rMob.setType(_room);
      }
    }
  }

  public class DimensionalRiftRoom
  {
    private final byte _type;
    private final byte _room;
    private final int _xMin;
    private final int _xMax;
    private final int _yMin;
    private final int _yMax;
    private final int _zMin;
    private final int _zMax;
    private final int[] _teleportCoords;
    private final Shape _s;
    private final boolean _isBossRoom;
    private final FastList<L2Spawn> _roomSpawns;
    private final FastList<L2NpcInstance> _roomMobs;

    public DimensionalRiftRoom(byte type, byte room, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax, int xT, int yT, int zT, boolean isBossRoom)
    {
      _type = type;
      _room = room;
      _xMin = (xMin + 128);
      _xMax = (xMax - 128);
      _yMin = (yMin + 128);
      _yMax = (yMax - 128);
      _zMin = zMin;
      _zMax = zMax;
      _teleportCoords = new int[] { xT, yT, zT };
      _isBossRoom = isBossRoom;
      _roomSpawns = new FastList<L2Spawn>();
      _roomMobs = new FastList<L2NpcInstance>();
      _s = new Polygon(new int[] { xMin, xMax, xMax, xMin }, new int[] { yMin, yMin, yMax, yMax }, 4);
    }

    public int getRandomX()
    {
      return Rnd.get(_xMin, _xMax);
    }

    public int getRandomY()
    {
      return Rnd.get(_yMin, _yMax);
    }

    public int[] getTeleportCoords()
    {
      return _teleportCoords;
    }

    public boolean checkIfInZone(int x, int y, int z)
    {
      return (_s.contains(x, y)) && (z >= _zMin) && (z <= _zMax);
    }

    public boolean isBossRoom()
    {
      return _isBossRoom;
    }

    public FastList<L2Spawn> getSpawns()
    {
      return _roomSpawns;
    }

    public void spawn()
    {
      for (L2Spawn spawn : _roomSpawns)
      {
        spawn.doSpawn();
        spawn.startRespawn();
      }
    }

    public void unspawn()
    {
      for (L2Spawn spawn : _roomSpawns)
      {
        spawn.stopRespawn();
        spawn.getLastSpawn().deleteMe();
      }
    }
  }
}