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
package org.l2jmobius.gameserver.model.holders;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.sql.impl.ClanTable;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

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
public class PlayerEventHolder
{
	private final PlayerInstance _player;
	private final String _name;
	private final String _title;
	private final int _clanId;
	private final Location _loc;
	private final int _pvpKills;
	private final int _pkKills;
	private final int _karma;
	
	private final Collection<PlayerInstance> _kills = ConcurrentHashMap.newKeySet();
	private boolean _sitForced;
	
	public PlayerEventHolder(PlayerInstance player)
	{
		this(player, false);
	}
	
	public PlayerEventHolder(PlayerInstance player, boolean sitForced)
	{
		_player = player;
		_name = player.getName();
		_title = player.getTitle();
		_clanId = player.getClanId();
		_loc = new Location(player);
		_pvpKills = player.getPvpKills();
		_pkKills = player.getPkKills();
		_karma = player.getKarma();
		_sitForced = sitForced;
	}
	
	public void restorePlayerStats()
	{
		_player.setName(_name);
		_player.setTitle(_title);
		_player.setClan(ClanTable.getInstance().getClan(_clanId));
		_player.teleToLocation(_loc, true);
		_player.setPvpKills(_pvpKills);
		_player.setPkKills(_pkKills);
		_player.setKarma(_karma);
		
	}
	
	public void setSitForced(boolean sitForced)
	{
		_sitForced = sitForced;
	}
	
	public boolean isSitForced()
	{
		return _sitForced;
	}
	
	public Collection<PlayerInstance> getKills()
	{
		return _kills;
	}
}
