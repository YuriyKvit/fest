package com.festina.gameserver.skills;

import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.StatsSet;

public class L2SkillCharge extends L2Skill {

	final int num_charges;
	
	public L2SkillCharge(StatsSet set) {
		super(set);
		num_charges = set.getInteger("num_charges", getLevel());
	}

	public void useSkill(L2Character caster, @SuppressWarnings("unused") L2Object[] targets) {
		if (caster.isAlikeDead())
			return;
		
		// get the effect
		EffectCharge effect = (EffectCharge) caster.getEffect(this);
		if (effect != null) {
			if (effect.num_charges < num_charges)
			{
				effect.num_charges++;
				caster.updateEffectIcons();
                SystemMessage sm = new SystemMessage(614);
                sm.addString("Charged to " + effect.num_charges);
                caster.sendPacket(sm);
			}
			return;
		}
		this.getEffects(caster, caster);
	}
	
}
