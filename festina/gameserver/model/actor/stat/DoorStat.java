package com.festina.gameserver.model.actor.stat;

import com.festina.gameserver.model.actor.instance.L2DoorInstance;

public class DoorStat extends CharStat
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public DoorStat(L2DoorInstance[] activeChar)
    {
        super(activeChar);

        setLevel(1);
    }

    // =========================================================
    // Method - Public

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2DoorInstance getActiveChar() { return (L2DoorInstance)super.getActiveChar(); }

    public final int getLevel() { return 1; }
}
