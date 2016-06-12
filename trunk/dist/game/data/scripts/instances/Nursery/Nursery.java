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
package instances.Nursery;

import com.l2jmobius.gameserver.enums.CategoryType;
import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.instancemanager.ZoneManager;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.instancezone.Instance;
import com.l2jmobius.gameserver.model.skills.BuffInfo;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.zone.L2ZoneType;
import com.l2jmobius.gameserver.model.zone.type.L2ScriptZone;
import com.l2jmobius.gameserver.network.NpcStringId;
import com.l2jmobius.gameserver.network.serverpackets.Earthquake;
import com.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import com.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import instances.AbstractInstance;

/**
 * Nursery instance zone.
 * @author St3eT
 */
public final class Nursery extends AbstractInstance
{
	// NPCs
	private static final int TIE = 33152;
	private static final int MAGUEN = 19037;
	private static final int[] MONSTERS =
	{
		23033, // Failed Creation
		23034, // Failed Creation
		23035, // Failed Creation
		23036, // Failed Creation
		23037, // Failed Creation
	};
	// Items
	private static final int SCORE_ITEM = 17610; // Tissue Energy Residue
	private static final int REWARD_ITEM = 17602; // Tissue Energy Crystal
	// Skill
	private static final SkillHolder ENERGY_SKILL_1 = new SkillHolder(14228, 1);
	private static final SkillHolder ENERGY_SKILL_2 = new SkillHolder(14229, 1);
	private static final SkillHolder ENERGY_SKILL_3 = new SkillHolder(14230, 1);
	private static final SkillHolder MAGUEN_STEAL_SKILL = new SkillHolder(14235, 1);
	// Zones
	private static final L2ScriptZone ENTER_ZONE_1 = ZoneManager.getInstance().getZoneById(23601, L2ScriptZone.class);
	private static final L2ScriptZone ENTER_ZONE_2 = ZoneManager.getInstance().getZoneById(23602, L2ScriptZone.class);
	// Misc
	private static final int TEMPLATE_ID = 171;
	
	public Nursery()
	{
		addStartNpc(TIE);
		addFirstTalkId(TIE);
		addTalkId(TIE);
		addAttackId(MAGUEN);
		addKillId(MONSTERS);
		addSpellFinishedId(MAGUEN);
		addEnterZoneId(ENTER_ZONE_1.getId(), ENTER_ZONE_2.getId());
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, L2Npc npc, L2PcInstance player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isNurseryInstance(instance))
		{
			final StatsSet npcVars = npc.getVariables();
			final int gameStage = npcVars.getInt("GAME_STAGE", 0);
			
			switch (event)
			{
				case "CLOCK_TIMER":
				{
					final int gameTime = npcVars.increaseInt("GAME_TIME", 1500, -1);
					instance.getPlayers().forEach(temp -> temp.sendPacket(new ExSendUIEvent(temp, 3, gameTime, npcVars.getInt("GAME_POINTS", 0), 0, 2042, 0, NpcStringId.ELAPSED_TIME.getId())));
					if (gameStage == 1)
					{
						if (gameTime == 0)
						{
							player = instance.getFirstPlayer();
							if ((player != null) && hasQuestItems(player, SCORE_ITEM))
							{
								final int itemCount = (int) getQuestItemsCount(player, SCORE_ITEM);
								takeItems(player, SCORE_ITEM, itemCount);
								npcVars.increaseInt("GAME_POINTS", 0, itemCount);
							}
							instance.despawnGroup("GAME_MONSTERS");
							npcVars.set("GAME_STAGE", 2);
						}
						else
						{
							getTimers().addTimer("CLOCK_TIMER", 1000, npc, null);
						}
					}
					break;
				}
				case "MAGUEN_WAIT_TIMER":
				{
					npc.getAI().stopFollow();
					addSkillCastDesire(npc, player, MAGUEN_STEAL_SKILL, 23);
					break;
				}
				case "MAGUEN_HIDE_TIMER":
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.OW_SINCE_I_M_DONE_I_M_GONE_OW);
					npc.setTargetable(false);
					npc.setDisplayEffect(5);
					npc.doDie(null);
					npcVars.set("MAGUEN_STATUS", 2);
					break;
				}
			}
		}
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		final Instance instance = npc.getInstanceWorld();
		String htmltext = null;
		
		if (isNurseryInstance(instance))
		{
			final StatsSet npcVars = npc.getVariables();
			
			final int gameStage = npcVars.getInt("GAME_STAGE", 0);
			switch (gameStage)
			{
				case 0:
					htmltext = "GameManager-01.html";
					break;
				case 2:
					htmltext = "GameManager-02.html";
					break;
				case 3:
					htmltext = "GameManager-03.html";
					break;
			}
			
			final BuffInfo energyInfo = player.getEffectList().getBuffInfoByAbnormalType(ENERGY_SKILL_1.getSkill().getAbnormalType());
			final int energyLv = energyInfo == null ? 0 : energyInfo.getSkill().getAbnormalLvl();
			
			if ((energyLv > 0) && (gameStage == 1) && (energyInfo != null))
			{
				int addPoints = 0;
				if (energyLv == 10)
				{
					addPoints = 40;
				}
				else if (energyLv == 11)
				{
					addPoints = 60;
				}
				else if (energyLv == 12)
				{
					addPoints = 80;
				}
				
				npcVars.set("GAME_POINTS", npcVars.getInt("GAME_POINTS", 0) + addPoints);
				showOnScreenMsg(instance, NpcStringId.SOLDIER_TIE_ABSORBED_REPRODUCTIVE_ENERGY_FROM_YOUR_BODY_AND_CONVERTED_S1_PIECES_OF_BIO_ENERGY, ExShowScreenMessage.TOP_CENTER, 3000, String.valueOf(addPoints));
				
				player.getEffectList().stopSkillEffects(true, ENERGY_SKILL_1.getSkill());
				player.getEffectList().stopSkillEffects(true, ENERGY_SKILL_2.getSkill());
				player.getEffectList().stopSkillEffects(true, ENERGY_SKILL_3.getSkill());
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isNurseryInstance(instance))
		{
			final StatsSet npcVars = npc.getVariables();
			final int gameStage = npcVars.getInt("GAME_STAGE", 0);
			
			switch (event)
			{
				case "startGame":
				{
					if (gameStage == 0)
					{
						instance.setReenterTime();
						instance.spawnGroup("GAME_MONSTERS");
						getTimers().addTimer("CLOCK_TIMER", 1000, npc, null);
						npcVars.set("GAME_STAGE", 1);
					}
					break;
				}
				case "calculatePoints":
				{
					if (gameStage == 2)
					{
						final int gamePoints = npcVars.getInt("GAME_POINTS", 0);
						int itemCount = 0;
						if ((gamePoints != 0) && (gamePoints <= 800))
						{
							itemCount = 10;
						}
						else if ((gamePoints > 800) && (gamePoints <= 1600))
						{
							itemCount = 60;
						}
						else if ((gamePoints > 1600) && (gamePoints <= 2000))
						{
							itemCount = 160;
						}
						else if ((gamePoints > 2000) && (gamePoints <= 2400))
						{
							itemCount = 200;
						}
						else if ((gamePoints > 2400) && (gamePoints <= 2800))
						{
							itemCount = 240;
						}
						else if ((gamePoints > 2800) && (gamePoints <= 3200))
						{
							itemCount = 280;
						}
						else if ((gamePoints > 3200) && (gamePoints <= 3600))
						{
							itemCount = 320;
						}
						else if ((gamePoints > 3600) && (gamePoints <= 4000))
						{
							itemCount = 360;
						}
						else if (gamePoints > 4000)
						{
							itemCount = 400;
						}
						
						if (gamePoints != 0)
						{
							giveItems(player, REWARD_ITEM, itemCount);
							addExpAndSp(player, 40000 * gamePoints, 0);
						}
						
						npcVars.set("GAME_STAGE", 3);
						instance.finishInstance(0);
					}
					break;
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isNurseryInstance(instance))
		{
			final StatsSet npcVars = npc.getVariables();
			final int maguenStatus = npcVars.getInt("MAGUEN_STATUS", 0);
			
			switch (maguenStatus)
			{
				case 0:
				{
					npc.setTargetable(false);
					npc.doDie(null);
					break;
				}
				case 1:
				{
					final int returnPoint = (npcVars.getInt("MAGUEN_STOLEN_COUNT", 0) / 2);
					if (returnPoint > 0)
					{
						final L2Npc gameManager = instance.getNpc(TIE);
						if (gameManager != null)
						{
							gameManager.getVariables().increaseInt("GAME_POINTS", returnPoint);
						}
						showOnScreenMsg(instance, NpcStringId.MAGUEN_GETS_SURPRISED_AND_GIVES_S1_PIECES_OF_BIO_ENERGY_RESIDUE, ExShowScreenMessage.MIDDLE_CENTER, 3000, String.valueOf(returnPoint));
						npc.setTargetable(false);
						npc.doDie(null);
					}
					break;
				}
				case 2:
				{
					npc.doDie(null);
					break;
				}
			}
			
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isNurseryInstance(instance))
		{
			if (getRandom(100) < 6)
			{
				switch (getRandom(3))
				{
					case 0:
						ENERGY_SKILL_1.getSkill().applyEffects(npc, killer);
						break;
					case 1:
						ENERGY_SKILL_2.getSkill().applyEffects(npc, killer);
						break;
					case 2:
						ENERGY_SKILL_3.getSkill().applyEffects(npc, killer);
						break;
				}
				instance.broadcastPacket(new Earthquake(npc, 50, 3));
				showOnScreenMsg(instance, NpcStringId.RECEIVED_REGENERATION_ENERGY, ExShowScreenMessage.MIDDLE_CENTER, 2000);
			}
			
			if (getRandom(100) < 4)
			{
				final L2Npc maguen = addSpawn(MAGUEN, npc, false, 0, false, instance.getId());
				maguen.setIsRunning(true);
				maguen.getAI().startFollow(killer);
				showOnScreenMsg(instance, NpcStringId.MAGUEN_APPEARANCE, ExShowScreenMessage.MIDDLE_CENTER, 4000);
				getTimers().addTimer("MAGUEN_WAIT_TIMER", 4000, maguen, killer);
				getTimers().addTimer("MAGUEN_HIDE_TIMER", 60000, maguen, null);
			}
			
			if ((getRandom(10) + 1) < 10)
			{
				int pointsCount = getRandom(6) + 3;
				
				if (killer.isInCategory(CategoryType.SIGEL_GROUP) || killer.isInCategory(CategoryType.AEORE_GROUP))
				{
					pointsCount += 6;
				}
				else if (killer.isInCategory(CategoryType.TYRR_GROUP))
				{
					pointsCount -= 1;
				}
				else if (killer.isInCategory(CategoryType.OTHELL_GROUP))
				{
					pointsCount += 2;
				}
				else if (killer.isInCategory(CategoryType.YUL_GROUP))
				{
					pointsCount += 1;
				}
				else if (killer.isInCategory(CategoryType.FEOH_GROUP) || killer.isInCategory(CategoryType.ISS_GROUP))
				{
					pointsCount += 0;
				}
				else if (killer.isInCategory(CategoryType.WYNN_GROUP))
				{
					pointsCount += 3;
				}
				
				final L2Npc gameManager = instance.getNpc(TIE);
				if (gameManager != null)
				{
					gameManager.getVariables().increaseInt("GAME_POINTS", pointsCount);
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, Skill skill)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isNurseryInstance(instance))
		{
			final L2Npc gameManager = instance.getNpc(TIE);
			if (gameManager != null)
			{
				final StatsSet managerVars = gameManager.getVariables();
				final StatsSet npcVars = npc.getVariables();
				final int gamePoints = managerVars.getInt("GAME_POINTS", 0);
				
				if (gamePoints > 0)
				{
					int decreasePoints = 0;
					if (gamePoints > 100)
					{
						decreasePoints = getRandom(80) + 21;
					}
					else
					{
						decreasePoints = getRandom(gamePoints) + 1;
					}
					npc.setTargetable(true);
					managerVars.increaseInt("GAME_POINTS", decreasePoints * -1);
					showOnScreenMsg(instance, NpcStringId.MAGUEN_STOLE_S1_PIECES_OF_BIO_ENERGY_RESIDUE, ExShowScreenMessage.MIDDLE_CENTER, 4000, String.valueOf(decreasePoints));
					npcVars.set("MAGUEN_STOLEN_COUNT", decreasePoints);
					npcVars.set("MAGUEN_STATUS", 1);
					
					if (decreasePoints > 50)
					{
						if (getRandom(100) < 20)
						{
							npcVars.set("MAGUEN_STATUS", 2);
							npc.setTargetable(false);
							npc.doDie(null);
						}
						else
						{
							npc.setDisplayEffect(4);
							getTimers().cancelTimer("MAGUEN_HIDE_TIMER", npc, null);
							getTimers().addTimer("MAGUEN_HIDE_TIMER", getRandom(3000), npc, null);
						}
					}
					else
					{
						npc.setDisplayEffect(4);
						getTimers().cancelTimer("MAGUEN_HIDE_TIMER", npc, null);
						getTimers().addTimer("MAGUEN_HIDE_TIMER", getRandom(3000), npc, null);
					}
				}
				else
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.PFFT_THIS_ONE_IS_A_MISS_I_WASTED_TOO_MUCH_STRENGTH_WHOA);
					npc.setTargetable(false);
					npc.doDie(null);
				}
			}
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		if (character.isPlayer())
		{
			enterInstance(character.getActingPlayer(), null, TEMPLATE_ID);
		}
		return super.onEnterZone(character, zone);
	}
	
	private boolean isNurseryInstance(Instance instance)
	{
		return ((instance != null) && (instance.getTemplateId() == TEMPLATE_ID));
	}
	
	public static void main(String[] args)
	{
		new Nursery();
	}
}