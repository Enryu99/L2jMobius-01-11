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
package ai.areas.TalkingIsland;

import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;

/**
 * Heymond AI.
 * @author St3eT
 */
public final class Heymond extends AbstractNpcAI
{
	// NPCs
	private static final int BANETTE = 33114;
	
	private Heymond()
	{
		addSpawnId(BANETTE);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, L2Npc npc, L2PcInstance player)
	{
		switch (getRandom(4))
		{
			case 0:
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.VIEW_OUR_WIDE_VARIETY_OF_ACCESSORIES);
				break;
			case 1:
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_BEST_WEAPON_DOESN_T_MAKE_YOU_THE_BEST);
				break;
			case 2:
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WE_BUY_AND_SELL_COME_TAKE_A_LOOK);
				break;
		}
		getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new Heymond();
	}
}