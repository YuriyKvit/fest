package com.festina.gameserver.serverpackets;

public class ShortBuffStatusUpdate extends ServerBasePacket
{
  private static final String _S__F4_SHORTBUFFSTATUSUPDATE = "[S] fa ShortBuffStatusUpdate";
  private int _skillId;
  private int _skillLvl;
  private int _duration;

  void runImpl()
  {
  }
  public ShortBuffStatusUpdate(int skillId, int skillLvl, int duration)
  {
    _skillId = skillId;
    _skillLvl = skillLvl;
    _duration = duration;
  }

  protected final void writeImpl()
  {
    writeC(0xfa);
    writeD(_skillId);
    writeD(_skillLvl);
    writeD(_duration);
  }

  public String getType()
  {
    return "[S] fa ShortBuffStatusUpdate";
  }

  
}