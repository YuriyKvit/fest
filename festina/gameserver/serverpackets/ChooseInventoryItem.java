package com.festina.gameserver.serverpackets;

public class ChooseInventoryItem extends ServerBasePacket
{
	private static final String _S__6F_CHOOSEINVENTORYITEM = "[S] 6f ChooseInventoryItem";

	public ChooseInventoryItem()
	{
	}


	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0x6f);
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__6F_CHOOSEINVENTORYITEM;
	}
}
