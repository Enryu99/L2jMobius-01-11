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
package com.l2jmobius.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.gameserver.InstanceListManager;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.entity.Fort;

public final class FortManager implements InstanceListManager
{
	protected static final Logger _log = Logger.getLogger(FortManager.class.getName());
	
	private final Map<Integer, Fort> _forts = new ConcurrentSkipListMap<>();
	
	public final Fort findNearestFort(L2Object obj)
	{
		return findNearestFort(obj, Long.MAX_VALUE);
	}
	
	public final Fort findNearestFort(L2Object obj, long maxDistance)
	{
		Fort nearestFort = getFort(obj);
		if (nearestFort == null)
		{
			for (Fort fort : getForts())
			{
				final double distance = fort.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					nearestFort = fort;
				}
			}
		}
		return nearestFort;
	}
	
	public final Fort getFortById(int fortId)
	{
		for (Fort f : getForts())
		{
			if (f.getResidenceId() == fortId)
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFortByOwner(L2Clan clan)
	{
		for (Fort f : getForts())
		{
			if (f.getOwnerClan() == clan)
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFort(String name)
	{
		for (Fort f : getForts())
		{
			if (f.getName().equalsIgnoreCase(name.trim()))
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFort(int x, int y, int z)
	{
		for (Fort f : getForts())
		{
			if (f.checkIfInZone(x, y, z))
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFort(L2Object activeObject)
	{
		return getFort(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final Collection<Fort> getForts()
	{
		return _forts.values();
	}
	
	@Override
	public void loadInstances()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM fort ORDER BY id"))
		{
			while (rs.next())
			{
				final int fortId = rs.getInt("id");
				_forts.put(fortId, new Fort(fortId));
			}
			
			_log.info(getClass().getSimpleName() + ": Loaded: " + getForts().size() + " fortress");
			for (Fort fort : getForts())
			{
				fort.getSiege().loadSiegeGuard();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: loadFortData(): " + e.getMessage(), e);
		}
	}
	
	@Override
	public void updateReferences()
	{
	}
	
	@Override
	public void activateInstances()
	{
		for (Fort fort : getForts())
		{
			fort.activateInstance();
		}
	}
	
	public static FortManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FortManager _instance = new FortManager();
	}
}
