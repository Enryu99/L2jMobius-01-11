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
package com.l2jmobius.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.instancemanager.tasks.StartMovingTask;
import com.l2jmobius.gameserver.model.L2NpcWalkerNode;
import com.l2jmobius.gameserver.model.L2WalkRoute;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.WalkInfo;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jmobius.gameserver.model.actor.tasks.npc.walker.ArrivedTask;
import com.l2jmobius.gameserver.model.events.EventDispatcher;
import com.l2jmobius.gameserver.model.events.impl.character.npc.OnNpcMoveNodeArrived;
import com.l2jmobius.gameserver.model.holders.NpcRoutesHolder;
import com.l2jmobius.gameserver.network.NpcStringId;
import com.l2jmobius.util.data.xml.IXmlReader;

/**
 * This class manages walking monsters.
 * @author GKR
 */
public final class WalkingManager implements IXmlReader
{
	// Repeat style:
	// -1 - no repeat
	// 0 - go back
	// 1 - go to first point (circle style)
	// 2 - teleport to first point (conveyor style)
	// 3 - random walking between points.
	public static final byte NO_REPEAT = -1;
	public static final byte REPEAT_GO_BACK = 0;
	public static final byte REPEAT_GO_FIRST = 1;
	public static final byte REPEAT_TELE_FIRST = 2;
	public static final byte REPEAT_RANDOM = 3;
	
	private final Map<String, L2WalkRoute> _routes = new HashMap<>(); // all available routes
	private final Map<Integer, WalkInfo> _activeRoutes = new HashMap<>(); // each record represents NPC, moving by predefined route from _routes, and moving progress
	private final Map<Integer, NpcRoutesHolder> _routesToAttach = new HashMap<>(); // each record represents NPC and all available routes for it
	
	protected WalkingManager()
	{
		load();
	}
	
	@Override
	public final void load()
	{
		parseDatapackFile("data/Routes.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _routes.size() + " walking routes.");
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node d = doc.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("route"))
			{
				final String routeName = parseString(d.getAttributes(), "name");
				final boolean repeat = parseBoolean(d.getAttributes(), "repeat");
				final String repeatStyle = d.getAttributes().getNamedItem("repeatStyle").getNodeValue().toLowerCase();
				
				final byte repeatType;
				switch (repeatStyle)
				{
					case "back":
					{
						repeatType = REPEAT_GO_BACK;
						break;
					}
					case "cycle":
					{
						repeatType = REPEAT_GO_FIRST;
						break;
					}
					case "conveyor":
					{
						repeatType = REPEAT_TELE_FIRST;
						break;
					}
					case "random":
					{
						repeatType = REPEAT_RANDOM;
						break;
					}
					default:
					{
						repeatType = NO_REPEAT;
						break;
					}
				}
				
				final List<L2NpcWalkerNode> list = new ArrayList<>();
				for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling())
				{
					if (r.getNodeName().equals("point"))
					{
						final NamedNodeMap attrs = r.getAttributes();
						final int x = parseInteger(attrs, "X");
						final int y = parseInteger(attrs, "Y");
						final int z = parseInteger(attrs, "Z");
						final int delay = parseInteger(attrs, "delay");
						final boolean run = parseBoolean(attrs, "run");
						NpcStringId npcString = null;
						String chatString = null;
						
						Node node = attrs.getNamedItem("string");
						if (node != null)
						{
							chatString = node.getNodeValue();
						}
						else
						{
							node = attrs.getNamedItem("npcString");
							if (node != null)
							{
								npcString = NpcStringId.getNpcStringId(node.getNodeValue());
								if (npcString == null)
								{
									LOGGER.warning(getClass().getSimpleName() + ": Unknown npcString '" + node.getNodeValue() + "' for route '" + routeName + "'");
									continue;
								}
							}
							else
							{
								node = attrs.getNamedItem("npcStringId");
								if (node != null)
								{
									npcString = NpcStringId.getNpcStringId(Integer.parseInt(node.getNodeValue()));
									if (npcString == null)
									{
										LOGGER.warning(getClass().getSimpleName() + ": Unknown npcString '" + node.getNodeValue() + "' for route '" + routeName + "'");
										continue;
									}
								}
							}
						}
						list.add(new L2NpcWalkerNode(x, y, z, delay, run, npcString, chatString));
					}
					
					else if (r.getNodeName().equals("target"))
					{
						final NamedNodeMap attrs = r.getAttributes();
						try
						{
							final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							final int x = Integer.parseInt(attrs.getNamedItem("spawnX").getNodeValue());
							final int y = Integer.parseInt(attrs.getNamedItem("spawnY").getNodeValue());
							final int z = Integer.parseInt(attrs.getNamedItem("spawnZ").getNodeValue());
							
							final NpcRoutesHolder holder = _routesToAttach.containsKey(npcId) ? _routesToAttach.get(npcId) : new NpcRoutesHolder();
							holder.addRoute(routeName, new Location(x, y, z));
							_routesToAttach.put(npcId, holder);
						}
						catch (Exception e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Error in target definition for route '" + routeName + "'");
						}
					}
				}
				_routes.put(routeName, new L2WalkRoute(routeName, list, repeat, false, repeatType));
			}
		}
	}
	
	/**
	 * @param npc NPC to check
	 * @return {@code true} if given NPC, or its leader is controlled by Walking Manager and moves currently.
	 */
	public boolean isOnWalk(L2Npc npc)
	{
		final L2MonsterInstance monster = npc.isMonster() ? ((L2MonsterInstance) npc).getLeader() == null ? (L2MonsterInstance) npc : ((L2MonsterInstance) npc).getLeader() : null;
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return false;
		}
		
		final WalkInfo walk = monster != null ? _activeRoutes.get(monster.getObjectId()) : _activeRoutes.get(npc.getObjectId());
		return !walk.isStoppedByAttack() && !walk.isSuspended();
	}
	
	public L2WalkRoute getRoute(String route)
	{
		return _routes.get(route);
	}
	
	/**
	 * @param npc NPC to check
	 * @return {@code true} if given NPC controlled by Walking Manager.
	 */
	public boolean isRegistered(L2Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId());
	}
	
	/**
	 * @param npc
	 * @return name of route
	 */
	public String getRouteName(L2Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId()) ? _activeRoutes.get(npc.getObjectId()).getRoute().getName() : "";
	}
	
	/**
	 * Start to move given NPC by given route
	 * @param npc NPC to move
	 * @param routeName name of route to move by
	 */
	public void startMoving(L2Npc npc, String routeName)
	{
		if (_routes.containsKey(routeName) && (npc != null) && !npc.isDead()) // check, if these route and NPC present
		{
			if (!_activeRoutes.containsKey(npc.getObjectId())) // new walk task
			{
				// only if not already moved / not engaged in battle... should not happens if called on spawn
				if ((npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ACTIVE) || (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE))
				{
					final WalkInfo walk = new WalkInfo(routeName);
					L2NpcWalkerNode node = walk.getCurrentNode();
					
					// adjust next waypoint, if NPC spawns at first waypoint
					if ((npc.getX() == node.getX()) && (npc.getY() == node.getY()))
					{
						walk.calculateNextNode(npc);
						node = walk.getCurrentNode();
					}
					
					if (!npc.isInsideRadius(node, 3000, true, false))
					{
						final String message = "Route '" + routeName + "': NPC (id=" + npc.getId() + ", x=" + npc.getX() + ", y=" + npc.getY() + ", z=" + npc.getZ() + ") is too far from starting point (node x=" + node.getX() + ", y=" + node.getY() + ", z=" + node.getZ() + ", range=" + npc.calculateDistance(node, true, true) + "), walking will not start";
						LOGGER.warning(getClass().getSimpleName() + ": " + message);
						return;
					}
					
					if (node.runToLocation())
					{
						npc.setRunning();
					}
					else
					{
						npc.setWalking();
					}
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, node);
					walk.setWalkCheckTask(ThreadPoolManager.scheduleAtFixedRate(new StartMovingTask(npc, routeName), 60000, 60000)); // start walk check task, for resuming walk after fight
					
					npc.getKnownList().startTrackingTask();
					
					_activeRoutes.put(npc.getObjectId(), walk); // register route
				}
				else
				{
					ThreadPoolManager.schedule(new StartMovingTask(npc, routeName), 60000);
				}
			}
			// walk was stopped due to some reason (arrived to node, script action, fight or something else), resume it
			else if (_activeRoutes.containsKey(npc.getObjectId()) && ((npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ACTIVE) || (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)))
			{
				final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
				if (walk == null)
				{
					return;
				}
				
				// Prevent call simultaneously from scheduled task and onArrived() or temporarily stop walking for resuming in future
				if (walk.isBlocked() || walk.isSuspended())
				{
					return;
				}
				
				walk.setBlocked(true);
				final L2NpcWalkerNode node = walk.getCurrentNode();
				if (node.runToLocation())
				{
					npc.setRunning();
				}
				else
				{
					npc.setWalking();
				}
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, node);
				walk.setBlocked(false);
				walk.setStoppedByAttack(false);
			}
		}
	}
	
	/**
	 * Cancel NPC moving permanently
	 * @param npc NPC to cancel
	 */
	public synchronized void cancelMoving(L2Npc npc)
	{
		final WalkInfo walk = _activeRoutes.remove(npc.getObjectId());
		if (walk == null)
		{
			return;
		}
		walk.getWalkCheckTask().cancel(true);
		npc.getKnownList().stopTrackingTask();
	}
	
	/**
	 * Resumes previously stopped moving
	 * @param npc NPC to resume
	 */
	public void resumeMoving(L2Npc npc)
	{
		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		if (walk == null)
		{
			return;
		}
		walk.setSuspended(false);
		walk.setStoppedByAttack(false);
		startMoving(npc, walk.getRoute().getName());
	}
	
	/**
	 * Pause NPC moving until it will be resumed
	 * @param npc NPC to pause moving
	 * @param suspend {@code true} if moving was temporarily suspended for some reasons of AI-controlling script
	 * @param stoppedByAttack {@code true} if moving was suspended because of NPC was attacked or desired to attack
	 */
	public void stopMoving(L2Npc npc, boolean suspend, boolean stoppedByAttack)
	{
		final L2MonsterInstance monster = npc.isMonster() ? ((L2MonsterInstance) npc).getLeader() == null ? (L2MonsterInstance) npc : ((L2MonsterInstance) npc).getLeader() : null;
		
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return;
		}
		
		final WalkInfo walk = monster != null ? _activeRoutes.get(monster.getObjectId()) : _activeRoutes.get(npc.getObjectId());
		
		walk.setSuspended(suspend);
		walk.setStoppedByAttack(stoppedByAttack);
		
		if (monster != null)
		{
			monster.stopMove(null);
			monster.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
		else
		{
			npc.stopMove(null);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}
	
	/**
	 * Manage "node arriving"-related tasks: schedule move to next node; send ON_NODE_ARRIVED event to Quest script
	 * @param npc NPC to manage
	 */
	public void onArrived(L2Npc npc)
	{
		if (!_activeRoutes.containsKey(npc.getObjectId()))
		{
			return;
		}
		
		// Notify quest
		EventDispatcher.getInstance().notifyEventAsync(new OnNpcMoveNodeArrived(npc), npc);
		
		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		// Opposite should not happen... but happens sometime
		if ((walk.getCurrentNodeId() < 0) || (walk.getCurrentNodeId() >= walk.getRoute().getNodesCount()))
		{
			return;
		}
		
		final L2NpcWalkerNode node = walk.getRoute().getNodeList().get(walk.getCurrentNodeId());
		if (!npc.isInsideRadius(node, 10, false, false))
		{
			return;
		}
		
		walk.calculateNextNode(npc);
		walk.setBlocked(true); // prevents to be ran from walk check task, if there is delay in this node.
		
		if (node.getNpcString() != null)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, node.getNpcString());
		}
		else if (!node.getChatText().isEmpty())
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, node.getChatText());
		}
		
		ThreadPoolManager.schedule(new ArrivedTask(npc, walk), 100 + (node.getDelay() * 1000L));
	}
	
	/**
	 * Manage "on death"-related tasks: permanently cancel moving of died NPC
	 * @param npc NPC to manage
	 */
	public void onDeath(L2Npc npc)
	{
		cancelMoving(npc);
	}
	
	/**
	 * Manage "on spawn"-related tasks: start NPC moving, if there is route attached to its spawn point
	 * @param npc NPC to manage
	 */
	public void onSpawn(L2Npc npc)
	{
		if (_routesToAttach.containsKey(npc.getId()))
		{
			final String routeName = _routesToAttach.get(npc.getId()).getRouteName(npc);
			if (!routeName.isEmpty())
			{
				startMoving(npc, routeName);
			}
		}
	}
	
	public static WalkingManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final WalkingManager _instance = new WalkingManager();
	}
}