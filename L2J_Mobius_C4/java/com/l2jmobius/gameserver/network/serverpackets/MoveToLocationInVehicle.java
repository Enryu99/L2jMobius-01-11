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

import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.util.Point3D;

/**
 * @author Maktakien
 */
public class MoveToLocationInVehicle extends L2GameServerPacket
{
	private final int _pciId;
	private final int _boatId;
	private final Point3D _destination;
	private final Point3D _origin;
	
	/**
	 * @param player
	 * @param destination
	 * @param origin
	 */
	public MoveToLocationInVehicle(L2PcInstance player, Point3D destination, Point3D origin)
	{
		
		_pciId = player.getObjectId();
		
		_boatId = player.getBoat().getObjectId();
		_destination = destination;
		_origin = origin;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		writeC(0x71);
		
		writeD(_pciId);
		
		writeD(_boatId);
		writeD(_destination.getX());
		writeD(_destination.getY());
		writeD(_destination.getZ());
		writeD(_origin.getX());
		writeD(_origin.getY());
		writeD(_origin.getZ());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] 71 MoveToLocationInVehicle";
	}
}