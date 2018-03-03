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

import com.l2jmobius.gameserver.model.L2ManufactureItem;
import com.l2jmobius.gameserver.model.L2ManufactureList;
import com.l2jmobius.gameserver.model.L2RecipeList;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * dd d(dd) d(ddd)
 * @version $Revision: 1.1.2.2.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class RecipeShopManageList extends L2GameServerPacket
{
	private final L2PcInstance _seller;
	private final boolean _isDwarven;
	private L2RecipeList[] _recipes;
	
	public RecipeShopManageList(L2PcInstance seller, boolean isDwarven)
	{
		_seller = seller;
		_isDwarven = isDwarven;
		
		if (_isDwarven && _seller.hasDwarvenCraft())
		{
			_recipes = _seller.getDwarvenRecipeBook();
		}
		else
		{
			_recipes = _seller.getCommonRecipeBook();
		}
		
		// clean previous recipes
		if (_seller.getCreateList() != null)
		{
			final L2ManufactureList list = _seller.getCreateList();
			for (L2ManufactureItem item : list.getList())
			{
				if (item.isDwarven() != _isDwarven)
				{
					list.getList().remove(item);
				}
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xd8);
		writeD(_seller.getObjectId());
		writeD(_seller.getAdena());
		writeD(_isDwarven ? 0x00 : 0x01);
		
		if (_recipes == null)
		{
			writeD(0);
		}
		else
		{
			writeD(_recipes.length);// number of items in recipe book
			
			for (int i = 0; i < _recipes.length; i++)
			{
				final L2RecipeList temp = _recipes[i];
				writeD(temp.getId());
				writeD(i + 1);
			}
		}
		
		if (_seller.getCreateList() == null)
		{
			writeD(0);
		}
		else
		{
			final L2ManufactureList list = _seller.getCreateList();
			writeD(list.size());
			
			for (L2ManufactureItem item : list.getList())
			{
				writeD(item.getRecipeId());
				writeD(0x00);
				writeD(item.getCost());
			}
		}
	}
}
