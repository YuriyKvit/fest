package com.festina.gameserver.handler.skillhandlers;

import com.festina.gameserver.handler.ISkillHandler;
import com.festina.gameserver.model.Inventory;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Skill;
import com.festina.gameserver.model.L2Skill.SkillType;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.InventoryUpdate;
import com.festina.gameserver.serverpackets.ItemList;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.templates.L2Weapon;
import com.festina.gameserver.templates.L2WeaponType;

public class Fishing implements ISkillHandler 
{ 
    //private static Logger _log = Logger.getLogger(SiegeFlag.class.getName()); 
	//protected SkillType[] _skillIds = {SkillType.FISHING};
	protected SkillType[] _skillIds = {SkillType.FISHING}; 
    
    public void useSkill(L2Character activeChar, @SuppressWarnings("unused") L2Skill skill, @SuppressWarnings("unused") L2Object[] targets)
    {
        if (activeChar == null || !(activeChar instanceof L2PcInstance)) return;

        L2PcInstance player = (L2PcInstance)activeChar;
		
		if (player.isFishing())
		{
			if (player.GetFish() !=null) player.GetFish().DoDie(false);
			else player.EndFishing(false);
			//Cancels fishing			
			player.sendPacket(new SystemMessage(1458));
			return;
		}		
        //if ()
		//{			
			//1456	You can't fish while you are on board			
			//return;
		//}
		if (activeChar.getZ() >= -3700)
		{
            //You can't fish here
			player.sendPacket(new SystemMessage(1457));
			if (!player.isGM())
			return;
		}
		if (activeChar.getZ() <= -3800)
		{
            //You can't fish in water
			player.sendPacket(new SystemMessage(1455));
			if (!player.isGM())
			return;
		}
		L2Weapon weaponItem = player.getActiveWeaponItem();
		if ((weaponItem==null || weaponItem.getItemType() != L2WeaponType.ROD))
		{
			//Fishing poles are not installed
			player.sendPacket(new SystemMessage(1453));
			return;
		}		
		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (lure == null)
		{
		    //Not enough bait
		    player.sendPacket(new SystemMessage(1459));
            return;
		}
		//if (!Config.ALLOWFISHING && !player.isGM())
		//{
		//	player.sendMessage("Not Working Yet");
		//	return;
		//}		
		player.SetLure(lure);
		L2ItemInstance lure2 = player.getInventory().destroyItem("Consume", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, player, null);

		if (lure2 == null || lure2.getCount() == 0)
		{
			player.sendPacket(new ItemList(player,false));
		}
		else
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(lure2);
			player.sendPacket(iu);
		}
		player.StartFishing();		
		
		
        
        
    } 
    
    public SkillType[] getSkillIds() 
    { 
        return _skillIds; 
    } 
    
}
