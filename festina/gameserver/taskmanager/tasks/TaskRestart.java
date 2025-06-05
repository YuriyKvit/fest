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
public final class TaskRestart extends Task
{
    public static String NAME = "restart";

    /* (non-Javadoc)
     * @see com.festina.gameserver.tasks.Task#getName()
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    /* (non-Javadoc)
     * @see com.festina.gameserver.tasks.Task#onTimeElapsed(com.festina.gameserver.tasks.TaskManager.ExecutedTask)
     */
    @Override
    public void onTimeElapsed(ExecutedTask task)
    {
        Shutdown handler = new Shutdown(Integer.valueOf(task.getParams()[2]),true);
        handler.start();
    }

}
