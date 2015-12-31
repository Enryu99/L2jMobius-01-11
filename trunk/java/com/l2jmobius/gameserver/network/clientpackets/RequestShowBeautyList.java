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

import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.ExResponseBeautyList;

/**
 * @author Sdw
 */
public class RequestShowBeautyList extends L2GameClientPacket
{
	private static final String _C__D0_CA_REQUESTSHOWBEAUTYLIST = "[C] D0:CA RequestShowBeautyList";
	
	private int _type;
	
	@Override
	protected void readImpl()
	{
		_type = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		activeChar.sendPacket(new ExResponseBeautyList(activeChar, _type));
	}
	
	@Override
	public String getType()
	{
		return _C__D0_CA_REQUESTSHOWBEAUTYLIST;
	}
}
