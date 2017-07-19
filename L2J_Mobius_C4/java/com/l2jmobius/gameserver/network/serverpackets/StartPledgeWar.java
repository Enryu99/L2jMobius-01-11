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

public class StartPledgeWar extends L2GameServerPacket
{
	private static final String _S__65_STARTPLEDGEWAR = "[S] 65 StartPledgeWar";
	private final String _pledgeName;
	private final String _char;
	
	public StartPledgeWar(String pledge, String charName)
	{
		_pledgeName = pledge;
		_char = charName;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x65);
		writeS(_char);
		writeS(_pledgeName);
	}
	
	@Override
	public String getType()
	{
		return _S__65_STARTPLEDGEWAR;
	}
}