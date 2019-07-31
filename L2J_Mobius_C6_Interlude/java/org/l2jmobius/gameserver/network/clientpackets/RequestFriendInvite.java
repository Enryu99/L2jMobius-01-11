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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.BlockList;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinFriend;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendInvite extends GameClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		
		if (player == null)
		{
			return;
		}
		
		final PlayerInstance friend = World.getInstance().getPlayer(_name);
		
		// can't use friend invite for locating invisible characters
		if ((friend == null) || (friend.isOnline() == 1) || friend.getAppearance().isInvisible())
		{
			// Target is not found in the game.
			player.sendPacket(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
			return;
		}
		
		if (friend == player)
		{
			// You cannot add yourself to your own friend list.
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
			return;
		}
		
		if (BlockList.isBlocked(player, friend))
		{
			player.sendMessage("You have blocked " + _name + ".");
			return;
		}
		
		if (BlockList.isBlocked(friend, player))
		{
			player.sendMessage("You are in " + _name + "'s block list.");
			return;
		}
		
		if (player.getFriendList().contains(friend.getObjectId()))
		{
			// Player already is in your friendlist
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST).addString(_name));
			return;
		}
		
		if (!friend.isProcessingRequest())
		{
			// request to become friend
			player.onTransactionRequest(friend);
			friend.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_REQUESTED_TO_BECOME_FRIENDS).addString(player.getName()));
			friend.sendPacket(new AskJoinFriend(player.getName()));
		}
		else
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(_name));
		}
	}
}