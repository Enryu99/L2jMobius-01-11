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
package instances.MonasteryOfSilence1;

import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.enums.Movie;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.instancezone.Instance;
import com.l2jmobius.gameserver.network.NpcStringId;

import instances.AbstractInstance;

/**
 * Monastery of Silence instance zone.
 * @author Adry_85
 */
public final class MonasteryOfSilence1 extends AbstractInstance
{
	// NPCs
	private static final int ELCADIA_INSTANCE = 32787;
	private static final int ERIS_EVIL_THOUGHTS = 32792;
	private static final int RELIC_GUARDIAN = 32803;
	private static final int RELIC_WATCHER1 = 32804;
	private static final int RELIC_WATCHER2 = 32805;
	private static final int RELIC_WATCHER3 = 32806;
	private static final int RELIC_WATCHER4 = 32807;
	private static final int ODD_GLOBE = 32815;
	private static final int TELEPORT_CONTROL_DEVICE1 = 32817;
	private static final int TELEPORT_CONTROL_DEVICE2 = 32818;
	private static final int TELEPORT_CONTROL_DEVICE3 = 32819;
	private static final int TELEPORT_CONTROL_DEVICE4 = 32820;
	// Skills
	private static final SkillHolder[] BUFFS =
	{
		new SkillHolder(6725, 1), // Bless the Blood of Elcadia
		new SkillHolder(6728, 1), // Recharge of Elcadia
		new SkillHolder(6730, 1), // Greater Battle Heal of Elcadia
	};
	// Locations
	private static final Location CENTRAL_ROOM_LOC = new Location(85794, -249788, -8320);
	private static final Location SOUTH_WATCHERS_ROOM_LOC = new Location(85798, -246566, -8320);
	private static final Location WEST_WATCHERS_ROOM_LOC = new Location(82531, -249405, -8320);
	private static final Location EAST_WATCHERS_ROOM_LOC = new Location(88665, -249784, -8320);
	private static final Location NORTH_WATCHERS_ROOM_LOC = new Location(85792, -252336, -8320);
	private static final Location BACK_LOC = new Location(120710, -86971, -3392);
	// NpcString
	private static final NpcStringId[] ELCADIA_DIALOGS =
	{
		NpcStringId.IT_SEEMS_THAT_YOU_CANNOT_REMEMBER_TO_THE_ROOM_OF_THE_WATCHER_WHO_FOUND_THE_BOOK,
		NpcStringId.WE_MUST_SEARCH_HIGH_AND_LOW_IN_EVERY_ROOM_FOR_THE_READING_DESK_THAT_CONTAINS_THE_BOOK_WE_SEEK,
		NpcStringId.REMEMBER_THE_CONTENT_OF_THE_BOOKS_THAT_YOU_FOUND_YOU_CAN_T_TAKE_THEM_OUT_WITH_YOU
	};
	// Misc
	private static final int TEMPLATE_ID = 151;
	
	public MonasteryOfSilence1()
	{
		addFirstTalkId(TELEPORT_CONTROL_DEVICE1, TELEPORT_CONTROL_DEVICE2, TELEPORT_CONTROL_DEVICE3, TELEPORT_CONTROL_DEVICE4, ERIS_EVIL_THOUGHTS);
		addStartNpc(ODD_GLOBE, TELEPORT_CONTROL_DEVICE1, TELEPORT_CONTROL_DEVICE2, TELEPORT_CONTROL_DEVICE3, TELEPORT_CONTROL_DEVICE4, ERIS_EVIL_THOUGHTS);
		addTalkId(ODD_GLOBE, ERIS_EVIL_THOUGHTS, RELIC_GUARDIAN, RELIC_WATCHER1, RELIC_WATCHER2, RELIC_WATCHER3, RELIC_WATCHER4, TELEPORT_CONTROL_DEVICE1, TELEPORT_CONTROL_DEVICE2, TELEPORT_CONTROL_DEVICE3, TELEPORT_CONTROL_DEVICE4, ERIS_EVIL_THOUGHTS);
	}
	
	@Override
	protected void onEnter(L2PcInstance player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		
		final L2Npc elcadia = addSpawn(ELCADIA_INSTANCE, player, false, 0, false, player.getId());
		startQuestTimer("FOLLOW", 3000, elcadia, player);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final Instance world = player.getInstanceWorld();
		if (world != null)
		{
			final L2Npc elcadia = world.getNpc(ELCADIA_INSTANCE);
			switch (event)
			{
				case "TELE2":
				{
					player.teleToLocation(CENTRAL_ROOM_LOC);
					elcadia.teleToLocation(CENTRAL_ROOM_LOC);
					startQuestTimer("START_MOVIE", 2000, npc, player);
					break;
				}
				case "EXIT":
				{
					cancelQuestTimer("FOLLOW", npc, player);
					world.finishInstance(0);
					break;
				}
				case "START_MOVIE":
				{
					playMovie(player, Movie.SSQ2_HOLY_BURIAL_GROUND_OPENING);
					break;
				}
				case "BACK":
				{
					player.teleToLocation(BACK_LOC);
					elcadia.teleToLocation(BACK_LOC);
					break;
				}
				case "EAST":
				{
					player.teleToLocation(EAST_WATCHERS_ROOM_LOC);
					elcadia.teleToLocation(EAST_WATCHERS_ROOM_LOC);
					break;
				}
				case "WEST":
				{
					player.teleToLocation(WEST_WATCHERS_ROOM_LOC);
					elcadia.teleToLocation(WEST_WATCHERS_ROOM_LOC);
					break;
				}
				case "NORTH":
				{
					player.teleToLocation(NORTH_WATCHERS_ROOM_LOC);
					elcadia.teleToLocation(NORTH_WATCHERS_ROOM_LOC);
					break;
				}
				case "SOUTH":
				{
					player.teleToLocation(SOUTH_WATCHERS_ROOM_LOC);
					elcadia.teleToLocation(SOUTH_WATCHERS_ROOM_LOC);
					break;
				}
				case "CENTER":
				{
					player.teleToLocation(CENTRAL_ROOM_LOC);
					elcadia.teleToLocation(CENTRAL_ROOM_LOC);
					break;
				}
				case "FOLLOW":
				{
					npc.setIsRunning(true);
					npc.getAI().startFollow(player);
					if (player.isInCombat())
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOUR_WORK_HERE_IS_DONE_SO_RETURN_TO_THE_CENTRAL_GUARDIAN);
						npc.setTarget(player);
						npc.doCast(BUFFS[getRandom(BUFFS.length)].getSkill());
					}
					else
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, ELCADIA_DIALOGS[getRandom(ELCADIA_DIALOGS.length)]);
					}
					startQuestTimer("FOLLOW", 10000, npc, player);
					break;
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		if (npc.getId() == ODD_GLOBE)
		{
			enterInstance(talker, npc, TEMPLATE_ID);
		}
		return super.onTalk(npc, talker);
	}
	
	public static void main(String[] args)
	{
		new MonasteryOfSilence1();
	}
}