package com.festina.gameserver.handler.admincommandhandlers;

import com.festina.Config;
import com.festina.gameserver.DoorTable;
import com.festina.gameserver.handler.IAdminCommandHandler;
import com.festina.gameserver.instancemanager.CastleManager;
import com.festina.gameserver.model.GMAudit;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2DoorInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.entity.Castle;

/**
 * This class handles following admin commands:
 * - open1 = open coloseum door 24190001
 * - open2 = open coloseum door 24190002
 * - open3 = open coloseum door 24190003
 * - open4 = open coloseum door 24190004
 * - openall = open all coloseum door
 * - close1 = close coloseum door 24190001
 * - close2 = close coloseum door 24190002
 * - close3 = close coloseum door 24190003
 * - close4 = close coloseum door 24190004
 * - closeall = close all coloseum door
 * 
 * - open = open selected door
 * - close = close selected door
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminDoorControl implements IAdminCommandHandler
{
    //private static Logger      _log            = Logger.getLogger(AdminDoorControl.class.getName());
    private static final int   REQUIRED_LEVEL  = Config.GM_DOOR;
    private static DoorTable   _doorTable;
    private static String[]    _adminCommands  = 
    {
        "admin_open",
        "admin_close",
        "admin_openall",
        "admin_closeall"
    };
    //private static final Map<String, Integer>   doorMap = new FastMap<String, Integer>(); //FIXME: should we jute remove this?
    
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;
        
		_doorTable = DoorTable.getInstance();
        
		try {
            if (command.startsWith("admin_open "))
            {
		int doorId = Integer.parseInt(command.substring(11));
		if (_doorTable.getDoor(doorId) != null)
            	    _doorTable.getDoor(doorId).openMe();
		else {
		  for (Castle castle: CastleManager.getInstance().getCastles())
		    if (castle.getDoor(doorId) != null){
            		castle.getDoor(doorId).openMe();
		    }
		}
            }
            else if (command.startsWith("admin_close "))
            {
		int doorId = Integer.parseInt(command.substring(12));
		if (_doorTable.getDoor(doorId) != null)
            	    _doorTable.getDoor(doorId).closeMe();
		else {
		  for (Castle castle: CastleManager.getInstance().getCastles())
		    if (castle.getDoor(doorId) != null){
            		castle.getDoor(doorId).closeMe();
		    }
		}
            }
            if (command.equals("admin_closeall"))
            {
                for(L2DoorInstance door : _doorTable.getDoors())
                    door.closeMe();
		for (Castle castle: CastleManager.getInstance().getCastles())
		    for (L2DoorInstance door: castle.getDoors())
            	        door.closeMe();
            }
            if (command.equals("admin_openall"))
            {
                for(L2DoorInstance door : _doorTable.getDoors())
                    door.openMe();
		for (Castle castle: CastleManager.getInstance().getCastles())
		    for (L2DoorInstance door: castle.getDoors())
            		door.openMe();
            }
            if (command.equals("admin_open"))
            {
                L2Object target     = activeChar.getTarget();
                if (target instanceof L2DoorInstance)
                {
                    ((L2DoorInstance)target).openMe();
                }
                else
                {
                    activeChar.sendMessage("Incorrect target.");
                }
            }
            
            if (command.equals("admin_close"))
            {
                L2Object target = activeChar.getTarget();
                if (target instanceof L2DoorInstance)
                {
                    ((L2DoorInstance)target).closeMe();
                }
                else
                {
                    activeChar.sendMessage("Incorrect target.");
                }
            }
        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }
		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");
        return true;
	}

    public String[] getAdminCommandList()
    {
        return _adminCommands;
    }

    private boolean checkLevel(int level)
    {
        return (level >= REQUIRED_LEVEL);
    }
}
	
   