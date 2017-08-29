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
import com.l2jmobius.gameserver.model.actor.L2Playable;
import com.l2jmobius.gameserver.model.effects.AbstractEffect;
import com.l2jmobius.gameserver.model.skills.BuffInfo;

/**
 * Target Me effect implementation.
 * @author -Nemesiss-
 */
public final class TargetMe extends AbstractEffect
{
	public TargetMe(StatsSet params)
	{
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (info.getEffected().isPlayable())
		{
			((L2Playable) info.getEffected()).setLockedTarget(null);
		}
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (info.getEffected().isPlayable())
		{
			if (info.getEffected().getTarget() != info.getEffector())
			{
				info.getEffected().setTarget(info.getEffector());
			}
			
			((L2Playable) info.getEffected()).setLockedTarget(info.getEffector());
		}
	}
}
