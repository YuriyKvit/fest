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

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.SkillTable;
import com.festina.gameserver.SkillTreeTable;
import com.festina.gameserver.model.L2EnchantSkillLearn;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.actor.instance.L2FolkInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ExEnchantSkillInfo;

/**
 * Format chdd
 * c: (id) 0xD0
 * h: (subid) 0x06
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public class RequestExEnchantSkillInfo extends ClientBasePacket
{
	private static final String _C__D0_06_REQUESTEXENCHANTSKILLINFO = "[C] D0:06 RequestExEnchantSkillInfo";
	@SuppressWarnings("unused")
	private int _skillID;
	@SuppressWarnings("unused")
	private int _skillLvl;
	/**
	 * @param buf
	 * @param client
	 */
	public RequestExEnchantSkillInfo(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_skillID = readD();
		_skillLvl = readD();
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	void runImpl()
	{
		 L2PcInstance activeChar = getClient().getActiveChar();
		 
		     if (activeChar == null)
		       return;
		     if (activeChar.getLevel() < 76)
		       return;
		     L2FolkInstance trainer = activeChar.getLastFolkNPC();
		 
		     if (((trainer == null) || (!activeChar.isInsideRadius(trainer, 150, false, false))) && (!activeChar.isGM())) {
		       return;
		     }
		     L2Skill skill = SkillTable.getInstance().getInfo(_skillID, _skillLvl);
		 
		     boolean canteach = false;
		 
		     if ((skill == null) || (skill.getId() != _skillID))
		     {
		       activeChar.sendMessage("This skill doesn't yet have enchant info in Datapack. Please contact with admin.");
		       return;
		     }
		 
		     if (!trainer.getTemplate().canTeach(activeChar.getClassId())) {
		       return;
		     }
		     L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(activeChar);
		 
		     for (L2EnchantSkillLearn s : skills)
		     {
		       if ((s.getId() == _skillID) && (s.getLevel() == _skillLvl))
		       {
		         canteach = true;
		         break;
		       }
		     }
		 
		     if (!canteach) {
		       return;
		     }
		     int requiredSp = SkillTreeTable.getInstance().getSkillSpCost(activeChar, skill);
		     int requiredExp = SkillTreeTable.getInstance().getSkillExpCost(activeChar, skill);
		     byte rate = SkillTreeTable.getInstance().getSkillRate(activeChar, skill);
		     ExEnchantSkillInfo asi = new ExEnchantSkillInfo(skill.getId(), skill.getLevel(), requiredSp, requiredExp, rate);
		 
		     if ((Config.SP_BOOK_NEEDED) && ((skill.getLevel() == 101) || (skill.getLevel() == 141)))
		     {
		       int spbId = 6622;
		 
		       asi.addRequirement(4, spbId, 1, 0);
		     }
		 
		     sendPacket(asi);
			}
	/* (non-Javadoc)
	 * @see com.festina.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_06_REQUESTEXENCHANTSKILLINFO;
	}
	
}