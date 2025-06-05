package com.festina.gameserver.instancemanager;


public class Manager
{
	public static void loadAll()
	{
		ArenaManager.getInstance();
		AuctionManager.getInstance();
		BoatManager.getInstance();
		CastleManager.getInstance();
		ClanHallManager.getInstance();
		MercTicketManager.getInstance();
		//PartyCommandManager.getInstance();
		PetitionManager.getInstance();
		QuestManager.getInstance();
		SiegeManager.getInstance();
		TownManager.getInstance();
		//ZoneManager.getInstance();
        OlympiadStadiaManager.getInstance();
	}

	public static void reloadAll()
	{
		AuctionManager.getInstance().reload();
		QuestManager.getInstance().reload();
	}
}