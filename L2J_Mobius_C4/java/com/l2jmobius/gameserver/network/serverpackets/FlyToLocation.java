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

import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Object;

/**
 * @author KenM
 */
public class FlyToLocation extends L2GameServerPacket
{
	private static final String _S__C5_FLYTOLOCATION = "[S] C5 FlyToLocation";
	
	private final int _destX, _destY, _destZ;
	private final int _chaObjId, _chaX, _chaY, _chaZ;
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
	
	public FlyToLocation(L2Character cha, L2Object dest, FlyType type)
	{
		this(cha, dest.getX(), dest.getY(), dest.getZ(), type);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xc5);
		writeD(_chaObjId);
		writeD(_destX);
		writeD(_destY);
		writeD(_destZ);
		writeD(_chaX);
		writeD(_chaY);
		writeD(_chaZ);
		writeD(_type.ordinal());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__C5_FLYTOLOCATION;
	}
}