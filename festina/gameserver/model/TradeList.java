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
package com.festina.gameserver.model;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.festina.Config;
import com.festina.gameserver.ItemTable;
import com.festina.gameserver.BasePacket.ItemRequest;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.InventoryUpdate;
import com.festina.gameserver.serverpackets.ItemList;
import com.festina.gameserver.serverpackets.StatusUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2Item;

/**
 * @author Advi
 *
 */
public class TradeList
{
    public class TradeItem
    {
        int _objectId;
        L2Item _item;
        int _enchant;
        int _count;
        int _price;

        public TradeItem(L2ItemInstance item, int count, int price)
        {
            _objectId = item.getObjectId();
            _item = item.getItem();
            _enchant = item.getEnchantLevel();
            _count = count;
            _price = price;
        }

        public TradeItem(L2Item item, int count, int price)
        {
            _objectId = 0;
            _item = item;
            _enchant = 0;
            _count = count;
            _price = price;
        }

        public TradeItem(TradeItem item, int count, int price)
        {
            _objectId = item.getObjectId();
            _item = item.getItem();
            _enchant = item.getEnchant();
            _count = count;
            _price = price;
        }

        public void setObjectId(int objectId)
        {
            _objectId = objectId;
        }

        public int getObjectId()
        {
            return _objectId;
        }

        public L2Item getItem()
        {
            return _item;
        }

        public void setEnchant(int enchant)
        {
            _enchant = enchant;
        }

        public int getEnchant()
        {
            return _enchant;
        }

        public void setCount(int count)
        {
            _count = count;
        }

        public int getCount()
        {
            return _count;
        }

        public void setPrice(int price)
        {
            _price = price;
        }

        public int getPrice()
        {
            return _price;
        }
    }

    private static Logger _log = Logger.getLogger(TradeList.class.getName());

    private L2PcInstance _owner;
    private L2PcInstance _partner;
    private List<TradeItem> _items;
    private String _title;

    private boolean _confirmed = false;
    private boolean _locked = false;

    public TradeList(L2PcInstance owner)
    {
        _items = new FastList<TradeItem>();
        _owner = owner;
    }

    public L2PcInstance getOwner()
    {
        return _owner;
    }

    public void setPartner(L2PcInstance partner)
    {
        _partner = partner;
    }

    public L2PcInstance getPartner()
    {
        return _partner;
    }

    public void setTitle(String title)
    {
        _title = title;
    }

    public String getTitle()
    {
        return _title;
    }

    public boolean isLocked()
    {
        return _locked;
    }

    public boolean isConfirmed()
    {
        return _confirmed;
    }

    /**
     * Retrieves items from TradeList
     */
    public TradeItem[] getItems()
    {
        return _items.toArray(new TradeItem[_items.size()]);
    }

    /**
     * Returns the list of items in inventory available for transaction
     * @return L2ItemInstance : items in inventory
     */
    public TradeList.TradeItem[] getAvailableItems(PcInventory inventory)
    {
        List<TradeList.TradeItem> list = new FastList<TradeList.TradeItem>();
        for (TradeList.TradeItem item : _items)
        {
            item = new TradeItem(item, item.getCount(), item.getPrice());
            inventory.adjustAvailableItem(item);
            list.add(item);
        }

        return list.toArray(new TradeList.TradeItem[list.size()]);
    }

    /**
     * Returns Item List size
     */
    public int getItemCount()
    {
        return _items.size();
    }

    /**
     * Adjust available item from Inventory by the one in this list
     * @param item : L2ItemInstance to be adjusted
     * @return TradeItem representing adjusted item 
     */
    public TradeItem adjustAvailableItem(L2ItemInstance item)
    {
        if (item.isStackable())
            for (TradeItem exclItem : _items)
                if (exclItem.getItem().getItemId() == item.getItemId())
                    if (item.getCount() <= exclItem.getCount()) return null;
                    else return new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
        return new TradeItem(item, item.getCount(), item.getReferencePrice());
    }

    /**
     * Adjust ItemRequest by corresponding item in this list using its <b>ObjectId</b>
     * @param item : ItemRequest to be adjusted
     */
    public void adjustItemRequest(ItemRequest item)
    {
        for (TradeItem filtItem : _items)
            if (filtItem.getObjectId() == item.getObjectId())
            {
                if (filtItem.getCount() < item.getCount()) item.setCount(filtItem.getCount());
                return;
            }
        item.setCount(0);
    }

    /**
     * Adjust ItemRequest by corresponding item in this list using its <b>ItemId</b>
     * @param item : ItemRequest to be adjusted
     */
    public void adjustItemRequestByItemId(ItemRequest item)
    {
        for (TradeItem filtItem : _items)
            if (filtItem.getItem().getItemId() == item.getItemId())
            {
                if (filtItem.getCount() < item.getCount()) item.setCount(filtItem.getCount());
                return;
            }
        item.setCount(0);
    }

    /**
     * Add simplified item to TradeList
     * @param objectId : int
     * @param count : int
     * @return
     */
    public synchronized TradeItem addItem(int objectId, int count)
    {
        return addItem(objectId, count, 0);
    }

    /**
     * Add item to TradeList
     * @param objectId : int
     * @param count : int
     * @param price : int
     * @return
     */
    public synchronized TradeItem addItem(int objectId, int count, int price)
    {
        if (isLocked())
        {
            _log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
            return null;
        }
        L2Object item = L2World.getInstance().findObject(objectId);
        
        if (item == null || !(item instanceof L2ItemInstance))
        {
            _log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
            return null;
        }
        
        int itemId = ((L2ItemInstance)item).getItemId();
        
        if ((itemId >= 6611 && itemId <= 6621) || itemId == 6842)
            return null;
        if (count > ((L2ItemInstance)item).getCount()) return null;
        if (!((L2ItemInstance) item).isStackable() && count > 1)
        {
            _log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
            return null;
        }
        for (TradeItem checkitem : _items)
        {
            if (checkitem.getObjectId() == objectId) return null;
        }
        TradeItem titem = new TradeItem((L2ItemInstance) item, count, price);
        _items.add(titem);

        // If Player has already confirmed this trade, invalidate the confirmation
        InvalidateConfirmation();
        return titem;
    }

    /**
     * Add item to TradeList
     * @param objectId : int
     * @param count : int
     * @param price : int
     * @return
     */
    public synchronized TradeItem addItemByItemId(int itemId, int count, int price)
    {
        if (isLocked())
        {
            _log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
            return null;
        }

        L2Item item = ItemTable.getInstance().getTemplate(itemId);
        if (item == null)
        {
            _log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
            return null;
        }
        
        if ((itemId >= 6611 && itemId <= 6621) || itemId == 6842)
            return null;

        if (!item.isStackable() && count > 1)
        {
            _log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
            return null;
        }

        TradeItem titem = new TradeItem(item, count, price);
        _items.add(titem);

        // If Player has already confirmed this trade, invalidate the confirmation
        InvalidateConfirmation();
        return titem;
    }

    /**
     * Remove item from TradeList
     * @param objectId : int
     * @param count : int
     * @return
     */
    public synchronized TradeItem removeItem(int objectId, int itemId, int count)
    {
        if (isLocked())
        {
            _log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
            return null;
        }

        for (TradeItem titem : _items)
            if (titem.getObjectId() == objectId || titem.getItem().getItemId() == itemId)
            {
                // If Partner has already confirmed this trade, invalidate the confirmation
                if (_partner != null)
                {
                    TradeList partnerList = _partner.getActiveTradeList();
                    if (partnerList == null)
                    {
                        _log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
                        return null;
                    }
                    partnerList.InvalidateConfirmation();
                }

                // Reduce item count or complete item
                if (count != -1 && titem.getCount() > count) titem.setCount(titem.getCount() - count);
                else _items.remove(titem);

                return titem;
            }
        return null;
    }

    /**
     * Update items in TradeList according their quantity in owner inventory
     */
    public synchronized void updateItems()
    {
        for (TradeItem titem : _items)
        {
            L2ItemInstance item = _owner.getInventory().getItemByObjectId(titem.getObjectId());
            if (item == null || titem.getCount() < 1) removeItem(titem.getObjectId(), -1, -1);
            else if (item.getCount() < titem.getCount()) titem.setCount(item.getCount());
        }
    }

    /**
     * Lockes TradeList, no further changes are allowed
     */
    public void Lock()
    {
        _locked = true;
    }

    /**
     * Clears item list
     */
    public void Clear()
    {
        _items.clear();
        _locked = false;
    }

    /**
     * Confirms TradeList
     * @return : boolean
     */
    public synchronized boolean Confirm()
    {
        if (_confirmed) return true; // Already confirmed

        // If Partner has already confirmed this trade, proceed exchange
        if (_partner != null)
        {
            TradeList partnerList = _partner.getActiveTradeList();
            if (partnerList == null)
            {
                _log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
                return false;
            }
           // synchronized (partnerList)
            //{
            //    _confirmed = true;
                if (partnerList.isConfirmed())
                {
                    partnerList.Lock();
                    this.Lock();
                    if (!partnerList.Validate()) return false;
                    if (!this.Validate()) return false;

                    doExchange(partnerList);
                }
                else 
                { 
                	_confirmed = true; 
                	_partner.onTradeConfirm(_owner); 
                } 
                //} 
        }
        else _confirmed = true;

        return _confirmed;
    }

    /**
     * Cancels TradeList confirmation
     */
    public void InvalidateConfirmation()
    {
        _confirmed = false;
    }

    /**
     * Validates TradeList with owner inventory
     */
    private boolean Validate()
    {
        // Check for Owner validity
        if (_owner == null || L2World.getInstance().findObject(_owner.getObjectId()) == null)
        {
            _log.warning("Invalid owner of TradeList");
            return false;
        }

        // Check for Item validity
        for (TradeItem titem : _items)
        {
            L2ItemInstance item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");
            if (item == null || titem.getCount() < 1)
            {
                _log.warning(_owner.getName() + ": Invalid Item in TradeList");
                return false;
            }
        }

        return true;
    }

    /**
     * Transfers all TradeItems from inventory to partner
     */
    private boolean TransferItems(L2PcInstance partner, InventoryUpdate ownerIU,
                                  InventoryUpdate partnerIU)
    {
        for (TradeItem titem : _items)
        {
            L2ItemInstance oldItem = _owner.getInventory().getItemByObjectId(titem.getObjectId());
            if (oldItem == null) return false;
            L2ItemInstance newItem = _owner.getInventory().transferItem("Trade", titem.getObjectId(), titem.getCount(), partner.getInventory(), _owner, _partner);
            if (newItem == null) return false;

            // Add changes to inventory update packets
            if (ownerIU != null)
            {
                if (oldItem.getCount() > 0 && oldItem != newItem) ownerIU.addModifiedItem(oldItem);
                else ownerIU.addRemovedItem(oldItem);
            }

            if (partnerIU != null)
            {
                if (newItem.getCount() > titem.getCount()) partnerIU.addModifiedItem(newItem);
                else partnerIU.addNewItem(newItem);
            }
        }
        return true;
    }

    /**                                                                                                   
     * Count items slots                                                                                  
     */

    public int countItemsSlots(L2PcInstance partner)
    {
        int slots = 0;

        for (TradeItem item : _items)
        {
            if (item == null) continue;
            L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
            if (template == null) continue;
            if (!template.isStackable()) slots += item.getCount();
            else if (partner.getInventory().getItemByItemId(item.getItem().getItemId()) == null)
                slots++;
        }

        return slots;
    }

    /**                                                                                                   
     * Calc weight of items in tradeList                                                                  
     */

    public int calcItemsWeight()
    {
        int weight = 0;

        for (TradeItem item : _items)
        {
            if (item == null) continue;
            L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
            if (template == null) continue;
            weight += item.getCount() * template.getWeight();
        }

        return weight;
    }

    /**
     * Proceeds with trade
     */
    private void doExchange(TradeList partnerList)
    {
        boolean success = false;
        // check weight and slots
        if ((!this.getOwner().getInventory().validateWeight(partnerList.calcItemsWeight()))
            || !(partnerList.getOwner().getInventory().validateWeight(this.calcItemsWeight())))
        {
            partnerList.getOwner().sendPacket(new SystemMessage(SystemMessage.WEIGHT_LIMIT_EXCEEDED));
            this.getOwner().sendPacket(new SystemMessage(SystemMessage.WEIGHT_LIMIT_EXCEEDED));
        }
        else if ((!this.getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(this.getOwner())))
            || (!partnerList.getOwner().getInventory().validateCapacity(this.countItemsSlots(partnerList.getOwner()))))
        {
            partnerList.getOwner().sendPacket(new SystemMessage(SystemMessage.SLOTS_FULL));
            this.getOwner().sendPacket(new SystemMessage(SystemMessage.SLOTS_FULL));
        }
        else
        {
            // Prepare inventory update packet
            InventoryUpdate ownerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
            InventoryUpdate partnerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();

            // Transfer items
            partnerList.TransferItems(this.getOwner(), partnerIU, ownerIU);
            this.TransferItems(partnerList.getOwner(), ownerIU, partnerIU);

            // Send inventory update packet
            if (ownerIU != null) _owner.sendPacket(ownerIU);
            else _owner.sendPacket(new ItemList(_owner, false));

            if (partnerIU != null) _partner.sendPacket(partnerIU);
            else _partner.sendPacket(new ItemList(_partner, false));

            // Update current load as well
            StatusUpdate playerSU = new StatusUpdate(_owner.getObjectId());
            playerSU.addAttribute(StatusUpdate.CUR_LOAD, _owner.getCurrentLoad());
            _owner.sendPacket(playerSU);
            playerSU = new StatusUpdate(_partner.getObjectId());
            playerSU.addAttribute(StatusUpdate.CUR_LOAD, _partner.getCurrentLoad());
            _partner.sendPacket(playerSU);
            
            success = true;
        }
        // Finish the trade
        partnerList.getOwner().onTradeFinish(success);
        this.getOwner().onTradeFinish(success);
    }

    /**
     * Buy items from this PrivateStore list
     * @return : boolean true if success
     */
    public synchronized boolean PrivateStoreBuy(L2PcInstance player, ItemRequest[] items, int price)
    {
        if (_locked) return false;
        if (!Validate())
        {
            Lock();
            return false;
        }

        int slots = 0;
        int weight = 0;

        for (ItemRequest item : items)
        {
            if (item == null) continue;
            L2Item template = ItemTable.getInstance().getTemplate(item.getItemId());
            if (template == null) continue;
            weight += item.getCount() * template.getWeight();
            if (!template.isStackable()) slots += item.getCount();
            else if (player.getInventory().getItemByItemId(item.getItemId()) == null) slots++;
        }

        if (!player.getInventory().validateWeight(weight))
        {
            player.sendPacket(new SystemMessage(SystemMessage.WEIGHT_LIMIT_EXCEEDED));
            return false;
        }

        if (!player.getInventory().validateCapacity(slots))
        {
            player.sendPacket(new SystemMessage(SystemMessage.SLOTS_FULL));
            return false;
        }

        PcInventory ownerInventory = _owner.getInventory();
        PcInventory playerInventory = player.getInventory();

        // Prepare inventory update packets
        InventoryUpdate ownerIU = new InventoryUpdate();
        InventoryUpdate playerIU = new InventoryUpdate();

        // Transfer adena
        if (price > playerInventory.getAdena())
        {
            Lock();
            return false;
        }
        ;
        L2ItemInstance adenaItem = playerInventory.getAdenaInstance();
        playerInventory.reduceAdena("PrivateStore", price, player, _owner);
        playerIU.addItem(adenaItem);
        ownerInventory.addAdena("PrivateStore", price, _owner, player);
        ownerIU.addItem(ownerInventory.getAdenaInstance());

        // Transfer items
        for (ItemRequest item : items)
        {
            //Check if requested item is sill on the list and adjust its count 
            adjustItemRequest(item);
            if (item.getCount() == 0) continue;

            // Check if requested item is available for manipulation
            L2ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
            if (oldItem == null)
            {
                Lock();
                return false;
            }
            ;

            // Proceed with item transfer
            L2ItemInstance newItem = ownerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), playerInventory, _owner, player);
            if (newItem == null) return false;
            removeItem(item.getObjectId(), -1, item.getCount());

            // Add changes to inventory update packets
            if (oldItem.getCount() > 0 && oldItem != newItem) ownerIU.addModifiedItem(oldItem);
            else ownerIU.addRemovedItem(oldItem);
            if (newItem.getCount() > item.getCount()) playerIU.addModifiedItem(newItem);
            else playerIU.addNewItem(newItem);

            // Send messages about the transaction to both players 
            if (newItem.isStackable())
            {
                SystemMessage msg = new SystemMessage(SystemMessage.S1_PURCHASED_S3_S2_s);
                msg.addString(player.getName());
                msg.addItemName(newItem.getItemId());
                msg.addNumber(item.getCount());
                _owner.sendPacket(msg);

                msg = new SystemMessage(SystemMessage.PURCHASED_S3_S2_s_FROM_S1);
                msg.addString(_owner.getName());
                msg.addItemName(newItem.getItemId());
                msg.addNumber(item.getCount());
                player.sendPacket(msg);
            }
            else
            {
                SystemMessage msg = new SystemMessage(SystemMessage.S1_PURCHASED_S2);
                msg.addString(player.getName());
                msg.addItemName(newItem.getItemId());
                _owner.sendPacket(msg);

                msg = new SystemMessage(SystemMessage.PURCHASED_S2_FROM_S1);
                msg.addString(_owner.getName());
                msg.addItemName(newItem.getItemId());
                player.sendPacket(msg);
            }
        }

        // Send inventory update packet
        _owner.sendPacket(ownerIU);
        player.sendPacket(playerIU);
        return true;
    }

    /**
     * Sell items to this PrivateStore list
     * @return : boolean true if success
     */
    public synchronized boolean PrivateStoreSell(L2PcInstance player, ItemRequest[] items, int price)
    {
        if (_locked) return false;

        PcInventory ownerInventory = _owner.getInventory();
        PcInventory playerInventory = player.getInventory();
        
        //we must check item are available before begining transaction, TODO: should we remove that check when transfering items as it's done here? (there might be synchro problems if player clicks fast if we remove it)
        for (ItemRequest item : items)
        {
            // Check if requested item is available for manipulation 
            L2ItemInstance oldItem = player.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
            if (oldItem == null)
                return false;
        }

        // Prepare inventory update packet
        InventoryUpdate ownerIU = new InventoryUpdate();
        InventoryUpdate playerIU = new InventoryUpdate();

        // Transfer items
        for (ItemRequest item : items)
        {
            // Check if requested item is sill on the list and adjust its count 
            adjustItemRequestByItemId(item);
            if (item.getCount() == 0) continue;

            // Check if requested item is available for manipulation 
            L2ItemInstance oldItem = player.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
            if (oldItem == null) return false;

            // Proceed with item transfer
            L2ItemInstance newItem = playerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), ownerInventory, player, _owner);
            if (newItem == null) return false;
            removeItem(-1, item.getItemId(), item.getCount());

            // Add changes to inventory update packets
            if (oldItem.getCount() > 0 && oldItem != newItem) playerIU.addModifiedItem(oldItem);
            else playerIU.addRemovedItem(oldItem);
            if (newItem.getCount() > item.getCount()) ownerIU.addModifiedItem(newItem);
            else ownerIU.addNewItem(newItem);

            // Send messages about the transaction to both players 
            if (newItem.isStackable())
            {
                SystemMessage msg = new SystemMessage(SystemMessage.PURCHASED_S3_S2_s_FROM_S1);
                msg.addString(player.getName());
                msg.addItemName(newItem.getItemId());
                msg.addNumber(item.getCount());
                _owner.sendPacket(msg);

                msg = new SystemMessage(SystemMessage.S1_PURCHASED_S3_S2_s);
                msg.addString(_owner.getName());
                msg.addItemName(newItem.getItemId());
                msg.addNumber(item.getCount());
                player.sendPacket(msg);
            }
            else
            {
                SystemMessage msg = new SystemMessage(SystemMessage.PURCHASED_S2_FROM_S1);
                msg.addString(player.getName());
                msg.addItemName(newItem.getItemId());
                _owner.sendPacket(msg);

                msg = new SystemMessage(SystemMessage.S1_PURCHASED_S2);
                msg.addString(_owner.getName());
                msg.addItemName(newItem.getItemId());
                player.sendPacket(msg);
            }
        }
        
        // Transfer adena
        if (price > ownerInventory.getAdena()) return false;
        L2ItemInstance adenaItem = ownerInventory.getAdenaInstance();
        ownerInventory.reduceAdena("PrivateStore", price, _owner, player);
        ownerIU.addItem(adenaItem);
        playerInventory.addAdena("PrivateStore", price, player, _owner);
        playerIU.addItem(playerInventory.getAdenaInstance());

        // Send inventory update packet
        _owner.sendPacket(ownerIU);
        player.sendPacket(playerIU);
        return true;
    }

	/**
	 * @param objectId
	 * @return
	 */
	public TradeItem getItem(int objectId)
	{
		for(TradeItem item : _items)
		{
			if(item.getObjectId() == objectId)
				return item;
		}
		return null;
	}
}
