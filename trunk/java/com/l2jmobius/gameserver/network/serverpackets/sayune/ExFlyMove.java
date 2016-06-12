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
package com.l2jmobius.gameserver.network.serverpackets.sayune;

import java.util.List;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.enums.SayuneType;
import com.l2jmobius.gameserver.model.SayuneEntry;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;
import com.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;

/**
 * @author UnAfraid
 */
public class ExFlyMove implements IClientOutgoingPacket
{
	private final int _objectId;
	private final SayuneType _type;
	private final int _mapId;
	private final List<SayuneEntry> _locations;
	
	public ExFlyMove(L2PcInstance activeChar, SayuneType type, int mapId, List<SayuneEntry> locations)
	{
		_objectId = activeChar.getObjectId();
		_type = type;
		_mapId = mapId;
		_locations = locations;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_FLY_MOVE.writeId(packet);
		
		packet.writeD(_objectId);
		
		packet.writeD(_type.ordinal());
		packet.writeD(0x00); // ??
		packet.writeD(_mapId);
		
		packet.writeD(_locations.size());
		for (SayuneEntry loc : _locations)
		{
			packet.writeD(loc.getId());
			packet.writeD(0x00); // ??
			packet.writeD(loc.getX());
			packet.writeD(loc.getY());
			packet.writeD(loc.getZ());
		}
		return true;
	}
}
