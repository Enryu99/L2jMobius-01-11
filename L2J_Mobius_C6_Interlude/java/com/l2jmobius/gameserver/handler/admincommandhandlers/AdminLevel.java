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
import com.l2jmobius.gameserver.datatables.xml.ExperienceData;
import com.l2jmobius.gameserver.handler.IAdminCommandHandler;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.L2Playable;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.util.BuilderUtil;

public class AdminLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_add_level",
		"admin_set_level"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		final L2Object targetChar = activeChar.getTarget();
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		
		String val = "";
		if (st.countTokens() >= 1)
		{
			val = st.nextToken();
		}
		
		if (actualCommand.equalsIgnoreCase("admin_add_level"))
		{
			try
			{
				if (targetChar instanceof L2Playable)
				{
					((L2Playable) targetChar).getStat().addLevel(Byte.parseByte(val));
				}
			}
			catch (NumberFormatException e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Wrong Number Format");
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_set_level"))
		{
			try
			{
				if ((targetChar == null) || !(targetChar instanceof L2Playable))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT); // incorrect
					return false;
				}
				
				final L2Playable targetPlayer = (L2Playable) targetChar;
				
				final byte lvl = Byte.parseByte(val);
				int max_level = ExperienceData.getInstance().getMaxLevel();
				
				if ((targetChar instanceof L2PcInstance) && ((L2PcInstance) targetPlayer).isSubClassActive())
				{
					max_level = Config.MAX_SUBCLASS_LEVEL;
				}
				
				if ((lvl >= 1) && (lvl <= max_level))
				{
					final long pXp = targetPlayer.getStat().getExp();
					final long tXp = ExperienceData.getInstance().getExpForLevel(lvl);
					
					if (pXp > tXp)
					{
						targetPlayer.getStat().removeExpAndSp(pXp - tXp, 0);
					}
					else if (pXp < tXp)
					{
						targetPlayer.getStat().addExpAndSp(tXp - pXp, 0);
					}
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "You must specify level between 1 and " + ExperienceData.getInstance().getMaxLevel() + ".");
					return false;
				}
			}
			catch (NumberFormatException e)
			{
				BuilderUtil.sendSysMessage(activeChar, "You must specify level between 1 and " + ExperienceData.getInstance().getMaxLevel() + ".");
				return false;
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
