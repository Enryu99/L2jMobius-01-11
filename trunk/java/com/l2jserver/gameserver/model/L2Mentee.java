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
package com.l2jserver.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author UnAfraid
 */
public class L2Mentee
{
	private static final Logger _log = Logger.getLogger(L2Mentee.class.getName());
	
	private final int _objectId;
	private String _name;
	private int _classId;
	private int _currentLevel;
	
	public L2Mentee(int objectId)
	{
		_objectId = objectId;
		load();
	}
	
	public void load()
	{
		L2PcInstance player = getPlayerInstance();
		if (player == null) // Only if player is offline
		{
			try (Connection con = ConnectionFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT char_name, level, base_class FROM characters WHERE charId = ?"))
			{
				ps.setInt(1, getObjectId());
				try (ResultSet rs = ps.executeQuery())
				{
					if (rs.next())
					{
						_name = rs.getString("char_name");
						_classId = rs.getInt("base_class");
						_currentLevel = rs.getInt("level");
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, e.getMessage(), e);
			}
		}
		else
		{
			_name = player.getName();
			_classId = player.getBaseClassId();
			_currentLevel = player.getLevel();
		}
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getClassId()
	{
		if (isOnline())
		{
			if (getPlayerInstance().getClassId().getId() != _classId)
			{
				_classId = getPlayerInstance().getClassId().getId();
			}
		}
		return _classId;
	}
	
	public int getLevel()
	{
		if (isOnline())
		{
			if (getPlayerInstance().getLevel() != _currentLevel)
			{
				_currentLevel = getPlayerInstance().getLevel();
			}
		}
		return _currentLevel;
	}
	
	public L2PcInstance getPlayerInstance()
	{
		return L2World.getInstance().getPlayer(_objectId);
	}
	
	public boolean isOnline()
	{
		return (getPlayerInstance() != null) && (getPlayerInstance().isOnlineInt() > 0);
	}
	
	public int isOnlineInt()
	{
		return isOnline() ? getPlayerInstance().isOnlineInt() : 0;
	}
	
	public void sendPacket(L2GameServerPacket packet)
	{
		if (isOnline())
		{
			getPlayerInstance().sendPacket(packet);
		}
	}
}
