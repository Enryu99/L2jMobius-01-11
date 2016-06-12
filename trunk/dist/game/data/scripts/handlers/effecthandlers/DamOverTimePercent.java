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
package handlers.effecthandlers;

import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.effects.AbstractEffect;
import com.l2jmobius.gameserver.model.effects.L2EffectType;
import com.l2jmobius.gameserver.model.skills.BuffInfo;
import com.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Damage Over Time Percent effect implementation.
 * @author Adry_85
 */
public final class DamOverTimePercent extends AbstractEffect
{
	private final boolean _canKill;
	private final double _power;
	
	public DamOverTimePercent(StatsSet params)
	{
		_canKill = params.getBoolean("canKill", false);
		_power = params.getDouble("power");
		setTicks(params.getInt("ticks"));
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DMG_OVER_TIME_PERCENT;
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		if (info.getEffected().isDead())
		{
			return false;
		}
		
		double damage = info.getEffected().getCurrentHp() * _power * getTicksMultiplier();
		if (damage >= (info.getEffected().getCurrentHp() - 1))
		{
			if (info.getSkill().isToggle())
			{
				info.getEffected().sendPacket(SystemMessageId.YOUR_SKILL_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_HP);
				return false;
			}
			
			// For DOT skills that will not kill effected player.
			if (!_canKill)
			{
				// Fix for players dying by DOTs if HP < 1 since reduceCurrentHP method will kill them
				if (info.getEffected().getCurrentHp() <= 1)
				{
					return info.getSkill().isToggle();
				}
				
				damage = info.getEffected().getCurrentHp() - 1;
			}
		}
		
		info.getEffected().reduceCurrentHp(damage, info.getEffector(), info.getSkill(), true, false, false, false);
		
		return info.getSkill().isToggle();
	}
}
