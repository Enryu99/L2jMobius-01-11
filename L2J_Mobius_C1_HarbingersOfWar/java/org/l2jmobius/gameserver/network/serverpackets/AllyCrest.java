/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.l2jmobius.gameserver.network.serverpackets;

import java.io.File;
import java.io.FileInputStream;

public class AllyCrest extends ServerBasePacket
{
	private final File _crestFile;
	private final int _crestId;
	
	public AllyCrest(int crestId, File crestFile)
	{
		_crestFile = crestFile;
		_crestId = crestId;
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0xC7);
		writeD(_crestId);
		try
		{
			final FileInputStream fis = new FileInputStream(_crestFile);
			// BufferedInputStream bfis = new BufferedInputStream(fis);
			final int crestSize = fis.available();
			writeD(crestSize);
			int temp = -1;
			while ((temp = fis.read()) != -1)
			{
				writeC(temp);
			}
			fis.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
