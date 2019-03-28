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

import com.l2jmobius.gameserver.model.actor.instance.HennaInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

public class HennaEquipList extends GameServerPacket
{
	private final PlayerInstance _player;
	private final HennaInstance[] _hennaEquipList;
	
	public HennaEquipList(PlayerInstance player, HennaInstance[] hennaEquipList)
	{
		_player = player;
		_hennaEquipList = hennaEquipList;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xe2);
		writeD(_player.getAdena()); // activeChar current amount of aden
		writeD(3); // available equip slot
		// writeD(10); // total amount of symbol available which depends on difference classes
		writeD(_hennaEquipList.length);
		
		for (HennaInstance element : _hennaEquipList)
		{
			/*
			 * Player must have at least one dye in inventory to be able to see the henna that can be applied with it.
			 */
			if (_player.getInventory().getItemByItemId(element.getItemIdDye()) != null)
			{
				writeD(element.getSymbolId()); // symbolid
				writeD(element.getItemIdDye()); // itemid of dye
				writeD(element.getAmountDyeRequire()); // amount of dye require
				writeD(element.getPrice()); // amount of aden require
				writeD(1); // meet the requirement or not
			}
			else
			{
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
			}
		}
	}
}
