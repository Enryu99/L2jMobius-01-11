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
package com.l2jmobius.gameserver.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.L2Vehicle;

public final class L2WorldRegion
{
	private static final Logger LOGGER = Logger.getLogger(L2WorldRegion.class.getName());
	
	/** Map containing visible objects in this world region. */
	private volatile Map<Integer, L2Object> _visibleObjects = new ConcurrentHashMap<>();
	/** Map containing nearby regions forming this world region's effective area. */
	private L2WorldRegion[] _surroundingRegions;
	private final int _regionX;
	private final int _regionY;
	private boolean _active = false;
	private ScheduledFuture<?> _neighborsTask = null;
	
	public L2WorldRegion(int regionX, int regionY)
	{
		_regionX = regionX;
		_regionY = regionY;
		
		// Default a newly initialized region to inactive, unless always on is specified.
		_active = Config.GRIDS_ALWAYS_ON;
	}
	
	/** Task of AI notification */
	public class NeighborsTask implements Runnable
	{
		private final boolean _isActivating;
		
		public NeighborsTask(boolean isActivating)
		{
			_isActivating = isActivating;
		}
		
		@Override
		public void run()
		{
			forEachSurroundingRegion(w ->
			{
				if (_isActivating || w.areNeighborsEmpty())
				{
					w.setActive(_isActivating);
				}
				return true;
			});
		}
	}
	
	private void switchAI(boolean isOn)
	{
		if (_visibleObjects.isEmpty())
		{
			return;
		}
		
		int c = 0;
		if (!isOn)
		{
			for (L2Object o : _visibleObjects.values())
			{
				if (o.isAttackable())
				{
					c++;
					final L2Attackable mob = (L2Attackable) o;
					
					// Set target to null and cancel attack or cast.
					mob.setTarget(null);
					
					// Stop movement.
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the L2Character.
					mob.stopAllEffects();
					
					mob.clearAggroList();
					mob.getAttackByList().clear();
					
					// Stop the AI tasks.
					if (mob.hasAI())
					{
						mob.getAI().setIntention(com.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE);
						mob.getAI().stopAITask();
					}
				}
				else if (o instanceof L2Vehicle)
				{
					c++;
				}
			}
			LOGGER.finer(c + " mobs were turned off");
		}
		else
		{
			for (L2Object o : _visibleObjects.values())
			{
				if (o.isAttackable())
				{
					c++;
					// Start HP/MP/CP regeneration task.
					((L2Attackable) o).getStatus().startHpMpRegeneration();
				}
				else if (o instanceof L2Npc)
				{
					((L2Npc) o).startRandomAnimationTask();
				}
			}
			LOGGER.finer(c + " mobs were turned on");
		}
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	public boolean areNeighborsEmpty()
	{
		return forEachSurroundingRegion(w -> !(w.isActive() && w.getVisibleObjects().values().stream().anyMatch(L2Object::isPlayable)));
	}
	
	/**
	 * this function turns this region's AI and geodata on or off
	 * @param value
	 */
	public void setActive(boolean value)
	{
		if (_active == value)
		{
			return;
		}
		
		_active = value;
		
		// Turn the AI on or off to match the region's activation.
		switchAI(value);
		
		LOGGER.finer((value ? "Starting" : "Stopping") + " Grid " + this);
	}
	
	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		// First set self to active and do self-tasks...
		setActive(true);
		
		// If the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// Then, set a timer to activate the neighbors.
			_neighborsTask = ThreadPool.schedule(new NeighborsTask(true), 1000 * Config.GRID_NEIGHBOR_TURNON_TIME);
		}
	}
	
	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// If the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// Start a timer to "suggest" a deactivate to self and neighbors.
			// Suggest means: first check if a neighbor has L2PcInstances in it. If not, deactivate.
			_neighborsTask = ThreadPool.schedule(new NeighborsTask(false), 1000 * Config.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}
	
	/**
	 * Add the L2Object in the L2ObjectHashSet(L2Object) _visibleObjects containing L2Object visible in this L2WorldRegion <BR>
	 * If L2Object is a L2PcInstance, Add the L2PcInstance in the L2ObjectHashSet(L2PcInstance) _allPlayable containing L2PcInstance of all player in game in this L2WorldRegion <BR>
	 * @param object
	 */
	public void addVisibleObject(L2Object object)
	{
		if (object == null)
		{
			return;
		}
		
		_visibleObjects.put(object.getObjectId(), object);
		
		if (object.isPlayable())
		{
			// If this is the first player to enter the region, activate self and neighbors.
			if (!_active && (!Config.GRIDS_ALWAYS_ON))
			{
				startActivation();
			}
		}
	}
	
	/**
	 * Remove the L2Object from the L2ObjectHashSet(L2Object) _visibleObjects in this L2WorldRegion. If L2Object is a L2PcInstance, remove it from the L2ObjectHashSet(L2PcInstance) _allPlayable of this L2WorldRegion <BR>
	 * @param object
	 */
	public void removeVisibleObject(L2Object object)
	{
		if (object == null)
		{
			return;
		}
		
		if (_visibleObjects.isEmpty())
		{
			return;
		}
		_visibleObjects.remove(object.getObjectId());
		
		if (object.isPlayable())
		{
			if (areNeighborsEmpty() && !Config.GRIDS_ALWAYS_ON)
			{
				startDeactivation();
			}
		}
	}
	
	public Map<Integer, L2Object> getVisibleObjects()
	{
		return _visibleObjects;
	}
	
	public boolean forEachSurroundingRegion(Predicate<L2WorldRegion> p)
	{
		for (L2WorldRegion worldRegion : _surroundingRegions)
		{
			if (!p.test(worldRegion))
			{
				return false;
			}
		}
		return true;
	}
	
	public void setSurroundingRegions(L2WorldRegion[] regions)
	{
		_surroundingRegions = regions;
	}
	
	public L2WorldRegion[] getSurroundingRegions()
	{
		return _surroundingRegions;
	}
	
	public boolean isSurroundingRegion(L2WorldRegion region)
	{
		return (region != null) && (_regionX >= (region.getRegionX() - 1)) && (_regionX <= (region.getRegionX() + 1)) && (_regionY >= (region.getRegionY() - 1)) && (_regionY <= (region.getRegionY() + 1));
	}
	
	public int getRegionX()
	{
		return _regionX;
	}
	
	public int getRegionY()
	{
		return _regionY;
	}
	
	@Override
	public String toString()
	{
		return "(" + _regionX + ", " + _regionY + ")";
	}
}
