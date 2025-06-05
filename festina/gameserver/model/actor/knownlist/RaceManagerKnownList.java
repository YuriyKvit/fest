package com.festina.gameserver.model.actor.knownlist;

import com.festina.gameserver.MonsterRace;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2RaceManagerInstance;
import com.festina.gameserver.serverpackets.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public RaceManagerKnownList(L2RaceManagerInstance[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    public boolean addKnownObject(L2Object object) { return addKnownObject(object, null); }
    public boolean addKnownObject(L2Object object, L2Character dropper)
    {
        if (!super.addKnownObject(object, dropper)) return false;

        /* DONT KNOW WHY WE NEED THIS WHEN RACE MANAGER HAS A METHOD THAT BROADCAST TO ITS KNOW PLAYERS
        if (object instanceof L2PcInstance) {
            if (packet != null)
                ((L2PcInstance) object).sendPacket(packet);
        }
        */

        return true;
    }

    public boolean removeKnownObject(L2Object object)
    {
        if (!super.removeKnownObject(object)) return false;

        if (object instanceof L2PcInstance)
        {
            //System.out.println("Sending delete monsrac info.");
            DeleteObject obj = null;
            for (int i=0; i<8; i++)
            {
                obj = new DeleteObject(MonsterRace.getInstance().getMonsters()[i]);
                ((L2PcInstance)object).sendPacket(obj);
            }
        }

        return true;
    }
    
    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2RaceManagerInstance getActiveChar() { return (L2RaceManagerInstance)super.getActiveChar(); }
}
