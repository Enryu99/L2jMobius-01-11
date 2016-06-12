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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.datatables.SpawnTable;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.L2Vehicle;

public final class L2WorldRegion
{
	private static final Logger _log = Logger.getLogger(L2WorldRegion.class.getName());
	
	/** Map containing visible objects in this world region. */
	private volatile Map<Integer, L2Object> _visibleObjects;
	private final int _regionX;
	private final int _regionY;
	private final int _regionZ;
	private boolean _active = false;
	private ScheduledFuture<?> _neighborsTask = null;
	
	public L2WorldRegion(int regionX, int regionY, int regionZ)
	{
		_regionX = regionX;
		_regionY = regionY;
		_regionZ = regionZ;
		
		// default a newly initialized region to inactive, unless always on is specified
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
			if (_isActivating)
			{
				// for each neighbor, if it's not active, activate.
				forEachSurroundingRegion(w ->
				{
					w.setActive(true);
					return true;
				});
			}
			else
			{
				if (areNeighborsEmpty())
				{
					setActive(false);
				}
				
				// check and deactivate
				forEachSurroundingRegion(w ->
				{
					if (w.areNeighborsEmpty())
					{
						w.setActive(false);
					}
					return true;
				});
				
			}
		}
	}
	
	private void switchAI(boolean isOn)
	{
		if (_visibleObjects == null)
		{
			return;
		}
		
		int c = 0;
		if (!isOn)
		{
			final Collection<L2Object> vObj = _visibleObjects.values();
			for (L2Object o : vObj)
			{
				if (o instanceof L2Attackable)
				{
					c++;
					final L2Attackable mob = (L2Attackable) o;
					
					// Set target to null and cancel Attack or Cast
					mob.setTarget(null);
					
					// Stop movement
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the L2Character
					mob.stopAllEffects();
					
					mob.clearAggroList();
					mob.getAttackByList().clear();
					
					// stop the ai tasks
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
			
			_log.finer(c + " mobs were turned off");
		}
		else
		{
			final Collection<L2Object> vObj = _visibleObjects.values();
			
			for (L2Object o : vObj)
			{
				if (o instanceof L2Attackable)
				{
					c++;
					// Start HP/MP/CP Regeneration task
					((L2Attackable) o).getStatus().startHpMpRegeneration();
				}
				else if (o instanceof L2Npc)
				{
					((L2Npc) o).startRandomAnimationTask();
				}
			}
			
			_log.finer(c + " mobs were turned on");
			
		}
		
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	public boolean areNeighborsEmpty()
	{
		return !forEachSurroundingRegion(w ->
		{
			return !(w.isActive() && w.getVisibleObjects().values().stream().anyMatch(L2Object::isPlayable));
		});
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
		
		// turn the AI on or off to match the region's activation.
		switchAI(value);
		
		_log.finer((value ? "Starting" : "Stoping") + " Grid " + getName());
	}
	
	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		// first set self to active and do self-tasks...
		setActive(true);
		
		// if the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// then, set a timer to activate the neighbors
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(true), 1000 * Config.GRID_NEIGHBOR_TURNON_TIME);
		}
	}
	
	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// if the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// start a timer to "suggest" a deactivate to self and neighbors.
			// suggest means: first check if a neighbor has L2PcInstances in it. If not, deactivate.
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(false), 1000 * Config.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}
	
	/**
	 * Add the L2Object in the L2ObjectHashSet(L2Object) _visibleObjects containing L2Object visible in this L2WorldRegion <BR>
	 * If L2Object is a L2PcInstance, Add the L2PcInstance in the L2ObjectHashSet(L2PcInstance) _allPlayable containing L2PcInstance of all player in game in this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this
	 * @param object
	 */
	public void addVisibleObject(L2Object object)
	{
		if (object == null)
		{
			return;
		}
		
		assert object.getWorldRegion() == this;
		if (_visibleObjects == null)
		{
			synchronized (object)
			{
				if (_visibleObjects == null)
				{
					_visibleObjects = new ConcurrentHashMap<>();
				}
			}
		}
		_visibleObjects.put(object.getObjectId(), object);
		
		if (object.isPlayable())
		{
			// if this is the first player to enter the region, activate self & neighbors
			if (!isActive() && (!Config.GRIDS_ALWAYS_ON))
			{
				startActivation();
			}
		}
	}
	
	/**
	 * Remove the L2Object from the L2ObjectHashSet(L2Object) _visibleObjects in this L2WorldRegion. If L2Object is a L2PcInstance, remove it from the L2ObjectHashSet(L2PcInstance) _allPlayable of this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this || object.getCurrentWorldRegion() == null
	 * @param object
	 */
	public void removeVisibleObject(L2Object object)
	{
		if (object == null)
		{
			return;
		}
		
		assert (object.getWorldRegion() == this) || (object.getWorldRegion() == null);
		if (_visibleObjects == null)
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
		return _visibleObjects != null ? _visibleObjects : Collections.emptyMap();
	}
	
	public String getName()
	{
		return "(" + _regionX + ", " + _regionY + ", " + _regionZ + ")";
	}
	
	/**
	 * Deleted all spawns in the world.
	 */
	public void deleteVisibleNpcSpawns()
	{
		if (_visibleObjects == null)
		{
			return;
		}
		
		_log.finer("Deleting all visible NPC's in Region: " + getName());
		
		final Collection<L2Object> vNPC = _visibleObjects.values();
		for (L2Object obj : vNPC)
		{
			if (obj instanceof L2Npc)
			{
				final L2Npc target = (L2Npc) obj;
				target.deleteMe();
				final L2Spawn spawn = target.getSpawn();
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
				}
				_log.finest("Removed NPC " + target.getObjectId());
			}
		}
		_log.info("All visible NPC's deleted in Region: " + getName());
	}
	
	public boolean forEachSurroundingRegion(Predicate<L2WorldRegion> p)
	{
		for (int x = _regionX - 1; x <= (_regionX + 1); x++)
		{
			for (int y = _regionY - 1; y <= (_regionY + 1); y++)
			{
				for (int z = _regionZ - 1; z <= (_regionZ + 1); z++)
				{
					if (L2World.validRegion(x, y, z))
					{
						final L2WorldRegion worldRegion = L2World.getInstance().getWorldRegions()[x][y][z];
						if (!p.test(worldRegion))
						{
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	public int getRegionX()
	{
		return _regionX;
	}
	
	public int getRegionY()
	{
		return _regionY;
	}
	
	public int getRegionZ()
	{
		return _regionZ;
	}
	
	public boolean isSurroundingRegion(L2WorldRegion region)
	{
		return (region != null) && (getRegionX() >= (region.getRegionX() - 1)) && (getRegionX() <= (region.getRegionX() + 1)) && (getRegionY() >= (region.getRegionY() - 1)) && (getRegionY() <= (region.getRegionY() + 1)) && (getRegionZ() >= (region.getRegionZ() - 1)) && (getRegionZ() <= (region.getRegionZ() + 1));
	}
}
