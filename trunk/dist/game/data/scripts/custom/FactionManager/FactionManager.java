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
package custom.FactionManager;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.npc.AbstractNpcAI;

/**
 * @author Mobius
 */
public class FactionManager extends AbstractNpcAI
{
	// NPC
	private static final int MANAGER = Config.FACTION_MANAGER_NPCID;
	// Other
	private static final String[] TEXTS =
	{
		Config.FACTION_GOOD_TEAM_NAME + " or " + Config.FACTION_EVIL_TEAM_NAME + "?",
		"Select your faction!",
		"The choice is yours!"
	};
	
	private FactionManager()
	{
		super(FactionManager.class.getSimpleName(), "custom");
		addSpawnId(MANAGER);
		addStartNpc(MANAGER);
		addTalkId(MANAGER);
		addFirstTalkId(MANAGER);
		
		if (Config.FACTION_SYSTEM_ENABLED)
		{
			addSpawn(MANAGER, Config.FACTION_MANAGER_LOCATION, false, 0);
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		switch (event)
		{
			case "selectGoodFaction":
			{
				if (Config.FACTION_BALANCE_ONLINE_PLAYERS && (L2World.getInstance().getAllGoodPlayersCount() >= ((L2World.getInstance().getAllEvilPlayersCount() + Config.FACTION_BALANCE_PLAYER_EXCEED_LIMIT))))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player.getHtmlPrefix(), "onlinelimit.html"));
					packet.replace("%name%", player.getName());
					packet.replace("%more%", Config.FACTION_GOOD_TEAM_NAME);
					packet.replace("%less%", Config.FACTION_EVIL_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				if (Config.FACTION_AUTO_NOBLESS)
				{
					player.setNoble(true);
				}
				player.setGood();
				player.getAppearance().setNameColor(Config.FACTION_GOOD_NAME_COLOR);
				player.getAppearance().setTitleColor(Config.FACTION_GOOD_NAME_COLOR);
				player.setTitle(Config.FACTION_GOOD_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + Config.FACTION_GOOD_TEAM_NAME + " faction.");
				player.teleToLocation(Config.FACTION_GOOD_BASE_LOCATION);
				broadcastMessageToFaction(Config.FACTION_GOOD_TEAM_NAME, Config.FACTION_GOOD_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				L2World.addFactionPlayerToWorld(player);
				break;
			}
			case "selectEvilFaction":
			{
				if (Config.FACTION_BALANCE_ONLINE_PLAYERS && (L2World.getInstance().getAllEvilPlayersCount() >= ((L2World.getInstance().getAllGoodPlayersCount() + Config.FACTION_BALANCE_PLAYER_EXCEED_LIMIT))))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player.getHtmlPrefix(), "onlinelimit.html"));
					packet.replace("%name%", player.getName());
					packet.replace("%more%", Config.FACTION_EVIL_TEAM_NAME);
					packet.replace("%less%", Config.FACTION_GOOD_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				if (Config.FACTION_AUTO_NOBLESS)
				{
					player.setNoble(true);
				}
				player.setEvil();
				player.getAppearance().setNameColor(Config.FACTION_EVIL_NAME_COLOR);
				player.getAppearance().setTitleColor(Config.FACTION_EVIL_NAME_COLOR);
				player.setTitle(Config.FACTION_EVIL_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + Config.FACTION_EVIL_TEAM_NAME + " faction.");
				player.teleToLocation(Config.FACTION_EVIL_BASE_LOCATION);
				broadcastMessageToFaction(Config.FACTION_EVIL_TEAM_NAME, Config.FACTION_EVIL_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				L2World.addFactionPlayerToWorld(player);
				break;
			}
			case "SPEAK":
			{
				if (npc != null)
				{
					broadcastNpcSay(npc, ChatType.NPC_GENERAL, TEXTS[getRandom(TEXTS.length)]);
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		final String htmltext = null;
		final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
		packet.setHtml(getHtm(player.getHtmlPrefix(), "manager.html"));
		packet.replace("%name%", player.getName());
		packet.replace("%good%", Config.FACTION_GOOD_TEAM_NAME);
		packet.replace("%evil%", Config.FACTION_EVIL_TEAM_NAME);
		player.sendPacket(packet);
		return htmltext;
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		startQuestTimer("SPEAK", 10000, npc, null, true);
		return super.onSpawn(npc);
	}
	
	private void broadcastMessageToFaction(String factionName, String message)
	{
		if (factionName.equals(Config.FACTION_GOOD_TEAM_NAME))
		{
			for (L2PcInstance player : L2World.getInstance().getAllGoodPlayers())
			{
				player.sendMessage(message);
			}
		}
		else
		{
			for (L2PcInstance player : L2World.getInstance().getAllEvilPlayers())
			{
				player.sendMessage(message);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new FactionManager();
	}
}
