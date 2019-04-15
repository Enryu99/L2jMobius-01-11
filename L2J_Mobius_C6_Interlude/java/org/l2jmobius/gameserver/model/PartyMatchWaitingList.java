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
package org.l2jmobius.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author Gnacik
 */
public class PartyMatchWaitingList
{
	private final List<PlayerInstance> _members;
	
	private PartyMatchWaitingList()
	{
		_members = new ArrayList<>();
	}
	
	public void addPlayer(PlayerInstance player)
	{
		if (!_members.contains(player))
		{
			_members.add(player);
		}
	}
	
	public void removePlayer(PlayerInstance player)
	{
		if (_members.contains(player))
		{
			_members.remove(player);
		}
	}
	
	public List<PlayerInstance> getPlayers()
	{
		return _members;
	}
	
	public static PartyMatchWaitingList getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PartyMatchWaitingList _instance = new PartyMatchWaitingList();
	}
}