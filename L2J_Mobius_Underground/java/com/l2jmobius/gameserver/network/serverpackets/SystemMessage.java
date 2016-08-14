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
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

/**
 * @author Forsaiken, UnAfraid
 */
public final class SystemMessage extends AbstractMessagePacket<SystemMessage>
{
	private SystemMessage(SystemMessageId smId)
	{
		super(smId);
	}
	
	public static SystemMessage sendString(String text)
	{
		if (text == null)
		{
			throw new NullPointerException();
		}
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S13);
		sm.addString(text);
		return sm;
	}
	
	public static SystemMessage getSystemMessage(SystemMessageId smId)
	{
		SystemMessage sm = smId.getStaticSystemMessage();
		if (sm != null)
		{
			return sm;
		}
		
		sm = new SystemMessage(smId);
		if (smId.getParamCount() == 0)
		{
			smId.setStaticSystemMessage(sm);
		}
		
		return sm;
	}
	
	/**
	 * Use {@link #getSystemMessage(SystemMessageId)} where possible instead
	 * @param id
	 * @return the system message associated to the given Id.
	 */
	public static SystemMessage getSystemMessage(int id)
	{
		return getSystemMessage(SystemMessageId.getSystemMessageId(id));
	}
	
	/**
	 * Use SystemMessage.getSystemMessage(SystemMessageId smId) where possible instead
	 * @param id
	 * @deprecated
	 */
	@Deprecated
	private SystemMessage(int id)
	{
		this(SystemMessageId.getSystemMessageId(id));
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.SYSTEM_MESSAGE.writeId(packet);
		
		packet.writeH(getId());
		writeMe(packet);
		return true;
	}
}
