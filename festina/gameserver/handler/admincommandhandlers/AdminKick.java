package com.festina.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import com.festina.Config;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.model.GMAudit;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.LeaveWorld;
import com.festina.gameserver.serverpackets.SystemMessage;

public class AdminKick implements IAdminCommandHandler {
    //private static Logger _log = Logger.getLogger(AdminKick.class.getName());
    private static String[] _adminCommands = {"admin_kick" ,"admin_kick_non_gm"};
    private static final int REQUIRED_LEVEL = Config.GM_KICK;
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {

        if (!Config.ALT_PRIVILEGES_ADMIN)
        {
    		if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
            {
                //System.out.println("Not required level");
                return false;
            }
        }
		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");
        
        if (command.startsWith("admin_kick"))
        {
            //System.out.println("ADMIN KICK");
            StringTokenizer st = new StringTokenizer(command);
            //System.out.println("Tokens: "+st.countTokens());
            if (st.countTokens() > 1)
            {
                st.nextToken();
                String player = st.nextToken();
                //System.out.println("Player1 "+player);
                L2PcInstance plyr = L2World.getInstance().getPlayer(player);
                if (plyr != null)
                {
                    //System.out.println("Player2 "+plyr.getName());
                    plyr.logout();
                }
				SystemMessage sm = new SystemMessage(614);
				sm.addString("You kicked " + plyr.getName() + " from the game.");
				activeChar.sendPacket(sm);
            }
        }
        if (command.startsWith("admin_kick_non_gm"))
        {
        	int counter = 0;
        	for (L2PcInstance player : L2World.getInstance().getAllPlayers())
            {
        		if(!player.isGM())
        		{
        			counter++;
        			player.sendPacket(new LeaveWorld());
        			player.logout();
        		}
            }
        	activeChar.sendMessage("Kicked "+counter+" players");
        }
        return true;
    }

    public String[] getAdminCommandList() {
        return _adminCommands;
    }
    
    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }
}
