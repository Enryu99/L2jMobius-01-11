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

import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.model.Effect;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.skills.Env;

/**
 * @author kombat
 */
final class EffectBestowSkill extends Effect
{
	public EffectBestowSkill(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
	
	@Override
	public void onStart()
	{
		final Skill tempSkill = SkillTable.getInstance().getInfo(getSkill().getTriggeredId(), getSkill().getTriggeredLevel());
		if (tempSkill != null)
		{
			getEffected().addSkill(tempSkill);
		}
	}
	
	@Override
	public void onExit()
	{
		getEffected().removeSkill(getSkill().getTriggeredId());
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
