package com.festina.gameserver.handler.itemhandlers;

import com.festina.gameserver.NpcTable;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.idfactory.IdFactory;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Spawn;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2NpcTemplate;

public class ChristmasTree implements IItemHandler
{
    private static int[] _itemIds = {
                                     5560, // x-mas tree
                                     5561  // Special x-mas tree
                                     };

    private static int[] _npcIds = {
                                    12619, //Christmas tree w. flashing lights and snow
                                    12620
                                    };

    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
        L2PcInstance activeChar = (L2PcInstance)playable;
        L2NpcTemplate template1 = null;

        int itemId = item.getItemId();
        for (int i = 0; i < _itemIds.length; i++)
        {
            if (_itemIds[i] == itemId)
            {
                template1 = NpcTable.getInstance().getTemplate(_npcIds[i]);
                break;
            }
        }
        
        if (template1 == null)
            return;

        L2Object target = activeChar.getTarget();
        if (target == null)
            target = activeChar;

        try
        {
            L2Spawn spawn = new L2Spawn(template1);
            spawn.setId(IdFactory.getInstance().getNextId());
            spawn.setLocx(target.getX());
            spawn.setLocy(target.getY());
            spawn.setLocz(target.getZ());
            L2World.getInstance().storeObject(spawn.spawnOne());

            activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);

            SystemMessage sm = new SystemMessage(614);
            sm.addString("Created " + template1.name + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
            activeChar.sendPacket(sm);
        }
        catch (Exception e)
        {
            SystemMessage sm = new SystemMessage(614);
            sm.addString("Target is not ingame.");
            activeChar.sendPacket(sm);
        }
    }

    public int[] getItemIds()
    {
        return _itemIds;
    }
}
