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
 * Banette AI.
 * @author St3eT
 */
public final class Banette extends AbstractNpcAI
{
	// NPCs
	private static final int BANETTE = 33114;
	
	private Banette()
	{
		addSpawnId(BANETTE);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, L2Npc npc, L2PcInstance player)
	{
		switch (getRandom(4))
		{
			case 0:
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TRAINING_GROUND_IS_LOCATED_STRAIGHT_AHEAD);
				break;
			case 1:
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHILE_TRAINING_IN_THE_TRAINING_GROUNDS_IT_BECOMES_PROGRESSIVELY_DIFFICULT);
				break;
			case 2:
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TRAINING_GROUNDS_ACCESS_YOU_NEED_TO_SPEAK_WITH_PANTHEON_IN_THE_MUSEUM);
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
		new Banette();
	}
}