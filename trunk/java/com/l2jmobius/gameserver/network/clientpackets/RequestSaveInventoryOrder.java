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
package com.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;

import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.enums.ItemLocation;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.itemcontainer.Inventory;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.network.client.L2GameClient;

/**
 * Format:(ch) d[dd]
 * @author -Wooden-
 */
public final class RequestSaveInventoryOrder implements IClientIncomingPacket
{
	private List<InventoryOrder> _order;
	
	/** client limit */
	private static final int LIMIT = 125;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		int sz = packet.readD();
		sz = Math.min(sz, LIMIT);
		_order = new ArrayList<>(sz);
		for (int i = 0; i < sz; i++)
		{
			final int objectId = packet.readD();
			final int order = packet.readD();
			_order.add(new InventoryOrder(objectId, order));
		}
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance player = client.getActiveChar();
		if (player != null)
		{
			final Inventory inventory = player.getInventory();
			for (InventoryOrder order : _order)
			{
				final L2ItemInstance item = inventory.getItemByObjectId(order.objectID);
				if ((item != null) && (item.getItemLocation() == ItemLocation.INVENTORY))
				{
					item.setItemLocation(ItemLocation.INVENTORY, order.order);
				}
			}
		}
	}
	
	private static class InventoryOrder
	{
		int order;
		
		int objectID;
		
		public InventoryOrder(int id, int ord)
		{
			objectID = id;
			order = ord;
		}
	}
}
