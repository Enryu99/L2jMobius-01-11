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
package org.l2jmobius.gameserver.model.waypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.idfactory.IdFactory;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;

public class WayPointNode extends WorldObject
{
	private int _id;
	private String _title;
	private String _type;
	private static final String NORMAL = "Node";
	private static final String SELECTED = "Selected";
	private static final String LINKED = "Linked";
	private static int _lineId = 5560;
	private static final String LINE_TYPE = "item";
	private final Map<WayPointNode, List<WayPointNode>> _linkLists;
	
	public WayPointNode(int objectId)
	{
		super(objectId);
		_linkLists = Collections.synchronizedMap(new WeakHashMap<WayPointNode, List<WayPointNode>>());
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	public static WayPointNode spawn(String type, int id, int x, int y, int z)
	{
		final WayPointNode newNode = new WayPointNode(IdFactory.getInstance().getNextId());
		newNode.getPoly().setPolyInfo(type, id + "");
		newNode.spawnMe(x, y, z);
		
		return newNode;
	}
	
	public static WayPointNode spawn(boolean isItemId, int id, PlayerInstance player)
	{
		return spawn(isItemId ? "item" : "npc", id, player.getX(), player.getY(), player.getZ());
	}
	
	public static WayPointNode spawn(boolean isItemId, int id, Location point)
	{
		return spawn(isItemId ? "item" : "npc", id, point.getX(), point.getY(), point.getZ());
	}
	
	public static WayPointNode spawn(Location point)
	{
		return spawn(Config.NEW_NODE_TYPE, Config.NEW_NODE_ID, point.getX(), point.getY(), point.getZ());
	}
	
	public static WayPointNode spawn(PlayerInstance player)
	{
		return spawn(Config.NEW_NODE_TYPE, Config.NEW_NODE_ID, player.getX(), player.getY(), player.getZ());
	}
	
	@Override
	public void onAction(PlayerInstance player)
	{
		if (player.getTarget() != this)
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
		}
	}
	
	public void setNormalInfo(String type, int id, String title)
	{
		_type = type;
		changeID(id, title);
	}
	
	public void setNormalInfo(String type, int id)
	{
		_type = type;
		changeID(id);
	}
	
	private void changeID(int id)
	{
		_id = id;
		toggleVisible();
		toggleVisible();
	}
	
	private void changeID(int id, String title)
	{
		setName(title);
		setTitle(title);
		changeID(id);
	}
	
	public void setLinked()
	{
		changeID(Config.LINKED_NODE_ID, LINKED);
	}
	
	public void setNormal()
	{
		changeID(Config.NEW_NODE_ID, NORMAL);
	}
	
	public void setSelected()
	{
		changeID(Config.SELECTED_NODE_ID, SELECTED);
	}
	
	public final String getTitle()
	{
		return _title;
	}
	
	public final void setTitle(String title)
	{
		_title = title;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public void setType(String type)
	{
		_type = type;
	}
	
	public static void drawLine(WayPointNode nodeA, WayPointNode nodeB)
	{
		int x1 = nodeA.getX();
		int y1 = nodeA.getY();
		int z1 = nodeA.getZ();
		final int x2 = nodeB.getX();
		final int y2 = nodeB.getY();
		final int z2 = nodeB.getZ();
		final int modX = (x1 - x2) > 0 ? -1 : 1;
		final int modY = (y1 - y2) > 0 ? -1 : 1;
		final int modZ = (z1 - z2) > 0 ? -1 : 1;
		
		final int diffX = Math.abs(x1 - x2);
		final int diffY = Math.abs(y1 - y2);
		final int diffZ = Math.abs(z1 - z2);
		
		final int distance = (int) Math.sqrt((diffX * diffX) + (diffY * diffY) + (diffZ * diffZ));
		
		final int steps = distance / 40;
		
		List<WayPointNode> lineNodes = new ArrayList<>();
		
		for (int i = 0; i < steps; i++)
		{
			x1 = x1 + ((modX * diffX) / steps);
			y1 = y1 + ((modY * diffY) / steps);
			z1 = z1 + ((modZ * diffZ) / steps);
			
			lineNodes.add(spawn(LINE_TYPE, _lineId, x1, y1, z1));
		}
		
		nodeA.addLineInfo(nodeB, lineNodes);
		nodeB.addLineInfo(nodeA, lineNodes);
	}
	
	public void addLineInfo(WayPointNode node, List<WayPointNode> line)
	{
		_linkLists.put(node, line);
	}
	
	public static void eraseLine(WayPointNode target, WayPointNode selectedNode)
	{
		List<WayPointNode> lineNodes = target.getLineInfo(selectedNode);
		
		if (lineNodes == null)
		{
			return;
		}
		
		for (WayPointNode node : lineNodes)
		{
			node.decayMe();
		}
		
		target.eraseLine(selectedNode);
		selectedNode.eraseLine(target);
	}
	
	public void eraseLine(WayPointNode target)
	{
		_linkLists.remove(target);
	}
	
	private List<WayPointNode> getLineInfo(WayPointNode selectedNode)
	{
		return _linkLists.get(selectedNode);
	}
	
	public static void setLineId(int line_id)
	{
		_lineId = line_id;
	}
	
	public List<WayPointNode> getLineNodes()
	{
		final List<WayPointNode> list = new ArrayList<>();
		
		for (List<WayPointNode> points : _linkLists.values())
		{
			list.addAll(points);
		}
		
		return list;
	}
}
