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
import com.festina.gameserver.instancemanager.QuestManager;
import com.festina.gameserver.model.actor.instance.L2PcInstance;
import com.festina.gameserver.model.quest.Quest;
import com.festina.gameserver.model.quest.QuestState;
import com.festina.gameserver.serverpackets.QuestList;
import com.festina.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestQuestAbort extends ClientBasePacket
{
	private static final String _C__64_REQUESTQUESTABORT = "[C] 64 RequestQuestAbort";
	private static Logger _log = Logger.getLogger(RequestQuestAbort.class.getName());

	private final int _QuestID;
	/**
	 * packet type id 0x64<p>
	 */
	public RequestQuestAbort(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_QuestID = readD();
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		    return;
        
        Quest qe = QuestManager.getInstance().getQuest(_QuestID);
        if (qe != null)
        {
    		QuestState qs = activeChar.getQuestState(qe.getName());
            if(qs != null)
            {
        		qs.exitQuest(true);
        		SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
        		sm.addString("Quest aborted.");
                activeChar.sendPacket(sm);
        		QuestList ql = new QuestList();
                activeChar.sendPacket(ql);
            } else
            {
                if (Config.DEBUG) _log.info("Player '"+activeChar.getName()+"' try to abort quest "+qe.getName()+" but he didn't have it started.");
            }
        } else
        {
            if (Config.DEBUG) _log.warning("Quest (id='"+_QuestID+"') not found.");
        }
	}

	/* (non-Javadoc)
	 * @see com.festina.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__64_REQUESTQUESTABORT;
	}
}