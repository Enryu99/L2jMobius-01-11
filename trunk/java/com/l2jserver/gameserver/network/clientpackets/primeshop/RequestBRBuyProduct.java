/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.clientpackets.primeshop;

import java.util.Calendar;

import com.l2jserver.Config;
import com.l2jserver.gameserver.data.xml.impl.PrimeShopData;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.request.PrimeShopRequest;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.primeshop.PrimeShopGroup;
import com.l2jserver.gameserver.model.primeshop.PrimeShopItem;
import com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket;
import com.l2jserver.gameserver.network.serverpackets.primeshop.ExBRBuyProduct;
import com.l2jserver.gameserver.network.serverpackets.primeshop.ExBRBuyProduct.ExBrProductReplyType;
import com.l2jserver.gameserver.network.serverpackets.primeshop.ExBRGamePoint;
import com.l2jserver.gameserver.util.Util;

/**
 * @author Gnacik, UnAfraid
 */
public final class RequestBRBuyProduct extends L2GameClientPacket
{
	private static final int HERO_COINS = 23805;
	
	private int _brId;
	private int _count;
	
	@Override
	protected void readImpl()
	{
		_brId = readD();
		_count = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.hasItemRequest() || activeChar.hasRequest(PrimeShopRequest.class))
		{
			activeChar.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return;
		}
		
		activeChar.addRequest(new PrimeShopRequest(activeChar));
		
		final PrimeShopGroup item = PrimeShopData.getInstance().getItem(_brId);
		if (validatePlayer(item, _count, activeChar))
		{
			final int price = (item.getPrice() * _count);
			final int paymentId = validatePaymentId(activeChar, item, price);
			
			if (paymentId < 0)
			{
				activeChar.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.LACK_OF_POINT));
				return;
			}
			else if (paymentId > 0)
			{
				if (!activeChar.destroyItemByItemId("PrimeShop-" + item.getBrId(), paymentId, price, activeChar, true))
				{
					activeChar.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.LACK_OF_POINT));
					return;
				}
			}
			else if (paymentId == 0)
			{
				if (activeChar.getPrimePoints() < price)
				{
					activeChar.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.LACK_OF_POINT));
					return;
				}
				activeChar.setPrimePoints(activeChar.getPrimePoints() - price);
			}
			
			for (PrimeShopItem subItem : item.getItems())
			{
				activeChar.addItem("PrimeShop", subItem.getId(), subItem.getCount() * _count, activeChar, true);
			}
			
			activeChar.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.SUCCESS));
			activeChar.sendPacket(new ExBRGamePoint(activeChar));
		}
		
		activeChar.removeRequest(PrimeShopRequest.class);
	}
	
	/**
	 * @param item
	 * @param count
	 * @param player
	 * @return
	 */
	private static boolean validatePlayer(PrimeShopGroup item, int count, L2PcInstance player)
	{
		final long currentTime = System.currentTimeMillis() / 1000;
		if (item == null)
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_PRODUCT));
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to buy invalid brId from Prime", Config.DEFAULT_PUNISH);
			return false;
		}
		else if ((count < 1) && (count > 99))
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to buy invalid itemcount [" + count + "] from Prime", Config.DEFAULT_PUNISH);
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return false;
		}
		else if ((item.getMinLevel() > 0) && (item.getMinLevel() > player.getLevel()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER));
			return false;
		}
		else if ((item.getMaxLevel() > 0) && (item.getMaxLevel() < player.getLevel()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER));
			return false;
		}
		else if ((item.getMinBirthday() > 0) && (item.getMinBirthday() > player.getBirthdays()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return false;
		}
		else if ((item.getMaxBirthday() > 0) && (item.getMaxBirthday() < player.getBirthdays()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return false;
		}
		else if ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) & item.getDaysOfWeek()) == 0)
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.NOT_DAY_OF_WEEK));
			return false;
		}
		else if ((item.getStartSale() > 1) && (item.getStartSale() > currentTime))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.BEFORE_SALE_DATE));
			return false;
		}
		else if ((item.getEndSale() > 1) && (item.getEndSale() < currentTime))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.AFTER_SALE_DATE));
			return false;
		}
		
		final int weight = item.getWeight() * count;
		final long slots = item.getCount() * count;
		
		if (player.getInventory().validateWeight(weight))
		{
			if (!player.getInventory().validateCapacity(slots))
			{
				player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTROY_OVERFLOW));
				return false;
			}
		}
		else
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTROY_OVERFLOW));
			return false;
		}
		
		return true;
	}
	
	private static int validatePaymentId(L2PcInstance player, PrimeShopGroup item, long amount)
	{
		switch (item.getPaymentType())
		{
			case 0: // Prime points
			{
				return 0;
			}
			case 1: // Adenas
			{
				return Inventory.ADENA_ID;
			}
			case 2: // Hero coins
			{
				return HERO_COINS;
			}
		}
		
		return -1;
	}
	
	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}
