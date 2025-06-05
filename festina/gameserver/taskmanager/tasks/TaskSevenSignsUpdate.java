package com.festina.gameserver.taskmanager.tasks;

import java.util.logging.Logger;

import com.festina.gameserver.SevenSigns;
import com.festina.gameserver.SevenSignsFestival;
import com.festina.gameserver.taskmanager.Task;
import com.festina.gameserver.taskmanager.TaskManager.ExecutedTask;
import com.festina.status.LoginStatusThread;

/**
 * Updates all data for the Seven Signs and Festival of Darkness engines,
 * when time is elapsed.
 * 
 * @author Tempy
 */
public class TaskSevenSignsUpdate extends Task
{
	private static final Logger _log = Logger.getLogger(LoginStatusThread.class.getName());
    public static final String NAME = "SevenSignsUpdate";
    
    public String getName()
    {
        return NAME;
    }

    public void onTimeElapsed(ExecutedTask task)
    {
        try {
            SevenSigns.getInstance().saveSevenSignsData(null, true);

            if (!SevenSigns.getInstance().isSealValidationPeriod())
                SevenSignsFestival.getInstance().saveFestivalData(false);
            
            _log.info("SevenSigns: Data updated successfully.");
        }
        catch (Exception e) {
        	 _log.info("SevenSigns: Failed to save Seven Signs configuration: " + e);
        }
    }
}
