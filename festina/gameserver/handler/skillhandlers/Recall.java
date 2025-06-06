package com.festina.gameserver.handler.skillhandlers;

import com.festina.Config;
import com.festina.gameserver.MapRegionTable;
import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.SystemMessage;

public class Recall implements ISkillHandler
{
	//private static Logger _log = Logger.getLogger(Recall.class.getName());
	protected SkillType[] _skillIds = {SkillType.RECALL};

 	public void useSkill(@SuppressWarnings("unused") L2Character activeChar, @SuppressWarnings("unused") L2Skill skill, L2Object[] targets)
	{
        if (activeChar instanceof L2PcInstance)
        {
            if (((L2PcInstance)activeChar).isInOlympiadMode())
            {
                ((L2PcInstance)activeChar).sendPacket(new SystemMessage(SystemMessage.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
                return;
            }
        }
        
		try 
        {
			for (int index = 0; index < targets.length; index++)
			{
				if (!(targets[index] instanceof L2Character))
					continue;

				L2Character target = (L2Character)targets[index];

                if (target instanceof L2PcInstance)
                {
                    L2PcInstance targetChar = (L2PcInstance)target;
                    
                    // Check to see if the current player target is in a festival.
                    if (targetChar.isFestivalParticipant()) {
                        targetChar.sendPacket(SystemMessage.sendString("You may not use an escape skill in a festival."));
                        continue;
                    }
                    
                    // Check to see if player is in jail
                    if (targetChar.isInJail())
                    {
                        targetChar.sendPacket(SystemMessage.sendString("You can not escape from jail."));
                        continue;
                    }
                }
                  
                target.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
        } catch (Throwable e) {
 	 	 	if (Config.DEBUG) e.printStackTrace();
 	 	}
 	}

	public SkillType[] getSkillIds()
	{
		return _skillIds;
	}
}