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
package com.l2jmobius.gameserver.network.serverpackets.pledgeV2;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.data.xml.impl.ClanMasteryData;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.holders.ClanMasteryHolder;
import com.l2jmobius.gameserver.network.OutgoingPackets;
import com.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Mobius
 */
public class ExPledgeMasteryInfo extends AbstractItemPacket
{
	final PlayerInstance _player;
	
	public ExPledgeMasteryInfo(PlayerInstance player)
	{
		_player = player;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		if (_player.getClan() == null)
		{
			return false;
		}
		
		OutgoingPackets.EX_PLEDGE_MASTERY_INFO.writeId(packet);
		
		packet.writeD(0); // Consumed development points
		packet.writeD(Math.max(0, _player.getClan().getLevel() - 4)); // Total development points
		
		packet.writeD(16); // Masteries count
		for (ClanMasteryHolder mastery : ClanMasteryData.getInstance().getMasteries())
		{
			if (mastery.getId() < 17)
			{
				packet.writeD(mastery.getId()); // Mastery
				packet.writeD(0); // Purchased?
				packet.writeC(/* _player.getClan().getLevel() >= mastery.getClanLevel() ? 1 : */ 0); // Availability - TODO: Previous level requirement.
			}
		}
		
		return true;
	}
}
