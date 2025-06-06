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



/**
 *
 * sample
 * <p>
 * 7d 
 * c1 b2 e0 4a 
 * 00 00 00 00
 * <p>
 * 
 * format
 * cdd
 * 
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class AskJoinAlly extends ServerBasePacket
{
	private static final String _S__a8_ASKJOINALLY_0Xa8 = "[S] a8 AskJoinAlly 0xa8";
	//private static Logger _log = Logger.getLogger(AskJoinAlly.class.getName());

	private String _requestorName;
    private int _requestorId;

	/**
	 *
	 */
	public AskJoinAlly(int requestorId, String requestorName)
	{
		_requestorName = requestorName;
    		_requestorId = requestorId;
	}

	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0xa8);
    		writeD(_requestorId);
		writeS(_requestorName);
	}
	
	/* (non-Javadoc)
	 * @see com.festina.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__a8_ASKJOINALLY_0Xa8;
	}

}
