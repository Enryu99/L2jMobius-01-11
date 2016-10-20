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

public final class AskJoinPledge extends L2GameServerPacket
{
	private final int _requestorObjId;
	private final String _subPledgeName;
	private final int _pledgeType;
	private final String _pledgeName;
	private final String _askjoinName;
	
	public AskJoinPledge(int requestorObjId, String subPledgeName, int pledgeType, String pledgeName, String askjoinName)
	{
		_requestorObjId = requestorObjId;
		_subPledgeName = subPledgeName;
		_pledgeType = pledgeType;
		_pledgeName = pledgeName;
		_askjoinName = askjoinName;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x2c);
		writeD(_requestorObjId);
		writeS(_askjoinName);
		writeS(_pledgeName);
		if (_pledgeType != 0)
		{
			writeD(_pledgeType);
		}
		if (_subPledgeName != null)
		{
			writeS(_pledgeType > 0 ? _subPledgeName : _pledgeName);
		}
	}
}
