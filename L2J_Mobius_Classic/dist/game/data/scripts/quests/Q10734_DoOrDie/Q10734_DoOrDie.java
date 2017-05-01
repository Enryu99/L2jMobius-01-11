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
package quests.Q10734_DoOrDie;

import com.l2jmobius.gameserver.enums.Race;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.quest.State;
import com.l2jmobius.gameserver.network.NpcStringId;
import com.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2jmobius.gameserver.network.serverpackets.TutorialShowHtml;

import quests.Q10733_TheTestForSurvival.Q10733_TheTestForSurvival;

/**
 * Do Or Die (10734)
 * @author Sdw
 */
public final class Q10734_DoOrDie extends Quest
{
	// NPC's
	private static final int KATALIN = 33943;
	private static final int AYANTHE = 33942;
	private static final int ADVENTURER_S_GUIDE_APPRENTICE = 33950;
	private static final int TRAINING_DUMMY = 19546;
	// Skills
	private static final SkillHolder[] COMMON_BUFFS =
	{
		new SkillHolder(5182, 1), // Blessing of Protection
		new SkillHolder(15642, 1), // Horn Melody
		new SkillHolder(15643, 1), // Drum Melody
		new SkillHolder(15644, 1), // Pipe Organ Melody
		new SkillHolder(15645, 1), // Guitar Melody
		new SkillHolder(15646, 1), // Harp Melody
		new SkillHolder(15647, 1), // Lute Melody
		new SkillHolder(15651, 1), // Prevailing Sonata
		new SkillHolder(15652, 1), // Daring Sonata
		new SkillHolder(15653, 1), // Refreshing Sonata
	};
	private static final SkillHolder WARRIOR_HARMONY = new SkillHolder(15649, 1);
	private static final SkillHolder WIZARD_HARMONY = new SkillHolder(15650, 1);
	// Misc
	private static final int MAX_LEVEL = 20;
	
	public Q10734_DoOrDie()
	{
		super(10734);
		addStartNpc(KATALIN, AYANTHE);
		addTalkId(KATALIN, AYANTHE, ADVENTURER_S_GUIDE_APPRENTICE);
		addKillId(TRAINING_DUMMY);
		addCondRace(Race.ERTHEIA, "");
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "33942-02.htm":
			case "33943-02.htm":
			case "33950-02.html":
				break;
			case "33942-03.htm":
			case "33943-03.htm":
			{
				qs.startQuest();
				showOnScreenMsg(player, NpcStringId.ATTACK_THE_TRAINING_DUMMY, ExShowScreenMessage.TOP_CENTER, 10000);
				break;
			}
			case "other_buffs":
			{
				htmltext = (player.isMageClass()) ? "33950-03.html" : "33950-05.html";
				player.sendPacket(new TutorialShowHtml(npc.getObjectId(), "..\\L2Text\\QT_002_Guide_01.htm", TutorialShowHtml.LARGE_WINDOW));
				break;
			}
			case "buffs":
			{
				if (qs.isCond(4) || qs.isCond(5))
				{
					qs.setCond(6, true);
					showOnScreenMsg(player, NpcStringId.ATTACK_THE_TRAINING_DUMMY, ExShowScreenMessage.TOP_CENTER, 10000);
					htmltext = castBuffs(npc, player, "33950-06.html", "33950-04.html");
				}
				break;
			}
			default:
				htmltext = null;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player, boolean isSimulated)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (npc.getId())
		{
			case KATALIN:
			{
				if (!player.isMageClass())
				{
					switch (qs.getState())
					{
						case State.CREATED:
							htmltext = (meetStartRestrictions(player)) ? "33943-01.htm" : "33943-08.htm";
							break;
						case State.STARTED:
						{
							switch (qs.getCond())
							{
								case 1:
								{
									if (!isSimulated)
									{
										showOnScreenMsg(player, NpcStringId.ATTACK_THE_TRAINING_DUMMY, ExShowScreenMessage.TOP_CENTER, 10000);
									}
									htmltext = "33943-04.html";
									break;
								}
								case 3:
								{
									if (!isSimulated)
									{
										showOnScreenMsg(player, NpcStringId.TALK_TO_THE_APPRENTICE_ADVENTURER_S_GUIDE, ExShowScreenMessage.TOP_CENTER, 10000);
										qs.setCond(5, true);
									}
									htmltext = "33943-05.html";
									break;
								}
								case 5:
								{
									if (!isSimulated)
									{
										showOnScreenMsg(player, NpcStringId.TALK_TO_THE_APPRENTICE_ADVENTURER_S_GUIDE, ExShowScreenMessage.TOP_CENTER, 10000);
									}
									htmltext = "33943-06.html";
									break;
								}
								case 8:
								{
									if (!isSimulated)
									{
										giveAdena(player, 7000, true);
										addExpAndSp(player, 805, 2);
										qs.exitQuest(false, true);
									}
									htmltext = "33943-07.html";
									break;
								}
							}
							break;
						}
						case State.COMPLETED:
							htmltext = getAlreadyCompletedMsg(player);
							break;
					}
				}
				break;
			}
			case AYANTHE:
			{
				if (player.isMageClass())
				{
					switch (qs.getState())
					{
						case State.CREATED:
							htmltext = (meetStartRestrictions(player)) ? "33942-01.htm" : "33942-08.htm";
							break;
						case State.STARTED:
						{
							switch (qs.getCond())
							{
								case 1:
								{
									if (!isSimulated)
									{
										showOnScreenMsg(player, NpcStringId.ATTACK_THE_TRAINING_DUMMY, ExShowScreenMessage.TOP_CENTER, 10000);
									}
									htmltext = "33942-04.html";
									break;
								}
								case 2:
								{
									if (!isSimulated)
									{
										showOnScreenMsg(player, NpcStringId.TALK_TO_THE_APPRENTICE_ADVENTURER_S_GUIDE, ExShowScreenMessage.TOP_CENTER, 10000);
										qs.setCond(4, true);
									}
									htmltext = "33942-05.html";
									break;
								}
								case 4:
								{
									if (!isSimulated)
									{
										showOnScreenMsg(player, NpcStringId.TALK_TO_THE_APPRENTICE_ADVENTURER_S_GUIDE, ExShowScreenMessage.TOP_CENTER, 10000);
									}
									htmltext = "33942-06.html";
									break;
								}
								case 7:
								{
									if (!isSimulated)
									{
										giveAdena(player, 7000, true);
										addExpAndSp(player, 805, 2);
										qs.exitQuest(false, true);
									}
									htmltext = "33942-07.html";
									break;
								}
							}
							break;
						}
						case State.COMPLETED:
							htmltext = getAlreadyCompletedMsg(player);
							break;
					}
				}
				break;
			}
			case ADVENTURER_S_GUIDE_APPRENTICE:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 4:
						case 5:
						{
							htmltext = "33950-01.html";
							break;
						}
						case 6:
						{
							if (!isSimulated)
							{
								showOnScreenMsg(player, NpcStringId.ATTACK_THE_TRAINING_DUMMY, ExShowScreenMessage.TOP_CENTER, 10000);
							}
							htmltext = castBuffs(npc, player, "33950-07.html", "33950-08.html");
							break;
						}
					}
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && (qs.isCond(1) || qs.isCond(6)))
		{
			final int nextCond = (killer.isMageClass()) ? (qs.getCond() + 1) : (qs.getCond() + 2);
			qs.setCond(nextCond, true);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private boolean meetStartRestrictions(L2PcInstance player)
	{
		final QuestState qs = player.getQuestState(Q10733_TheTestForSurvival.class.getSimpleName());
		return ((player.getLevel() < MAX_LEVEL) && (qs != null) && qs.isCompleted());
	}
	
	private String castBuffs(L2Npc npc, L2PcInstance player, String mage, String fighter)
	{
		for (SkillHolder skillHolder : COMMON_BUFFS)
		{
			npc.setTarget(player);
			npc.doCast(skillHolder.getSkill());
		}
		
		npc.setTarget(player);
		if (player.isMageClass())
		{
			npc.doCast(WIZARD_HARMONY.getSkill());
			return mage;
		}
		npc.doCast(WARRIOR_HARMONY.getSkill());
		return fighter;
	}
}
