package com.festina.gameserver.handler.itemhandlers;

import com.festina.gameserver.SkillTable;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.serverpackets.SystemMessage;

public class ValakasProtection
  implements IItemHandler
{
  private static int[] _scrolls = { 6652, 6653, 6655 };

  public void useItem(L2PlayableInstance playable, L2ItemInstance item) {
    L2PcInstance activeChar = (L2PcInstance)playable;

    if (activeChar == null) {
      return;
    }
    if (activeChar.isAllSkillsDisabled())
    {
      ActionFailed af = new ActionFailed();
      activeChar.sendPacket(af);
      return;
    }

    if (activeChar.isInOlympiadMode())
    {
      activeChar.sendPacket(new SystemMessage(1508));
      return;
    }

    if (activeChar.isDead()) {
      return;
    }
    int itemId = item.getItemId();

    if (itemId == 6652)
    {
      if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
        return;
      MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2231, 1, 1, 0);
      activeChar.broadcastPacket(MSU);
      L2Skill skill = SkillTable.getInstance().getInfo(2231, 1);
      if (skill != null) {
        activeChar.doCast(skill);
      }
    }
    else if ((itemId == 6653) || (itemId == 6654))
    {
      if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
        return;
      for (L2Effect ef : activeChar.getAllEffects())
      {
        if ((ef.getSkill().getId() == 4683) || (ef.getSkill().getId() == 4684))
        {
          activeChar.stopEffect(ef.getSkill().getId());
        }
      }
    }
    else if (itemId == 6655)
    {
      if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
        return;
      MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2232, 1, 1, 0);
      activeChar.broadcastPacket(MSU);
      L2Skill skill = SkillTable.getInstance().getInfo(2232, 1);
      if (skill != null)
        activeChar.doCast(skill);
    }
  }

  public int[] getItemIds()
  {
    return _scrolls;
  }
}