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
package com.festina;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;

import javolution.lang.TextBuilder;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.4.1 $ $Date: 2005/03/27 15:30:08 $
 */

public class FileLogFormatter extends Formatter
{

	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	private static final String CRLF = "\r\n";
	private static final String STRING = "\t";
	private SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss,SSS");
	
	public String format(LogRecord record)
	{
        TextBuilder output = new TextBuilder();
		
		return output
		.append(dateFmt.format(new Date(record.getMillis())))
		.append(STRING)
		.append(record.getLevel().getName())
		.append(STRING)
		.append(record.getThreadID())
		.append(STRING)
		.append(record.getLoggerName())
		.append(STRING)
		.append(record.getMessage())
		.append(CRLF)
		.toString();
	}
}
