package com.festina.gameserver.model.actor.instance;

import com.festina.gameserver.templates.L2NpcTemplate;
import com.festina.gameserver.model.actor.instance.L2MonsterInstance;

public class L2RiftInvaderInstance extends L2MonsterInstance
{
  private byte _type;
  private byte _room;

  public L2RiftInvaderInstance(int objectId, L2NpcTemplate template)
  {
    super(objectId, template);
  }

  public byte getType()
  {
    return _type;
  }

  public byte getRoom()
  {
    return _room;
  }

  public void setType(byte type)
  {
    _type = type;
  }

  public void setRoom(byte room)
  {
    _room = room;
  }
}