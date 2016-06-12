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
package com.l2jmobius.gameserver.network.serverpackets;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

public final class SpawnItem implements IClientOutgoingPacket
{
	private final int _objectId;
	private int _itemId;
	private final int _x, _y, _z;
	private int _stackable;
	private long _count;
	
	public SpawnItem(L2Object obj)
	{
		_objectId = obj.getObjectId();
		_x = obj.getX();
		_y = obj.getY();
		_z = obj.getZ();
		
		if (obj.isItem())
		{
			final L2ItemInstance item = (L2ItemInstance) obj;
			_itemId = item.getDisplayId();
			_stackable = item.isStackable() ? 0x01 : 0x00;
			_count = item.getCount();
		}
		else
		{
			_itemId = obj.getPoly().getPolyId();
			_stackable = 0;
			_count = 1;
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.SPAWN_ITEM.writeId(packet);
		
		packet.writeD(_objectId);
		packet.writeD(_itemId);
		
		packet.writeD(_x);
		packet.writeD(_y);
		packet.writeD(_z);
		// only show item count if it is a stackable item
		packet.writeD(_stackable);
		packet.writeQ(_count);
		packet.writeD(0x00); // c2
		return true;
	}
}
