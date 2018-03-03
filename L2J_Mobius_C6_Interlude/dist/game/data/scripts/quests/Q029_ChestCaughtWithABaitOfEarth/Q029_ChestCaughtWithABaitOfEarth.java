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
package quests.Q029_ChestCaughtWithABaitOfEarth;

import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.quest.State;

public class Q029_ChestCaughtWithABaitOfEarth extends Quest
{
	private static final String qn = "Q029_ChestCaughtWithABaitOfEarth";
	
	// NPCs
	private static final int WILLIE = 31574;
	private static final int ANABEL = 30909;
	
	// Items
	private static final int SMALL_PURPLE_TREASURE_CHEST = 6507;
	private static final int SMALL_GLASS_BOX = 7627;
	private static final int PLATED_LEATHER_GLOVES = 2455;
	
	public Q029_ChestCaughtWithABaitOfEarth()
	{
		super(29, qn, "Chest caught with a bait of earth");
		
		registerQuestItems(SMALL_GLASS_BOX);
		
		addStartNpc(WILLIE);
		addTalkId(WILLIE, ANABEL);
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("31574-04.htm"))
		{
			st.setState(State.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equals("31574-07.htm"))
		{
			if (st.hasQuestItems(SMALL_PURPLE_TREASURE_CHEST))
			{
				st.set("cond", "2");
				st.takeItems(SMALL_PURPLE_TREASURE_CHEST, 1);
				st.giveItems(SMALL_GLASS_BOX, 1);
			}
			else
			{
				htmltext = "31574-08.htm";
			}
		}
		else if (event.equals("30909-02.htm"))
		{
			if (st.hasQuestItems(SMALL_GLASS_BOX))
			{
				htmltext = "30909-02.htm";
				st.takeItems(SMALL_GLASS_BOX, 1);
				st.giveItems(PLATED_LEATHER_GLOVES, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
			{
				htmltext = "30909-03.htm";
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() < 48)
				{
					htmltext = "31574-02.htm";
				}
				else
				{
					QuestState st2 = player.getQuestState("Q052_WilliesSpecialBait");
					if ((st2 != null) && st2.isCompleted())
					{
						htmltext = "31574-01.htm";
					}
					else
					{
						htmltext = "31574-03.htm";
					}
				}
				break;
			
			case State.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case WILLIE:
						if (cond == 1)
						{
							htmltext = (!st.hasQuestItems(SMALL_PURPLE_TREASURE_CHEST)) ? "31574-06.htm" : "31574-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31574-09.htm";
						}
						break;
					
					case ANABEL:
						if (cond == 2)
						{
							htmltext = "30909-01.htm";
						}
						break;
				}
				break;
			
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		
		return htmltext;
	}
}