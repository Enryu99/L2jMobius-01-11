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

import javolution.util.FastList;

/**
 * Format : (h) d [dS] h sub id d: number of manors [ d: id S: manor name ]
 * @author l3x
 */
public class ExSendManorList extends L2GameServerPacket
{
	private static final String _S__FE_1B_EXSENDMANORLIST = "[S] FE:1B ExSendManorList";
	
	private final FastList<String> _manors;
	
	public ExSendManorList(FastList<String> manors)
	{
		_manors = manors;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x1B);
		writeD(_manors.size());
		for (int i = 0; i < _manors.size(); i++)
		{
			final int j = i + 1;
			writeD(j);
			writeS(_manors.get(i));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_1B_EXSENDMANORLIST;
	}
}