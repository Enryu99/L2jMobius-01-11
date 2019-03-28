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

import java.util.Collection;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.data.sql.impl.ClanTable;
import com.l2jmobius.gameserver.model.SiegeClan;
import com.l2jmobius.gameserver.model.clan.Clan;
import com.l2jmobius.gameserver.model.entity.Castle;
import com.l2jmobius.gameserver.model.entity.clanhall.SiegableHall;
import com.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * Populates the Siege Attacker List in the SiegeInfo Window<BR>
 * <BR>
 * c = ca<BR>
 * d = CastleID<BR>
 * d = unknow (0x00)<BR>
 * d = unknow (0x01)<BR>
 * d = unknow (0x00)<BR>
 * d = Number of Attackers Clans?<BR>
 * d = Number of Attackers Clans<BR>
 * { //repeats<BR>
 * d = ClanID<BR>
 * S = ClanName<BR>
 * S = ClanLeaderName<BR>
 * d = ClanCrestID<BR>
 * d = signed time (seconds)<BR>
 * d = AllyID<BR>
 * S = AllyName<BR>
 * S = AllyLeaderName<BR>
 * d = AllyCrestID<BR>
 * @author KenM
 */
public final class SiegeAttackerList implements IClientOutgoingPacket
{
	private Castle _castle;
	private SiegableHall _hall;
	
	public SiegeAttackerList(Castle castle)
	{
		_castle = castle;
	}
	
	public SiegeAttackerList(SiegableHall hall)
	{
		_hall = hall;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.CASTLE_SIEGE_ATTACKER_LIST.writeId(packet);
		
		if (_castle != null)
		{
			packet.writeD(_castle.getResidenceId());
			packet.writeD(0x00); // 0
			packet.writeD(0x01); // 1
			packet.writeD(0x00); // 0
			final int size = _castle.getSiege().getAttackerClans().size();
			if (size > 0)
			{
				Clan clan;
				
				packet.writeD(size);
				packet.writeD(size);
				for (SiegeClan siegeclan : _castle.getSiege().getAttackerClans())
				{
					clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
					if (clan == null)
					{
						continue;
					}
					
					packet.writeD(clan.getId());
					packet.writeS(clan.getName());
					packet.writeS(clan.getLeaderName());
					packet.writeD(clan.getCrestId());
					packet.writeD(0x00); // signed time (seconds) (not storated by L2J)
					packet.writeD(clan.getAllyId());
					packet.writeS(clan.getAllyName());
					packet.writeS(""); // AllyLeaderName
					packet.writeD(clan.getAllyCrestId());
				}
			}
			else
			{
				packet.writeD(0x00);
				packet.writeD(0x00);
			}
		}
		else
		{
			packet.writeD(_hall.getId());
			packet.writeD(0x00); // 0
			packet.writeD(0x01); // 1
			packet.writeD(0x00); // 0
			final Collection<SiegeClan> attackers = _hall.getSiege().getAttackerClans();
			final int size = attackers.size();
			if (size > 0)
			{
				packet.writeD(size);
				packet.writeD(size);
				for (SiegeClan sClan : attackers)
				{
					final Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
					if (clan == null)
					{
						continue;
					}
					
					packet.writeD(clan.getId());
					packet.writeS(clan.getName());
					packet.writeS(clan.getLeaderName());
					packet.writeD(clan.getCrestId());
					packet.writeD(0x00); // signed time (seconds) (not storated by L2J)
					packet.writeD(clan.getAllyId());
					packet.writeS(clan.getAllyName());
					packet.writeS(""); // AllyLeaderName
					packet.writeD(clan.getAllyCrestId());
				}
			}
			else
			{
				packet.writeD(0x00);
				packet.writeD(0x00);
			}
		}
		return true;
	}
}
