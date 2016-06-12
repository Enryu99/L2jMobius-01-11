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

import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.TargetUnselected;

/**
 * This class ...
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestTargetCanceld implements IClientIncomingPacket
{
	private int _unselect;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_unselect = packet.readH();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isLockedTarget())
		{
			activeChar.sendPacket(SystemMessageId.FAILED_TO_REMOVE_ENMITY);
			return;
		}
		
		if (_unselect == 0)
		{
			// Try to abort cast, if that fails, then cancel target.
			final boolean castAborted = activeChar.abortCast();
			if (!castAborted && (activeChar.getTarget() != null))
			{
				activeChar.setTarget(null);
			}
		}
		else if (activeChar.getTarget() != null)
		{
			activeChar.setTarget(null);
		}
		else if (activeChar.isInAirShip())
		{
			activeChar.broadcastPacket(new TargetUnselected(activeChar));
		}
	}
}
