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

/**
 * Format: (chd)
 * @author mrTJO
 */
public class ExCubeGameRequestReady extends L2GameServerPacket
{
	/**
	 * Show Confirm Dialog for 10 seconds
	 */
	public ExCubeGameRequestReady()
	{
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x98);
		writeD(0x04);
	}
}