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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class ShortBuffStatusUpdate implements IClientOutgoingPacket
{
	private final int _skillId;
	private final int _skillLevel;
	private final int _duration;
	
	public ShortBuffStatusUpdate(int skillId, int skillLevel, int duration)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
		_duration = duration;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.SHORT_BUFF_STATUS_UPDATE.writeId(packet);
		packet.writeD(_skillId);
		packet.writeD(_skillLevel);
		packet.writeD(_duration);
		return true;
	}
}