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
package quests.Q00450_GraveRobberRescue;

import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.enums.QuestSound;
import com.l2jmobius.gameserver.enums.QuestType;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.quest.State;
import com.l2jmobius.gameserver.network.NpcStringId;

/**
 * Grave Robber Rescue (450)
 * @author malyelfik
 */
public class Q00450_GraveRobberRescue extends Quest
{
	// NPCs
	private static final int KANEMIKA = 32650;
	private static final int WARRIOR = 32651;
	// Monster
	private static final int WARRIOR_MON = 22741;
	// Item
	private static final int EVIDENCE_OF_MIGRATION = 14876;
	// Misc
	private static final int MIN_LEVEL = 80;
	
	public Q00450_GraveRobberRescue()
	{
		super(450);
		addStartNpc(KANEMIKA);
		addTalkId(KANEMIKA, WARRIOR);
		registerQuestItems(EVIDENCE_OF_MIGRATION);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState st = getQuestState(player, false);
		
		if (st == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "32650-04.htm":
			case "32650-05.htm":
			case "32650-06.html":
				break;
			case "32650-07.htm":
				st.startQuest();
				break;
			case "despawn":
				npc.setBusy(false);
				npc.deleteMe();
				htmltext = null;
				break;
			default:
				htmltext = null;
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		if (st == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == KANEMIKA)
		{
			switch (st.getState())
			{
				case State.COMPLETED:
					if (!st.isNowAvailable())
					{
						htmltext = "32650-03.html";
						break;
					}
					st.setState(State.CREATED);
				case State.CREATED:
					htmltext = (player.getLevel() >= MIN_LEVEL) ? "32650-01.htm" : "32650-02.htm";
					break;
				case State.STARTED:
					if (st.isCond(1))
					{
						htmltext = (!hasQuestItems(player, EVIDENCE_OF_MIGRATION)) ? "32650-08.html" : "32650-09.html";
					}
					else
					{
						giveAdena(player, 65000, true); // Glory days reward: 6 886 980 exp, 8 116 410 sp, 371 400 Adena
						st.exitQuest(QuestType.DAILY, true);
						htmltext = "32650-10.html";
					}
					break;
			}
		}
		else if (st.isCond(1))
		{
			if (npc.isBusy())
			{
				return null;
			}
			
			if (getRandom(100) < 66)
			{
				giveItems(player, EVIDENCE_OF_MIGRATION, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(npc.getX() + 100, npc.getY() + 100, npc.getZ(), 0));
				npc.setBusy(true);
				
				startQuestTimer("despawn", 3000, npc, player);
				
				if (getQuestItemsCount(player, EVIDENCE_OF_MIGRATION) == 10)
				{
					st.setCond(2, true);
				}
				htmltext = "32651-01.html";
			}
			else
			{
				if (getRandom(100) < 50)
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.GRUNT_OH);
				}
				else
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.GRUNT_WHAT_S_WRONG_WITH_ME);
				}
				npc.deleteMe();
				htmltext = null;
				
				final L2Attackable monster = (L2Attackable) addSpawn(WARRIOR_MON, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 600000);
				monster.setRunning();
				monster.addDamageHate(player, 0, 999);
				monster.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
				showOnScreenMsg(player, NpcStringId.THE_GRAVE_ROBBER_WARRIOR_HAS_BEEN_FILLED_WITH_DARK_ENERGY_AND_IS_ATTACKING_YOU, 5, 5000);
			}
		}
		
		return htmltext;
	}
}
