/*
 * Copyright (C) 2004-2014 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.geoeditorcon;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Luno, Dezmond
 */
public class GeoEditorThread extends Thread
{
	private static Logger _log = Logger.getLogger(GeoEditorThread.class.getName());
	
	private boolean _working = false;
	
	private int _mode = 0; // 0 - don't send coords, 1 - send each
	
	// validateposition from client, 2 - send in intervals of _sendDelay ms.
	private int _sendDelay = 1000; // default - once in second
	
	private final Socket _geSocket;
	
	private OutputStream _out;
	
	private final FastList<L2PcInstance> _gms;
	
	public GeoEditorThread(Socket ge)
	{
		_geSocket = ge;
		_working = true;
		_gms = new FastList<>();
	}
	
	@Override
	public void interrupt()
	{
		try
		{
			_geSocket.close();
		}
		catch (Exception e)
		{
		}
		super.interrupt();
	}
	
	@Override
	public void run()
	{
		try
		{
			_out = _geSocket.getOutputStream();
			int timer = 0;
			
			while (_working)
			{
				if (!isConnected())
				{
					_working = false;
				}
				
				if ((_mode == 2) && (timer > _sendDelay))
				{
					for (L2PcInstance gm : _gms)
					{
						if (!gm.getClient().getConnection().isClosed())
						{
							sendGmPosition(gm);
						}
						else
						{
							_gms.remove(gm);
						}
					}
					timer = 0;
				}
				
				try
				{
					sleep(100);
					if (_mode == 2)
					{
						timer += 100;
					}
				}
				catch (Exception e)
				{
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "GeoEditor disconnected. " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				_geSocket.close();
			}
			catch (Exception e)
			{
			}
			_working = false;
		}
	}
	
	public void sendGmPosition(int gx, int gy, short z)
	{
		if (!isConnected())
		{
			return;
		}
		try
		{
			synchronized (_out)
			{
				writeC(0x0b); // length 11 bytes!
				writeC(0x01); // Cmd = save cell;
				writeD(gx); // Global coord X;
				writeD(gy); // Global coord Y;
				writeH(z); // Coord Z;
				_out.flush();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "GeoEditor disconnected. " + e.getMessage(), e);
			_working = false;
		}
		finally
		{
			try
			{
				_geSocket.close();
			}
			catch (Exception ex)
			{
			}
			_working = false;
		}
	}
	
	public void sendGmPosition(L2PcInstance _gm)
	{
		sendGmPosition(_gm.getX(), _gm.getY(), (short) _gm.getZ());
	}
	
	public void sendPing()
	{
		if (!isConnected())
		{
			return;
		}
		try
		{
			synchronized (_out)
			{
				writeC(0x01); // length 1 byte!
				writeC(0x02); // Cmd = ping (dummy packet for connection
				// test);
				_out.flush();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "GeoEditor disconnected. " + e.getMessage(), e);
			_working = false;
		}
		finally
		{
			try
			{
				_geSocket.close();
			}
			catch (Exception ex)
			{
			}
			_working = false;
		}
	}
	
	private void writeD(int value) throws IOException
	{
		_out.write(value & 0xff);
		_out.write((value >> 8) & 0xff);
		_out.write((value >> 16) & 0xff);
		_out.write((value >> 24) & 0xff);
	}
	
	private void writeH(int value) throws IOException
	{
		_out.write(value & 0xff);
		_out.write((value >> 8) & 0xff);
	}
	
	private void writeC(int value) throws IOException
	{
		_out.write(value & 0xff);
	}
	
	public void setMode(int value)
	{
		_mode = value;
	}
	
	public void setTimer(int value)
	{
		if (value < 500)
		{
			_sendDelay = 500; // maximum - 2 times per second!
		}
		else if (value > 60000)
		{
			_sendDelay = 60000; // Minimum - 1 time per minute.
		}
		else
		{
			_sendDelay = value;
		}
	}
	
	public void addGM(L2PcInstance gm)
	{
		if (!_gms.contains(gm))
		{
			_gms.add(gm);
		}
	}
	
	public void removeGM(L2PcInstance gm)
	{
		if (_gms.contains(gm))
		{
			_gms.remove(gm);
		}
	}
	
	public boolean isSend(L2PcInstance gm)
	{
		if ((_mode == 1) && _gms.contains(gm))
		{
			return true;
		}
		return false;
	}
	
	private boolean isConnected()
	{
		return _geSocket.isConnected() && !_geSocket.isClosed();
	}
	
	public boolean isWorking()
	{
		sendPing();
		return _working;
	}
	
	public int getMode()
	{
		return _mode;
	}
}
