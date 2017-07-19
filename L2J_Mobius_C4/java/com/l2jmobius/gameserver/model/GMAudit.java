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
package com.l2jmobius.gameserver.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.Config;

public class GMAudit
{
	private static final Logger _log = Logger.getLogger("gmaudit");
	
	public static void auditGMAction(String gmName, String action, String target, String params)
	{
		if (Config.GMAUDIT)
		{
			String today;
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
			today = formatter.format(new Date());
			
			_log.log(Level.INFO, today + ">" + gmName + ">" + action + ">" + target + ">" + params);
		}
	}
}