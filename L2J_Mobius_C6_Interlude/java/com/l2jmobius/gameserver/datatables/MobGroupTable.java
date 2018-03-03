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

import java.util.HashMap;
import java.util.Map;

import com.l2jmobius.gameserver.model.MobGroup;
import com.l2jmobius.gameserver.model.actor.instance.L2ControllableMobInstance;

/**
 * @author littlecrow
 */
public class MobGroupTable
{
	private static MobGroupTable _instance;
	private final Map<Integer, MobGroup> _groupMap;
	
	public static final int FOLLOW_RANGE = 300;
	public static final int RANDOM_RANGE = 300;
	
	public MobGroupTable()
	{
		_groupMap = new HashMap<>();
	}
	
	public static MobGroupTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new MobGroupTable();
		}
		
		return _instance;
	}
	
	public void addGroup(int groupKey, MobGroup group)
	{
		_groupMap.put(groupKey, group);
	}
	
	public MobGroup getGroup(int groupKey)
	{
		return _groupMap.get(groupKey);
	}
	
	public int getGroupCount()
	{
		return _groupMap.size();
	}
	
	public MobGroup getGroupForMob(L2ControllableMobInstance mobInst)
	{
		for (MobGroup mobGroup : _groupMap.values())
		{
			if (mobGroup.isGroupMember(mobInst))
			{
				return mobGroup;
			}
		}
		
		return null;
	}
	
	public MobGroup[] getGroups()
	{
		return _groupMap.values().toArray(new MobGroup[getGroupCount()]);
	}
	
	public boolean removeGroup(int groupKey)
	{
		return _groupMap.remove(groupKey) != null;
	}
}
