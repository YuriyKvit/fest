package com.festina.gameserver.clientpackets;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
//import com.festina.protection.CatsGuard;

/**
 * @author zabbix
 * Lets drink to code!
 * 
 * Unknown Packet:ca
 * 0000: 45 00 01 00 1e 37 a2 f5 00 00 00 00 00 00 00 00    E....7..........
 */

public class GameGuardReply extends ClientBasePacket
{
    private static final String _C__CA_GAMEGUARDREPLY = "[C] CA GameGuardReply";
    private int[] _reply = new int[4];
    @SuppressWarnings("unused")
    
    private byte[] _data;
    public GameGuardReply(ByteBuffer buf, ClientThread client)
    {
        super(buf,client);
    }

    void readImpl()
    {
		/*if(CatsGuard.getInstance().isEnabled() && getClient().getHWid() == null) 
		{
			_reply[0] = readD();
			_reply[1] = readD();
			_reply[2] = readD();
			_reply[3] = readD();
		} 
		else 
		{
			byte[] b = new byte[getByteBuffer().remaining()];
			readB(b);
		}*/
    }

	protected void runImpl()
    {
  		/*if(CatsGuard.getInstance().isEnabled()) 
  			CatsGuard.getInstance().initSession(getClient(), _reply);*/
    }
    
    public String getType()
    {
        return _C__CA_GAMEGUARDREPLY;
    }

}
