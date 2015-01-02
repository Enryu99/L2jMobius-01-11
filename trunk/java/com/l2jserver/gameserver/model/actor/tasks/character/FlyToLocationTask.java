/*
 * Copyright (C) 2004-2015 L2J Server
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
package com.l2jserver.gameserver.model.actor.tasks.character;

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.serverpackets.FlyToLocation;

/**
 * Task dedicated to fly a player to the location
 * @author xban1x
 */
public final class FlyToLocationTask implements Runnable
{
	private final L2Character _character;
	private final L2Object _target;
	private final Skill _skill;
	
	public FlyToLocationTask(L2Character character, L2Object target, Skill skill)
	{
		_character = character;
		_target = target;
		_skill = skill;
	}
	
	@Override
	public void run()
	{
		if (_character != null)
		{
			_character.broadcastPacket(new FlyToLocation(_character, _target, _skill.getFlyType()));
			_character.setXYZ(_target.getX(), _target.getY(), _target.getZ());
		}
	}
}
