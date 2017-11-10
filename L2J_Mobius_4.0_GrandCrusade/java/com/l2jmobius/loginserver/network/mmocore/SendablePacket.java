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
package com.l2jmobius.loginserver.network.mmocore;

/**
 * @author KenM
 * @param <T>
 */
public abstract class SendablePacket<T extends MMOClient<?>>extends AbstractPacket<T>
{
	protected final void putInt(int value)
	{
		_buf.putInt(value);
	}
	
	protected final void putDouble(double value)
	{
		_buf.putDouble(value);
	}
	
	protected final void putFloat(float value)
	{
		_buf.putFloat(value);
	}
	
	/**
	 * Write <B>byte</B> to the buffer. <BR>
	 * 8bit integer (00)
	 * @param data
	 */
	protected final void writeC(boolean data)
	{
		_buf.put((byte) (data ? 0x01 : 0x00));
	}
	
	/**
	 * Write <B>byte</B> to the buffer. <BR>
	 * 8bit integer (00)
	 * @param data
	 */
	protected final void writeC(int data)
	{
		_buf.put((byte) data);
	}
	
	/**
	 * Write <B>double</B> to the buffer. <BR>
	 * 64bit double precision float (00 00 00 00 00 00 00 00)
	 * @param value
	 */
	protected final void writeF(double value)
	{
		_buf.putDouble(value);
	}
	
	/**
	 * Write <B>short</B> to the buffer. <BR>
	 * 16bit integer (00 00)
	 * @param value
	 */
	protected final void writeH(int value)
	{
		_buf.putShort((short) value);
	}
	
	/**
	 * Write <B>int</B> to the buffer. <BR>
	 * 32bit integer (00 00 00 00)
	 * @param value
	 */
	protected final void writeD(int value)
	{
		_buf.putInt(value);
	}
	
	/**
	 * Write <B>int</B> to the buffer. <BR>
	 * 32bit integer (00 00 00 00)
	 * @param value
	 */
	protected final void writeD(boolean value)
	{
		_buf.putInt(value ? 0x01 : 0x00);
	}
	
	/**
	 * Write <B>long</B> to the buffer. <BR>
	 * 64bit integer (00 00 00 00 00 00 00 00)
	 * @param value
	 */
	protected final void writeQ(long value)
	{
		_buf.putLong(value);
	}
	
	/**
	 * Write <B>byte[]</B> to the buffer. <BR>
	 * 8bit integer array (00 ...)
	 * @param data
	 */
	protected final void writeB(byte[] data)
	{
		_buf.put(data);
	}
	
	/**
	 * Write <B>String</B> to the buffer.
	 * @param text
	 */
	protected final void writeS(String text)
	{
		if (text != null)
		{
			final int len = text.length();
			for (int i = 0; i < len; i++)
			{
				_buf.putChar(text.charAt(i));
			}
		}
		
		_buf.putChar('\000');
	}
	
	protected abstract void write();
}
