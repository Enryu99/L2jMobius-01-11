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

import org.l2jmobius.gameserver.datatables.csv.MapRegionTable.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * An olympiad stadium
 * @author durgus
 */
public class OlympiadStadiumZone extends ZoneType
{
	private int _stadiumId;
	
	public OlympiadStadiumZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("stadiumId"))
		{
			_stadiumId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	public void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, true);
		
		if (creature instanceof PlayerInstance)
		{
			if ((((PlayerInstance) creature).getOlympiadGameId() + 1) == _stadiumId)
			{
				((PlayerInstance) creature).sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
			}
			else
			{
				creature.teleToLocation(TeleportWhereType.Town);
			}
		}
	}
	
	@Override
	public void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, false);
		
		if (creature instanceof PlayerInstance)
		{
			((PlayerInstance) creature).sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
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
	
	/**
	 * Returns this zones stadium id (if any)
	 * @return
	 */
	public int getStadiumId()
	{
		return _stadiumId;
	}
}