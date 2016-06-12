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

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.model.actor.L2Summon;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

/**
 * @author Yme
 */
public class PetStatusShow implements IClientOutgoingPacket
{
	private final int _summonType;
	private final int _summonObjectId;
	
	public PetStatusShow(L2Summon summon)
	{
		_summonType = summon.getSummonType();
		_summonObjectId = summon.getObjectId();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PET_STATUS_SHOW.writeId(packet);
		
		packet.writeD(_summonType);
		packet.writeD(_summonObjectId);
		return true;
	}
}
