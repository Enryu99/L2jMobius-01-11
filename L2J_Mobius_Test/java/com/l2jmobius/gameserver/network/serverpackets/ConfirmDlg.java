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

import com.l2jmobius.gameserver.network.SystemMessageId;

/**
 * ConfirmDlg server packet implementation.
 * @author kombat, UnAfraid
 */
public class ConfirmDlg extends AbstractMessagePacket<ConfirmDlg>
{
	private int _time;
	private int _requesterId;
	
	public ConfirmDlg(SystemMessageId smId)
	{
		super(smId);
	}
	
	public ConfirmDlg(int id)
	{
		this(SystemMessageId.getSystemMessageId(id));
	}
	
	public ConfirmDlg(String text)
	{
		this(SystemMessageId.S13);
		addString(text);
	}
	
	public ConfirmDlg addTime(int time)
	{
		_time = time;
		return this;
	}
	
	public ConfirmDlg addRequesterId(int id)
	{
		_requesterId = id;
		return this;
	}
	
	@Override
	protected void writeParamsSize(int size)
	{
		writeD(size);
	}
	
	@Override
	protected void writeParamType(int type)
	{
		writeD(type);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xF3);
		writeD(getId());
		writeMe();
		writeD(_time);
		writeD(_requesterId);
	}
}
