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
package com.l2jmobius.gameserver.handler.usercommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.handler.IUserCommandHandler;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Support for /clanwarlist command
 * @author Tempy
 */
public class ClanWarsList implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		88,
		89,
		90
	};
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.handler.IUserCommandHandler#useUserCommand(int, com.l2jmobius.gameserver.model.L2PcInstance)
	 */
	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if ((id != COMMAND_IDS[0]) && (id != COMMAND_IDS[1]) && (id != COMMAND_IDS[2]))
		{
			return false;
		}
		
		final L2Clan clan = activeChar.getClan();
		if (clan == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_A_CLAN_MEMBER));
			return false;
		}
		
		SystemMessage sm;
		String query;
		
		if (id == 88)
		{
			// Attack List
			activeChar.sendPacket(new SystemMessage(1571));
			query = "select clan_name,clan_id,ally_id,ally_name from clan_data,clan_wars where clan1=? and clan_id=clan2 and clan2 not in (select clan1 from clan_wars where clan2=?)";
			
		}
		else if (id == 89)
		{
			// Under Attack List
			activeChar.sendPacket(new SystemMessage(1572));
			query = "select clan_name,clan_id,ally_id,ally_name from clan_data,clan_wars where clan2=? and clan_id=clan1 and clan1 not in (select clan2 from clan_wars where clan1=?)";
			
		}
		else // ID = 90
		{
			// War List
			activeChar.sendPacket(new SystemMessage(1612));
			query = "select clan_name,clan_id,ally_id,ally_name from clan_data,clan_wars where clan1=? and clan_id=clan2 and clan2 in (select clan1 from clan_wars where clan2=?)";
			
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(query))
		{
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, clan.getClanId());
			
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final String clanName = rset.getString("clan_name");
					final int ally_id = rset.getInt("ally_id");
					if (ally_id > 0)
					{
						// Target With Ally
						sm = new SystemMessage(1200);
						sm.addString(clanName);
						sm.addString(rset.getString("ally_name"));
					}
					else
					{
						// Target Without Ally
						sm = new SystemMessage(1202);
						sm.addString(clanName);
					}
					activeChar.sendPacket(sm);
				}
			}
			activeChar.sendPacket(new SystemMessage(SystemMessage.FRIEND_LIST_FOOT));
		}
		catch (final Exception e)
		
		{
		}
		
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.handler.IUserCommandHandler#getUserCommandList()
	 */
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}