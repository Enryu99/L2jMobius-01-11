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
package org.l2jmobius.gameserver.handler.admincommandhandlers;

import org.l2jmobius.gameserver.datatables.GmListTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * This class handles following admin commands: - gmchat text = sends text to all online GM's - gmchat_menu text = same as gmchat, displays the admin panel after chat
 * @version $Revision: 1.2.4.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminGmChat implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gmchat",
		"admin_snoop",
		"admin_gmchat_menu"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		if (command.startsWith("admin_gmchat"))
		{
			handleGmChat(command, activeChar);
		}
		else if (command.startsWith("admin_snoop"))
		{
			snoop(command, activeChar);
		}
		
		if (command.startsWith("admin_gmchat_menu"))
		{
			AdminHelpPage.showHelpPage(activeChar, "main_menu.htm");
		}
		
		return true;
	}
	
	/**
	 * @param command
	 * @param activeChar
	 */
	private void snoop(String command, PlayerInstance activeChar)
	{
		WorldObject target = null;
		if (command.length() > 12)
		{
			target = World.getInstance().getPlayer(command.substring(12));
		}
		if (target == null)
		{
			target = activeChar.getTarget();
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_MUST_SELECT_A_TARGET);
			return;
		}
		if (!(target instanceof PlayerInstance))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		PlayerInstance player = (PlayerInstance) target;
		player.addSnooper(activeChar);
		activeChar.addSnooped(player);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * @param command
	 * @param activeChar
	 */
	private void handleGmChat(String command, PlayerInstance activeChar)
	{
		try
		{
			int offset = 0;
			
			String text;
			
			if (command.contains("menu"))
			{
				offset = 17;
			}
			else
			{
				offset = 13;
			}
			
			text = command.substring(offset);
			CreatureSay cs = new CreatureSay(0, 9, activeChar.getName(), text);
			GmListTable.broadcastToGMs(cs);
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}
}