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
package com.l2jmobius.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mmocore.network.ReceivablePacket;

import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.network.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * Packets received by the game server from clients
 * @author KenM
 */
public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient>
{
	private static Logger _log = Logger.getLogger(L2GameClientPacket.class.getName());
	
	@Override
	protected boolean read()
	{
		try
		{
			readImpl();
			return true;
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + e.getMessage(), e);
			
			if (e instanceof BufferUnderflowException) // only one allowed per client per minute
			{
				if ((GameTimeController.getGameTicks() - getClient().underflowReadStartTick) > 600)
				{
					getClient().underflowReadStartTick = GameTimeController.getGameTicks();
					getClient().underflowReadsInMin = 1;
				}
				else if (++getClient().underflowReadsInMin > 1)
				{
					getClient().closeNow();
					_log.severe("Client " + getClient().toString() + " - Disconnected: Too many buffer underflow exceptions");
				}
			}
		}
		return false;
	}
	
	protected abstract void readImpl();
	
	@Override
	public final void run()
	{
		try
		{
			// flood protection
			
			if ((GameTimeController.getGameTicks() - getClient().packetsSentStartTick) > 10)
			{
				getClient().packetsSentStartTick = GameTimeController.getGameTicks();
				getClient().packetsSentInSec = 0;
			}
			else
			{
				getClient().packetsSentInSec++;
				if (getClient().packetsSentInSec > 12)
				{
					if (getClient().packetsSentInSec < 100)
					{
						sendPacket(new ActionFailed());
					}
					return;
				}
			}
			
			runImpl();
			
			/*
			 * Removes onspawn protection - player has faster computer than average
			 */
			if (triggersOnActionRequest() && (getClient().getActiveChar() != null))
			{
				getClient().getActiveChar().onActionRequest();
			}
			
			cleanUp();
		}
		catch (final Throwable t)
		{
			_log.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed running: " + getType() + " ; " + t.getMessage(), t);
			// in case of EnterWorld error kick player from game
			if (this instanceof EnterWorld)
			{
				getClient().closeNow();
			}
		}
	}
	
	protected abstract void runImpl();
	
	protected final void sendPacket(L2GameServerPacket gsp)
	{
		getClient().sendPacket(gsp);
	}
	
	/**
	 * @return A String with this packet name for debuging purposes
	 */
	public abstract String getType();
	
	/**
	 * Overridden with true value on some packets that should disable spawn protection
	 * @return
	 */
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
	
	protected void cleanUp()
	{
	}
}