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
import com.festina.gameserver.RecipeController;

public class RequestRecipeBookOpen extends ClientBasePacket 
{
    private static final String _C__AC_REQUESTRECIPEBOOKOPEN = "[C] AC RequestRecipeBookOpen";
	private static Logger _log = Logger.getLogger(RequestRecipeBookOpen.class.getName());
    
    private final boolean isDwarvenCraft;

	/**
	 * packet type id 0xac
	 * packet format rev656  cd
	 * @param decrypt
	 */
	public RequestRecipeBookOpen(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
        isDwarvenCraft = (readD() == 0);
        if (Config.DEBUG) _log.info("RequestRecipeBookOpen : " + (isDwarvenCraft ? "dwarvenCraft" : "commonCraft"));
	}

	void runImpl()
	{
	    if (getClient().getActiveChar() == null)
	        return;
        
        if (getClient().getActiveChar().getPrivateStoreType() != 0)
        {
            getClient().getActiveChar().sendMessage("Cannot use recipe book while trading");
            return;
        }
        
        RecipeController.getInstance().requestBookOpen(getClient().getActiveChar(), isDwarvenCraft);
	}
	
    /* (non-Javadoc)
     * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType() 
    {
        return _C__AC_REQUESTRECIPEBOOKOPEN;
    }
}
