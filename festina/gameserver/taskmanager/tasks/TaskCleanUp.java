/**
 * 
 */
package com.festina.gameserver.taskmanager.tasks;

import com.festina.gameserver.taskmanager.Task;
import com.festina.gameserver.taskmanager.TaskManager;
import com.festina.gameserver.taskmanager.TaskTypes;
import com.festina.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Tempy
 *
 */
public final class TaskCleanUp extends Task
{
    public static String NAME = "CleanUp";
    
    public String getName()
    {
        return NAME;
    }

    public void onTimeElapsed(ExecutedTask task)
    {
        System.runFinalization();
        System.gc();
    }
    
    public void initializate()
    {
        super.initializate();
        TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "0", "1800000", "");
    }
}
