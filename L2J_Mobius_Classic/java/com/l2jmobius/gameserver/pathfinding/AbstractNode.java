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
package com.l2jmobius.gameserver.pathfinding;

public abstract class AbstractNode<T extends AbstractNodeLoc>
{
	private T _loc;
	private AbstractNode<T> _parent;
	
	public AbstractNode(T loc)
	{
		_loc = loc;
	}
	
	public void setParent(AbstractNode<T> p)
	{
		_parent = p;
	}
	
	public AbstractNode<T> getParent()
	{
		return _parent;
	}
	
	public T getLoc()
	{
		return _loc;
	}
	
	public void setLoc(T l)
	{
		_loc = l;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((_loc == null) ? 0 : _loc.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof AbstractNode))
		{
			return false;
		}
		final AbstractNode<?> other = (AbstractNode<?>) obj;
		if (_loc == null)
		{
			if (other._loc != null)
			{
				return false;
			}
		}
		else if (!_loc.equals(other._loc))
		{
			return false;
		}
		return true;
	}
}