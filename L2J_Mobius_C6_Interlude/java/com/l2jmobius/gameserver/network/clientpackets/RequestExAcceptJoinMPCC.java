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

import com.l2jmobius.gameserver.model.CommandChannel;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author -Wooden-
 */
public final class RequestExAcceptJoinMPCC extends GameClientPacket
{
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInstance requestor = player.getActiveRequester();
		if (requestor == null)
		{
			return;
		}
		
		if (_response == 1)
		{
			boolean newCc = false;
			if (!requestor.getParty().isInCommandChannel())
			{
				new CommandChannel(requestor); // Create new CC
				newCc = true;
			}
			
			requestor.getParty().getCommandChannel().addParty(player.getParty());
			if (!newCc)
			{
				player.sendPacket(SystemMessageId.JOINED_COMMAND_CHANNEL);
			}
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_DECLINED_CHANNEL_INVITATION).addString(player.getName()));
		}
		
		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
}
