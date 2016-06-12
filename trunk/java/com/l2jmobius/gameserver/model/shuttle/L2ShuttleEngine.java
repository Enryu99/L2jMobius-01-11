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
package com.l2jmobius.gameserver.model.shuttle;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.data.xml.impl.DoorData;
import com.l2jmobius.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2ShuttleInstance;

/**
 * @author UnAfraid
 */
public class L2ShuttleEngine implements Runnable
{
	private static final Logger _log = Logger.getLogger(L2ShuttleEngine.class.getName());
	
	private static final int DELAY = 15 * 1000;
	
	private final L2ShuttleInstance _shuttle;
	private int _cycle = 0;
	private final L2DoorInstance _door1;
	private final L2DoorInstance _door2;
	
	public L2ShuttleEngine(L2ShuttleData data, L2ShuttleInstance shuttle)
	{
		_shuttle = shuttle;
		_door1 = DoorData.getInstance().getDoor(data.getDoors().get(0));
		_door2 = DoorData.getInstance().getDoor(data.getDoors().get(1));
	}
	
	// TODO: Rework me..
	@Override
	public void run()
	{
		try
		{
			if (!_shuttle.isSpawned())
			{
				return;
			}
			switch (_cycle)
			{
				case 0:
				{
					_door1.openMe();
					_door2.closeMe();
					_shuttle.openDoor(0);
					_shuttle.closeDoor(1);
					_shuttle.broadcastShuttleInfo();
					ThreadPoolManager.getInstance().scheduleGeneral(this, DELAY);
					break;
				}
				case 1:
				{
					_door1.closeMe();
					_door2.closeMe();
					_shuttle.closeDoor(0);
					_shuttle.closeDoor(1);
					_shuttle.broadcastShuttleInfo();
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
					break;
				}
				case 2:
				{
					_shuttle.executePath(_shuttle.getShuttleData().getRoutes().get(0));
					break;
				}
				case 3:
				{
					_door1.closeMe();
					_door2.openMe();
					_shuttle.openDoor(1);
					_shuttle.closeDoor(0);
					_shuttle.broadcastShuttleInfo();
					ThreadPoolManager.getInstance().scheduleGeneral(this, DELAY);
					break;
				}
				case 4:
				{
					_door1.closeMe();
					_door2.closeMe();
					_shuttle.closeDoor(0);
					_shuttle.closeDoor(1);
					_shuttle.broadcastShuttleInfo();
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
					break;
				}
				case 5:
				{
					_shuttle.executePath(_shuttle.getShuttleData().getRoutes().get(1));
					break;
				}
			}
			
			_cycle++;
			if (_cycle > 5)
			{
				_cycle = 0;
			}
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, e.getMessage(), e);
		}
	}
}
