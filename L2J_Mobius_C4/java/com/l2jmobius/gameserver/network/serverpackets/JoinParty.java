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
 * 4c 01 00 00 00
 * <p>
 * format cd
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class JoinParty extends L2GameServerPacket
{
	private static final String _S__4C_JOINPARTY = "[S] 3a JoinParty";
	// private static Logger _log = Logger.getLogger(JoinParty.class.getName());
	
	private final int _response;
	
	/**
	 * @param response
	 */
	public JoinParty(int response)
	{
		_response = response;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x3a);
		writeD(_response);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__4C_JOINPARTY;
	}
}