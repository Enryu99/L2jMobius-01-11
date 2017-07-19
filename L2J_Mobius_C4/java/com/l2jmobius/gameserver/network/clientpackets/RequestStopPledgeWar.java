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

import com.l2jmobius.gameserver.datatables.ClanTable;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestStopPledgeWar extends L2GameClientPacket
{
	private static final String _C__4F_REQUESTSTOPPLEDGEWAR = "[C] 4F RequestStopPledgeWar";
	// private static Logger _log = Logger.getLogger(RequestStopPledgeWar.class.getName());
	
	private String _pledgeName;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		final L2Clan playerClan = player.getClan();
		if (playerClan == null)
		{
			return;
		}
		
		final L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		
		if (clan == null)
		{
			player.sendMessage("Clan does not exist.");
			player.sendPacket(new ActionFailed());
			return;
		}
		
		if ((player.getClanPrivileges() & L2Clan.CP_CL_CLAN_WAR) != L2Clan.CP_CL_CLAN_WAR)
		{
			player.sendMessage("You are not authorized to manage clan wars.");
			player.sendPacket(new ActionFailed());
			return;
		}
		
		if (!playerClan.isAtWarWith(clan.getClanId()))
		{
			player.sendMessage("You aren't at war with this clan.");
			player.sendPacket(new ActionFailed());
			return;
		}
		
		ClanTable.getInstance().deleteclanswars(playerClan.getClanId(), clan.getClanId());
		for (final L2PcInstance cha : L2World.getInstance().getAllPlayers())
		{
			if ((cha.getClan() == player.getClan()) || (cha.getClan() == clan))
			{
				cha.broadcastUserInfo();
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _C__4F_REQUESTSTOPPLEDGEWAR;
	}
}