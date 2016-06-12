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
package ai.areas.TalkingIsland.Apprentice;

import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.instancemanager.QuestManager;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;
import quests.Q10329_BackupSeekers.Q10329_BackupSeekers;

/**
 * Apprentice AI.
 * @author St3eT
 */
public final class Apprentice extends AbstractNpcAI
{
	// NPCs
	private static final int APPRENTICE = 33124;
	// Skill
	private static final SkillHolder KUKURU = new SkillHolder(9204, 1); // Kukuru
	
	private Apprentice()
	{
		addSpawnId(APPRENTICE);
		addStartNpc(APPRENTICE);
		addTalkId(APPRENTICE);
		addFirstTalkId(APPRENTICE);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equals("rideKukuru"))
		{
			if (!player.isTransformed())
			{
				KUKURU.getSkill().applyEffects(npc, player);
				final QuestState st = player.getQuestState(Q10329_BackupSeekers.class.getSimpleName());
				if ((st != null) && st.isStarted())
				{
					final Quest quest_10329 = QuestManager.getInstance().getQuest(Q10329_BackupSeekers.class.getSimpleName());
					if (quest_10329 != null)
					{
						quest_10329.notifyEvent("RESPAWN_BART", null, player);
					}
				}
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_CAN_T_RIDE_A_KUKURI_NOW);
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, L2Npc npc, L2PcInstance player)
	{
		if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TRY_RIDING_A_KUKURI, 1000);
		}
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		getTimers().addRepeatingTimer("SPAM_TEXT", 12000, npc, null);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new Apprentice();
	}
}
