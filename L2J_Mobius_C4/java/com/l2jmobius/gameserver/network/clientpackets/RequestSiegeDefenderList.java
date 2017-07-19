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

import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.model.entity.Castle;
import com.l2jmobius.gameserver.network.serverpackets.SiegeDefenderList;

/**
 * This class ...
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestSiegeDefenderList extends L2GameClientPacket
{
	
	private static final String _C__a3_RequestSiegeDefenderList = "[C] a3 RequestSiegeDefenderList";
	// private static Logger _log = Logger.getLogger(RequestJoinParty.class.getName());
	
	private int _CastleId;
	
	@Override
	protected void readImpl()
	{
		_CastleId = readD();
	}
	
	@Override
	public void runImpl()
	{
		final Castle castle = CastleManager.getInstance().getCastleById(_CastleId);
		if (castle == null)
		{
			return;
		}
		final SiegeDefenderList sdl = new SiegeDefenderList(castle);
		sendPacket(sdl);
	}
	
	@Override
	public String getType()
	{
		return _C__a3_RequestSiegeDefenderList;
	}
}
