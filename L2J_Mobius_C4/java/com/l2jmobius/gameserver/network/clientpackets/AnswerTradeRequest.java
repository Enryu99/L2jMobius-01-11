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
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.SendTradeDone;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 * @version $Revision: 1.5.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class AnswerTradeRequest extends L2GameClientPacket
{
	private static final String _C__40_ANSWERTRADEREQUEST = "[C] 40 AnswerTradeRequest";
	// private static Logger _log = Logger.getLogger(AnswerTradeRequest.class.getName());
	
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (Config.GM_DISABLE_TRANSACTION && (player.getAccessLevel() >= Config.GM_TRANSACTION_MIN) && (player.getAccessLevel() <= Config.GM_TRANSACTION_MAX))
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			sendPacket(new ActionFailed());
			return;
		}
		
		final L2PcInstance partner = player.getActiveRequester();
		if (partner == null)
		{
			// Trade partner not found, cancel trade
			player.sendPacket(new SendTradeDone(0));
			
			player.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_NOT_FOUND_IN_THE_GAME));
			player.setActiveRequester(null);
			
			return;
		}
		
		if ((_response == 1) && !partner.isRequestExpired())
		{
			player.startTrade(partner);
		}
		else
		{
			SystemMessage msg = new SystemMessage(SystemMessage.S1_DENIED_TRADE_REQUEST);
			msg.addString(player.getName());
			partner.sendPacket(msg);
			
			msg = null;
		}
		
		// Clears requesting status
		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__40_ANSWERTRADEREQUEST;
	}
}