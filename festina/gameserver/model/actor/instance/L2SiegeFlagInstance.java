package com.festina.gameserver.model.actor.instance;

import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.instancemanager.SiegeManager;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2SiegeClan;
import com.festina.gameserver.model.entity.Siege;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.MyTargetSelected;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.ValidateLocation;
import com.festina.gameserver.templates.L2NpcTemplate;

public class L2SiegeFlagInstance extends L2NpcInstance
{
    private L2PcInstance _Player;
    private Siege _Siege;
    
	public L2SiegeFlagInstance(L2PcInstance player, int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

        _Player = player;
        _Siege = SiegeManager.getInstance().getSiege(_Player.getX(), _Player.getY(), _Player.getZ());
        if (_Player.getClan() == null || _Siege == null)
        {
            this.deleteMe();
        }
        else
        {
            L2SiegeClan sc = _Siege.getAttackerClan(_Player.getClan());
            if (sc == null)
                this.deleteMe();
            else
                sc.addFlag(this);
        }
	}

    public boolean isAttackable()
    {
        // Attackable during siege by attacker only
        return (getCastle() != null
                && getCastle().getCastleId() > 0
                && getCastle().getSiege().getIsInProgress());
    }

	public boolean isAutoAttackable(L2Character attacker) 
	{
		// Attackable during siege by attacker only
		return (attacker != null 
		        && attacker instanceof L2PcInstance 
		        && getCastle() != null
		        && getCastle().getCastleId() > 0
		        && getCastle().getSiege().getIsInProgress());
	}
	
    public void doDie(L2Character killer)
    {
        L2SiegeClan sc = _Siege.getAttackerClan(_Player.getClan());
        if (sc != null)
        	sc.removeFlag(this);
        
        super.doDie(killer);
    }

    public void onForcedAttack(L2PcInstance player)
	{
		onAction(player);
	}
	
	public void onAction(L2PcInstance player)
	{
        if (player == null)
            return;
        
		if (this != player.getTarget())
		{
			player.setTarget(this);
			
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
            StatusUpdate su = new StatusUpdate(getObjectId());
            su.addAttribute(StatusUpdate.CUR_HP, (int)getCurrentHp() );
            su.addAttribute(StatusUpdate.MAX_HP, getMaxHp() );
            player.sendPacket(su);
			
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			if (
                    isAutoAttackable(player) &&                 // Object is attackable
                    Math.abs(player.getZ() - getZ()) < 100      // Less then max height difference
			    )
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
            else 
                player.sendPacket(new ActionFailed());
		}
	}
}
