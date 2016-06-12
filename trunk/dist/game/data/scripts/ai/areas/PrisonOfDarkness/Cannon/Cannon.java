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
package ai.areas.PrisonOfDarkness.Cannon;

import java.util.HashMap;
import java.util.Map;

import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.network.NpcStringId;
import com.l2jmobius.gameserver.network.serverpackets.Earthquake;
import com.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

import ai.AbstractNpcAI;

/**
 * Cannon AI.
 * @author St3eT
 */
public final class Cannon extends AbstractNpcAI
{
	// NPCs
	private static final int[] CANNONS =
	{
		32939,
		32940,
		32941,
		32942,
	};
	private static final int INVISIBLE_NPC = 32943;
	// Skills
	private static final SkillHolder PRESENT_SKILL = new SkillHolder(14175, 1); // Cannon Blast
	// Items
	private static final int CANNONBALL = 17611; // Giant Cannonbal
	private static final int MEMORY_FRAGMENT = 17612; // Memory Fragment
	private static final int F_MEMORY_FRAGMENT = 17613; // Frightening Memory Fragment
	// Misc
	private static final Map<Integer, Integer> TRANSFORM_DATA = new HashMap<>();
	
	static
	{
		TRANSFORM_DATA.put(22965, 22979); // Novice Phantom -> Novice Escort Swordsman
		TRANSFORM_DATA.put(22966, 22980); // Phantom Wizard -> Novice Escort Wizard
		TRANSFORM_DATA.put(22967, 22981); // Median Phantom -> Median Escort Swordsman
	}
	
	private Cannon()
	{
		addStartNpc(CANNONS);
		addTalkId(CANNONS);
		addFirstTalkId(CANNONS);
		addSpawnId(CANNONS);
		addSpellFinishedId(CANNONS);
		addKillId(TRANSFORM_DATA.keySet());
		addKillId(TRANSFORM_DATA.values());
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = null;
		
		if (event.equals("useCannonBall"))
		{
			if (npc.isScriptValue(0))
			{
				htmltext = "cannon-recharge.html";
			}
			else if (getQuestItemsCount(player, CANNONBALL) == 0)
			{
				htmltext = "cannon-noItem.html";
			}
			else
			{
				takeItems(player, CANNONBALL, 1);
				npc.setScriptValue(0);
				npc.setTitleString(NpcStringId.CANNON_IS_LOADING);
				npc.broadcastInfo();
				addSkillCastDesire(npc, npc, PRESENT_SKILL, 23);
				getTimers().addTimer("CANNON_RECHARGE", 300000, npc, null);
			}
		}
		return htmltext;
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, L2Npc npc, L2PcInstance player)
	{
		switch (event)
		{
			case "CANNON_RECHARGE":
			{
				npc.setScriptValue(1);
				npc.setTitleString(NpcStringId.EMPTY_CANNON);
				npc.broadcastInfo();
				break;
			}
			case "LIGHT_CHECK":
			{
				L2World.getInstance().forEachVisibleObjectInRange(npc, L2MonsterInstance.class, 1000, monster ->
				{
					final int monsterId = monster.getId();
					if (TRANSFORM_DATA.containsKey(monsterId))
					{
						final L2Npc transformed = addSpawn(TRANSFORM_DATA.get(monster.getId()), monster);
						transformed.getVariables().set("DROP_MEMORY_FRAGMENT", true);
						transformed.getVariables().set("COUNTDOWN_TIME", 21);
						getTimers().addTimer("COUTDOWN", 100, transformed, null);
						monster.deleteMe();
					}
				});
				break;
			}
			case "COUTDOWN":
			{
				final int time = npc.getVariables().getInt("COUNTDOWN_TIME", 0) - 1;
				npc.setTitle(npc.isDead() ? null : Integer.toString(time));
				npc.broadcastInfo();
				if (time == 0)
				{
					npc.deleteMe();
				}
				else if (!npc.isDead())
				{
					npc.getVariables().set("COUNTDOWN_TIME", time);
					getTimers().addTimer("COUTDOWN", 1000, npc, null);
				}
				break;
			}
		}
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		if (npc.getVariables().getBoolean("DROP_MEMORY_FRAGMENT", false))
		{
			npc.dropItem(killer, MEMORY_FRAGMENT, 1);
			npc.dropItem(killer, F_MEMORY_FRAGMENT, 1);
		}
		else if (getRandom(10) < 1)
		{
			npc.dropItem(killer, CANNONBALL, 1);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		getTimers().addTimer("CANNON_RECHARGE", 1000, npc, null);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, Skill skill)
	{
		if (skill.getId() == PRESENT_SKILL.getSkillId())
		{
			final StatsSet npcParams = npc.getParameters();
			
			npc.broadcastPacket(new Earthquake(npc, 10, 5));
			npc.broadcastPacket(new OnEventTrigger(npcParams.getInt("TRIGGER_ID"), true));
			final L2Npc light = addSpawn(INVISIBLE_NPC, npcParams.getInt("LIGHT_ZONE_POS_X"), npcParams.getInt("LIGHT_ZONE_POS_Y"), npcParams.getInt("LIGHT_ZONE_POS_Z"), 0, false, 10000);
			getTimers().addTimer("LIGHT_CHECK", 500, light, null);
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		return "cannon.html";
	}
	
	public static void main(String[] args)
	{
		new Cannon();
	}
}