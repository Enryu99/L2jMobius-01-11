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
package com.l2jmobius.gameserver.model.extender;

import com.l2jmobius.gameserver.model.WorldObject;

/**
 * @author Azagthtot BaseExtender
 */
public class BaseExtender
{
	public enum EventType
	{
		LOAD("load"), // null
		STORE("store"), // null
		CAST("cast"), // Skill , Creature, Creature[]
		ATTACK("attack"), // Creature
		CRAFT("craft"),
		ENCHANT("enchant"),
		SPAWN("spawn"), // null
		DELETE("delete"), // null
		SETOWNER("setwoner"), // int, String
		DROP("drop"), // null
		DIE("die"), // Creature
		REVIVE("revive"), // null
		SETINTENTION("setintention"); // CtrlIntention
		public final String name;
		
		EventType(String name)
		{
			this.name = name;
		}
	}
	
	/**
	 * @param object as WorldObject<br>
	 * @return as boolean<br>
	 */
	public static boolean canCreateFor(WorldObject object)
	{
		return true;
	}
	
	protected WorldObject _owner;
	private BaseExtender _next = null;
	
	/**
	 * @param owner - WorldObject
	 */
	public BaseExtender(WorldObject owner)
	{
		_owner = owner;
	}
	
	/**
	 * @return as Object
	 */
	public WorldObject getOwner()
	{
		return _owner;
	}
	
	/**
	 * onEvent - super.onEvent(event,params);<BR>
	 * <BR>
	 * @param event as String<br>
	 * @param params as Object[]<br>
	 * @return as Object
	 */
	public Object onEvent(String event, Object... params)
	{
		if (_next == null)
		{
			return null;
		}
		return _next.onEvent(event, params);
	}
	
	/**
	 * @param simpleClassName as String<br>
	 * @return as BaseExtender - null
	 */
	public BaseExtender getExtender(String simpleClassName)
	{
		if (getClass().getSimpleName().compareTo(simpleClassName) == 0)
		{
			return this;
		}
		else if (_next != null)
		{
			return _next.getExtender(simpleClassName);
		}
		else
		{
			return null;
		}
	}
	
	public void removeExtender(BaseExtender ext)
	{
		if (_next != null)
		{
			if (_next == ext)
			{
				_next = _next._next;
			}
			else
			{
				_next.removeExtender(ext);
			}
		}
	}
	
	public BaseExtender getNextExtender()
	{
		return _next;
	}
	
	/**
	 * @param newExtender as BaseExtender
	 */
	public void addExtender(BaseExtender newExtender)
	{
		if (_next == null)
		{
			_next = newExtender;
		}
		else
		{
			_next.addExtender(newExtender);
		}
	}
}
