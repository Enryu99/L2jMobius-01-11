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

import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestDeleteMacro extends GameClientPacket
{
	private int _id;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (getClient().getPlayer() == null)
		{
			return;
		}
		
		// Macro exploit fix
		if (!getClient().getFloodProtectors().getMacro().tryPerformAction("delete macro"))
		{
			return;
		}
		
		getClient().getPlayer().deleteMacro(_id);
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		sm.addString("Delete macro id=" + _id);
		sendPacket(sm);
	}
}
