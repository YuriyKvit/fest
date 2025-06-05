package com.festina.gameserver.clientpackets;

import java.nio.ByteBuffer;

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.serverpackets.SystemMessage;

public class RequestDeleteMacro extends ClientBasePacket
{  
	private int _id;
	
	private static final String _C__C2_REQUESTDELETEMACRO = "[C] C2 RequestDeleteMacro";
	
	/**
	 * packet type id 0xc2
	 * 
	 * sample
	 * 
	 * c2
	 * d // macro id
	 * 
	 * format:		cd
	 * @param decrypt
	 */
	public RequestDeleteMacro(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_id = readD();
		//System.out.println("Delete macro id="+_id);
	}
	
	void runImpl()
	{
		if (getClient().getActiveChar() == null)
		    return;
		getClient().getActiveChar().deleteMacro(_id);
	    SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
	    sm.addString("Delete macro id="+_id);
		sendPacket(sm);
	}
	
	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__C2_REQUESTDELETEMACRO;
	}

}
