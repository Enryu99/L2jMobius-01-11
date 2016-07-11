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
package quests.Q10338_SeizeYourDestiny;

import com.l2jmobius.gameserver.enums.CategoryType;
import com.l2jmobius.gameserver.enums.Movie;
import com.l2jmobius.gameserver.enums.Race;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.base.ClassId;
import com.l2jmobius.gameserver.model.holders.ItemHolder;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.network.NpcStringId;
import com.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Seize Your Destiny (10338)
 * @author Sdw
 */
public final class Q10338_SeizeYourDestiny extends Quest
{
	// NPCs
	private static final int CELLPHINE = 33477;
	private static final int HADEL = 33344;
	private static final int HERMUNCUS = 33340;
	// Monsters
	private static final int HARNAKS_WRAITH = 27445;
	// Items
	private static final ItemHolder SCROLL_OF_AFTERLIFE = new ItemHolder(17600, 1);
	private static final ItemHolder STEEL_DOOR_GUILD_COIN = new ItemHolder(37045, 400);
	// Locations
	private static final Location RELIQUARY_OF_THE_GIANT = new Location(-114962, 226564, -2864);
	// Misc
	private static final int MIN_LV = 85;
	
	public Q10338_SeizeYourDestiny()
	{
		super(10338);
		addStartNpc(CELLPHINE);
		addTalkId(CELLPHINE, HADEL, HERMUNCUS);
		addKillId(HARNAKS_WRAITH);
		addCondNotRace(Race.ERTHEIA, "33477-08.html");
		addCondNotClassId(ClassId.JUDICATOR, "");
		addCondIsNotSubClassActive("");
		addCondMinLevel(MIN_LV, "33477-07.html");
		addCondInCategory(CategoryType.FOURTH_CLASS_GROUP, "33477-07.html");
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		
		switch (event)
		{
			case "TELEPORT":
			{
				if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "";
					break;
				}
				player.teleToLocation(RELIQUARY_OF_THE_GIANT, null);
				playMovie(player, Movie.SC_AWAKENING_VIEW);
				break;
			}
			case "33477-03.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "33344-05.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "33340-02.html":
			{
				if (qs.isCond(3))
				{
					showOnScreenMsg(player, NpcStringId.YOU_MAY_USE_SCROLL_OF_AFTERLIFE_FROM_HERMUNCUS_TO_AWAKEN, ExShowScreenMessage.TOP_CENTER, 10000);
					giveItems(player, SCROLL_OF_AFTERLIFE);
					rewardItems(player, STEEL_DOOR_GUILD_COIN);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "33344-02.html":
			case "33344-03.html":
			case "33344-04.html":
			case "33477-02.htm":
			{
				htmltext = event;
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
			case CELLPHINE:
			{
				if (qs.isStarted())
				{
					htmltext = "33477-06.html";
				}
				if (hasQuestItems(player, SCROLL_OF_AFTERLIFE.getId()) || qs.isCompleted())
				{
					htmltext = "33477-05.html";
				}
				else if (qs.isCreated())
				{
					htmltext = "33477-01.htm";
				}
				break;
			}
			case HADEL:
			{
				if (qs.isCompleted() || player.isInCategory(CategoryType.AWAKEN_GROUP) || hasQuestItems(player, SCROLL_OF_AFTERLIFE.getId()))
				{
					htmltext = "33344-07.html";
				}
				else if (player.getLevel() < 85)
				{
					htmltext = "33344-06.html";
				}
				else if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "33344-09.html";
				}
				else
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "33344-01.html";
							break;
						}
						case 2:
						{
							htmltext = "33344-08.html";
							break;
						}
						case 3:
						{
							htmltext = "33344-07.html";
							break;
						}
					}
				}
				break;
			}
			case HERMUNCUS:
			{
				if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "33340-04.html";
					break;
				}
				else if (qs.isCond(3))
				{
					htmltext = "33340-01.html";
					break;
				}
				else if (hasQuestItems(player, SCROLL_OF_AFTERLIFE.getId()))
				{
					htmltext = "33340-03.html";
					break;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		if (npc.getId() == HARNAKS_WRAITH)
		{
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isCond(2))
			{
				qs.setCond(3, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
}