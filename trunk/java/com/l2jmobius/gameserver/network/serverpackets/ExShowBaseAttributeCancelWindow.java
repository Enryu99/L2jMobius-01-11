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

import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;

public class ExShowBaseAttributeCancelWindow extends L2GameServerPacket
{
	private final L2ItemInstance[] _items;
	private long _price;
	
	public ExShowBaseAttributeCancelWindow(L2PcInstance player)
	{
		_items = player.getInventory().getElementItems();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x75);
		writeD(_items.length);
		for (L2ItemInstance item : _items)
		{
			writeD(item.getObjectId());
			writeQ(getPrice(item));
		}
	}
	
	/**
	 * TODO: Update prices for Top/Mid/Low S80/S84
	 * @param item
	 * @return
	 */
	private long getPrice(L2ItemInstance item)
	{
		switch (item.getItem().getCrystalType())
		{
			case S:
			{
				if (item.isWeapon())
				{
					_price = 50000;
				}
				else
				{
					_price = 40000;
				}
				break;
			}
			case S80:
			{
				if (item.isWeapon())
				{
					_price = 100000;
				}
				else
				{
					_price = 80000;
				}
				break;
			}
			case S84:
			{
				if (item.isWeapon())
				{
					_price = 200000;
				}
				else
				{
					_price = 160000;
				}
				break;
			}
			case R:
			{
				if (item.isWeapon())
				{
					_price = 250000;
				}
				else
				{
					_price = 240000;
				}
				break;
			}
			case R95:
			{
				if (item.isWeapon())
				{
					_price = 300000;
				}
				else
				{
					_price = 280000;
				}
				break;
			}
			case R99:
			{
				if (item.isWeapon())
				{
					_price = 350000;
				}
				else
				{
					_price = 320000;
				}
				break;
			}
		}
		
		return _price;
	}
}