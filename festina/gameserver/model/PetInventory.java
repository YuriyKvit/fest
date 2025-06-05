package com.festina.gameserver.model;

import com.festina.gameserver.model.L2ItemInstance.ItemLocation;
import com.festina.gameserver.model.actor.instance.L2PetInstance;

public class PetInventory extends Inventory 
{
	private final L2PetInstance _owner;

	public PetInventory(L2PetInstance owner) 
    {
		_owner = owner;
	}
    
	public L2PetInstance getOwner() 
    { 
        return _owner; 
    }
    
	protected ItemLocation getBaseLocation() 
    {
        return ItemLocation.PET; 
    }
    
	protected ItemLocation getEquipLocation() 
    { 
        return ItemLocation.PET_EQUIP; 
    }
}
