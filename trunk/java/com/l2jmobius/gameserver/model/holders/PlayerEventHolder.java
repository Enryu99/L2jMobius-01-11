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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.data.sql.impl.CharNameTable;
import com.l2jmobius.gameserver.data.sql.impl.ClanTable;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * Player event holder, meant for restoring player after event has finished.<br>
 * Allows you to restore following information about player:
 * <ul>
 * <li>Name</li>
 * <li>Title</li>
 * <li>Clan</li>
 * <li>Location</li>
 * <li>PvP Kills</li>
 * <li>PK Kills</li>
 * <li>Karma</li>
 * </ul>
 * @author Nik, xban1x
 */
public final class PlayerEventHolder
{
	private final L2PcInstance _player;
	private final String _name;
	private final String _title;
	private final int _clanId;
	private final Location _loc;
	private final int _pvpKills;
	private final int _pkKills;
	private final int _reputation;
	
	private final Map<L2PcInstance, Integer> _kills = new ConcurrentHashMap<>();
	private boolean _sitForced;
	
	public PlayerEventHolder(L2PcInstance player)
	{
		this(player, false);
	}
	
	public PlayerEventHolder(L2PcInstance player, boolean sitForced)
	{
		_player = player;
		_name = player.getName();
		_title = player.getTitle();
		_clanId = player.getClanId();
		_loc = new Location(player);
		_pvpKills = player.getPvpKills();
		_pkKills = player.getPkKills();
		_reputation = player.getReputation();
		_sitForced = sitForced;
	}
	
	public void restorePlayerStats()
	{
		_player.setName(_name);
		if (Config.CACHE_CHAR_NAMES)
		{
			CharNameTable.getInstance().addName(_player);
		}
		_player.setTitle(_title);
		_player.setClan(ClanTable.getInstance().getClan(_clanId));
		_player.teleToLocation(_loc, true);
		_player.setPvpKills(_pvpKills);
		_player.setPkKills(_pkKills);
		_player.setReputation(_reputation);
		
	}
	
	public void setSitForced(boolean sitForced)
	{
		_sitForced = sitForced;
	}
	
	public boolean isSitForced()
	{
		return _sitForced;
	}
	
	public Map<L2PcInstance, Integer> getKills()
	{
		return Collections.unmodifiableMap(_kills);
	}
	
	public void addKill(L2PcInstance player)
	{
		_kills.merge(player, 1, Integer::sum);
	}
}
