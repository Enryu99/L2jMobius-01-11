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
package org.l2jmobius.gameserver.model.conditions;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.skills.AbnormalType;
import org.l2jmobius.gameserver.model.skills.Skill;

/**
 * The Class ConditionTargetAbnormal.
 * @author janiii
 */
public class ConditionTargetAbnormalType extends Condition
{
	private final AbnormalType _abnormalType;
	
	/**
	 * Instantiates a new condition target abnormal type.
	 * @param abnormalType the abnormal type
	 */
	public ConditionTargetAbnormalType(AbnormalType abnormalType)
	{
		_abnormalType = abnormalType;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, Item item)
	{
		return effected.hasAbnormalType(_abnormalType);
	}
}
