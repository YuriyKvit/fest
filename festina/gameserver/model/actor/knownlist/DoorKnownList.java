package com.festina.gameserver.model.actor.knownlist;

import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2DoorInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2SiegeGuardInstance;

public class DoorKnownList extends CharKnownList
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public DoorKnownList(L2DoorInstance[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    
    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public final L2DoorInstance getActiveChar() { return (L2DoorInstance)super.getActiveChar(); }

    public int getDistanceToForgetObject(L2Object object)
    {
    	if (object instanceof L2SiegeGuardInstance) return 800; 
    	if (!(object instanceof L2PcInstance)) 
            return 0;
        
        return 4000;
    }

    public int getDistanceToWatchObject(L2Object object)
    {
    	if (object instanceof L2SiegeGuardInstance) return 600; 
    	if (!(object instanceof L2PcInstance))
            return 0;
        return 2000;
    }
}
