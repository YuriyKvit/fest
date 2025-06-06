package com.festina.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.festina.gameserver.ClanTable;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.L2Clan;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;

public class RequestStopPledgeWar extends ClientBasePacket
{
	private static final String _C__4F_REQUESTSTOPPLEDGEWAR = "[C] 4F RequestStopPledgeWar";
	private static Logger _log = Logger.getLogger(RequestStopPledgeWar.class.getName());

	String _pledgeName;

	public RequestStopPledgeWar(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_pledgeName = readS();
	}

	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null) return;
		L2Clan playerClan = player.getClan();
		if (playerClan == null) return;

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

		if (clan == null)
		{
			player.sendMessage("No such clan.");
			player.sendPacket(new ActionFailed());
			return;
		}

		if (!playerClan.isAtWarWith(clan.getClanId()))
		{
			player.sendMessage("You aren't at war with this clan.");
			player.sendPacket(new ActionFailed());
			return;
		}

		_log.info("RequestStopPledgeWar: By leader: " + playerClan.getLeaderName() + " of clan: "
			+ playerClan.getName() + " to clan: " + _pledgeName);

		//        L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());
		//        if(leader != null && leader.isOnline() == 0)
		//        {
		//            player.sendMessage("Clan leader isn't online.");
		//            player.sendPacket(new ActionFailed());
		//            return;                        
		//        }

		//        if (leader.isProcessingRequest())
		//        {
		//            SystemMessage sm = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
		//            sm.addString(leader.getName());
		//            player.sendPacket(sm);
		//            return;
		//        } 

		ClanTable.getInstance().deleteclanswars(playerClan.getClanId(), clan.getClanId());
		//        player.onTransactionRequest(leader);
		//        leader.sendPacket(new StopPledgeWar(_clan.getName(),player.getName()));
	}

	public String getType()
	{
		return _C__4F_REQUESTSTOPPLEDGEWAR;
	}
}