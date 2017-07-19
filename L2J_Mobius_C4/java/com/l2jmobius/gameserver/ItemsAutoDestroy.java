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
package com.l2jmobius.gameserver;

import java.util.List;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2World;

import javolution.util.FastList;

public class ItemsAutoDestroy
{
	protected static Logger _log = Logger.getLogger("ItemsAutoDestroy");
	private static ItemsAutoDestroy _instance;
	protected List<L2ItemInstance> _items = null;
	protected static long _sleep;
	
	private ItemsAutoDestroy()
	{
		_items = new FastList<>();
		_sleep = Config.AUTODESTROY_ITEM_AFTER * 1000;
		if (_sleep == 0)
		{
			_sleep = 3600000;
		}
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckItemsForDestroy(), 5000, 5000);
	}
	
	public static ItemsAutoDestroy getInstance()
	{
		if (_instance == null)
		{
			System.out.println("Initializing ItemsAutoDestroy.");
			_instance = new ItemsAutoDestroy();
		}
		return _instance;
	}
	
	public synchronized void addItem(L2ItemInstance item)
	{
		item.setDropTime(System.currentTimeMillis());
		_items.add(item);
	}
	
	public synchronized void removeItems()
	{
		if (Config.DEBUG)
		{
			_log.info("[ItemsAutoDestroy] : " + _items.size() + " items to check.");
		}
		
		if (_items.isEmpty())
		{
			return;
		}
		
		final long curtime = System.currentTimeMillis();
		for (final L2ItemInstance item : _items)
		{
			if ((item == null) || (item.getDropTime() == 0) || (item.getLocation() != L2ItemInstance.ItemLocation.VOID))
			{
				_items.remove(item);
			}
			else
			{
				if ((curtime - item.getDropTime()) > _sleep)
				{
					L2World.getInstance().removeVisibleObject(item, item.getWorldRegion());
					L2World.getInstance().removeObject(item);
					
					_items.remove(item);
					if (Config.SAVE_DROPPED_ITEM)
					{
						ItemsOnGroundManager.getInstance().removeObject(item);
					}
				}
			}
		}
		
		if (Config.DEBUG)
		{
			_log.info("[ItemsAutoDestroy] : " + _items.size() + " items remaining.");
		}
	}
	
	protected class CheckItemsForDestroy extends Thread
	{
		@Override
		public void run()
		{
			removeItems();
		}
	}
}