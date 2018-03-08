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
package com.l2jmobius.gameserver.model.holders;

import com.l2jmobius.gameserver.model.actor.instance.L2DoorInstance;

/**
 * @author UnAfraid
 */
public class DoorRequestHolder
{
	private final L2DoorInstance _target;
	
	public DoorRequestHolder(L2DoorInstance door)
	{
		_target = door;
	}
	
	public L2DoorInstance getDoor()
	{
		return _target;
	}
}
