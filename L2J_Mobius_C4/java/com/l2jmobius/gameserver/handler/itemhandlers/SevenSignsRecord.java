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
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PetInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PlayableInstance;
import com.l2jmobius.gameserver.network.serverpackets.SSQStatus;

/**
 * Item Handler for Seven Signs Record
 * @author Tempy
 */
public class SevenSignsRecord implements IItemHandler
{
	private static int[] _itemIds =
	{
		5707
	};
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;
		
		if (playable instanceof L2PcInstance)
		{
			activeChar = (L2PcInstance) playable;
		}
		else if (playable instanceof L2PetInstance)
		{
			activeChar = ((L2PetInstance) playable).getOwner();
		}
		else
		{
			return;
		}
		
		final SSQStatus ssqs = new SSQStatus(activeChar, 1);
		activeChar.sendPacket(ssqs);
	}
	
	@Override
	public int[] getItemIds()
	{
		return _itemIds;
	}
}