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
package quests.Q10431_TheSealOfPunishmentDenOfEvil;

import java.util.HashMap;
import java.util.Map;

import com.l2jmobius.gameserver.enums.CategoryType;
import com.l2jmobius.gameserver.enums.QuestSound;
import com.l2jmobius.gameserver.enums.Race;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;

/**
 * The Seal of Punishment: Den of Evil (10431)
 * @author Stayway
 */
public class Q10431_TheSealOfPunishmentDenOfEvil extends Quest
{
	// Npcs
	private static final int JOKEL = 33868;
	private static final int CHAIREN = 32655;
	// Item
	private static final int EVIL_FREED_SOUL = 36715;
	// Misc
	private static final Map<Integer, Double> RAGNA_ORC = new HashMap<>();
	static
	{
		RAGNA_ORC.put(22692, 0.888); // Ragna Orc Warriors
		RAGNA_ORC.put(22693, 0.888); // Ragna Orc Heroes
		RAGNA_ORC.put(22694, 0.888); // Ragna Orc Commanders
		RAGNA_ORC.put(22695, 0.888); // Ragna Orc Healers
		RAGNA_ORC.put(22696, 0.888); // Ragna Orc Shamans
		RAGNA_ORC.put(22697, 0.888); // Ragna Orc Priests
		RAGNA_ORC.put(22698, 0.888); // Ragna Orc Archers
		RAGNA_ORC.put(22699, 0.888); // Ragna Orc Snipers
		RAGNA_ORC.put(22701, 0.888); // Varangka's Dre Vanuls
		RAGNA_ORC.put(22702, 0.888); // Varangka's Destroyers
	}
	private static final int MIN_LEVEL = 81;
	private static final int MAX_LEVEL = 84;
	
	public Q10431_TheSealOfPunishmentDenOfEvil()
	{
		super(10431);
		addStartNpc(JOKEL);
		addTalkId(JOKEL, CHAIREN);
		addKillId(RAGNA_ORC.keySet());
		registerQuestItems(EVIL_FREED_SOUL);
		addCondMaxLevel(MAX_LEVEL, "33868-06.html");
		addCondMaxLevel(MIN_LEVEL, "33868-06.html");
		addCondNotRace(Race.ERTHEIA, "noErtheia.html");
		addCondInCategory(CategoryType.FOURTH_CLASS_GROUP, "nocond.html");
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		String htmltext = event;
		switch (event)
		{
			case "32655-02.htm":
			case "33868-02.htm":
			case "33868-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33868-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32655-03.html":
			{
				qs.setCond(2, true);
				htmltext = event;
				break;
			}
			case "reward_9546":
			case "reward_9547":
			case "reward_9548":
			case "reward_9549":
			case "reward_9550":
			case "reward_9551":
			{
				if (!qs.isCond(3))
				{
					break;
				}
				takeItems(player, EVIL_FREED_SOUL, -1);
				final int stoneId = Integer.parseInt(event.replaceAll("reward_", ""));
				giveItems(player, stoneId, 15);
				giveStoryQuestReward(player, 60);
				final long count = getQuestItemsCount(player, EVIL_FREED_SOUL);
				if ((count >= 50) && (count < 100))
				{
					addExpAndSp(player, 28240800, 6777);
				}
				else if ((count >= 100) && (count < 200))
				{
					addExpAndSp(player, 56481600, 13554);
				}
				else if ((count >= 200) && (count < 300))
				{
					addExpAndSp(player, 84722400, 20331);
				}
				else if ((count >= 300) && (count < 400))
				{
					addExpAndSp(player, 112963200, 27108);
				}
				else if ((count >= 400) && (count < 500))
				{
					addExpAndSp(player, 141204000, 33835);
				}
				else if ((count >= 500) && (count < 600))
				{
					addExpAndSp(player, 169444800, 40662);
				}
				else if ((count >= 600) && (count < 700))
				{
					addExpAndSp(player, 197685600, 47439);
				}
				else if ((count >= 700) && (count < 800))
				{
					addExpAndSp(player, 225926400, 54216);
				}
				else if ((count >= 800) && (count < 900))
				{
					addExpAndSp(player, 254167200, 60993);
				}
				else if (count >= 900)
				{
					addExpAndSp(player, 282408000, 67770);
				}
				qs.exitQuest(false, true);
				htmltext = "32655-06.html";
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (npc.getId())
		{
			case JOKEL:
			{
				if (qs.isCreated())
				{
					htmltext = "33868-01.htm";
				}
				else if (qs.isCond(1))
				{
					htmltext = "33868-05.html";
					
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
			case CHAIREN:
			{
				if (qs.isCond(1))
				{
					htmltext = "32655-01.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "32655-04.html";
				}
				else if (qs.isCond(3))
				{
					htmltext = "32655-05.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		
		if (qs == null)
		{
			return null;
		}
		
		final int npcId = npc.getId();
		
		if (RAGNA_ORC.containsKey(npcId))
		{
			giveItemRandomly(qs.getPlayer(), npc, EVIL_FREED_SOUL, 1, 0, RAGNA_ORC.get(npcId), true);
		}
		if (getQuestItemsCount(killer, EVIL_FREED_SOUL) < 50)
		{
			giveItems(killer, EVIL_FREED_SOUL, 1);
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if (getQuestItemsCount(killer, EVIL_FREED_SOUL) >= 50)
		{
			qs.setCond(3, true);
		}
		return super.onKill(npc, killer, isSummon);
	}
}