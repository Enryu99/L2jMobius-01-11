/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.l2jmobius.gameserver.network.clientpackets;

import java.io.IOException;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.network.ClientThread;
import org.l2jmobius.gameserver.network.Connection;
import org.l2jmobius.gameserver.network.serverpackets.KeyPacket;

public class ProtocolVersion extends ClientBasePacket
{
	private static final Logger _log = Logger.getLogger(ProtocolVersion.class.getName());
	
	public ProtocolVersion(byte[] rawPacket, ClientThread client) throws IOException
	{
		super(rawPacket);
		final int version = readD();
		
		// this packet is never encrypted
		if (version == -2)
		{
			// this is just a ping attempt from the client
		}
		else if (version != Config.CLIENT_PROTOCOL_VERSION)
		{
			_log.warning("Wrong Protocol Version: " + version + ", " + client);
		}
		else
		{
			final Connection con = client.getConnection();
			con.sendPacket(new KeyPacket(con.getCryptKey()));
			con.activateCryptKey();
		}
	}
}
