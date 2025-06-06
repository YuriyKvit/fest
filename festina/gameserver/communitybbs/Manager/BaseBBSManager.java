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
package com.festina.gameserver.communitybbs.Manager;

import java.util.List;
import java.util.logging.*;
import javolution.util.FastList;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.serverpackets.ShowBoard;
import com.festina.gameserver.cache.HtmCache;

public abstract class BaseBBSManager
{
	public static final String PWHTML = "data/html/CommunityBoard/";
	public static HtmCache _hc = HtmCache.getInstance();
	protected static final Logger _log = Logger.getLogger(BaseBBSManager.class.getName());

	public abstract void parsecmd(String command, L2PcInstance activeChar);
	public abstract void parsewrite(String ar1,String ar2,String ar3,String ar4,String ar5, L2PcInstance activeChar);
	protected void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null) {
			return;
		}
		if (html.length() < 8180)
		{
			acha.sendPacket(new ShowBoard(html, "101"));
			acha.sendPacket(new ShowBoard(null, "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < 16360)
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 8180), "101"));
			acha.sendPacket(new ShowBoard(html.substring(8180, html.length()), "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
			
		}
		else if (html.length() < 24540)
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 8180), "101"));
			acha.sendPacket(new ShowBoard(html.substring(8180, 16360), "102"));
			acha.sendPacket(new ShowBoard(html.substring(16360, html.length()), "103"));
			
		}
	}
	/**
	 * @param html
	 */
	protected void send1001(String html, L2PcInstance acha)
	{
		if (html.length() < 8180)
		{
			acha.sendPacket(new ShowBoard(html, "1001"));			
		}
	}
	/**
	 * @param i
	 */
	protected void send1002(L2PcInstance acha)
	{		
		send1002(acha," "," ","0");
	}
	/**
	 * @param activeChar
	 * @param string
	 * @param string2
	 */
	protected void send1002(L2PcInstance activeChar, String string, String string2,String string3)
	{		
		List<String> _arg = new FastList<String>();
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add(activeChar.getName());
		_arg.add(Integer.toString(activeChar.getObjectId()));
		_arg.add(activeChar.getAccountName());
		_arg.add("9");
		_arg.add(string2);
		_arg.add(string2);
		_arg.add(string);		
		_arg.add(string3);
		_arg.add(string3);
		_arg.add("0");
		_arg.add("0");
		activeChar.sendPacket(new ShowBoard(_arg));
	}

	public static String getPwHtm(String page) {
		return _hc.getHtm(PWHTML + page + ".htm");
	}

}
