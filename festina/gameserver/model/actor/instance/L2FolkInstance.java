package com.festina.gameserver.model.actor.instance;

import javolution.lang.TextBuilder;
import com.festina.Config;
import com.festina.gameserver.SkillTable;
import com.festina.gameserver.SkillTreeTable;
import com.festina.gameserver.model.L2EnchantSkillLearn;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2SkillLearn;
import com.festina.gameserver.model.base.ClassId;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.AquireSkillList;
import com.festina.gameserver.serverpackets.ExEnchantSkillList;
import com.festina.gameserver.serverpackets.NpcHtmlMessage;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2NpcTemplate;

public class L2FolkInstance extends L2NpcInstance 
{
	//private static Logger _log = Logger.getLogger(L2FolkInstance.class.getName());
	private final ClassId[] _classesToTeach;

	public L2FolkInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_classesToTeach = template.getTeachInfo();
	}

	/**
	 * this displays SkillList to the player.
	 * @param player
	 */
	public void showSkillList(L2PcInstance player, ClassId classId)
	{
		if (Config.DEBUG) 
            _log.fine("SkillList activated on: "+getObjectId());
		
        int npcId = getTemplate().npcId;
        
		if (_classesToTeach == null)
        {
			NpcHtmlMessage html = new NpcHtmlMessage(1);
            TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:"+npcId+", Your classId:"+player.getClassId().getId()+"<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
            
			return;
		}
        
		if (!getTemplate().canTeach(classId))
        {
			NpcHtmlMessage html = new NpcHtmlMessage(1);
            TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append("I cannot teach you any skills.<br> You must find your current class teachers.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
            
			return;
		}
        
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		AquireSkillList asl = new AquireSkillList(false);
		int counts = 0;
        
		for (L2SkillLearn s: skills)
		{			
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
            
			if (sk == null || !sk.getCanLearn(player.getClassId()) || !sk.canTeachBy(npcId))
				continue;
            
			int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;
            
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}
        
		if (counts == 0)
		{
		    NpcHtmlMessage html = new NpcHtmlMessage(1);
		    int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);
		    
		    if (minlevel > 0)
		    {
		        SystemMessage sm = new SystemMessage(607);
		        sm.addNumber(minlevel);
		        player.sendPacket(sm);
		    }
		    else
		    {
                TextBuilder sb = new TextBuilder();
		        sb.append("<html><head><body>");
		        sb.append("You've learned all skills for your class.<br>");
		        sb.append("</body></html>");
		        html.setHtml(sb.toString());
		        player.sendPacket(html);
		    }
		} 
		else 
		{
            player.sendPacket(asl);
		}
        
		player.sendPacket(new ActionFailed());
	}
	
	public void showEnchantSkillList(L2PcInstance player, ClassId classId)
	  {
	    if (Config.DEBUG)
	      _log.fine("EnchantSkillList activated on: " + getObjectId());
	    int npcId = getTemplate().npcId;
	
	    if (_classesToTeach == null)
	    {
	      NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	      TextBuilder sb = new TextBuilder();
	      sb.append("<html><head><body>");
	      sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
	      sb.append("</body></html>");
	      html.setHtml(sb.toString());
	      player.sendPacket(html);
	
	      return;
	    }
	
	    if (!getTemplate().canTeach(classId))
	    {
	      NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	      TextBuilder sb = new TextBuilder();
	      sb.append("<html><head><body>");
	      sb.append("I cannot teach you any skills.<br> You must find your current class teachers.");
	      sb.append("</body></html>");
	      html.setHtml(sb.toString());
	      player.sendPacket(html);
	
	      return;
	    }
	    if (player.getClassId().getId() < 88)
	    {
	      NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	      TextBuilder sb = new TextBuilder();
	      sb.append("<html><head><body>");
	      sb.append("You must have 3rd class change quest completed.");
	      sb.append("</body></html>");
	      html.setHtml(sb.toString());
	      player.sendPacket(html);
	
	      return;
	    }
	
	    L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
	    ExEnchantSkillList esl = new ExEnchantSkillList();
	    int counts = 0;
	
	    for (L2EnchantSkillLearn s : skills)
	    {
	      L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
	      if (sk != null) {
	        counts++;
	        esl.addSkill(s.getId(), s.getLevel(), s.getSpCost(), s.getExp());
	      }
	    }
	    if (counts == 0)
	    {
	      player.sendMessage("there are no enchant skills");
	      NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	      int level = player.getLevel();
	
	      if (level < 74)
	      {
	        SystemMessage sm = new SystemMessage(607);
	        sm.addNumber(level);
	        player.sendPacket(sm);
	      }
	      else
	      {
	        TextBuilder sb = new TextBuilder();
	        sb.append("<html><head><body>");
	        sb.append("You've learned all skills for your class.<br>");
	        sb.append("</body></html>");
	        html.setHtml(sb.toString());
	        player.sendPacket(html);
	      }
	    }
	    else
	    {
	      player.sendPacket(esl);
	    }
	
	    player.sendPacket(new ActionFailed());
	  }
	
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("SkillList"))
		{
			if (Config.ALT_GAME_SKILL_LEARN)
			{
				String id = command.substring(9).trim(); 
                
				if (id.length() != 0) 
                {
					player.setSkillLearningClassId(ClassId.values()[Integer.parseInt(id)]);
					showSkillList(player, ClassId.values()[Integer.parseInt(id)]);
				} 
                else 
                {
					boolean own_class = false;
                    
					if (_classesToTeach != null) 
                    {
						for (ClassId cid : _classesToTeach) 
                        {
							if (cid.equalsOrChildOf(player.getClassId())) 
                            {
								own_class = true;
								break;
							}
						}
					}
                    
					String text = 
						"<html>\n"+
						"<body>\n"+
						"<center>Skill learning:</center>\n"+
						"<br>\n";
                    
					if (!own_class) 
                    {
						String mages = player.getClassId().isMage() ? "fighters" : "mages";
						text +=
							"Skills of your class are the easiest to learn.<br>\n"+
							"Skills of another class are harder.<br>\n"+
							"Skills for another race are event more harder to learn.<br>\n"+
							"You can also learn skills of "+mages+", and they are"+
							" the harders to learn!<br>\n"+
							"<br>\n";
					}
                    
					// make a list of classes
					if (_classesToTeach != null) 
                    {
					    for (ClassId cid : _classesToTeach)
					    {
					        if (cid.level() != player.getClassId().level())
					            continue;
                            
					        if (SkillTreeTable.getInstance().getAvailableSkills(player, cid).length == 0)
					            continue;
                            
					        text += "<a action=\"bypass -h npc_%objectId%_SkillList "+cid.getId()+"\">Learn "+cid+"'s class Skills</a><br>\n";
					    }
                    }
                    else
                    {
                        text += "No Skills.<br>\n";
                    }
                    
					text +=
						"</body>\n"+
						"</html>";
                    
					insertObjectIdAndShowChatWindow(player, text);
					player.sendPacket( new ActionFailed() );
				}
			} 
			else if (command.startsWith("EnchantSkillList"))
			{
				showEnchantSkillList(player, player.getClassId());
			}
            else 
            {
				player.setSkillLearningClassId(player.getClassId());
				showSkillList(player, player.getClassId());
			}
		}
		else 
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			
			super.onBypassFeedback(player, command);
		}
	}
}
