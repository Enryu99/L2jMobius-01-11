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
package com.l2jmobius.gameserver.idfactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.L2DatabaseFactory;

/**
 * This class ...
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class CompactionIDFactory extends IdFactory
{
	private static Logger _log = Logger.getLogger(CompactionIDFactory.class.getName());
	private int _curOID;
	private final int _freeSize;
	
	protected CompactionIDFactory()
	{
		super();
		_curOID = FIRST_OID;
		_freeSize = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final Integer[] tmp_obj_ids = extractUsedObjectIDTable();
			
			int N = tmp_obj_ids.length;
			for (int idx = 0; idx < N; idx++)
			{
				N = insertUntil(tmp_obj_ids, idx, N, con);
			}
			_curOID++;
			_log.config("IdFactory: Next usable Object ID is: " + _curOID);
			initialized = true;
		}
		catch (final Exception e1)
		{
			e1.printStackTrace();
			_log.severe("ID Factory could not be initialized correctly:" + e1);
		}
	}
	
	private int insertUntil(Integer[] tmp_obj_ids, int idx, int N, Connection con) throws SQLException
	{
		int id = tmp_obj_ids[idx];
		if (id == _curOID)
		{
			_curOID++;
			return N;
		}
		// check these IDs not present in DB
		if (Config.BAD_ID_CHECKING)
		{
			for (final String check : id_checks)
			{
				try (PreparedStatement ps = con.prepareStatement(check))
				{
					ps.setInt(1, _curOID);
					ps.setInt(2, id);
					try (ResultSet rs = ps.executeQuery())
					{
						while (rs.next())
						{
							final int badId = rs.getInt(1);
							_log.severe("Bad ID " + badId + " in DB found by: " + check);
							throw new RuntimeException();
						}
					}
				}
			}
		}
		
		int hole = id - _curOID;
		if (hole > (N - idx))
		{
			hole = N - idx;
		}
		for (int i = 1; i <= hole; i++)
		{
			id = tmp_obj_ids[N - i];
			System.out.println("Compacting DB object ID=" + id + " into " + (_curOID));
			for (final String update : id_updates)
			{
				try (PreparedStatement ps = con.prepareStatement(update))
				{
					ps.setInt(1, _curOID);
					ps.setInt(2, id);
					ps.execute();
				}
			}
			_curOID++;
		}
		if (hole < (N - idx))
		{
			_curOID++;
		}
		return N - hole;
	}
	
	@Override
	public synchronized int getNextId()
	{
		return _curOID++;
	}
	
	@Override
	public synchronized void releaseId(int id)
	{
		// dont release ids until we are sure it isnt messing up
		/*
		 * if (_freeSize >= _freeOIDs.length) { int[] tmp = new int[_freeSize + STACK_SIZE_INCREMENT]; System.arraycopy(_freeOIDs, 0, tmp, 0, _freeSize); _freeOIDs = tmp; } _freeOIDs[_freeSize++] = id;
		 */
	}
	
	@Override
	public int size()
	{
		return (_freeSize + LAST_OID) - FIRST_OID;
	}
}