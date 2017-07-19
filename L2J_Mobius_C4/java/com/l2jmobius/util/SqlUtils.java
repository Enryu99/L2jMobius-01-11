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
package com.l2jmobius.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.l2jmobius.L2DatabaseFactory;

public class SqlUtils
{
	private static Logger _log = Logger.getLogger(SqlUtils.class.getName());
	
	// =========================================================
	// Data Field
	private static SqlUtils _instance;
	
	// =========================================================
	// Property - Public
	public static SqlUtils getInstance()
	{
		if (_instance == null)
		{
			_instance = new SqlUtils();
		}
		return _instance;
	}
	
	// =========================================================
	// Method - Public
	public static Integer getIntValue(String resultField, String tableName, String whereClause)
	{
		Integer res = null;
		
		String query = "";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			query = L2DatabaseFactory.getInstance().prepQuerySelect(new String[]
			{
				resultField
			}, tableName, whereClause, true);
			try (PreparedStatement statement = con.prepareStatement(query);
				ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					res = rset.getInt(1);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		
		return res;
	}
	
	public static Integer[] getIntArray(String resultField, String tableName, String whereClause)
	{
		Integer[] res = null;
		
		String query = "";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			query = L2DatabaseFactory.getInstance().prepQuerySelect(new String[]
			{
				resultField
			}, tableName, whereClause, false);
			try (PreparedStatement statement = con.prepareStatement(query);
				ResultSet rset = statement.executeQuery())
			{
				int rows = 0;
				
				while (rset.next())
				{
					rows++;
				}
				
				if (rows == 0)
				{
					return new Integer[0];
				}
				
				res = new Integer[rows - 1];
				
				rset.first();
				
				final int row = 0;
				while (rset.next())
				{
					res[row] = rset.getInt(1);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("mSGI: Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		
		return res;
	}
	
	public static Integer[][] get2DIntArray(String[] resultFields, String usedTables, String whereClause)
	{
		final long start = System.currentTimeMillis();
		
		Integer res[][] = null;
		
		String query = "";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			query = L2DatabaseFactory.getInstance().prepQuerySelect(resultFields, usedTables, whereClause, false);
			try (PreparedStatement statement = con.prepareStatement(query);
				ResultSet rset = statement.executeQuery())
			{
				int rows = 0;
				
				while (rset.next())
				{
					rows++;
				}
				
				res = new Integer[rows - 1][resultFields.length];
				
				rset.first();
				
				int row = 0;
				while (rset.next())
				{
					for (int i = 0; i < resultFields.length; i++)
					{
						res[row][i] = rset.getInt(i + 1);
					}
					row++;
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		
		_log.fine("Get all rows in query '" + query + "' in " + (System.currentTimeMillis() - start) + "ms");
		return res;
	}
}