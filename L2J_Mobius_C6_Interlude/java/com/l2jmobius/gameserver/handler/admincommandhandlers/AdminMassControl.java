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

import com.l2jmobius.gameserver.handler.IAdminCommandHandler;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.util.BuilderUtil;

/**
 * <b>This class handles Admin mass commands:</b><br>
 * <br>
 * @author Rayan
 */
public class AdminMassControl implements IAdminCommandHandler
{
	private static String[] ADMIN_COMMANDS =
	{
		"admin_masskill",
		"admin_massress"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_mass"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				
				if (st.nextToken().equalsIgnoreCase("kill"))
				{
					int counter = 0;
					
					for (L2PcInstance player : L2World.getInstance().getAllPlayers())
					{
						if (!player.isGM())
						{
							counter++;
							player.getStatus().setCurrentHp(0);
							player.doDie(player);
							BuilderUtil.sendSysMessage(activeChar, "You've Killed " + counter + " players.");
						}
					}
				}
				else if (st.nextToken().equalsIgnoreCase("ress"))
				{
					int counter = 0;
					
					for (L2PcInstance player : L2World.getInstance().getAllPlayers())
					{
						if (!player.isGM() && player.isDead())
						{
							counter++;
							player.doRevive();
							BuilderUtil.sendSysMessage(activeChar, "You've Ressurected " + counter + " players.");
						}
					}
				}
			}
			catch (Exception ex)
			{
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
