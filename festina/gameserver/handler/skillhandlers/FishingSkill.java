package com.festina.gameserver.handler.skillhandlers;

import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Fishing;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2Weapon;

public class FishingSkill implements ISkillHandler 
{ 
    //private static Logger _log = Logger.getLogger(SiegeFlag.class.getName()); 
	protected SkillType[] _skillIds = {SkillType.PUMPING, SkillType.REELING}; 
    
    public void useSkill(L2Character activeChar, @SuppressWarnings("unused") L2Skill skill, @SuppressWarnings("unused") L2Object[] targets)
    {
        if (activeChar == null || !(activeChar instanceof L2PcInstance)) return;

        L2PcInstance player = (L2PcInstance)activeChar;

		L2Fishing fish = player.GetFish();
        if (fish == null)
		{
			if (skill.getSkillType()==SkillType.PUMPING)
			{
                //Pumping skill is available only while fishing
				player.sendPacket(new SystemMessage(1462));
			}
			else if (skill.getSkillType()==SkillType.REELING)
			{
                //Reeling skill is available only while fishing
				player.sendPacket(new SystemMessage(1463));
			}			
			return;
		}
		L2Weapon weaponItem = player.getActiveWeaponItem();
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		int SS = 1;
		int pen = 0;
		if (weaponInst != null && weaponInst.getChargedFishshot()) SS = 2;
		double gradebonus = 1 + weaponItem.getCrystalType() * 0.145;
		int dmg = (int)(skill.getPower()*gradebonus*SS);		
		if (player.getSkillLevel(1315) <= skill.getLevel()-2) //1315 - Fish Expertise 
		{//Penalty
            pen = (skill.getLevel()-player.getSkillLevel(1315)-1)*5;
			int penatlydmg = (100-pen)*dmg/100;
			if (player.isGM()) player.sendMessage("Dmg w/o penalty = " +dmg);
			dmg = penatlydmg;			
		}
		if (SS > 1)
		{
			weaponInst.setChargedFishshot(false);
		}
		if (skill.getSkillType() == SkillType.REELING)//Realing
		{
			fish.UseRealing(dmg, pen);
		}
		else//Pumping
		{
			fish.UsePomping(dmg, pen);
		}
		
        
        
    } 
    
    public SkillType[] getSkillIds() 
    { 
        return _skillIds; 
    }    
}
