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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.entity.siege.Castle;
import org.l2jmobius.gameserver.model.entity.siege.Fort;
import org.l2jmobius.gameserver.network.serverpackets.FortSiegeAttackerList;
import org.l2jmobius.gameserver.network.serverpackets.SiegeAttackerList;

/**
 * @author programmos
 */
public final class RequestSiegeAttackerList extends GameClientPacket
{
	private int _castleId;
	
	@Override
	protected void readImpl()
	{
		_castleId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (_castleId < 100)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
			
			if (castle == null)
			{
				return;
			}
			
			final SiegeAttackerList sal = new SiegeAttackerList(castle);
			sendPacket(sal);
		}
		else
		{
			final Fort fort = FortManager.getInstance().getFortById(_castleId);
			
			if (fort == null)
			{
				return;
			}
			
			final FortSiegeAttackerList sal = new FortSiegeAttackerList(fort);
			sendPacket(sal);
		}
	}
}
