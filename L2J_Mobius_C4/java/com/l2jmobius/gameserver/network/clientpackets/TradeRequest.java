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

import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.BlockList;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.SendTradeRequest;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.util.Util;

/**
 * This class ...
 * @version $Revision: 1.2.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class TradeRequest extends L2GameClientPacket
{
	private static final String TRADEREQUEST__C__15 = "[C] 15 TradeRequest";
	private static Logger _log = Logger.getLogger(TradeRequest.class.getName());
	
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
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
		
		final L2Object target = L2World.getInstance().findObject(_objectId);
		if ((target == null) || !player.getKnownList().knowsObject(target) || !(target instanceof L2PcInstance) || (target.getObjectId() == player.getObjectId()))
		{
			player.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
			return;
		}
		
		if (player.isDead())
		{
			player.sendMessage("Dead players cannot request for a trade.");
			return;
		}
		
		if (player.isFishing())
		{
			player.sendMessage("Cannot request for a trade while fishing.");
			return;
		}
		
		final L2PcInstance partner = (L2PcInstance) target;
		if (partner.isInOlympiadMode() || player.isInOlympiadMode())
		{
			player.sendMessage("You or your target cant request trade in Olympiad mode.");
			return;
		}
		
		if (BlockList.isBlocked(partner, player))
		{
			player.sendMessage("This player has added you to his/her block list, therefore you cannot request for a trade.");
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && ((player.getKarma() > 0) || (partner.getKarma() > 0)))
		{
			player.sendMessage("Chaotic players can't use Trade.");
			return;
		}
		
		if ((player.getPrivateStoreType() != 0) || (partner.getPrivateStoreType() != 0))
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			return;
		}
		
		if (player.isProcessingTransaction())
		{
			if (Config.DEBUG)
			{
				_log.fine("already trading with someone");
			}
			player.sendPacket(new SystemMessage(SystemMessage.ALREADY_TRADING));
			return;
		}
		
		if (partner.isProcessingRequest() || partner.isProcessingTransaction())
		{
			if (Config.DEBUG)
			{
				_log.info("transaction already in progress.");
			}
			final SystemMessage sm = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
			sm.addString(partner.getName());
			player.sendPacket(sm);
			
			return;
		}
		
		if (partner.getTradeRefusal())
		{
			player.sendMessage("Target is in trade refusal mode.");
			return;
		}
		
		if (Util.calculateDistance(player, partner, true) > 150)
		{
			player.sendPacket(new SystemMessage(SystemMessage.TARGET_TOO_FAR));
			return;
		}
		
		player.onTransactionRequest(partner);
		partner.sendPacket(new SendTradeRequest(player.getObjectId()));
		final SystemMessage sm = new SystemMessage(SystemMessage.REQUEST_S1_FOR_TRADE);
		sm.addString(partner.getName());
		player.sendPacket(sm);
	}
	
	@Override
	public String getType()
	{
		return TRADEREQUEST__C__15;
	}
}