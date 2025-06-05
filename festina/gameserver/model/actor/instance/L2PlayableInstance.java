package com.festina.gameserver.model.actor.instance;

import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Effect;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Summon;
import com.festina.gameserver.model.actor.knownlist.PlayableKnownList;
import com.festina.gameserver.model.actor.stat.PlayableStat;
import com.festina.gameserver.model.actor.status.PlayableStatus;
import com.festina.gameserver.templates.L2CharTemplate;

/**
 * This class represents all Playable characters in the world.<BR><BR>
 * 
 * L2PlayableInstance :<BR><BR>
 * <li>L2PcInstance</li>
 * <li>L2Summon</li><BR><BR>
 * 
 */

public abstract class L2PlayableInstance extends L2Character 
{
	private boolean _IsNoblesseBlessed = false; // for Noblesse Blessing skill, restores buffs after death
	/**
	 * Constructor of L2PlayableInstance (use L2Character constructor).<BR><BR>
	 *  
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2PlayableInstance </li><BR><BR>
	 * 
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the L2PlayableInstance
	 * 
	 */
	public L2PlayableInstance(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
        super.setKnownList(new PlayableKnownList(new L2PlayableInstance[] {this}));
        super.setStat(new PlayableStat(new L2PlayableInstance[] {this}));
        super.setStatus(new PlayableStatus(new L2PlayableInstance[] {this}));
	}
    
    public PlayableKnownList getKnownList() { return (PlayableKnownList)super.getKnownList(); }
    public PlayableStat getStat() { return (PlayableStat)super.getStat(); }
    public PlayableStatus getStatus() { return (PlayableStatus)super.getStatus(); }

    public void doDie(L2Character killer)
    {
        if (killer != null)
        {
            L2PcInstance player = null;
            if (killer instanceof L2PcInstance)
                player = (L2PcInstance)killer;
            else if (killer instanceof L2Summon)
                player = ((L2Summon)killer).getOwner();

            if (player != null) player.onKillUpdatePvPKarma(this);
        }

        super.doDie(killer);
    }

    public boolean checkIfPvP(L2Character target)
    {
        if (target == null) return false;                                               // Target is null
        if (target == this) return false;                                               // Target is self
        if (!(target instanceof L2PlayableInstance)) return false;                      // Target is not a L2PlayableInstance

        L2PcInstance player = null;
        if (this instanceof L2PcInstance)
            player = (L2PcInstance)this;
        else if (this instanceof L2Summon)
            player = ((L2Summon)this).getOwner();

        if (player == null) return false;                                               // Active player is null
        if (player.getKarma() != 0) return false;                                       // Active player has karma

        L2PcInstance targetPlayer = null;
        if (target instanceof L2PcInstance)
            targetPlayer = (L2PcInstance)target;
        else if (target instanceof L2Summon)
            targetPlayer = ((L2Summon)target).getOwner();

        if (targetPlayer == null) return false;                                         // Target player is null
        if (targetPlayer == this) return false;                                         // Target player is self
        if (targetPlayer.getKarma() != 0) return false;                                 // Target player has karma

        return true;
        /*  Even at war, there should be PvP flag
        if(
                player.getClan() == null ||
                targetPlayer.getClan() == null ||
                (
                        !targetPlayer.getClan().isAtWarWith(player.getClanId()) &&
                        targetPlayer.getWantsPeace() == 0 &&
                        player.getWantsPeace() == 0
                )
            )
        {
            return true;
        }

        return false;
        */
    }
    
    /**
	 * Return True.<BR><BR>
	 */
    public boolean isAttackable()
    {
        return true;
    }
	// Поддержка нубл эффекта
    public final boolean isNoblesseBlessed() { return _IsNoblesseBlessed; } 
	public final void setIsNoblesseBlessed(boolean value) { _IsNoblesseBlessed = value; } 

	public final void startNoblesseBlessing() 
	{ 
	setIsNoblesseBlessed(true); 
	updateAbnormalEffect(); 
	} 

	public final void stopNoblesseBlessing(L2Effect effect) 
	{ 
	if (effect == null) 
		stopEffects(L2Effect.EffectType.NOBLESSE_BLESSING); 
	else 
		removeEffect(effect); 
                    
	setIsNoblesseBlessed(false); 
	updateAbnormalEffect(); 
	}
	public abstract boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage);
	public abstract boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage);
}
