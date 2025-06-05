package com.festina.gameserver.model.actor.instance;

import com.festina.Config;
import com.festina.gameserver.NpcTable;
import com.festina.gameserver.SkillTable;
import com.festina.gameserver.lib.Rnd;
import com.festina.gameserver.model.L2Attackable;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.serverpackets.MagicSkillUser;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2NpcTemplate;

public final class L2ChestInstance extends L2Attackable
{
  private volatile boolean _isBox;
  private volatile boolean _isOpen;
  private volatile boolean _specialDrop;

  public L2ChestInstance(int objectId, L2NpcTemplate template)
  {
    super(objectId, template);
    _isBox = (Rnd.get(100) < 20);
    _isOpen = false;
    _specialDrop = false;
  }

  public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
    super.reduceCurrentHp(damage, attacker, awake);
    if ((!isAlikeDead()) && (_isBox))
    {
      setHaveToDrop(false);
      setMustRewardExpSp(false);
      doDie(attacker);
    }
  }

  public boolean isAutoAttackable(L2Character attacker)
  {
    return true;
  }

  public boolean isAttackable()
  {
    return true;
  }

  public void doDie(L2Character killer)
  {
    killer.setTarget(null);
    setCurrentHpMp(0.0D, 0.0D);
    super.doDie(killer);
  }

  public boolean isAggressive()
  {
    return false;
  }

  public void OnSpawn()
  {
    super.OnSpawn();

    _isBox = (Rnd.get(100) < Config.BOX_CHANCE);

    _isOpen = false;
    _specialDrop = false;
    _specialDrop = true;
    setMustRewardExpSp(true);
    setHaveToDrop(true);
  }

  public synchronized boolean isBox() {
    return _isBox;
  }

  public synchronized boolean isOpen() {
    return _isOpen;
  }
  public synchronized void setOpen() {
    _isOpen = true;
  }

  public synchronized boolean isSpecialDrop() {
    return _specialDrop;
  }

  public synchronized void setSpecialDrop()
  {
    _specialDrop = true;
  }

  public void doItemDrop(L2NpcTemplate npcTemplate, L2Character lastAttacker)
  {
    int id = getTemplate().npcId;

    if (_specialDrop)
    {
      id += 18265;
      super.doItemDrop(NpcTable.getInstance().getTemplate(id), lastAttacker);
    }
    else
    {
      super.doItemDrop(NpcTable.getInstance().getTemplate(id), lastAttacker);
    }
  }

  public void chestTrap(L2Character player)
  {
    int trapSkillId = 0;
    int rnd = Rnd.get(120);

    if (getTemplate().level >= 61)
    {
      if (rnd >= 90) trapSkillId = 4139;
      else if (rnd >= 50) trapSkillId = 4118;
      else if (rnd >= 20) trapSkillId = 1167; else
        trapSkillId = 223;
    }
    else if (getTemplate().level >= 41)
    {
      if (rnd >= 90) trapSkillId = 4139;
      else if (rnd >= 60) trapSkillId = 96;
      else if (rnd >= 20) trapSkillId = 1167; else
        trapSkillId = 4118;
    }
    else if (getTemplate().level >= 21)
    {
      if (rnd >= 80) trapSkillId = 4139;
      else if (rnd >= 50) trapSkillId = 96;
      else if (rnd >= 20) trapSkillId = 1167; else {
        trapSkillId = 129;
      }

    }
    else if (rnd >= 80) trapSkillId = 4139;
    else if (rnd >= 50) trapSkillId = 96; else {
      trapSkillId = 129;
    }

    player.sendPacket(SystemMessage.sendString("There was a trap!"));
    handleCast(player, trapSkillId);
  }

  private boolean handleCast(L2Character player, int skillId)
  {
    int skillLevel = 1;

    if ((getTemplate().level > 20) && (getTemplate().level <= 40)) skillLevel = 3;
    else if ((getTemplate().level > 40) && (getTemplate().level <= 60)) skillLevel = 5;
    else if (getTemplate().level > 60) skillLevel = 6;

    if ((player.isDead()) || (!player.isVisible()) || (!player.isInsideRadius(this, getDistanceToWatchObject(player), false, false)))
    {
      return false;
    }
    L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);

    if (player.getEffect(skill) == null)
    {
      skill.getEffects(this, player);
      broadcastPacket(new MagicSkillUser(this, player, skill.getId(), skillLevel, skill.getSkillTime(), 0));

      return true;
    }
    return false;
  }
}