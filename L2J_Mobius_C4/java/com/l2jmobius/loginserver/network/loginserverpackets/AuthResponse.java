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
package com.l2jmobius.loginserver.network.loginserverpackets;

import com.l2jmobius.loginserver.GameServerTable;
import com.l2jmobius.loginserver.network.serverpackets.ServerBasePacket;

/**
 * @author -Wooden-
 */
public class AuthResponse extends ServerBasePacket
{
	/**
	 * @param serverID
	 */
	public AuthResponse(int serverID)
	{
		writeC(0x02);
		writeC(serverID);
		writeS(GameServerTable.getInstance().serverNames.get(serverID));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.loginserver.serverpackets.ServerBasePacket#getContent()
	 */
	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}