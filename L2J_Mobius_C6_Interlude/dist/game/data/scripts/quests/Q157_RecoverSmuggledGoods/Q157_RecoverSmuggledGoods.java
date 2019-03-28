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
package quests.Q157_RecoverSmuggledGoods;

import com.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.quest.State;

public class Q157_RecoverSmuggledGoods extends Quest
{
	private static final String qn = "Q157_RecoverSmuggledGoods";
	
	// Item
	private static final int ADAMANTITE_ORE = 1024;
	
	// Reward
	private static final int BUCKLER = 20;
	
	public Q157_RecoverSmuggledGoods()
	{
		super(157, qn, "Recover Smuggled Goods");
		
		registerQuestItems(ADAMANTITE_ORE);
		
		addStartNpc(30005); // Wilford
		addTalkId(30005);
		
		addKillId(20121); // Toad
	}
	
	@Override
	public String onAdvEvent(String event, NpcInstance npc, PlayerInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30005-05.htm"))
		{
			st.setState(State.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(NpcInstance npc, PlayerInstance player)
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
				htmltext = (player.getLevel() < 5) ? "30005-02.htm" : "30005-03.htm";
				break;
			
			case State.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
				{
					htmltext = "30005-06.htm";
				}
				else if (cond == 2)
				{
					htmltext = "30005-07.htm";
					st.takeItems(ADAMANTITE_ORE, -1);
					st.giveItems(BUCKLER, 1);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(false);
				}
				break;
			
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(NpcInstance npc, PlayerInstance player, boolean isPet)
	{
		QuestState st = checkPlayerCondition(player, npc, "cond", "1");
		if (st == null)
		{
			return null;
		}
		
		if (st.dropItems(ADAMANTITE_ORE, 1, 20, 400000))
		{
			st.set("cond", "2");
		}
		
		return null;
	}
}