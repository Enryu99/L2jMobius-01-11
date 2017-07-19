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

/**
 * sample
 * <p>
 * 7d c1 b2 e0 4a 00 00 00 00
 * <p>
 * format cdd
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class AskJoinFriend extends L2GameServerPacket
{
	private static final String _S__7d_ASKJoinFriend_0X7d = "[S] 7d AskJoinFriend 0x7d";
	// private static Logger _log = Logger.getLogger(AskJoinFriend.class.getName());
	
	private final String _requestorName;
	
	public AskJoinFriend(String requestorName)
	{
		_requestorName = requestorName;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x7d);
		writeS(_requestorName);
		writeD(0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__7d_ASKJoinFriend_0X7d;
	}
}