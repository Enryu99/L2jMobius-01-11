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

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @version $Revision: 1.2.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PledgeShowInfoUpdate implements IClientOutgoingPacket
{
	private final Clan _clan;
	
	public PledgeShowInfoUpdate(Clan clan)
	{
		_clan = clan;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		final int TOP = ClanTable.getInstance().getTopRate(_clan.getClanId());
		// ddddddddddSdd
		OutgoingPackets.PLEDGE_SHOW_INFO_UPDATE.writeId(packet);
		// sending empty data so client will ask all the info in response ;)
		packet.writeD(_clan.getClanId());
		packet.writeD(_clan.getCrestId());
		packet.writeD(_clan.getLevel()); // clan level
		packet.writeD(_clan.getFortId() != 0 ? _clan.getFortId() : _clan.getCastleId());
		packet.writeD(_clan.getHideoutId());
		packet.writeD(TOP);
		packet.writeD(_clan.getReputationScore()); // clan reputation score
		packet.writeD(0);
		packet.writeD(0);
		packet.writeD(_clan.getAllyId());
		packet.writeS(_clan.getAllyName());
		packet.writeD(_clan.getAllyCrestId());
		packet.writeD(_clan.isAtWar());
		return true;
	}
}
