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
package com.l2jmobius.gameserver.model.stats.finalizers;

import java.util.Optional;

import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.stats.IStatsFunction;
import com.l2jmobius.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class MAccuracyFinalizer implements IStatsFunction
{
	@Override
	public double calc(L2Character creature, Optional<Double> base, Stats stat)
	{
		throwIfPresent(base);
		
		double baseValue = calcWeaponPlusBaseValue(creature, stat);
		
		if (creature.isPlayer())
		{
			// Enchanted gloves bonus
			baseValue += calcEnchantBodyPart(creature, L2Item.SLOT_GLOVES);
		}
		
		return Stats.defaultValue(creature, stat, baseValue + (Math.sqrt(creature.getWIT()) * 3) + (creature.getLevel() * 2));
	}
	
	@Override
	public double calcEnchantBodyPartBonus(int enchantLevel, boolean isBlessed)
	{
		if (isBlessed)
		{
			return (0.3 * Math.max(enchantLevel - 3, 0)) + (0.3 * Math.max(enchantLevel - 6, 0));
		}
		
		return (0.2 * Math.max(enchantLevel - 3, 0)) + (0.2 * Math.max(enchantLevel - 6, 0));
	}
}
