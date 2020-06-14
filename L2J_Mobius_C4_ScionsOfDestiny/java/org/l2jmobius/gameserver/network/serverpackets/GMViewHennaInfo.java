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

import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.Henna;

public class GMViewHennaInfo extends GameServerPacket
{
	private final PlayerInstance _player;
	private final Henna[] _hennas = new Henna[3];
	private int _count;
	
	public GMViewHennaInfo(PlayerInstance player)
	{
		_player = player;
		_count = 0;
		for (int i = 0; i < 3; i++)
		{
			final Henna h = _player.getHenna(i + 1);
			if (h != null)
			{
				_hennas[_count++] = h;
			}
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xea);
		
		writeC(_player.getHennaStatINT());
		writeC(_player.getHennaStatSTR());
		writeC(_player.getHennaStatCON());
		writeC(_player.getHennaStatMEN());
		writeC(_player.getHennaStatDEX());
		writeC(_player.getHennaStatWIT());
		
		writeD(3); // slots?
		
		writeD(_count); // size
		for (int i = 0; i < _count; i++)
		{
			writeD(_hennas[i].getSymbolId());
			writeD(_hennas[i].canBeUsedBy(_player) ? _hennas[i].getSymbolId() : 0);
		}
	}
}