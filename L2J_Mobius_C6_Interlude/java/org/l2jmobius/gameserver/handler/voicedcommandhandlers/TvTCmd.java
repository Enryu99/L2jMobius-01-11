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
package org.l2jmobius.gameserver.handler.voicedcommandhandlers;

import org.l2jmobius.gameserver.datatables.ItemTable;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.entity.event.TvT;

public class TvTCmd implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"tvtjoin",
		"tvtleave",
		"tvtinfo"
	};
	
	@Override
	public boolean useVoicedCommand(String command, PlayerInstance activeChar, String target)
	{
		if (command.startsWith("tvtjoin"))
		{
			JoinTvT(activeChar);
		}
		else if (command.startsWith("tvtleave"))
		{
			LeaveTvT(activeChar);
		}
		
		else if (command.startsWith("tvtinfo"))
		{
			TvTinfo(activeChar);
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	public boolean JoinTvT(PlayerInstance activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (!TvT.isJoining())
		{
			activeChar.sendMessage("There is no TvT Event in progress.");
			return false;
		}
		else if (TvT.isJoining() && activeChar._inEventTvT)
		{
			activeChar.sendMessage("You are already registered.");
			return false;
		}
		else if (activeChar.isCursedWeaponEquipped())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because you are holding a Cursed Weapon.");
			return false;
		}
		else if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because you are in Olympiad.");
			return false;
		}
		else if (activeChar.getLevel() < TvT.getMinLevel())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because your level is too low.");
			return false;
		}
		else if (activeChar.getLevel() > TvT.getMaxLevel())
		{
			activeChar.sendMessage("You are not allowed to participate to the event because your level is too high.");
			return false;
		}
		else if (activeChar.getKarma() > 0)
		{
			activeChar.sendMessage("You are not allowed to participate to the event because you have Karma.");
			return false;
		}
		else if (TvT.isTeleport() || TvT.isStarted())
		{
			activeChar.sendMessage("TvT Event registration period is over. You can't register now.");
			return false;
		}
		else
		{
			activeChar.sendMessage("Your participation in the TvT event has been approved.");
			TvT.addPlayer(activeChar, "");
			return false;
		}
	}
	
	public boolean LeaveTvT(PlayerInstance activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (!TvT.isJoining())
		{
			activeChar.sendMessage("There is no TvT Event in progress.");
			return false;
		}
		else if ((TvT.isTeleport() || TvT.isStarted()) && activeChar._inEventTvT)
		{
			activeChar.sendMessage("You can not leave now because TvT event has started.");
			return false;
		}
		else if (TvT.isJoining() && !activeChar._inEventTvT)
		{
			activeChar.sendMessage("You aren't registered in the TvT Event.");
			return false;
		}
		else
		{
			TvT.removePlayer(activeChar);
			return true;
		}
	}
	
	public boolean TvTinfo(PlayerInstance activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (!TvT.isJoining())
		{
			activeChar.sendMessage("There is no TvT Event in progress.");
			return false;
		}
		else if (TvT.isTeleport() || TvT.isStarted())
		{
			activeChar.sendMessage("I can't provide you this info. Command available only in joining period.");
			return false;
		}
		else
		{
			if (TvT._playersShuffle.size() == 1)
			{
				activeChar.sendMessage("There is " + TvT._playersShuffle.size() + " player participating in this event.");
				activeChar.sendMessage("Reward: " + TvT.getRewardAmount() + " " + ItemTable.getInstance().getTemplate(TvT.getRewardId()).getName() + " !");
				activeChar.sendMessage("Player Min lvl: " + TvT.getMinLevel() + ".");
				activeChar.sendMessage("Player Max lvl: " + TvT.getMaxLevel() + ".");
			}
			else
			{
				activeChar.sendMessage("There are " + TvT._playersShuffle.size() + " players participating in this event.");
				activeChar.sendMessage("Reward: " + TvT.getRewardAmount() + " " + ItemTable.getInstance().getTemplate(TvT.getRewardId()).getName() + " !");
				activeChar.sendMessage("Player Min lvl: " + TvT.getMinLevel() + ".");
				activeChar.sendMessage("Player Max lvl: " + TvT.getMaxLevel() + ".");
			}
			return true;
		}
	}
}