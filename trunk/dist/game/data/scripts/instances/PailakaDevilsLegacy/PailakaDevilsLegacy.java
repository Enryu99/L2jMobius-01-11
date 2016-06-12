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
package instances.PailakaDevilsLegacy;

import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.instancezone.Instance;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.zone.L2ZoneType;

import instances.AbstractInstance;
import quests.Q00129_PailakaDevilsLegacy.Q00129_PailakaDevilsLegacy;

/**
 * Pailaka Devil's Legacy Instance zone.
 * @author St3eT
 */
public final class PailakaDevilsLegacy extends AbstractInstance
{
	// NPCs
	private static final int LEMATAN = 18633; // Lematan
	private static final int SURVIVOR = 32498; // Devil's Isle Survivor
	private static final int FOLLOWERS = 18634; // Lematan's Follower
	private static final int POWDER_KEG = 18622; // Powder Keg
	private static final int TREASURE_BOX = 32495; // Treasure Chest
	private static final int ADVENTURER2 = 32511; // Dwarf Adventurer
	// Items
	private static final int ANTIDOTE_POTION = 13048; // Pailaka Antidote
	private static final int DIVINE_POTION = 13049; // Divine Soul
	private static final int PAILAKA_KEY = 13150; // Pailaka All-Purpose Key
	private static final int SHIELD = 13032; // Pailaka Instant Shield
	private static final int DEFENCE_POTION = 13059; // Long-Range Defense Increasing Potion
	private static final int HEALING_POTION = 13033; // Quick Healing Potion
	// Skills
	private static final SkillHolder ENERGY = new SkillHolder(5712, 1); // Energy Ditch
	private static final SkillHolder BOOM = new SkillHolder(5714, 1); // Boom Up
	private static final SkillHolder AV_TELEPORT = new SkillHolder(4671, 1); // AV - Teleport
	// Locations
	private static final Location TELEPORT = new Location(76427, -219045, -3780);
	private static final Location LEMATAN_PORT_POINT = new Location(86116, -209117, -3774);
	private static final Location LEMATAN_PORT = new Location(85000, -208699, -3336);
	private static final Location ADVENTURER_LOC = new Location(84983, -208736, -3336, 49915);
	// Misc
	private static final int TEMPLATE_ID = 44;
	private static final int ZONE = 20109;
	private static final int ZONE_EXIT = 200000;
	private static final int TIGRESS_LVL1 = 14916;
	private static final int TIGRESS_LVL2 = 14917;
	
	public PailakaDevilsLegacy()
	{
		addTalkId(SURVIVOR);
		addAttackId(POWDER_KEG, TREASURE_BOX, LEMATAN);
		addKillId(LEMATAN);
		addSpawnId(FOLLOWERS);
		addEnterZoneId(ZONE);
		addExitZoneId(ZONE_EXIT);
		addMoveFinishedId(LEMATAN);
	}
	
	@Override
	protected void onEnter(L2PcInstance player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		if (firstEnter)
		{
			final QuestState qs = player.getQuestState(Q00129_PailakaDevilsLegacy.class.getSimpleName());
			if (qs.isCond(1))
			{
				qs.setCond(2, true);
				showHtmlFile(player, "32498-01.htm");
			}
			else
			{
				showHtmlFile(player, "32498-02.htm");
			}
		}
	}
	
	@Override
	public final String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equals("enter"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		else
		{
			final Instance world = npc.getInstanceWorld();
			if (world != null)
			{
				switch (event)
				{
					case "FOLLOWER_CAST":
					{
						if (!npc.isDead())
						{
							for (L2Npc follower : world.getNpcs(FOLLOWERS))
							{
								follower.setTarget(npc);
								follower.doCast(ENERGY.getSkill());
							}
							startQuestTimer("FOLLOWER_CAST", 15000, npc, null);
						}
						break;
					}
					case "LEMATAN_TELEPORT":
					{
						((L2Attackable) npc).clearAggroList();
						npc.disableCoreAI(false);
						npc.teleToLocation(LEMATAN_PORT);
						npc.getVariables().set("ON_SHIP", 1);
						npc.getSpawn().setLocation(LEMATAN_PORT);
						world.spawnGroup("followers");
						startQuestTimer("FOLLOWER_CAST", 4000, npc, null);
						break;
					}
					case "TELEPORT":
					{
						player.teleToLocation(TELEPORT);
						break;
					}
					case "DELETE":
					{
						npc.deleteMe();
						break;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public final String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			switch (npc.getId())
			{
				case POWDER_KEG:
				{
					if ((damage > 0) && npc.isScriptValue(0))
					{
						L2World.getInstance().forEachVisibleObjectInRange(npc, L2MonsterInstance.class, 600, monster ->
						{
							monster.addDamageHate(npc, 0, 999);
							monster.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, npc);
							monster.reduceCurrentHp(500 + getRandom(0, 200), npc, BOOM.getSkill());
						});
						npc.doCast(BOOM.getSkill());
						npc.setScriptValue(1);
						startQuestTimer("DELETE", 2000, npc, null);
					}
					break;
				}
				case LEMATAN:
				{
					if (npc.isScriptValue(0) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.5)))
					{
						npc.disableCoreAI(true);
						npc.setScriptValue(1);
						npc.setIsRunning(true);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, LEMATAN_PORT_POINT);
					}
					break;
				}
				case TREASURE_BOX:
				{
					if (npc.isScriptValue(0))
					{
						switch (getRandom(7))
						{
							case 0:
							case 1:
							{
								npc.dropItem(attacker, ANTIDOTE_POTION, getRandom(1, 10));
								break;
							}
							case 2:
							{
								npc.dropItem(attacker, DIVINE_POTION, getRandom(1, 5));
								break;
							}
							case 3:
							{
								npc.dropItem(attacker, PAILAKA_KEY, getRandom(1, 2));
								break;
							}
							case 4:
							{
								npc.dropItem(attacker, DEFENCE_POTION, getRandom(1, 7));
								break;
							}
							case 5:
							{
								npc.dropItem(attacker, SHIELD, getRandom(1, 10));
								break;
							}
							case 6:
							{
								npc.dropItem(attacker, HEALING_POTION, getRandom(1, 10));
								break;
							}
						}
						npc.setScriptValue(1);
						startQuestTimer("DELETE", 3000, npc, attacker);
					}
					break;
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public final String onKill(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			world.getNpcs(FOLLOWERS).forEach(L2Npc::deleteMe);
			addSpawn(ADVENTURER2, ADVENTURER_LOC, false, 0, false, npc.getInstanceId());
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		if ((character.isPlayer()) && !character.isDead() && !character.isTeleporting() && ((L2PcInstance) character).isOnline())
		{
			final Instance world = character.getInstanceWorld();
			if ((world != null) && (world.getTemplateId() == TEMPLATE_ID))
			{
				startQuestTimer("TELEPORT", 1000, world.getNpc(LEMATAN), character.getActingPlayer());
			}
		}
		return super.onEnterZone(character, zone);
	}
	
	@Override
	public String onExitZone(L2Character character, L2ZoneType zone)
	{
		if (character.isPlayer() && character.hasSummon())
		{
			L2World.getInstance().forEachVisibleObject(character, L2Summon.class, summon ->
			{
				if ((summon.getTemplate().getId() == TIGRESS_LVL1) || (summon.getTemplate().getId() == TIGRESS_LVL2))
				{
					summon.unSummon(((L2PcInstance) character));
				}
			});
		}
		return super.onExitZone(character, zone);
	}
	
	@Override
	public void onMoveFinished(L2Npc npc)
	{
		if (npc.getLocation() == LEMATAN_PORT_POINT)
		{
			npc.doCast(AV_TELEPORT.getSkill());
			startQuestTimer("LEMATAN_TELEPORT", 2000, npc, null);
		}
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		npc.disableCoreAI(true);
		npc.setIsImmobilized(true);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new PailakaDevilsLegacy();
	}
}
