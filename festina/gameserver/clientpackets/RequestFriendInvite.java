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
import com.festina.gameserver.serverpackets.AskJoinFriend;
import com.festina.gameserver.serverpackets.SystemMessage;
import com.festina.gameserver.util.Util;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestFriendInvite extends ClientBasePacket{
	
	private static final String _C__5E_REQUESTFRIENDINVITE = "[C] 5E RequestFriendInvite";
	private static Logger _log = Logger.getLogger(RequestFriendInvite.class.getName());

	private String _name;
	
	public RequestFriendInvite(ByteBuffer buf, ClientThread client)
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
        
        L2PcInstance friend = L2World.getInstance().getPlayer(_name);
        _name = Util.capitalizeFirst(_name); //FIXME: is it right to capitalize a nickname?
        
    	if (friend == null)
        {
    	    //Target is not found in the game.
    	    sm = new SystemMessage(170);
    	    activeChar.sendPacket(sm);
    	    return;
    	}
        else if (friend == activeChar)
        {
    	    //You cannot add yourself to your own friend list.
    	    sm = new SystemMessage(165);
    	    activeChar.sendPacket(sm);
    	    return;
    	}

		try 
		{
		    con = L2DatabaseFactory.getInstance().getConnection();
		    PreparedStatement statement = con.prepareStatement("SELECT char_id FROM character_friends WHERE char_id=? AND friend_id=?");
		    statement.setInt(1, activeChar.getObjectId());
		    statement.setInt(2, friend.getObjectId());
		    ResultSet rset = statement.executeQuery();
		    
            if (rset.next())
            {
    			//Player already is in your friendlist
    			sm = new SystemMessage(167);
    			sm.addString(_name);
		    } 
            else 
            {
		        if (!friend.isProcessingRequest())
		        {
		    	    //requets to become friend
    			    activeChar.onTransactionRequest(friend);
    			    sm = new SystemMessage(168);
    			    sm.addString(_name);
    			    AskJoinFriend ajf = new AskJoinFriend(activeChar.getName());
    			    friend.sendPacket(ajf);
    			} 
                else 
                {
    			    sm = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
    			}
		    }  
            
			friend.sendPacket(sm);
			rset.close();
			statement.close();
		} 
		catch (Exception e)
		{
		    _log.log(Level.WARNING, "could not add friend objectid: ", e);
		}
		finally
		{
		    try { con.close(); } catch (Exception e) {}
		}
	}
	
	public String getType()
	{
		return _C__5E_REQUESTFRIENDINVITE;
	}
}