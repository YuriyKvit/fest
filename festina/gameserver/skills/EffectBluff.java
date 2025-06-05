package com.festina.gameserver.skills;

import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.serverpackets.StartRotation;
import com.festina.gameserver.serverpackets.StopRotation;

public class EffectBluff extends L2Effect
{
  public EffectBluff(Env env, EffectTemplate template)
  {
    super(env, template);
  }

  public L2Effect.EffectType getEffectType()
  {
    return L2Effect.EffectType.BLUFF;
  }

  public void onStart()
  {
    getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(), 1));
    getEffected().broadcastPacket(new StopRotation(getEffected(), getEffector().getHeading()));
    getEffected().setHeading(getEffector().getHeading());
  }

  public boolean onActionTime()
  {
    return false;
  }
}
