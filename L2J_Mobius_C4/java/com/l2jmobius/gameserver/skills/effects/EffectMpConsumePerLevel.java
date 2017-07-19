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
package com.l2jmobius.gameserver.skills.effects;

import com.l2jmobius.gameserver.model.L2Effect;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.skills.Env;

class EffectMpConsumePerLevel extends L2Effect
{
	public EffectMpConsumePerLevel(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MP_CONSUME_PER_LEVEL;
	}
	
	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
		{
			return false;
		}
		
		final double base = calc();
		final double consume = ((getEffected().getLevel() - 1) / 7.5) * base * getPeriod();
		
		if (consume > getEffected().getCurrentMp())
		{
			final SystemMessage sm = new SystemMessage(140);
			getEffected().sendPacket(sm);
			return false;
		}
		
		getEffected().reduceCurrentMp(consume);
		return true;
	}
}