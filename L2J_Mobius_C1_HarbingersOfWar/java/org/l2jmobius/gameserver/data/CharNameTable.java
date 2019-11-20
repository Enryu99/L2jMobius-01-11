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
package org.l2jmobius.gameserver.data;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CharNameTable
{
	private static Logger _log = Logger.getLogger(CharNameTable.class.getName());
	private static CharNameTable _instance;
	private final List<String> _charNames;
	
	public static CharNameTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new CharNameTable();
		}
		return _instance;
	}
	
	private CharNameTable()
	{
		File _accountsFolder = new File("data/accounts");
		_accountsFolder.mkdirs();
		_charNames = new ArrayList<>();
		File[] accounts = _accountsFolder.listFiles();
		for (File account : accounts)
		{
			try
			{
				File _charFolder = new File("data/accounts/" + account.getName());
				File[] chars = _charFolder.listFiles((FilenameFilter) (dir, name) -> name.endsWith("_char.csv"));
				for (File c : chars)
				{
					_charNames.add(c.getName().replaceAll("_char.csv", "").toLowerCase());
				}
				continue;
			}
			catch (NullPointerException e)
			{
				// empty catch block
			}
		}
		_log.fine("Loaded " + _charNames.size() + " charnames to the memory.");
	}
	
	public void addCharName(String name)
	{
		_log.fine("Added charname: " + name + " to the memory.");
		_charNames.add(name.toLowerCase());
	}
	
	public void deleteCharName(String name)
	{
		_log.fine("Deleted charname: " + name + " from the memory.");
		_charNames.remove(name.toLowerCase());
	}
	
	public boolean doesCharNameExist(String name)
	{
		return _charNames.contains(name.toLowerCase());
	}
}
