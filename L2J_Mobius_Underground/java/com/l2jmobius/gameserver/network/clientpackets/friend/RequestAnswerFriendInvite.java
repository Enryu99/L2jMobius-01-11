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
package com.l2jmobius.gameserver.network.clientpackets.friend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.clientpackets.IClientIncomingPacket;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.friend.FriendAddRequestResult;

public final class RequestAnswerFriendInvite implements IClientIncomingPacket
{
	private int _response;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		packet.readC();
		_response = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final L2PcInstance requestor = player.getActiveRequester();
		if (requestor == null)
		{
			return;
		}
		
		if (player == requestor)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_YOUR_OWN_FRIEND_LIST);
			return;
		}
		
		if (player.getFriendList().contains(requestor.getObjectId()) //
			|| requestor.getFriendList().contains(player.getObjectId()))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_ON_YOUR_FRIEND_LIST);
			sm.addCharName(player);
			requestor.sendPacket(sm);
			return;
		}
		
		if (_response == 1)
		{
			try (Connection con = DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId) VALUES (?, ?), (?, ?)"))
			{
				statement.setInt(1, requestor.getObjectId());
				statement.setInt(2, player.getObjectId());
				statement.setInt(3, player.getObjectId());
				statement.setInt(4, requestor.getObjectId());
				statement.execute();
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.THAT_PERSON_HAS_BEEN_SUCCESSFULLY_ADDED_TO_YOUR_FRIEND_LIST);
				requestor.sendPacket(msg);
				
				// Player added to your friend list
				msg = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED_TO_YOUR_FRIENDS_LIST);
				msg.addString(player.getName());
				requestor.sendPacket(msg);
				requestor.getFriendList().add(player.getObjectId());
				
				// has joined as friend.
				msg = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_JOINED_AS_A_FRIEND);
				msg.addString(requestor.getName());
				player.sendPacket(msg);
				player.getFriendList().add(requestor.getObjectId());
				
				// Send notifications for both player in order to show them online
				player.sendPacket(new FriendAddRequestResult(requestor, 1));
				requestor.sendPacket(new FriendAddRequestResult(player, 1));
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not add friend objectid: " + e.getMessage(), e);
			}
		}
		else
		{
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_A_FRIEND_TO_YOUR_FRIENDS_LIST);
			requestor.sendPacket(msg);
		}
		
		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
}
