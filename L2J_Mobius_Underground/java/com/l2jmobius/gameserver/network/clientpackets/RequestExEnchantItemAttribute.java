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
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.enums.AttributeType;
import com.l2jmobius.gameserver.enums.PrivateStoreType;
import com.l2jmobius.gameserver.model.Elementals;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.request.EnchantItemAttributeRequest;
import com.l2jmobius.gameserver.model.items.enchant.attribute.AttributeHolder;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ExAttributeEnchantResult;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;
import com.l2jmobius.gameserver.util.Util;

public class RequestExEnchantItemAttribute implements IClientIncomingPacket
{
	private int _objectId;
	private long _count;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
		_count = packet.readQ();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final EnchantItemAttributeRequest request = player.getRequest(EnchantItemAttributeRequest.class);
		if (request == null)
		{
			return;
		}
		
		request.setProcessing(true);
		
		if (_objectId == 0xFFFFFFFF)
		{
			// Player canceled enchant
			player.removeRequest(request.getClass());
			client.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!player.isOnline())
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			client.sendPacket(SystemMessageId.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Restrict enchant during a trade (bug if enchant fails)
		if (player.getActiveRequester() != null)
		{
			// Cancel trade
			player.cancelActiveTrade();
			player.removeRequest(request.getClass());
			client.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_TRADING);
			return;
		}
		
		final L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		final L2ItemInstance stone = request.getEnchantingStone();
		if ((item == null) || (stone == null))
		{
			player.removeRequest(request.getClass());
			client.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!item.isElementable())
		{
			client.sendPacket(SystemMessageId.ELEMENTAL_POWER_ENHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
			player.removeRequest(request.getClass());
			return;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				if (item.getOwnerId() != player.getObjectId())
				{
					player.removeRequest(request.getClass());
					return;
				}
				break;
			}
			default:
			{
				player.removeRequest(request.getClass());
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
				return;
			}
		}
		
		final int stoneId = stone.getId();
		final long count = Math.min(stone.getCount(), _count);
		AttributeType elementToAdd = AttributeType.findByClientId(Elementals.getItemElement(stoneId));
		// Armors have the opposite element
		if (item.isArmor())
		{
			elementToAdd = elementToAdd.getOpposite();
		}
		final AttributeType opositeElement = elementToAdd.getOpposite();
		
		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final int limit = getLimit(item, stoneId);
		int powerToAdd = getPowerToAdd(stoneId, elementValue, item);
		
		if ((item.isWeapon() && (oldElement != null) && (oldElement.getType() != elementToAdd) && (oldElement.getType() != AttributeType.NONE)) || (item.isArmor() && (item.getAttribute(elementToAdd) == null) && (item.getAttributes() != null) && (item.getAttributes().size() >= 3)))
		{
			client.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_HAS_ALREADY_BEEN_ADDED_THIS_ELEMENTAL_POWER_CANNOT_BE_ADDED);
			player.removeRequest(request.getClass());
			return;
		}
		
		if (item.isArmor() && (item.getAttributes() != null))
		{
			// can't add opposite element
			for (AttributeHolder attribute : item.getAttributes())
			{
				if (attribute.getType() == opositeElement)
				{
					player.removeRequest(request.getClass());
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to add oposite attribute to item!", Config.DEFAULT_PUNISH);
					return;
				}
			}
		}
		
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			client.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(request.getClass());
			return;
		}
		
		int usedStones = 0;
		int successfulAttempts = 0;
		int failedAttempts = 0;
		for (int i = 0; i < count; i++)
		{
			usedStones++;
			final int result = addElement(player, stone, item, elementToAdd);
			if (result == 1)
			{
				successfulAttempts++;
			}
			else if (result == 0)
			{
				failedAttempts++;
			}
			else
			{
				break;
			}
		}
		
		player.destroyItem("AttrEnchant", stone, usedStones, player, true);
		final AttributeHolder newElement = item.getAttribute(elementToAdd);
		final int newValue = newElement != null ? newElement.getValue() : 0;
		final AttributeType realElement = item.isArmor() ? opositeElement : elementToAdd;
		final InventoryUpdate iu = new InventoryUpdate();
		
		if (successfulAttempts > 0)
		{
			SystemMessage sm;
			if (item.getEnchantLevel() == 0)
			{
				if (item.isArmor())
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S2_S_ATTRIBUTE_WAS_SUCCESSFULLY_BESTOWED_ON_S1_AND_RESISTANCE_TO_S3_WAS_INCREASED);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO_S1);
				}
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (item.isArmor())
				{
					sm.addAttribute(realElement.getOpposite().getClientId());
				}
			}
			else
			{
				if (item.isArmor())
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S3_S_ATTRIBUTE_WAS_SUCCESSFULLY_BESTOWED_ON_S1_S2_AND_RESISTANCE_TO_S4_WAS_INCREASED);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S3_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO_S1_S2);
				}
				sm.addInt(item.getEnchantLevel());
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (item.isArmor())
				{
					sm.addAttribute(realElement.getOpposite().getClientId());
				}
			}
			player.sendPacket(sm);
			
			// send packets
			iu.addModifiedItem(item);
		}
		else
		{
			client.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_ELEMENTAL_POWER);
		}
		
		int result = 0;
		if (successfulAttempts == 0)
		{
			// Failed
			result = 2;
		}
		
		// Stone must be removed
		if (stone.getCount() == 0)
		{
			iu.addRemovedItem(stone);
		}
		else
		{
			iu.addModifiedItem(stone);
		}
		
		player.removeRequest(request.getClass());
		client.sendPacket(new ExAttributeEnchantResult(result, item.isWeapon(), elementToAdd, elementValue, newValue, successfulAttempts, failedAttempts));
		client.sendPacket(new UserInfo(player));
		player.sendInventoryUpdate(iu);
	}
	
	private int addElement(L2PcInstance player, L2ItemInstance stone, L2ItemInstance item, AttributeType elementToAdd)
	{
		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final int limit = getLimit(item, stone.getId());
		int powerToAdd = getPowerToAdd(stone.getId(), elementValue, item);
		
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(EnchantItemAttributeRequest.class);
			return -1;
		}
		
		boolean success = false;
		switch (stone.getItem().getCrystalType())
		{
			case R:
			{
				success = Rnd.get(100) < 80;
				break;
			}
			case R95:
			case R99:
			{
				success = true;
				break;
			}
			default:
			{
				switch (Elementals.getItemElemental(stone.getId())._type)
				{
					case Stone:
					case Roughore:
						success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_STONE;
						break;
					case Crystal:
						success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_CRYSTAL;
						break;
					case Jewel:
						success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_JEWEL;
						break;
					case Energy:
						success = Rnd.get(100) < Config.ENCHANT_CHANCE_ELEMENT_ENERGY;
						break;
				}
			}
		}
		
		if (success)
		{
			item.setAttribute(new AttributeHolder(elementToAdd, newPower));
		}
		
		return success ? 1 : 0;
	}
	
	public int getLimit(L2ItemInstance item, int sotneId)
	{
		final Elementals.ElementalItems elementItem = Elementals.getItemElemental(sotneId);
		if (elementItem == null)
		{
			return 0;
		}
		
		if (item.isWeapon())
		{
			return Elementals.WEAPON_VALUES[elementItem._type._maxLevel];
		}
		return Elementals.ARMOR_VALUES[elementItem._type._maxLevel];
	}
	
	public int getPowerToAdd(int stoneId, int oldValue, L2ItemInstance item)
	{
		if (Elementals.getItemElement(stoneId) != -1)
		{
			if (item.isWeapon())
			{
				if (oldValue == 0)
				{
					return Elementals.FIRST_WEAPON_BONUS;
				}
				return Elementals.NEXT_WEAPON_BONUS;
			}
			else if (item.isArmor())
			{
				return Elementals.ARMOR_BONUS;
			}
		}
		return 0;
	}
}
