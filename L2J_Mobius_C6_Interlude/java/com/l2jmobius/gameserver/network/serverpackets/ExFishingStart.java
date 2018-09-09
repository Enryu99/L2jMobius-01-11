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

import com.l2jmobius.gameserver.model.actor.L2Character;

/**
 * Format (ch)ddddd
 * @author -Wooden-
 */
public class ExFishingStart extends L2GameServerPacket
{
	private final L2Character _activeChar;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _fishType;
	@SuppressWarnings("unused")
	private final boolean _isNightLure;
	
	public ExFishingStart(L2Character character, int fishType, int x, int y, int z, boolean isNightLure)
	{
		_activeChar = character;
		_fishType = fishType;
		_x = x;
		_y = y;
		_z = z;
		_isNightLure = isNightLure;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x13);
		writeD(_activeChar.getObjectId());
		writeD(_fishType); // fish type
		writeD(_x); // x poisson
		writeD(_y); // y poisson
		writeD(_z); // z poisson
		writeC(0x00); // night lure
		writeC(0x00); // ??
		writeC((_fishType >= 7) && (_fishType <= 9) ? 0x01 : 0x00); // 0 = day lure 1 = night lure
		writeC(0x00);
	}
}
