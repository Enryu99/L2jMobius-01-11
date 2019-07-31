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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.PartyMatchRoom;
import org.l2jmobius.gameserver.model.PartyMatchRoomList;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchDetail;

public class RequestWithDrawalParty extends GameClientPacket
{
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Party party = player.getParty();
		
		if (party != null)
		{
			if (party.isInDimensionalRift() && !party.getDimensionalRift().getRevivedAtWaitingRoom().contains(player))
			{
				player.sendMessage("You can't exit party when you are in Dimensional Rift.");
			}
			else
			{
				party.removePartyMember(player);
				
				if (player.isInPartyMatchRoom())
				{
					final PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(player);
					if (_room != null)
					{
						player.sendPacket(new PartyMatchDetail(player, _room));
						player.sendPacket(new ExPartyRoomMember(player, _room, 0));
						player.sendPacket(new ExClosePartyRoom());
						
						_room.deleteMember(player);
					}
					player.setPartyRoom(0);
					player.broadcastUserInfo();
				}
			}
		}
	}
}