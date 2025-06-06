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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.festina.L2DatabaseFactory;
import com.festina.gameserver.ClientThread;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.FriendList;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestFriendDel extends ClientBasePacket{
	
	private static final String _C__61_REQUESTFRIENDDEL = "[C] 61 RequestFriendDel";
	private static Logger _log = Logger.getLogger(RequestFriendDel.class.getName());

	private final String _name;
	
	public RequestFriendDel(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_name = readS();
	}

	void runImpl()
	{
		SystemMessage sm;
		java.sql.Connection con = null;
		L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) 
            return;
        
		try 
		{
		    L2PcInstance friend = L2World.getInstance().getPlayer(_name);
		    con = L2DatabaseFactory.getInstance().getConnection();
		    PreparedStatement statement;
		    ResultSet rset;
		    if (friend != null)
            {
    			statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id=? and friend_id=?");
    			statement.setInt(1, activeChar.getObjectId());
    			statement.setInt(2, friend.getObjectId());
    			rset = statement.executeQuery();
    			if (!rset.next())
                {
    			    statement.close();
    			    // Player is not in your friendlist
    			    sm = new SystemMessage(171);
    			    sm.addString(_name);
    			    activeChar.sendPacket(sm);
    			    return;
    			}
		    } else 
            {
    			statement = con.prepareStatement("SELECT friend_id FROM character_friends, characters WHERE char_id=? AND friend_id=obj_id AND char_name=?");
    			statement.setInt(1, activeChar.getObjectId());
    			statement.setString(2, _name);
    			rset = statement.executeQuery();
    			if (!rset.next())
                {
    				statement.close();
    				// Player is not in your friendlist
    				sm = new SystemMessage(171);
    				sm.addString(_name);
    				activeChar.sendPacket(sm);
    				return;
    			}
		    }
            
			int objectId = rset.getInt("friend_id");
			statement.close();
            rset.close();
            
			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? AND friend_id=?");
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, objectId);
			statement.execute();
			// Player deleted from your friendlist
			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? AND friend_id=?");
			statement.setInt(1, objectId);
			statement.setInt(2, activeChar.getObjectId());
			statement.execute();
			
			friend.sendPacket(new FriendList(friend, false, null));
			activeChar.sendPacket(new FriendList(activeChar, false, null));
			sm = new SystemMessage(133);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			statement.close();
		} 
		catch (Exception e)
		{
		    _log.log(Level.WARNING, "could not del friend objectid: ", e);
		}
		finally
		{
		    try { con.close(); } catch (Exception e) {}
		}
		
	}
	
	
	public String getType()
	{
		return _C__61_REQUESTFRIENDDEL;
	}
}

