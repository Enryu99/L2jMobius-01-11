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

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import com.l2jmobius.gameserver.util.Util;

/**
 * @author KenM
 */
public class SetPrivateStoreWholeMsg extends L2GameClientPacket
{
	private static final String _C_D0_4A_SETPRIVATESTOREWHOLEMSG = "[C] D0:4A SetPrivateStoreWholeMsg";
	private static final int MAX_MSG_LENGTH = 29;
	
	private String _msg;
	
	@Override
	protected void readImpl()
	{
		_msg = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if ((player == null) || (player.getSellList() == null))
		{
			return;
		}
		
		if ((_msg != null) && (_msg.length() > MAX_MSG_LENGTH))
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to overflow private store whole message", Config.DEFAULT_PUNISH);
			return;
		}
		
		player.getSellList().setTitle(_msg);
		sendPacket(new ExPrivateStoreSetWholeMsg(player));
	}
	
	@Override
	public String getType()
	{
		return _C_D0_4A_SETPRIVATESTOREWHOLEMSG;
	}
}
