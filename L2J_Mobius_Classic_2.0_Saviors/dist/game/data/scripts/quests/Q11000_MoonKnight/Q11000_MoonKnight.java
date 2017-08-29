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
package quests.Q11000_MoonKnight;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.enums.QuestSound;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.events.EventType;
import com.l2jmobius.gameserver.model.events.ListenerRegisterType;
import com.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import com.l2jmobius.gameserver.model.events.annotations.RegisterType;
import com.l2jmobius.gameserver.model.events.impl.character.player.OnPlayerLevelChanged;
import com.l2jmobius.gameserver.model.events.impl.character.player.OnPlayerLogin;
import com.l2jmobius.gameserver.model.events.impl.character.player.OnPlayerPressTutorialMark;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.network.serverpackets.TutorialShowHtml;
import com.l2jmobius.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * Moon Knight (11000)
 * @author Stayway
 */
public class Q11000_MoonKnight extends Quest
{
	// NPC
	private static final int JONES = 30939;
	private static final int DAMION = 30208;
	private static final int AMORA = 30940;
	private static final int NETI = 30425;
	private static final int ROLENTO = 30437;
	private static final int GUDZ = 30941;
	// Monsters
	private static final int OL_MAHUM_THIEF = 27201;
	private static final int TUREK_ORC_COMMANDER = 27202;
	private static final int TUREK_ORC_INVADER = 27203;
	// Rewards
	private static final int MOON_HELMET = 7850;
	private static final int MOON_ARMOR = 7851;
	private static final int MOON_GAUNTLETS_HEAVY = 7852;
	private static final int MOON_BOOTS_HEAVY = 7853;
	private static final int MOON_SHELL = 7854;
	private static final int MOON_LEATHER_GLOVES = 7855;
	private static final int MOON_SHOES = 7856;
	private static final int MOON_CAPE = 7857;
	private static final int MOON_SILK_GLOVES = 7858;
	private static final int MOON_SANDALS = 7859;
	// Items
	private static final int MOLD = 49555;
	private static final int AMORA_RECEIPT = 49556;
	private static final int ARMOR_TRADE_CONTRACT = 49557;
	private static final int TUREK_ORC_ORDER = 49558;
	private static final int TUREK_ORC_INVADER_HEAD = 49561;
	private static final int ROLENTO_BAG = 49559;
	private static final int IRON_SCALE_GUILD_CERTIFICATE = 49560;
	// Misc
	private static final int QUESTION_MARK_ID = 18;
	private static final int MIN_LVL = 25;
	private static final int MAX_LVL = 40;
	
	public Q11000_MoonKnight()
	{
		super(11000);
		addStartNpc(JONES);
		addTalkId(JONES, DAMION, AMORA, NETI, ROLENTO, GUDZ);
		addKillId(OL_MAHUM_THIEF, TUREK_ORC_COMMANDER, TUREK_ORC_INVADER);
		addCondLevel(MIN_LVL, MAX_LVL, "no_level.html");
		registerQuestItems(MOLD, AMORA_RECEIPT, ARMOR_TRADE_CONTRACT, TUREK_ORC_ORDER, TUREK_ORC_INVADER_HEAD);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs != null)
		{
			switch (event)
			{
				case "30208-01.html":
				case "30208-02.html":
				case "30437-02.html":
				case "30941-02.html":
				case "30941-03.html":
				{
					htmltext = event;
					break;
				}
				case "30939-02.htm":
				{
					qs.startQuest();
					htmltext = event;
					break;
				}
				case "30939-04.html":
				{
					if (qs.isCond(4))
					{
						qs.setCond(5, true);
						htmltext = event;
					}
					break;
				}
				case "30939-06.html":
				{
					if (qs.isCond(5))
					{
						qs.setCond(6, true);
						htmltext = event;
					}
					break;
				}
				case "30425-01.html":
				{
					if (qs.isCond(6))
					{
						qs.setCond(7, true);
						htmltext = event;
					}
					break;
				}
				case "30437-03.html":
				{
					if (qs.isCond(7))
					{
						qs.setCond(8, true);
						takeItems(player, ARMOR_TRADE_CONTRACT, 1);
						giveItems(player, ROLENTO_BAG, 1);
						giveItems(player, IRON_SCALE_GUILD_CERTIFICATE, 1);
						htmltext = event;
					}
					break;
				}
				case "30941-04.html":
				{
					if (qs.isCond(8))
					{
						qs.setCond(9, true);
						takeItems(player, TUREK_ORC_ORDER, 1);
						takeItems(player, ROLENTO_BAG, 1);
						takeItems(player, IRON_SCALE_GUILD_CERTIFICATE, 1);
						htmltext = event;
					}
					break;
				}
				case "reward1":
				{
					if (qs.isCond(10))
					{
						giveItems(player, MOON_HELMET, 1);
						giveItems(player, MOON_SHELL, 1);
						giveItems(player, MOON_LEATHER_GLOVES, 1);
						giveItems(player, MOON_SHOES, 1);
						qs.exitQuest(false, true);
					}
					break;
				}
				case "reward2":
				{
					if (qs.isCond(10))
					{
						giveItems(player, MOON_HELMET, 1);
						giveItems(player, MOON_ARMOR, 1);
						giveItems(player, MOON_GAUNTLETS_HEAVY, 1);
						giveItems(player, MOON_BOOTS_HEAVY, 1);
						qs.exitQuest(false, true);
					}
					break;
				}
				case "reward3":
				{
					if (qs.isCond(10))
					{
						giveItems(player, MOON_HELMET, 1);
						giveItems(player, MOON_CAPE, 1);
						giveItems(player, MOON_SILK_GLOVES, 1);
						giveItems(player, MOON_SANDALS, 1);
						qs.exitQuest(false, true);
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			switch (npc.getId())
			{
				case OL_MAHUM_THIEF:
				{
					if (qs.isCond(2))
					{
						giveItemRandomly(killer, npc, MOLD, 1, 10, 1, true);
					}
					break;
				}
				case TUREK_ORC_COMMANDER:
				{
					if (qs.isCond(5))
					{
						giveItemRandomly(killer, npc, ARMOR_TRADE_CONTRACT, 1, 1, 0.25, true);
						giveItemRandomly(killer, npc, TUREK_ORC_ORDER, 1, 1, 0.25, true);
					}
					break;
				}
				case TUREK_ORC_INVADER:
				{
					if (qs.isCond(9))
					{
						giveItemRandomly(killer, npc, TUREK_ORC_INVADER_HEAD, 1, 10, 1, true);
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (npc.getId())
		{
			case JONES:
			{
				if (qs.isCreated())
				{
					htmltext = "30939-01.htm";
				}
				else if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						case 2:
						case 3:
						{
							htmltext = "30939-02a.html";
							break;
						}
						case 4:
						{
							htmltext = "30939-03.html";
							break;
						}
						case 5:
						{
							if ((getQuestItemsCount(talker, TUREK_ORC_ORDER) < 1) || (getQuestItemsCount(talker, ARMOR_TRADE_CONTRACT) < 1))
							{
								htmltext = "30939-04a.html";
							}
							else if ((getQuestItemsCount(talker, TUREK_ORC_ORDER) >= 1) && (getQuestItemsCount(talker, ARMOR_TRADE_CONTRACT) >= 1))
							{
								htmltext = "30939-05.html";
							}
							break;
						}
						case 6:
						{
							htmltext = "30939-06a.html";
							break;
						}
						case 10:
						{
							htmltext = "30939-07.html";
							break;
						}
					}
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(talker);
				}
				break;
			}
			case DAMION:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30208-01.html";
						qs.setCond(2, true);
						break;
					}
					case 2:
					{
						htmltext = "30208-01.html";
						break;
					}
					case 3:
					{
						if (hasQuestItems(talker, AMORA_RECEIPT))
						{
							takeItems(talker, AMORA_RECEIPT, 1);
							htmltext = "30208-03.html";
							qs.setCond(4, true);
						}
						break;
					}
					case 4:
					{
						htmltext = "30208-03a.html";
						break;
					}
				}
				break;
			}
			case AMORA:
			{
				if (qs.isCond(2) && (getQuestItemsCount(talker, MOLD) >= 10))
				{
					giveItems(talker, AMORA_RECEIPT, 1);
					takeItems(talker, MOLD, 10);
					qs.setCond(3, true);
					htmltext = "30940-01.html";
				}
				else if (qs.isCond(3))
				{
					htmltext = "30940-01a.html";
				}
				break;
			}
			case NETI:
			{
				if (qs.isCond(6) && hasQuestItems(talker, TUREK_ORC_ORDER, ARMOR_TRADE_CONTRACT))
				{
					htmltext = "30425-01.html";
					qs.setCond(7, true);
				}
				else if (qs.isCond(7))
				{
					htmltext = "30425-01a.html"; // Need retail html
				}
				break;
			}
			case ROLENTO:
			{
				if (qs.isCond(7) && hasQuestItems(talker, TUREK_ORC_ORDER, ARMOR_TRADE_CONTRACT))
				{
					htmltext = "30437-01.html";
				}
				else if (qs.isCond(8))
				{
					htmltext = "30437-03a.html";
				}
				break;
			}
			case GUDZ:
			{
				switch (qs.getCond())
				{
					case 8:
					{
						if (hasQuestItems(talker, TUREK_ORC_ORDER, ROLENTO_BAG, IRON_SCALE_GUILD_CERTIFICATE))
						{
							htmltext = "30941-01.html";
						}
						break;
					}
					case 9:
					{
						if (getQuestItemsCount(talker, TUREK_ORC_INVADER_HEAD) >= 10)
						{
							htmltext = "30941-05.html";
							takeItems(talker, TUREK_ORC_INVADER_HEAD, 10);
							qs.setCond(10, true);
						}
						else
						{
							htmltext = "30941-04a.html";
						}
						break;
					}
					case 10:
					{
						htmltext = "30941-05a.html";
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return;
		}
		
		final L2PcInstance player = event.getActiveChar();
		final QuestState qs = getQuestState(player, false);
		
		if ((qs == null) && (event.getOldLevel() < event.getNewLevel()) && canStartQuest(player))
		{
			player.sendPacket(new TutorialShowQuestionMark(QUESTION_MARK_ID, 1));
			playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return;
		}
		
		final L2PcInstance player = event.getActiveChar();
		final QuestState qs = getQuestState(player, false);
		
		if ((qs == null) && canStartQuest(player))
		{
			player.sendPacket(new TutorialShowQuestionMark(QUESTION_MARK_ID, 1));
			playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		final L2PcInstance player = event.getActiveChar();
		if ((event.getMarkId() == QUESTION_MARK_ID) && canStartQuest(player))
		{
			final String html = getHtm(player.getHtmlPrefix(), "popup.html");
			player.sendPacket(new TutorialShowHtml(html));
		}
	}
}