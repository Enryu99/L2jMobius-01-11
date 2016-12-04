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

import com.l2jmobius.Config;
import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.ai.CtrlEvent;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.ai.NextAction;
import com.l2jmobius.gameserver.enums.ItemSkillType;
import com.l2jmobius.gameserver.enums.PrivateStoreType;
import com.l2jmobius.gameserver.handler.IItemHandler;
import com.l2jmobius.gameserver.handler.ItemHandler;
import com.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.effects.L2EffectType;
import com.l2jmobius.gameserver.model.holders.ItemSkillHolder;
import com.l2jmobius.gameserver.model.items.L2EtcItem;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.ExUseSharedGroupItem;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public final class UseItem implements IClientIncomingPacket
{
	private int _objectId;
	private boolean _ctrlPressed;
	private int _itemId;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_objectId = packet.readD();
		_ctrlPressed = packet.readD() != 0;
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		// Flood protect UseItem
		if (!client.getFloodProtectors().getUseItem().tryPerformAction("use item"))
		{
			return;
		}
		
		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.cancelActiveTrade();
		}
		
		if (activeChar.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			activeChar.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			// gm can use other player item
			if (activeChar.isGM())
			{
				final L2Object obj = L2World.getInstance().findObject(_objectId);
				if (obj instanceof L2ItemInstance)
				{
					activeChar.useAdminCommand("admin_use_item " + _objectId);
				}
			}
			return;
		}
		
		if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_USE_QUEST_ITEMS);
			return;
		}
		
		// No UseItem is allowed while the player is in special conditions
		if (activeChar.hasBlockActions() || activeChar.isControlBlocked() || activeChar.isAlikeDead())
		{
			return;
		}
		
		// Char cannot use item when dead
		if (activeChar.isDead() || !activeChar.getInventory().canManipulateWithItemId(item.getId()))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addItemName(item);
			activeChar.sendPacket(sm);
			return;
		}
		
		if (!item.isEquipped() && !item.getItem().checkCondition(activeChar, activeChar, true))
		{
			return;
		}
		
		_itemId = item.getId();
		if (activeChar.isFishing() && ((_itemId < 6535) || (_itemId > 6540)))
		{
			// You cannot do anything else while fishing
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING3);
			return;
		}
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (activeChar.getReputation() < 0))
		{
			final List<ItemSkillHolder> skills = item.getItem().getSkills(ItemSkillType.NORMAL);
			if ((skills != null) && skills.stream().anyMatch(holder -> holder.getSkill().hasEffectType(L2EffectType.TELEPORT)))
			{
				return;
			}
		}
		
		// If the item has reuse time and it has not passed.
		// Message from reuse delay must come from item.
		final int reuseDelay = item.getReuseDelay();
		final int sharedReuseGroup = item.getSharedReuseGroup();
		if (reuseDelay > 0)
		{
			final long reuse = activeChar.getItemRemainingReuseTime(item.getObjectId());
			if (reuse > 0)
			{
				reuseData(activeChar, item, reuse);
				sendSharedGroupUpdate(activeChar, sharedReuseGroup, reuse, reuseDelay);
				return;
			}
			
			final long reuseOnGroup = activeChar.getReuseDelayOnGroup(sharedReuseGroup);
			if (reuseOnGroup > 0)
			{
				reuseData(activeChar, item, reuseOnGroup);
				sendSharedGroupUpdate(activeChar, sharedReuseGroup, reuseOnGroup, reuseDelay);
				return;
			}
		}
		
		if (item.isEquipable())
		{
			// Don't allow to put formal wear while a cursed weapon is equipped.
			if (activeChar.isCursedWeaponEquipped() && (_itemId == 6408))
			{
				return;
			}
			
			// Equip or unEquip
			if (FortSiegeManager.getInstance().isCombat(_itemId))
			{
				return; // no message
			}
			
			if (activeChar.isCombatFlagEquipped())
			{
				return;
			}
			
			if (activeChar.getInventory().isItemSlotBlocked(item.getItem().getBodyPart()))
			{
				activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			
			switch (item.getItem().getBodyPart())
			{
				case L2Item.SLOT_LR_HAND:
				case L2Item.SLOT_L_HAND:
				case L2Item.SLOT_R_HAND:
				{
					// Prevent players to equip weapon while wearing combat flag
					if ((activeChar.getActiveWeaponItem() != null) && (activeChar.getActiveWeaponItem().getId() == 9819))
					{
						activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					
					if (activeChar.isMounted() || activeChar.isDisarmed())
					{
						activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					
					// Don't allow weapon/shield equipment if a cursed weapon is equipped.
					if (activeChar.isCursedWeaponEquipped())
					{
						return;
					}
					break;
				}
				case L2Item.SLOT_DECO:
				{
					if (!item.isEquipped() && (activeChar.getInventory().getTalismanSlots() == 0))
					{
						activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					break;
				}
				case L2Item.SLOT_BROOCH_JEWEL:
				{
					if (!item.isEquipped() && (activeChar.getInventory().getBroochJewelSlots() == 0))
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_EQUIP_S1_WITHOUT_EQUIPPING_A_BROOCH);
						sm.addItemName(item);
						activeChar.sendPacket(sm);
						return;
					}
					break;
				}
			}
			
			if (activeChar.isCastingNow())
			{
				// Create and Bind the next action to the AI
				activeChar.getAI().setNextAction(new NextAction(CtrlEvent.EVT_FINISH_CASTING, CtrlIntention.AI_INTENTION_CAST, () -> activeChar.useEquippableItem(item, true)));
			}
			else if (activeChar.isAttackingNow())
			{
				ThreadPoolManager.getInstance().scheduleGeneral(() ->
				{
					// If character is still engaged in strike we should not change weapon
					if (activeChar.isAttackingNow())
					{
						return;
					}
					
					// Equip or unEquip
					activeChar.useEquippableItem(item, false);
				}, activeChar.getAttackEndTime() - System.currentTimeMillis());
			}
			else
			{
				activeChar.useEquippableItem(item, true);
			}
		}
		else
		{
			final L2EtcItem etcItem = item.getEtcItem();
			final IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
			if (handler == null)
			{
				if ((etcItem != null) && (etcItem.getHandlerName() != null))
				{
					_log.warning("Unmanaged Item handler: " + etcItem.getHandlerName() + " for Item Id: " + _itemId + "!");
				}
				else if (Config.DEBUG)
				{
					_log.warning("No Item handler registered for Item Id: " + _itemId + "!");
				}
			}
			else if (handler.useItem(activeChar, item, _ctrlPressed))
			{
				// Item reuse time should be added if the item is successfully used.
				// Skill reuse delay is done at handlers.itemhandlers.ItemSkillsTemplate;
				if (reuseDelay > 0)
				{
					activeChar.addTimeStampItem(item, reuseDelay);
					sendSharedGroupUpdate(activeChar, sharedReuseGroup, reuseDelay, reuseDelay);
				}
			}
		}
	}
	
	private void reuseData(L2PcInstance activeChar, L2ItemInstance item, long remainingTime)
	{
		final int hours = (int) (remainingTime / 3600000L);
		final int minutes = (int) (remainingTime % 3600000L) / 60000;
		final int seconds = (int) ((remainingTime / 1000) % 60);
		final SystemMessage sm;
		if (hours > 0)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THERE_ARE_S2_HOUR_S_S3_MINUTE_S_AND_S4_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
			sm.addItemName(item);
			sm.addInt(hours);
			sm.addInt(minutes);
		}
		else if (minutes > 0)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THERE_ARE_S2_MINUTE_S_S3_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
			sm.addItemName(item);
			sm.addInt(minutes);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THERE_ARE_S2_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
			sm.addItemName(item);
		}
		sm.addInt(seconds);
		activeChar.sendPacket(sm);
	}
	
	private void sendSharedGroupUpdate(L2PcInstance activeChar, int group, long remaining, int reuse)
	{
		if (group > 0)
		{
			activeChar.sendPacket(new ExUseSharedGroupItem(_itemId, group, remaining, reuse));
		}
	}
}
