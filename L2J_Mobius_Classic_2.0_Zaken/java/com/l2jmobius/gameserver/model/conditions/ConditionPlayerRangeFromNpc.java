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
package com.l2jmobius.gameserver.model.conditions;

import com.l2jmobius.commons.util.CommonUtil;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.skills.Skill;

/**
 * Exist NPC condition.
 * @author UnAfraid, Zoey76
 */
public class ConditionPlayerRangeFromNpc extends Condition
{
	/** NPC Ids. */
	private final int[] _npcIds;
	/** Radius to check. */
	private final int _radius;
	/** Expected value. */
	private final boolean _val;
	
	public ConditionPlayerRangeFromNpc(int[] npcIds, int radius, boolean val)
	{
		_npcIds = npcIds;
		_radius = radius;
		_val = val;
	}
	
	@Override
	public boolean testImpl(L2Character effector, L2Character effected, Skill skill, L2Item item)
	{
		boolean existNpc = false;
		if ((_npcIds != null) && (_npcIds.length > 0) && (_radius > 0))
		{
			for (L2Npc target : L2World.getInstance().getVisibleObjects(effector, L2Npc.class, _radius))
			{
				if (CommonUtil.contains(_npcIds, target.getId()))
				{
					existNpc = true;
					break;
				}
			}
		}
		return existNpc == _val;
	}
}
