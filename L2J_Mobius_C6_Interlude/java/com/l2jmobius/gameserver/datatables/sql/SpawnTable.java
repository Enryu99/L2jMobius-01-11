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
package com.l2jmobius.gameserver.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.gameserver.instancemanager.DayNightSpawnManager;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.entity.olympiad.Olympiad;
import com.l2jmobius.gameserver.model.spawn.L2Spawn;
import com.l2jmobius.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class ...
 * @author Nightmare
 * @version $Revision: 1.5.2.6.2.7 $ $Date: 2005/03/27 15:29:18 $
 */
public class SpawnTable
{
	private static final Logger LOGGER = Logger.getLogger(SpawnTable.class.getName());
	
	private static final SpawnTable _instance = new SpawnTable();
	
	private final Map<Integer, L2Spawn> spawntable = new ConcurrentHashMap<>();
	private int npcSpawnCount;
	private int customSpawnCount;
	
	private int _highestId;
	
	public static SpawnTable getInstance()
	{
		return _instance;
	}
	
	private SpawnTable()
	{
		if (!Config.ALT_DEV_NO_SPAWNS)
		{
			fillSpawnTable();
		}
	}
	
	public Map<Integer, L2Spawn> getSpawnTable()
	{
		return spawntable;
	}
	
	private void fillSpawnTable()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement;
			
			if (Config.DELETE_GMSPAWN_ON_CUSTOM)
			{
				statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM spawnlist where id NOT in ( select id from custom_notspawned where isCustom = false ) ORDER BY id");
			}
			else
			{
				statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM spawnlist ORDER BY id");
			}
			
			final ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if (template1.type.equalsIgnoreCase("L2SiegeGuard"))
					{
						// Don't spawn
					}
					else if (template1.type.equalsIgnoreCase("L2RaidBoss"))
					{
						// Don't spawn raidboss
					}
					else if (template1.type.equalsIgnoreCase("L2GrandBoss"))
					{
						// Don't spawn grandboss
					}
					else if (!Config.ALLOW_CLASS_MASTERS && template1.type.equals("L2ClassMaster"))
					{
						// Dont' spawn class masters
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(rset.getInt("id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setX(rset.getInt("locx"));
						spawnDat.setY(rset.getInt("locy"));
						spawnDat.setZ(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						
						final int loc_id = rset.getInt("loc_id");
						
						spawnDat.setLocation(loc_id);
						
						switch (rset.getInt("periodOfDay"))
						{
							case 0: // default
							{
								npcSpawnCount += spawnDat.init();
								break;
							}
							case 1: // Day
							{
								DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
								npcSpawnCount++;
								break;
							}
							case 2: // Night
							{
								DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
								npcSpawnCount++;
								break;
							}
						}
						
						spawntable.put(spawnDat.getId(), spawnDat);
						if (spawnDat.getId() > _highestId)
						{
							_highestId = spawnDat.getId();
						}
						if (spawnDat.getTemplate().getNpcId() == Olympiad.OLY_MANAGER)
						{
							Olympiad.olymanagers.add(spawnDat);
						}
					}
				}
				else
				{
					LOGGER.warning("SpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("SpawnTable: Spawn could not be initialized. " + e);
		}
		
		LOGGER.info("SpawnTable: Loaded " + spawntable.size() + " Npc Spawn Locations. ");
		LOGGER.info("SpawnTable: Total number of NPCs in the world: " + npcSpawnCount);
		
		// -------------------------------Custom Spawnlist----------------------------//
		if (Config.CUSTOM_SPAWNLIST_TABLE)
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				final PreparedStatement statement;
				
				if (Config.DELETE_GMSPAWN_ON_CUSTOM)
				{
					statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM custom_spawnlist where id NOT in ( select id from custom_notspawned where isCustom = false ) ORDER BY id");
				}
				else
				{
					statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM custom_spawnlist ORDER BY id");
				}
				
				final ResultSet rset = statement.executeQuery();
				
				L2Spawn spawnDat;
				L2NpcTemplate template1;
				
				while (rset.next())
				{
					template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
					
					if (template1 != null)
					{
						if (template1.type.equalsIgnoreCase("L2SiegeGuard"))
						{
							// Don't spawn
						}
						else if (template1.type.equalsIgnoreCase("L2RaidBoss"))
						{
							// Don't spawn raidboss
						}
						else if (!Config.ALLOW_CLASS_MASTERS && template1.type.equals("L2ClassMaster"))
						{
							// Dont' spawn class masters
						}
						else
						{
							spawnDat = new L2Spawn(template1);
							spawnDat.setId(rset.getInt("id"));
							spawnDat.setAmount(rset.getInt("count"));
							spawnDat.setX(rset.getInt("locx"));
							spawnDat.setY(rset.getInt("locy"));
							spawnDat.setZ(rset.getInt("locz"));
							spawnDat.setHeading(rset.getInt("heading"));
							spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
							
							final int loc_id = rset.getInt("loc_id");
							
							spawnDat.setLocation(loc_id);
							
							switch (rset.getInt("periodOfDay"))
							{
								case 0: // default
								{
									customSpawnCount += spawnDat.init();
									break;
								}
								case 1: // Day
								{
									DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
									customSpawnCount++;
									break;
								}
								case 2: // Night
								{
									DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
									customSpawnCount++;
									break;
								}
							}
							
							spawntable.put(spawnDat.getId(), spawnDat);
							if (spawnDat.getId() > _highestId)
							{
								_highestId = spawnDat.getId();
							}
						}
					}
					else
					{
						LOGGER.warning("CustomSpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
				rset.close();
				statement.close();
			}
			catch (Exception e)
			{
				LOGGER.warning("CustomSpawnTable: Spawn could not be initialized. " + e);
			}
			
			LOGGER.info("CustomSpawnTable: Loaded " + customSpawnCount + " Npc Spawn Locations. ");
			LOGGER.info("CustomSpawnTable: Total number of NPCs in the world: " + customSpawnCount);
		}
	}
	
	public L2Spawn getTemplate(int id)
	{
		return spawntable.get(id);
	}
	
	public void addNewSpawn(L2Spawn spawn, boolean storeInDb)
	{
		_highestId++;
		spawn.setId(_highestId);
		spawntable.put(_highestId, spawn);
		
		if (storeInDb)
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				final PreparedStatement statement = con.prepareStatement("INSERT INTO " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + "(id,count,npc_templateid,locx,locy,locz,heading,respawn_delay,loc_id) values(?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, spawn.getId());
				statement.setInt(2, spawn.getAmount());
				statement.setInt(3, spawn.getNpcId());
				statement.setInt(4, spawn.getX());
				statement.setInt(5, spawn.getY());
				statement.setInt(6, spawn.getZ());
				statement.setInt(7, spawn.getHeading());
				statement.setInt(8, spawn.getRespawnDelay() / 1000);
				statement.setInt(9, spawn.getLocation());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				LOGGER.warning("SpawnTable: Could not store spawn in the DB. " + e);
			}
		}
	}
	
	public void deleteSpawn(L2Spawn spawn, boolean updateDb)
	{
		if (spawntable.remove(spawn.getId()) == null)
		{
			return;
		}
		
		if (updateDb)
		{
			if (Config.DELETE_GMSPAWN_ON_CUSTOM)
			{
				try (Connection con = DatabaseFactory.getConnection())
				{
					final PreparedStatement statement = con.prepareStatement("Replace into custom_notspawned VALUES (?,?)");
					statement.setInt(1, spawn.getId());
					statement.setBoolean(2, false);
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					LOGGER.warning("SpawnTable: Spawn " + spawn.getId() + " could not be insert into DB. " + e);
				}
			}
			else
			{
				try (Connection con = DatabaseFactory.getConnection())
				{
					final PreparedStatement statement = con.prepareStatement("DELETE FROM " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + " WHERE id=?");
					statement.setInt(1, spawn.getId());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					LOGGER.warning("SpawnTable: Spawn " + spawn.getId() + " could not be removed from DB. " + e);
				}
			}
		}
	}
	
	// just wrapper
	public void reloadAll()
	{
		fillSpawnTable();
	}
	
	/**
	 * Get all the spawn of a NPC<BR>
	 * <BR>
	 * @param activeChar
	 * @param npcId : ID of the NPC to find.
	 * @param teleportIndex
	 */
	public void findNPCInstances(L2PcInstance activeChar, int npcId, int teleportIndex)
	{
		int index = 0;
		for (L2Spawn spawn : spawntable.values())
		{
			if (npcId == spawn.getNpcId())
			{
				index++;
				
				if (teleportIndex > -1)
				{
					if (teleportIndex == index)
					{
						activeChar.teleToLocation(spawn.getX(), spawn.getY(), spawn.getZ(), true);
					}
				}
				else
				{
					activeChar.sendMessage(index + " - " + spawn.getTemplate().name + " (" + spawn.getId() + "): " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ());
				}
			}
		}
		
		if (index == 0)
		{
			activeChar.sendMessage("No current spawns found.");
		}
	}
	
	public Map<Integer, L2Spawn> getAllTemplates()
	{
		return spawntable;
	}
}
