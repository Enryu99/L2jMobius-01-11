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
 * This class ...
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 * @author godson
 */
public class ExOlympiadMode extends L2GameServerPacket
{
	private static final String _S__FE_2B_OLYMPIADMODE = "[S] FE:2B ExOlympiadMode";
	private static int _mode;
	
	/**
	 * @param mode (0 = return, 3 = spectate)
	 */
	public ExOlympiadMode(int mode)
	{
		_mode = mode;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x2b);
		writeC(_mode);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_2B_OLYMPIADMODE;
	}
}