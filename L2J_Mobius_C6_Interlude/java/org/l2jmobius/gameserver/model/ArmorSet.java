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
package org.l2jmobius.gameserver.model;

import org.l2jmobius.gameserver.model.actor.instance.ItemInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author Luno
 */
public final class ArmorSet
{
	private final int _chest;
	private final int _legs;
	private final int _head;
	private final int _gloves;
	private final int _feet;
	private final int _skillId;
	
	private final int _shield;
	private final int _shieldSkillId;
	
	private final int _enchant6Skill;
	
	public ArmorSet(int chest, int legs, int head, int gloves, int feet, int skill_id, int shield, int shield_skill_id, int enchant6skill)
	{
		_chest = chest;
		_legs = legs;
		_head = head;
		_gloves = gloves;
		_feet = feet;
		_skillId = skill_id;
		
		_shield = shield;
		_shieldSkillId = shield_skill_id;
		
		_enchant6Skill = enchant6skill;
	}
	
	/**
	 * Checks if player have equiped all items from set (not checking shield)
	 * @param player whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(PlayerInstance player)
	{
		final Inventory inv = player.getInventory();
		
		ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		if (legsItem != null)
		{
			legs = legsItem.getItemId();
		}
		if (headItem != null)
		{
			head = headItem.getItemId();
		}
		if (glovesItem != null)
		{
			gloves = glovesItem.getItemId();
		}
		if (feetItem != null)
		{
			feet = feetItem.getItemId();
		}
		
		return containAll(_chest, legs, head, gloves, feet);
	}
	
	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if ((_chest != 0) && (_chest != chest))
		{
			return false;
		}
		if ((_legs != 0) && (_legs != legs))
		{
			return false;
		}
		if ((_head != 0) && (_head != head))
		{
			return false;
		}
		if ((_gloves != 0) && (_gloves != gloves))
		{
			return false;
		}
		if ((_feet != 0) && (_feet != feet))
		{
			return false;
		}
		
		return true;
	}
	
	public boolean containItem(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
			{
				return _chest == itemId;
			}
			case Inventory.PAPERDOLL_LEGS:
			{
				return _legs == itemId;
			}
			case Inventory.PAPERDOLL_HEAD:
			{
				return _head == itemId;
			}
			case Inventory.PAPERDOLL_GLOVES:
			{
				return _gloves == itemId;
			}
			case Inventory.PAPERDOLL_FEET:
			{
				return _feet == itemId;
			}
			default:
			{
				return false;
			}
		}
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public boolean containShield(PlayerInstance player)
	{
		Inventory inv = player.getInventory();
		
		ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		
		if ((shieldItem != null) && (shieldItem.getItemId() == _shield))
		{
			return true;
		}
		
		return false;
	}
	
	public boolean containShield(int shield_id)
	{
		if (_shield == 0)
		{
			return false;
		}
		
		return _shield == shield_id;
	}
	
	public int getShieldSkillId()
	{
		return _shieldSkillId;
	}
	
	public int getEnchant6skillId()
	{
		return _enchant6Skill;
	}
	
	/**
	 * Checks if all parts of set are enchanted to +6 or more
	 * @param player
	 * @return
	 */
	public boolean isEnchanted6(PlayerInstance player)
	{
		// Player don't have full set
		if (!containAll(player))
		{
			return false;
		}
		
		final Inventory inv = player.getInventory();
		
		ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if (chestItem.getEnchantLevel() < 6)
		{
			return false;
		}
		if ((_legs != 0) && (legsItem.getEnchantLevel() < 6))
		{
			return false;
		}
		if ((_gloves != 0) && (glovesItem.getEnchantLevel() < 6))
		{
			return false;
		}
		if ((_head != 0) && (headItem.getEnchantLevel() < 6))
		{
			return false;
		}
		if ((_feet != 0) && (feetItem.getEnchantLevel() < 6))
		{
			return false;
		}
		
		return true;
	}
}
