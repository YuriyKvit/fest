package com.festina.gameserver.serverpackets;

import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Zombie_Killer
 * Пакет на окошки в олимпе.
 */
public class ExOlympiadUserInfoSpectator extends ServerBasePacket
{
	// chcdSddddd
	private static final String _S__FE_29_OLYMPIADUSERINFOSPECTATOR = "[S] FE:29 OlympiadUserInfoSpectator";
	private static int _side;
	private static L2PcInstance _player;
	

	/**
	 * @param _player
	 * @param _side (1 = right, 2 = left)
	 */
	public ExOlympiadUserInfoSpectator(L2PcInstance player, int side)
	{
		_player = player;
		_side = side;
	}
	

	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x29);
		writeC(_side);
		writeD(_player.getObjectId());
		writeS(_player.getName());
		writeD(_player.getClassId().getId());
		writeD((int)_player.getCurrentHp());
		writeD(_player.getMaxHp());
		writeD((int)_player.getCurrentCp());
		writeD(_player.getMaxCp());
	}
	
	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__FE_29_OLYMPIADUSERINFOSPECTATOR;
	}


	@Override
	void runImpl() {
		// null
		
	}
}