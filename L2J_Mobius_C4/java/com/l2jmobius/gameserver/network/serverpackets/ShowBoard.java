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

import java.util.List;

public class ShowBoard extends L2GameServerPacket
{
	private static final String _S__6E_SHOWBOARD = "[S] 6e ShowBoard";
	
	private final String _htmlCode;
	private final String _id;
	private List<String> _arg;
	
	public ShowBoard(String htmlCode, String id)
	{
		_id = id;
		_htmlCode = htmlCode; // html code must not exceed 8192 bytes
	}
	
	public ShowBoard(List<String> arg)
	{
		_id = "1002";
		_htmlCode = null;
		_arg = arg;
		
	}
	
	private byte[] get1002()
	{
		int len = (_id.getBytes().length * 2) + 2;
		for (final String arg : _arg)
		{
			len += (arg.getBytes().length + 4) * 2;
		}
		
		final byte data[] = new byte[len];
		int i = 0;
		for (int j = 0; j < _id.getBytes().length; j++, i += 2)
		{
			data[i] = _id.getBytes()[j];
			data[i + 1] = 0;
		}
		
		data[i++] = 8;
		data[i++] = 0;
		for (final String arg : _arg)
		{
			for (int j = 0; j < arg.getBytes().length; j++, i += 2)
			{
				data[i] = arg.getBytes()[j];
				data[i + 1] = 0;
			}
			data[i++] = 0x20;
			data[i++] = 0x0;
			data[i++] = 0x8;
			data[i++] = 0x0;
		}
		return data;
	}
	
	@SuppressWarnings("null")
	@Override
	protected final void writeImpl()
	{
		writeC(0x6e);
		writeC(0x01); // c4 1 to show community 00 to hide
		writeS("bypass _bbshome"); // top
		writeS("bypass _bbsgetfav"); // favorite
		writeS("bypass _bbsloc"); // region
		writeS("bypass _bbsclan"); // clan
		writeS("bypass _bbsmemo"); // memo
		writeS("bypass _bbsmail"); // mail
		writeS("bypass _bbsfriends"); // friends
		writeS("bypass bbs_add_fav"); // add fav.
		if (!_id.equals("1002"))
		{
			// getBytes is a very costly operation, and should only be called once
			byte htmlBytes[] = null;
			if (_htmlCode != null)
			{
				htmlBytes = _htmlCode.getBytes();
			}
			
			final byte data[] = new byte[2 + 2 + 2 + (_id.getBytes().length * 2) + (2 * ((_htmlCode != null) ? htmlBytes.length : 0))];
			int i = 0;
			for (int j = 0; j < _id.getBytes().length; j++, i += 2)
			{
				data[i] = _id.getBytes()[j];
				data[i + 1] = 0;
			}
			data[i++] = 8;
			data[i++] = 0;
			if (_htmlCode != null)
			{
				for (int j = 0; j < htmlBytes.length; i += 2, j++)
				{
					data[i] = _htmlCode.getBytes()[j];
					data[i + 1] = 0;
				}
			}
			data[i++] = 0;
			data[i] = 0;
			writeB(data);
		}
		else
		{
			writeB(get1002());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__6E_SHOWBOARD;
	}
}