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
package instances.KrofinNest;

import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;

/**
 * @author CostyKiller, NasSeKa
 * @URL https://www.youtube.com/watch?v=UCI2lnm-Bc0
 */
public class KrofinNest extends AbstractInstance
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int KROSHA_FIRST_FORM = 26389;
	private static final int KROSHA_FINAL_FORM = 26390;
	private static final int KROSHA_FINAL_FORM_110 = 26467;
	private static final int KROPION = 26396;
	private static final int KROPION_110 = 26474;
	private static final int[] FIRST_AREA =
	{
		KROPION,
		26398
	};
	private static final int[] FIRST_AREA_110 =
	{
		26479,
		26478,
		26477,
		26476
	};
	private static final int[] SECOND_AREA =
	{
		KROPION,
		26398
	};
	private static final int[] SECOND_AREA_110 =
	{
		KROPION_110,
		26475
	};
	private static final int[] THIRD_AREA =
	{
		26395,
		26397
	};
	private static final int[] THIRD_AREA_110 =
	{
		26470,
		26471
	};
	private static final int[] FOURTH_AREA =
	{
		26395,
		KROPION,
		26397,
		26398
	};
	private static final int[] FOURTH_AREA_110 =
	{
		26468,
		26469
	};
	private static final int[] KROSHA_FIRST_FORM_MINIONS =
	{
		26393,
		26394
	};
	private static final int[] ENHANCED_MINIONS =
	{
		26391,
		26392
	};
	// Items
	private static final ItemHolder BENUSTAS_REWARD_BOX = new ItemHolder(81151, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_110 = new ItemHolder(81741, 1);
	// Misc
	private static final int[] TEMPLATE_IDS =
	{
		291,
		315
	};
	private static final int DOOR1 = 23220101;
	private static final int DOOR2 = 24250002;
	private static final int DOOR3 = 24250004;
	private static final int DOOR4 = 24250006;
	
	public KrofinNest()
	{
		super(TEMPLATE_IDS);
		addStartNpc(BENUSTA);
		addTalkId(BENUSTA);
		addAttackId(FIRST_AREA);
		addAttackId(FIRST_AREA_110);
		addAttackId(SECOND_AREA);
		addAttackId(SECOND_AREA_110);
		addAttackId(THIRD_AREA);
		addAttackId(THIRD_AREA_110);
		addAttackId(FOURTH_AREA);
		addAttackId(FOURTH_AREA_110);
		addAttackId(KROSHA_FIRST_FORM);
		addAttackId(KROSHA_FINAL_FORM);
		addAttackId(KROSHA_FINAL_FORM_110);
		addKillId(KROSHA_FIRST_FORM_MINIONS);
		addKillId(ENHANCED_MINIONS);
		addKillId(KROSHA_FINAL_FORM);
		addKillId(KROSHA_FINAL_FORM_110);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.contains("enterInstance"))
		{
			if (event.contains("110"))
			{
				// Cannot enter if player finished another instance.
				final long currentTime = Chronos.currentTimeMillis();
				if ((currentTime < InstanceManager.getInstance().getInstanceTime(player, 291)))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON).addString(player.getName()));
					return null;
				}
				enterInstance(player, npc, TEMPLATE_IDS[1]);
			}
			else
			{
				enterInstance(player, npc, TEMPLATE_IDS[0]);
			}
			if (player.getInstanceWorld() != null)
			{
				startQuestTimer("check_status", 1000, null, player);
			}
		}
		else if (event.equals("check_status"))
		{
			final Instance world = player.getInstanceWorld();
			if (!isInInstance(world))
			{
				return null;
			}
			
			switch (world.getStatus())
			{
				case 0:
				{
					world.setStatus(1);
					world.spawnGroup("FIRST_AREA");
					startQuestTimer("check_status", 10000, null, player);
					break;
				}
				case 1:
				{
					if (world.getAliveNpcs(world.getTemplateId() == TEMPLATE_IDS[0] ? FIRST_AREA : FIRST_AREA_110).isEmpty())
					{
						showOnScreenMsg(world, NpcStringId.THE_WATER_ENERGY_IS_NO_LONGER_ACTIVE_THE_WAY_IS_CLEAR, ExShowScreenMessage.TOP_CENTER, 10000, true);
						world.setStatus(2);
						world.getDoor(DOOR1).openMe();
						world.spawnGroup("SECOND_AREA");
					}
					startQuestTimer("check_status", 1000, null, player);
					break;
				}
				case 2:
				{
					if (world.getAliveNpcs(world.getTemplateId() == TEMPLATE_IDS[0] ? SECOND_AREA : SECOND_AREA_110).isEmpty())
					{
						showOnScreenMsg(world, NpcStringId.THE_WATER_ENERGY_IS_NO_LONGER_ACTIVE_THE_WAY_IS_CLEAR, ExShowScreenMessage.TOP_CENTER, 10000, true);
						world.setStatus(3);
						world.getDoor(DOOR2).openMe();
						world.spawnGroup("THIRD_AREA");
					}
					startQuestTimer("check_status", 10000, null, player);
					break;
				}
				case 3:
				{
					if (world.getAliveNpcs(world.getTemplateId() == TEMPLATE_IDS[0] ? THIRD_AREA : THIRD_AREA_110).isEmpty())
					{
						showOnScreenMsg(world, NpcStringId.THE_WATER_ENERGY_IS_NO_LONGER_ACTIVE_THE_WAY_IS_CLEAR, ExShowScreenMessage.TOP_CENTER, 10000, true);
						world.setStatus(4);
						world.getDoor(DOOR3).openMe();
						world.spawnGroup("FOURTH_AREA");
					}
					startQuestTimer("check_status", 10000, null, player);
					break;
				}
				case 4:
				{
					if (world.getAliveNpcs(world.getTemplateId() == TEMPLATE_IDS[0] ? FOURTH_AREA : FOURTH_AREA_110).isEmpty())
					{
						showOnScreenMsg(world, NpcStringId.THE_WATER_ENERGY_IS_NO_LONGER_ACTIVE_THE_WAY_IS_CLEAR, ExShowScreenMessage.TOP_CENTER, 10000, true);
						world.setStatus(5);
						world.getDoor(DOOR4).openMe();
						world.spawnGroup(world.getTemplateId() == TEMPLATE_IDS[0] ? "KROSHA_FIRST_FORM" : "KROSHA_FINAL_FORM");
					}
					startQuestTimer("check_status", 10000, null, player);
					break;
				}
			}
		}
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		final Instance world = attacker.getInstanceWorld();
		if (isInInstance(world))
		{
			final boolean kropionMinionsSpawned = world.getParameters().getBoolean("KROPION_MINIONS_SPAWNED", false);
			final boolean kroshaFirstFormMinionsSpawned = world.getParameters().getBoolean("KROSHA_FIRST_FORM_MINIONS_SPAWNED", false);
			final boolean kroshaFinalFormMinionsSpawned = world.getParameters().getBoolean("KROSHA_FINAL_FORM_MINIONS_SPAWNED", false);
			if ((world.getStatus() == 2) && ((npc.getId() == KROPION) || (npc.getId() == KROPION_110)))
			{
				if (!kropionMinionsSpawned)
				{
					world.getParameters().set("KROPION_MINIONS_SPAWNED", true);
					world.spawnGroup("KROPION_MINIONS");
				}
			}
			else if (world.getStatus() == 5)
			{
				if ((npc.getId() == KROSHA_FIRST_FORM) && !kroshaFirstFormMinionsSpawned)
				{
					world.getParameters().set("KROSHA_FIRST_FORM_MINIONS_SPAWNED", true);
					world.spawnGroup("KROSHA_FIRST_FORM_MINIONS");
				}
				else if (((npc.getId() == KROSHA_FINAL_FORM) || (npc.getId() == KROSHA_FINAL_FORM_110)) && !kroshaFinalFormMinionsSpawned)
				{
					world.getParameters().set("KROSHA_FINAL_FORM_MINIONS_SPAWNED", true);
					world.spawnGroup("KROSHA_FINAL_FORM_MINIONS");
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isPet)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final boolean kroshaFirstFormMinionsSpawnedTwice = world.getParameters().getBoolean("KROSHA_FIRST_FORM_MINIONS_SPAWNED_TWICE", false);
			if (world.getStatus() == 5)
			{
				if (CommonUtil.contains(KROSHA_FIRST_FORM_MINIONS, npc.getId()))
				{
					if (world.getAliveNpcs(KROSHA_FIRST_FORM_MINIONS).isEmpty() && !kroshaFirstFormMinionsSpawnedTwice)
					{
						world.getParameters().set("KROSHA_FIRST_FORM_MINIONS_SPAWNED_TWICE", true);
						world.spawnGroup("KROSHA_FIRST_FORM_MINIONS");
					}
					else if (world.getAliveNpcs(KROSHA_FIRST_FORM_MINIONS).isEmpty() && kroshaFirstFormMinionsSpawnedTwice)
					{
						world.despawnGroup("KROSHA_FIRST_FORM");
						showOnScreenMsg(world, NpcStringId.QUEEN_KROSHA_HAS_DISAPPEARED, ExShowScreenMessage.TOP_CENTER, 7000, true);
						world.spawnGroup("ENHANCED_MINIONS");
					}
				}
				else if (CommonUtil.contains(ENHANCED_MINIONS, npc.getId()))
				{
					if ((world.getAliveNpcs(ENHANCED_MINIONS).isEmpty()))
					{
						world.spawnGroup("KROSHA_FINAL_FORM");
						showOnScreenMsg(world, NpcStringId.QUEEN_KROSHA_HAS_RETURNED_MORE_POWERFUL_THAN_EVER, ExShowScreenMessage.TOP_CENTER, 7000, true);
					}
				}
				else if (npc.getId() == KROSHA_FINAL_FORM)
				{
					for (PlayerInstance member : world.getPlayers())
					{
						giveItems(member, BENUSTAS_REWARD_BOX);
					}
					showOnScreenMsg(world, NpcStringId.THE_WATER_POWER_PROTECTING_QUEEN_KROSHA_HAS_DISAPPEARED, ExShowScreenMessage.TOP_CENTER, 7000, true);
					world.finishInstance();
				}
				else if (npc.getId() == KROSHA_FINAL_FORM_110)
				{
					for (PlayerInstance member : world.getPlayers())
					{
						giveItems(member, BENUSTAS_REWARD_BOX_110);
					}
					showOnScreenMsg(world, NpcStringId.THE_WATER_POWER_PROTECTING_QUEEN_KROSHA_HAS_DISAPPEARED, ExShowScreenMessage.TOP_CENTER, 7000, true);
					world.finishInstance();
				}
				else
				{
					world.setReenterTime();
				}
			}
		}
		return super.onKill(npc, killer, isPet);
	}
	
	public static void main(String[] args)
	{
		new KrofinNest();
	}
}
