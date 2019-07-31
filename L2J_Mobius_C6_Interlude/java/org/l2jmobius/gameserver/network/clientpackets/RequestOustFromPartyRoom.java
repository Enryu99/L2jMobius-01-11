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

import org.l2jmobius.gameserver.instancemanager.TownManager;
import org.l2jmobius.gameserver.model.PartyMatchRoom;
import org.l2jmobius.gameserver.model.PartyMatchRoomList;
import org.l2jmobius.gameserver.model.PartyMatchWaitingList;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchList;

/**
 * format (ch) d
 * @author -Wooden-
 */
public class RequestOustFromPartyRoom extends GameClientPacket
{
	private int _charid;
	
	@Override
	protected void readImpl()
	{
		_charid = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInstance member = World.getInstance().getPlayer(_charid);
		if (member == null)
		{
			return;
		}
		
		final PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(member);
		if (_room == null)
		{
			return;
		}
		
		if (_room.getOwner() != player)
		{
			return;
		}
		
		if (player.isInParty() && member.isInParty() && (player.getParty().getPartyLeaderOID() == member.getParty().getPartyLeaderOID()))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISMISS_PARTY_MEMBER);
		}
		else
		{
			_room.deleteMember(member);
			member.setPartyRoom(0);
			
			// Close the PartyRoom window
			member.sendPacket(new ExClosePartyRoom());
			
			// Add player back on waiting list
			PartyMatchWaitingList.getInstance().addPlayer(member);
			
			// Send Room list
			final int _loc = TownManager.getClosestLocation(member);
			member.sendPacket(new PartyMatchList(member, 0, _loc, member.getLevel()));
			
			// Clean player's LFP title
			member.broadcastUserInfo();
			
			member.sendPacket(SystemMessageId.OUSTED_FROM_PARTY_ROOM);
		}
	}
}