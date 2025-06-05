/**
 * 
 */
package com.festina.gameserver.taskmanager.tasks;

import com.festina.gameserver.taskmanager.Task;
import com.festina.gameserver.taskmanager.TaskManager.ExecutedTask;

import org.python.util.PythonInterpreter;

/**
 * @author Layane
 *
 */
public class TaskJython extends Task
{
    public static final String NAME = "jython";
    
    private final PythonInterpreter _python = new PythonInterpreter();
    
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
        _python.cleanup();
        _python.exec("import sys");
        _python.execfile("data/scripts/cron/" + task.getParams()[2]);
    }

}
