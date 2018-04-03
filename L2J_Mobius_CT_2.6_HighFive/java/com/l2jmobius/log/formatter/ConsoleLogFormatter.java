/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jmobius.log.formatter;

import java.util.logging.LogRecord;

import com.l2jmobius.Config;
import com.l2jmobius.commons.util.CommonUtil;

public class ConsoleLogFormatter extends AbstractFormatter
{
	@Override
	public String format(LogRecord record)
	{
		final StringBuilder output = new StringBuilder(128);
		output.append(super.format(record));
		output.append(Config.EOL);
		
		if (record.getThrown() != null)
		{
			try
			{
				output.append(CommonUtil.getStackTrace(record.getThrown()));
				output.append(Config.EOL);
			}
			catch (Exception ex)
			{
			}
		}
		return output.toString();
	}
}
