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
package com.festina.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.handler.IItemHandler;
import com.festina.gameserver.handler.ItemHandler;
import com.festina.gameserver.model.Inventory;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.InventoryUpdate;
import com.festina.gameserver.serverpackets.ItemList;
import com.festina.gameserver.serverpackets.ShowCalculator;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2Item;
import com.festina.gameserver.templates.L2Weapon;
import com.festina.gameserver.templates.L2WeaponType;
import com.festina.gameserver.util.FloodProtector;
/**
 * This class ...
 * 
 * @version $Revision: 1.18.2.7.2.9 $ $Date: 2005/03/27 15:29:30 $
 */
public class UseItem extends ClientBasePacket
{
	private static Logger _log = Logger.getLogger(UseItem.class.getName());
	private static final String _C__14_USEITEM = "[C] 14 UseItem";

	private final int _objectId;

	/**
	 * packet type id 0x14
	 * format:		cd
	 * @param decrypt
	 */
	public UseItem(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_objectId = readD();
	}

	void runImpl()
	{

		L2PcInstance activeChar = getClient().getActiveChar();
        
		if (activeChar == null) 
            return;
		if (!FloodProtector.getInstance().tryPerformAction(activeChar.getObjectId(), FloodProtector.PROTECTED_USEITEM))
			return; 
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			activeChar.sendPacket(new ActionFailed());
			return;
		}

		if (activeChar.isFakeDeath()) {
			return;
		}
		
		// NOTE: disabled due to deadlocks
//        synchronized (activeChar.getInventory())
//		{
			L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);

			if (item == null)
                return;

			// Alt game - Karma punishment // SOE
			int itemId = item.getItemId(); 
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT
					&& (itemId == 736 || itemId == 1538 || itemId == 1829 
            || itemId == 1830 || itemId == 3958 || itemId == 5858 || itemId == 5859) 
				&& activeChar.getKarma() > 0) return;

			// Items that cannot be used
			if (itemId == 57)  
                return;
            
			if (activeChar.isFishing() && (itemId < 6535 || itemId > 6540))
            {
                // You cannot do anything else while fishing
                SystemMessage sm = new SystemMessage(1471);
                getClient().getActiveChar().sendPacket(sm);
                return;
            }
            
			// Char cannot use item when dead
			if (activeChar.isDead())
			{
				SystemMessage sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
				sm.addItemName(itemId);
				getClient().getActiveChar().sendPacket(sm);
				return;
			}
            
			// Char cannot use pet items
			if (item.getItem().isForWolf() || item.getItem().isForHatchling()
				|| item.getItem().isForStrider())
			{
				SystemMessage sm = new SystemMessage(600); // You cannot equip a pet item.
				  sm.addItemName(itemId); 
				getClient().getActiveChar().sendPacket(sm);
				return;
			}
            
			if (Config.DEBUG) 
                _log.finest(activeChar.getObjectId() + ": use item " + _objectId);

			if (item.isEquipable())
			{
                // Don't allow weapon/shield equipment if mounted
				int bodyPart = item.getItem().getBodyPart(); 
                if (activeChar.isMounted() 
                	      && (bodyPart == L2Item.SLOT_LR_HAND  
                	                      || bodyPart == L2Item.SLOT_L_HAND  
                	              || bodyPart == L2Item.SLOT_R_HAND)) 
                {
                	return;
                }
             // Don't allow use weapon/shield when player is stun/sleep 
                if (activeChar.isStunned() ||  activeChar.isSleeping() 
                               && (item.getItem().getBodyPart() == L2Item.SLOT_LR_HAND  
            	                            || item.getItem().getBodyPart() == L2Item.SLOT_L_HAND  
           	                            || item.getItem().getBodyPart() == L2Item.SLOT_R_HAND) 
                                      && (bodyPart == L2Item.SLOT_LR_HAND  
                                         || bodyPart == L2Item.SLOT_L_HAND  
                                       || bodyPart == L2Item.SLOT_R_HAND)) 
                          { 
                                  return; 
                          }
                // Don't allow weapon/shield equipment if wearing formal wear
                if (activeChar.isWearingFormalWear()
                		&& (bodyPart == L2Item.SLOT_LR_HAND  
                			                            || bodyPart == L2Item.SLOT_L_HAND  
                			 || bodyPart == L2Item.SLOT_R_HAND))
                {
        				SystemMessage sm = new SystemMessage(SystemMessage.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR);
        				activeChar.sendPacket(sm);
                        return;
                }
                
				L2ItemInstance[] items = activeChar.getInventory().equipItemAndRecord(item);
				activeChar.refreshExpertisePenalty();
                
				if (item.getItem().getType2() == L2Item.TYPE2_WEAPON)
					activeChar.CheckIfWeaponIsAllowed();

				if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessage.S1_S2_EQUIPPED);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(itemId);
					activeChar.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessage.S1_EQUIPPED);
					sm.addItemName(itemId);
					activeChar.sendPacket(sm);
				}

				InventoryUpdate iu = new InventoryUpdate();
				iu.addItems(Arrays.asList(items));
				activeChar.sendPacket(iu);
				activeChar.abortAttack();
				activeChar.broadcastUserInfo();
				
				if (item.getItem().getType2() == 0)
				{
					item.setChargedSoulshot(0);
					item.setChargedSpiritshot(0);
					activeChar.rechargeAutoSoulShot(true, true, false);
				}
			}
			else
			{
                L2Weapon weaponItem = activeChar.getActiveWeaponItem();
                int itemid = item.getItemId();
				//_log.finest("item not equipable id:"+ item.getItemId());
                if (itemid == 4393) 
                {
                        activeChar.sendPacket(new ShowCalculator(4393));
                }
                else if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD)
                    && ((itemid >= 6519 && itemid <= 6527) || (itemid >= 7807 && itemid <= 7809)))
                {
                    activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
                
                    // Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
                    ItemList il = new ItemList(activeChar, false);
                    sendPacket(il);
                    return;
                }
				else
				{
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());
                    
					if (handler == null) 
                        _log.warning("No item handler registered for item ID " + item.getItemId() + ".");
					else 
                        handler.useItem(activeChar, item);
				}
			}
//		}
	}

	public String getType()
	{
		return _C__14_USEITEM;
	}

}
