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

import java.awt.Polygon;
import java.awt.Shape;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jmobius.Config;
import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.Olympiad;
import com.l2jmobius.gameserver.datatables.NpcTable;
import com.l2jmobius.gameserver.datatables.SpawnTable;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2Spawn;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.entity.DimensionalRift;
import com.l2jmobius.gameserver.model.entity.TvTEvent;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;
import com.l2jmobius.gameserver.util.Util;
import com.l2jmobius.util.Rnd;

import javolution.util.FastList;

/**
 * Thanks to L2Fortress and balancer.ru - kombat
 */
public class DimensionalRiftManager
{
	private static Logger _log = Logger.getLogger(DimensionalRiftManager.class.getName());
	private static DimensionalRiftManager _instance;
	private final Map<Byte, Map<Byte, DimensionalRiftRoom>> _rooms = new HashMap<>(7);
	private final int DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;
	
	public static DimensionalRiftManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new DimensionalRiftManager();
		}
		return _instance;
	}
	
	private DimensionalRiftManager()
	{
		loadRooms();
		loadSpawns();
	}
	
	public DimensionalRiftRoom getRoom(byte type, byte room)
	{
		return _rooms.get(type) == null ? null : _rooms.get(type).get(room);
	}
	
	private void loadRooms()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement s = con.prepareStatement("SELECT * FROM dimensional_rift");
			ResultSet rs = s.executeQuery())
		{
			while (rs.next())
			{
				// 0 waiting room, 1 recruit, 2 soldier, 3 officer, 4 captain , 5 commander, 6 hero
				final byte type = rs.getByte("type");
				final byte room_id = rs.getByte("room_id");
				
				// coords related
				final int xMin = rs.getInt("xMin");
				final int xMax = rs.getInt("xMax");
				final int yMin = rs.getInt("yMin");
				final int yMax = rs.getInt("yMax");
				final int z1 = rs.getInt("zMin");
				final int z2 = rs.getInt("zMax");
				final int xT = rs.getInt("xT");
				final int yT = rs.getInt("yT");
				final int zT = rs.getInt("zT");
				final boolean isBossRoom = rs.getByte("boss") > 0;
				
				if (!_rooms.containsKey(type))
				{
					_rooms.put(type, new HashMap<Byte, DimensionalRiftRoom>(9));
				}
				
				_rooms.get(type).put(room_id, new DimensionalRiftRoom(type, room_id, xMin, xMax, yMin, yMax, z1, z2, xT, yT, zT, isBossRoom));
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Can't load Dimension Rift zones. " + e.getMessage(), e);
		}
		
		final int typeSize = _rooms.keySet().size();
		int roomSize = 0;
		
		for (final byte b : _rooms.keySet())
		{
			roomSize += _rooms.get(b).keySet().size();
		}
		
		_log.info(getClass().getSimpleName() + ": Loaded " + typeSize + " room types with " + roomSize + " rooms.");
	}
	
	public void loadSpawns()
	{
		int countGood = 0, countBad = 0;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(Config.DATAPACK_ROOT + "/data/dimensionalRift.xml");
			if (!file.exists())
			{
				_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't find data/" + file.getName());
				return;
			}
			
			final Document doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			byte type, roomId;
			int mobId, x, y, z, delay, count;
			L2Spawn spawnDat;
			L2NpcTemplate template;
			
			for (Node rift = doc.getFirstChild(); rift != null; rift = rift.getNextSibling())
			{
				if ("rift".equalsIgnoreCase(rift.getNodeName()))
				{
					for (Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					{
						if ("area".equalsIgnoreCase(area.getNodeName()))
						{
							attrs = area.getAttributes();
							type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());
							
							for (Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
							{
								if ("room".equalsIgnoreCase(room.getNodeName()))
								{
									attrs = room.getAttributes();
									roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());
									
									for (Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									{
										if ("spawn".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
											delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
											count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
											
											template = NpcTable.getInstance().getTemplate(mobId);
											if (template == null)
											{
												_log.warning("Template " + mobId + " not found!");
											}
											
											if (!_rooms.containsKey(type))
											{
												_log.warning("Type " + type + " not found!");
											}
											else if (!_rooms.get(type).containsKey(roomId))
											{
												_log.warning("Room " + roomId + " in Type " + type + " not found!");
											}
											
											for (int i = 0; i < count; i++)
											{
												final DimensionalRiftRoom riftRoom = _rooms.get(type).get(roomId);
												x = riftRoom.getRandomX();
												y = riftRoom.getRandomY();
												z = riftRoom.getTeleportCoords()[2];
												
												if ((template != null) && _rooms.containsKey(type) && _rooms.get(type).containsKey(roomId))
												{
													spawnDat = new L2Spawn(template);
													spawnDat.setAmount(1);
													spawnDat.setLocx(x);
													spawnDat.setLocy(y);
													spawnDat.setLocz(z);
													spawnDat.setHeading(-1);
													spawnDat.setRespawnDelay(delay);
													
													SpawnTable.getInstance().addNewSpawn(spawnDat, false);
													_rooms.get(type).get(roomId).getSpawns().add(spawnDat);
													countGood++;
												}
												else
												{
													countBad++;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Error on loading dimensional rift spawns: " + e.getMessage(), e);
		}
		_log.info(getClass().getSimpleName() + ": Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
	}
	
	public void reload()
	{
		for (final byte b : _rooms.keySet())
		{
			for (final byte i : _rooms.get(b).keySet())
			{
				_rooms.get(b).get(i).getSpawns().clear();
			}
			_rooms.get(b).clear();
		}
		_rooms.clear();
		loadRooms();
		loadSpawns();
	}
	
	public boolean checkIfInRiftZone(int x, int y, int z, boolean ignorePeaceZone)
	{
		if (ignorePeaceZone)
		{
			return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z);
		}
		return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z) && !_rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}
	
	public boolean checkIfInPeaceZone(int x, int y, int z)
	{
		return _rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}
	
	public void teleportToWaitingRoom(L2PcInstance player)
	{
		final int[] coords = getRoom((byte) 0, (byte) 0).getTeleportCoords();
		
		// just to avoid critical errors during restart
		if (player.isOnline() == 1)
		{
			player.teleToLocation(coords[0], coords[1], coords[2]);
		}
		else
		{
			player.setXYZInvisible(coords[0], coords[1], coords[2]);
		}
	}
	
	public synchronized void start(L2PcInstance player, byte type, L2NpcInstance npc)
	{
		boolean canPass = true;
		if (!player.isInParty())
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NoParty.htm", npc);
			return;
		}
		
		if (player.getParty().getPartyLeaderOID() != player.getObjectId())
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}
		
		if (player.getParty().isInDimensionalRift())
		{
			handleCheat(player, npc);
			return;
		}
		
		if (player.getParty().getMemberCount() < Config.RIFT_MIN_PARTY_SIZE)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/seven_signs/rift/SmallParty.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", Integer.toString(Config.RIFT_MIN_PARTY_SIZE));
			player.sendPacket(html);
			return;
		}
		
		// max parties inside is rooms count - 1
		if (!isAllowedEnter(type))
		{
			player.sendMessage("Rift is full. Try later.");
			return;
		}
		
		for (final L2PcInstance p : player.getParty().getPartyMembers())
		{
			if (!checkIfInPeaceZone(p.getX(), p.getY(), p.getZ()))
			{
				canPass = false;
				break;
			}
		}
		
		if (!canPass)
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotInWaitingRoom.htm", npc);
			return;
		}
		
		L2ItemInstance i;
		final int count = getNeededItems(type);
		for (final L2PcInstance p : player.getParty().getPartyMembers())
		{
			if (p.isFestivalParticipant())
			{
				player.sendMessage("Somebody among the party members has registered to participate in the Festival of Darkness.");
				return;
			}
			
			if (Olympiad.getInstance().isRegisteredInComp(p))
			{
				player.sendMessage("Somebody among the party members has registered to participate in the Grand Olympiad games.");
				return;
			}
			
			if (TvTEvent.isRegistered(p))
			{
				player.sendMessage("Somebody among the party members has registered to participate in the TvT Event.");
				return;
			}
			
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			if (i == null)
			{
				canPass = false;
				break;
			}
			
			if (i.getCount() > 0)
			{
				if (i.getCount() < getNeededItems(type))
				{
					canPass = false;
					break;
				}
			}
			else
			{
				canPass = false;
				break;
			}
		}
		
		if (!canPass)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/seven_signs/rift/NoFragments.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", Integer.toString(count));
			player.sendPacket(html);
			return;
		}
		
		for (final L2PcInstance p : player.getParty().getPartyMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			if (!p.destroyItem("RiftEntrance", i, count, null, false))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile("data/html/seven_signs/rift/NoFragments.htm");
				html.replace("%npc_name%", npc.getName());
				html.replace("%count%", Integer.toString(count));
				player.sendPacket(html);
				return;
			}
		}
		
		byte room;
		FastList<Byte> emptyRooms;
		do
		{
			emptyRooms = getFreeRooms(type);
			room = emptyRooms.get(Rnd.get(1, emptyRooms.size()) - 1);
		}
		// find empty room
		while (_rooms.get(type).get(room).ispartyInside());
		new DimensionalRift(player.getParty(), type, room);
	}
	
	public void killRift(DimensionalRift d)
	{
		if (d.getTeleportTimerTask() != null)
		{
			d.getTeleportTimerTask().cancel();
			d.setTeleportTimerTask(null);
		}
		
		if (d.getTeleportTimer() != null)
		{
			d.getTeleportTimer().cancel();
			d.setTeleportTimer(null);
		}
		
		if (d.getSpawnTimerTask() != null)
		{
			d.getSpawnTimerTask().cancel();
			d.setSpawnTimerTask(null);
		}
		
		if (d.getSpawnTimer() != null)
		{
			d.getSpawnTimer().cancel();
			d.setSpawnTimer(null);
		}
	}
	
	public class DimensionalRiftRoom
	{
		@SuppressWarnings("unused")
		private final byte _type;
		final byte _room;
		private final int _xMin;
		private final int _xMax;
		private final int _yMin;
		private final int _yMax;
		private final int _zMin;
		private final int _zMax;
		private final int[] _teleportCoords;
		private final Shape _s;
		private final boolean _isBossRoom;
		private final FastList<L2Spawn> _roomSpawns;
		@SuppressWarnings("unused")
		private final FastList<L2NpcInstance> _roomMobs;
		private boolean _partyInside = false;
		
		public DimensionalRiftRoom(byte type, byte room, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax, int xT, int yT, int zT, boolean isBossRoom)
		{
			_type = type;
			_room = room;
			_xMin = (xMin + 128);
			_xMax = (xMax - 128);
			_yMin = (yMin + 128);
			_yMax = (yMax - 128);
			_zMin = zMin;
			_zMax = zMax;
			_teleportCoords = new int[]
			{
				xT,
				yT,
				zT
			};
			_isBossRoom = isBossRoom;
			_roomSpawns = new FastList<>();
			_roomMobs = new FastList<>();
			_s = new Polygon(new int[]
			{
				xMin,
				xMax,
				xMax,
				xMin
			}, new int[]
			{
				yMin,
				yMin,
				yMax,
				yMax
			}, 4);
		}
		
		public int getRandomX()
		{
			return Rnd.get(_xMin, _xMax);
		}
		
		public int getRandomY()
		{
			return Rnd.get(_yMin, _yMax);
		}
		
		public int[] getTeleportCoords()
		{
			return _teleportCoords;
		}
		
		public boolean checkIfInZone(int x, int y, int z)
		{
			return _s.contains(x, y) && (z >= _zMin) && (z <= _zMax);
		}
		
		public boolean isBossRoom()
		{
			return _isBossRoom;
		}
		
		public FastList<L2Spawn> getSpawns()
		{
			return _roomSpawns;
		}
		
		public void spawn()
		{
			for (final L2Spawn spawn : _roomSpawns)
			{
				spawn.doSpawn();
				spawn.startRespawn();
			}
		}
		
		public DimensionalRiftRoom unspawn()
		{
			for (final L2Spawn spawn : _roomSpawns)
			{
				spawn.stopRespawn();
				if (spawn.getLastSpawn() != null)
				{
					spawn.getLastSpawn().deleteMe();
				}
				spawn.decreaseCount(null);
			}
			return this;
		}
		
		/**
		 * @return the _partyInside
		 */
		public boolean ispartyInside()
		{
			return _partyInside;
		}
		
		public void setpartyInside(boolean partyInside)
		{
			_partyInside = partyInside;
		}
	}
	
	private int getNeededItems(byte type)
	{
		switch (type)
		{
			case 1:
				return Config.RIFT_ENTER_COST_RECRUIT;
			case 2:
				return Config.RIFT_ENTER_COST_SOLDIER;
			case 3:
				return Config.RIFT_ENTER_COST_OFFICER;
			case 4:
				return Config.RIFT_ENTER_COST_CAPTAIN;
			case 5:
				return Config.RIFT_ENTER_COST_COMMANDER;
			case 6:
				return Config.RIFT_ENTER_COST_HERO;
			default:
				throw new IndexOutOfBoundsException();
		}
	}
	
	public void showHtmlFile(L2PcInstance player, String file, L2NpcInstance npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(file);
		html.replace("%npc_name%", npc.getName());
		player.sendPacket(html);
	}
	
	public void handleCheat(L2PcInstance player, L2NpcInstance npc)
	{
		showHtmlFile(player, "data/html/seven_signs/rift/Cheater.htm", npc);
		if (!player.isGM())
		{
			_log.warning("Player " + player.getName() + "(" + player.getObjectId() + ") was cheating in dimension rift area!");
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " tried to cheat in dimensional rift.", Config.DEFAULT_PUNISH);
		}
	}
	
	public boolean isAllowedEnter(byte type)
	{
		int count = 0;
		for (final Object room : _rooms.get(type).values())
		{
			if (((DimensionalRiftRoom) room).ispartyInside())
			{
				count++;
			}
		}
		return (count < (_rooms.get(type).size() - 1));
	}
	
	public FastList<Byte> getFreeRooms(byte type)
	{
		final FastList<Byte> list = new FastList<>();
		for (final Object room : _rooms.get(type).values())
		{
			if (!((DimensionalRiftRoom) room).ispartyInside())
			{
				list.add(((DimensionalRiftRoom) room)._room);
			}
		}
		return list;
	}
}