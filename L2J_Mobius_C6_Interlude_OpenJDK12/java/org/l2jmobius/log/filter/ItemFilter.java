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
package org.l2jmobius.log.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.l2jmobius.gameserver.model.actor.instance.ItemInstance;

/**
 * @author Advi
 */
public class ItemFilter implements Filter
{
	private String _excludeProcess;
	private String _excludeItemType;
	
	// This is example how to exclude consuming of shots and arrows from logging
	// private String _excludeProcess = "Consume";
	// private String _excludeItemType = "Arrow, Shot";
	
	@Override
	public boolean isLoggable(LogRecord record)
	{
		if (record.getLoggerName() != "item")
		{
			return false;
		}
		
		if (_excludeProcess != null)
		{
			// if (record.getMessage() == null) return true;
			final String[] messageList = record.getMessage().split(":");
			
			if ((messageList.length < 2) || !_excludeProcess.contains(messageList[1]))
			{
				return true;
			}
		}
		if (_excludeItemType != null)
		{
			// if (record.getParameters() == null || record.getParameters().length == 0 || !(record.getParameters()[0] instanceof ItemInstance))
			// return true;
			final ItemInstance item = (ItemInstance) record.getParameters()[0];
			
			if (!_excludeItemType.contains(item.getItemType().toString()))
			{
				return true;
			}
		}
		return (_excludeProcess == null) && (_excludeItemType == null);
	}
}
