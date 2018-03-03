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
package com.l2jmobius.gameserver.handler.skillhandlers;

import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.handler.ISkillHandler;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Skill.SkillType;
import com.l2jmobius.gameserver.model.actor.L2Character;

public class ZakenSelf implements ISkillHandler
{
	// private static Logger LOGGER = Logger.getLogger(ZakenSelf.class);
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.ZAKENSELF
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		try
		{
			for (L2Object target1 : targets)
			{
				if (!(target1 instanceof L2Character))
				{
					continue;
				}
				final L2Character target = (L2Character) target1;
				final int ch = (Rnd.get(14) + 1);
				if (ch == 1)
				{
					target.teleToLocation(55299, 219120, -2952, true);
				}
				else if (ch == 2)
				{
					target.teleToLocation(56363, 218043, -2952, true);
				}
				else if (ch == 3)
				{
					target.teleToLocation(54245, 220162, -2952, true);
				}
				else if (ch == 4)
				{
					target.teleToLocation(56289, 220126, -2952, true);
				}
				else if (ch == 5)
				{
					target.teleToLocation(55299, 219120, -3224, true);
				}
				else if (ch == 6)
				{
					target.teleToLocation(56363, 218043, -3224, true);
				}
				else if (ch == 7)
				{
					target.teleToLocation(54245, 220162, -3224, true);
				}
				else if (ch == 8)
				{
					target.teleToLocation(56289, 220126, -3224, true);
				}
				else if (ch == 9)
				{
					target.teleToLocation(55299, 219120, -3496, true);
				}
				else if (ch == 10)
				{
					target.teleToLocation(56363, 218043, -3496, true);
				}
				else if (ch == 11)
				{
					target.teleToLocation(54245, 220162, -3496, true);
				}
				else if (ch == 12)
				{
					target.teleToLocation(56289, 220126, -3496, true);
				}
				else
				{
					target.teleToLocation(53930, 217760, -2944, true);
				}
			}
		}
		catch (Throwable e)
		{
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}