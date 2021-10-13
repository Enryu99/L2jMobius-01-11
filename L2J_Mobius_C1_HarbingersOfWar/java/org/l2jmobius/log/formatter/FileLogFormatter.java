/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.l2jmobius.log.formatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringJoiner;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.l2jmobius.Config;

/**
 * @version $Revision: 1.1.4.1 $ $Date: 2005/03/27 15:30:08 $
 */
public class FileLogFormatter extends Formatter
{
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss,SSS");
	
	@Override
	public String format(LogRecord record)
	{
		final StringJoiner sj = new StringJoiner("\t", "", Config.EOL);
		sj.add(dateFormat.format(new Date(record.getMillis())));
		sj.add(record.getLevel().getName());
		sj.add(String.valueOf(record.getLongThreadID()));
		sj.add(record.getLoggerName());
		sj.add(record.getMessage());
		return sj.toString();
	}
}
