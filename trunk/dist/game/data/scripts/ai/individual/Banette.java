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
package ai.individual;

import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.NpcStringId;

import ai.npc.AbstractNpcAI;

/**
 * Banette AI.
 * @author Gladicek
 */
final class Banette extends AbstractNpcAI
{
	// NPCs
	private static final int BANETTE = 33114;
	// Misc
	private static final NpcStringId[] BANETTE_SHOUT =
	{
		NpcStringId.TRAINING_GROUND_IS_LOCATED_STRAIGHT_AHEAD,
		NpcStringId.WHILE_TRAINING_IN_THE_TRAINING_GROUNDS_IT_BECOMES_PROGRESSIVELY_DIFFICULT,
		NpcStringId.TRAINING_GROUNDS_ACCESS_YOU_NEED_TO_SPEAK_WITH_PANTHEON_IN_THE_MUSEUM
	};
	
	private Banette()
	{
		super(Banette.class.getSimpleName(), "ai/individual");
		addSpawnId(BANETTE);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equals("SPAM_TEXT") && (npc != null))
		{
			broadcastNpcSay(npc, ChatType.NPC_GENERAL, BANETTE_SHOUT[getRandom(3)], 1000);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		startQuestTimer("SPAM_TEXT", 5000, npc, null, true);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new Banette();
	}
}