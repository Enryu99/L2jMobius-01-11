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

import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format: (ch) dc d: character object id c: 1 if won 0 if failed
 * @author -Wooden-
 */
public class ExFishingEnd extends L2GameServerPacket
{
	private static final String _S__FE_14_EXFISHINGEND = "[S] FE:14 ExFishingEnd";
	private final boolean _win;
	L2Character _character;
	
	public ExFishingEnd(boolean win, L2PcInstance character)
	{
		_win = win;
		_character = character;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x14);
		writeD(_character.getObjectId());
		writeC(_win ? 1 : 0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_14_EXFISHINGEND;
	}
}