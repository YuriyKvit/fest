package com.festina.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import com.festina.gameserver.LevelUpData;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.base.Experience;

public class AdminSetLevel implements IAdminCommandHandler {
	private static String[] _adminCommands = {"admin_setlevel"};
	@SuppressWarnings("unused")
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar) {
		L2PcInstance target = (L2PcInstance) activeChar.getTarget();
		if (command.startsWith("admin_setlevel")) 
		{
			if(!(target instanceof L2PcInstance))
			{
				activeChar.sendMessage("This command only for characters");
				return false;
			}
			else if(target.isDead())
			{
				activeChar.sendMessage("You are dead, can't use command");
				return false;
			}
			else if(target == null)
			{
				activeChar.sendMessage("Set target to character for seting level");
				return false;
			}
			else
			{
			String level = command.substring(15);
			adminAddExpSp(target, level);
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return _adminCommands;
	}
	private void adminAddExpSp(L2PcInstance activeChar, String level)
	{
		int lvl = Integer.parseInt(level);
		if(lvl < 1 || lvl > Experience.MAX_LEVEL) 
		{
			activeChar.sendMessage("Wrong level to set");
			return;
		}
		activeChar.setExp(0);
		activeChar.addExpAndSp(Experience.LEVEL[lvl], 0);
		
	}

}
