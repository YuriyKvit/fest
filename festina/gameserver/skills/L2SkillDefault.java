package com.festina.gameserver.skills;

import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.StatsSet;

public class L2SkillDefault extends L2Skill {

	public L2SkillDefault(StatsSet set) {
		super(set);
	}

	public void useSkill(L2Character caster, @SuppressWarnings("unused") L2Object[] targets) {
		caster.sendPacket(new ActionFailed());
		SystemMessage sm = new SystemMessage(614);
		sm.addString("Skill not implemented.  Skill ID: " + getId() + " " + getSkillType());
		caster.sendPacket(sm);
	}
	
}
