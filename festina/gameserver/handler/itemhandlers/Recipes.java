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

import com.festina.gameserver.RecipeController;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2RecipeList;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PlayableInstance;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.2.5.2.5 $ $Date: 2005/04/06 16:13:51 $
 */

public class Recipes implements IItemHandler
{
    private static int[] _itemIds = null;

    public Recipes()
    {
        RecipeController rc = RecipeController.getInstance();
        _itemIds = new int[rc.getRecipesCount()];
        for (int i = 0; i < rc.getRecipesCount(); i++)
        {
            _itemIds[i] = rc.getRecipeList(i).getRecipeId();
        }
    }

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		L2PcInstance activeChar = (L2PcInstance)playable;
        L2RecipeList rp = RecipeController.getInstance().getRecipeByItemId(item.getItemId()); 
     	if (activeChar.hasRecipeList(rp.getId())) 
        {
     		SystemMessage sm = new SystemMessage(SystemMessage.RECIPE_ALREADY_REGISTERED); 
     	 	activeChar.sendPacket(sm); 
        }
        else
        {
        	if (rp.isDwarvenRecipe()) 
        	{
        		if (activeChar.hasDwarvenCraft()) 
        		{ 
			    if (rp.getLevel()>activeChar.getDwarvenCraft())
			    {
				//can't add recipe, becouse create item level too low
        			SystemMessage sm = new SystemMessage(404); 
        			activeChar.sendPacket(sm); 
			    }
			    else if (activeChar.getDwarvenRecipeBook().length >= activeChar.GetDwarfRecipeLimit())
				{
					//Up to $s1 recipes can be registered.
					SystemMessage sm = new SystemMessage(894);
					sm.addNumber(activeChar.GetDwarfRecipeLimit());
        			activeChar.sendPacket(sm);
				}
				else
			    {
        			activeChar.registerDwarvenRecipeList(rp); 
        			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false); 
        			SystemMessage sm = new SystemMessage(614); 
        			sm.addString("Added recipe \"" + rp.getRecipeName() + "\" to Dwarven RecipeBook"); 
        			activeChar.sendPacket(sm); 
			    }
        		} 
        		else 
        		{ 
        			SystemMessage sm = new SystemMessage(SystemMessage.CANT_REGISTER_NO_ABILITY_TO_CRAFT); 
        			activeChar.sendPacket(sm); 
        		} 
        	} 
        	else 
        	{ 
        		if (activeChar.hasCommonCraft()) 
        		{ 
			    if (rp.getLevel()>activeChar.getCommonCraft())
			    {
				//can't add recipe, becouse create item level too low
        			SystemMessage sm = new SystemMessage(404); 
        			activeChar.sendPacket(sm); 
			    }
			    else if (activeChar.getCommonRecipeBook().length >= activeChar.GetCommonRecipeLimit())
				{
					//Up to $s1 recipes can be registered.
					SystemMessage sm = new SystemMessage(894);
					sm.addNumber(activeChar.GetCommonRecipeLimit());
        			activeChar.sendPacket(sm);
				}
				else
			    {
        			activeChar.registerCommonRecipeList(rp); 
        			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false); 
        			SystemMessage sm = new SystemMessage(614); 
        			sm.addString("Added recipe \"" + rp.getRecipeName() + "\" to Common RecipeBook"); 
        			activeChar.sendPacket(sm); 
			    }
        		} 
        		else 
        		{ 
        			SystemMessage sm = new SystemMessage(SystemMessage.CANT_REGISTER_NO_ABILITY_TO_CRAFT); 
        			activeChar.sendPacket(sm); 
        		} 
        	}
        }
    }

    public int[] getItemIds()
    {
        return _itemIds;
    }
}
