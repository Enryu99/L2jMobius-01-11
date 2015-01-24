/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.serverpackets;

import static com.l2jserver.gameserver.data.xml.impl.MultisellData.PAGE_SIZE;

import com.l2jserver.gameserver.model.multisell.Entry;
import com.l2jserver.gameserver.model.multisell.Ingredient;
import com.l2jserver.gameserver.model.multisell.ListContainer;

public final class MultiSellList extends L2GameServerPacket
{
	private int _size, _index;
	private final ListContainer _list;
	private final boolean _finished;
	
	public MultiSellList(ListContainer list, int index)
	{
		_list = list;
		_index = index;
		_size = list.getEntries().size() - index;
		if (_size > PAGE_SIZE)
		{
			_finished = false;
			_size = PAGE_SIZE;
		}
		else
		{
			_finished = true;
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xD0);
		writeD(_list.getListId()); // list id
		writeC(0x00); // GOD Unknown
		writeD(1 + (_index / PAGE_SIZE)); // page started from 1
		writeD(_finished ? 0x01 : 0x00); // finished
		writeD(PAGE_SIZE); // size of pages
		writeD(_size); // list length
		writeC(_list.isNewMultisell() ? 0x01 : 0x00); // new multisell window
		
		Entry ent;
		while (_size-- > 0)
		{
			ent = _list.getEntries().get(_index++);
			writeD(ent.getEntryId());
			writeC(ent.isStackable() ? 1 : 0);
			writeH(0x00);
			writeD(0x00);
			writeD(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			
			writeH(ent.getProducts().size());
			writeH(ent.getIngredients().size());
			
			for (Ingredient ing : ent.getProducts())
			{
				writeD(ing.getItemId());
				if (ing.getTemplate() != null)
				{
					writeQ(ing.getTemplate().getBodyPart());
					writeH(ing.getTemplate().getType2());
				}
				else
				{
					writeQ(0);
					writeH(65535);
				}
				writeQ(ing.getItemCount());
				if (ing.getItemInfo() != null)
				{
					writeH(ing.getItemInfo().getEnchantLevel()); // enchant level
					writeD(_list.isNewMultisell() ? ing.getChance() : ing.getItemInfo().getAugmentId()); // augment id
					writeD(0x00); // mana
					writeD(0x00); // time ?
					writeH(ing.getItemInfo().getElementId()); // attack element
					writeH(ing.getItemInfo().getElementPower()); // element power
					writeH(ing.getItemInfo().getElementals()[0]); // fire
					writeH(ing.getItemInfo().getElementals()[1]); // water
					writeH(ing.getItemInfo().getElementals()[2]); // wind
					writeH(ing.getItemInfo().getElementals()[3]); // earth
					writeH(ing.getItemInfo().getElementals()[4]); // holy
					writeH(ing.getItemInfo().getElementals()[5]); // dark
				}
				else
				{
					writeH(0x00); // enchant level
					writeD(ing.getChance()); // augment id
					writeD(0x00); // mana
					writeD(0x00); // time ?
					writeH(0x00); // attack element
					writeH(0x00); // element power
					writeH(0x00); // fire
					writeH(0x00); // water
					writeH(0x00); // wind
					writeH(0x00); // earth
					writeH(0x00); // holy
					writeH(0x00); // dark
				}
			}
			
			for (Ingredient ing : ent.getIngredients())
			{
				writeD(ing.getItemId());
				writeH(ing.getTemplate() != null ? ing.getTemplate().getType2() : 65535);
				writeQ(ing.getItemCount());
				if (ing.getItemInfo() != null)
				{
					writeH(ing.getItemInfo().getEnchantLevel()); // enchant level
					writeD(_list.isNewMultisell() ? ing.getChance() : ing.getItemInfo().getAugmentId()); // augment id
					writeD(0x00); // mana
					writeH(ing.getItemInfo().getElementId()); // attack element
					writeH(ing.getItemInfo().getElementPower()); // element power
					writeH(ing.getItemInfo().getElementals()[0]); // fire
					writeH(ing.getItemInfo().getElementals()[1]); // water
					writeH(ing.getItemInfo().getElementals()[2]); // wind
					writeH(ing.getItemInfo().getElementals()[3]); // earth
					writeH(ing.getItemInfo().getElementals()[4]); // holy
					writeH(ing.getItemInfo().getElementals()[5]); // dark
				}
				else
				{
					writeH(0x00); // enchant level
					writeD(ing.getChance()); // augment id
					writeD(0x00); // mana
					writeH(0x00); // attack element
					writeH(0x00); // element power
					writeH(0x00); // fire
					writeH(0x00); // water
					writeH(0x00); // wind
					writeH(0x00); // earth
					writeH(0x00); // holy
					writeH(0x00); // dark
				}
			}
		}
	}
}
