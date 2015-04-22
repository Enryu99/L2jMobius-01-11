/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.clientpackets.adenadistribution;

import java.util.Objects;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.request.AdenaDistributionRequest;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket;
import com.l2jserver.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaCancel;

/**
 * @author Sdw
 */
public class RequestDivideAdenaCancel extends L2GameClientPacket
{
	private boolean _cancel;
	
	@Override
	protected void readImpl()
	{
		_cancel = readC() == 0;
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (_cancel)
		{
			final AdenaDistributionRequest request = player.getRequest(AdenaDistributionRequest.class);
			request.getPlayers().stream().filter(Objects::nonNull).forEach(p ->
			{
				p.sendPacket(SystemMessageId.ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
				p.sendPacket(ExDivideAdenaCancel.STATIC_PACKET);
				p.removeRequest(AdenaDistributionRequest.class);
			});
		}
	}
	
	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}
