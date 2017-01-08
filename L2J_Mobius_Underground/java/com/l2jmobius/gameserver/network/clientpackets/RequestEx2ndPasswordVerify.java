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
package com.l2jmobius.gameserver.network.clientpackets;

import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.data.xml.impl.SecondaryAuthData;
import com.l2jmobius.gameserver.network.L2GameClient;

/**
 * Format: (ch)S S: numerical password
 * @author mrTJO
 */
public class RequestEx2ndPasswordVerify implements IClientIncomingPacket
{
	private String _password;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_password = packet.readS();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		if (!SecondaryAuthData.getInstance().isEnabled())
		{
			return;
		}
		
		client.getSecondaryAuth().checkPassword(_password, false);
	}
}
