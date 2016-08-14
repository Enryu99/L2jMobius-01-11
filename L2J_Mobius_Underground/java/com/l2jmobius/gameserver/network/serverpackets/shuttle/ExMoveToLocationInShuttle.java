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
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;
import com.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;

/**
 * @author UnAfraid
 */
public class ExMoveToLocationInShuttle implements IClientOutgoingPacket
{
	private final int _charObjId;
	private final int _airShipId;
	private final int _targetX, _targetY, _targetZ;
	private final int _fromX, _fromY, _fromZ;
	
	public ExMoveToLocationInShuttle(L2PcInstance player, int fromX, int fromY, int fromZ)
	{
		_charObjId = player.getObjectId();
		_airShipId = player.getShuttle().getObjectId();
		_targetX = player.getInVehiclePosition().getX();
		_targetY = player.getInVehiclePosition().getY();
		_targetZ = player.getInVehiclePosition().getZ();
		_fromX = fromX;
		_fromY = fromY;
		_fromZ = fromZ;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_MOVE_TO_LOCATION_IN_SUTTLE.writeId(packet);
		
		packet.writeD(_charObjId);
		packet.writeD(_airShipId);
		packet.writeD(_targetX);
		packet.writeD(_targetY);
		packet.writeD(_targetZ);
		packet.writeD(_fromX);
		packet.writeD(_fromY);
		packet.writeD(_fromZ);
		return true;
	}
}
