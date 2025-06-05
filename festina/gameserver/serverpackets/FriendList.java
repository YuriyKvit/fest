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
package com.festina.gameserver.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.festina.L2DatabaseFactory;
import com.festina.gameserver.model.L2World;
import com.festina.gameserver.model.actor.instance.L2PcInstance;

/**
 * Support for "Chat with Friends" dialog. 
 * 
 * Format: ch (hdSdh)
 * h: Total Friend Count
 * 
 * h: Unknown
 * d: Player Object ID
 * S: Friend Name
 * d: Online/Offline
 * h: Unknown
 * 
 * @author Tempy
 *
 */
public class FriendList extends ServerBasePacket
{
	private static Logger _log = Logger.getLogger(FriendList.class.getName());
	private static final String _S__FA_FRIENDLIST = "[S] FA FriendList";
	int objectId = 0;
	int LogOutObjectId = 0;
	private static L2PcInstance _cha;
	private static L2PcInstance _logouter;
	private boolean _logout;
    
    public FriendList(L2PcInstance cha, boolean logout, L2PcInstance logouter)
    {
        _cha = cha;
        objectId = cha.getObjectId();
        _logout = logout;
        _logouter = logouter;
    }
	
	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		if (_cha == null)  
			return;  
		
        Connection con = null;
        
		try
		{
			String sqlQuery = "SELECT friend_id, friend_name FROM character_friends WHERE " +
                    "char_id=" + objectId + " ORDER BY friend_name ASC";
			
			con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(sqlQuery);
			ResultSet rset = statement.executeQuery(sqlQuery);

			// Obtain the total number of friend entries for this player.
			rset.last();
            
            if (rset.getRow() > 0)
            {
            	if (_logout)
            	{
            		writeC(0xfa);
            		writeH(rset.getRow());
            		writeH(0);
            		writeD(_logouter.getObjectId());
            		writeS(_logouter.getName());
            		writeD(0);
            	}
            	else
            	{
            		writeC(0xfa);
            		writeH(rset.getRow());
                
    			rset.beforeFirst();
                
    			while (rset.next())
    			{
                    int friendId = rset.getInt("friend_id");
    				String friendName = rset.getString("friend_name");
                    
                    if (friendId == objectId)
                        continue;
    				
    				L2PcInstance friend = (L2PcInstance)L2World.getInstance().findObject(friendId);
    
    				writeH(0); // ??
    				writeD(objectId);
    				writeS(friendName);
    				
    				if (friend == null)
    					writeD(0); // offline
    				else
    					writeD(1); // online
    			
    				writeH(0); // ??				
    			}
            	}
            	}
            	else 
            	{
            		writeC(0xfa);
            		writeH(0);
            		writeH(0);
            		writeD(0);
            		writeS("");
            		writeD(0);
            	}
			rset.close();
			statement.close();
		}
		catch (Exception e)	{
			_log.warning("Error found in " + _cha.getName() + "'s FriendList: " + e);
			e.printStackTrace();
		}
		finally	{
			try {con.close();} catch (Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__FA_FRIENDLIST;
	}
}
