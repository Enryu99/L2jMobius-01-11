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
package handlers.admincommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Chronos;
import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;

/**
 * @author St3eT
 */
public class AdminServerInfo implements IAdminCommandHandler
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("hh:mm a");
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_serverinfo"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		if (command.equals("admin_serverinfo"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage();
			final Runtime runTime = Runtime.getRuntime();
			final int mb = 1024 * 1024;
			html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/serverinfo.htm"));
			html.replace("%os_name%", System.getProperty("os.name"));
			html.replace("%os_ver%", System.getProperty("os.version"));
			html.replace("%slots%", getPlayersCount("ALL") + "/" + Config.MAXIMUM_ONLINE_USERS);
			html.replace("%gameTime%", GameTimeTaskManager.getInstance().getGameHour() + ":" + GameTimeTaskManager.getInstance().getGameMinute());
			html.replace("%dayNight%", GameTimeTaskManager.getInstance().isNight() ? "Night" : "Day");
			html.replace("%geodata%", Config.PATHFINDING ? "Enabled" : "Disabled");
			html.replace("%serverTime%", SDF.format(new Date(Chronos.currentTimeMillis())));
			html.replace("%serverUpTime%", getServerUpTime());
			html.replace("%onlineAll%", getPlayersCount("ALL"));
			html.replace("%offlineTrade%", getPlayersCount("OFF_TRADE"));
			html.replace("%onlineGM%", getPlayersCount("GM"));
			html.replace("%onlineReal%", getPlayersCount("ALL_REAL"));
			html.replace("%usedMem%", (runTime.maxMemory() / mb) - (((runTime.maxMemory() - runTime.totalMemory()) + runTime.freeMemory()) / mb));
			html.replace("%freeMem%", ((runTime.maxMemory() - runTime.totalMemory()) + runTime.freeMemory()) / mb);
			html.replace("%totalMem%", Runtime.getRuntime().maxMemory() / 1048576);
			activeChar.sendPacket(html);
		}
		return true;
	}
	
	private String getServerUpTime()
	{
		long time = Chronos.currentTimeMillis() - GameServer.dateTimeServerStarted.getTimeInMillis();
		
		final long days = TimeUnit.MILLISECONDS.toDays(time);
		time -= TimeUnit.DAYS.toMillis(days);
		final long hours = TimeUnit.MILLISECONDS.toHours(time);
		time -= TimeUnit.HOURS.toMillis(hours);
		return days + " Days, " + hours + " Hours, " + TimeUnit.MILLISECONDS.toMinutes(time) + " Minutes";
	}
	
	private int getPlayersCount(String type)
	{
		switch (type)
		{
			case "ALL":
			{
				return World.getInstance().getPlayers().size();
			}
			case "OFF_TRADE":
			{
				int offlineCount = 0;
				
				final Collection<PlayerInstance> objs = World.getInstance().getPlayers();
				for (PlayerInstance player : objs)
				{
					if ((player.getClient() == null) || player.getClient().isDetached())
					{
						offlineCount++;
					}
				}
				return offlineCount;
			}
			case "GM":
			{
				int onlineGMcount = 0;
				for (PlayerInstance gm : AdminData.getInstance().getAllGms(true))
				{
					if ((gm != null) && gm.isOnline() && (gm.getClient() != null) && !gm.getClient().isDetached())
					{
						onlineGMcount++;
					}
				}
				return onlineGMcount;
			}
			case "ALL_REAL":
			{
				final Set<String> realPlayers = new HashSet<>();
				for (PlayerInstance onlinePlayer : World.getInstance().getPlayers())
				{
					if ((onlinePlayer != null) && (onlinePlayer.getClient() != null) && !onlinePlayer.getClient().isDetached())
					{
						realPlayers.add(onlinePlayer.getIPAddress());
					}
				}
				return realPlayers.size();
			}
		}
		return 0;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
