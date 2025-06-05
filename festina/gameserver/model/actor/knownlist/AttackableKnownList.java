package com.festina.gameserver.model.actor.knownlist;

import java.util.Collection;

import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.ai.L2CharacterAI;
import com.festina.gameserver.model.L2Attackable;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.actor.instance.L2FolkInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;

public class AttackableKnownList extends NpcKnownList
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public AttackableKnownList(L2Attackable[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    public boolean removeKnownObject(L2Object object)
    {
        if (!super.removeKnownObject(object)) return false;

        if (getActiveChar().getAggroList() != null)
        {
            // Remove the L2Object from the _aggrolist of the L2Attackable
            synchronized (getActiveChar().getAggroList())
            {
                if (object != null && object instanceof L2Character)
                {
                    getActiveChar().getAggroList().remove(object);
                    if (getActiveChar().getAggroList().isEmpty()) getActiveChar().setAggroList(null);
                }
            }
        }
        
        // Set the L2Attackable Intention to AI_INTENTION_IDLE
        Collection<L2PcInstance> known = getKnownPlayers().values();
        
        //FIXME: This is a temporary solution
        L2CharacterAI ai = getActiveChar().getAI();
        if (ai != null && (known == null || known.isEmpty()))
        {
            ai.setIntention(CtrlIntention.AI_INTENTION_IDLE);
        }

        return true;
    }
    
    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2Attackable getActiveChar() { return (L2Attackable)super.getActiveChar(); }

    public int getDistanceToForgetObject(L2Object object)
    {
        if (getActiveChar().getAggroList() != null)
        {
            synchronized (getActiveChar().getAggroList())
            {
                if (getActiveChar().getAggroList().get(object) != null) return 3000;
            }
        }
        
        return 2 * getDistanceToWatchObject(object);
    }

    public int getDistanceToWatchObject(L2Object object)
    {
        if (object instanceof L2FolkInstance || !(object instanceof L2Character))
            return 0;
        
        if (object instanceof L2PlayableInstance) 
            return 1500;
        
        if (getActiveChar().getAggroRange() > getActiveChar().getFactionRange())
            return getActiveChar().getAggroRange();
        
        if (getActiveChar().getFactionRange() > 200)
            return getActiveChar().getFactionRange();
        
        return 200;
    }
}
