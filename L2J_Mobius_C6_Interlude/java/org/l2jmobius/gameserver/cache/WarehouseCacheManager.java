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
package org.l2jmobius.gameserver.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author -Nemesiss-
 */
public class WarehouseCacheManager
{
	protected final Map<PlayerInstance, Long> _cachedWh;
	protected final long _cacheTime;
	
	private WarehouseCacheManager()
	{
		_cacheTime = Config.WAREHOUSE_CACHE_TIME * 60000; // 60*1000 = 60000
		_cachedWh = new ConcurrentHashMap<>();
		ThreadPool.scheduleAtFixedRate(new CacheScheduler(), 120000, 60000);
	}
	
	public void addCacheTask(PlayerInstance pc)
	{
		_cachedWh.put(pc, System.currentTimeMillis());
	}
	
	public void remCacheTask(PlayerInstance pc)
	{
		_cachedWh.remove(pc);
	}
	
	public class CacheScheduler implements Runnable
	{
		@Override
		public void run()
		{
			final long cTime = System.currentTimeMillis();
			for (PlayerInstance pc : _cachedWh.keySet())
			{
				if ((cTime - _cachedWh.get(pc)) > _cacheTime)
				{
					pc.clearWarehouse();
					_cachedWh.remove(pc);
				}
			}
		}
	}
	
	public static WarehouseCacheManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final WarehouseCacheManager INSTANCE = new WarehouseCacheManager();
	}
}
