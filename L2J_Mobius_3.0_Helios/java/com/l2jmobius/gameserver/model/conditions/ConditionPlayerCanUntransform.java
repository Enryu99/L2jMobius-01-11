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

import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Player Can Untransform condition implementation.
 * @author Adry_85
 */
public class ConditionPlayerCanUntransform extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerCanUntransform(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(L2Character effector, L2Character effected, Skill skill, L2Item item)
	{
		boolean canUntransform = true;
		final L2PcInstance player = effector.getActingPlayer();
		if (player == null)
		{
			canUntransform = false;
		}
		else if (player.isAlikeDead() || player.isCursedWeaponEquipped())
		{
			canUntransform = false;
		}
		else if (player.isFlyingMounted() && !player.isInsideZone(ZoneId.LANDING))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_TOO_HIGH_TO_PERFORM_THIS_ACTION_PLEASE_LOWER_YOUR_ALTITUDE_AND_TRY_AGAIN); // TODO: check if message is retail like.
			canUntransform = false;
		}
		return (_val == canUntransform);
	}
}
