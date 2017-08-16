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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.l2jmobius.gameserver.data.xml.impl.ArmorSetsData;
import com.l2jmobius.gameserver.model.L2ArmorSet;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.stats.BaseStats;
import com.l2jmobius.gameserver.model.stats.IStatsFunction;
import com.l2jmobius.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class BaseStatsFinalizer implements IStatsFunction
{
	@Override
	public double calc(L2Character creature, Optional<Double> base, Stats stat)
	{
		throwIfPresent(base);
		
		// Apply template value
		double baseValue = creature.getTemplate().getBaseValue(stat, 0);
		
		final L2PcInstance player = creature.getActingPlayer();
		if (player != null)
		{
			final Set<L2ArmorSet> appliedSets = new HashSet<>(2);
			
			// Armor sets calculation
			for (L2ItemInstance item : player.getInventory().getPaperdollItems())
			{
				for (L2ArmorSet set : ArmorSetsData.getInstance().getSets(item.getId()))
				{
					if ((set.getPiecesCount(player, L2ItemInstance::getId) >= set.getMinimumPieces()) && appliedSets.add(set))
					{
						baseValue += set.getStatsBonus(BaseStats.valueOf(stat));
					}
				}
			}
			
			// Henna calculation
			baseValue += player.getHennaValue(BaseStats.valueOf(stat));
		}
		return validateValue(creature, Stats.defaultValue(creature, stat, baseValue), 1, BaseStats.MAX_STAT_VALUE - 1);
	}
	
}
