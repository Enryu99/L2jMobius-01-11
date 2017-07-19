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
package com.l2jmobius.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.Announcements;
import com.l2jmobius.gameserver.Shutdown;

/**
 * @author -Nemesiss- L2M
 */
public class DeadLockDetector extends Thread
{
	private static Logger _log = Logger.getLogger(DeadLockDetector.class.getName());
	
	private static final int _sleepTime = Config.DEADLOCK_CHECK_INTERVAL * 1000;
	
	private final ThreadMXBean tmx;
	
	public DeadLockDetector()
	{
		super("DeadLockDetector");
		tmx = ManagementFactory.getThreadMXBean();
	}
	
	@Override
	public final void run()
	{
		boolean deadlock = false;
		while (!deadlock)
		{
			try
			{
				long[] ids;
				// JDK 1.5 only supports the findMonitorDeadlockedThreads()
				// method, so you need to comment out the following three lines
				if (tmx.isSynchronizerUsageSupported())
				{
					ids = tmx.findDeadlockedThreads();
				}
				else
				{
					ids = tmx.findMonitorDeadlockedThreads();
				}
				
				if (ids != null)
				{
					deadlock = true;
					final ThreadInfo[] tis = tmx.getThreadInfo(ids, true, true);
					String info = "DeadLock Found!\n";
					for (final ThreadInfo ti : tis)
					{
						info += ti.toString();
					}
					
					for (final ThreadInfo ti : tis)
					{
						final LockInfo[] locks = ti.getLockedSynchronizers();
						final MonitorInfo[] monitors = ti.getLockedMonitors();
						if ((locks.length == 0) && (monitors.length == 0))
						{
							continue;
						}
						
						ThreadInfo dl = ti;
						info += "Java-level deadlock:\n";
						info += "\t" + dl.getThreadName() + " is waiting to lock " + dl.getLockInfo().toString() + " which is held by " + dl.getLockOwnerName() + "\n";
						while ((dl = tmx.getThreadInfo(new long[]
						{
							dl.getLockOwnerId()
						}, true, true)[0]).getThreadId() != ti.getThreadId())
						{
							info += "\t" + dl.getThreadName() + " is waiting to lock " + dl.getLockInfo().toString() + " which is held by " + dl.getLockOwnerName() + "\n";
						}
					}
					_log.warning(info);
					
					if (Config.RESTART_ON_DEADLOCK)
					{
						final Announcements an = Announcements.getInstance();
						an.announceToAll("Server has stability issues - restarting now.");
						Shutdown.getInstance().startTelnetShutdown("DeadLockDetector - Auto Restart", 60, true);
					}
				}
				Thread.sleep(_sleepTime);
			}
			catch (final Exception e)
			{
				_log.log(Level.WARNING, "DeadLockDetector: ", e);
			}
		}
	}
}