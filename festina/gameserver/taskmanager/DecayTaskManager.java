/**
 * 
 */
package com.festina.gameserver.taskmanager;

import java.util.Map;
import java.util.NoSuchElementException;

import javolution.util.FastMap;
import com.festina.gameserver.ThreadPoolManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.actor.instance.L2RaidBossInstance;

/**
 * @author la2
 * Lets drink to code!
 */
public class DecayTaskManager
{
    protected Map<L2Character,Long> _decayTasks = new FastMap<L2Character,Long>().setShared(true);

    public static DecayTaskManager _instance;
    
    public DecayTaskManager()
    {
    	ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new DecayScheduler(),10000,5000);
    }
    
    public static DecayTaskManager getInstance()
    {
        if(_instance == null)
            _instance = new DecayTaskManager();
        
        return _instance;
    }
    
    public void addDecayTask(L2Character actor)
    {
        _decayTasks.put(actor,System.currentTimeMillis());
    }

    public void addDecayTask(L2Character actor, int interval)
    {
        _decayTasks.put(actor,System.currentTimeMillis()+interval);
    }
    
    public void cancelDecayTask(L2Character actor)
    {
    	try
    	{
    		_decayTasks.remove(actor);
    	}
    	catch(NoSuchElementException e){}
    }
    
    private class DecayScheduler implements Runnable
    {
    	protected DecayScheduler()
    	{
    		// Do nothing
    	}
    	
        public void run()
        {
            Long current = System.currentTimeMillis();
            int delay;
            if (_decayTasks != null)
                for(L2Character actor : _decayTasks.keySet())
                {
                    if(actor instanceof L2RaidBossInstance) delay = 30000;
                    else delay = 8500;
                    if((current - _decayTasks.get(actor)) > delay)
                    {
                        actor.onDecay();
                        _decayTasks.remove(actor);
                    }
                }
        }
    }

    public String toString()
    {
        String ret = "============= DecayTask Manager Report ============\r\n";
        ret += "Tasks count: "+_decayTasks.size()+"\r\n";
        ret += "Tasks dump:\r\n";
        
        Long current = System.currentTimeMillis();
        for( L2Character actor : _decayTasks.keySet())
        {
            ret += "Class/Name: "+actor.getClass().getSimpleName()+"/"+actor.getName()
            +" decay timer: "+(current - _decayTasks.get(actor))+"\r\n";
        }
        
        return ret;
    }
}
