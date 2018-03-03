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
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import com.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import com.l2jmobius.gameserver.util.Util;

public final class RequestPrivateStoreManageBuy extends L2GameClientPacket
{
	private static Logger LOGGER = Logger.getLogger(EnterWorld.class.getName());
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		// Fix for privatestore exploit during login
		if (!player.isVisible() || player.isLocked())
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " try exploit at login with privatestore!", Config.DEFAULT_PUNISH);
			LOGGER.warning("Player " + player.getName() + " try exploit at login with privatestore!");
			return;
		}
		
		// Private store disabled by config
		if (player.isGM() && Config.GM_TRADE_RESTRICTED_ITEMS)
		{
			player.sendMessage("Gm private store disabled by config!");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// If player is in store mode /offline_shop like L2OFF
		if (player.isStored())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (player.isAlikeDead())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInOlympiadMode())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// You can't open store when the task is launched
		if (player.isSittingTaskLaunched())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Like L2OFF - You can't open buy/sell when you are sitting
		if (player.isSitting() && (player.getPrivateStoreType() == 0))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isSitting() && (player.getPrivateStoreType() != 0))
		{
			player.standUp();
		}
		
		if (player.getMountType() != 0)
		{
			return;
		}
		
		if ((player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY) || (player.getPrivateStoreType() == (L2PcInstance.STORE_PRIVATE_BUY + 1)))
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
		}
		
		if (player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
		{
			if (player.isSitting())
			{
				player.standUp();
			}
			
			if (Config.SELL_BY_ITEM)
			{
				final CreatureSay cs11 = new CreatureSay(0, 15, "", "ATTENTION: Store System is not based on Adena, be careful!"); // 8D
				player.sendPacket(cs11);
			}
			
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY + 1);
			player.sendPacket(new PrivateStoreManageListBuy(player));
		}
	}
}