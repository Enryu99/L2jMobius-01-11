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
package org.l2jmobius.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.ItemInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.entity.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.entity.siege.Castle;

public class CastleManager
{
	
	protected static final Logger LOGGER = Logger.getLogger(CastleManager.class.getName());
	
	public static final CastleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private List<Castle> _castles;
	
	private static final int _castleCirclets[] =
	{
		0,
		6838,
		6835,
		6839,
		6837,
		6840,
		6834,
		6836,
		8182,
		8183
	};
	
	public CastleManager()
	{
		load();
	}
	
	public int findNearestCastlesIndex(WorldObject obj)
	{
		int index = getCastleIndex(obj);
		if (index < 0)
		{
			double closestDistance = 99999999;
			double distance;
			Castle castle;
			for (int i = 0; i < getCastles().size(); i++)
			{
				castle = getCastles().get(i);
				
				if (castle == null)
				{
					continue;
				}
				
				distance = castle.getDistance(obj);
				
				if (closestDistance > distance)
				{
					closestDistance = distance;
					index = i;
				}
			}
		}
		return index;
	}
	
	private final void load()
	{
		LOGGER.info("Initializing CastleManager");
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("Select id from castle order by id");
			final ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				getCastles().add(new Castle(rs.getInt("id")));
			}
			
			rs.close();
			statement.close();
			
			LOGGER.info("Loaded: " + getCastles().size() + " castles");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public Castle getCastleById(int castleId)
	{
		for (Castle temp : getCastles())
		{
			if (temp.getCastleId() == castleId)
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastleByOwner(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		for (Castle temp : getCastles())
		{
			if ((temp != null) && (temp.getOwnerId() == clan.getClanId()))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(String name)
	{
		if (name == null)
		{
			return null;
		}
		
		for (Castle temp : getCastles())
		{
			if (temp.getName().equalsIgnoreCase(name.trim()))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(int x, int y, int z)
	{
		for (Castle temp : getCastles())
		{
			if (temp.checkIfInZone(x, y, z))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(WorldObject activeObject)
	{
		if (activeObject == null)
		{
			return null;
		}
		
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public int getCastleIndex(int castleId)
	{
		Castle castle;
		for (int i = 0; i < getCastles().size(); i++)
		{
			castle = getCastles().get(i);
			if ((castle != null) && (castle.getCastleId() == castleId))
			{
				return i;
			}
		}
		return -1;
	}
	
	public int getCastleIndex(WorldObject activeObject)
	{
		return getCastleIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public int getCastleIndex(int x, int y, int z)
	{
		Castle castle;
		for (int i = 0; i < getCastles().size(); i++)
		{
			castle = getCastles().get(i);
			if ((castle != null) && castle.checkIfInZone(x, y, z))
			{
				return i;
			}
		}
		return -1;
	}
	
	public List<Castle> getCastles()
	{
		if (_castles == null)
		{
			_castles = new ArrayList<>();
		}
		return _castles;
	}
	
	public void validateTaxes(int sealStrifeOwner)
	{
		int maxTax;
		
		switch (sealStrifeOwner)
		{
			case SevenSigns.CABAL_DUSK:
			{
				maxTax = 5;
				break;
			}
			case SevenSigns.CABAL_DAWN:
			{
				maxTax = 25;
				break;
			}
			default: // no owner
			{
				maxTax = 15;
				break;
			}
		}
		
		for (Castle castle : _castles)
		{
			if (castle.getTaxPercent() > maxTax)
			{
				castle.setTaxPercent(maxTax);
			}
		}
	}
	
	int _castleId = 1; // from this castle
	
	public int getCirclet()
	{
		return getCircletByCastleId(_castleId);
	}
	
	public int getCircletByCastleId(int castleId)
	{
		if ((castleId > 0) && (castleId < 10))
		{
			return _castleCirclets[castleId];
		}
		
		return 0;
	}
	
	// remove this castle's circlets from the clan
	public void removeCirclet(Clan clan, int castleId)
	{
		for (ClanMember member : clan.getMembers())
		{
			removeCirclet(member, castleId);
		}
	}
	
	// added: remove clan cirlet for clan leaders
	public void removeCirclet(ClanMember member, int castleId)
	{
		if (member == null)
		{
			return;
		}
		
		PlayerInstance player = member.getPlayerInstance();
		final int circletId = getCircletByCastleId(castleId);
		
		if (circletId != 0)
		{
			// online-player circlet removal
			if (player != null)
			{
				try
				{
					if (player.isClanLeader())
					{
						ItemInstance crown = player.getInventory().getItemByItemId(6841);
						
						if (crown != null)
						{
							if (crown.isEquipped())
							{
								player.getInventory().unEquipItemInSlotAndRecord(crown.getEquipSlot());
							}
							player.destroyItemByItemId("CastleCrownRemoval", 6841, 1, player, true);
						}
					}
					
					ItemInstance circlet = player.getInventory().getItemByItemId(circletId);
					if (circlet != null)
					{
						if (circlet.isEquipped())
						{
							player.getInventory().unEquipItemInSlotAndRecord(circlet.getEquipSlot());
						}
						player.destroyItemByItemId("CastleCircletRemoval", circletId, 1, player, true);
					}
					return;
				}
				catch (NullPointerException e)
				{
					// continue removing offline
				}
			}
			PreparedStatement statement = null;
			try (Connection con = DatabaseFactory.getConnection())
			{
				statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?");
				statement.setInt(1, member.getObjectId());
				statement.setInt(2, 6841);
				statement.execute();
				statement.close();
				
				statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?");
				statement.setInt(1, member.getObjectId());
				statement.setInt(2, circletId);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				LOGGER.info("Failed to remove castle circlets offline for player " + member.getName());
				e.printStackTrace();
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManager INSTANCE = new CastleManager();
	}
}
