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
package com.l2jmobius.gameserver.model.spawn;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.datatables.sql.TerritoryTable;
import com.l2jmobius.gameserver.idfactory.IdFactory;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class manages the spawn and respawn of a group of L2NpcInstance that are in the same are and have the same type. <B><U> Concept</U> :</B><BR>
 * <BR>
 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position. The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR>
 * <BR>
 * @author Nightmare
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2Spawn
{
	protected static final Logger LOGGER = Logger.getLogger(L2Spawn.class.getName());
	
	/**
	 * The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...)
	 */
	private L2NpcTemplate _template;
	
	/** The Identifier of this spawn in the spawn table */
	private int _id;
	
	// private String _location = DEFAULT_LOCATION;
	
	/** The identifier of the location area where L2NpcInstance can be spawned */
	private int _location;
	
	/** The maximum number of L2NpcInstance that can manage this L2Spawn */
	private int _maximumCount;
	
	/** The current number of L2NpcInstance managed by this L2Spawn */
	private int _currentCount;
	
	/** The current number of SpawnTask in progress or stand by of this L2Spawn */
	protected int _scheduledCount;
	
	/** The X position of the spawn point */
	private int _locX;
	
	/** The Y position of the spawn point */
	private int _locY;
	
	/** The Z position of the spawn point */
	private int _locZ;
	
	/** The heading of L2NpcInstance when they are spawned */
	private int _heading;
	
	/** The delay between a L2NpcInstance remove and its re-spawn */
	private int _respawnDelay;
	
	/** Minimum delay RaidBoss */
	private int _respawnMinDelay;
	
	/** Maximum delay RaidBoss */
	private int _respawnMaxDelay;
	
	/** The generic constructor of L2NpcInstance managed by this L2Spawn */
	private Constructor<?> _constructor;
	
	/** If True a L2NpcInstance is respawned each time that another is killed */
	private boolean _doRespawn;
	
	private int _instanceId = 0;
	
	private L2NpcInstance _lastSpawn;
	private static List<SpawnListener> _spawnListeners = new ArrayList<>();
	
	/** The task launching the function doSpawn() */
	class SpawnTask implements Runnable
	{
		public SpawnTask()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				doSpawn();
			}
			catch (Exception e)
			{
				LOGGER.warning(e.getMessage());
			}
			
			_scheduledCount--;
		}
	}
	
	/**
	 * Constructor of L2Spawn.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...). All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn. Each template is loaded once in the server cache memory (reduce memory use). When a new instance of L2Spawn is
	 * created, server just create a link between the instance and the template. This link is stored in <B>_template</B><BR>
	 * <BR>
	 * Each L2NpcInstance is linked to a L2Spawn that manages its spawn and respawn (delay, location...). This link is stored in <B>_spawn</B> of the L2NpcInstance<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _template of the L2Spawn</li>
	 * <li>Calculate the implementationName used to generate the generic constructor of L2NpcInstance managed by this L2Spawn</li>
	 * <li>Create the generic constructor of L2NpcInstance managed by this L2Spawn</li><BR>
	 * <BR>
	 * @param mobTemplate The L2NpcTemplate to link to this L2Spawn
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 */
	public L2Spawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;
		
		if (_template == null)
		{
			return;
		}
		
		// The Name of the L2NpcInstance type managed by this L2Spawn
		String implementationName = _template.type; // implementing class name
		
		if (mobTemplate.npcId == 30995)
		{
			implementationName = "L2RaceManager";
		}
		
		// if (mobTemplate.npcId == 8050)
		
		if ((mobTemplate.npcId >= 31046) && (mobTemplate.npcId <= 31053))
		{
			implementationName = "L2SymbolMaker";
		}
		
		// Create the generic constructor of L2NpcInstance managed by this L2Spawn
		final Class<?>[] parameters =
		{
			int.class,
			L2NpcTemplate.class
		};
		_constructor = Class.forName("com.l2jmobius.gameserver.model.actor.instance." + implementationName + "Instance").getConstructor(parameters);
	}
	
	/**
	 * @return the maximum number of L2NpcInstance that this L2Spawn can manage.
	 */
	public int getAmount()
	{
		return _maximumCount;
	}
	
	/**
	 * @return the Identifier of this L2spawn (used as key in the SpawnTable).
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the Identifier of the location area where L2NpcInstance can be spawned.
	 */
	public int getLocation()
	{
		return _location;
	}
	
	/**
	 * @return the X position of the spawn point.
	 */
	public int getX()
	{
		return _locX;
	}
	
	/**
	 * @return the Y position of the spawn point.
	 */
	public int getY()
	{
		return _locY;
	}
	
	/**
	 * @return the Z position of the spawn point.
	 */
	public int getZ()
	{
		return _locZ;
	}
	
	/**
	 * @return the Identifier of the L2NpcInstance manage by this L2spawn contained in the L2NpcTemplate.
	 */
	public int getNpcId()
	{
		if (_template == null)
		{
			return -1;
		}
		
		return _template.npcId;
	}
	
	/**
	 * @return the heading of L2NpcInstance when they are spawned.
	 */
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * @return the delay between a L2NpcInstance remove and its re-spawn.
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	
	/**
	 * @return Min RaidBoss Spawn delay.
	 */
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	/**
	 * @return Max RaidBoss Spawn delay.
	 */
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	/**
	 * Set the maximum number of L2NpcInstance that this L2Spawn can manage.
	 * @param amount
	 */
	public void setAmount(int amount)
	{
		_maximumCount = amount;
	}
	
	/**
	 * Set the Identifier of this L2spawn (used as key in the SpawnTable).
	 * @param id
	 */
	public void setId(int id)
	{
		_id = id;
	}
	
	/**
	 * Set the Identifier of the location area where L2NpcInstance can be spawned.
	 * @param location
	 */
	public void setLocation(int location)
	{
		_location = location;
	}
	
	/**
	 * Set Minimum Respawn Delay.
	 * @param date
	 */
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}
	
	/**
	 * Set Maximum Respawn Delay.
	 * @param date
	 */
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	
	/**
	 * Set the X position of the spawn point.
	 * @param locx
	 */
	public void setX(int locx)
	{
		_locX = locx;
	}
	
	/**
	 * Set the Y position of the spawn point.
	 * @param locy
	 */
	public void setY(int locy)
	{
		_locY = locy;
	}
	
	/**
	 * Set the Z position of the spawn point.
	 * @param locz
	 */
	public void setZ(int locz)
	{
		_locZ = locz;
	}
	
	/**
	 * Set the heading of L2NpcInstance when they are spawned.
	 * @param heading
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	public void setLoc(int locx, int locy, int locz, int heading)
	{
		_locX = locx;
		_locY = locy;
		_locZ = locz;
		_heading = heading;
	}
	
	/**
	 * Kidzor Set the spawn as custom.
	 * @param custom
	 */
	public void setCustom(boolean custom)
	{
		_customSpawn = custom;
	}
	
	/**
	 * Kidzor Return type of spawn.
	 * @return
	 */
	public boolean isCustom()
	{
		return _customSpawn;
	}
	
	/** If true then spawn is custom */
	private boolean _customSpawn;
	
	private boolean _customBossInstance = false;
	
	/**
	 * @return the _customBossInstance
	 */
	public boolean is_customBossInstance()
	{
		return _customBossInstance;
	}
	
	/**
	 * @param customBossInstance the _customBossInstance to set
	 */
	public void set_customBossInstance(boolean customBossInstance)
	{
		_customBossInstance = customBossInstance;
	}
	
	/**
	 * Decrease the current number of L2NpcInstance of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Decrease the current number of L2NpcInstance of this L2Spawn</li>
	 * <li>Check if respawn is possible to prevent multiple respawning caused by lag</li>
	 * <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn</li>
	 * <li>Create a new SpawnTask to launch after the respawn Delay</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR>
	 * <BR>
	 * @param oldNpc
	 */
	public void decreaseCount(/* int npcId */final L2NpcInstance oldNpc)
	{
		// Decrease the current number of L2NpcInstance of this L2Spawn
		_currentCount--;
		
		// Check if respawn is possible to prevent multiple respawning caused by lag
		if (_doRespawn && ((_scheduledCount + _currentCount) < _maximumCount))
		{
			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;
			
			// Create a new SpawnTask to launch after the respawn Delay
			ThreadPool.schedule(new SpawnTask(), _respawnDelay);
		}
	}
	
	/**
	 * Create the initial spawning and set _doRespawn to True.<BR>
	 * <BR>
	 * @return The number of L2NpcInstance that were spawned
	 */
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		_doRespawn = true;
		
		return _currentCount;
	}
	
	/**
	 * Create a L2NpcInstance in this L2Spawn.
	 * @return
	 */
	public L2NpcInstance spawnOne()
	{
		return doSpawn();
	}
	
	/**
	 * Set _doRespawn to False to stop respawn in this L2Spawn.
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	/**
	 * Set _doRespawn to True to start or restart respawn in this L2Spawn.
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	/**
	 * Create the L2NpcInstance, add it to the world and launch its OnSpawn action.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position. The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR>
	 * <BR>
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR>
	 * <BR>
	 * <li>Get L2NpcInstance Init parameters and its generate an Identifier</li>
	 * <li>Call the constructor of the L2NpcInstance</li>
	 * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn</li>
	 * <li>Set the position of the L2NpcInstance</li>
	 * <li>Set the HP and MP of the L2NpcInstance to the max</li>
	 * <li>Set the heading of the L2NpcInstance (random heading if not defined : value=-1)</li>
	 * <li>Link the L2NpcInstance to this L2Spawn</li>
	 * <li>Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world</li>
	 * <li>Lauch the action OnSpawn fo the L2NpcInstance</li><BR>
	 * <BR>
	 * <li>Increase the current number of L2NpcInstance managed by this L2Spawn</li><BR>
	 * <BR>
	 * @return
	 */
	public L2NpcInstance doSpawn()
	{
		L2NpcInstance mob = null;
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion spawn
			if (_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion"))
			{
				_currentCount++;
				
				return mob;
			}
			
			// Get L2NpcInstance Init parameters and its generate an Identifier
			final Object[] parameters =
			{
				IdFactory.getInstance().getNextId(),
				_template
			};
			
			// Call the constructor of the L2NpcInstance
			// (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance,
			// L2FeedableBeastInstance, L2TamedBeastInstance, L2FolkInstance)
			final Object tmp = _constructor.newInstance(parameters);
			
			// Must be done before object is spawned into visible world
			((L2Object) tmp).setInstanceId(_instanceId);
			
			// Check if the Instance is a L2NpcInstance
			if (!(tmp instanceof L2NpcInstance))
			{
				return mob;
			}
			
			mob = (L2NpcInstance) tmp;
			
			return initializeNpcInstance(mob);
		}
		catch (Exception e)
		{
			LOGGER.warning("NPC " + _template.npcId + " class not found " + e);
		}
		return mob;
	}
	
	/**
	 * @param mob
	 * @return
	 */
	private L2NpcInstance initializeNpcInstance(L2NpcInstance mob)
	{
		int newlocx, newlocy, newlocz;
		
		// If Locx=0 and Locy=0, the L2NpcInstance must be spawned in an area defined by location
		if ((getX() == 0) && (getY() == 0))
		{
			if (getLocation() == 0)
			{
				return mob;
			}
			
			// Calculate the random position in the location area
			final int p[] = TerritoryTable.getInstance().getRandomPoint(getLocation());
			
			// Set the calculated position of the L2NpcInstance
			newlocx = p[0];
			newlocy = p[1];
			newlocz = p[3];
		}
		else
		{
			// The L2NpcInstance is spawned at the exact position (Lox, Locy, Locz)
			newlocx = getX();
			newlocy = getY();
			newlocz = getZ();
		}
		
		if (mob != null)
		{
			mob.stopAllEffects();
			
			// Set the HP and MP of the L2NpcInstance to the max
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			
			// Set the heading of the L2NpcInstance (random heading if not defined)
			if (getHeading() == -1)
			{
				mob.setHeading(Rnd.nextInt(61794));
			}
			else
			{
				mob.setHeading(getHeading());
			}
			
			// Reset decay info
			mob.setDecayed(false);
			
			// Link the L2NpcInstance to this L2Spawn
			mob.setSpawn(this);
			
			// Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
			mob.spawnMe(newlocx, newlocy, newlocz);
			
			notifyNpcSpawned(mob);
			
			_lastSpawn = mob;
			
			if (Config.DEBUG)
			{
				LOGGER.info("spawned Mob ID: " + _template.npcId + " ,at: " + mob.getX() + " x, " + mob.getY() + " y, " + mob.getZ() + " z");
			}
			
			for (Quest quest : mob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN))
			{
				quest.notifySpawn(mob);
			}
			
			// Increase the current number of L2NpcInstance managed by this L2Spawn
			_currentCount++;
		}
		return mob;
	}
	
	public static void addSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}
	
	public static void removeSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}
	
	public static void notifyNpcSpawned(L2NpcInstance npc)
	{
		synchronized (_spawnListeners)
		{
			for (SpawnListener listener : _spawnListeners)
			{
				listener.npcSpawned(npc);
			}
		}
	}
	
	/**
	 * @param i delay in seconds
	 */
	public void setRespawnDelay(int i)
	{
		if (i < 0)
		{
			LOGGER.warning("respawn delay is negative for spawnId:" + _id);
		}
		
		if (i < 10)
		{
			i = 10;
		}
		
		_respawnDelay = i * 1000;
	}
	
	public L2NpcInstance getLastSpawn()
	{
		return _lastSpawn;
	}
	
	public L2NpcTemplate getTemplate()
	{
		return _template;
	}
	
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
}
