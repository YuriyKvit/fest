package com.festina.gameserver.model.zone.type;

import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.zone.L2ZoneType;
import com.festina.gameserver.serverpackets.NpcInfo;

public class L2WaterZone extends L2ZoneType
{
  public L2WaterZone(int id)
  {
  }

  protected void onEnter(L2Character character)
  {
    character.setInsideZone(128, true);

    if ((character instanceof L2PcInstance))
    {
      if (((L2PcInstance)character).isMounted()) {
        ((L2PcInstance)character).dismount();
      }
      else
      {
        ((L2PcInstance)character).broadcastUserInfo();
      }
    } else if ((character instanceof L2NpcInstance))
    {
      for (L2PcInstance player : character.getKnownList().getKnownPlayers().values())
        if (player != null)
          player.sendPacket(new NpcInfo((L2NpcInstance)character, player));
    }
  }

  protected void onExit(L2Character character)
  {
    character.setInsideZone(128, false);

    if ((character instanceof L2PcInstance))
    {
      ((L2PcInstance)character).broadcastUserInfo();
    }
    else if ((character instanceof L2NpcInstance))
    {
      for (L2PcInstance player : character.getKnownList().getKnownPlayers().values())
        if (player != null)
          player.sendPacket(new NpcInfo((L2NpcInstance)character, player));
    }
  }
}