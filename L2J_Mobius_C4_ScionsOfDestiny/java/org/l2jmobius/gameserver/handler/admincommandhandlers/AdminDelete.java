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

import org.l2jmobius.gameserver.data.sql.SpawnTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.instancemanager.GrandBossManager;
import org.l2jmobius.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.spawn.Spawn;
import org.l2jmobius.gameserver.util.BuilderUtil;
import org.l2jmobius.gameserver.util.Util;

/**
 * This class handles following admin commands: - delete = deletes target
 */
public class AdminDelete implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_delete"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		if (command.startsWith("admin_delete"))
		{
			final String[] split = command.split(" ");
			handleDelete(activeChar, (split.length > 1) && Util.isDigit(split[1]) ? Integer.parseInt(split[1]) : 0);
		}
		return true;
	}
	
	private void handleDelete(PlayerInstance activeChar, int range)
	{
		if (range > 0)
		{
			for (WorldObject target : World.getInstance().getVisibleObjects(activeChar, range))
			{
				if (!target.isNpc())
				{
					continue;
				}
				deleteNpc(activeChar, (NpcInstance) target);
			}
			return;
		}
		
		final WorldObject obj = activeChar.getTarget();
		if (obj instanceof NpcInstance)
		{
			deleteNpc(activeChar, (NpcInstance) obj);
		}
		else
		{
			BuilderUtil.sendSysMessage(activeChar, "Incorrect target.");
		}
	}
	
	private void deleteNpc(PlayerInstance activeChar, NpcInstance target)
	{
		target.deleteMe();
		
		final Spawn spawn = target.getSpawn();
		if (spawn != null)
		{
			if (GrandBossManager.getInstance().isDefined(spawn.getNpcId()))
			{
				BuilderUtil.sendSysMessage(activeChar, "You cannot delete a grandboss.");
				return;
			}
			
			spawn.stopRespawn();
			
			if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()))
			{
				RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
			}
			else
			{
				SpawnTable.getInstance().deleteSpawn(spawn, true);
			}
		}
		
		BuilderUtil.sendSysMessage(activeChar, "Deleted " + target.getName() + " from " + target.getObjectId() + ".");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
