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

import com.l2jmobius.commons.util.CommonUtil;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.effects.AbstractEffect;
import com.l2jmobius.gameserver.model.events.EventType;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureSkillUse;
import com.l2jmobius.gameserver.model.events.listeners.FunctionEventListener;
import com.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import com.l2jmobius.gameserver.model.skills.BuffInfo;

/**
 * Block Skills by isMagic type.
 * @author Nik
 */
public final class BlockSkill extends AbstractEffect
{
	private final int[] _magicTypes;
	
	public BlockSkill(StatsSet params)
	{
		_magicTypes = params.getIntArray("magicTypes", ";");
	}
	
	public TerminateReturn onSkillUseEvent(OnCreatureSkillUse event)
	{
		if (CommonUtil.contains(_magicTypes, event.getSkill().getMagicType()))
		{
			return new TerminateReturn(true, true, true);
		}
		
		return null;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if ((_magicTypes == null) || (_magicTypes.length == 0))
		{
			return;
		}
		
		info.getEffected().addListener(new FunctionEventListener(info.getEffected(), EventType.ON_CREATURE_SKILL_USE, (OnCreatureSkillUse event) -> onSkillUseEvent(event), this));
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().removeListenerIf(EventType.ON_CREATURE_SKILL_USE, listener -> listener.getOwner() == this);
	}
	
}
