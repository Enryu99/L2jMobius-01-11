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

import com.l2jmobius.Config;
import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.data.sql.impl.ClanTable;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.PledgeInfo;

public final class RequestPledgeInfo implements IClientIncomingPacket
{
	private int _clanId;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_clanId = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		
		final L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final L2Clan clan = ClanTable.getInstance().getClan(_clanId);
		if (clan == null)
		{
			if (Config.DEBUG)
			{
				_log.warning(getClass().getSimpleName() + ": Clan data for clanId " + _clanId + " is missing for player " + activeChar);
			}
			return; // we have no clan data ?!? should not happen
		}
		
		client.sendPacket(new PledgeInfo(clan));
	}
}
