package com.festina.gameserver.skills;

import com.festina.gameserver.model.L2Effect;

/**
 * @author -Nemesiss-
 *
 */
public class EffectPsychicalMute extends L2Effect {

    
    public EffectPsychicalMute(Env env, EffectTemplate template) {
        super(env, template);
    }


    public EffectType getEffectType() {
        return L2Effect.EffectType.PSYCHICAL_MUTE;
    }

    public void onStart() {
        getEffected().startPsychicalMuted();
    }
    
    public boolean onActionTime() {
        // Simply stop the effect
        getEffected().stopPsychicalMuted();
        return false;
    }

    public void onExit() {
        getEffected().stopPsychicalMuted();
    }
}
