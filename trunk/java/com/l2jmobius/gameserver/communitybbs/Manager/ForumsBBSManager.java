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
package com.l2jmobius.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.gameserver.communitybbs.BB.Forum;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

public class ForumsBBSManager extends BaseBBSManager
{
	private static Logger _log = Logger.getLogger(ForumsBBSManager.class.getName());
	private final List<Forum> _table = new CopyOnWriteArrayList<>();
	private int _lastid = 1;
	
	/**
	 * Instantiates a new forums bbs manager.
	 */
	protected ForumsBBSManager()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT forum_id FROM forums WHERE forum_type = 0"))
		{
			while (rs.next())
			{
				addForum(new Forum(rs.getInt("forum_id"), null));
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Data error on Forum (root): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Inits the root.
	 */
	public void initRoot()
	{
		_table.forEach(f -> f.vload());
		_log.info("Loaded " + _table.size() + " forums. Last forum id used: " + _lastid);
	}
	
	/**
	 * Adds the forum.
	 * @param ff the forum
	 */
	public void addForum(Forum ff)
	{
		if (ff == null)
		{
			return;
		}
		
		_table.add(ff);
		
		if (ff.getID() > _lastid)
		{
			_lastid = ff.getID();
		}
	}
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
	}
	
	/**
	 * Gets the forum by name.
	 * @param name the forum name
	 * @return the forum by name
	 */
	public Forum getForumByName(String name)
	{
		return _table.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
	}
	
	/**
	 * Creates the new forum.
	 * @param name the forum name
	 * @param parent the parent forum
	 * @param type the forum type
	 * @param perm the perm
	 * @param oid the oid
	 * @return the new forum
	 */
	public Forum createNewForum(String name, Forum parent, int type, int perm, int oid)
	{
		final Forum forum = new Forum(name, parent, type, perm, oid);
		forum.insertIntoDb();
		return forum;
	}
	
	/**
	 * Gets the a new Id.
	 * @return the a new Id
	 */
	public int getANewID()
	{
		return ++_lastid;
	}
	
	/**
	 * Gets the forum by Id.
	 * @param idf the the forum Id
	 * @return the forum by Id
	 */
	public Forum getForumByID(int idf)
	{
		return _table.stream().filter(f -> f.getID() == idf).findFirst().orElse(null);
	}
	
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
	}
	
	/**
	 * Gets the single instance of ForumsBBSManager.
	 * @return single instance of ForumsBBSManager
	 */
	public static ForumsBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ForumsBBSManager _instance = new ForumsBBSManager();
	}
}