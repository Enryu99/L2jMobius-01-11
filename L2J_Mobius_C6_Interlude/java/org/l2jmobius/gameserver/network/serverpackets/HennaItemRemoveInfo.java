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

public class HennaItemRemoveInfo extends GameServerPacket
{
	private final PlayerInstance _player;
	private final Henna _henna;
	
	public HennaItemRemoveInfo(Henna henna, PlayerInstance player)
	{
		_henna = henna;
		_player = player;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xe6);
		writeD(_henna.getSymbolId()); // symbol Id
		writeD(_henna.getDyeId()); // item id of dye
		writeD(Henna.getRequiredDyeAmount() / 2); // amount of given dyes
		writeD(_henna.getPrice() / 5); // amount of required adenas
		writeD(1); // able to remove or not 0 is false and 1 is true
		writeD(_player.getAdena());
		
		writeD(_player.getINT()); // current INT
		writeC(_player.getINT() - _henna.getINT()); // equip INT
		writeD(_player.getSTR()); // current STR
		writeC(_player.getSTR() - _henna.getSTR()); // equip STR
		writeD(_player.getCON()); // current CON
		writeC(_player.getCON() - _henna.getCON()); // equip CON
		writeD(_player.getMEN()); // current MEM
		writeC(_player.getMEN() - _henna.getMEN()); // equip MEM
		writeD(_player.getDEX()); // current DEX
		writeC(_player.getDEX() - _henna.getDEX()); // equip DEX
		writeD(_player.getWIT()); // current WIT
		writeC(_player.getWIT() - _henna.getWIT()); // equip WIT
	}
}