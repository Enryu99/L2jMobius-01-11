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

import com.l2jmobius.gameserver.model.L2Party;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author zabbix
 */
public class PartyMemberPosition extends L2GameServerPacket
{
	L2Party _party;
	
	public PartyMemberPosition(L2PcInstance actor)
	{
		_party = actor.getParty();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xa7);
		writeD(_party.getMemberCount());
		
		for (final L2PcInstance pm : _party.getPartyMembers())
		{
			if (pm == null)
			{
				continue;
			}
			
			writeD(pm.getObjectId());
			writeD(pm.getX());
			writeD(pm.getY());
			writeD(pm.getZ());
		}
	}
	
	@Override
	public String getType()
	{
		return null;
	}
}