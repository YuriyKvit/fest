/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */

package com.festina.gameserver.handler.itemhandlers;

import java.util.Random;

import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.ItemList;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * @author DrLecter
 * @version 0.1 10/08/05 0039
 * Created for quest items with special needs. Mainly the c3 quests.
 */

public class AdvQuestItems implements IItemHandler
{
    private static int[] _itemIds = { 5944, 5955, 5966, 5967, 5968, 5969, 6007, 6008, 6009, 6010 };
    
    private static int[] award_parch = { 5922, 5923, 5924, 5925, 5926, 5927, 5928, 5929, 5930, 5931, 5932, 5933, 5934, 5935, 5936, 5937, 5938, 5939, 5940, 5941, 5942, 5943};
    private static int[] award_gbook = { 5942, 5943, 5945, 5946, 5947, 5948, 5949, 5950, 5951, 5952, 5953, 5954};
    private static int[] award_papy1 = { 5970, 5971, 5977, 5978, 5979, 5986, 5993, 5994, 5995, 5997, 5983, 6001};
    private static int[] award_papy2 = { 5970, 5971, 5975, 5976, 5980, 5985, 5993, 5994, 5995, 5997, 5983, 6001};
    private static int[] award_papy3 = { 5973, 5974, 5981, 5984, 5989, 5990, 5991, 5992, 5996, 5998, 5999, 6000, 5988, 5983, 6001};
    private static int[] award_papy4 = { 5970, 5971, 5982, 5987, 5989, 5990, 5991, 5992, 5996, 5998, 5999, 6000, 5972, 6001};
    private static int[] GreyPounch = { 6018, 6019, 6012, 6013 };
    
    private static int[] BrownPounch = { 6015, 6017, 6016 };
    
    private static int[] ReagentBox = { 6016, 6014, 6020 };
    
    private final static Random _rnd = new Random();
    
    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
        if (!(playable instanceof L2PcInstance)) return;
        L2PcInstance activeChar = (L2PcInstance)playable;
        int count = 1;
        int itemId = item.getItemId();
        int itemToCreateId = itemId;

        switch (itemId)
        {
        case 5944:
            itemToCreateId = award_parch[ _rnd.nextInt( award_parch.length)];
            break;
        case 5955:
            itemToCreateId = award_gbook[ _rnd.nextInt( award_gbook.length)];
            break;
        case 5966:
            itemToCreateId = award_papy1[ _rnd.nextInt( award_papy1.length)];
            break;
        case 5967:
            itemToCreateId = award_papy2[ _rnd.nextInt( award_papy2.length)];
            break;
        case 5968:
            itemToCreateId = award_papy3[ _rnd.nextInt( award_papy3.length)];
            break;
        case 5969:
            itemToCreateId = award_papy4[ _rnd.nextInt( award_papy4.length)];
            break;
        case 6007:
        	itemToCreateId = GreyPounch[_rnd.nextInt(GreyPounch.length)];
        	if ((itemToCreateId == 6018) || (itemToCreateId == 6019))
        		count = 2;
        	break;
        case 6008:
        	itemToCreateId = BrownPounch[_rnd.nextInt(BrownPounch.length)];
        	if ((itemToCreateId == 6015) || (itemToCreateId == 6017))
        		count = 2;
        	break;
        //case 6009:
        case 6010:
        	 itemToCreateId = ReagentBox[_rnd.nextInt(ReagentBox.length)];
        	 if (itemToCreateId == 6020) 
        		 count = 2;
        	 break;
        }

        activeChar.getInventory().destroyItemByItemId("Extract", item.getItemId(), 1, activeChar, null);
	    activeChar.getInventory().addItem("Extract", itemToCreateId, count, activeChar, item);

        SystemMessage sm = new SystemMessage(SystemMessage.EARNED_S2_S1_s); 
        sm.addItemName(itemToCreateId);
        sm.addNumber(1);
        activeChar.sendPacket(sm);

        ItemList playerUI = new ItemList(activeChar, false);
        activeChar.sendPacket(playerUI);
    }
    public int[] getItemIds()
    {
        return _itemIds;
    }
}