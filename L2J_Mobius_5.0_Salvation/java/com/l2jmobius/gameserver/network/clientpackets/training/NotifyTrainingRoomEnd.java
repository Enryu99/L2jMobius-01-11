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
package com.l2jmobius.gameserver.network.clientpackets.training;

import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.holders.TrainingHolder;
import com.l2jmobius.gameserver.network.GameClient;
import com.l2jmobius.gameserver.network.clientpackets.IClientIncomingPacket;
import com.l2jmobius.gameserver.network.serverpackets.training.ExTrainingZone_Leaving;

/**
 * @author Sdw
 */
public class NotifyTrainingRoomEnd implements IClientIncomingPacket
{
	@Override
	public boolean read(GameClient client, PacketReader packet)
	{
		// Nothing to read
		return true;
	}
	
	@Override
	public void run(GameClient client)
	{
		final PlayerInstance player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final TrainingHolder holder = player.getTraingCampInfo();
		if (holder == null)
		{
			return;
		}
		
		if (holder.isTraining())
		{
			holder.setEndTime(System.currentTimeMillis());
			player.setTraingCampInfo(holder);
			player.enableAllSkills();
			player.setIsInvul(false);
			player.setInvisible(false);
			player.setIsImmobilized(false);
			player.teleToLocation(player.getLastLocation());
			player.sendPacket(ExTrainingZone_Leaving.STATIC_PACKET);
			holder.setEndTime(System.currentTimeMillis());
			player.setTraingCampInfo(holder);
		}
	}
}
