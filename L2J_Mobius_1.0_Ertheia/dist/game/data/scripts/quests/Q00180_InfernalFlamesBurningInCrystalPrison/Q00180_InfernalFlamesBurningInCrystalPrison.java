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
package quests.Q00180_InfernalFlamesBurningInCrystalPrison;

import com.l2jmobius.gameserver.enums.QuestSound;
import com.l2jmobius.gameserver.enums.QuestType;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.util.Util;

/**
 * @author hlwrave
 * @URL https://l2wiki.com/Infernal_Flames_Burning_in_Crystal_Prison
 */
public class Q00180_InfernalFlamesBurningInCrystalPrison extends Quest
{
	// Npc
	public static final int FIOREN = 33044;
	// Monster
	public static final int BAYLOR = 29213;
	// Misc
	public static final int MIN_LEVEL = 97;
	// Quest Item
	private static final int BELETH_MARK = 17591;
	// Item
	private static final int ENCHANT_SROLL_R = 22428;
	
	public Q00180_InfernalFlamesBurningInCrystalPrison()
	{
		super(180);
		addStartNpc(FIOREN);
		addTalkId(FIOREN);
		registerQuestItems(BELETH_MARK);
		addKillId(BAYLOR);
		addCondMinLevel(MIN_LEVEL, "33044-02.html");
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return getNoQuestMsg(player);
		}
		
		if ("33044-06.html".equals(event))
		{
			qs.startQuest();
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		if (qs.isCreated())
		{
			htmltext = "33044-01.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				htmltext = "33044-07.html";
			}
			else if (qs.isCond(2))
			{
				takeItems(player, BELETH_MARK, -1);
				giveItems(player, ENCHANT_SROLL_R, 1);
				addExpAndSp(player, 14000000, 6400000);
				qs.exitQuest(QuestType.ONE_TIME, true);
				htmltext = "33044-08.html";
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = "33044-03.html";
		}
		
		return htmltext;
	}
	
	@Override
	public void actionForEachPlayer(L2PcInstance player, L2Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && Util.checkIfInRange(1500, npc, player, false))
		{
			giveItems(player, BELETH_MARK, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			qs.setCond(2, true);
		}
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
		return super.onKill(npc, killer, isSummon);
	}
}
