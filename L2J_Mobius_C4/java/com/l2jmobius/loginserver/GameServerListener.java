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
package com.l2jmobius.loginserver;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import com.l2jmobius.Config;

import javolution.util.FastList;

/**
 * This class waits for incoming connections from GameServers and launches (@link GameServerThread GameServerThreads}
 * @author luisantonioa, -Wooden-
 */

public class GameServerListener extends FloodProtectedListener
{
	protected static Logger _log = Logger.getLogger(LoginServer.class.getName());
	private final List<GameServerThread> _gameServerThreads = new FastList<>();
	
	public GameServerListener() throws IOException
	{
		super(Config.GAME_SERVER_LOGIN_HOST, Config.GAME_SERVER_LOGIN_PORT);
		setName(getClass().getSimpleName());
	}
	
	/**
	 * @return Returns the gameServerThreads.
	 */
	public List<GameServerThread> getGameServerThreads()
	{
		return _gameServerThreads;
	}
	
	/**
	 * Removes a GameServerThread from the list
	 * @param gst
	 */
	public void removeGameServer(GameServerThread gst)
	{
		_gameServerThreads.remove(gst);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.loginserver.FloodProtectedListener#addClient(java.net.Socket)
	 */
	@Override
	public void addClient(Socket s)
	{
		final GameServerThread gst = new GameServerThread(s);
		_gameServerThreads.add(gst);
	}
}