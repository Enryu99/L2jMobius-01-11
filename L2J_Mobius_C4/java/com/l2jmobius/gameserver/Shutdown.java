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
package com.l2jmobius.gameserver;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.datatables.OfflineTradersTable;
import com.l2jmobius.gameserver.instancemanager.CastleManorManager;
import com.l2jmobius.gameserver.instancemanager.GrandBossManager;
import com.l2jmobius.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jmobius.gameserver.instancemanager.QuestManager;
import com.l2jmobius.gameserver.instancemanager.RaidBossSpawnManager;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.gameserverpackets.ServerStatus;

/**
 * This class provides the functions for shutting down and restarting the server It closes all open clientconnections and saves all data.
 * @version $Revision: 1.2.4.5 $ $Date: 2005/03/27 15:29:09 $
 */
public class Shutdown extends Thread
{
	private static Logger _log = Logger.getLogger(Shutdown.class.getName());
	private static Shutdown _instance;
	private static Shutdown _counterInstance = null;
	
	private int secondsShut;
	
	private int shutdownMode;
	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	private static String[] _modeText =
	{
		"SIGTERM",
		"shutting down",
		"restarting",
		"aborting"
	};
	
	/**
	 * This function starts a shutdown countdown from Telnet (Copied from Function startShutdown())
	 * @param IP Which Issued shutdown command
	 * @param seconds seconds untill shutdown
	 * @param restart true if the server will restart after shutdown
	 */
	public void startTelnetShutdown(String IP, int seconds, boolean restart)
	{
		final Announcements _an = Announcements.getInstance();
		_log.warning("IP: " + IP + " issued shutdown command. " + _modeText[shutdownMode] + " in " + seconds + " seconds!");
		
		if (restart)
		{
			shutdownMode = GM_RESTART;
		}
		else
		{
			shutdownMode = GM_SHUTDOWN;
		}
		
		if (shutdownMode > 0)
		{
			_an.announceToAll("Attention players!");
			_an.announceToAll("Server is " + _modeText[shutdownMode] + " in " + seconds + " seconds!");
			if ((shutdownMode == 1) || (shutdownMode == 2))
			{
				_an.announceToAll("Please, avoid to use Gatekeepers/SoE");
				_an.announceToAll("during server " + _modeText[shutdownMode] + " procedure.");
			}
		}
		
		if (_counterInstance != null)
		{
			_counterInstance._abort();
		}
		
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}
	
	/**
	 * This function aborts a running countdown
	 * @param IP IP Which Issued shutdown command
	 */
	public void Telnetabort(String IP)
	{
		_log.warning("IP: " + IP + " issued shutdown ABORT. " + _modeText[shutdownMode] + " has been stopped!");
		
		if (_counterInstance != null)
		{
			_counterInstance._abort();
			final Announcements _an = Announcements.getInstance();
			_an.announceToAll("Server aborts " + _modeText[shutdownMode] + " and continues normal operation!");
		}
		
	}
	
	/**
	 * Default constucter is only used internal to create the shutdown-hook instance
	 */
	public Shutdown()
	{
		secondsShut = -1;
		shutdownMode = SIGTERM;
	}
	
	/**
	 * This creates a countdown instance of Shutdown.
	 * @param seconds how many seconds until shutdown
	 * @param restart true is the server shall restart after shutdown
	 */
	public Shutdown(int seconds, boolean restart)
	{
		if (seconds < 0)
		{
			seconds = 0;
		}
		
		secondsShut = seconds;
		
		if (restart)
		{
			shutdownMode = GM_RESTART;
		}
		else
		{
			shutdownMode = GM_SHUTDOWN;
		}
	}
	
	/**
	 * get the shutdown-hook instance the shutdown-hook instance is created by the first call of this function, but it has to be registrered externaly.
	 * @return instance of Shutdown, to be used as shutdown hook
	 */
	public static Shutdown getInstance()
	{
		if (_instance == null)
		{
			_instance = new Shutdown();
		}
		return _instance;
	}
	
	/**
	 * this function is called, when a new thread starts if this thread is the thread of getInstance, then this is the shutdown hook and we save all data and disconnect all clients. after this thread ends, the server will completely exit if this is not the thread of getInstance, then this is a
	 * countdown thread. we start the countdown, and when we finished it, and it was not aborted, we tell the shutdown-hook why we call exit, and then call exit when the exit status of the server is 1, startServer.sh / startServer.bat will restart the server.
	 */
	@Override
	public void run()
	{
		// disallow new logins
		
		if (this == _instance)
		{
			try
			{
				if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
				{
					OfflineTradersTable.storeOffliners();
				}
			}
			catch (final Throwable t)
			{
			}
			
			try
			{
				disconnectAllCharacters();
				_log.info("All players have been disconnected.");
			}
			catch (final Throwable t)
			{
			}
			
			// ensure all services are stopped
			try
			{
				GameTimeController.getInstance().stopTimer();
			}
			catch (final Throwable t)
			{
			}
			
			// stop all threadpools
			try
			{
				ThreadPoolManager.getInstance().shutdown();
			}
			catch (final Throwable t)
			{
			}
			
			try
			{
				LoginServerThread.getInstance().interrupt();
			}
			catch (final Throwable t)
			{
			}
			
			// last bye bye, save all data and quit this server
			saveData();
			
			// saveData sends messages to exit players, so shutdown selector after it
			try
			{
				GameServer.gameServer.getSelectorThread().shutdown();
			}
			catch (final Throwable t)
			{
			}
			
			// commit data, last chance
			try
			{
				L2DatabaseFactory.getInstance().shutdown();
			}
			catch (final Throwable t)
			{
			}
			
			// server will quit, when this function ends.
			if (_instance.shutdownMode == GM_RESTART)
			{
				Runtime.getRuntime().halt(2);
			}
			else
			{
				Runtime.getRuntime().halt(0);
			}
		}
		else
		{
			// gm shutdown: send warnings and then call exit to start shutdown sequence
			countdown();
			
			// last point where logging is operational :(
			_log.warning("GM shutdown countdown is over. " + _modeText[shutdownMode] + " NOW!");
			switch (shutdownMode)
			{
				case GM_SHUTDOWN:
					_instance.setMode(GM_SHUTDOWN);
					System.exit(0);
					break;
				case GM_RESTART:
					_instance.setMode(GM_RESTART);
					System.exit(2);
					break;
			}
		}
	}
	
	/**
	 * This functions starts a shutdown countdown
	 * @param activeChar GM who issued the shutdown command
	 * @param seconds seconds until shutdown
	 * @param restart true if the server will restart after shutdown
	 */
	public void startShutdown(L2PcInstance activeChar, int seconds, boolean restart)
	{
		final Announcements _an = Announcements.getInstance();
		_log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown command. " + _modeText[shutdownMode] + " in " + seconds + " seconds!");
		
		if (restart)
		{
			shutdownMode = GM_RESTART;
		}
		else
		{
			shutdownMode = GM_SHUTDOWN;
		}
		
		if (shutdownMode > 0)
		{
			_an.announceToAll("Attention players!");
			_an.announceToAll("Server is " + _modeText[shutdownMode] + " in " + seconds + " seconds!");
			if ((shutdownMode == 1) || (shutdownMode == 2))
			{
				_an.announceToAll("Please, avoid to use Gatekeepers/SoE");
				_an.announceToAll("during server " + _modeText[shutdownMode] + " procedure.");
			}
		}
		
		if (_counterInstance != null)
		{
			_counterInstance._abort();
		}
		
		// the main instance should only run for shutdown hook, so we start a new instance
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}
	
	/**
	 * This function aborts a running countdown
	 * @param activeChar GM who issued the abort command
	 */
	public void abort(L2PcInstance activeChar)
	{
		_log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown ABORT. " + _modeText[shutdownMode] + " has been stopped!");
		
		if (_counterInstance != null)
		{
			_counterInstance._abort();
			final Announcements _an = Announcements.getInstance();
			_an.announceToAll("Server aborts " + _modeText[shutdownMode] + " and continues normal operation!");
		}
	}
	
	/**
	 * set the shutdown mode
	 * @param mode what mode shall be set
	 */
	private void setMode(int mode)
	{
		shutdownMode = mode;
	}
	
	/**
	 * set shutdown mode to ABORT
	 */
	private void _abort()
	{
		shutdownMode = ABORT;
	}
	
	/**
	 * this counts the countdown and reports it to all players countdown is aborted if mode changes to ABORT
	 */
	private void countdown()
	{
		final Announcements _an = Announcements.getInstance();
		
		try
		{
			while (secondsShut > 0)
			{
				switch (secondsShut)
				{
					case 540:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 9 minutes.");
						break;
					case 480:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 8 minutes.");
						break;
					case 420:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 7 minutes.");
						break;
					case 360:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 6 minutes.");
						break;
					case 300:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 5 minutes.");
						break;
					case 240:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 4 minutes.");
						break;
					case 180:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 3 minutes.");
						break;
					case 120:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 2 minutes.");
						break;
					case 60:
						LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN); // prevents new players from logging in
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 1 minute.");
						break;
					case 30:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 30 seconds.");
						break;
					case 5:
						_an.announceToAll("The server is " + _modeText[shutdownMode] + " in 5 seconds, please log out NOW !");
						break;
				}
				
				secondsShut--;
				
				final int delay = 1000; // milliseconds
				Thread.sleep(delay);
				
				if (shutdownMode == ABORT)
				{
					break;
				}
			}
		}
		catch (final InterruptedException e)
		{
			// this will never happen
		}
	}
	
	/**
	 * this sends a last byebye, disconnects all players and saves data
	 */
	private void saveData()
	{
		final Announcements _an = Announcements.getInstance();
		switch (shutdownMode)
		{
			case SIGTERM:
				_log.info("SIGTERM received. Shutting down NOW!");
				break;
			case GM_SHUTDOWN:
				_log.info("GM shutdown received. Shutting down NOW!");
				break;
			case GM_RESTART:
				_log.info("GM restart received. Restarting NOW!");
				break;
		}
		
		try
		{
			_an.announceToAll("Server is " + _modeText[shutdownMode] + " NOW!");
		}
		catch (final Throwable t)
		{
			_log.log(Level.INFO, "", t);
		}
		
		// Seven Signs data is now saved along with Festival data.
		if (!SevenSigns.getInstance().isSealValidationPeriod())
		{
			SevenSignsFestival.getInstance().saveFestivalData(false);
		}
		
		// Save Seven Signs data before closing. :)
		SevenSigns.getInstance().saveSevenSignsData(null, true);
		
		// Save all raidboss and grandboss status ^_^
		RaidBossSpawnManager.getInstance().cleanUp();
		_log.info("RaidBossSpawnManager: All Raid Boss info saved!!");
		GrandBossManager.getInstance().cleanUp();
		_log.info("GrandBossManager: All Grand Boss info saved!!");
		
		TradeController.getInstance().dataCountStore();
		_log.info("TradeController: All count Item Saved!!");
		
		try
		{
			Olympiad.getInstance().save();
			_log.info("Olympiad System: Data saved!!");
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		
		// Save all manor data
		CastleManorManager.getInstance().save();
		
		// Save all global (non-player specific) Quest data that needs to persist after reboot
		QuestManager.getInstance().save();
		
		// Save items on ground before closing
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().saveInDb();
			ItemsOnGroundManager.getInstance().cleanUp();
			_log.info("ItemsOnGroundManager: All items on ground saved!");
		}
		
		try
		{
			final int delay = 5000;
			Thread.sleep(delay);
		}
		catch (final InterruptedException e)
		{
			// never happens :p
		}
	}
	
	/**
	 * this disconnects all clients from the server
	 */
	private void disconnectAllCharacters()
	{
		for (final L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			// Logout Character
			try
			{
				player.logout(false);
			}
			catch (final Throwable t)
			{
			}
		}
	}
}