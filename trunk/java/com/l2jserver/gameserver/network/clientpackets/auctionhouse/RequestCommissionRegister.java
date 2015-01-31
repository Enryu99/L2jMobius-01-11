/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.clientpackets.auctionhouse;

import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.AuctionHouseManager;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.auctionhouse.ExResponseCommissionInfo;
import com.l2jserver.gameserver.network.serverpackets.auctionhouse.ExResponseCommissionItemList;
import com.l2jserver.gameserver.network.serverpackets.auctionhouse.ExResponseCommissionList;
import com.l2jserver.gameserver.network.serverpackets.auctionhouse.ExResponseCommissionRegister;

/**
 * @author Erlandys
 */
public final class RequestCommissionRegister extends L2GameClientPacket
{
	private static final String _C__D0_9D_REQUESTCOMMISSIONREGISTER = "[C] D0:9D RequestCommissionRegister";
	
	private int _itemOID;
	private String _itemName;
	private long _price;
	private long _count;
	private int _duration;
	
	@Override
	protected void readImpl()
	{
		_itemOID = readD();
		_itemName = readS();
		_price = readQ();
		_count = readQ();
		_duration = readD();
		readQ(); // Unknown
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		long destroyPrice = _price;
		AuctionHouseManager am = AuctionHouseManager.getInstance();
		am.checkForAuctionsDeletion();
		long timeToAdd = 0;
		switch (_duration)
		{
			case 0:
				timeToAdd = 86400000;
				destroyPrice *= 0.0001;
				break;
			case 1:
				timeToAdd = 259200000;
				destroyPrice *= 0.0003;
				break;
			case 2:
				timeToAdd = 432000000;
				destroyPrice *= 0.0005;
				break;
			case 3:
				timeToAdd = 604800000;
				destroyPrice *= 0.0007;
		}
		
		if (destroyPrice < 1000)
		{
			destroyPrice = 1000;
		}
		
		if ((player.getInventory().getItemByItemId(57) == null) || (player.getInventory().getItemByItemId(57).getCount() < destroyPrice))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			reloadAuction(player, false);
			return;
		}
		
		if (player.getInventory().getItemByObjectId(_itemOID) == null)
		{
			player.sendPacket(SystemMessageId.REGISTRATION_IS_NOT_AVAILABLE_BECAUSE_THE_CORRESPONDING_ITEM_DOES_NOT_EXIST);
			reloadAuction(player, false);
			return;
		}
		
		if (player.getInventory().getItemByObjectId(_itemOID).isEquipped())
		{
			player.sendPacket(SystemMessageId.THE_ITEM_THAT_IS_CURRENTLY_WORN_CANNOT_BE_REGISTERED);
			reloadAuction(player, false);
			return;
		}
		
		final int itemID = player.getInventory().getItemByObjectId(_itemOID).getId();
		final L2Item item = ItemTable.getInstance().getTemplate(itemID);
		
		if (((player.getAuctionInventory().getSize() >= 10) && !player.isGM()) || ((player.getAuctionInventory().getSize() >= 99999) && player.isGM()) || !item.isTradeable() || !item.isSellable())
		{
			player.sendPacket(SystemMessageId.THE_ITEM_CANNOT_BE_REGISTERED_BECAUSE_REQUIREMENTS_ARE_NOT_MET);
			reloadAuction(player, false);
			return;
		}
		
		final int category = am.getCategoryByItem(player.getInventory().getItemByObjectId(_itemOID));
		player.getInventory().destroyItemByItemId("CreateAuction", 57, destroyPrice, null, null);
		player.getInventory().transferItem("CreateAuction", _itemOID, _count, player.getAuctionInventory(), player, null);
		final long finishTime = (System.currentTimeMillis() + timeToAdd) / 1000;
		
		int auctionID = IdFactory.getInstance().getNextId();
		if (player.getAuctionInventory().getItemByObjectId(_itemOID) == null)
		{
			am.createAuction(auctionID, player.getObjectId(), _itemOID, player.getAuctionInventory().getItemByItemId(itemID), _itemName, _price, _count, _duration, finishTime, category);
		}
		else
		{
			am.createAuction(auctionID, player.getObjectId(), _itemOID, player.getAuctionInventory().getItemByObjectId(_itemOID), _itemName, _price, _count, _duration, finishTime, category);
		}
		am.insertAuction(am.getAuctionById(auctionID));
		player.sendPacket(SystemMessageId.THE_ITEM_HAS_BEEN_SUCCESSFULLY_REGISTERED);
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(player.getInventory().getItemByItemId(57));
		iu.addModifiedItem(player.getAuctionInventory().getItemByObjectId(_itemOID));
		player.sendPacket(iu);
		reloadAuction(player, true);
	}
	
	private void reloadAuction(L2PcInstance player, boolean success)
	{
		player.sendPacket(new ExResponseCommissionRegister(success));
		player.sendPacket(new ExResponseCommissionList(player));
		player.sendPacket(new ExResponseCommissionInfo(player, 0, success));
		player.sendPacket(new ExResponseCommissionItemList(player));
	}
	
	@Override
	public String getType()
	{
		return _C__D0_9D_REQUESTCOMMISSIONREGISTER;
	}
}
