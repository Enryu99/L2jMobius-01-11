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

import com.l2jmobius.gameserver.geodata.GeoData;
import com.l2jmobius.gameserver.handler.IAdminCommandHandler;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.util.BuilderUtil;
import com.l2jmobius.gameserver.util.GeoUtils;

/**
 * @author -Nemesiss-
 */
public class AdminGeodata implements IAdminCommandHandler
{
	private static String[] _adminCommands =
	{
		"admin_geo_pos",
		"admin_geo_spawn_pos",
		"admin_geo_can_move",
		"admin_geo_can_see",
		"admin_geogrid",
		"admin_geomap"
	};
	
	// private static final int REQUIRED_LEVEL = Config.GM_MIN;
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		// if (!Config.ALT_PRIVILEGES_ADMIN)
		// {
		// if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
		// {
		// return false;
		// }
		// }
		
		if (command.equals("admin_geo_pos"))
		{
			final int worldX = activeChar.getX();
			final int worldY = activeChar.getY();
			final int worldZ = activeChar.getZ();
			final int geoX = GeoData.getInstance().getGeoX(worldX);
			final int geoY = GeoData.getInstance().getGeoY(worldY);
			
			if (GeoData.getInstance().hasGeoPos(geoX, geoY))
			{
				BuilderUtil.sendSysMessage(activeChar, "WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoData.getInstance().getNearestZ(geoX, geoY, worldZ));
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "There is no geodata at this position.");
			}
		}
		else if (command.equals("admin_geo_spawn_pos"))
		{
			final int worldX = activeChar.getX();
			final int worldY = activeChar.getY();
			final int worldZ = activeChar.getZ();
			final int geoX = GeoData.getInstance().getGeoX(worldX);
			final int geoY = GeoData.getInstance().getGeoY(worldY);
			
			if (GeoData.getInstance().hasGeoPos(geoX, geoY))
			{
				BuilderUtil.sendSysMessage(activeChar, "WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoData.getInstance().getSpawnHeight(worldX, worldY, worldZ));
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "There is no geodata at this position.");
			}
		}
		else if (command.equals("admin_geo_can_move"))
		{
			final L2Object target = activeChar.getTarget();
			if (target != null)
			{
				if (GeoData.getInstance().canSeeTarget(activeChar, target))
				{
					BuilderUtil.sendSysMessage(activeChar, "Can move beeline.");
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Can not move beeline!");
				}
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "Incorrect Target.");
			}
		}
		else if (command.equals("admin_geo_can_see"))
		{
			final L2Object target = activeChar.getTarget();
			if (target != null)
			{
				if (GeoData.getInstance().canSeeTarget(activeChar, target))
				{
					BuilderUtil.sendSysMessage(activeChar, "Can see target.");
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Cannot see Target.");
				}
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "Incorrect Target.");
			}
		}
		else if (command.equals("admin_geogrid"))
		{
			GeoUtils.debugGrid(activeChar);
		}
		else if (command.equals("admin_geomap"))
		{
			final int x = ((activeChar.getX() - L2World.MAP_MIN_X) >> 15) + L2World.TILE_X_MIN;
			final int y = ((activeChar.getY() - L2World.MAP_MIN_Y) >> 15) + L2World.TILE_Y_MIN;
			BuilderUtil.sendSysMessage(activeChar, "GeoMap: " + x + "_" + y);
		}
		else
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
	
	// private boolean checkLevel(int level)
	// {
	// return (level >= REQUIRED_LEVEL);
	// }
}