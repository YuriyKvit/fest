package com.festina.gameserver.taskmanager.tasks;

import java.util.logging.Logger;

import com.festina.gameserver.Olympiad;
import com.festina.gameserver.taskmanager.Task;
import com.festina.gameserver.taskmanager.TaskManager;
import com.festina.gameserver.taskmanager.TaskTypes;
import com.festina.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * Updates all data of Olympiad nobles in db
 * 
 * @author godson
 */
public class TaskOlympiadSave extends Task
{
    private static final Logger _log = Logger.getLogger(TaskOlympiadSave.class.getName());
    public static final String NAME = "OlympiadSave";
    
    public String getName()
    {
        return NAME;
    }

    public void onTimeElapsed(ExecutedTask task)
    {
        try {
            Olympiad.getInstance().save();
            _log.info("Olympiad System: Data updated successfully.");
        }
        catch (Exception e) {
            _log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
        }
    }
    
    public void initializate()
    {
        super.initializate();
        TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "0", "1800000", "");
    }
}
