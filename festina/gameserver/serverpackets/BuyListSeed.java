package com.festina.gameserver.serverpackets;

import java.util.List;

import javolution.util.FastList;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2TradeList;


public class BuyListSeed extends ServerBasePacket
{
    private static final String _S__E8_BUYLISTSEED = "[S] E8 BuyListSeed";
    
    //private static Logger _log = Logger.getLogger(BuyListSeed.class.getName());
    
    private int _manorId;
    private List<L2ItemInstance> _list = new FastList<L2ItemInstance>();
    private int _money;

    public BuyListSeed(L2TradeList list, int manorId, int currentMoney)
    {
        _money  = currentMoney;
        _manorId = manorId;
        _list   = list.getItems();
    }
    
    final void runImpl()
    {
        // no long-running tasks
    }
    //;BuyListSeedPacket;ddh(h dddhh [dhhh] d)
    final void writeImpl()
    {
        writeC(0xE8);
        
        writeD(_money);                                 // current money
        writeD(_manorId);                               // manor id
        
        writeH(_list.size());                           // list length
        
        for (L2ItemInstance item : _list)
        {
            writeH(0x04);                               // item->type1
            writeD(0x00);                               // objectId
            writeD(item.getItemId());                   // item id
            writeD(item.getCount());                    // item count
            writeH(0x04);                               // item->type2
            writeH(0x00);                               // unknown :)
            writeD(item.getPriceToSell());              // price
        }
    }
    
    public String getType()
    {
        return _S__E8_BUYLISTSEED;
    }
}
