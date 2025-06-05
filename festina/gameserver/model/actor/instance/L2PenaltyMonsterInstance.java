package com.festina.gameserver.model.actor.instance;

import java.util.Random;

import com.festina.gameserver.SpawnTable;
import com.festina.gameserver.ai.CtrlEvent;
import com.festina.gameserver.clientpackets.Say2;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2Spawn;
import com.festina.gameserver.serverpackets.CreatureSay;
import com.festina.gameserver.templates.L2NpcTemplate;

public class L2PenaltyMonsterInstance extends L2MonsterInstance
{
	private L2PcInstance _ptk;
	private final static Random _rnd = new Random();

	public L2PenaltyMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	public L2Character getMostHated()
	{
		return _ptk; //zawsze attakuje tylko 1 osobe chodzby nie wiem co xD
	}
	public void NotifyPlayerDead()
	{
		// Monster kill player and can by deleted
		deleteMe();

		L2Spawn spawn = getSpawn();
		if (spawn != null)
		{
			spawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(spawn, false);
		}
	}

	public void SetPlayerToKill(L2PcInstance ptk)
	{
		if (_rnd.nextInt(100) <= 80)
		{
			CreatureSay cs = new CreatureSay(this.getObjectId(), Say2.ALL, this.getName(),
												"mmm your bait was delicious");
			this.broadcastPacket(cs);
		}
		_ptk = ptk;
		addDamageHate(ptk, 10, 10);
		getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, ptk);
		addAttackerToAttackByList(ptk);
	}

	public void doDie(L2Character killer)
	{
		if (_rnd.nextInt(100) <= 75)
		{
			CreatureSay cs = new CreatureSay(this.getObjectId(), Say2.ALL, this.getName(),
												"I will tell fishes not to take your bait");
			this.broadcastPacket(cs);
		}
		super.doDie(killer);
	}

}
