/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q10296_SevenSignsPowerOfTheSeal;

import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;

public final class Q10296_SevenSignsPowerOfTheSeal extends Quest
{
	// NPCs
	private static final int ErissEvilThoughts = 32792;
	private static final int Elcadia = 32784;
	private static final int Elcadia_Support = 32785;
	private static final int Hardin = 30832;
	private static final int Wood = 32593;
	private static final int Franz = 32597;
	// Mobs
	private static final int EtisVanEtina = 18949;
	// Items
	private static final int CertificateOfDawn = 17265;
	// Misc
	private static final int MIN_LEVEL = 81;
	
	public Q10296_SevenSignsPowerOfTheSeal()
	{
		super(10296, Q10296_SevenSignsPowerOfTheSeal.class.getSimpleName(), "Seven Signs, One Who Seeks the Power of the Seal");
		addStartNpc(ErissEvilThoughts);
		addTalkId(ErissEvilThoughts, Elcadia, Hardin, Wood, Franz, Elcadia_Support);
		addKillId(EtisVanEtina);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		switch (event)
		{
			case "32792-04.html":
				st.startQuest();
				break;
			case "32784-03.html":
				st.set("cond", "4");
				st.playSound("ItemSound.quest_middle");
				break;
			case "see":
				st.set("cond", "5");
				st.playSound("ItemSound.quest_middle");
				htmltext = "30832-03.html";
				break;
			case "presentation":
				player.showQuestMovie(28);
				break;
			case "reward":
				if (player.isSubClassActive())
				{
					htmltext = "32597-04.html";
				}
				else if (player.getLevel() >= MIN_LEVEL)
				{
					st.addExpAndSp(125000000, 12500000);
					st.giveItems(CertificateOfDawn, 1);
					htmltext = "32597-03.html";
					st.unset("boss");
					st.playSound("ItemSound.quest_finish");
					st.exitQuest(false);
				}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		switch (npc.getId())
		{
			case ErissEvilThoughts:
				switch (st.getState())
				{
					case State.CREATED:
						QuestState SolinasTomb = player.getQuestState("Q10295_SevenSignsSolinasTomb");
						if ((player.getLevel() >= 81) && SolinasTomb.isCompleted())
						{
							htmltext = "32792-01.htm";
						}
						else
						{
							htmltext = "32792-12.html";
							st.exitQuest(true);
						}
					case State.STARTED:
						switch (st.getCond())
						{
							case 1:
								st.set("cond", "2");
								st.playSound("ItemSound.quest_middle");
								htmltext = "32792-05.html";
								break;
							case 2:
								htmltext = "32792-06.html";
						}
						break;
					case State.COMPLETED:
						htmltext = "32792-02.html";
				}
				break;
			case Elcadia:
				if (st.isStarted())
				{
					switch (st.getCond())
					{
						case 3:
							htmltext = "32784-01.html";
							break;
						case 4:
							htmltext = "32784-04.html";
					}
				}
				break;
			case Hardin:
				if (st.isStarted())
				{
					switch (st.getCond())
					{
						case 4:
							htmltext = "30832-01.html";
							break;
						case 5:
							htmltext = "30832-03.html";
					}
				}
				break;
			case Wood:
				if (st.isStarted())
				{
					if (st.getCond() == 5)
					{
						htmltext = "32593-01.html";
					}
				}
				break;
			case Franz:
				if (st.isStarted())
				{
					if (st.getCond() == 5)
					{
						htmltext = "32597-01.html";
					}
				}
				break;
			case Elcadia_Support:
				if (st.isStarted())
				{
					if ((st.getCond() == 2) && (st.getInt("boss") == 1))
					{
						st.set("cond", "3");
						st.playSound("ItemSound.quest_middle");
						htmltext = "32785-01.html";
					}
					else if (st.getCond() == 3)
					{
						htmltext = "32785-01.html";
					}
				}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		player.showQuestMovie(30);
		if (st.getInt("boss") != 1)
		{
			st.set("boss", "1");
		}
		
		return null;
	}
}