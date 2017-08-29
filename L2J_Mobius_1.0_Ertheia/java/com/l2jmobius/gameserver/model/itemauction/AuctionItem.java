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
package com.l2jmobius.gameserver.model.itemauction;

import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.model.L2Augmentation;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author Forsaiken
 */
public final class AuctionItem
{
	private final int _auctionItemId;
	private final int _auctionLength;
	private final long _auctionInitBid;
	
	private final int _itemId;
	private final long _itemCount;
	private final StatsSet _itemExtra;
	
	public AuctionItem(int auctionItemId, int auctionLength, long auctionInitBid, int itemId, long itemCount, StatsSet itemExtra)
	{
		_auctionItemId = auctionItemId;
		_auctionLength = auctionLength;
		_auctionInitBid = auctionInitBid;
		
		_itemId = itemId;
		_itemCount = itemCount;
		_itemExtra = itemExtra;
	}
	
	public final boolean checkItemExists()
	{
		return ItemTable.getInstance().getTemplate(_itemId) != null;
	}
	
	public final int getAuctionItemId()
	{
		return _auctionItemId;
	}
	
	public final int getAuctionLength()
	{
		return _auctionLength;
	}
	
	public final long getAuctionInitBid()
	{
		return _auctionInitBid;
	}
	
	public final int getItemId()
	{
		return _itemId;
	}
	
	public final long getItemCount()
	{
		return _itemCount;
	}
	
	public final L2ItemInstance createNewItemInstance()
	{
		final L2ItemInstance item = ItemTable.getInstance().createItem("ItemAuction", _itemId, _itemCount, null, null);
		
		item.setEnchantLevel(item.getItem().getDefaultEnchantLevel());
		
		final int augmentationId = _itemExtra.getInt("augmentation_id", 0);
		if (augmentationId > 0)
		{
			item.setAugmentation(new L2Augmentation(augmentationId));
		}
		
		return item;
	}
}