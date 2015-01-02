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
package com.l2jserver.gameserver.model.events.impl.character.player.mentoring;

import com.l2jserver.gameserver.model.L2Mentee;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.impl.IBaseEvent;

/**
 * @author UnAfraid
 */
public class OnPlayerMenteeRemove implements IBaseEvent
{
	private final L2PcInstance _mentor;
	private final L2Mentee _mentee;
	
	public OnPlayerMenteeRemove(L2PcInstance mentor, L2Mentee mentee)
	{
		_mentor = mentor;
		_mentee = mentee;
	}
	
	public L2PcInstance getMentor()
	{
		return _mentor;
	}
	
	public L2Mentee getMentee()
	{
		return _mentee;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_MENTEE_REMOVE;
	}
}
