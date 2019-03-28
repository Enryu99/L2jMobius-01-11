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

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.gameserver.TradeController;
import com.l2jmobius.gameserver.datatables.xml.ItemTable;
import com.l2jmobius.gameserver.model.StoreTradeList;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.actor.instance.ItemInstance;
import com.l2jmobius.gameserver.model.actor.instance.MercManagerInstance;
import com.l2jmobius.gameserver.model.actor.instance.MerchantInstance;
import com.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.templates.item.Item;
import com.l2jmobius.gameserver.util.Util;

public final class RequestWearItem extends GameClientPacket
{
	protected static final Logger LOGGER = Logger.getLogger(RequestWearItem.class.getName());
	
	protected Future<?> _removeWearItemsTask;
	
	@SuppressWarnings("unused")
	private int _unknow;
	
	/** List of ItemID to Wear */
	private int _listId;
	
	/** Number of Item to Wear */
	private int _count;
	
	/** Table of ItemId containing all Item to Wear */
	private int[] _items;
	
	/** Player that request a Try on */
	protected PlayerInstance _player;
	
	class RemoveWearItemsTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				_player.destroyWearedItems("Wear", null, true);
			}
			catch (Throwable e)
			{
				LOGGER.warning(e.getMessage());
			}
		}
	}
	
	/**
	 * Decrypt the RequestWearItem Client->Server Packet and Create _items table containing all ItemID to Wear.<BR>
	 * <BR>
	 */
	@Override
	protected void readImpl()
	{
		// Read and Decrypt the RequestWearItem Client->Server Packet
		_player = getClient().getPlayer();
		_unknow = readD();
		_listId = readD(); // List of ItemID to Wear
		_count = readD(); // Number of Item to Wear
		
		if (_count < 0)
		{
			_count = 0;
		}
		
		if (_count > 100)
		{
			_count = 0; // prevent too long lists
		}
		
		// Create _items table that will contain all ItemID to Wear
		_items = new int[_count];
		
		// Fill _items table with all ItemID to Wear
		for (int i = 0; i < _count; i++)
		{
			final int itemId = readD();
			_items[i] = itemId;
		}
	}
	
	/**
	 * Launch Wear action.<BR>
	 * <BR>
	 */
	@Override
	protected void runImpl()
	{
		// Get the current player and return if null
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!Config.ALLOW_WEAR)
		{
			player.sendMessage("Item wear is disabled");
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
		}
		
		// If Alternate rule Karma punishment is set to true, forbid Wear to player with Karma
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (player.getKarma() > 0))
		{
			return;
		}
		
		// Check current target of the player and the INTERACTION_DISTANCE
		final WorldObject target = player.getTarget();
		if (!player.isGM() && ((target == null // No target (ie GM Shop)
		) || (!(target instanceof MerchantInstance) && !(target instanceof MercManagerInstance)) // Target not a merchant and not mercmanager
			|| !player.isInsideRadius(target, NpcInstance.INTERACTION_DISTANCE, false, false)))
		{
			return; // Distance is too far
		}
		
		StoreTradeList list = null;
		
		// Get the current merchant targeted by the player
		final MerchantInstance merchant = (target != null) && (target instanceof MerchantInstance) ? (MerchantInstance) target : null;
		if (merchant == null)
		{
			LOGGER.warning("Null merchant!");
			return;
		}
		
		final List<StoreTradeList> lists = TradeController.getInstance().getBuyListByNpcId(merchant.getNpcId());
		if (lists == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
			return;
		}
		
		for (StoreTradeList tradeList : lists)
		{
			if (tradeList.getListId() == _listId)
			{
				list = tradeList;
			}
		}
		
		if (list == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
			return;
		}
		
		_listId = list.getListId();
		
		// Check if the quantity of Item to Wear
		if ((_count < 1) || (_listId >= 1000000))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Total Price of the Try On
		long totalPrice = 0;
		
		// Check for buylist validity and calculates summary values
		int slots = 0;
		int weight = 0;
		
		for (int i = 0; i < _count; i++)
		{
			final int itemId = _items[i];
			
			if (!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
				return;
			}
			
			final Item template = ItemTable.getInstance().getTemplate(itemId);
			weight += template.getWeight();
			slots++;
			
			totalPrice += Config.WEAR_PRICE;
			if (totalPrice > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}
		}
		
		// Check the weight
		if (!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		
		// Check the inventory capacity
		if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		
		// Charge buyer and add tax to castle treasury if not owned by npc clan because a Try On is not Free
		if ((totalPrice < 0) || !player.reduceAdena("Wear", (int) totalPrice, player.getLastFolkNPC(), false))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}
		
		// Proceed the wear
		final InventoryUpdate playerIU = new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			final int itemId = _items[i];
			
			if (!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
				return;
			}
			
			// If player doesn't own this item : Add this ItemInstance to Inventory and set properties lastchanged to ADDED and _wear to True
			// If player already own this item : Return its ItemInstance (will not be destroy because property _wear set to False)
			final ItemInstance item = player.getInventory().addWearItem("Wear", itemId, player, merchant);
			
			// Equip player with this item (set its location)
			player.getInventory().equipItemAndRecord(item);
			
			// Add this Item in the InventoryUpdate Server->Client Packet
			playerIU.addItem(item);
		}
		
		// Send the InventoryUpdate Server->Client Packet to the player
		// Add Items in player inventory and equip them
		player.sendPacket(playerIU);
		
		// Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD (0x0e) information
		final StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		// Send a Server->Client packet UserInfo to this PlayerInstance and CharInfo to all PlayerInstance in its _KnownPlayers
		player.broadcastUserInfo();
		
		// All weared items should be removed in ALLOW_WEAR_DELAY sec.
		if (_removeWearItemsTask == null)
		{
			_removeWearItemsTask = ThreadPool.schedule(new RemoveWearItemsTask(), Config.WEAR_DELAY * 1000);
		}
	}
}
