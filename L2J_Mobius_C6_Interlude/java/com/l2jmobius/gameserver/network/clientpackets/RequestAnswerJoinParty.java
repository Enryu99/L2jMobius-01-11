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

import com.l2jmobius.gameserver.model.PartyMatchRoom;
import com.l2jmobius.gameserver.model.PartyMatchRoomList;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ExManagePartyRoomMember;
import com.l2jmobius.gameserver.network.serverpackets.JoinParty;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * sample 2a 01 00 00 00 format cdd
 */
public final class RequestAnswerJoinParty extends GameClientPacket
{
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInstance requestor = player.getActiveRequester();
		if (requestor == null)
		{
			return;
		}
		
		if (player.isCursedWeaponEquiped() || requestor.isCursedWeaponEquiped())
		{
			requestor.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		
		requestor.sendPacket(new JoinParty(_response));
		
		if (_response == 1)
		{
			if (requestor.isInParty())
			{
				if (requestor.getParty().getMemberCount() >= 9)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.PARTY_FULL);
					player.sendPacket(sm);
					requestor.sendPacket(sm);
					return;
				}
			}
			player.joinParty(requestor.getParty());
			
			if (requestor.isInPartyMatchRoom() && player.isInPartyMatchRoom())
			{
				final PartyMatchRoomList list = PartyMatchRoomList.getInstance();
				if ((list != null) && (list.getPlayerRoomId(requestor) == list.getPlayerRoomId(player)))
				{
					final PartyMatchRoom room = list.getPlayerRoom(requestor);
					if (room != null)
					{
						final ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
						for (PlayerInstance member : room.getPartyMembers())
						{
							if (member != null)
							{
								member.sendPacket(packet);
							}
						}
					}
				}
			}
			else if (requestor.isInPartyMatchRoom() && !player.isInPartyMatchRoom())
			{
				final PartyMatchRoomList list = PartyMatchRoomList.getInstance();
				if (list != null)
				{
					final PartyMatchRoom room = list.getPlayerRoom(requestor);
					if (room != null)
					{
						room.addMember(player);
						final ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
						for (PlayerInstance member : room.getPartyMembers())
						{
							if (member != null)
							{
								member.sendPacket(packet);
							}
						}
						player.setPartyRoom(room.getId());
						player.broadcastUserInfo();
					}
				}
			}
		}
		else // activate garbage collection if there are no other members in party (happens when we were creating new one)
		if (requestor.isInParty() && (requestor.getParty().getMemberCount() == 1))
		{
			requestor.getParty().removePartyMember(requestor, false);
		}
		
		if (requestor.isInParty())
		{
			requestor.getParty().setPendingInvitation(false);
		}
		
		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
}