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

import java.util.OptionalDouble;

import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.items.instance.ItemInstance;
import com.l2jmobius.gameserver.model.stats.IStatsFunction;
import com.l2jmobius.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class ShotsBonusFinalizer implements IStatsFunction
{
	@Override
	public double calc(Creature creature, OptionalDouble base, Stats stat)
	{
		throwIfPresent(base);
		
		double baseValue = 1;
		double rubyBonus = 0;
		final PlayerInstance player = creature.getActingPlayer();
		if (player != null)
		{
			final ItemInstance weapon = player.getActiveWeaponInstance();
			if ((weapon != null) && weapon.isEnchanted())
			{
				baseValue += (weapon.getEnchantLevel() * 0.7) / 100;
			}
			if (player.getActiveRubyJewel() != null)
			{
				rubyBonus = player.getActiveRubyJewel().getBonus();
			}
		}
		return Stats.defaultValue(creature, stat, baseValue + rubyBonus);
	}
}
