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
package quests.Q00015_SweetWhispers;

import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.quest.State;

/**
 * Sweet Whispers (15)
 * @author nonom
 */
public class Q00015_SweetWhispers extends Quest
{
	// NPCs
	private static final int VLADIMIR = 31302;
	private static final int HIERARCH = 31517;
	private static final int M_NECROMANCER = 31518;
	// Misc
	private static final int MIN_LEVEL = 60;
	
	public Q00015_SweetWhispers()
	{
		super(15);
		addStartNpc(VLADIMIR);
		addTalkId(VLADIMIR, HIERARCH, M_NECROMANCER);
		addCondMinLevel(MIN_LEVEL, "31302-00a.html");
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31302-01.html":
				st.startQuest();
				break;
			case "31518-01.html":
				if (st.isCond(1))
				{
					st.setCond(2);
				}
				break;
			case "31517-01.html":
				if (st.isCond(2))
				{
					addExpAndSp(player, 714215, 171);
					st.exitQuest(false, true);
				}
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
		
		final int npcId = npc.getId();
		switch (st.getState())
		{
			case State.CREATED:
				if (npcId == VLADIMIR)
				{
					htmltext = "31302-00.htm";
				}
				break;
			case State.STARTED:
				switch (npcId)
				{
					case VLADIMIR:
						if (st.isCond(1))
						{
							htmltext = "31302-01a.html";
						}
						break;
					case M_NECROMANCER:
						switch (st.getCond())
						{
							case 1:
								htmltext = "31518-00.html";
								break;
							case 2:
								htmltext = "31518-01a.html";
								break;
						}
						break;
					case HIERARCH:
						if (st.isCond(2))
						{
							htmltext = "31517-00.html";
						}
						break;
				}
				break;
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
		}
		return htmltext;
	}
}
