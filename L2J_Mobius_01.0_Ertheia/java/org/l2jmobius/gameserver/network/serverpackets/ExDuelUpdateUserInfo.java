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
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @author KenM
 */
public class ExDuelUpdateUserInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _player;
	
	public ExDuelUpdateUserInfo(PlayerInstance player)
	{
		_player = player;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_DUEL_UPDATE_USER_INFO.writeId(packet);
		
		packet.writeS(_player.getName());
		packet.writeD(_player.getObjectId());
		packet.writeD(_player.getClassId().getId());
		packet.writeD(_player.getLevel());
		packet.writeD((int) _player.getCurrentHp());
		packet.writeD(_player.getMaxHp());
		packet.writeD((int) _player.getCurrentMp());
		packet.writeD(_player.getMaxMp());
		packet.writeD((int) _player.getCurrentCp());
		packet.writeD(_player.getMaxCp());
		return true;
	}
}
