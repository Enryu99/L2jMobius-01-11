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

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.interfaces.ILocational;
import com.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author KenM
 */
public final class FlyToLocation implements IClientOutgoingPacket
{
	private final int _destX;
	private final int _destY;
	private final int _destZ;
	private final int _chaObjId;
	private final int _chaX;
	private final int _chaY;
	private final int _chaZ;
	private final FlyType _type;
	
	public enum FlyType
	{
		THROW_UP,
		THROW_HORIZONTAL,
		DUMMY, // no effect
		CHARGE;
	}
	
	public FlyToLocation(L2Character cha, int destX, int destY, int destZ, FlyType type)
	{
		_chaObjId = cha.getObjectId();
		_chaX = cha.getX();
		_chaY = cha.getY();
		_chaZ = cha.getZ();
		_destX = destX;
		_destY = destY;
		_destZ = destZ;
		_type = type;
	}
	
	public FlyToLocation(L2Character cha, ILocational dest, FlyType type)
	{
		this(cha, dest.getX(), dest.getY(), dest.getZ(), type);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.FLY_TO_LOCATION.writeId(packet);
		packet.writeD(_chaObjId);
		packet.writeD(_destX);
		packet.writeD(_destY);
		packet.writeD(_destZ);
		packet.writeD(_chaX);
		packet.writeD(_chaY);
		packet.writeD(_chaZ);
		packet.writeD(_type.ordinal());
		return true;
	}
}
