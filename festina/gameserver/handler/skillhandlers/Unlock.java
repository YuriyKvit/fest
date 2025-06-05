package com.festina.gameserver.handler.skillhandlers;

import com.festina.gameserver.ai.CtrlEvent;
import com.festina.gameserver.ai.L2CharacterAI;
import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.lib.Rnd;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2ChestInstance;
import com.festina.gameserver.model.actor.instance.L2DoorInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.skills.Formulas;

public class Unlock
  implements ISkillHandler
{
  protected L2Skill.SkillType[] _skillIds = { L2Skill.SkillType.UNLOCK };

  public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
  {
    L2Object[] targetList = skill.getTargetList(activeChar);

    for (int index = 0; index < targetList.length; index++)
    {
      L2Object target = targetList[index];

      boolean success = Formulas.getInstance().calculateUnlockChance(skill);
      if ((target instanceof L2DoorInstance))
      {
        L2DoorInstance door = (L2DoorInstance)target;
        if (!door.isUnlockable())
        {
          activeChar.sendPacket(new SystemMessage(319));
          activeChar.sendPacket(new ActionFailed());
          return;
        }

        if ((success) && (door.getOpen() == 1))
        {
          door.openMe();
          door.onOpen();
          SystemMessage systemmessage = new SystemMessage(614);

          systemmessage.addString("Unlock the door!");
          activeChar.sendPacket(systemmessage);
        }
        else
        {
          activeChar.sendPacket(new SystemMessage(320));
        }
      }
      else if ((target instanceof L2ChestInstance))
      {
        L2ChestInstance chest = (L2ChestInstance) targetList[index];
        if ((chest.getCurrentHp() <= 0.0D) || (chest.isOpen()))
        {
          activeChar.sendPacket(new ActionFailed());
          return;
        }

        int chestChance = 0;
        int chestGroup = 0;
        int chestTrapLimit = 0;

        if (chest.getLevel() > 60) chestGroup = 4;
        else if (chest.getLevel() > 40) chestGroup = 3;
        else if (chest.getLevel() > 30) chestGroup = 2; else {
          chestGroup = 1;
        }
        switch (chestGroup)
        {
        case 1:
          if (skill.getLevel() > 10) chestChance = 100;
          else if (skill.getLevel() >= 3) chestChance = 50;
          else if (skill.getLevel() == 2) chestChance = 45;
          else if (skill.getLevel() == 1) chestChance = 40;

          chestTrapLimit = 10;

          break;
        case 2:
          if (skill.getLevel() > 12) chestChance = 100;
          else if (skill.getLevel() >= 7) chestChance = 50;
          else if (skill.getLevel() == 6) chestChance = 45;
          else if (skill.getLevel() == 5) chestChance = 40;
          else if (skill.getLevel() == 4) chestChance = 35;
          else if (skill.getLevel() == 3) chestChance = 30;

          chestTrapLimit = 30;

          break;
        case 3:
          if (skill.getLevel() >= 14) chestChance = 50;
          else if (skill.getLevel() == 13) chestChance = 45;
          else if (skill.getLevel() == 12) chestChance = 40;
          else if (skill.getLevel() == 11) chestChance = 35;
          else if (skill.getLevel() == 10) chestChance = 30;
          else if (skill.getLevel() == 9) chestChance = 25;
          else if (skill.getLevel() == 8) chestChance = 20;
          else if (skill.getLevel() == 7) chestChance = 15;
          else if (skill.getLevel() == 6) chestChance = 10;

          chestTrapLimit = 50;

          break;
        case 4:
          if (skill.getLevel() >= 14) chestChance = 50;
          else if (skill.getLevel() == 13) chestChance = 45;
          else if (skill.getLevel() == 12) chestChance = 40;
          else if (skill.getLevel() == 11) chestChance = 35;

          chestTrapLimit = 80;
        }

        chest.setOpen();
        if (chestChance == 0)
        {
          activeChar.sendPacket(SystemMessage.sendString("Too hard to open for you.."));
          activeChar.sendPacket(new ActionFailed());
          chest.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
          return;
        }

        if (Rnd.get(120) < chestChance)
        {
          activeChar.sendPacket(SystemMessage.sendString("You open the chest!"));

          chest.setSpecialDrop();
          chest.setHaveToDrop(true);
          chest.setMustRewardExpSp(false);
          chest.doItemDrop(activeChar);
          chest.doDie(activeChar);
        }
        else
        {
          activeChar.sendPacket(SystemMessage.sendString("Unlock failed!"));

          if (Rnd.get(100) < chestTrapLimit) chest.chestTrap(activeChar);
          chest.setHaveToDrop(false);
          chest.setMustRewardExpSp(false);
          chest.doDie(activeChar);
        }
      }
    }
  }

  public L2Skill.SkillType[] getSkillIds()
  {
    return _skillIds;
  }
}