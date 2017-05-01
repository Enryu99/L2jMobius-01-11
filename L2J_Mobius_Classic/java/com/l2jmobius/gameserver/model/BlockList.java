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
package com.l2jmobius.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.gameserver.data.sql.impl.CharNameTable;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.BlockListPacket;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class BlockList
{
	private static Logger _log = Logger.getLogger(BlockList.class.getName());
	private static Map<Integer, List<Integer>> _offlineList = new ConcurrentHashMap<>();
	
	private final L2PcInstance _owner;
	private List<Integer> _blockList;
	
	public BlockList(L2PcInstance owner)
	{
		_owner = owner;
		_blockList = _offlineList.get(owner.getObjectId());
		if (_blockList == null)
		{
			_blockList = loadList(_owner.getObjectId());
		}
	}
	
	private void addToBlockList(int target)
	{
		_blockList.add(target);
		updateInDB(target, true);
	}
	
	private void removeFromBlockList(int target)
	{
		_blockList.remove(Integer.valueOf(target));
		updateInDB(target, false);
	}
	
	public void playerLogout()
	{
		_offlineList.put(_owner.getObjectId(), _blockList);
	}
	
	private static List<Integer> loadList(int ObjId)
	{
		final List<Integer> list = new ArrayList<>();
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=1"))
		{
			statement.setInt(1, ObjId);
			try (ResultSet rset = statement.executeQuery())
			{
				int friendId;
				while (rset.next())
				{
					friendId = rset.getInt("friendId");
					if (friendId == ObjId)
					{
						continue;
					}
					list.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error found in " + ObjId + " FriendList while loading BlockList: " + e.getMessage(), e);
		}
		return list;
	}
	
	private void updateInDB(int targetId, boolean state)
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			if (state) // add
			{
				try (PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId, relation) VALUES (?, ?, 1)"))
				{
					statement.setInt(1, _owner.getObjectId());
					statement.setInt(2, targetId);
					statement.execute();
				}
			}
			else
			// remove
			{
				try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? AND friendId=? AND relation=1"))
				{
					statement.setInt(1, _owner.getObjectId());
					statement.setInt(2, targetId);
					statement.execute();
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not add block player: " + e.getMessage(), e);
		}
	}
	
	public boolean isInBlockList(L2PcInstance target)
	{
		return _blockList.contains(target.getObjectId());
	}
	
	public boolean isInBlockList(int targetId)
	{
		return _blockList.contains(targetId);
	}
	
	public boolean isBlockAll()
	{
		return _owner.getMessageRefusal();
	}
	
	public static boolean isBlocked(L2PcInstance listOwner, L2PcInstance target)
	{
		final BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(target);
	}
	
	public static boolean isBlocked(L2PcInstance listOwner, int targetId)
	{
		final BlockList blockList = listOwner.getBlockList();
		return blockList.isBlockAll() || blockList.isInBlockList(targetId);
	}
	
	private void setBlockAll(boolean state)
	{
		_owner.setMessageRefusal(state);
	}
	
	private List<Integer> getBlockList()
	{
		return _blockList;
	}
	
	public static void addToBlockList(L2PcInstance listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		final String charName = CharNameTable.getInstance().getNameById(targetId);
		
		if (listOwner.getFriendList().contains(targetId))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_ON_YOUR_FRIENDS_LIST);
			sm.addString(charName);
			listOwner.sendPacket(sm);
			return;
		}
		
		if (listOwner.getBlockList().getBlockList().contains(targetId))
		{
			listOwner.sendMessage("Already in ignore list.");
			return;
		}
		
		listOwner.getBlockList().addToBlockList(targetId);
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
		
		final L2PcInstance player = L2World.getInstance().getPlayer(targetId);
		
		if (player != null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_PLACED_YOU_ON_HIS_HER_IGNORE_LIST);
			sm.addString(listOwner.getName());
			player.sendPacket(sm);
		}
	}
	
	public static void removeFromBlockList(L2PcInstance listOwner, int targetId)
	{
		if (listOwner == null)
		{
			return;
		}
		
		SystemMessage sm;
		
		final String charName = CharNameTable.getInstance().getNameById(targetId);
		
		if (!listOwner.getBlockList().getBlockList().contains(targetId))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			listOwner.sendPacket(sm);
			return;
		}
		
		listOwner.getBlockList().removeFromBlockList(targetId);
		
		sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_REMOVED_FROM_YOUR_IGNORE_LIST);
		sm.addString(charName);
		listOwner.sendPacket(sm);
	}
	
	public static boolean isInBlockList(L2PcInstance listOwner, L2PcInstance target)
	{
		return listOwner.getBlockList().isInBlockList(target);
	}
	
	public boolean isBlockAll(L2PcInstance listOwner)
	{
		return listOwner.getBlockList().isBlockAll();
	}
	
	public static void setBlockAll(L2PcInstance listOwner, boolean newValue)
	{
		listOwner.getBlockList().setBlockAll(newValue);
	}
	
	public static void sendListToOwner(L2PcInstance listOwner)
	{
		listOwner.sendPacket(new BlockListPacket(listOwner.getBlockList().getBlockList()));
	}
	
	/**
	 * @param ownerId object id of owner block list
	 * @param targetId object id of potential blocked player
	 * @return true if blocked
	 */
	public static boolean isInBlockList(int ownerId, int targetId)
	{
		final L2PcInstance player = L2World.getInstance().getPlayer(ownerId);
		if (player != null)
		{
			return BlockList.isBlocked(player, targetId);
		}
		if (!_offlineList.containsKey(ownerId))
		{
			_offlineList.put(ownerId, loadList(ownerId));
		}
		return _offlineList.get(ownerId).contains(targetId);
	}
}
