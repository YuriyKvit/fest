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
package com.festina.gameserver.script;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Luis Arias
 *
 */
public class DateRange
{
	
    private Date startDate, endDate;
    
    public DateRange(Date from, Date to)
    {
        this.startDate   = from;
        this.endDate     = to;
    }
    
    public static DateRange parse(String dateRange, DateFormat format)
    {
        String[] date = dateRange.split("-");
        if (date.length == 2)
        {
            try
            {
                Date start  = format.parse(date[0]);
                Date end    = format.parse(date[1]);
                
                return new DateRange(start, end);
            } 
            catch (ParseException e)
            {
                System.err.println("Invalid Date Format.");
                e.printStackTrace();
            }
        }
        return new DateRange(null, null);
    }
    
    public boolean isValid()
    {
        return startDate == null || endDate == null;
    }
    
    public boolean isWithinRange(Date date)
    {
        return date.after(startDate) && date.before(endDate);
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public Date getStartDate()
    {
        return startDate;
    }
}
