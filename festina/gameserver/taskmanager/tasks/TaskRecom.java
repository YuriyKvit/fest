/**
 * 
 */
package com.festina.gameserver.taskmanager.tasks;

import java.util.logging.Logger;

import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.UserInfo;
import com.festina.gameserver.taskmanager.Task;
import com.festina.gameserver.taskmanager.TaskManager;
import com.festina.gameserver.taskmanager.TaskTypes;
import com.festina.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Layane
 *
 */
public class TaskRecom extends Task
{
    private static final Logger _log = Logger.getLogger(TaskRecom.class.getName());
    private static final String NAME = "sp_recommendations";
    
    /* (non-Javadoc)
     * @see com.festina.gameserver.taskmanager.Task#getName()
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.taskmanager.Task#onTimeElapsed(com.festina.gameserver.taskmanager.TaskManager.ExecutedTask)
     */
    @Override
    public void onTimeElapsed(ExecutedTask task)
    {
        for (L2PcInstance player: L2World.getInstance().getAllPlayers())
        {
            player.restartRecom();
            player.sendPacket(new UserInfo(player));
        }
        _log.config("Recommendation Global Task: launched.");
    }
    
    public void  initializate()
    {
        super.initializate();
        TaskManager.addUniqueTask(NAME,TaskTypes.TYPE_GLOBAL_TASK,"1","13:00:00","");
    }

}
