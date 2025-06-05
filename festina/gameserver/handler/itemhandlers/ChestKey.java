package com.festina.gameserver.handler.itemhandlers;

import com.festina.gameserver.SkillTable;
import com.festina.gameserver.ai.CtrlEvent;
import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.ai.L2CharacterAI;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.lib.Rnd;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.actor.instance.L2ChestInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.PlaySound;
import com.festina.gameserver.serverpackets.SocialAction;
import com.festina.gameserver.templates.L2Item;

public class ChestKey
  implements IItemHandler
{
  public static final int INTERACTION_DISTANCE = 100;
  private static int[] _itemIds = { 5197, 5198, 5199, 5200, 5201, 5202, 5203, 5204, 6665, 6666, 6667, 6668, 6669, 6670, 6671, 6672 };

  public boolean useSkill(L2PcInstance activeChar, int magicId, int level)
  {
    L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
    if (skill != null)
    {
      activeChar.doCast(skill);
      if (!activeChar.isSitting()) return true;
    }
    return false;
  }

  public void useItem(L2PlayableInstance playable, L2ItemInstance item)
  {
    if (!(playable instanceof L2PcInstance)) return;

    L2PcInstance activeChar = (L2PcInstance)playable;
    L2Skill skill = SkillTable.getInstance().getInfo(2065, 1);
    int itemId = item.getItemId();
    L2Object target = activeChar.getTarget();

    if ((!(target instanceof L2ChestInstance)) || (target == null))
    {
      activeChar.sendMessage("Invalid target.");
      activeChar.sendPacket(new ActionFailed());
    }
    else
    {
      L2ChestInstance chest = (L2ChestInstance)target;
      if ((chest.isDead()) || (chest.isOpen()))
      {
        activeChar.sendMessage("The chest Is empty.");
        activeChar.sendPacket(new ActionFailed());
        return;
      }
      if (!chest.isBox()) {
        activeChar.sendMessage("Use " + item.getItem().getName() + ".");
        playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
        activeChar.sendMessage("Failed to open chest");
        activeChar.sendPacket(new ActionFailed());
        chest.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
        chest.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
        return;
      }
      if ((activeChar.getAbnormalEffect() > 0) || (activeChar.isInCombat()))
      {
        activeChar.sendMessage("You cannot use the key,now.");
        activeChar.sendPacket(new ActionFailed());
        return;
      }

      if (!activeChar.isInsideRadius(chest, 100, false, false))
      {
        activeChar.sendMessage("Too far.");
        activeChar.sendPacket(new ActionFailed());
        return;
      }

      activeChar.sendMessage("Use " + item.getItem().getName() + ".");
      activeChar.useMagic(skill, false, false);

      if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false)) return;
      chest.setOpen();
      int openChance = 0;
      int chestGroup = 0;

      if (chest.getLevel() >= 80) chestGroup = 8;
      else if (chest.getLevel() >= 70) chestGroup = 7;
      else if (chest.getLevel() >= 60) chestGroup = 6;
      else if (chest.getLevel() >= 50) chestGroup = 5;
      else if (chest.getLevel() >= 40) chestGroup = 4;
      else if (chest.getLevel() >= 30) chestGroup = 3;
      else if (chest.getLevel() >= 20) chestGroup = 2; else {
        chestGroup = 1;
      }

      switch (itemId)
      {
      case 5197:
        if (chest.getLevel() == 1)
        {
          openChance = 60;
        }
        else
        {
          openChance = 10;
        }

        break;
      case 5198:
        if (chest.getLevel() == 10)
        {
          openChance = 60;
        } else {
          if (chest.getLevel() < 10)
          {
            sendKeyNotAdpated(activeChar, chest);
            return;
          }
          if (chest.getLevel() > 10)
          {
            openChance = 10; }  } break;
      case 5199:
        if (chest.getLevel() == 20)
        {
          openChance = 60;
        } else {
          if (chest.getLevel() < 20)
          {
            sendKeyNotAdpated(activeChar, chest);
            return;
          }
          if (chest.getLevel() > 20)
          {
            openChance = 10; }  } break;
      case 5200:
        if (chest.getLevel() == 30)
        {
          openChance = 60;
        } else {
          if (chest.getLevel() < 30)
          {
            sendKeyNotAdpated(activeChar, chest);
            return;
          }
          if (chest.getLevel() > 30)
          {
            openChance = 10; }  } break;
      case 5201:
        if (chest.getLevel() == 40)
        {
          openChance = 60;
        } else {
          if (chest.getLevel() < 40)
          {
            sendKeyNotAdpated(activeChar, chest);
            return;
          }
          if (chest.getLevel() > 40)
          {
            openChance = 10; }  } break;
      case 5202:
        if (chest.getLevel() == 50)
        {
          openChance = 60;
        } else {
          if (chest.getLevel() < 50)
          {
            sendKeyNotAdpated(activeChar, chest);
            return;
          }
          if (chest.getLevel() > 50)
          {
            openChance = 10; }  } break;
      case 5203:
        if (chest.getLevel() == 60)
        {
          openChance = 60;
        } else {
          if (chest.getLevel() < 60)
          {
            sendKeyNotAdpated(activeChar, chest);
            return;
          }
          if (chest.getLevel() > 60)
          {
            openChance = 10; }  } break;
      case 5204:
        if (chest.getLevel() == 70)
        {
          openChance = 60;
        } else {
          if (chest.getLevel() < 70)
          {
            sendKeyNotAdpated(activeChar, chest);
            return;
          }
          if (chest.getLevel() > 70)
          {
            openChance = 10; }  } break;
      case 6665:
        if (chestGroup == 1)
        {
          openChance = 100;
        }
        else if (chestGroup == 2)
        {
          openChance = 60;
        }
        else if (chestGroup == 3)
        {
          openChance = 20;
        }
        else
        {
          openChance = 0;
        }

        break;
      case 6666:
        if (chestGroup < 2)
        {
          sendKeyNotAdpated(activeChar, chest);
          return;
        }
        if (chestGroup == 2)
        {
          openChance = 100;
        }
        else if (chestGroup == 3)
        {
          openChance = 60;
        }
        else if (chestGroup == 4)
        {
          openChance = 20;
        }
        else
        {
          openChance = 0;
        }

        break;
      case 6667:
        if (chestGroup < 3)
        {
          sendKeyNotAdpated(activeChar, chest);
          return;
        }
        if (chestGroup == 3)
        {
          openChance = 100;
        }
        else if (chestGroup == 4)
        {
          openChance = 60;
        }
        else if (chestGroup == 5)
        {
          openChance = 20;
        }
        else
        {
          openChance = 0;
        }

        break;
      case 6668:
        if (chestGroup < 4)
        {
          sendKeyNotAdpated(activeChar, chest);
          return;
        }
        if (chestGroup == 4)
        {
          openChance = 100;
        }
        else if (chestGroup == 5)
        {
          openChance = 60;
        }
        else if (chestGroup == 6)
        {
          openChance = 20;
        }
        else
        {
          openChance = 0;
        }

        break;
      case 6669:
        if (chestGroup < 5)
        {
          sendKeyNotAdpated(activeChar, chest);
          return;
        }
        if (chestGroup == 5)
        {
          openChance = 100;
        }
        else if (chestGroup == 6)
        {
          openChance = 60;
        }
        else if (chestGroup == 7)
        {
          openChance = 20;
        }
        else
        {
          openChance = 0;
        }

        break;
      case 6670:
        if (chestGroup < 6)
        {
          sendKeyNotAdpated(activeChar, chest);
          return;
        }
        if (chestGroup == 6)
        {
          openChance = 100;
        }
        else if (chestGroup == 7)
        {
          openChance = 60;
        }
        else if (chestGroup == 8)
        {
          openChance = 20;
        }
        else
        {
          openChance = 0;
        }

        break;
      case 6671:
        if (chestGroup < 7)
        {
          sendKeyNotAdpated(activeChar, chest);
          return;
        }
        if (chestGroup == 7)
        {
          openChance = 100;
        }
        else if (chestGroup == 8)
        {
          openChance = 60; } break;
      case 6672:
        if (chestGroup < 8)
        {
          sendKeyNotAdpated(activeChar, chest);
          return;
        }
        if (chestGroup == 8)
        {
          openChance = 100; } break;
      default:
        sendKeyNotAdpated(activeChar, chest);
        return;
      }

      if ((openChance > 0) && (Rnd.get(100) < openChance))
      {
        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
        PlaySound playSound = new PlaySound("interfacesound.inventory_open_01");
        activeChar.sendPacket(playSound);
        activeChar.sendMessage("You open the chest!");
        chest.setHaveToDrop(true);
        chest.setMustRewardExpSp(false);
        chest.setSpecialDrop();
        chest.doItemDrop(activeChar);
        chest.doDie(activeChar);
      }
      else
      {
        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
        PlaySound playSound = new PlaySound("interfacesound.system_close_01");
        activeChar.sendPacket(playSound);
        activeChar.sendMessage("The key has been broken off!");

        if (Rnd.get(10) < 5) chest.chestTrap(activeChar);
        chest.setHaveToDrop(false);
        chest.setMustRewardExpSp(false);
        chest.doDie(activeChar);
      }
    }
  }

  private void sendKeyNotAdpated(L2PcInstance player, L2ChestInstance chest)
  {
    player.sendMessage("The key seems not to be adapted.");
    PlaySound playSound = new PlaySound("interfacesound.system_close_01");
    player.sendPacket(playSound);
    player.sendPacket(new ActionFailed());
    chest.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
    chest.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);
  }

  public int[] getItemIds()
  {
    return _itemIds;
  }
}