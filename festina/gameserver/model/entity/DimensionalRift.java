package com.festina.gameserver.model.entity;

import com.festina.Config;
import com.festina.gameserver.instancemanager.DimensionalRiftManager;
import com.festina.gameserver.instancemanager.DimensionalRiftManager.DimensionalRiftRoom;
import com.festina.gameserver.model.L2Party;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.util.Rnd;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javolution.util.FastList;

public class DimensionalRift
{
  private byte _type;
  private L2Party _party;
  private FastList<Byte> _completedRooms = new FastList<Byte>();
  private static final long seconds_5 = 5000L;
  private static final int MILLISECONDS_IN_MINUTE = 60000;
  private byte jumps_current = 0;
  private Timer teleporterTimer;
  private TimerTask teleporterTimerTask;
  private Timer spawnTimer;
  private TimerTask spawnTimerTask;
  private byte _choosenRoom = -1;
  private boolean _hasJumped = false;
  private FastList<L2PcInstance> deadPlayers = new FastList<L2PcInstance>();
  private FastList<L2PcInstance> revivedInWaitingRoom = new FastList<L2PcInstance>();
  private boolean isBossRoom = false;

  private static Logger _log = Logger.getLogger(DimensionalRift.class.getName());

  public DimensionalRift(L2Party party, byte type, byte room)
  {
    _type = type;
    _party = party;
    _choosenRoom = room;
    int[] coords = getRoomCoord(room);
    party.setDimensionalRift(this);
    for (L2PcInstance p : party.getPartyMembers())
      p.teleToLocation(coords[0], coords[1], coords[2]);
    createSpawnTimer(_choosenRoom);
    createTeleporterTimer(true);
  }

  public byte getType()
  {
    return _type;
  }

  public byte getCurrentRoom()
  {
    return _choosenRoom;
  }

  private void createTeleporterTimer(final boolean reasonTP)
  {
    if (teleporterTimerTask != null)
    {
      teleporterTimerTask.cancel();
      teleporterTimerTask = null;
    }

    if (teleporterTimer != null)
    {
      teleporterTimer.cancel();
      teleporterTimer = null;
    }

    teleporterTimer = new Timer();
    teleporterTimerTask = new TimerTask()
    {
      public void run()
      {
        if (_choosenRoom > -1) {
          DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn();
        }
        if ((reasonTP) && (jumps_current < getMaxJumps()) && (_party.getMemberCount() > deadPlayers.size()))
        {
          //DimensionalRift.;

          _completedRooms.add(Byte.valueOf(_choosenRoom));
          _choosenRoom = -1;

          for (L2PcInstance p : _party.getPartyMembers())
            if (!revivedInWaitingRoom.contains(p))
              DimensionalRift.this.teleportToNextRoom(p);
          DimensionalRift.this.createTeleporterTimer(true);
          createSpawnTimer(_choosenRoom);
        }
        else
        {
          for (L2PcInstance p : _party.getPartyMembers())
            if (!revivedInWaitingRoom.contains(p))
              DimensionalRift.this.teleportToWaitingRoom(p);
          killRift();
          cancel();
        }
      }
    };
    if (reasonTP)
      teleporterTimer.schedule(teleporterTimerTask, (long) calcTimeToNextJump());
    else
      teleporterTimer.schedule(teleporterTimerTask, 5000L);
  }

  public void createSpawnTimer(final byte room)
  {
    if (spawnTimerTask != null)
    {
      spawnTimerTask.cancel();
      spawnTimerTask = null;
    }

    if (spawnTimer != null)
    {
      spawnTimer.cancel();
      spawnTimer = null;
    }

    spawnTimer = new Timer();
    spawnTimerTask = new TimerTask()
    {
      public void run()
      {
        DimensionalRiftManager.getInstance().getRoom(_type, room).spawn();
      }
    };
    spawnTimer.schedule(spawnTimerTask, Config.RIFT_SPAWN_DELAY);
  }

  public void partyMemberInvited()
  {
    createTeleporterTimer(false);
  }

  public void partyMemberExited(L2PcInstance player)
  {
    if (deadPlayers.contains(player)) {
      deadPlayers.remove(player);
    }
    if (revivedInWaitingRoom.contains(player)) {
      revivedInWaitingRoom.remove(player);
    }
    if ((_party.getMemberCount() < Config.RIFT_MIN_PARTY_SIZE) || (_party.getMemberCount() == 1))
    {
      for (L2PcInstance p : _party.getPartyMembers())
        teleportToWaitingRoom(p);
      killRift();
    }
  }

  public void manualTeleport(L2PcInstance player, L2NpcInstance npc)
  {
    if ((!player.isInParty()) || (!player.getParty().isInDimensionalRift())) {
      return;
    }
    if (player.getObjectId() != player.getParty().getPartyLeaderOID())
    {
      DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
      return;
    }

    if (_hasJumped)
    {
      DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/AlreadyTeleported.htm", npc);
      return;
    }

    _hasJumped = true;

    DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn();
    _completedRooms.add(Byte.valueOf(_choosenRoom));
    _choosenRoom = -1;

    for (L2PcInstance p : _party.getPartyMembers()) {
      teleportToNextRoom(p);
    }
    createSpawnTimer(_choosenRoom);
    createTeleporterTimer(true);
  }

  public void manualExitRift(L2PcInstance player, L2NpcInstance npc)
  {
    if ((!player.isInParty()) || (!player.getParty().isInDimensionalRift())) {
      return;
    }
    if (player.getObjectId() != player.getParty().getPartyLeaderOID())
    {
      DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
      return;
    }

    for (L2PcInstance p : player.getParty().getPartyMembers())
      teleportToWaitingRoom(p);
    killRift();
  }

  private void teleportToNextRoom(L2PcInstance player)
  {
    if (_choosenRoom == -1) {
      do
        _choosenRoom = ((byte)Rnd.get(1, 9));
      while (_completedRooms.contains(Byte.valueOf(_choosenRoom)));
    }

    checkBossRoom(_choosenRoom);
    int[] coords = getRoomCoord(_choosenRoom);
    player.teleToLocation(coords[0], coords[1], coords[2]);
  }

  private void teleportToWaitingRoom(L2PcInstance player)
  {
    DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
  }

  public void killRift()
  {
    _completedRooms = null;

    if (_party != null) 
    {
      _party.setDimensionalRift(null);
    }
    _party = null;
    revivedInWaitingRoom = null;
    deadPlayers = null;
    DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn();
    DimensionalRiftManager.getInstance().killRift(this);
  }

  public Timer getTeleportTimer()
  {
    return teleporterTimer;
  }

  public TimerTask getTeleportTimerTask()
  {
    return teleporterTimerTask;
  }

  public Timer getSpawnTimer()
  {
    return spawnTimer;
  }

  public TimerTask getSpawnTimerTask()
  {
    return spawnTimerTask;
  }

  public void setTeleportTimer(Timer t)
  {
    teleporterTimer = t;
  }

  public void setTeleportTimerTask(TimerTask tt)
  {
    teleporterTimerTask = tt;
  }

  public void setSpawnTimer(Timer t)
  {
    spawnTimer = t;
  }

  public void setSpawnTimerTask(TimerTask st)
  {
    spawnTimerTask = st;
  }

  private float calcTimeToNextJump()
  {
    float time = Rnd.get(Config.RIFT_AUTO_JUMPS_TIME_MIN, Config.RIFT_AUTO_JUMPS_TIME_MAX) * 1000;

    if (isBossRoom) 
    {
     
    	//return  
    	 time = Config.RIFT_BOSS_ROOM_TIME_MUTIPLY;
    }
    
    
    return time;
  }

  public void memberDead(L2PcInstance player)
  {
    if (!deadPlayers.contains(player))
      deadPlayers.add(player);
  }

  public void memberRessurected(L2PcInstance player)
  {
    if (deadPlayers.contains(player))
      deadPlayers.remove(player);
  }

  public void usedTeleport(L2PcInstance player)
  {
    if (!revivedInWaitingRoom.contains(player)) {
      revivedInWaitingRoom.add(player);
    }
    if (!deadPlayers.contains(player)) {
      deadPlayers.add(player);
    }
    if (_party.getMemberCount() - revivedInWaitingRoom.size() < Config.RIFT_MIN_PARTY_SIZE)
    {
      int pcm = _party.getMemberCount();
      int rev = revivedInWaitingRoom.size();
      int min = Config.RIFT_MIN_PARTY_SIZE;

      for (L2PcInstance p : _party.getPartyMembers())
        if (!revivedInWaitingRoom.contains(p))
          teleportToWaitingRoom(p);
      killRift();
    }
  }

  public FastList<L2PcInstance> getDeadMemberList()
  {
    return deadPlayers;
  }

  public FastList<L2PcInstance> getRevivedAtWaitingRoom()
  {
    return revivedInWaitingRoom;
  }

  public void checkBossRoom(byte room)
  {
    isBossRoom = DimensionalRiftManager.getInstance().getRoom(_type, room).isBossRoom();
  }

  public int[] getRoomCoord(byte room)
  {
    return DimensionalRiftManager.getInstance().getRoom(_type, room).getTeleportCoords();
  }

  public byte getMaxJumps()
  {
    if ((Config.RIFT_MAX_JUMPS <= 8) && (Config.RIFT_MAX_JUMPS >= 1)) {
      return (byte)Config.RIFT_MAX_JUMPS;
    }
    return 4;
  }
}