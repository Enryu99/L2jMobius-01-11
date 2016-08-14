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

import java.util.concurrent.ConcurrentHashMap;

import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.holders.WarpedSpaceHolder;
import com.l2jmobius.gameserver.model.instancezone.Instance;
import com.l2jmobius.gameserver.pathfinding.AbstractNodeLoc;
import com.l2jmobius.gameserver.util.Util;

/**
 * @author Sdw
 */
public class WarpedSpaceManager
{
	private volatile ConcurrentHashMap<L2Character, WarpedSpaceHolder> _warpedSpace = null;
	
	public void addWarpedSpace(L2Character creature, int radius)
	{
		if (_warpedSpace == null)
		{
			synchronized (this)
			{
				if (_warpedSpace == null)
				{
					_warpedSpace = new ConcurrentHashMap<>();
				}
			}
		}
		_warpedSpace.put(creature, new WarpedSpaceHolder(creature, radius));
	}
	
	public void removeWarpedSpace(L2Character creature)
	{
		_warpedSpace.remove(creature);
	}
	
	public boolean checkForWarpedSpace(AbstractNodeLoc start, AbstractNodeLoc end, Instance instance)
	{
		return checkForWarpedSpace(new Location(start.getX(), start.getY(), start.getZ()), new Location(end.getX(), end.getY(), end.getZ()), instance);
	}
	
	public boolean checkForWarpedSpace(Location origin, Location destination, Instance instance)
	{
		if (_warpedSpace != null)
		{
			for (WarpedSpaceHolder holder : _warpedSpace.values())
			{
				final L2Character creature = holder.getCreature();
				if (creature.getInstanceWorld() != instance)
				{
					continue;
				}
				final int radius = creature.getTemplate().getCollisionRadius();
				final boolean originInRange = Util.calculateDistance(creature, origin, false, false) <= (holder.getRange() + radius);
				final boolean destinationInRange = Util.calculateDistance(creature, destination, false, false) <= (holder.getRange() + radius);
				return destinationInRange ? !originInRange : originInRange;
			}
		}
		return false;
	}
	
	public static WarpedSpaceManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final WarpedSpaceManager _instance = new WarpedSpaceManager();
	}
}
