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

import java.util.ArrayList;
import java.util.List;

import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.model.L2Effect;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.skills.Env;

/**
 * @author littlecrow Implementation of the Confusion Effect
 */
final class EffectConfusion extends L2Effect
{
	public EffectConfusion(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.CONFUSION;
	}
	
	@Override
	public void onStart()
	{
		getEffected().startConfused();
		onActionTime();
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopConfused(this);
	}
	
	@Override
	public boolean onActionTime()
	{
		final List<L2Character> targetList = new ArrayList<>();
		
		// Getting the possible targets
		
		for (L2Object obj : getEffected().getKnownList().getKnownObjects().values())
		{
			if (obj == null)
			{
				continue;
			}
			
			if ((obj instanceof L2Character) && (obj != getEffected()))
			{
				targetList.add((L2Character) obj);
			}
		}
		// if there is no target, exit function
		if (targetList.size() == 0)
		{
			return true;
		}
		
		// Choosing randomly a new target
		final int nextTargetIdx = Rnd.get(targetList.size());
		final L2Object target = targetList.get(nextTargetIdx);
		
		// Attacking the target
		getEffected().setTarget(target);
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
		
		return true;
	}
}
