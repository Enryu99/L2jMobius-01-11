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
package com.l2jmobius.gameserver.model.instancezone.conditions;

import java.util.List;

import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.instancezone.InstanceTemplate;
import com.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Instance enter group min size
 * @author malyelfik
 */
public final class ConditionGroupMin extends Condition
{
	public ConditionGroupMin(InstanceTemplate template, StatsSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, true, showMessageAndHtml);
		setSystemMessage(SystemMessageId.YOU_MUST_HAVE_A_MINIMUM_OF_S1_PEOPLE_TO_ENTER_THIS_INSTANCED_ZONE, (msg, player) -> msg.addInt(getLimit()));
	}
	
	@Override
	protected boolean test(L2PcInstance player, L2Npc npc, List<L2PcInstance> group)
	{
		return group.size() >= getLimit();
	}
	
	public int getLimit()
	{
		return getParameters().getInt("limit");
	}
}
