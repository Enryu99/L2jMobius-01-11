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
package com.l2jmobius.gameserver.model.events.impl.character.npc;

import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.events.EventType;
import com.l2jmobius.gameserver.model.events.impl.IBaseEvent;

/**
 * An instantly executed event when L2Attackable is killed by L2PcInstance.
 * @author UnAfraid
 */
public class OnAttackableKill implements IBaseEvent
{
	private final L2PcInstance _attacker;
	private final L2Attackable _target;
	private final boolean _isSummon;
	
	public OnAttackableKill(L2PcInstance attacker, L2Attackable target, boolean isSummon)
	{
		_attacker = attacker;
		_target = target;
		_isSummon = isSummon;
	}
	
	public final L2PcInstance getAttacker()
	{
		return _attacker;
	}
	
	public final L2Attackable getTarget()
	{
		return _target;
	}
	
	public final boolean isSummon()
	{
		return _isSummon;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_ATTACKABLE_KILL;
	}
}