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
package instances.KartiasLabyrinth;

import java.util.List;

import com.l2jmobius.commons.util.CommonUtil;
import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.instancemanager.WalkingManager;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureDeath;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureSee;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.instancezone.Instance;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.quest.State;
import com.l2jmobius.gameserver.model.zone.L2ZoneType;
import com.l2jmobius.gameserver.network.NpcStringId;
import com.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import instances.AbstractInstance;
import quests.Q00494_IncarnationOfGreedZellakaGroup.Q00494_IncarnationOfGreedZellakaGroup;
import quests.Q00495_IncarnationOfJealousyPellineGroup.Q00495_IncarnationOfJealousyPellineGroup;
import quests.Q00496_IncarnationOfGluttonyKaliosGroup.Q00496_IncarnationOfGluttonyKaliosGroup;
import quests.Q00497_IncarnationOfGreedZellakaSolo.Q00497_IncarnationOfGreedZellakaSolo;
import quests.Q00498_IncarnationOfJealousyPellineSolo.Q00498_IncarnationOfJealousyPellineSolo;
import quests.Q00499_IncarnationOfGluttonyKaliosSolo.Q00499_IncarnationOfGluttonyKaliosSolo;

/**
 * Kartia Labyrinth instance zone.
 * @author St3eT
 */
public final class KartiasLabyrinth extends AbstractInstance
{
	// NPCs
	private static final int KARTIA_RESEARCHER = 33647;
	private static final int BOZ_ENERGY = 18830;
	private static final int[] ADOLPH =
	{
		33608,
		33619,
		33630,
	};
	private static final int[] BARTON =
	{
		33610,
		33621,
		33632,
	};
	private static final int[] ELISE =
	{
		33616,
		33627,
		33638,
	};
	private static final int[] ELIYAH =
	{
		33614,
		33625,
		33636,
	};
	private static final int[] HAYUK =
	{
		33612,
		33623,
		33634,
	};
	// @formatter:off
	private static final int[] MONSTERS =
	{
		19220, 19221, 19222, // Solo 85
		19223, 19224, 19225, // Solo 90
		19226, 19227, 19228, // Solo 95
		19229, 19230, 19231, // Group 85
		19232, 19233, 19234, // Group 90
		19235, 19236, 19237, // Group 95
	};
	// @formatter:on
	private static final int[] BOSSES =
	{
		19253, // Zellaka (Solo 85)
		25882, // Zellaka (Group 85)
		19254, // Pelline (Solo 90)
		25883, // Pelline (Group 90)
		19255, // Kalios (Solo 95)
		25884, // Kalios (Group 95)
	};
	private static final int[] MINI_BOSSES =
	{
		19222, // Kartia Dimensional Watchman (solo 85)
		19225, // Kartia Dimensional Watchman (solo 90)
		19228, // Kartia Dimensional Watchman (solo 95)
		19231, // Kartia Dimensional Watchman (group 85)
		19234, // Kartia Dimensional Watchman (group 90)
		19237, // Kartia Dimensional Watchman (group 95)
	};
	private static final int[] MIRRORS =
	{
		33798, // Life Plunderer (85)
		33799, // Life Plunderer (90)
		33800, // Life Plunderer (95)
	};
	private static final int[] PRISONERS =
	{
		33641, // Kartia Prisoner (85)
		33643, // Kartia Prisoner (90)
		33645, // Kartia Prisoner (95)
	};
	// Skills
	private static final SkillHolder MIRROR_SKILL_1 = new SkillHolder(15401, 1);
	private static final SkillHolder MIRROR_SKILL_2 = new SkillHolder(14065, 1);
	private static final SkillHolder BOSS_STONE = new SkillHolder(15155, 1);
	private static final SkillHolder PRISONER_HOLD = new SkillHolder(14988, 1);
	private static final SkillHolder PRISONER_CLEANSE = new SkillHolder(14992, 1);
	// Zones
	private static final int KARTIA_85_DETECT_1 = 12020;
	private static final int KARTIA_85_DETECT_2 = 12021;
	private static final int KARTIA_85_TELEPORT_1 = 12022;
	private static final int KARTIA_85_TELEPORT_2 = 12023;
	private static final int KARTIA_85_TELEPORT_3 = 12024;
	private static final int KARTIA_90_DETECT_1 = 12025;
	private static final int KARTIA_90_DETECT_2 = 12026;
	private static final int KARTIA_90_TELEPORT_1 = 12027;
	private static final int KARTIA_90_TELEPORT_2 = 12028;
	private static final int KARTIA_90_TELEPORT_3 = 12029;
	private static final int KARTIA_95_DETECT_1 = 12030;
	private static final int KARTIA_95_DETECT_2 = 12031;
	private static final int KARTIA_95_TELEPORT_1 = 12032;
	private static final int KARTIA_95_TELEPORT_2 = 12033;
	private static final int KARTIA_95_TELEPORT_3 = 12034;
	// Misc
	private static final int TEMPLATE_ID_SOLO_85 = 205;
	private static final int TEMPLATE_ID_SOLO_90 = 206;
	private static final int TEMPLATE_ID_SOLO_95 = 207;
	private static final int TEMPLATE_ID_GROUP_85 = 208;
	private static final int TEMPLATE_ID_GROUP_90 = 209;
	private static final int TEMPLATE_ID_GROUP_95 = 210;
	
	public KartiasLabyrinth()
	{
		super(TEMPLATE_ID_SOLO_85, TEMPLATE_ID_SOLO_90, TEMPLATE_ID_SOLO_95, TEMPLATE_ID_GROUP_85, TEMPLATE_ID_GROUP_90, TEMPLATE_ID_GROUP_95);
		addStartNpc(KARTIA_RESEARCHER);
		addFirstTalkId(KARTIA_RESEARCHER);
		addFirstTalkId(ADOLPH);
		addFirstTalkId(BARTON);
		addFirstTalkId(ELISE);
		addFirstTalkId(ELIYAH);
		addFirstTalkId(HAYUK);
		addTalkId(ADOLPH);
		addTalkId(KARTIA_RESEARCHER);
		addSpawnId(BOZ_ENERGY);
		addSpawnId(BOSSES);
		addAttackId(MINI_BOSSES);
		addAttackId(MIRRORS);
		addMoveFinishedId(MINI_BOSSES);
		addMoveFinishedId(PRISONERS);
		addRouteFinishedId(MONSTERS);
		setCreatureKillId(this::onCreatureKill, MONSTERS);
		setCreatureKillId(this::onBossKill, BOSSES);
		setCreatureSeeId(this::onCreatureSee, MONSTERS);
		addEnterZoneId(KARTIA_85_DETECT_1, KARTIA_85_DETECT_2, KARTIA_85_TELEPORT_1, KARTIA_85_TELEPORT_2, KARTIA_85_TELEPORT_3);
		addEnterZoneId(KARTIA_90_DETECT_1, KARTIA_90_DETECT_2, KARTIA_90_TELEPORT_1, KARTIA_90_TELEPORT_2, KARTIA_90_TELEPORT_3);
		addEnterZoneId(KARTIA_95_DETECT_1, KARTIA_95_DETECT_2, KARTIA_95_TELEPORT_1, KARTIA_95_TELEPORT_2, KARTIA_95_TELEPORT_3);
		addInstanceCreatedId(TEMPLATE_ID_SOLO_85, TEMPLATE_ID_SOLO_90, TEMPLATE_ID_SOLO_95, TEMPLATE_ID_GROUP_85, TEMPLATE_ID_GROUP_90, TEMPLATE_ID_GROUP_95);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		switch (event)
		{
			case "33647-1.htm":
			case "33647-2.htm":
			case "33647-3.htm":
			case "33647-4.htm":
			{
				return event;
			}
			case "request_zellaka_solo":
			{
				enterInstance(player, npc, TEMPLATE_ID_SOLO_85);
				return null;
			}
			case "request_pelline_solo":
			{
				enterInstance(player, npc, TEMPLATE_ID_SOLO_90);
				return null;
			}
			case "request_kalios_solo":
			{
				enterInstance(player, npc, TEMPLATE_ID_SOLO_95);
				return null;
			}
			case "request_zellaka_party":
			{
				enterInstance(player, npc, TEMPLATE_ID_GROUP_85);
				return null;
			}
			case "request_pelline_party":
			{
				enterInstance(player, npc, TEMPLATE_ID_GROUP_90);
				return null;
			}
			case "request_kalios_party":
			{
				enterInstance(player, npc, TEMPLATE_ID_GROUP_95);
				return null;
			}
			default:
			{
				final Instance instance = npc.getInstanceWorld();
				if (instance != null)
				{
					switch (event)
					{
						case "adolph-01.html":
						case "adolph-03.html":
						{
							return event;
						}
						case "sendBarton":
						case "sendHayuk":
						case "sendEliyah":
						case "sendElise":
						{
							if (npc.isScriptValue(0))
							{
								npc.setScriptValue(1);
								instance.openCloseDoor(instance.getTemplateParameters().getInt("firstDoorId"), true);
								instance.spawnGroup("HELPERS").stream().filter(n -> n.getId() == instance.getTemplateParameters().getInt(event.replace("send", "helper"))).forEach(n -> n.deleteMe());
								getTimers().addTimer("TELEPORT_PLAYER", 3000, npc, player);
								return "adolph-04.html";
							}
							return "adolph-02.html";
						}
					}
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, L2Npc npc, L2PcInstance player)
	{
		final Instance instance = (npc != null) ? npc.getInstanceWorld() : player.getInstanceWorld();
		if (instance != null)
		{
			switch (event)
			{
				case "TELEPORT_PLAYER":
				{
					instance.openCloseDoor(instance.getTemplateParameters().getInt("firstDoorId"), false);
					instance.setStatus(1); // Used for notify helper's AI
					player.teleToLocation(instance.getTemplateParameters().getLocation("playerLoc"));
					manageProgressInInstance(instance);
					break;
				}
				case "MOVE_TO_MIDDLE":
				{
					if (npc != null)
					{
						if (npc.getInstanceWorld().getParameters().getInt("ROOM", 1) <= 2)
						{
							final Location loc = instance.getTemplateParameters().getLocation("middlePointRoom1");
							final Location moveTo = new Location(loc.getX() + getRandom(-100, 100), loc.getY() + getRandom(-100, 100), loc.getZ());
							npc.setIsRunning(true);
							addMoveToDesire(npc, moveTo, 6);
							getTimers().addTimer("START_MOVE", 15000, npc, null);
						}
						else if (npc.getInstanceWorld().getParameters().getInt("ROOM", 1) == 3)
						{
							final Location loc = instance.getTemplateParameters().getLocation("middlePointRoom3");
							final Location moveTo = new Location(loc.getX() + getRandom(-200, 200), loc.getY() + getRandom(-200, 200), loc.getZ());
							npc.setIsRunning(true);
							addMoveToDesire(npc, moveTo, 23);
						}
					}
					break;
				}
				case "START_MOVE":
				{
					if (npc != null)
					{
						WalkingManager.getInstance().startMoving(npc, instance.getTemplateParameters().getString(getRandomBoolean() ? "route1" : "route2"));
						getTimers().addTimer("CHANGE_TARGETABLE_STATUS", 5000, npc, null);
					}
					break;
				}
				case "CHANGE_TARGETABLE_STATUS":
				{
					if (npc != null)
					{
						npc.setTargetable(true);
					}
					break;
				}
				case "START_3RD_ROOM":
				{
					instance.openCloseDoor(instance.getTemplateParameters().getInt("thirdDoorId"), false);
					instance.getAliveNpcs(MONSTERS).forEach(n -> n.doDie(null));
					getTimers().addTimer("CALL_PROGRESS", 1000, n -> manageProgressInInstance(instance));
					instance.getParameters().set("TELEPORT_3_ENABLED", true);
					break;
				}
				case "MIRROR_DESPAWN":
				{
					showOnScreenMsg(instance, NpcStringId.THE_LIFE_PLUNDERER_HAS_DISAPPEARED, ExShowScreenMessage.TOP_CENTER, 5000, true);
					manageProgressInInstance(instance);
					break;
				}
			}
		}
	}
	
	@Override
	public void onInstanceCreated(Instance instance, L2PcInstance player)
	{
		instance.spawnGroup("PRISONERS").forEach(npc ->
		{
			final SkillHolder poison = npc.getParameters().getSkillHolder("poison_skill");
			poison.getSkill().applyEffects(npc, npc);
			PRISONER_HOLD.getSkill().applyEffects(npc, npc);
			
		});
		
		instance.getParameters().set("TELEPORT_1_ENABLED", true);
		if (!isSoloKartia(instance))
		{
			getTimers().addTimer("CALL_PROGRESS", 2500, n -> manageProgressInInstance(instance));
		}
	}
	
	public void onCreatureKill(OnCreatureDeath event)
	{
		final L2Npc npc = (L2Npc) event.getTarget();
		final Instance instance = npc.getInstanceWorld();
		
		if (instance != null)
		{
			final StatsSet param = instance.getParameters();
			if (param.getBoolean("BOSS_KILL_OPEN_DOOR", false) && CommonUtil.contains(MINI_BOSSES, npc.getId()))
			{
				instance.setParameter("BOSS_KILL_OPEN_DOOR", true);
				instance.openCloseDoor(instance.getTemplateParameters().getInt("thirdDoorId"), true);
				instance.setStatus(3); // Used for notify helper's AI
			}
			else if (param.getBoolean("CONTINUE_AFTER_KILL", false) && (instance.getAliveNpcs(MONSTERS).size() <= 1))
			{
				param.set("CONTINUE_AFTER_KILL", false);
				getTimers().addTimer("CALL_PROGRESS", 5000, n -> manageProgressInInstance(instance));
			}
		}
	}
	
	public void onBossKill(OnCreatureDeath event)
	{
		final L2Npc npc = (L2Npc) event.getTarget();
		final Instance instance = npc.getInstanceWorld();
		
		if (instance != null)
		{
			if (isSoloKartia(instance))
			{
				final StatsSet tempParam = instance.getTemplateParameters();
				final int xp = tempParam.getInt("soloEXP");
				final int xp_rnd = tempParam.getInt("SoloEXP_Rand");
				final int sp = tempParam.getInt("SoloSP");
				final int sp_rnd = tempParam.getInt("SoloSP_Rand");
				
				instance.getPlayers().forEach(player ->
				{
					addExpAndSp(player, (xp + getRandom(xp_rnd)), (sp + getRandom(sp_rnd)));
				});
			}
			
			// Check Instance Quests.
			instance.getPlayers().forEach(player ->
			{
				switch (instance.getTemplateId())
				{
					case TEMPLATE_ID_SOLO_85:
					{
						final QuestState qs = player.getQuestState(Q00497_IncarnationOfGreedZellakaSolo.class.getSimpleName());
						if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(1))
						{
							qs.setCond(2, true);
						}
						break;
					}
					case TEMPLATE_ID_SOLO_90:
					{
						final QuestState qs = player.getQuestState(Q00498_IncarnationOfJealousyPellineSolo.class.getSimpleName());
						if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(1))
						{
							qs.setCond(2, true);
						}
						break;
					}
					case TEMPLATE_ID_SOLO_95:
					{
						final QuestState qs = player.getQuestState(Q00499_IncarnationOfGluttonyKaliosSolo.class.getSimpleName());
						if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(1))
						{
							qs.setCond(2, true);
						}
						break;
					}
					case TEMPLATE_ID_GROUP_85:
					{
						final QuestState qs = player.getQuestState(Q00494_IncarnationOfGreedZellakaGroup.class.getSimpleName());
						if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(1))
						{
							qs.setCond(2, true);
						}
						break;
					}
					case TEMPLATE_ID_GROUP_90:
					{
						final QuestState qs = player.getQuestState(Q00495_IncarnationOfJealousyPellineGroup.class.getSimpleName());
						if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(1))
						{
							qs.setCond(2, true);
						}
						break;
					}
					case TEMPLATE_ID_GROUP_95:
					{
						final QuestState qs = player.getQuestState(Q00496_IncarnationOfGluttonyKaliosGroup.class.getSimpleName());
						if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(1))
						{
							qs.setCond(2, true);
						}
						break;
					}
				}
			});
			
			instance.finishInstance();
		}
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			if (CommonUtil.contains(MINI_BOSSES, npc.getId()))
			{
				if (npc.isScriptValue(0) && (npc.getCurrentHpPercent() < 50) && instance.getParameters().getBoolean("BOSS_CAN_ESCAPE", false))
				{
					instance.setParameter("BOSS_CAN_ESCAPE", false);
					npc.setScriptValue(1);
					npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.NOT_BAD_FOR_A_BUNCH_OF_HUMANS_I_M_LEAVING);
					npc.disableCoreAI(true);
					addMoveToDesire(npc, instance.getTemplateParameters().getLocation("bossEscapeLoc1"), 23);
				}
			}
			else
			{
				npc.setUndying(true);
				if (npc.isScriptValue(0) && (npc.getCurrentHpPercent() < 10))
				{
					npc.setScriptValue(1);
					showOnScreenMsg(instance, NpcStringId.BURNING_BLOOD_S_EFFECT_IS_FELT, ExShowScreenMessage.TOP_CENTER, 5000, true);
					MIRROR_SKILL_1.getSkill().applyEffects(attacker, attacker);
					MIRROR_SKILL_2.getSkill().applyEffects(attacker, attacker);
					manageProgressInInstance(instance);
					getTimers().addTimer("SUICIDE", 5000, n -> npc.doDie(null));
					getTimers().cancelTimer("MIRROR_DESPAWN", npc, null);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		final Instance instance = character.getInstanceWorld();
		if ((instance != null) && character.isPlayer())
		{
			final L2PcInstance player = character.getActingPlayer();
			switch (zone.getId())
			{
				case KARTIA_85_DETECT_1:
				case KARTIA_90_DETECT_1:
				case KARTIA_95_DETECT_1:
				{
					if (instance.getParameters().getBoolean("SECOND_ROOM_OPENED", true))
					{
						instance.getParameters().set("SECOND_ROOM_OPENED", false);
						getTimers().addTimer("CLOSE_SECOND_DOORS", 20000, n ->
						{
							instance.openCloseDoor(instance.getTemplateParameters().getInt("secondDoorId"), false);
							instance.getParameters().set("TELEPORT_2_ENABLED", true);
						});
					}
					break;
				}
				case KARTIA_85_DETECT_2:
				case KARTIA_90_DETECT_2:
				case KARTIA_95_DETECT_2:
				{
					if (instance.getParameters().getBoolean("LAST_ROOM_OPENED", true))
					{
						instance.getParameters().set("LAST_ROOM_OPENED", false);
						getTimers().addTimer("START_3RD_ROOM", 10000, null, character.getActingPlayer());
					}
					break;
				}
				case KARTIA_85_TELEPORT_1:
				case KARTIA_90_TELEPORT_1:
				case KARTIA_95_TELEPORT_1:
				{
					if (instance.getParameters().getBoolean("TELEPORT_1_ENABLED", false))
					{
						player.teleToLocation(instance.getTemplateParameters().getLocation("teleportZone1_loc"));
					}
					break;
				}
				case KARTIA_85_TELEPORT_2:
				case KARTIA_90_TELEPORT_2:
				case KARTIA_95_TELEPORT_2:
				{
					if (instance.getParameters().getBoolean("TELEPORT_2_ENABLED", false))
					{
						player.teleToLocation(instance.getTemplateParameters().getLocation("teleportZone2_loc"));
					}
					break;
				}
				case KARTIA_85_TELEPORT_3:
				case KARTIA_90_TELEPORT_3:
				case KARTIA_95_TELEPORT_3:
				{
					if (instance.getParameters().getBoolean("TELEPORT_3_ENABLED", false))
					{
						player.teleToLocation(instance.getTemplateParameters().getLocation("teleportZone3_loc"));
					}
					break;
				}
			}
		}
		return super.onEnterZone(character, zone);
	}
	
	@Override
	public void onMoveFinished(L2Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			if (CommonUtil.contains(PRISONERS, npc.getId()))
			{
				if (npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					final Location moveTo = new Location(npc.getX() + getRandom(-500, 500), npc.getY() + getRandom(-500, 500), npc.getZ());
					addMoveToDesire(npc, moveTo, 23);
				}
				else
				{
					npc.deleteMe();
				}
			}
			else if (npc.isScriptValue(1))
			{
				npc.setScriptValue(2);
				addMoveToDesire(npc, instance.getTemplateParameters().getLocation("bossEscapeLoc2"), 23);
			}
			else if (npc.isScriptValue(2))
			{
				instance.setParameter("MINIBOSS_SURVIVED", true);
				instance.openCloseDoor(instance.getTemplateParameters().getInt("thirdDoorId"), true);
				instance.setStatus(3); // Used for notify helper's AI
				npc.deleteMe();
			}
		}
		super.onMoveFinished(npc);
	}
	
	@Override
	public void onRouteFinished(L2Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			final Location moveTo = new Location(npc.getX() + getRandom(-100, 100), npc.getY() + getRandom(-100, 100), npc.getZ());
			npc.setRandomWalking(true);
			addMoveToDesire(npc, moveTo, 0);
		}
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			if (npc.getId() == BOZ_ENERGY)
			{
				npc.setDisplayEffect(2);
			}
			else if (CommonUtil.contains(BOSSES, npc.getId()))
			{
				npc.setTarget(npc);
				npc.doCast(BOSS_STONE.getSkill());
				((L2Attackable) npc).setCanReturnToSpawnPoint(false);
				npc.setRandomWalking(false);
				npc.setTargetable(false);
				npc.setIsInvul(true);
			}
		}
		return super.onSpawn(npc);
	}
	
	private void manageProgressInInstance(Instance instance)
	{
		final StatsSet param = instance.getParameters();
		final int room = param.getInt("ROOM", 1);
		final int stage = param.getInt("STAGE", 1);
		final int wave = param.getInt("WAVE", 1);
		
		if (room == 1)
		{
			switch (stage)
			{
				case 1:
					switch (wave)
					{
						case 1:
						{
							showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
							moveMonsters(instance.spawnGroup("ROOM1_STAGE1_WAVE1"));
							param.set("WAVE", 2);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 2:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE1_WAVE2"));
							param.set("WAVE", 3);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 3:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE1_WAVE3"));
							param.set("WAVE", 1);
							param.set("STAGE", 2);
							param.set("CONTINUE_AFTER_KILL", true);
							break;
						}
					}
					break;
				case 2:
					switch (wave)
					{
						case 1:
						{
							showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
							moveMonsters(instance.spawnGroup("ROOM1_STAGE2_WAVE1"));
							param.set("WAVE", 2);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 2:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE2_WAVE2"));
							param.set("WAVE", 3);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 3:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE2_WAVE3"));
							param.set("WAVE", 1);
							param.set("STAGE", 3);
							param.set("CONTINUE_AFTER_KILL", true);
							break;
						}
					}
					break;
				case 3:
					switch (wave)
					{
						case 1:
						{
							showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
							moveMonsters(instance.spawnGroup("ROOM1_STAGE3_WAVE1"));
							param.set("WAVE", 2);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 2:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE3_WAVE2"));
							param.set("WAVE", 3);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 3:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE3_WAVE3"));
							if (isSoloKartia(instance))
							{
								param.set("WAVE", 4);
								
								getTimers().addTimer("PRISONERS_ESCAPE", 5000, n ->
								{
									instance.getAliveNpcs(PRISONERS).forEach(prisoner ->
									{
										param.set("SURVIVOR_COUNT", param.getInt("SURVIVOR_COUNT", 0) + 1);
										prisoner.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.I_AM_SAFE_THANKS_TO_YOU_I_WILL_BEGIN_SUPPORTING_AS_SOON_AS_PREPARATIONS_ARE_COMPLETE);
										prisoner.setTargetable(false);
										PRISONER_CLEANSE.getSkill().applyEffects(prisoner, prisoner);
										final Location loc = instance.getTemplateParameters().getLocation("prisonerEscapeLoc");
										addMoveToDesire(prisoner, loc, 23);
									});
								});
							}
							else
							{
								param.set("STAGE", 4);
								param.set("WAVE", 1);
							}
							param.set("CONTINUE_AFTER_KILL", true);
							break;
						}
						case 4:
						{
							showOnScreenMsg(instance, NpcStringId.THE_LIFE_PLUNDERER_S_TRUE_FORM_IS_REVEALED, ExShowScreenMessage.TOP_CENTER, 5000, true);
							instance.spawnGroup("ROOM1_STAGE3_WAVE4");
							instance.getAliveNpcs(BOZ_ENERGY).forEach(npc -> npc.deleteMe());
							instance.getAliveNpcs(MIRRORS).forEach(npc -> getTimers().addTimer("MIRROR_DESPAWN", 180000, npc, null));
							param.set("ROOM", 2);
							param.set("STAGE", 1);
							param.set("WAVE", 1);
							break;
						}
					}
					break;
				case 4:
					switch (wave)
					{
						case 1:
						{
							showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
							moveMonsters(instance.spawnGroup("ROOM1_STAGE4_WAVE1"));
							param.set("WAVE", 2);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 2:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE4_WAVE2"));
							param.set("WAVE", 3);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 3:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE4_WAVE3"));
							param.set("WAVE", 1);
							param.set("STAGE", 5);
							param.set("CONTINUE_AFTER_KILL", true);
							break;
						}
					}
					break;
				case 5:
					switch (wave)
					{
						case 1:
						{
							showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
							moveMonsters(instance.spawnGroup("ROOM1_STAGE5_WAVE1"));
							param.set("WAVE", 2);
							getTimers().addTimer("NEXT_WAVE_DELAY", 30000, n -> manageProgressInInstance(instance));
							break;
						}
						case 2:
						{
							moveMonsters(instance.spawnGroup("ROOM1_STAGE5_WAVE2"));
							param.set("ROOM", 2);
							param.set("STAGE", 1);
							param.set("WAVE", 1);
							param.set("CONTINUE_AFTER_KILL", true);
							
							getTimers().addTimer("PRISONERS_ESCAPE", 5000, n ->
							{
								instance.getAliveNpcs(PRISONERS).forEach(prisoner ->
								{
									param.set("SURVIVOR_COUNT", param.getInt("SURVIVOR_COUNT", 0) + 1);
									prisoner.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.I_AM_SAFE_THANKS_TO_YOU_I_WILL_BEGIN_SUPPORTING_AS_SOON_AS_PREPARATIONS_ARE_COMPLETE);
									prisoner.setTargetable(false);
									PRISONER_CLEANSE.getSkill().applyEffects(prisoner, prisoner);
									final Location loc = instance.getTemplateParameters().getLocation("prisonerEscapeLoc");
									addMoveToDesire(prisoner, loc, 23);
								});
							});
							break;
						}
					}
					break;
			}
		}
		else if (room == 2)
		{
			instance.getParameters().set("TELEPORT_1_ENABLED", false);
			instance.setParameter("BOSS_CAN_ESCAPE", true);
			instance.setParameter("BOSS_KILL_OPEN_DOOR", true);
			instance.spawnGroup("ROOM2_STAGE1_WAVE1");
			instance.openCloseDoor(instance.getTemplateParameters().getInt("secondDoorId"), true);
			instance.setStatus(2); // Used for notify helper's AI
			instance.getAliveNpcs(BOZ_ENERGY).forEach(npc -> npc.deleteMe());
			param.set("ROOM", 3);
			param.set("STAGE", 1);
			param.set("WAVE", 1);
		}
		else if (room == 3)
		{
			switch (stage)
			{
				case 1:
				{
					showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
					moveMonsters(instance.spawnGroup("ROOM3_STAGE1_WAVE1"));
					param.set("STAGE", 2);
					param.set("CONTINUE_AFTER_KILL", true);
					
					final Location survivorLoc = instance.getTemplateParameters().getLocation("middlePointRoom3");
					final int survivorCount = param.getInt("SURVIVOR_COUNT");
					for (int i = 0; i < survivorCount; i++)
					{
						final Location loc = new Location(survivorLoc.getX() + getRandom(-200, 200), survivorLoc.getY() + getRandom(-200, 200), survivorLoc.getZ(), 47595);
						addSpawn(instance.getTemplateParameters().getInt("helperSurvivor"), loc, false, 0, false, instance.getId());
					}
					break;
				}
				case 2:
				{
					showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
					moveMonsters(instance.spawnGroup("ROOM3_STAGE2_WAVE1"));
					param.set("STAGE", 3);
					param.set("CONTINUE_AFTER_KILL", true);
					break;
				}
				case 3:
				{
					showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
					moveMonsters(instance.spawnGroup("ROOM3_STAGE3_WAVE1"));
					
					if (isSoloKartia(instance))
					{
						instance.getAliveNpcs(BOSSES).forEach(npc ->
						{
							npc.stopSkillEffects(BOSS_STONE.getSkill());
							npc.setTargetable(true);
							npc.setIsInvul(false);
							final Location loc = instance.getTemplateParameters().getLocation("middlePointRoom3");
							final Location moveTo = new Location(loc.getX() + getRandom(-200, 200), loc.getY() + getRandom(-200, 200), loc.getZ());
							addMoveToDesire(npc, moveTo, 23);
						});
					}
					else
					{
						param.set("STAGE", 4);
						param.set("CONTINUE_AFTER_KILL", true);
					}
					break;
				}
				case 4:
				{
					showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
					moveMonsters(instance.spawnGroup("ROOM3_STAGE4_WAVE1"));
					param.set("STAGE", 5);
					param.set("CONTINUE_AFTER_KILL", true);
					break;
				}
				case 5:
				{
					showOnScreenMsg(instance, NpcStringId.STAGE_S1, ExShowScreenMessage.TOP_CENTER, 5000, true, Integer.toString(stage));
					moveMonsters(instance.spawnGroup("ROOM3_STAGE4_WAVE1"));
					instance.getAliveNpcs(BOSSES).forEach(npc ->
					{
						npc.stopSkillEffects(BOSS_STONE.getSkill());
						npc.setTargetable(true);
						npc.setIsInvul(false);
						npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.THE_RITUAL_IS_COMPLETE_NOW_I_WILL_SHOW_YOU_HELL);
						final Location loc = instance.getTemplateParameters().getLocation("middlePointRoom3");
						final Location moveTo = new Location(loc.getX() + getRandom(-200, 200), loc.getY() + getRandom(-200, 200), loc.getZ());
						addMoveToDesire(npc, moveTo, 23);
					});
					break;
				}
			}
		}
	}
	
	private void moveMonsters(List<L2Npc> monsterList)
	{
		int delay = 500;
		for (L2Npc npc : monsterList)
		{
			final Instance world = npc.getInstanceWorld();
			if (npc.isAttackable() && (world != null))
			{
				if (world.getParameters().getInt("ROOM", 1) <= 2)
				{
					npc.setRandomWalking(false);
					npc.setTargetable(false);
					getTimers().addTimer("MOVE_TO_MIDDLE", delay, npc, null);
					delay += 250;
				}
				else if (world.getParameters().getInt("ROOM", 1) == 3)
				{
					onTimerEvent("MOVE_TO_MIDDLE", null, npc, null);
				}
				((L2Attackable) npc).setCanReturnToSpawnPoint(false);
				npc.initSeenCreatures();
			}
		}
	}
	
	public void onCreatureSee(OnCreatureSee event)
	{
		final L2Character creature = event.getSeen();
		final L2Npc npc = (L2Npc) event.getSeer();
		final Instance world = npc.getInstanceWorld();
		
		if ((world != null) && creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			addAttackDesire(npc, creature);
		}
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		if (npc.getId() == KARTIA_RESEARCHER)
		{
			return "33647.htm";
		}
		if (CommonUtil.contains(ADOLPH, npc.getId()))
		{
			return "adolph.html";
		}
		if (CommonUtil.contains(BARTON, npc.getId()))
		{
			return "barton.html";
		}
		if (CommonUtil.contains(ELISE, npc.getId()))
		{
			return "elise.html";
		}
		if (CommonUtil.contains(ELIYAH, npc.getId()))
		{
			return "eliyah.html";
		}
		if (CommonUtil.contains(HAYUK, npc.getId()))
		{
			return "hayuk.html";
		}
		return "";
	}
	
	private boolean isSoloKartia(Instance instance)
	{
		return (instance.getTemplateId() == TEMPLATE_ID_SOLO_85) || (instance.getTemplateId() == TEMPLATE_ID_SOLO_90) || (instance.getTemplateId() == TEMPLATE_ID_SOLO_95);
	}
	
	public static void main(String[] args)
	{
		new KartiasLabyrinth();
	}
}