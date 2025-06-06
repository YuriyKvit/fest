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
package com.festina.gameserver.script.faenor;

import java.util.Date;
import java.util.logging.Logger;

import com.festina.gameserver.script.DateRange;
import com.festina.gameserver.script.IntList;
import com.festina.gameserver.script.Parser;
import com.festina.gameserver.script.ParserFactory;
import com.festina.gameserver.script.ScriptEngine;

import org.apache.bsf.BSFManager;
import org.w3c.dom.Node;

/**
 * @author Luis Arias
 *
 */
public class FaenorEventParser extends FaenorParser
{
    static Logger _log = Logger.getLogger(FaenorEventParser.class.getName());
    private DateRange eventDates = null;
    
    public void parseScript(Node eventNode, @SuppressWarnings("unused") BSFManager context)
    {
        String ID = attribute(eventNode, "ID");
        
        if (DEBUG) _log.fine("Parsing Event \""+ID+"\"");
        
        eventDates = DateRange.parse(attribute(eventNode, "Active"), DATE_FORMAT);
        
        Date currentDate = new Date();
        if (eventDates.getEndDate().before(currentDate))
        {
            _log.warning("Event ID: (" + ID + ") has passed... Ignored.");
            return;
        }
        
        for (Node node = eventNode.getFirstChild(); node != null; node = node.getNextSibling()) {
            
            if (isNodeName(node, "DropList"))
            {
                parseEventDropList(node);
            }
            else if (isNodeName(node, "Message"))
            {
                parseEventMessage(node);
            } 
        }
    }

    private void parseEventMessage(Node sysMsg)
    {
        if (DEBUG) _log.fine("Parsing Event Message.");

        try
        {
            String type         = attribute(sysMsg, "Type");
            String[] message    = attribute(sysMsg, "Msg").split("\n");
            
            if (type.equalsIgnoreCase("OnJoin"))
            {
                bridge.onPlayerLogin(message, eventDates);
            }
        } 
        catch (Exception e)
        {
            _log.warning("Error in event parser.");
            e.printStackTrace();
        }
    }

    private void parseEventDropList(Node dropList)
    {
        if (DEBUG) _log.fine("Parsing Droplist.");
        
        for (Node node = dropList.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (isNodeName(node, "AllDrop"))
            {
                parseEventDrop(node);
            }
        }
    }

    private void parseEventDrop(Node drop)
    {
        if (DEBUG) _log.fine("Parsing Drop.");
        
        try
        {
            int[] items         = IntList.parse(attribute(drop, "Items"));
            int[] count         = IntList.parse(attribute(drop, "Count"));
            double chance       = getPercent(attribute(drop, "Chance"));
            
            bridge.addEventDrop(items, count, chance, eventDates);
        }
        catch (Exception e)
        {
            System.err.println("ERROR(parseEventDrop):" + e.getMessage());
        }
    }
    
    static class FaenorEventParserFactory extends ParserFactory
    {
        public Parser create()
        {
            return(new FaenorEventParser());
        }
    }

    static
    {
        ScriptEngine.parserFactories.put(getParserName("Event"), new FaenorEventParserFactory());
    }
}
