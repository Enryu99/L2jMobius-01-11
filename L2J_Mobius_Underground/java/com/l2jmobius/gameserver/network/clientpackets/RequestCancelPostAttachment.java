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
import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.enums.ItemLocation;
import com.l2jmobius.gameserver.enums.PrivateStoreType;
import com.l2jmobius.gameserver.instancemanager.MailManager;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.entity.Message;
import com.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ExChangePostState;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.util.Util;

/**
 * @author Migi, DS
 */
public final class RequestCancelPostAttachment implements IClientIncomingPacket
{
	private int _msgId;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_msgId = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance activeChar = client.getActiveChar();
		if ((activeChar == null) || !Config.ALLOW_MAIL || !Config.ALLOW_ATTACHMENTS)
		{
			return;
		}
		
		if (!client.getFloodProtectors().getTransaction().tryPerformAction("cancelpost"))
		{
			return;
		}
		
		final Message msg = MailManager.getInstance().getMessage(_msgId);
		if (msg == null)
		{
			return;
		}
		if (msg.getSenderId() != activeChar.getObjectId())
		{
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to cancel not own post!", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!activeChar.isInsideZone(ZoneId.PEACE))
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_IN_A_NON_PEACE_ZONE_LOCATION);
			return;
		}
		
		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_DURING_AN_EXCHANGE);
			return;
		}
		
		if (activeChar.hasItemRequest())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_DURING_AN_ITEM_ENHANCEMENT_OR_ATTRIBUTE_ENHANCEMENT);
			return;
		}
		
		if (activeChar.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_BECAUSE_THE_PRIVATE_STORE_OR_WORKSHOP_IS_IN_PROGRESS);
			return;
		}
		
		if (!msg.hasAttachments())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_SENT_MAIL_SINCE_THE_RECIPIENT_RECEIVED_IT);
			return;
		}
		
		final ItemContainer attachments = msg.getAttachments();
		if ((attachments == null) || (attachments.getSize() == 0))
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_CANCEL_SENT_MAIL_SINCE_THE_RECIPIENT_RECEIVED_IT);
			return;
		}
		
		int weight = 0;
		int slots = 0;
		
		for (L2ItemInstance item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (item.getOwnerId() != activeChar.getObjectId())
			{
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to get not own item from cancelled attachment!", Config.DEFAULT_PUNISH);
				return;
			}
			
			if (item.getItemLocation() != ItemLocation.MAIL)
			{
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to get items not from mail !", Config.DEFAULT_PUNISH);
				return;
			}
			
			if (item.getLocationSlot() != msg.getId())
			{
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to get items from different attachment!", Config.DEFAULT_PUNISH);
				return;
			}
			
			weight += item.getCount() * item.getItem().getWeight();
			if (!item.isStackable())
			{
				slots += item.getCount();
			}
			else if (activeChar.getInventory().getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		if (!activeChar.getInventory().validateCapacity(slots))
		{
			activeChar.sendPacket(SystemMessageId.YOU_COULD_NOT_CANCEL_RECEIPT_BECAUSE_YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		if (!activeChar.getInventory().validateWeight(weight))
		{
			activeChar.sendPacket(SystemMessageId.YOU_COULD_NOT_CANCEL_RECEIPT_BECAUSE_YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		// Proceed to the transfer
		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (L2ItemInstance item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			final long count = item.getCount();
			final L2ItemInstance newItem = attachments.transferItem(attachments.getName(), item.getObjectId(), count, activeChar.getInventory(), activeChar, null);
			if (newItem == null)
			{
				return;
			}
			
			if (playerIU != null)
			{
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
			}
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S2_S1);
			sm.addItemName(item.getId());
			sm.addLong(count);
			activeChar.sendPacket(sm);
		}
		
		msg.removeAttachments();
		
		// Send updated item list to the player
		if (playerIU != null)
		{
			activeChar.sendInventoryUpdate(playerIU);
		}
		else
		{
			activeChar.sendItemList(false);
		}
		
		final L2PcInstance receiver = L2World.getInstance().getPlayer(msg.getReceiverId());
		if (receiver != null)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANCELED_THE_SENT_MAIL);
			sm.addCharName(activeChar);
			receiver.sendPacket(sm);
			receiver.sendPacket(new ExChangePostState(true, _msgId, Message.DELETED));
		}
		
		MailManager.getInstance().deleteMessageInDb(_msgId);
		
		activeChar.sendPacket(new ExChangePostState(false, _msgId, Message.DELETED));
		activeChar.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_CANCELLED);
	}
}
