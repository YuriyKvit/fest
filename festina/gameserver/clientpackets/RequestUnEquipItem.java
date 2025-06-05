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
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.InventoryUpdate;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2Item;

/**
 * This class ...
 * 
 * @version $Revision: 1.8.2.3.2.7 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestUnEquipItem extends ClientBasePacket
{
    private static final String _C__11_REQUESTUNEQUIPITEM = "[C] 11 RequestUnequipItem";
    private static Logger _log = Logger.getLogger(RequestUnEquipItem.class.getName());
    

    // cd
    private final int _slot;

    /**
     * packet type id 0x11 format: cd
     * 
     * @param decrypt
     */
    public RequestUnEquipItem(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _slot = readD();
    }
    private  
    int _objectId;
    void runImpl()
    {
        if (Config.DEBUG) _log.fine("request unequip slot " + _slot);

        L2PcInstance activeChar = getClient().getActiveChar();

        if (activeChar == null) 
        	return;

        if (activeChar.getPrivateStoreType() != 0)
        {
        	activeChar.sendPacket(new SystemMessage(1065));
        	activeChar.sendPacket(new ActionFailed());
        	return;
        }
        if (activeChar.isFakeDeath()) {
        	return;
        }
        // Prevent Stunned player to remove the weapon
        if (activeChar.isStunned() || activeChar.isSleeping())
        {
            activeChar.sendMessage("Your status does not allow you to do that.");
            return;
        }
        L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
                 // Don't allow use weapon/shield when player is stun/sleep 
                 if (activeChar.isStunned() ||  activeChar.isSleeping() 
                         && (item.getItem().getBodyPart() == L2Item.SLOT_LR_HAND  
                             || item.getItem().getBodyPart() == L2Item.SLOT_L_HAND  
                             || item.getItem().getBodyPart() == L2Item.SLOT_R_HAND)) 
                 { 
                     return; 
                 }

        L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(_slot);

        // show the update in the inventory
        InventoryUpdate iu = new InventoryUpdate();

        for (int i = 0; i < unequiped.length; i++)
        {
            if (unequiped[i].isWear()) return;

            iu.addModifiedItem(unequiped[i]);
        }

        activeChar.sendPacket(iu);

        activeChar.abortAttack();

        activeChar.refreshExpertisePenalty();
        activeChar.broadcastUserInfo();

        // this can be 0 if the user pressed the right mousebutton twice very fast
        if (unequiped.length > 0)
        {
            if (unequiped[0].isWear()) return;

            SystemMessage sm = null;

            if (unequiped[0].getEnchantLevel() > 0)
            {
                sm = new SystemMessage(SystemMessage.EQUIPMENT_S1_S2_REMOVED);
                sm.addNumber(unequiped[0].getEnchantLevel());
                sm.addItemName(unequiped[0].getItemId());
            }
            else
            {
                sm = new SystemMessage(SystemMessage.S1_DISARMED);
                sm.addItemName(unequiped[0].getItemId());
            }

            activeChar.sendPacket(sm);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__11_REQUESTUNEQUIPITEM;
    }
}
