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
package com.l2jmobius.gameserver.model.zone.type;

import java.util.Collection;

import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.model.zone.ZoneType;
import com.l2jmobius.gameserver.network.serverpackets.NpcInfo;

public class WaterZone extends ZoneType
{
	public WaterZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.WATER, true);
		
		if (creature instanceof PlayerInstance)
		{
			((PlayerInstance) creature).broadcastUserInfo();
		}
		else if (creature instanceof NpcInstance)
		{
			final Collection<PlayerInstance> plrs = creature.getKnownList().getKnownPlayers().values();
			for (PlayerInstance player : plrs)
			{
				player.sendPacket(new NpcInfo((NpcInstance) creature, player));
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.WATER, false);
		
		// TODO: update to only send speed status when that packet is known
		if (creature instanceof PlayerInstance)
		{
			// Mobius: Attempt to stop water task.
			if (!creature.isInsideZone(ZoneId.WATER))
			{
				((PlayerInstance) creature).stopWaterTask();
			}
			((PlayerInstance) creature).broadcastUserInfo();
		}
		else if (creature instanceof NpcInstance)
		{
			Collection<PlayerInstance> plrs = creature.getKnownList().getKnownPlayers().values();
			for (PlayerInstance player : plrs)
			{
				player.sendPacket(new NpcInfo((NpcInstance) creature, player));
			}
		}
	}
	
	@Override
	public void onDieInside(Creature creature)
	{
	}
	
	@Override
	public void onReviveInside(Creature creature)
	{
	}
	
	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
}
