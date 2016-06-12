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
package com.l2jmobius.gameserver.network.clientpackets.alchemy;

import java.util.LinkedList;
import java.util.List;

import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.enums.PrivateStoreType;
import com.l2jmobius.gameserver.enums.Race;
import com.l2jmobius.gameserver.enums.TryMixCubeResultType;
import com.l2jmobius.gameserver.enums.TryMixCubeType;
import com.l2jmobius.gameserver.model.PcCondOverride;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.holders.AlchemyResult;
import com.l2jmobius.gameserver.model.holders.ItemHolder;
import com.l2jmobius.gameserver.model.itemcontainer.Inventory;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.skills.CommonSkill;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.clientpackets.IClientIncomingPacket;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jmobius.gameserver.network.serverpackets.alchemy.ExTryMixCube;
import com.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * @author Sdw
 */
public class RequestAlchemyTryMixCube implements IClientIncomingPacket
{
	// TODO: Figure out how much stones are given
	private static final int TEMPEST_STONE_AMOUNT = 1;
	
	private final List<ItemHolder> _items = new LinkedList<>();
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		final int itemsCount = packet.readD();
		if ((itemsCount <= 0) || (itemsCount > 4))
		{
			return false;
		}
		
		for (int i = 0; i < itemsCount; i++)
		{
			_items.add(new ItemHolder(packet.readD(), packet.readQ()));
		}
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final L2PcInstance player = client.getActiveChar();
		if ((player == null) || (player.getRace() != Race.ERTHEIA))
		{
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_DURING_BATTLE);
			return;
		}
		else if (player.isInStoreMode() || (player.getPrivateStoreType() != PrivateStoreType.NONE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_WHILE_TRADING_OR_USING_A_PRIVATE_STORE_OR_SHOP);
			return;
		}
		else if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_WHILE_DEAD);
			return;
		}
		else if (player.isMovementDisabled())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_ALCHEMY_WHILE_IMMOBILE);
			return;
		}
		
		if ((player.getKnownSkill(CommonSkill.ALCHEMY_CUBE.getId()) == null) && !player.canOverrideCond(PcCondOverride.SKILL_CONDITIONS))
		{
			player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_SKILL_WRONG));
			return;
		}
		
		int position = 0;
		long itemsPrice = 0;
		
		// First loop for safety check + price calculation
		for (ItemHolder item : _items)
		{
			final L2ItemInstance itemInstance = player.getInventory().getItemByObjectId(item.getId());
			if (itemInstance == null)
			{
				return;
			}
			
			if ((itemInstance.getCount() <= 0) || (itemInstance.getCount() < item.getCount()))
			{
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
			
			final int price = itemInstance.getReferencePrice();
			if (itemInstance.getReferencePrice() == 0)
			{
				player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_COMBINED);
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
			
			if ((itemInstance.getEnchantLevel() > 0) || itemInstance.isAugmented())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_COMBINE_ITEMS_THAT_HAVE_BEEN_ENCHANTED_OR_AUGMENTED);
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
			
			itemsPrice += price * item.getCount();
			position++;
			
			if ((position == 4) && (itemInstance.getId() != Inventory.ELCYUM_CRYSTAL_ID))
			{
				player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
				return;
			}
		}
		
		// Calculate the amount of air stones the player should received based on the total price of items he mixed.
		int airStonesCount = (int) Math.floor((itemsPrice / 5_000) * (_items.size() < 3 ? 0.3f : 0.5f));
		
		// Process only if there is at least one air stone to give
		if (airStonesCount > 0)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			
			long elcyumCrystals = 0;
			
			// Second loop for items deletion if we're still in the game
			for (ItemHolder item : _items)
			{
				final L2ItemInstance itemInstance = player.getInventory().getItemByObjectId(item.getId());
				if (itemInstance == null)
				{
					return;
				}
				
				if (itemInstance.getCount() < item.getCount())
				{
					return;
				}
				
				if (itemInstance.getId() == Inventory.ELCYUM_CRYSTAL_ID)
				{
					elcyumCrystals = item.getCount();
				}
				
				player.getInventory().destroyItem("Alchemy", itemInstance, item.getCount(), player, null);
				iu.addItem(itemInstance);
			}
			
			final ExTryMixCube mixCubeResult = new ExTryMixCube(TryMixCubeType.SUCCESS_NORMAL);
			
			// Whenever there is Elcyum Crystal applied there's a chance to receive Tempest Stone
			// TODO: Figure out the chance
			if ((elcyumCrystals > 0) && (Rnd.get(100) < 50))
			{
				// Broadcast animation on success
				player.broadcastPacket(new MagicSkillUse(player, CommonSkill.ALCHEMY_CUBE_RANDOM_SUCCESS.getId(), TEMPEST_STONE_AMOUNT, 500, 1500));
				
				// Give Tempest Stone to the player
				final L2ItemInstance tempestStonesInstance = player.addItem("Alchemy", Inventory.TEMPEST_STONE_ID, TEMPEST_STONE_AMOUNT, null, true);
				iu.addItem(tempestStonesInstance);
				
				// Add the alchemy result entry to the packet
				mixCubeResult.addItem(new AlchemyResult(Inventory.TEMPEST_STONE_ID, TEMPEST_STONE_AMOUNT, TryMixCubeResultType.EXTRA));
			}
			
			// Calculate the elcyum crystals bonus
			final boolean bonusSuccess = ((100. * Rnd.nextDouble()) < (elcyumCrystals / 1000));
			if (bonusSuccess)
			{
				airStonesCount *= Math.min(elcyumCrystals, 2);
			}
			
			final L2ItemInstance airStonesInstance = player.addItem("Alchemy", Inventory.AIR_STONE_ID, airStonesCount, null, true);
			iu.addItem(airStonesInstance);
			
			// Add the Air Stones
			mixCubeResult.addItem(new AlchemyResult(Inventory.AIR_STONE_ID, airStonesCount, bonusSuccess ? TryMixCubeResultType.BONUS : TryMixCubeResultType.NORMAL));
			
			// send packets
			player.sendPacket(mixCubeResult);
			player.sendInventoryUpdate(iu);
		}
		else
		{
			player.sendPacket(new ExTryMixCube(TryMixCubeType.FAIL_ITEM_WRONG));
		}
	}
}
