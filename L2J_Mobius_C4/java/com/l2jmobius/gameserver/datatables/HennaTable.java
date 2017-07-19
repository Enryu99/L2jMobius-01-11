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
package com.l2jmobius.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.templates.L2Henna;
import com.l2jmobius.gameserver.templates.StatsSet;

import javolution.util.FastMap;

/**
 * This class ...
 * @version $Revision$ $Date$
 */
public class HennaTable
{
	private static Logger _log = Logger.getLogger(HennaTable.class.getName());
	
	private static HennaTable _instance;
	
	private final Map<Integer, L2Henna> _henna;
	private final boolean _initialized = true;
	
	public static HennaTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new HennaTable();
		}
		return _instance;
	}
	
	private HennaTable()
	{
		_henna = new FastMap<>();
		RestoreHennaData();
	}
	
	/**
	 * 
	 */
	private void RestoreHennaData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT symbol_id, symbol_name, dye_id, dye_amount, price, stat_INT, stat_STR, stat_CON, stat_MEN, stat_DEX, stat_WIT FROM henna");
			ResultSet hennadata = statement.executeQuery())
		{
			fillHennaTable(hennadata);
		}
		catch (final Exception e)
		{
			_log.severe("error while creating henna table " + e);
			e.printStackTrace();
		}
	}
	
	private void fillHennaTable(ResultSet HennaData) throws Exception
	{
		while (HennaData.next())
		{
			final StatsSet hennaDat = new StatsSet();
			final int id = HennaData.getInt("symbol_id");
			
			hennaDat.set("symbol_id", id);
			// hennaDat.set("symbol_name", HennaData.getString("symbol_name"));
			hennaDat.set("dye", HennaData.getInt("dye_id"));
			hennaDat.set("price", HennaData.getInt("price"));
			// amount of dye required
			hennaDat.set("amount", HennaData.getInt("dye_amount"));
			hennaDat.set("stat_INT", HennaData.getInt("stat_INT"));
			hennaDat.set("stat_STR", HennaData.getInt("stat_STR"));
			hennaDat.set("stat_CON", HennaData.getInt("stat_CON"));
			hennaDat.set("stat_MEN", HennaData.getInt("stat_MEN"));
			hennaDat.set("stat_DEX", HennaData.getInt("stat_DEX"));
			hennaDat.set("stat_WIT", HennaData.getInt("stat_WIT"));
			
			final L2Henna template = new L2Henna(hennaDat);
			_henna.put(id, template);
		}
		_log.config("HennaTable: Loaded " + _henna.size() + " Templates.");
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	public L2Henna getTemplate(int id)
	{
		return _henna.get(id);
	}
}