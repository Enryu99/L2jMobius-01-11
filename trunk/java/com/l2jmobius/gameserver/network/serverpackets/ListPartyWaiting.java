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
package com.l2jmobius.gameserver.network.serverpackets;

import java.util.LinkedList;
import java.util.List;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.instancemanager.MatchingRoomManager;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.matching.MatchingRoom;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

/**
 * @author Gnacik
 */
public class ListPartyWaiting implements IClientOutgoingPacket
{
	private final List<MatchingRoom> _rooms = new LinkedList<>();
	private final int _size;
	
	private static final int NUM_PER_PAGE = 64;
	
	public ListPartyWaiting(int level, int location, int page)
	{
		final List<MatchingRoom> rooms = MatchingRoomManager.getInstance().getPartyMathchingRooms(location, level);
		
		_size = rooms.size();
		final int startIndex = (page - 1) * NUM_PER_PAGE;
		int chunkSize = _size - startIndex;
		if (chunkSize > NUM_PER_PAGE)
		{
			chunkSize = NUM_PER_PAGE;
		}
		for (int i = startIndex; i < (startIndex + chunkSize); i++)
		{
			_rooms.add(rooms.get(i));
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.LIST_PARTY_WATING.writeId(packet);
		
		packet.writeD(_size);
		packet.writeD(_rooms.size());
		for (MatchingRoom room : _rooms)
		{
			packet.writeD(room.getId());
			packet.writeS(room.getTitle());
			packet.writeD(room.getLocation());
			packet.writeD(room.getMinLvl());
			packet.writeD(room.getMaxLvl());
			packet.writeD(room.getMaxMembers());
			packet.writeS(room.getLeader().getName());
			packet.writeD(room.getMembersCount());
			for (L2PcInstance member : room.getMembers())
			{
				packet.writeD(member.getClassId().getId());
				packet.writeS(member.getName());
			}
		}
		return true;
	}
}
