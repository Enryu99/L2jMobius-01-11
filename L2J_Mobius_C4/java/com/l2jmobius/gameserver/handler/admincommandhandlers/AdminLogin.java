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
package com.l2jmobius.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.LoginServerThread;
import com.l2jmobius.gameserver.handler.IAdminCommandHandler;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.gameserverpackets.ServerStatus;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles the admin commands that acts on the login
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/04/11 10:05:56 $
 */
public class AdminLogin implements IAdminCommandHandler
{
	// private static Logger _log = Logger.getLogger(AdminDelete.class.getName());
	
	private static String[] _adminCommands =
	{
		"admin_server_gm_only",
		"admin_server_all",
		"admin_server_max_player",
		"admin_server_list_clock",
		"admin_server_login"
	};
	
	private static final int REQUIRED_LEVEL = Config.GM_ACCESSLEVEL;
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.handler.IAdminCommandHandler#useAdminCommand(java.lang.String, com.l2jmobius.gameserver.model.L2PcInstance)
	 */
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (!Config.ALT_PRIVILEGES_ADMIN)
		{
			if (activeChar.getAccessLevel() < REQUIRED_LEVEL)
			{
				return false;
			}
		}
		
		if (command.equals("admin_server_gm_only"))
		{
			gmOnly();
			activeChar.sendMessage("Server is now GM only");
			showWindow(activeChar);
		}
		else if (command.equals("admin_server_all"))
		{
			AllowToAll();
			activeChar.sendMessage("Server is not GM only anymore");
			showWindow(activeChar);
		}
		else if (command.startsWith("admin_server_max_player"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String number = st.nextToken();
				try
				{
					LoginServerThread.getInstance().setMaxPlayer(new Integer(number).intValue());
					activeChar.sendMessage("maxPlayer set to " + new Integer(number).intValue());
					showWindow(activeChar);
				}
				catch (final NumberFormatException e)
				{
					activeChar.sendMessage("Max players must be a number.");
				}
			}
			else
			{
				activeChar.sendMessage("Format is server_max_player <max>");
			}
		}
		else if (command.startsWith("admin_server_list_clock"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String mode = st.nextToken();
				if (mode.equals("on"))
				{
					LoginServerThread.getInstance().sendServerStatus(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.ON);
					activeChar.sendMessage("A clock will now be displayed next to the server name");
					Config.SERVER_LIST_CLOCK = true;
					showWindow(activeChar);
				}
				else if (mode.equals("off"))
				{
					LoginServerThread.getInstance().sendServerStatus(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.OFF);
					Config.SERVER_LIST_CLOCK = false;
					activeChar.sendMessage("The clock will not be displayed");
					showWindow(activeChar);
				}
				else
				{
					activeChar.sendMessage("Format is server_list_clock <on/off>");
				}
			}
			else
			{
				activeChar.sendMessage("Format is server_list_clock <on/off>");
			}
		}
		else if (command.equals("admin_server_login"))
		{
			showWindow(activeChar);
		}
		return true;
	}
	
	private void showWindow(L2PcInstance activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/admin/login.htm");
		html.replace("%server_name%", LoginServerThread.getInstance().getServerName());
		html.replace("%status%", LoginServerThread.getInstance().getStatusString());
		html.replace("%clock%", String.valueOf(Config.SERVER_LIST_CLOCK));
		html.replace("%brackets%", String.valueOf(Config.SERVER_LIST_BRACKET));
		html.replace("%max_players%", String.valueOf(LoginServerThread.getInstance().getMaxPlayer()));
		activeChar.sendPacket(html);
		
	}
	
	private void AllowToAll()
	{
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
		Config.SERVER_GMONLY = false;
	}
	
	private void gmOnly()
	{
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_GM_ONLY);
		Config.SERVER_GMONLY = true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.handler.IAdminCommandHandler#getAdminCommandList()
	 */
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}