package com.festina.gameserver.model.actor.knownlist;


import com.festina.Config;
import com.festina.gameserver.model.L2Character;
import com.festina.gameserver.model.L2ItemInstance;
import com.festina.gameserver.model.L2Object;
import com.festina.gameserver.model.L2Summon;
import com.festina.gameserver.model.actor.instance.L2BoatInstance;
import com.festina.gameserver.model.actor.instance.L2DoorInstance;
import com.festina.gameserver.model.actor.instance.L2NpcInstance;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.actor.instance.L2PetInstance;
import com.festina.gameserver.model.actor.instance.L2StaticObjectInstance;
import com.festina.gameserver.serverpackets.CharInfo;
import com.festina.gameserver.serverpackets.DeleteObject;
import com.festina.gameserver.serverpackets.DoorInfo;
import com.festina.gameserver.serverpackets.DoorStatusUpdate;
import com.festina.gameserver.serverpackets.DropItem;
import com.festina.gameserver.serverpackets.GetOnVehicle;
import com.festina.gameserver.serverpackets.NpcInfo;
import com.festina.gameserver.serverpackets.PetInfo;
import com.festina.gameserver.serverpackets.PetItemList;
import com.festina.gameserver.serverpackets.PrivateStoreMsgSell;
import com.festina.gameserver.serverpackets.SpawnItem;
import com.festina.gameserver.serverpackets.SpawnItemPoly;
import com.festina.gameserver.serverpackets.StaticObject;
import com.festina.gameserver.serverpackets.VehicleInfo;

public class PcKnownList extends PlayableKnownList
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public PcKnownList(L2PcInstance[] activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    /**
     * Add a visible L2Object to L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packets needed to inform the L2PcInstance of its state and actions in progress.<BR><BR>
     *
     * <B><U> object is a L2ItemInstance </U> :</B><BR><BR>
     * <li> Send Server-Client Packet DropItem/SpawnItem to the L2PcInstance </li><BR><BR>
     *
     * <B><U> object is a L2DoorInstance </U> :</B><BR><BR>
     * <li> Send Server-Client Packets DoorInfo and DoorStatusUpdate to the L2PcInstance </li>
     * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
     *
     * <B><U> object is a L2NpcInstance </U> :</B><BR><BR>
     * <li> Send Server-Client Packet NpcInfo to the L2PcInstance </li>
     * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
     *
     * <B><U> object is a L2Summon </U> :</B><BR><BR>
     * <li> Send Server-Client Packet NpcInfo/PetItemList (if the L2PcInstance is the owner) to the L2PcInstance </li>
     * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
     *
     * <B><U> object is a L2PcInstance </U> :</B><BR><BR>
     * <li> Send Server-Client Packet CharInfo to the L2PcInstance </li>
     * <li> If the object has a private store, Send Server-Client Packet PrivateStoreMsgSell to the L2PcInstance </li>
     * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
     *
     * @param object The L2Object to add to _knownObjects and _knownPlayer
     * @param dropper The L2Character who dropped the L2Object
     */
    public boolean addKnownObject(L2Object object) { return addKnownObject(object, null); }
    public boolean addKnownObject(L2Object object, L2Character dropper)
    {
        if (!super.addKnownObject(object, dropper)) return false;

        if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
        {
            //if (object.getPolytype().equals("item"))
                getActiveChar().sendPacket(new SpawnItemPoly(object));
            //else if (object.getPolytype().equals("npc"))
            //    sendPacket(new NpcInfoPoly(object, this));

        }
        else
        {
            if (object instanceof L2ItemInstance)
            {
                if (dropper != null)
                    getActiveChar().sendPacket(new DropItem((L2ItemInstance) object, dropper.getObjectId()));
                else
                    getActiveChar().sendPacket(new SpawnItem((L2ItemInstance) object));
            }
            else if (object instanceof L2DoorInstance)
            {
                getActiveChar().sendPacket(new DoorInfo((L2DoorInstance) object));
                getActiveChar().sendPacket(new DoorStatusUpdate((L2DoorInstance) object));
            }
            else if (object instanceof L2BoatInstance)
            {
            	if(!getActiveChar().isInBoat())
            	if(object != getActiveChar().getBoat())
            	{
            		getActiveChar().sendPacket(new VehicleInfo((L2BoatInstance) object));
            		((L2BoatInstance) object).sendVehicleDeparture(getActiveChar());
            	}
            }
            else if (object instanceof L2StaticObjectInstance)
            {
                getActiveChar().sendPacket(new StaticObject((L2StaticObjectInstance) object));
            }
            else if (object instanceof L2NpcInstance)
            {
                if (Config.CHECK_KNOWN) getActiveChar().sendMessage("Added NPC: "+((L2NpcInstance) object).getName());
                getActiveChar().sendPacket(new NpcInfo((L2NpcInstance) object, getActiveChar()));
            }
            else if (object instanceof L2Summon)
            {
                L2Summon summon = (L2Summon) object;

                // Check if the L2PcInstance is the owner of the Pet
                if (this.equals(summon.getOwner()))
                {
                    getActiveChar().sendPacket(new PetInfo(summon));
                    if (summon instanceof L2PetInstance)
                    {
                        getActiveChar().sendPacket(new PetItemList((L2PetInstance) summon));
                    }
                }
                else
                    getActiveChar().sendPacket(new NpcInfo(summon, getActiveChar()));
            }
            else if (object instanceof L2PcInstance)
            {
                L2PcInstance otherPlayer = (L2PcInstance) object;
                if(otherPlayer.isInBoat())
                {
                	otherPlayer.getPosition().setWorldPosition(otherPlayer.getBoat().getPosition().getWorldPosition());
                	getActiveChar().sendPacket(new CharInfo(otherPlayer));
                	getActiveChar().sendPacket(new GetOnVehicle(otherPlayer, otherPlayer.getBoat(), otherPlayer.getInBoatPosition().getX(), otherPlayer.getInBoatPosition().getY(), otherPlayer.getInBoatPosition().getZ()));
                	/*if(otherPlayer.getBoat().GetVehicleDeparture() == null)
                	{                	
                		
                		int xboat = otherPlayer.getBoat().getX();
                		int yboat= otherPlayer.getBoat().getY();
                		double modifier = Math.PI/2;
                		if (yboat == 0)
                		{
                			yboat = 1;
                		}
                		if(yboat < 0)
                		{
                			modifier = -modifier;
                		}                		
                		double angleboat = modifier - Math.atan(xboat/yboat);
                		int xp = otherPlayer.getX();
                		int yp = otherPlayer.getY();
                		modifier = Math.PI/2;
                		if (yp == 0)
                		{
                			yboat = 1;
                		}
                		if(yboat < 0)
                		{
                			modifier = -modifier;
                		}                		
                		double anglep = modifier - Math.atan(yp/xp);
                		
                		double finx = Math.cos(anglep - angleboat)*Math.sqrt(xp *xp +yp*yp ) + Math.cos(angleboat)*Math.sqrt(xboat *xboat +yboat*yboat );
                		double finy = Math.sin(anglep - angleboat)*Math.sqrt(xp *xp +yp*yp ) + Math.sin(angleboat)*Math.sqrt(xboat *xboat +yboat*yboat );
                		//otherPlayer.getPosition().setWorldPosition(otherPlayer.getBoat().getX() - otherPlayer.getInBoatPosition().x,otherPlayer.getBoat().getY() - otherPlayer.getInBoatPosition().y,otherPlayer.getBoat().getZ()- otherPlayer.getInBoatPosition().z);
                		otherPlayer.getPosition().setWorldPosition((int)finx,(int)finy,otherPlayer.getBoat().getZ()- otherPlayer.getInBoatPosition().z);
                		
                	}*/
                }
                else
                {
                	getActiveChar().sendPacket(new CharInfo(otherPlayer));
                }

                if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY)
                	getActiveChar().sendPacket(new PrivateStoreMsgSell(otherPlayer));
                else if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL)
                	getActiveChar().sendPacket(new PrivateStoreMsgSell(otherPlayer));
// TODO: corrrect msg                else if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE)
//                	getActiveChar().sendPacket(new PrivateStoreMsgSell(otherPlayer));
            }

            if (object instanceof L2Character)
            {
                // Update the state of the L2Character object client side by sending Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance
                L2Character obj = (L2Character) object;
                obj.getAI().describeStateToPlayer(getActiveChar());
            }
        }

        return true;
    }

    /**
     * Remove a L2Object from L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packet DeleteObject to the L2PcInstance.<BR><BR>
     *
     * @param object The L2Object to remove from _knownObjects and _knownPlayer
     *
     */
    public boolean removeKnownObject(L2Object object)
    {
            if (!super.removeKnownObject(object)) return false;
        // Send Server-Client Packet DeleteObject to the L2PcInstance
        getActiveChar().sendPacket(new DeleteObject(object));       	       
       if (Config.CHECK_KNOWN && object instanceof L2NpcInstance) getActiveChar().sendMessage("Removed NPC: "+((L2NpcInstance)object).getName());
        return true;
    }
    
    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public final L2PcInstance getActiveChar() { return (L2PcInstance)super.getActiveChar(); }

    public int getDistanceToForgetObject(L2Object object) { return 4500; }

    public int getDistanceToWatchObject(L2Object object) { return 3500; }
}
