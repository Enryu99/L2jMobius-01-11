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

import com.l2jmobius.gameserver.datatables.sql.CharNameTable;
import com.l2jmobius.gameserver.model.World;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendList extends GameClientPacket
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
		
		// ======<Friend List>======
		player.sendPacket(SystemMessageId.FRIEND_LIST_HEAD);
		
		for (int id : player.getFriendList())
		{
			final String friendName = CharNameTable.getInstance().getPlayerName(id);
			if (friendName == null)
			{
				continue;
			}
			
			final PlayerInstance friend = World.getInstance().getPlayer(id);
			
			player.sendPacket(SystemMessage.getSystemMessage(((friend == null) || (friend.isOnline() == 0)) ? SystemMessageId.S1_OFFLINE : SystemMessageId.S1_ONLINE).addString(friendName));
		}
		
		// =========================
		player.sendPacket(SystemMessageId.FRIEND_LIST_FOOT);
	}
}
