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
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

public class ValidateLocationInVehicle extends L2GameServerPacket
{
	private int _boat = 1343225858;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	private final int _playerObj;
	
	public ValidateLocationInVehicle(L2Character player)
	{
		_playerObj = player.getObjectId();
		_x = player.getX();
		_y = player.getY();
		_z = player.getZ();
		_heading = player.getHeading();
		_boat = ((L2PcInstance) player).getBoat().getObjectId();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x73);
		writeD(_playerObj);
		writeD(_boat);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
	}
}