/**
 * 
 */
package com.festina.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.festina.gameserver.ClientThread;

/**
 * @author zabbix
 * Lets drink to code!
 */
public class DummyPacket extends ClientBasePacket
{
	private static Logger _log = Logger.getLogger(DummyPacket.class.getName());
	
	private int _packetId;
	
	public DummyPacket(ByteBuffer buf, ClientThread client, int packetId)
	{
		super(buf,client);
		_packetId = packetId;
	}

	public void runImpl()
	{
		_log.warning("DummyPacket " + _packetId + " (Length = " + getLength() + ") recieved.");
		//getClient().getConnection().close();
	}

	public String getType()
	{
		return "DummyPacket";
	}
}
