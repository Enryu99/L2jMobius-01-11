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

import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

public class ExMoveToLocationInAirShip extends L2GameServerPacket
{
	private final int _charObjId;
	private final int _airShipId;
	private final Location _destination;
	private final int _heading;
	
	/**
	 * @param player
	 */
	public ExMoveToLocationInAirShip(L2PcInstance player)
	{
		_charObjId = player.getObjectId();
		_airShipId = player.getAirShip().getObjectId();
		_destination = player.getInVehiclePosition();
		_heading = player.getHeading();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x6E);
		writeD(_charObjId);
		writeD(_airShipId);
		writeD(_destination.getX());
		writeD(_destination.getY());
		writeD(_destination.getZ());
		writeD(_heading);
	}
}