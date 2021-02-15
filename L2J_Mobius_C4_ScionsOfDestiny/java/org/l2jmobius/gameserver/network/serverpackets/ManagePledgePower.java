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
package org.l2jmobius.gameserver.network.serverpackets;

public class ManagePledgePower extends GameServerPacket
{
	private final int _privs;
	
	public ManagePledgePower(int privs)
	{
		_privs = privs;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x30);
		writeD(0);
		writeD(0);
		writeD(_privs);
	}
}
