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

import java.util.ArrayList;
import java.util.List;

import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author ShanSoft
 */
public class ExBuySellList extends AbstractItemPacket
{
	private final List<L2ItemInstance> _items = new ArrayList<>();
	private final List<L2ItemInstance> _sellList = new ArrayList<>();
	private L2ItemInstance[] _refundList = null;
	private final boolean _done;
	
	public ExBuySellList(L2PcInstance player, boolean done)
	{
		for (L2ItemInstance item : player.getInventory().getItems())
		{
			if (!item.isQuestItem())
			{
				_items.add(item);
			}
		}
		
		for (L2ItemInstance item : player.getInventory().getAvailableItems(false, false, false))
		{
			if (item.isSellable())
			{
				_sellList.add(item);
			}
		}
		
		if (player.hasRefund())
		{
			_refundList = player.getRefund().getItems();
		}
		_done = done;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0xB8);
		writeD(0x01);
		writeD(_items.size());
		
		if (_sellList.size() > 0)
		{
			writeH(_sellList.size());
			for (L2ItemInstance item : _sellList)
			{
				writeItem(item);
				writeQ(item.getItem().getReferencePrice() / 2);
			}
		}
		else
		{
			writeH(0x00);
		}
		
		if ((_refundList != null) && (_refundList.length > 0))
		{
			writeH(_refundList.length);
			int i = 0;
			for (L2ItemInstance item : _refundList)
			{
				writeItem(item);
				writeD(i++);
				writeQ((item.getItem().getReferencePrice() / 2) * item.getCount());
			}
		}
		else
		{
			writeH(0x00);
		}
		
		writeC(_done ? 0x01 : 0x00);
	}
}