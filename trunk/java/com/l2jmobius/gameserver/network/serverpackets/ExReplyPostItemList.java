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

/**
 * @author Migi, DS
 */

public class ExReplyPostItemList extends AbstractItemPacket
{
	L2PcInstance _activeChar;
	private final L2ItemInstance[] _itemList;
	
	public ExReplyPostItemList(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
		_itemList = _activeChar.getInventory().getAvailableItems(true, false, false);
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xB3);
		writeD(_itemList.length);
		for (L2ItemInstance item : _itemList)
		{
			writeItem(item);
		}
	}
}
