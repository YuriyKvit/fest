/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.festina.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.SkillTable;
import com.festina.gameserver.SkillTreeTable;
import com.festina.gameserver.model.L2EnchantSkillLearn;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2ShortCut;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.actor.instance.L2FolkInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ShortCutRegister;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.status.LoginStatusThread;
import com.festina.util.Rnd;
import com.festina.gameserver.util.Util;

/**
 * Format chdd
 * c: (id) 0xD0
 * h: (subid) 0x06
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public class RequestExEnchantSkill extends ClientBasePacket
{
	private static Logger _log = Logger.getLogger(LoginStatusThread.class.getName());
	private static final String _C__D0_07_REQUESTEXENCHANTSKILL = "[C] D0:07 RequestExEnchantSkill";
	private int _skillID;
	private int _skillLvl;

	/**
	 * @param buf
	 * @param client
	 */
	public RequestExEnchantSkill(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_skillID = readD();
		_skillLvl = readD();
	}
	
	@Override
	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		    if (player == null) {
		      return;
		    }
		    L2FolkInstance trainer = player.getLastFolkNPC();
		    if (trainer == null) {
		      return;
		    }
		    int npcid = trainer.getNpcId();
		
		    if (((trainer == null) || (!player.isInsideRadius(trainer, 150, false, false))) && (!player.isGM())) {
		      return;
		    }
		    if (player.getSkillLevel(_skillID) >= _skillLvl)
		      return;
		    if ((player.getClassId().getId() < 88) || (player.getLevel() < 76))
		      return;
		    L2Skill skill = SkillTable.getInstance().getInfo(_skillID, _skillLvl);
		
		    int counts = 0;
		    int _requiredSp = 10000000;
		    int _requiredExp = 100000;
		    byte _rate = 0;
		    int _baseLvl = 1;
		
		    L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
		
		    for (L2EnchantSkillLearn s : skills)
		    {
		      L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
		      if ((sk != null) && (sk == skill) && (sk.getCanLearn(player.getClassId())) && (sk.canTeachBy(npcid)))
		      {
		        counts++;
		        _requiredSp = s.getSpCost();
		        _requiredExp = s.getExp();
		        _rate = s.getRate(player);
		        _baseLvl = s.getBaseLevel();
		      }
		    }
		    if ((counts == 0) && (!Config.ALT_GAME_SKILL_LEARN))
		    {
		      player.sendMessage("You are trying to learn skill that u can't..");
		      Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", 2);
		
		      return;
		    }
		
		    if (player.getSp() >= _requiredSp)
		    {
		      if (player.getExp() >= _requiredExp)
		      {
		        if ((Config.SP_BOOK_NEEDED) && ((_skillLvl == 101) || (_skillLvl == 141)))
		        {
		          int spbId = 6622;
		
		          L2ItemInstance spb = player.getInventory().getItemByItemId(spbId);
		
		          if (spb == null)
		          {
		            player.sendMessage("You don't have all of the items needed to enchant that skill.");
		            return;
		          }
		
		          player.destroyItem("Consume", spb, trainer, true);
		        }
		      }
		      else
		      {
		        player.sendMessage("You don't have enough exp to enchant that skill.");
		      }
		
		    }
		    else
		    {
		      SystemMessage sm = new SystemMessage(278);
		      player.sendPacket(sm);
		      return;
		    }
		    if (Rnd.get(100) <= _rate)
		    {
		      player.addSkill(skill, true);
		
		      if (Config.DEBUG) 
		      {
		        _log.fine("Learned skill " + _skillID + " for " + _requiredSp + " SP.");
		      }
		      player.setSp(player.getSp() - _requiredSp);
		      player.setExp(player.getExp() - _requiredExp);
		      player.updateStats();
		
		      StatusUpdate su = new StatusUpdate(player.getObjectId());
		      su.addAttribute(StatusUpdate.SP, player.getSp());
		      player.sendPacket(su);
		
		      SystemMessage sm = new SystemMessage(277);
		      sm.addSkillName(_skillID);
		      player.sendPacket(sm);
		    }
		    else
		    {
		      if (skill.getLevel() > 100)
		      {
		        _skillLvl = _baseLvl;
		        player.addSkill(SkillTable.getInstance().getInfo(_skillID, _skillLvl), true);
		      }
		      player.sendMessage("You have failed to e");
		    }
		    trainer.showEnchantSkillList(player, player.getClassId());
		
		    L2ShortCut[] allShortCuts = player.getAllShortCuts();
		
		    for (L2ShortCut sc : allShortCuts)
		    {
		      if ((sc.getId() == _skillID) && (sc.getType() == 2))
		      {
		        L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _skillLvl, 1);
		        player.sendPacket(new ShortCutRegister(newsc));
		        player.registerShortCut(newsc);
		      }
		    }
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_07_REQUESTEXENCHANTSKILL;
	}
	
}