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
package handlers.skillconditionhandlers;

import com.l2jmobius.gameserver.enums.SkillConditionAffectType;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.skills.ISkillCondition;
import com.l2jmobius.gameserver.model.skills.Skill;

/**
 * @author Sdw
 */
public class OpEquipItemSkillCondition implements ISkillCondition
{
	private final int _itemId;
	private final SkillConditionAffectType _affectType;
	
	public OpEquipItemSkillCondition(StatsSet params)
	{
		_itemId = params.getInt("itemId");
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
	}
	
	@Override
	public boolean canUse(L2Character caster, Skill skill, L2Object target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return caster.getInventory().getItems(L2ItemInstance::isEquipped, i -> i.getId() == _itemId).size() > 0;
			}
			case TARGET:
			{
				if ((target != null) && target.isPlayer())
				{
					return target.getActingPlayer().getInventory().getItems(L2ItemInstance::isEquipped, i -> i.getId() == _itemId).size() > 0;
				}
			}
			case BOTH:
			{
				if ((target != null) && target.isPlayer())
				{
					return (caster.getInventory().getItems(L2ItemInstance::isEquipped, i -> i.getId() == _itemId).size() > 0) && (target.getActingPlayer().getInventory().getItems(L2ItemInstance::isEquipped, i -> i.getId() == _itemId).size() > 0);
				}
			}
		}
		return false;
	}
}
