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
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

/**
 * @author Sdw
 */
public class ExShowBeautyMenu implements IClientOutgoingPacket
{
	private final L2PcInstance _activeChar;
	private final int _type;
	
	// TODO: Enum
	public final static int MODIFY_APPEARANCE = 0;
	public final static int RESTORE_APPEARANCE = 1;
	
	public ExShowBeautyMenu(L2PcInstance activeChar, int type)
	{
		_activeChar = activeChar;
		_type = type;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_SHOW_BEAUTY_MENU.writeId(packet);
		
		packet.writeD(_type);
		packet.writeD(_activeChar.getVisualHair());
		packet.writeD(_activeChar.getVisualHairColor());
		packet.writeD(_activeChar.getVisualFace());
		return true;
	}
}