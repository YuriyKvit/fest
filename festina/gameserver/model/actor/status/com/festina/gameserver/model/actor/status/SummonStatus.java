package com.festina.gameserver.model.actor.status;

import com.festina.gameserver.model.L2Summon;

public class SummonStatus extends PlayableStatus
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public SummonStatus(L2Summon[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2Summon getActiveChar() { return (L2Summon)super.getActiveChar(); }
}
