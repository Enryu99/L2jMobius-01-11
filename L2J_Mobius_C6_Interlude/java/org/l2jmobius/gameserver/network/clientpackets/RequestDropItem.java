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

import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.datatables.xml.AdminData;
import org.l2jmobius.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance.SkillDat;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.items.type.EtcItemType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.IllegalPlayerAction;
import org.l2jmobius.gameserver.util.Util;

public class RequestDropItem extends GameClientPacket
{
	private static Logger LOGGER = Logger.getLogger(RequestDropItem.class.getName());
	
	private int _objectId;
	private int _count;
	private int _x;
	private int _y;
	private int _z;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		if ((player == null) || player.isDead())
		{
			return;
		}
		
		if (player.isGM() && (player.getAccessLevel().getLevel() > 2))
		{ // just head gm and admin can drop items on the ground
			sendPacket(SystemMessage.sendString("You have not right to discard anything from inventory"));
			return;
		}
		
		// Fix against safe enchant exploit
		if (player.getActiveEnchantItem() != null)
		{
			sendPacket(SystemMessage.sendString("You can't discard items during enchant."));
			return;
		}
		
		// Flood protect drop to avoid packet lag
		if (!getClient().getFloodProtectors().getDropItem().tryPerformAction("drop item"))
		{
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		
		if ((item == null) || (_count == 0) || !player.validateItemManipulation(_objectId, "drop"))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		
		if ((!Config.ALLOW_DISCARDITEM && !player.isGM()) || (!item.isDropable()))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		
		if (item.isAugmented())
		{
			player.sendPacket(SystemMessageId.AUGMENTED_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		if ((item.getItemType() == EtcItemType.QUEST) && !(player.isGM()))
		{
			return;
		}
		
		// Drop item disabled by config
		if (player.isGM() && Config.GM_TRADE_RESTRICTED_ITEMS)
		{
			player.sendMessage("Drop item disabled for GM by config!");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int itemId = item.getItemId();
		
		// Cursed Weapons cannot be dropped
		if (CursedWeaponsManager.getInstance().isCursed(itemId))
		{
			return;
		}
		
		if (_count > item.getCount())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		
		if ((Config.PLAYER_SPAWN_PROTECTION > 0) && player.isInvul() && !player.isGM())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		
		if (_count <= 0)
		{
			player.setAccessLevel(-1); // ban
			Util.handleIllegalPlayerAction(player, "[RequestDropItem] count <= 0! ban! oid: " + _objectId + " owner: " + player.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		
		if (!item.isStackable() && (_count > 1))
		{
			Util.handleIllegalPlayerAction(player, "[RequestDropItem] count > 1 but item is not stackable! ban! oid: " + _objectId + " owner: " + player.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Unsufficient privileges.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isProcessingTransaction() || (player.getPrivateStoreType() != 0))
		{
			player.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}
		
		if (player.isFishing())
		{
			// You can't mount, dismount, break and drop items while fishing
			player.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
			return;
		}
		
		// Cannot discard item that the skill is consuming.
		if (player.isCastingNow())
		{
			final SkillDat skillDat = player.getCurrentSkill();
			if ((skillDat != null) && (skillDat.getSkill().getItemConsumeId() == item.getItemId()) //
				&& ((player.getInventory().getInventoryItemCount(item.getItemId(), -1) - skillDat.getSkill().getItemConsume()) < _count))
			{
				player.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}
		
		if ((Item.TYPE2_QUEST == item.getItem().getType2()) && !player.isGM())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISCARD_EXCHANGE_ITEM);
			return;
		}
		
		if (!player.isInsideRadius(_x, _y, 150, false) || (Math.abs(_z - player.getZ()) > 50))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISCARD_DISTANCE_TOO_FAR);
			return;
		}
		
		if (item.isEquipped())
		{
			// Remove augementation boni on unequip
			if (item.isAugmented())
			{
				item.getAugmentation().removeBonus(player);
			}
			
			final ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			
			for (ItemInstance element : unequiped)
			{
				player.checkSSMatch(null, element);
				
				iu.addModifiedItem(element);
			}
			player.sendPacket(iu);
			player.broadcastUserInfo();
			
			final ItemList il = new ItemList(player, true);
			player.sendPacket(il);
		}
		
		final ItemInstance dropedItem = player.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false, false);
		if ((dropedItem != null) && (dropedItem.getItemId() == 57) && (dropedItem.getCount() >= 1000000) && (Config.RATE_DROP_ADENA <= 200))
		{
			final String msg = "Character (" + player.getName() + ") has dropped (" + dropedItem.getCount() + ")adena at (" + _x + "," + _y + "," + _z + ")";
			LOGGER.warning(msg);
			AdminData.broadcastMessageToGMs(msg);
		}
	}
}
