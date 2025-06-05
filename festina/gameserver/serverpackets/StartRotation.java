package com.festina.gameserver.serverpackets;

public class StartRotation extends ServerBasePacket
{
  private static final String _S__62_BEGINROTATION = "[S] 62 BeginRotation";
  private int _charObjId;
  private int _degree;
  private int _side;

  public StartRotation(int objectId, int degree, int side)
  {
    _charObjId = objectId;
    _degree = degree;
    _side = side;
  }

  protected final void writeImpl()
  {
    writeC(0x62);
    writeD(_charObjId);
    writeD(_degree);
    writeD(_side);
  }

  public String getType()
  {
    return "[S] 62 BeginRotation";
  }

  void runImpl()
  {
  }
}