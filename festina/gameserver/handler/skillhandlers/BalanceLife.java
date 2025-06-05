package com.festina.gameserver.handler.skillhandlers;
	
import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.handler.SkillHandler;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
/**
 	* This class ...
 	*
	* @author earendil
	*
	* @version $Revision: 1.1.2.2.2.4 $ $Date: 2005/04/06 16:13:48 $
	*/

		public class BalanceLife implements ISkillHandler
		{
			private static SkillType[] _skillIds = {SkillType.BALANCE_LIFE};
	       
			public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
			{
				//L2Character activeChar = activeChar;
				//check for other effects
				try {
					ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.BUFF);

					if (handler != null)
						handler.useSkill(activeChar, skill, targets);
				} 
				catch (Exception e) {}

				L2Character target = null;
 
					L2PcInstance player = null;
					if (activeChar instanceof L2PcInstance)
						player = (L2PcInstance)activeChar;
       
					double fullHP = 0;
					double currentHPs = 0;
 
					for (int index = 0; index < targets.length; index++)
					{
						target = (L2Character)targets[index];
                      
		// We should not heal if char is dead
						if (target == null || target.isDead())
							continue;
         
						fullHP += target.getMaxHp();
						currentHPs += target.getCurrentHp();
					}
					double percentHP = currentHPs / fullHP;
          
					for (int index = 0; index < targets.length; index++)
					{       

						double newHP = target.getMaxHp() * percentHP;
						double totalHeal = newHP - target.getCurrentHp();
       
						target.setCurrentHp(newHP);
                              
						if(totalHeal > 0)
							target.setLastHealAmount((int)totalHeal);       

		StatusUpdate su = new StatusUpdate(target.getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int)target.getCurrentHp());
		target.sendPacket(su);
            
		SystemMessage sm = new SystemMessage(614);
		sm.addString("HP of the party has been balanced.");
		target.sendPacket(sm);
		
					}
			}
   
     
			public SkillType[] getSkillIds()
			{
				return _skillIds;
			}
		}
