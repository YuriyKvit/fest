package com.festina.gameserver.model.actor.knownlist;

import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2CabaleBufferInstance;
import com.festina.gameserver.model.actor.instance.L2FestivalGuideInstance;
import com.festina.gameserver.model.actor.instance.L2FolkInstance;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;

public class NpcKnownList extends CharKnownList
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public NpcKnownList(L2NpcInstance[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    
    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2NpcInstance getActiveChar() { return (L2NpcInstance)super.getActiveChar(); }

    public int getDistanceToForgetObject(L2Object object) { return 2 * getDistanceToWatchObject(object); }

    public int getDistanceToWatchObject(L2Object object)
    {
        if (object instanceof L2FestivalGuideInstance)
            return 10000;
        
        if (object instanceof L2FolkInstance || !(object instanceof L2Character))
            return 0;
        
        if (object instanceof L2CabaleBufferInstance)
            return 900;
        
        if (object instanceof L2PlayableInstance) 
            return 1500;
        
        return 500;
    }
}
