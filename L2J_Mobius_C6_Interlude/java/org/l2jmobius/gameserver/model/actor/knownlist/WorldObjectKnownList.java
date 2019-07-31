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
package org.l2jmobius.gameserver.model.actor.knownlist;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.BoatInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.util.Util;

public class WorldObjectKnownList
{
	private final WorldObject _activeObject;
	private Map<Integer, WorldObject> _knownObjects;
	
	public WorldObjectKnownList(WorldObject activeObject)
	{
		_activeObject = activeObject;
	}
	
	public boolean addKnownObject(WorldObject object)
	{
		return addKnownObject(object, null);
	}
	
	public boolean addKnownObject(WorldObject object, Creature dropper)
	{
		if ((object == null) || (object == _activeObject))
		{
			return false;
		}
		
		// Check if already know object
		if (knowsObject(object))
		{
			if (!object.isVisible())
			{
				removeKnownObject(object);
			}
			return false;
		}
		
		// Check if object is not inside distance to watch object
		if (!Util.checkIfInRange(getDistanceToWatchObject(object), _activeObject, object, true))
		{
			return false;
		}
		
		return getKnownObjects().put(object.getObjectId(), object) == null;
	}
	
	public boolean knowsObject(WorldObject object)
	{
		if (object == null)
		{
			return false;
		}
		
		return (_activeObject == object) || getKnownObjects().containsKey(object.getObjectId());
	}
	
	/** Remove all WorldObject from _knownObjects */
	public void removeAllKnownObjects()
	{
		getKnownObjects().clear();
	}
	
	public boolean removeKnownObject(WorldObject object)
	{
		if (object == null)
		{
			return false;
		}
		
		return getKnownObjects().remove(object.getObjectId()) != null;
	}
	
	/**
	 * Update the _knownObject and _knowPlayers of the Creature and of its already known WorldObject.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove invisible and too far WorldObject from _knowObject and if necessary from _knownPlayers of the Creature</li>
	 * <li>Add visible WorldObject near the Creature to _knowObject and if necessary to _knownPlayers of the Creature</li>
	 * <li>Add Creature to _knowObject and if necessary to _knownPlayers of WorldObject alreday known by the Creature</li><BR>
	 * <BR>
	 */
	public synchronized void updateKnownObjects()
	{
		// Only bother updating knownobjects for Creature; don't for WorldObject
		if (_activeObject instanceof Creature)
		{
			findCloseObjects();
			forgetObjects();
		}
	}
	
	private final void findCloseObjects()
	{
		final boolean isActiveObjectPlayable = _activeObject instanceof Playable;
		
		if (isActiveObjectPlayable)
		{
			Collection<WorldObject> objects = World.getInstance().getVisibleObjects(getActiveObject());
			
			if (objects == null)
			{
				return;
			}
			
			// Go through all visible WorldObject near the Creature
			for (WorldObject object : objects)
			{
				if (object == null)
				{
					continue;
				}
				
				// Try to add object to active object's known objects
				// PlayableInstance sees everything
				addKnownObject(object);
				
				// Try to add active object to object's known objects
				// Only if object is a Creature and active object is a PlayableInstance
				if (object instanceof Creature)
				{
					object.getKnownList().addKnownObject(_activeObject);
				}
			}
		}
		else
		{
			Collection<Playable> playables = World.getInstance().getVisiblePlayable(getActiveObject());
			
			if (playables == null)
			{
				return;
			}
			
			// Go through all visible WorldObject near the Creature
			for (WorldObject playable : playables)
			{
				if (playable == null)
				{
					continue;
				}
				
				// Try to add object to active object's known objects
				// Creature only needs to see visible PlayerInstance and PlayableInstance, when moving. Other l2characters are currently only known from initial spawn area.
				// Possibly look into getDistanceToForgetObject values before modifying this approach...
				addKnownObject(playable);
			}
		}
	}
	
	public void forgetObjects()
	{
		// Go through knownObjects
		Collection<WorldObject> knownObjects = getKnownObjects().values();
		
		if ((knownObjects == null) || (knownObjects.size() == 0))
		{
			return;
		}
		
		for (WorldObject object : knownObjects)
		{
			if (object == null)
			{
				continue;
			}
			
			// Remove all invisible object
			// Remove all too far object
			if (!object.isVisible() || !Util.checkIfInRange(getDistanceToForgetObject(object), _activeObject, object, true))
			{
				if ((object instanceof BoatInstance) && (_activeObject instanceof PlayerInstance))
				{
					if (((BoatInstance) object).getVehicleDeparture() == null)
					{
						//
					}
					else if (((PlayerInstance) _activeObject).isInBoat())
					{
						if (((PlayerInstance) _activeObject).getBoat() == object)
						{
							//
						}
						else
						{
							removeKnownObject(object);
						}
					}
					else
					{
						removeKnownObject(object);
					}
				}
				else
				{
					removeKnownObject(object);
				}
			}
		}
	}
	
	public WorldObject getActiveObject()
	{
		return _activeObject;
	}
	
	public int getDistanceToForgetObject(WorldObject object)
	{
		return 0;
	}
	
	public int getDistanceToWatchObject(WorldObject object)
	{
		return 0;
	}
	
	/**
	 * @return the _knownObjects containing all WorldObject known by the Creature.
	 */
	public Map<Integer, WorldObject> getKnownObjects()
	{
		if (_knownObjects == null)
		{
			_knownObjects = new ConcurrentHashMap<>();
		}
		
		return _knownObjects;
	}
	
	public static class KnownListAsynchronousUpdateTask implements Runnable
	{
		private final WorldObject _obj;
		
		public KnownListAsynchronousUpdateTask(WorldObject obj)
		{
			_obj = obj;
		}
		
		@Override
		public void run()
		{
			if (_obj != null)
			{
				_obj.getKnownList().updateKnownObjects();
			}
		}
	}
}
