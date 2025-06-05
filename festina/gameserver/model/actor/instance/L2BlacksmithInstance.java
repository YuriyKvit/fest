/**
 * 
 */
package com.festina.gameserver.model.actor.instance;

import com.festina.gameserver.serverpackets.MultiSellList;
import com.festina.gameserver.templates.L2NpcTemplate;

/**
 * @author zabbix
 * Lets drink to code!
 */
public class L2BlacksmithInstance extends L2FolkInstance
{

	public L2BlacksmithInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	public void onAction(L2PcInstance player)
	{
		player.setLastFolkNPC(this);
		super.onAction(player);
	}
	
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("multisell"))
		{
			String listId = command.substring(9).trim();
			player.sendPacket(new MultiSellList(Integer.parseInt(listId)));
		}
		
		super.onBypassFeedback(player,command);
	}
	
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		} 
		else 
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/blacksmith/" + pom + ".htm";
	}
}
