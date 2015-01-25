/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.network.serverpackets.ExChangeAttributeInfo;

/**
 * @author Erlandys
 */
public class SendChangeAttributeTargetItem extends L2GameClientPacket
{
	
	private static final String _C__D0_B0_SENDCHANGEATTRIBUTETARGETITEM = "[C] D0:B0 SendChangeAttributeTargetItem";
	int _elementOID;
	int _itemOID;
	
	@Override
	protected void readImpl()
	{
		_elementOID = readD();
		_itemOID = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		L2ItemInstance item = player.getInventory().getItemByObjectId(_itemOID);
		if (item == null)
		{
			return;
		}
		player.sendPacket(new ExChangeAttributeInfo(_elementOID, item.getAttackElementType()));
	}
	
	@Override
	public String getType()
	{
		return _C__D0_B0_SENDCHANGEATTRIBUTETARGETITEM;
	}
}