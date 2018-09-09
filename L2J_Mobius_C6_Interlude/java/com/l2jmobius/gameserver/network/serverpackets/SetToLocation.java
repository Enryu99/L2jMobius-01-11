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
 * 0000: 76 7a 07 80 49 ea 01 00 00 c1 37 fe uz..Ic'.J.....7.
 * <p>
 * 0010: ff 9e c3 03 00 8f f3 ff ff .........
 * <p>
 * <p>
 * format dddddd (player id, target id, distance, startx, starty, startz)
 * <p>
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class SetToLocation extends L2GameServerPacket
{
	private final int _charObjId;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	
	public SetToLocation(L2Character character)
	{
		_charObjId = character.getObjectId();
		_x = character.getX();
		_y = character.getY();
		_z = character.getZ();
		_heading = character.getHeading();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x76);
		
		writeD(_charObjId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
	}
}
