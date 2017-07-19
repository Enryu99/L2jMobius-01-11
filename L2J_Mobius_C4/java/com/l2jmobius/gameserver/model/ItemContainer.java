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
package com.l2jmobius.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.model.L2ItemInstance.ItemLocation;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.templates.L2Item;

import javolution.util.FastList;

/**
 * @author Advi
 */
public abstract class ItemContainer
{
	protected static final Logger _log = Logger.getLogger(ItemContainer.class.getName());
	
	protected final List<L2ItemInstance> _items;
	
	protected ItemContainer()
	{
		_items = new FastList<>();
	}
	
	protected abstract L2Character getOwner();
	
	protected abstract ItemLocation getBaseLocation();
	
	/**
	 * Returns the ownerID of the inventory
	 * @return int
	 */
	public int getOwnerId()
	{
		return getOwner() == null ? 0 : getOwner().getObjectId();
	}
	
	/**
	 * Returns the quantity of items in the inventory
	 * @return int
	 */
	public int getSize()
	{
		return _items.size();
	}
	
	/**
	 * Returns the list of items in inventory
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getItems()
	{
		return _items.toArray(new L2ItemInstance[_items.size()]);
	}
	
	public List<L2ItemInstance> getItemList()
	{
		return _items;
	}
	
	/**
	 * Returns the item from inventory by using its <B>itemId</B><BR>
	 * <BR>
	 * @param itemId : int designating the ID of the item
	 * @return L2ItemInstance designating the item or null if not found in inventory
	 */
	public L2ItemInstance getItemByItemId(int itemId)
	{
		for (final L2ItemInstance item : _items)
		{
			if ((item != null) && (item.getItemId() == itemId))
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the item from inventory by using its <B>itemId</B><BR>
	 * <BR>
	 * @param itemId : int designating the ID of the item
	 * @param itemToIgnore : used during a loop, to avoid returning the same item
	 * @return L2ItemInstance designating the item or null if not found in inventory
	 */
	public L2ItemInstance getItemByItemId(int itemId, L2ItemInstance itemToIgnore)
	{
		for (final L2ItemInstance item : _items)
		{
			if ((item != null) && (item.getItemId() == itemId) && !item.equals(itemToIgnore))
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns item from inventory by using its <B>objectId</B>
	 * @param objectId : int designating the ID of the object
	 * @return L2ItemInstance designating the item or null if not found in inventory
	 */
	public L2ItemInstance getItemByObjectId(int objectId)
	{
		for (final L2ItemInstance item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			if (item.getObjectId() == objectId)
			{
				return item;
			}
		}
		
		return null;
	}
	
	public int getInventoryItemCount(int itemId, int enchantLevel)
	{
		int count = 0;
		
		for (final L2ItemInstance item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			if ((item.getItemId() == itemId) && ((item.getEnchantLevel() == enchantLevel) || (enchantLevel < 0)))
			
			{
				if (item.isStackable())
				{
					count = item.getCount();
				}
				else
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	/**
	 * Adds item to inventory
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		final L2ItemInstance olditem = getItemByItemId(item.getItemId());
		
		// If stackable item is found in inventory just add to current quantity
		if ((olditem != null) && olditem.isStackable())
		{
			olditem.changeCount(process, item.getCount(), actor, reference);
			olditem.setLastChange(L2ItemInstance.MODIFIED);
			
			// And destroys the item
			ItemTable.getInstance().destroyItem(process, item, actor, reference);
			item.updateDatabase();
			item = olditem;
			
			// Updates database
			if ((item.getItemId() == 57) && (item.getCount() < (10000 * Config.RATE_DROP_ADENA)))
			{
				// Small adena changes won't be saved to database all the time
				if ((GameTimeController.getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		// If item hasn't be found in inventory, create new one
		else
		{
			item.setOwnerId(process, getOwnerId(), actor, reference);
			item.setLocation(getBaseLocation());
			item.setLastChange((L2ItemInstance.ADDED));
			
			// Add item in inventory
			addItem(item);
			
			// Updates database
			item.updateDatabase();
		}
		
		refreshWeight();
		return item;
	}
	
	/**
	 * Adds item to inventory
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance addItem(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = getItemByItemId(itemId);
		
		// If stackable item is found in inventory just add to current quantity
		if ((item != null) && item.isStackable())
		{
			item.changeCount(process, count, actor, reference);
			item.setLastChange(L2ItemInstance.MODIFIED);
			// Updates database
			if ((itemId == 57) && (count < (10000 * Config.RATE_DROP_ADENA)))
			{
				// Small adena changes won't be saved to database all the time
				if ((GameTimeController.getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		// If item hasn't be found in inventory, create new one
		else
		{
			for (int i = 0; i < count; i++)
			{
				final L2Item template = ItemTable.getInstance().getTemplate(itemId);
				if (template == null)
				{
					_log.log(Level.WARNING, (actor != null ? "[" + actor.getName() + "] " : "") + "Invalid ItemId requested: ", itemId);
					return null;
				}
				
				item = ItemTable.getInstance().createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
				item.setOwnerId(getOwnerId());
				item.setLocation(getBaseLocation());
				item.setLastChange(L2ItemInstance.ADDED);
				
				// Add item in inventory
				addItem(item);
				// Updates database
				item.updateDatabase();
				
				// If stackable, end loop as entire count is included in 1 instance of item
				if (template.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
		}
		
		refreshWeight();
		return item;
	}
	
	/**
	 * Adds Wear/Try On item to inventory<BR>
	 * <BR>
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new weared item
	 */
	public L2ItemInstance addWearItem(String process, int itemId, L2PcInstance actor, L2Object reference)
	{
		// Surch the item in the inventory of the player
		L2ItemInstance item = getItemByItemId(itemId);
		
		// There is such item already in inventory
		if (item != null)
		{
			return item;
		}
		
		// Create and Init the L2ItemInstance corresponding to the Item Identifier and quantity
		// Add the L2ItemInstance object to _allObjects of L2world
		item = ItemTable.getInstance().createItem(process, itemId, 1, actor, reference);
		
		// Set Item Properties
		item.setWear(true); // "Try On" Item -> Don't save it in database
		item.setOwnerId(getOwnerId());
		item.setLocation(getBaseLocation());
		item.setLastChange((L2ItemInstance.ADDED));
		
		// Add item in inventory and equip it if necessary (item location defined)
		addItem(item);
		
		// Calculate the weight loaded by player
		refreshWeight();
		
		return item;
	}
	
	/**
	 * Transfers item to another inventory
	 * @param process : String Identifier of process triggering this action
	 * @param objectId
	 * @param count : int Quantity of items to be transfered
	 * @param target
	 * @param actor : L2PcInstance Player requesting the item transfer
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, int count, ItemContainer target, L2PcInstance actor, L2Object reference)
	{
		if (target == null)
		{
			return null;
		}
		
		final L2ItemInstance sourceitem = getItemByObjectId(objectId);
		if (sourceitem == null)
		{
			return null;
		}
		L2ItemInstance targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getItemId()) : null;
		
		synchronized (sourceitem)
		{
			if (getItemByObjectId(objectId) != sourceitem)
			{
				return null;
			}
			
			// Check if requested quantity is available
			if (count > sourceitem.getCount())
			{
				count = sourceitem.getCount();
			}
			
			// If possible, move entire item object
			if ((sourceitem.getCount() == count) && (targetitem == null))
			{
				removeItem(sourceitem);
				target.addItem(process, sourceitem, actor, reference);
				targetitem = sourceitem;
			}
			else
			{
				if (sourceitem.getCount() > count)
				{
					sourceitem.changeCount(process, -count, actor, reference);
				}
				else // Otherwise destroy old item
				{
					removeItem(sourceitem);
					ItemTable.getInstance().destroyItem(process, sourceitem, actor, reference);
				}
				
				if (targetitem != null)
				{
					targetitem.changeCount(process, count, actor, reference);
				}
				else
				{
					targetitem = target.addItem(process, sourceitem.getItemId(), count, actor, reference);
				}
			}
			
			// Updates database
			sourceitem.updateDatabase(true);
			if ((targetitem != sourceitem) && (targetitem != null))
			{
				targetitem.updateDatabase();
			}
			refreshWeight();
		}
		return targetitem;
	}
	
	/**
	 * Destroy item from inventory and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param count
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, int count, L2PcInstance actor, L2Object reference)
	{
		
		synchronized (item)
		{
			
			// Adjust item quantity
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);
				
				// dont update often for untraced items
				if ((process != null) || ((GameTimeController.getGameTicks() % 10) == 0))
				{
					item.updateDatabase();
				}
				
				refreshWeight();
				
				return item;
			}
			if (item.getCount() < count)
			{
				return null;
			}
			
			final boolean removed = removeItem(item);
			if (!removed)
			{
				return null;
			}
			
			ItemTable.getInstance().destroyItem(process, item, actor, reference);
			
			item.updateDatabase();
			refreshWeight();
		}
		
		return item;
	}
	
	/**
	 * Destroy item from inventory by using its <B>objectID</B> and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance destroyItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference)
	{
		final L2ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
		{
			return null;
		}
		
		return destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance destroyItemByItemId(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		final L2ItemInstance item = getItemByItemId(itemId);
		if (item == null)
		{
			return null;
		}
		
		return destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy all items from inventory and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public synchronized void destroyAllItems(String process, L2PcInstance actor, L2Object reference)
	{
		for (final L2ItemInstance item : _items)
		{
			destroyItem(process, item, actor, reference);
		}
	}
	
	/**
	 * Get warehouse adena
	 * @return
	 */
	public int getAdena()
	{
		int count = 0;
		
		for (final L2ItemInstance item : _items)
		{
			if (item.getItemId() == 57)
			{
				count = item.getCount();
				return count;
			}
		}
		
		return count;
	}
	
	/**
	 * Adds item to inventory for further adjustments.
	 * @param item : L2ItemInstance to be added from inventory
	 */
	protected void addItem(L2ItemInstance item)
	{
		_items.add(item);
	}
	
	/**
	 * Removes item from inventory for further adjustments.
	 * @param item : L2ItemInstance to be removed from inventory
	 * @return
	 */
	protected boolean removeItem(L2ItemInstance item)
	{
		return _items.remove(item);
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	protected void refreshWeight()
	{
	}
	
	/**
	 * Delete item object from world
	 */
	public void deleteMe()
	{
		try
		{
			updateDatabase();
		}
		catch (final Throwable t)
		{
			_log.log(Level.SEVERE, "deletedMe()", t);
		}
		final List<L2Object> items = new FastList<>(_items);
		_items.clear();
		
		L2World.getInstance().removeObjects(items);
	}
	
	/**
	 * Update database with items in inventory
	 */
	public void updateDatabase()
	{
		if (getOwner() != null)
		{
			for (final L2ItemInstance item : _items)
			{
				if (item != null)
				{
					item.updateDatabase(true);
				}
			}
		}
	}
	
	/**
	 * Get back items in container from database
	 */
	public void restore()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id FROM items WHERE owner_id=? AND (loc=?) ORDER BY object_id DESC"))
		{
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
			try (ResultSet inv = statement.executeQuery())
			{
				L2ItemInstance item;
				while (inv.next())
				{
					final int objectId = inv.getInt(1);
					item = L2ItemInstance.restoreFromDb(objectId);
					if (item == null)
					{
						continue;
					}
					
					L2World.getInstance().storeObject(item);
					
					// If stackable item is found in inventory just add to current quantity
					if (item.isStackable() && (getItemByItemId(item.getItemId()) != null))
					{
						addItem("Restore", item, null, getOwner());
					}
					else
					{
						addItem(item);
					}
				}
			}
			refreshWeight();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not restore container:", e);
		}
	}
	
	public boolean validateCapacity(int slots)
	{
		return true;
	}
	
	public boolean validateWeight(int weight)
	{
		return true;
	}
}