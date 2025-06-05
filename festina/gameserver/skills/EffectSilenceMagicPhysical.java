package com.festina.gameserver.skills;

import com.festina.gameserver.model.L2Effect;

public class EffectSilenceMagicPhysical extends L2Effect {

   
    public EffectSilenceMagicPhysical(Env env, EffectTemplate template) {
        super(env, template);
    }

    public EffectType getEffectType() {
        return L2Effect.EffectType.SILENCE_MAGIC_PHYSICAL;
    }

    public void onStart()
    {
        getEffected().startMuted();
        getEffected().startPsychicalMuted();
    }
   
    public boolean onActionTime()
    {
        getEffected().stopMuted(this);
        getEffected().stopPsychicalMuted();
        return false;
    }

    public void onExit()
    {
        getEffected().stopMuted(this);
        getEffected().stopPsychicalMuted();
    }
}