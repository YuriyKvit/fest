/**
 * 
 */
package com.festina.gameserver.taskmanager.tasks;

import com.festina.gameserver.Shutdown;
import com.festina.gameserver.taskmanager.Task;
import com.festina.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Layane
 *
 */
public class TaskShutdown extends Task
{
    public static String NAME = "shutdown";
    
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
        Shutdown handler = new Shutdown(Integer.valueOf(task.getParams()[2]),false);
        handler.start();
    }

}
