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
package com.l2jmobius.gameserver.network.serverpackets.shuttle;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2ShuttleInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;
import com.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;

/**
 * @author UnAfraid
 */
public class ExShuttleGetOn implements IClientOutgoingPacket
{
	private final int _playerObjectId, _shuttleObjectId;
	private final Location _pos;
	
	public ExShuttleGetOn(L2PcInstance player, L2ShuttleInstance shuttle)
	{
		_playerObjectId = player.getObjectId();
		_shuttleObjectId = shuttle.getObjectId();
		_pos = player.getInVehiclePosition();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_SUTTLE_GET_ON.writeId(packet);
		
		packet.writeD(_playerObjectId);
		packet.writeD(_shuttleObjectId);
		packet.writeD(_pos.getX());
		packet.writeD(_pos.getY());
		packet.writeD(_pos.getZ());
		return true;
	}
}
