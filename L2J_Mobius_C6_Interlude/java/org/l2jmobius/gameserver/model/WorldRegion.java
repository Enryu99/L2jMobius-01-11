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
package org.l2jmobius.gameserver.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.object.L2ObjectSet;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.FortSiegeGuardAI;
import org.l2jmobius.gameserver.ai.SiegeGuardAI;
import org.l2jmobius.gameserver.datatables.sql.SpawnTable;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.spawn.Spawn;
import org.l2jmobius.gameserver.model.zone.ZoneManager;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.PeaceZone;

public final class WorldRegion
{
	private static Logger LOGGER = Logger.getLogger(WorldRegion.class.getName());
	
	private final L2ObjectSet<Playable> _allPlayable;
	private final L2ObjectSet<WorldObject> _visibleObjects;
	private final List<WorldRegion> _surroundingRegions;
	private final int _tileX;
	private final int _tileY;
	private Boolean _active = false;
	private ScheduledFuture<?> _neighborsTask = null;
	private ZoneManager _zoneManager;
	
	public WorldRegion(int pTileX, int pTileY)
	{
		_allPlayable = L2ObjectSet.createL2PlayerSet();
		_visibleObjects = L2ObjectSet.createL2ObjectSet();
		_surroundingRegions = new ArrayList<>();
		
		_tileX = pTileX;
		_tileY = pTileY;
		
		// default a newly initialized region to inactive, unless always on is specified
		if (Config.GRIDS_ALWAYS_ON)
		{
			_active = true;
		}
		else
		{
			_active = false;
		}
	}
	
	public void addZone(ZoneType zone)
	{
		if (_zoneManager == null)
		{
			_zoneManager = new ZoneManager();
		}
		_zoneManager.registerNewZone(zone);
	}
	
	public void removeZone(ZoneType zone)
	{
		if (_zoneManager == null)
		{
			return;
		}
		
		_zoneManager.unregisterZone(zone);
	}
	
	public void revalidateZones(Creature creature)
	{
		if (_zoneManager == null)
		{
			return;
		}
		
		if (_zoneManager != null)
		{
			_zoneManager.revalidateZones(creature);
		}
	}
	
	public void removeFromZones(Creature creature)
	{
		if (_zoneManager == null)
		{
			return;
		}
		
		if (_zoneManager != null)
		{
			_zoneManager.removeCharacter(creature);
		}
	}
	
	public void onDeath(Creature creature)
	{
		if (_zoneManager == null)
		{
			return;
		}
		
		if (_zoneManager != null)
		{
			_zoneManager.onDeath(creature);
		}
	}
	
	public void onRevive(Creature creature)
	{
		if (_zoneManager == null)
		{
			return;
		}
		
		if (_zoneManager != null)
		{
			_zoneManager.onRevive(creature);
		}
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
				for (WorldRegion neighbor : getSurroundingRegions())
				{
					neighbor.setActive(true);
				}
			}
			else
			{
				if (areNeighborsEmpty())
				{
					setActive(false);
				}
				
				// check and deactivate
				for (WorldRegion neighbor : getSurroundingRegions())
				{
					if (neighbor.areNeighborsEmpty())
					{
						neighbor.setActive(false);
					}
				}
			}
		}
	}
	
	private void switchAI(Boolean isOn)
	{
		if (!isOn)
		{
			for (WorldObject o : _visibleObjects)
			{
				if (o instanceof Attackable)
				{
					final Attackable mob = (Attackable) o;
					
					// Set target to null and cancel Attack or Cast
					mob.setTarget(null);
					
					// Stop movement
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the Creature
					mob.stopAllEffects();
					
					mob.clearAggroList();
					mob.getKnownList().removeAllKnownObjects();
					
					if (mob.getAI() != null)
					{
						mob.getAI().setIntention(org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE);
						
						// stop the ai tasks
						if (mob.getAI() instanceof AttackableAI)
						{
							((AttackableAI) mob.getAI()).stopAITask();
						}
						else if (mob.getAI() instanceof FortSiegeGuardAI)
						{
							((FortSiegeGuardAI) mob.getAI()).stopAITask();
						}
						else if (mob.getAI() instanceof SiegeGuardAI)
						{
							((SiegeGuardAI) mob.getAI()).stopAITask();
						}
					}
				}
			}
		}
		else
		{
			for (WorldObject o : _visibleObjects)
			{
				if (o instanceof Attackable)
				{
					// Start HP/MP/CP Regeneration task
					((Attackable) o).getStatus().startHpMpRegeneration();
				}
				else if (o instanceof NpcInstance)
				{
					// Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it
					// Monsterinstance/Attackable socials are handled by AI (TODO: check the instances)
					((NpcInstance) o).startRandomAnimationTask();
				}
			}
		}
	}
	
	public Boolean isActive()
	{
		return _active;
	}
	
	// check if all 9 neighbors (including self) are inactive or active but with no players.
	// returns true if the above condition is met.
	public Boolean areNeighborsEmpty()
	{
		// if this region is occupied, return false.
		if (_active && (_allPlayable.size() > 0))
		{
			return false;
		}
		
		// if any one of the neighbors is occupied, return false
		for (WorldRegion neighbor : _surroundingRegions)
		{
			if (neighbor.isActive() && (neighbor._allPlayable.size() > 0))
			{
				return false;
			}
		}
		
		// in all other cases, return true.
		return true;
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
	}
	
	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		// first set self to active and do self-tasks...
		setActive(true);
		
		// if the timer to deactivate neighbors is running, cancel it.
		if (_neighborsTask != null)
		{
			_neighborsTask.cancel(true);
			_neighborsTask = null;
		}
		
		// then, set a timer to activate the neighbors
		_neighborsTask = ThreadPool.schedule(new NeighborsTask(true), 1000 * Config.GRID_NEIGHBOR_TURNON_TIME);
	}
	
	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// if the timer to activate neighbors is running, cancel it.
		if (_neighborsTask != null)
		{
			_neighborsTask.cancel(true);
			_neighborsTask = null;
		}
		
		// start a timer to "suggest" a deactivate to self and neighbors.
		// suggest means: first check if a neighbor has PlayerInstances in it. If not, deactivate.
		_neighborsTask = ThreadPool.schedule(new NeighborsTask(false), 1000 * Config.GRID_NEIGHBOR_TURNOFF_TIME);
	}
	
	/**
	 * Add the WorldObject in the WorldObjectHashSet(WorldObject) _visibleObjects containing WorldObject visible in this WorldRegion <BR>
	 * If WorldObject is a PlayerInstance, Add the PlayerInstance in the WorldObjectHashSet(PlayerInstance) _allPlayable containing PlayerInstance of all player in game in this WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this
	 * @param object
	 */
	public void addVisibleObject(WorldObject object)
	{
		if (Config.ASSERT)
		{
			assert object.getWorldRegion() == this;
		}
		
		if (object == null)
		{
			return;
		}
		
		_visibleObjects.put(object);
		
		if (object instanceof Playable)
		{
			_allPlayable.put((Playable) object);
			
			// if this is the first player to enter the region, activate self & neighbors
			if ((_allPlayable.size() == 1) && !Config.GRIDS_ALWAYS_ON)
			{
				startActivation();
			}
		}
	}
	
	/**
	 * Remove the WorldObject from the WorldObjectHashSet(WorldObject) _visibleObjects in this WorldRegion <BR>
	 * <BR>
	 * If WorldObject is a PlayerInstance, remove it from the WorldObjectHashSet(PlayerInstance) _allPlayable of this WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this || object.getCurrentWorldRegion() == null
	 * @param object
	 */
	public void removeVisibleObject(WorldObject object)
	{
		if (Config.ASSERT)
		{
			assert (object.getWorldRegion() == this) || (object.getWorldRegion() == null);
		}
		
		if (object == null)
		{
			return;
		}
		
		_visibleObjects.remove(object);
		
		if (object instanceof Playable)
		{
			_allPlayable.remove((Playable) object);
			
			if ((_allPlayable.size() == 0) && !Config.GRIDS_ALWAYS_ON)
			{
				startDeactivation();
			}
		}
	}
	
	public void addSurroundingRegion(WorldRegion region)
	{
		_surroundingRegions.add(region);
	}
	
	/**
	 * @return the list _surroundingRegions containing all WorldRegion around the current WorldRegion
	 */
	public List<WorldRegion> getSurroundingRegions()
	{
		return _surroundingRegions;
	}
	
	public Iterator<Playable> iterateAllPlayers()
	{
		return _allPlayable.iterator();
	}
	
	public L2ObjectSet<WorldObject> getVisibleObjects()
	{
		return _visibleObjects;
	}
	
	public String getName()
	{
		return "(" + _tileX + ", " + _tileY + ")";
	}
	
	/**
	 * Deleted all spawns in the world.
	 */
	public synchronized void deleteVisibleNpcSpawns()
	{
		LOGGER.info("Deleting all visible NPCs in Region: " + getName());
		for (WorldObject obj : _visibleObjects)
		{
			if (obj instanceof NpcInstance)
			{
				NpcInstance target = (NpcInstance) obj;
				target.deleteMe();
				Spawn spawn = target.getSpawn();
				
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
				}
				
				LOGGER.info("Removed NPC " + target.getObjectId());
			}
		}
	}
	
	/**
	 * @param skill
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean checkEffectRangeInsidePeaceZone(Skill skill, int x, int y, int z)
	{
		if (_zoneManager != null)
		{
			final int range = skill.getEffectRange();
			final int up = y + range;
			final int down = y - range;
			final int left = x + range;
			final int right = x - range;
			
			for (ZoneType e : _zoneManager.getZones())
			{
				if (e instanceof PeaceZone)
				{
					if (e.isInsideZone(x, up, z))
					{
						return false;
					}
					
					if (e.isInsideZone(x, down, z))
					{
						return false;
					}
					
					if (e.isInsideZone(left, y, z))
					{
						return false;
					}
					
					if (e.isInsideZone(right, y, z))
					{
						return false;
					}
					
					if (e.isInsideZone(x, y, z))
					{
						return false;
					}
				}
			}
			return true;
		}
		return true;
	}
}
