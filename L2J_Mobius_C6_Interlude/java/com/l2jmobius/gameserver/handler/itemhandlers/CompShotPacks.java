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
package com.l2jmobius.gameserver.handler.itemhandlers;

import com.l2jmobius.gameserver.handler.IItemHandler;
import com.l2jmobius.gameserver.model.actor.Playable;
import com.l2jmobius.gameserver.model.actor.instance.ItemInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ItemList;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class CompShotPacks implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		5134,
		5135,
		5136,
		5137,
		5138,
		5139,
		5250,
		5251,
		5252,
		5253,
		5254,
		5255
	};
	
	@Override
	public void useItem(Playable playable, ItemInstance item)
	{
		if (!(playable instanceof PlayerInstance))
		{
			return;
		}
		
		PlayerInstance player = (PlayerInstance) playable;
		
		final int itemId = item.getItemId();
		int itemToCreateId = 0;
		int amount = 0; // default regular pack
		
		if ((itemId >= 5134) && (itemId <= 5139)) // SS
		{
			if (itemId == 5134)
			{
				itemToCreateId = 1835;
			}
			else
			{
				itemToCreateId = itemId - 3672;
			}
			
			amount = 300;
		}
		else if ((itemId >= 5250) && (itemId <= 5255)) // Greater SS
		{
			if (itemId == 5250)
			{
				itemToCreateId = 1835;
			}
			else
			{
				itemToCreateId = itemId - 3788;
			}
			
			amount = 1000;
		}
		else if ((itemId >= 5140) && (itemId <= 5145)) // SpS
		{
		}
		else if ((itemId >= 5256) && (itemId <= 5261)) // Greater SpS
		{
		}
		
		player.getInventory().destroyItem("Extract", item, player, null);
		player.getInventory().addItem("Extract", itemToCreateId, amount, player, item);
		
		SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(itemToCreateId);
		sm.addNumber(amount);
		player.sendPacket(sm);
		
		ItemList playerUI = new ItemList(player, false);
		player.sendPacket(playerUI);
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
