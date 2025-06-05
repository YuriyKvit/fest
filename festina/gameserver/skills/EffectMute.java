package com.festina.gameserver.skills;

import com.festina.gameserver.model.L2Effect;

public class EffectMute extends L2Effect {

	
	public EffectMute(Env env, EffectTemplate template) {
		super(env, template);
	}


	public EffectType getEffectType() {
		return L2Effect.EffectType.MUTE;
	}

	public void onStart() {
		getEffected().startMuted();
	}
	
	public boolean onActionTime() {
		// Simply stop the effect
		getEffected().stopMuted(this);
		return false;
	}


	public void onExit() {
		getEffected().stopMuted(this);
	}



}
