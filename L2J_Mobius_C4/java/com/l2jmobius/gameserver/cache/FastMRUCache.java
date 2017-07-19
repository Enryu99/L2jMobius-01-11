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
package com.l2jmobius.gameserver.cache;

import javolution.context.ObjectFactory;
import javolution.lang.Reusable;
import javolution.util.FastCollection;
import javolution.util.FastComparator;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastMap.Entry;

/**
 * @author Layane
 * @param <K>
 * @param <V>
 */
public class FastMRUCache<K, V>extends FastCollection<Object> implements Reusable
{
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_CAPACITY = 50;
	public static final int DEFAULT_FORGET_TIME = 300000; // 5 Minutes
	
	FastMap<K, CacheNode> _cache = new FastMap<K, CacheNode>().setKeyComparator(FastComparator.DIRECT);
	FastMap<K, V> _map;
	FastList<K> _mruList = new FastList<>();
	int _cacheSize;
	int _forgetTime;
	
	class CacheNode
	{
		long lastModified;
		V node;
		
		public CacheNode(V object)
		{
			lastModified = System.currentTimeMillis();
			node = object;
		}
		
		@Override
		public boolean equals(Object object)
		{
			return node == object;
		}
		
	}
	
	/**
	 * Holds the set factory.
	 */
	private static final ObjectFactory<?> FACTORY = new ObjectFactory<Object>()
	{
		
		@Override
		public Object create()
		{
			return new FastMRUCache<>();
		}
		
		@Override
		public void cleanup(Object obj)
		{
			((FastMRUCache<?, ?>) obj).reset();
		}
	};
	
	/**
	 * Returns a set allocated from the stack when executing in a PoolContext}).
	 * @return a new, pre-allocated or recycled set instance.
	 */
	public static FastMRUCache<?, ?> newInstance()
	{
		return (FastMRUCache<?, ?>) FACTORY.object();
	}
	
	public FastMRUCache()
	{
		this(new FastMap<K, V>(), DEFAULT_CAPACITY, DEFAULT_FORGET_TIME);
	}
	
	public FastMRUCache(FastMap<K, V> map)
	{
		this(map, DEFAULT_CAPACITY, DEFAULT_FORGET_TIME);
	}
	
	public FastMRUCache(FastMap<K, V> map, int max)
	{
		this(map, max, DEFAULT_FORGET_TIME);
	}
	
	public FastMRUCache(FastMap<K, V> map, int max, int forgetTime)
	{
		_map = map;
		_cacheSize = max;
		_forgetTime = forgetTime;
		_map.setKeyComparator(FastComparator.DIRECT);
	}
	
	// Implements Reusable.
	@Override
	public synchronized void reset()
	{
		_map.reset();
		_cache.reset();
		_mruList.reset();
		_map.setKeyComparator(FastComparator.DIRECT);
		_cache.setKeyComparator(FastComparator.DIRECT);
	}
	
	public synchronized V get(K key)
	{
		V result;
		
		if (!_cache.containsKey(key))
		{
			
			if (_mruList.size() >= _cacheSize)
			{
				_cache.remove(_mruList.getLast());
				_mruList.removeLast();
			}
			
			result = _map.get(key);
			
			_cache.put(key, new CacheNode(result));
			_mruList.addFirst(key);
			
		}
		else
		{
			final CacheNode current = _cache.get(key);
			
			if ((current.lastModified + _forgetTime) <= System.currentTimeMillis())
			{
				
				current.lastModified = System.currentTimeMillis();
				current.node = _map.get(key);
				_cache.put(key, current);
				
			}
			
			_mruList.remove(key);
			_mruList.addFirst(key);
			
			result = current.node;
		}
		
		return result;
	}
	
	@Override
	public synchronized boolean remove(Object key)
	{
		_cache.remove(key);
		_mruList.remove(key);
		return _map.remove(key) == key;
	}
	
	public FastMap<K, V> getContentMap()
	{
		return _map;
	}
	
	@Override
	public int size()
	{
		return _mruList.size();
	}
	
	public int capacity()
	{
		return _cacheSize;
	}
	
	public int getForgetTime()
	{
		return _forgetTime;
	}
	
	@Override
	public synchronized void clear()
	{
		_cache.clear();
		_mruList.clear();
		_map.clear();
	}
	
	// Implements FastCollection abstract method.
	@Override
	public final Record head()
	{
		return _mruList.head();
	}
	
	@Override
	public final Record tail()
	{
		return _mruList.tail();
	}
	
	@Override
	public final Object valueOf(Record record)
	{
		return ((Entry<?, ?>) record).getKey();
	}
	
	@Override
	public final void delete(Record record)
	{
		remove(((Entry<?, ?>) record).getKey());
	}
}