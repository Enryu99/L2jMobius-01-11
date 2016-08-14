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
package com.l2jmobius.gameserver.network.clientpackets;

import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.items.type.CrystalType;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ExPutIntensiveResultForVariationMake;

/**
 * Fromat(ch) dd
 * @author -Wooden-
 */
public class RequestConfirmRefinerItem extends AbstractRefinePacket
{
	private int _targetItemObjId;
	private int _refinerItemObjId;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_targetItemObjId = packet.readD();
		_refinerItemObjId = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			return;
		}
		
		final L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
		if (refinerItem == null)
		{
			return;
		}
		
		if (!isValid(activeChar, targetItem, refinerItem))
		{
			activeChar.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}
		
		final int refinerItemId = refinerItem.getItem().getId();
		final CrystalType grade = targetItem.getItem().getCrystalType();
		final LifeStone ls = getLifeStone(refinerItemId);
		int gemStoneId = 0;
		if (getGemStoneId(grade) != null)
		{
			for (int id : getGemStoneId(grade))
			{
				if (activeChar.getInventory().getAllItemsByItemId(id) != null)
				{
					gemStoneId = id;
					break;
				}
			}
		}
		activeChar.sendPacket(new ExPutIntensiveResultForVariationMake(_refinerItemObjId, refinerItemId, gemStoneId, getGemStoneCount(grade, ls.getGrade())));
	}
}
