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
package org.l2jmobius.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.SiegeSummonInstance;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneRespawn;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * A castle zone
 * @author programmos
 */
public class FortZone extends ZoneRespawn
{
	private Fort _fort;
	
	public FortZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "fortId":
			{
				final int fortId = Integer.parseInt(value);
				// Register self to the correct fort
				_fort = FortManager.getInstance().getFortById(fortId);
				_fort.setZone(this);
				break;
			}
			default:
			{
				super.setParameter(name, value);
				break;
			}
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (_fort.getSiege().isInProgress())
		{
			creature.setInsideZone(ZoneId.PVP, true);
			creature.setInsideZone(ZoneId.SIEGE, true);
			if (creature instanceof PlayerInstance)
			{
				((PlayerInstance) creature).sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (_fort.getSiege().isInProgress())
		{
			creature.setInsideZone(ZoneId.PVP, false);
			creature.setInsideZone(ZoneId.SIEGE, false);
			if (creature instanceof PlayerInstance)
			{
				((PlayerInstance) creature).sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
				
				// Set pvp flag
				if (((PlayerInstance) creature).getPvpFlag() == 0)
				{
					((PlayerInstance) creature).startPvPFlag();
				}
			}
		}
		if (creature instanceof SiegeSummonInstance)
		{
			((SiegeSummonInstance) creature).unSummon(((SiegeSummonInstance) creature).getOwner());
		}
	}
	
	@Override
	protected void onDieInside(Creature creature)
	{
	}
	
	@Override
	protected void onReviveInside(Creature creature)
	{
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (_fort.getSiege().isInProgress())
		{
			for (Creature creature : getCharactersInside())
			{
				try
				{
					onEnter(creature);
				}
				catch (NullPointerException e)
				{
				}
			}
		}
		else
		{
			for (Creature creature : getCharactersInside())
			{
				try
				{
					creature.setInsideZone(ZoneId.PVP, false);
					creature.setInsideZone(ZoneId.SIEGE, false);
					if (creature instanceof PlayerInstance)
					{
						((PlayerInstance) creature).sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
					}
					
					if (creature instanceof SiegeSummonInstance)
					{
						((SiegeSummonInstance) creature).unSummon(((SiegeSummonInstance) creature).getOwner());
					}
				}
				catch (NullPointerException e)
				{
				}
			}
		}
	}
	
	/**
	 * Removes all foreigners from the fort
	 * @param owningClanId
	 */
	public void banishForeigners(int owningClanId)
	{
		for (Creature temp : getCharactersInside())
		{
			if (!(temp instanceof PlayerInstance))
			{
				continue;
			}
			
			if (((PlayerInstance) temp).getClanId() == owningClanId)
			{
				continue;
			}
			
			((PlayerInstance) temp).teleToLocation(TeleportWhereType.TOWN);
		}
	}
	
	/**
	 * Sends a message to all players in this zone
	 * @param message
	 */
	public void announceToPlayers(String message)
	{
		for (Creature temp : getCharactersInside())
		{
			if (temp instanceof PlayerInstance)
			{
				((PlayerInstance) temp).sendMessage(message);
			}
		}
	}
	
	/**
	 * Returns all players within this zone
	 * @return
	 */
	public List<PlayerInstance> getAllPlayers()
	{
		final List<PlayerInstance> players = new ArrayList<>();
		for (Creature temp : getCharactersInside())
		{
			if (temp instanceof PlayerInstance)
			{
				players.add((PlayerInstance) temp);
			}
		}
		return players;
	}
}
