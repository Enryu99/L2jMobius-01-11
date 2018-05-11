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
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.stats.BaseStats;
import com.l2jmobius.gameserver.model.stats.IStatsFunction;
import com.l2jmobius.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class RegenCPFinalizer implements IStatsFunction
{
	@Override
	public double calc(L2Character creature, Optional<Double> base, Stats stat)
	{
		throwIfPresent(base);
		if (!creature.isPlayer())
		{
			return 0;
		}
		
		final L2PcInstance player = creature.getActingPlayer();
		double baseValue = player.getTemplate().getBaseCpRegen(creature.getLevel()) * creature.getLevelMod() * BaseStats.CON.calcBonus(creature);
		if (player.isSitting())
		{
			baseValue *= 1.5; // Sitting
		}
		else if (!player.isMoving())
		{
			baseValue *= 1.1; // Staying
		}
		else if (player.isRunning())
		{
			baseValue *= 0.7; // Running
		}
		return Stats.defaultValue(player, stat, baseValue);
	}
}
