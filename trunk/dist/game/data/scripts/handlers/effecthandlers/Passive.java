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
import com.l2jmobius.gameserver.model.conditions.Condition;
import com.l2jmobius.gameserver.model.effects.AbstractEffect;
import com.l2jmobius.gameserver.model.skills.BuffInfo;

/**
 * Passive effect implementation.
 * @author Adry_85
 */
public final class Passive extends AbstractEffect
{
	public Passive(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().enableAllSkills();
		info.getEffected().setIsImmobilized(false);
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return info.getEffected().isAttackable();
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final L2Attackable target = (L2Attackable) info.getEffected();
		target.abortAttack();
		target.abortCast();
		target.disableAllSkills();
		target.setIsImmobilized(true);
	}
}
