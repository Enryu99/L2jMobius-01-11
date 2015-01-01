/*
 * Copyright (C) 2004-2014 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.pathfinding.utils;

import com.l2jserver.gameserver.pathfinding.AbstractNode;

/**
 * @author -Nemesiss-
 */
public class FastNodeList
{
	private final AbstractNode[] _list;
	private int _size;
	
	public FastNodeList(int size)
	{
		_list = new AbstractNode[size];
	}
	
	public void add(AbstractNode n)
	{
		_list[_size++] = n;
	}
	
	public boolean contains(AbstractNode n)
	{
		for (int i = 0; i < _size; i++)
		{
			if (_list[i].equals(n))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean containsRev(AbstractNode n)
	{
		for (int i = _size - 1; i >= 0; i--)
		{
			if (_list[i].equals(n))
			{
				return true;
			}
		}
		return false;
	}
}
