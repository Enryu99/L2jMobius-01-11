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
package com.l2jmobius.gameserver.model.events.impl.character;

import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.events.EventType;
import com.l2jmobius.gameserver.model.events.impl.IBaseEvent;

/**
 * @author UnAfraid
 */
public class OnCreatureSee implements IBaseEvent
{
	private final L2Character _seer;
	private final L2Character _seen;
	
	public OnCreatureSee(L2Character seer, L2Character seen)
	{
		_seer = seer;
		_seen = seen;
	}
	
	public final L2Character getSeer()
	{
		return _seer;
	}
	
	public final L2Character getSeen()
	{
		return _seen;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_SEE;
	}
}