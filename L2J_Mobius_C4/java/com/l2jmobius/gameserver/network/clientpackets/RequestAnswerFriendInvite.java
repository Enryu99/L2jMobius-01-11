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
package com.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance.Friend;
import com.l2jmobius.gameserver.network.serverpackets.FriendList;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * sample 5F 01 00 00 00 format cdd
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestAnswerFriendInvite extends L2GameClientPacket
{
	private static final String _C__5F_REQUESTANSWERFRIENDINVITE = "[C] 5F RequestAnswerFriendInvite";
	private static Logger _log = Logger.getLogger(RequestAnswerFriendInvite.class.getName());
	
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player != null)
		{
			final L2PcInstance requestor = player.getActiveRequester();
			if (requestor == null)
			{
				return;
			}
			
			if (_response == 1)
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (char_id, friend_id) VALUES (?, ?), (?, ?)"))
				{
					statement.setInt(1, requestor.getObjectId());
					statement.setInt(2, player.getObjectId());
					statement.setInt(3, player.getObjectId());
					statement.setInt(4, requestor.getObjectId());
					statement.execute();
					
					SystemMessage msg = new SystemMessage(SystemMessage.YOU_HAVE_SUCCEEDED_INVITING_FRIEND);
					requestor.sendPacket(msg);
					
					// Player added to your friendlist
					msg = new SystemMessage(SystemMessage.S1_ADDED_TO_FRIENDS);
					msg.addString(player.getName());
					requestor.sendPacket(msg);
					
					requestor.getFriendList().add(new Friend(player.getObjectId(), player.getName()));
					player.getFriendList().add(new Friend(requestor.getObjectId(), requestor.getName()));
					
					// has joined as friend.
					msg = new SystemMessage(SystemMessage.S1_JOINED_AS_FRIEND);
					msg.addString(requestor.getName());
					player.sendPacket(msg);
					msg = null;
					
					// Send notifications for both players in order to show them online
					player.sendPacket(new FriendList(requestor));
					requestor.sendPacket(new FriendList(player));
				}
				catch (final Exception e)
				{
					_log.warning("could not add friend objectid: " + e);
				}
			}
			else
			{
				requestor.sendPacket(new SystemMessage(SystemMessage.FAILED_TO_INVITE_A_FRIEND));
			}
			
			player.setActiveRequester(null);
			requestor.onTransactionResponse();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__5F_REQUESTANSWERFRIENDINVITE;
	}
}