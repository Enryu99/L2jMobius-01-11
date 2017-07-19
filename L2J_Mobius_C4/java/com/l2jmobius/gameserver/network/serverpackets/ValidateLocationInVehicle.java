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
 * This class ...
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class ValidateLocationInVehicle extends L2GameServerPacket
{
	private static final String _S__73_ValidateLocationInVehicle = "[S] 73 ValidateLocationInVehicle";
	
	private final int _charObjId;
	private final int _boatObjId;
	private final int _heading;
	private final Point3D _pos;
	
	/**
	 * 0x73 ValidateLocationInVehicle hdd
	 * @param player
	 */
	public ValidateLocationInVehicle(L2PcInstance player)
	{
		_charObjId = player.getObjectId();
		_boatObjId = player.getBoat().getObjectId();
		_heading = player.getHeading();
		_pos = player.getInBoatPosition();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x73);
		writeD(_charObjId);
		writeD(_boatObjId);
		writeD(_pos.getX());
		writeD(_pos.getY());
		writeD(_pos.getZ());
		writeD(_heading);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__73_ValidateLocationInVehicle;
	}
}