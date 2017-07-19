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
package com.l2jmobius.gameserver.pathfinding;

import java.util.List;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.pathfinding.cellnodes.CellPathFinding;
import com.l2jmobius.gameserver.pathfinding.geonodes.GeoPathFinding;

/**
 * @author -Nemesiss-
 */
public abstract class PathFinding
{
	private static PathFinding _instance;
	
	public static PathFinding getInstance()
	{
		if (_instance == null)
		{
			if (!Config.GEODATA_CELLFINDING)
			{
				// Higher Memory Usage, Smaller Cpu Usage
				return GeoPathFinding.getInstance();
			}
			return CellPathFinding.getInstance();
		}
		return _instance;
	}
	
	public abstract boolean pathNodesExist(short regionoffset);
	
	public abstract List<AbstractNodeLoc> findPath(int x, int y, int z, int tx, int ty, int tz, boolean playable);
	
	/**
	 * Convert geodata position to pathnode position
	 * @param geo_pos
	 * @return pathnode position
	 */
	public short getNodePos(int geo_pos)
	{
		return (short) (geo_pos >> 3); // OK?
	}
	
	/**
	 * Convert node position to pathnode block position
	 * @param node_pos
	 * @return pathnode block position (0...255)
	 */
	public short getNodeBlock(int node_pos)
	{
		return (short) (node_pos % 256);
	}
	
	public byte getRegionX(int node_pos)
	{
		return (byte) ((node_pos >> 8) + 16);
	}
	
	public byte getRegionY(int node_pos)
	{
		return (byte) ((node_pos >> 8) + 10);
	}
	
	public short getRegionOffset(byte rx, byte ry)
	{
		return (short) ((rx << 5) + ry);
	}
	
	/**
	 * Convert pathnode x to World x position
	 * @param node_x
	 * @return
	 */
	public int calculateWorldX(short node_x)
	{
		return L2World.MAP_MIN_X + (node_x * 128) + 48;
	}
	
	/**
	 * Convert pathnode y to World y position
	 * @param node_y
	 * @return
	 */
	public int CalculateWorldY(short node_y)
	{
		return L2World.MAP_MIN_Y + (node_y * 128) + 48;
	}
	
	public String[] getStat()
	{
		return null;
	}
}