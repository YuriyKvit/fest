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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.festina.Config;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.TaskPriority;
import com.festina.gameserver.ai.CtrlIntention;
import com.festina.gameserver.model.L2CharPosition;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ActionFailed;
import com.festina.gameserver.serverpackets.PartyMemberPosition;
import com.festina.gameserver.templates.L2WeaponType;
//import com.festina.gameserver.serverpackets.AttackCanceld;
/**
 * This class ...
 * 
 * @version $Revision: 1.11.2.4.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class MoveBackwardToLocation extends ClientBasePacket
{
	private static Logger _log = Logger.getLogger(MoveBackwardToLocation.class.getName());
	// cdddddd
	private final int _targetX;
	private final int _targetY;
	private final int _targetZ;
	@SuppressWarnings("unused")
    private final int _originX;
	@SuppressWarnings("unused")
    private final int _originY;
	@SuppressWarnings("unused")
    private final int _originZ;
	private       int _moveMovement; 
	
	public TaskPriority getPriority() { return TaskPriority.PR_HIGH; }

	private static final String _C__01_MOVEBACKWARDTOLOC = "[C] 01 MoveBackwardToLoc";
	/**
	 * packet type id 0x01
	 * @param decrypt
	 */
	public MoveBackwardToLocation(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_targetX  = readD();
		_targetY  = readD();
		_targetZ  = readD();
		_originX  = readD();
		_originY  = readD();
		_originZ  = readD();
		if (Config.allowL2Walker(client.getActiveChar()) && client.getRevision() == Config.L2WALKER_REVISION) {
            _moveMovement = 1;
		} else {
			try {
				_moveMovement = readD(); // is 0 if cursor keys are used  1 if mouse is used
			} catch (BufferUnderflowException e) {
				_log.warning("Incompatible client found: L2Walker? "+client.getActiveChar());
			    if (Config.AUTOBAN_L2WALKER_ACC){
				L2PcInstance activeChar = getClient().getActiveChar();
				if(activeChar == null)
					return;
				activeChar.setAccessLevel(-100);
				activeChar.store();
				activeChar.deleteMe();
			    }
			}
		}
	}

	
	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if(activeChar.isInBoat())
		{
			activeChar.setInBoat(false);
		}
		if (activeChar.getTeleMode() > 0)
		{
			if (activeChar.getTeleMode() == 1)
				activeChar.setTeleMode(0);
			activeChar.sendPacket(new ActionFailed());
			activeChar.teleToLocation(_targetX, _targetY, _targetZ);
			return;
		}
		
		if (_moveMovement == 0 && Config.GEODATA < 1) // cursor movement without geodata is disabled
		{
			activeChar.sendPacket(new ActionFailed());
		}
		else if (activeChar.isAttackingNow() && activeChar.getActiveWeaponItem() != null && (activeChar.getActiveWeaponItem().getItemType() == L2WeaponType.BOW))
		{
			activeChar.sendPacket(new ActionFailed());
		}
		else 
		{
			double dx = _targetX-activeChar.getX();
			double dy = _targetY-activeChar.getY();
			// Can't move if character is confused, or trying to move a huge distance
			if (activeChar.isOutOfControl()||((dx*dx+dy*dy) > 98010000)) { // 9900*9900
				activeChar.sendPacket(new ActionFailed());
				return;
			}

			//activeChar.setXYZ(_originX, _originY, _originZ);
			//activeChar.sendPacket(new CharMoveToLocation(activeChar));
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,
					new L2CharPosition(_targetX, _targetY, _targetZ, 0));
			
			if(activeChar.getParty() != null)
				activeChar.getParty().broadcastToPartyMembers(activeChar,new PartyMemberPosition(activeChar));
		}		
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__01_MOVEBACKWARDTOLOC;
	}
}
