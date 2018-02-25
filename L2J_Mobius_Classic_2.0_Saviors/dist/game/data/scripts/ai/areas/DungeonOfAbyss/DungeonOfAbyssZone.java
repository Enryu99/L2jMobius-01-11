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
package ai.areas.DungeonOfAbyss;

import com.l2jmobius.gameserver.instancemanager.ZoneManager;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.zone.L2ZoneType;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class DungeonOfAbyssZone extends AbstractNpcAI
{
	private static final L2ZoneType ABYSS_WEST_ZONE_1 = ZoneManager.getInstance().getZoneByName("The West Dungeon of Abyss");
	private static final L2ZoneType ABYSS_WEST_ZONE_2 = ZoneManager.getInstance().getZoneByName("The West Dungeon of Abyss 2nd");
	private static final L2ZoneType ABYSS_EAST_ZONE_3 = ZoneManager.getInstance().getZoneByName("The East Dungeon of Abyss");
	private static final L2ZoneType ABYSS_EAST_ZONE_4 = ZoneManager.getInstance().getZoneByName("The East Dungeon of Abyss 2nd");
	
	private static final L2ZoneType ABYSS_WEST_ZONE_BOSS_1 = ZoneManager.getInstance().getZoneByName("The West Dungeon of Abyss Boss Room");
	private static final L2ZoneType ABYSS_WEST_ZONE_BOSS_2 = ZoneManager.getInstance().getZoneByName("The West Dungeon of Abyss 2nd Boss Room");
	private static final L2ZoneType ABYSS_EAST_ZONE_BOSS_3 = ZoneManager.getInstance().getZoneByName("The East Dungeon of Abyss Boss Room");
	private static final L2ZoneType ABYSS_EAST_ZONE_BOSS_4 = ZoneManager.getInstance().getZoneByName("The East Dungeon of Abyss 2nd Boss Room");
	
	private static final int EXIT_TIME = 60 * 60 * 1000; // 60 minute
	private static final int EXIT_TIME_BOSS_ROOM = 30 * 60 * 1000; // 60 minute
	private static final Location EXIT_LOCATION_1 = new Location(-120019, -182575, -6751); // Move to Magrit
	private static final Location EXIT_LOCATION_2 = new Location(-119977, -179753, -6751); // Move to Ingrit
	private static final Location EXIT_LOCATION_3 = new Location(-109554, -180408, -6753); // Move to Iris
	private static final Location EXIT_LOCATION_4 = new Location(-109595, -177560, -6753); // Move to Rosammy
	
	private DungeonOfAbyssZone()
	{
		addEnterZoneId(ABYSS_WEST_ZONE_1.getId(), ABYSS_WEST_ZONE_2.getId(), ABYSS_EAST_ZONE_3.getId(), ABYSS_EAST_ZONE_4.getId(), ABYSS_WEST_ZONE_BOSS_1.getId(), ABYSS_WEST_ZONE_BOSS_2.getId(), ABYSS_EAST_ZONE_BOSS_3.getId(), ABYSS_EAST_ZONE_BOSS_4.getId());
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.startsWith("EXIT_PLAYER"))
		{
			if (player != null)
			{
				if (event.contains(ABYSS_WEST_ZONE_1.getName()) && ABYSS_WEST_ZONE_1.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_1);
				}
				else if (event.contains(ABYSS_WEST_ZONE_2.getName()) && ABYSS_WEST_ZONE_2.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_2);
				}
				else if (event.contains(ABYSS_EAST_ZONE_3.getName()) && ABYSS_EAST_ZONE_3.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_3);
				}
				else if (event.contains(ABYSS_EAST_ZONE_4.getName()) && ABYSS_EAST_ZONE_4.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_4);
				}
				else if (event.contains(ABYSS_WEST_ZONE_BOSS_1.getName()) && ABYSS_WEST_ZONE_BOSS_1.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_1);
				}
				else if (event.contains(ABYSS_WEST_ZONE_BOSS_2.getName()) && ABYSS_WEST_ZONE_BOSS_2.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_2);
				}
				else if (event.contains(ABYSS_EAST_ZONE_BOSS_3.getName()) && ABYSS_EAST_ZONE_BOSS_3.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_3);
				}
				else if (event.contains(ABYSS_EAST_ZONE_BOSS_4.getName()) && ABYSS_EAST_ZONE_BOSS_4.getPlayersInside().contains(player))
				{
					player.teleToLocation(EXIT_LOCATION_4);
				}
			}
		}
		
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onEnterZone(L2Character creature, L2ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final L2PcInstance player = creature.getActingPlayer();
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_WEST_ZONE_1.getName() + player.getObjectId(), null, player);
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_WEST_ZONE_2.getName() + player.getObjectId(), null, player);
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_EAST_ZONE_3.getName() + player.getObjectId(), null, player);
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_EAST_ZONE_4.getName() + player.getObjectId(), null, player);
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_WEST_ZONE_BOSS_1.getName() + player.getObjectId(), null, player);
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_WEST_ZONE_BOSS_2.getName() + player.getObjectId(), null, player);
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_EAST_ZONE_BOSS_3.getName() + player.getObjectId(), null, player);
			cancelQuestTimer("EXIT_PLAYER" + ABYSS_EAST_ZONE_BOSS_4.getName() + player.getObjectId(), null, player);
			startQuestTimer("EXIT_PLAYER" + zone.getName() + player.getObjectId(), zone.getName().contains("boss") ? EXIT_TIME_BOSS_ROOM : EXIT_TIME, null, player);
		}
		return super.onEnterZone(creature, zone);
	}
	
	public static void main(String[] args)
	{
		new DungeonOfAbyssZone();
	}
}
