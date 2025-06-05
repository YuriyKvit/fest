package com.festina.gameserver.handler.admincommandhandlers;

import com.festina.gameserver.datatables.NobleTable;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

public class AdminSetNoble implements IAdminCommandHandler {
	private static String[] _adminCommands = {"admin_setnoble"};
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar) {
		if (command.startsWith("admin_setnoble")) 
		{
			L2PcInstance target = (L2PcInstance) activeChar.getTarget();
			if (target == null)
			{
				activeChar.sendMessage("Set target to character to getting noblesse");
				return false;
			}
			else if (!(target instanceof L2PcInstance))
			{
				activeChar.sendMessage("This command only for characters");
				return false;
			}
			else if (NobleTable.isNoble(target.getObjectId()))
			{
				activeChar.sendMessage("Already noblessed");
				return false;
			}
			else
			{
				NobleTable.setnoble(target.getObjectId());
				NobleTable.getNobleSkill(target);
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return _adminCommands;
	}
}
