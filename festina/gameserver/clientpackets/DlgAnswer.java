package com.festina.gameserver.clientpackets;

import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import java.nio.ByteBuffer;

public class DlgAnswer extends ClientBasePacket
{
  int message;
  int answer;

  public DlgAnswer(ByteBuffer buf, ClientThread client)
  {
    super(buf, client);
    message = readD();
    answer = readD();
  }

  void runImpl()
  {
    getClient().getActiveChar().onDialogAnswer(message, answer);
  }

  public String getType()
  {
    return null;
  }
}
