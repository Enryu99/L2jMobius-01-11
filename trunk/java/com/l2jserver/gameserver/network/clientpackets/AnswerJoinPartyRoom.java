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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.PartyMatchRoom;
import com.l2jserver.gameserver.model.PartyMatchRoomList;
import com.l2jserver.gameserver.model.PartyMatchWaitingList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExManagePartyRoomMember;
import com.l2jserver.gameserver.network.serverpackets.ExPartyRoomMember;
import com.l2jserver.gameserver.network.serverpackets.PartyMatchDetail;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) d
 * @author -Wooden-, Tryskell
 */
public final class AnswerJoinPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_30_ANSWERJOINPARTYROOM = "[C] D0:30 AnswerJoinPartyRoom";
	private int _answer; // 1 or 0
	
	@Override
	protected void readImpl()
	{
		_answer = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final L2PcInstance partner = player.getActiveRequester();
		if (partner == null)
		{
			// Partner hasn't been found, cancel the invitation
			player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
			player.setActiveRequester(null);
			return;
		}
		else if (L2World.getInstance().getPlayer(partner.getObjectId()) == null)
		{
			// Partner hasn't been found, cancel the invitation
			player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
			player.setActiveRequester(null);
			return;
		}
		
		if (Config.FACTION_SYSTEM_ENABLED && ((player.isEvil() && partner.isGood()) || (player.isGood() && partner.isEvil())))
		{
			player.sendMessage("You cannot party with different team members.");
			player.setActiveRequester(null);
			return;
		}
		
		// If answer is positive, join the requester's PartyRoom.
		if ((_answer == 1) && !partner.isRequestExpired())
		{
			final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(partner.getPartyRoom());
			if (room == null)
			{
				return;
			}
			
			if ((player.getLevel() >= room.getMinLvl()) && (player.getLevel() <= room.getMaxLvl()))
			{
				// Remove from waiting list
				PartyMatchWaitingList.getInstance().removePlayer(player);
				
				player.setPartyRoom(partner.getPartyRoom());
				
				player.sendPacket(new PartyMatchDetail(player, room));
				player.sendPacket(new ExPartyRoomMember(player, room, 0));
				
				for (L2PcInstance member : room.getPartyMembers())
				{
					if (member == null)
					{
						continue;
					}
					
					member.sendPacket(new ExManagePartyRoomMember(player, room, 0));
					member.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_ENTERED_THE_PARTY_ROOM).addPcName(player));
				}
				room.addMember(player);
				
				// Info Broadcast
				player.broadcastUserInfo();
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIREMENTS_TO_ENTER_THAT_PARTY_ROOM);
			}
		}
		else
		{
			partner.sendPacket(SystemMessageId.THE_RECIPIENT_OF_YOUR_INVITATION_DID_NOT_ACCEPT_THE_PARTY_MATCHING_INVITATION);
		}
		
		// reset transaction timers
		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}
	
	@Override
	public String getType()
	{
		return _C__D0_30_ANSWERJOINPARTYROOM;
	}
}
