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
 * sample 0000: 85 00 00 00 00 f0 1a 00 00
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class SetupGauge extends L2GameServerPacket
{
	private static final String _S__85_SETUPGAUGE = "[S] 6d SetupGauge";
	public static final int BLUE = 0;
	public static final int RED = 1;
	public static final int CYAN = 2;
	
	private final int _dat1;
	private final int _time;
	private final int _time2;
	
	public SetupGauge(int dat1, int time)
	{
		_dat1 = dat1; // color 0-blue 1-red 2-cyan 3-green
		_time = time;
		_time2 = time;
	}
	
	public SetupGauge(int color, int currentTime, int maxTime)
	{
		_dat1 = color; // color 0-blue 1-red 2-cyan 3-green
		_time = currentTime;
		_time2 = maxTime;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x6d);
		writeD(_dat1);
		writeD(_time);
		writeD(_time2);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__85_SETUPGAUGE;
	}
}