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
package com.l2jmobius.gameserver.model.matching;

import com.l2jmobius.gameserver.enums.ExManagePartyRoomMemberType;
import com.l2jmobius.gameserver.enums.MatchingMemberType;
import com.l2jmobius.gameserver.enums.MatchingRoomType;
import com.l2jmobius.gameserver.enums.UserInfoType;
import com.l2jmobius.gameserver.instancemanager.MatchingRoomManager;
import com.l2jmobius.gameserver.model.L2CommandChannel;
import com.l2jmobius.gameserver.model.L2Party;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ExDissmissMPCCRoom;
import com.l2jmobius.gameserver.network.serverpackets.ExMPCCRoomInfo;
import com.l2jmobius.gameserver.network.serverpackets.ExMPCCRoomMember;
import com.l2jmobius.gameserver.network.serverpackets.ExManageMpccRoomMember;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Sdw
 */
public class CommandChannelMatchingRoom extends MatchingRoom
{
	public CommandChannelMatchingRoom(String title, int loot, int minlvl, int maxlvl, int maxmem, L2PcInstance leader)
	{
		super(title, loot, minlvl, maxlvl, maxmem, leader);
	}
	
	@Override
	protected void onRoomCreation(L2PcInstance player)
	{
		player.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_MATCHING_ROOM_WAS_CREATED);
	}
	
	@Override
	protected void notifyInvalidCondition(L2PcInstance player)
	{
		player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_THE_COMMAND_CHANNEL_MATCHING_ROOM_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS);
	}
	
	@Override
	protected void notifyNewMember(L2PcInstance player)
	{
		// Update others player
		getMembers().stream().filter(p -> p != player).forEach(p ->
		{
			p.sendPacket(new ExManageMpccRoomMember(p, this, ExManagePartyRoomMemberType.ADD_MEMBER));
		});
		
		// Send SystemMessage to others player
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ENTERED_THE_COMMAND_CHANNEL_MATCHING_ROOM);
		sm.addPcName(player);
		getMembers().stream().filter(p -> p != player).forEach(sm::sendTo);
		
		// Update new player
		player.sendPacket(new ExMPCCRoomInfo(this));
		player.sendPacket(new ExMPCCRoomMember(player, this));
	}
	
	@Override
	protected void notifyRemovedMember(L2PcInstance player, boolean kicked, boolean leaderChanged)
	{
		getMembers().forEach(p ->
		{
			p.sendPacket(new ExMPCCRoomInfo(this));
			p.sendPacket(new ExMPCCRoomMember(player, this));
		});
		
		final SystemMessage sm = SystemMessage.getSystemMessage(kicked ? SystemMessageId.YOU_WERE_EXPELLED_FROM_THE_COMMAND_CHANNEL_MATCHING_ROOM : SystemMessageId.YOU_EXITED_FROM_THE_COMMAND_CHANNEL_MATCHING_ROOM);
		player.sendPacket(sm);
	}
	
	@Override
	public void disbandRoom()
	{
		getMembers().forEach(p ->
		{
			p.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_MATCHING_ROOM_WAS_CANCELLED);
			p.sendPacket(ExDissmissMPCCRoom.STATIC_PACKET);
			p.setMatchingRoom(null);
			p.broadcastUserInfo(UserInfoType.CLAN);
			MatchingRoomManager.getInstance().addToWaitingList(p);
		});
		
		getMembers().clear();
		
		MatchingRoomManager.getInstance().removeMatchingRoom(this);
	}
	
	@Override
	public MatchingRoomType getRoomType()
	{
		return MatchingRoomType.COMMAND_CHANNEL;
	}
	
	@Override
	public MatchingMemberType getMemberType(L2PcInstance player)
	{
		if (isLeader(player))
		{
			return MatchingMemberType.COMMAND_CHANNEL_LEADER;
		}
		
		final L2Party playerParty = player.getParty();
		
		if (playerParty == null)
		{
			return MatchingMemberType.WAITING_PLAYER_NO_PARTY;
		}
		
		final L2Party leaderParty = getLeader().getParty();
		if (leaderParty != null)
		{
			final L2CommandChannel cc = leaderParty.getCommandChannel();
			if ((leaderParty == playerParty) || ((cc != null) && cc.getPartys().contains(playerParty)))
			{
				return MatchingMemberType.COMMAND_CHANNEL_PARTY_MEMBER;
			}
		}
		
		return MatchingMemberType.WAITING_PARTY;
	}
}
