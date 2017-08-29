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
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.conditions.Condition;
import com.l2jmobius.gameserver.model.effects.AbstractEffect;
import com.l2jmobius.gameserver.model.skills.BuffInfo;
import com.l2jmobius.gameserver.model.stats.Formulas;
import com.l2jmobius.gameserver.util.Util;

/**
 * Transfer Hate effect implementation.
 * @author Adry_85
 */
public final class TransferHate extends AbstractEffect
{
	private final int _chance;
	
	public TransferHate(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_chance = params.getInt("chance", 100);
	}
	
	@Override
	public boolean calcSuccess(BuffInfo info)
	{
		return Formulas.calcProbability(_chance, info.getEffector(), info.getEffected(), info.getSkill());
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (!Util.checkIfInRange(info.getSkill().getEffectRange(), info.getEffector(), info.getEffected(), true))
		{
			return;
		}
		
		for (L2Character obj : info.getEffector().getKnownList().getKnownCharactersInRadius(info.getSkill().getAffectRange()))
		{
			if ((obj == null) || !obj.isAttackable() || obj.isDead())
			{
				continue;
			}
			
			final L2Attackable hater = ((L2Attackable) obj);
			final int hate = hater.getHating(info.getEffector());
			if (hate <= 0)
			{
				continue;
			}
			
			hater.reduceHate(info.getEffector(), -hate);
			hater.addDamageHate(info.getEffected(), 0, hate);
		}
	}
}
