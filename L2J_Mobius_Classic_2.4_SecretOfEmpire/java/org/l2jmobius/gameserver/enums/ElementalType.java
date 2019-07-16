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
package org.l2jmobius.gameserver.enums;

import org.l2jmobius.gameserver.model.stats.Stats;

/**
 * @author JoeAlisson
 */
public enum ElementalType
{
	NONE,
	FIRE,
	WATER,
	WIND,
	EARTH;
	
	public byte getId()
	{
		return (byte) (ordinal());
	}
	
	public static ElementalType of(byte elementId)
	{
		return values()[elementId];
	}
	
	public ElementalType getDominating()
	{
		return dominating(this);
	}
	
	public ElementalType dominating(ElementalType elementalType)
	{
		switch (elementalType)
		{
			case FIRE:
			{
				return WATER;
			}
			case WATER:
			{
				return EARTH;
			}
			case WIND:
			{
				return FIRE;
			}
			case EARTH:
			{
				return WIND;
			}
			default:
			{
				return NONE;
			}
		}
	}
	
	public Stats getAttackStat()
	{
		switch (this)
		{
			case EARTH:
			{
				return Stats.ELEMENTAL_SPIRIT_EARTH_ATTACK;
			}
			case WIND:
			{
				return Stats.ELEMENTAL_SPIRIT_WIND_ATTACK;
			}
			case FIRE:
			{
				return Stats.ELEMENTAL_SPIRIT_FIRE_ATTACK;
			}
			case WATER:
			{
				return Stats.ELEMENTAL_SPIRIT_WATER_ATTACK;
			}
			default:
			{
				return null;
			}
		}
	}
	
	public Stats getDefenseStat()
	{
		switch (this)
		{
			case EARTH:
			{
				return Stats.ELEMENTAL_SPIRIT_EARTH_DEFENSE;
			}
			case WIND:
			{
				return Stats.ELEMENTAL_SPIRIT_WIND_DEFENSE;
			}
			case FIRE:
			{
				return Stats.ELEMENTAL_SPIRIT_FIRE_DEFENSE;
			}
			case WATER:
			{
				return Stats.ELEMENTAL_SPIRIT_WATER_DEFENSE;
			}
			default:
			{
				return null;
			}
		}
	}
}
