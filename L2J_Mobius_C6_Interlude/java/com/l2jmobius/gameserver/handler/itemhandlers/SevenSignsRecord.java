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
import com.l2jmobius.gameserver.model.actor.instance.PetInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.serverpackets.SSQStatus;

/**
 * Item Handler for Seven Signs Record
 * @author Tempy
 */
public class SevenSignsRecord implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		5707
	};
	
	@Override
	public void useItem(Playable playable, ItemInstance item)
	{
		PlayerInstance player;
		
		if (playable instanceof PlayerInstance)
		{
			player = (PlayerInstance) playable;
		}
		else if (playable instanceof PetInstance)
		{
			player = ((PetInstance) playable).getOwner();
		}
		else
		{
			return;
		}
		
		SSQStatus ssqs = new SSQStatus(player, 1);
		player.sendPacket(ssqs);
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
