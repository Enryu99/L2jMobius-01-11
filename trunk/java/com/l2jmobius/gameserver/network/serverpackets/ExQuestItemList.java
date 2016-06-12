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

import java.util.Collection;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

/**
 * @author JIV
 */
public class ExQuestItemList extends AbstractItemPacket
{
	private final L2PcInstance _activeChar;
	private final Collection<L2ItemInstance> _items;
	
	public ExQuestItemList(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
		_items = activeChar.getInventory().getItems(L2ItemInstance::isQuestItem);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_QUEST_ITEM_LIST.writeId(packet);
		
		packet.writeH(_items.size());
		for (L2ItemInstance item : _items)
		{
			writeItem(packet, item);
		}
		writeInventoryBlock(packet, _activeChar.getInventory());
		return true;
	}
}
