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
package org.l2jmobius.gameserver.skills.effects;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.model.Effect;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.SiegeSummonInstance;
import org.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import org.l2jmobius.gameserver.skills.Env;

/**
 * @author eX1steam
 */
public class EffectTargetMe extends Effect
{
	public EffectTargetMe(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TARGET_ME;
	}
	
	@Override
	public void onStart()
	{
		if (getEffected() instanceof Playable)
		{
			if (getEffected() instanceof SiegeSummonInstance)
			{
				return;
			}
			
			if (getEffected().getTarget() != getEffector())
			{
				// Target is different - stop autoattack and break cast
				getEffected().setTarget(getEffector());
				final MyTargetSelected my = new MyTargetSelected(getEffector().getObjectId(), 0);
				getEffected().sendPacket(my);
				getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			}
			getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getEffector());
		}
	}
	
	@Override
	public void onExit()
	{
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
