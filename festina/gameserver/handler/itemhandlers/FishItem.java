package com.festina.gameserver.handler.itemhandlers;

import java.util.List;

import com.festina.gameserver.FishTable;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.lib.Rnd;
import com.festina.gameserver.model.FishDropData;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * @author -Nemesiss-
 *
 */
public class FishItem implements IItemHandler
{
	private static int[] _itemIds = null;
	public FishItem()
	{
		FishTable ft = FishTable.getInstance();
        _itemIds = new int[ft.GetFishItemCount()];
        for (int i = 0; i < ft.GetFishItemCount(); i++)
        {
            _itemIds[i] = ft.getFishIdfromList(i);
        }
    }
	/* (non-Javadoc)
	 * @see com.festina.gameserver.handler.IItemHandler#useItem(com.festina.gameserver.model.L2PcInstance, com.festina.gameserver.model.L2ItemInstance)
	 */
	public synchronized void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
        L2PcInstance activeChar = (L2PcInstance)playable;
		List<FishDropData> rewards = FishTable.getInstance().GetFishRreward(item.getItemId());
		int chance = Rnd.get(100);
		int count = 0;
		takeItems(activeChar, item.getItemId());
		for (FishDropData d: rewards)
		{
			if (chance >= d.getMinChance() && chance <= d.getMaxChance())
			{
				giveItems(activeChar, d.getRewardItemId(), d.getCount());
				count++;
				break;
			}

		}
		if (count == 0)
		{
			//send msg
			activeChar.sendMessage("Fish failed to open!");
		}
	}
	public void giveItems(L2PcInstance activeChar, int itemId, int count)
    {
        activeChar.addItem("FishItem", itemId, count, null, false);
        
        if (count > 1)
        {
            SystemMessage smsg = new SystemMessage(SystemMessage.EARNED_S2_S1_s);
            smsg.addItemName(itemId);
            smsg.addNumber(count);
            activeChar.sendPacket(smsg);
        } else
        {
            SystemMessage smsg = new SystemMessage(SystemMessage.EARNED_ITEM);
            smsg.addItemName(itemId);
            activeChar.sendPacket(smsg);
        }
    }

    public void takeItems(L2PcInstance player, int itemId)
    {
		player.destroyItemByItemId("FishItem", itemId, 1, null, false);
    }
	public int[] getItemIds()
	{
		return _itemIds;
	}
}
