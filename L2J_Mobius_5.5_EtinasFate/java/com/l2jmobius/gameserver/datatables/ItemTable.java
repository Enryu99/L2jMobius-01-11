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
package com.l2jmobius.gameserver.datatables;

import static com.l2jmobius.gameserver.model.itemcontainer.Inventory.ADENA_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.gameserver.data.xml.impl.EnchantItemHPBonusData;
import com.l2jmobius.gameserver.engines.DocumentEngine;
import com.l2jmobius.gameserver.enums.ItemLocation;
import com.l2jmobius.gameserver.idfactory.IdFactory;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2EventMonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.events.EventDispatcher;
import com.l2jmobius.gameserver.model.events.impl.item.OnItemCreate;
import com.l2jmobius.gameserver.model.items.L2Armor;
import com.l2jmobius.gameserver.model.items.L2EtcItem;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.items.L2Weapon;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.util.GMAudit;

/**
 * This class serves as a container for all item templates in the game.
 */
public class ItemTable
{
	private static Logger LOGGER = Logger.getLogger(ItemTable.class.getName());
	private static Logger LOGGER_ITEMS = Logger.getLogger("item");
	
	public static final Map<String, Long> SLOTS = new HashMap<>();
	
	private L2Item[] _allTemplates;
	private final Map<Integer, L2EtcItem> _etcItems = new HashMap<>();
	private final Map<Integer, L2Armor> _armors = new HashMap<>();
	private final Map<Integer, L2Weapon> _weapons = new HashMap<>();
	static
	{
		SLOTS.put("shirt", (long) L2Item.SLOT_UNDERWEAR);
		SLOTS.put("lbracelet", (long) L2Item.SLOT_L_BRACELET);
		SLOTS.put("rbracelet", (long) L2Item.SLOT_R_BRACELET);
		SLOTS.put("talisman", (long) L2Item.SLOT_DECO);
		SLOTS.put("chest", (long) L2Item.SLOT_CHEST);
		SLOTS.put("fullarmor", (long) L2Item.SLOT_FULL_ARMOR);
		SLOTS.put("head", (long) L2Item.SLOT_HEAD);
		SLOTS.put("hair", (long) L2Item.SLOT_HAIR);
		SLOTS.put("hairall", (long) L2Item.SLOT_HAIRALL);
		SLOTS.put("underwear", (long) L2Item.SLOT_UNDERWEAR);
		SLOTS.put("back", (long) L2Item.SLOT_BACK);
		SLOTS.put("neck", (long) L2Item.SLOT_NECK);
		SLOTS.put("legs", (long) L2Item.SLOT_LEGS);
		SLOTS.put("feet", (long) L2Item.SLOT_FEET);
		SLOTS.put("gloves", (long) L2Item.SLOT_GLOVES);
		SLOTS.put("chest,legs", (long) L2Item.SLOT_CHEST | L2Item.SLOT_LEGS);
		SLOTS.put("belt", (long) L2Item.SLOT_BELT);
		SLOTS.put("rhand", (long) L2Item.SLOT_R_HAND);
		SLOTS.put("lhand", (long) L2Item.SLOT_L_HAND);
		SLOTS.put("lrhand", (long) L2Item.SLOT_LR_HAND);
		SLOTS.put("rear;lear", (long) L2Item.SLOT_R_EAR | L2Item.SLOT_L_EAR);
		SLOTS.put("rfinger;lfinger", (long) L2Item.SLOT_R_FINGER | L2Item.SLOT_L_FINGER);
		SLOTS.put("wolf", (long) L2Item.SLOT_WOLF);
		SLOTS.put("greatwolf", (long) L2Item.SLOT_GREATWOLF);
		SLOTS.put("hatchling", (long) L2Item.SLOT_HATCHLING);
		SLOTS.put("strider", (long) L2Item.SLOT_STRIDER);
		SLOTS.put("babypet", (long) L2Item.SLOT_BABYPET);
		SLOTS.put("brooch", (long) L2Item.SLOT_BROOCH);
		SLOTS.put("brooch_jewel", (long) L2Item.SLOT_BROOCH_JEWEL);
		SLOTS.put("agathion", L2Item.SLOT_AGATHION);
		SLOTS.put("artifactbook", L2Item.SLOT_ARTIFACT_BOOK);
		SLOTS.put("artifact", L2Item.SLOT_ARTIFACT);
		SLOTS.put("none", (long) L2Item.SLOT_NONE);
		
		// retail compatibility
		SLOTS.put("onepiece", (long) L2Item.SLOT_FULL_ARMOR);
		SLOTS.put("hair2", (long) L2Item.SLOT_HAIR2);
		SLOTS.put("dhair", (long) L2Item.SLOT_HAIRALL);
		SLOTS.put("alldress", (long) L2Item.SLOT_ALLDRESS);
		SLOTS.put("deco1", (long) L2Item.SLOT_DECO);
		SLOTS.put("waist", (long) L2Item.SLOT_BELT);
	}
	
	/**
	 * @return a reference to this ItemTable object
	 */
	public static ItemTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected ItemTable()
	{
		load();
	}
	
	private void load()
	{
		int highest = 0;
		_armors.clear();
		_etcItems.clear();
		_weapons.clear();
		for (L2Item item : DocumentEngine.getInstance().loadItems())
		{
			if (highest < item.getId())
			{
				highest = item.getId();
			}
			if (item instanceof L2EtcItem)
			{
				_etcItems.put(item.getId(), (L2EtcItem) item);
			}
			else if (item instanceof L2Armor)
			{
				_armors.put(item.getId(), (L2Armor) item);
			}
			else
			{
				_weapons.put(item.getId(), (L2Weapon) item);
			}
		}
		buildFastLookupTable(highest);
		LOGGER.info(getClass().getSimpleName() + ": Loaded: " + _etcItems.size() + " Etc Items");
		LOGGER.info(getClass().getSimpleName() + ": Loaded: " + _armors.size() + " Armor Items");
		LOGGER.info(getClass().getSimpleName() + ": Loaded: " + _weapons.size() + " Weapon Items");
		LOGGER.info(getClass().getSimpleName() + ": Loaded: " + (_etcItems.size() + _armors.size() + _weapons.size()) + " Items in total.");
	}
	
	/**
	 * Builds a variable in which all items are putting in in function of their ID.
	 * @param size
	 */
	private void buildFastLookupTable(int size)
	{
		// Create a FastLookUp Table called _allTemplates of size : value of the highest item ID
		LOGGER.info(getClass().getSimpleName() + ": Highest item id used: " + size);
		_allTemplates = new L2Item[size + 1];
		
		// Insert armor item in Fast Look Up Table
		for (L2Armor item : _armors.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		// Insert weapon item in Fast Look Up Table
		for (L2Weapon item : _weapons.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		// Insert etcItem item in Fast Look Up Table
		for (L2EtcItem item : _etcItems.values())
		{
			_allTemplates[item.getId()] = item;
		}
	}
	
	/**
	 * Returns the item corresponding to the item ID
	 * @param id : int designating the item
	 * @return L2Item
	 */
	public L2Item getTemplate(int id)
	{
		if ((id >= _allTemplates.length) || (id < 0))
		{
			return null;
		}
		
		return _allTemplates[id];
	}
	
	/**
	 * Create the L2ItemInstance corresponding to the Item Identifier and quantitiy add logs the activity. <B><U> Actions</U> :</B>
	 * <li>Create and Init the L2ItemInstance corresponding to the Item Identifier and quantity</li>
	 * <li>Add the L2ItemInstance object to _allObjects of L2world</li>
	 * <li>Logs Item creation according to log settings</li>
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be created
	 * @param count : int Quantity of items to be created for stackable items
	 * @param actor : L2Character requesting the item creation
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item
	 */
	public L2ItemInstance createItem(String process, int itemId, long count, L2Character actor, Object reference)
	{
		// Create and Init the L2ItemInstance corresponding to the Item Identifier
		final L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		
		if (process.equalsIgnoreCase("loot") && !Config.AUTO_LOOT_ITEM_IDS.contains(itemId))
		{
			ScheduledFuture<?> itemLootShedule;
			if ((reference instanceof L2Attackable) && ((L2Attackable) reference).isRaid()) // loot privilege for raids
			{
				final L2Attackable raid = (L2Attackable) reference;
				// if in CommandChannel and was killing a World/RaidBoss
				if ((raid.getFirstCommandChannelAttacked() != null) && !Config.AUTO_LOOT_RAIDS)
				{
					item.setOwnerId(raid.getFirstCommandChannelAttacked().getLeaderObjectId());
					itemLootShedule = ThreadPool.schedule(new ResetOwner(item), Config.LOOT_RAIDS_PRIVILEGE_INTERVAL);
					item.setItemLootShedule(itemLootShedule);
				}
			}
			else if (!Config.AUTO_LOOT || ((reference instanceof L2EventMonsterInstance) && ((L2EventMonsterInstance) reference).eventDropOnGround()))
			{
				item.setOwnerId(actor.getObjectId());
				itemLootShedule = ThreadPool.schedule(new ResetOwner(item), 15000);
				item.setItemLootShedule(itemLootShedule);
			}
		}
		
		// Add the L2ItemInstance object to _allObjects of L2world
		L2World.getInstance().addObject(item);
		
		// Set Item parameters
		if (item.isStackable() && (count > 1))
		{
			item.setCount(count);
		}
		
		if (Config.LOG_ITEMS && !process.equals("Reset"))
		{
			if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == ADENA_ID))))
			{
				if (item.getEnchantLevel() > 0)
				{
					LOGGER_ITEMS.info("CREATE:" + String.valueOf(process) // in case of null
						+ ", item " + item.getObjectId() //
						+ ":+" + item.getEnchantLevel() //
						+ " " + item.getItem().getName() //
						+ "(" + item.getCount() //
						+ "), " + String.valueOf(actor) // in case of null
						+ ", " + String.valueOf(reference)); // in case of null
				}
				else
				{
					LOGGER_ITEMS.info("CREATE:" + String.valueOf(process) // in case of null
						+ ", item " + item.getObjectId() //
						+ ":" + item.getItem().getName() //
						+ "(" + item.getCount() //
						+ "), " + String.valueOf(actor) // in case of null
						+ ", " + String.valueOf(reference)); // in case of null
				}
			}
		}
		
		if ((actor != null) && actor.isGM())
		{
			String referenceName = "no-reference";
			if (reference instanceof L2Object)
			{
				referenceName = (((L2Object) reference).getName() != null ? ((L2Object) reference).getName() : "no-name");
			}
			else if (reference instanceof String)
			{
				referenceName = (String) reference;
			}
			final String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
			if (Config.GMAUDIT)
			{
				GMAudit.auditGMAction(actor.getName() + " [" + actor.getObjectId() + "]" //
					, String.valueOf(process) // in case of null
						+ "(id: " + itemId //
						+ " count: " + count //
						+ " name: " + item.getItemName() //
						+ " objId: " + item.getObjectId() + ")" //
					, targetName //
					, "L2Object referencing this action is: " + referenceName);
			}
		}
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnItemCreate(process, item, actor, reference), item.getItem());
		return item;
	}
	
	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor)
	{
		return createItem(process, itemId, count, actor, null);
	}
	
	/**
	 * Destroys the L2ItemInstance.<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Sets L2ItemInstance parameters to be unusable</li>
	 * <li>Removes the L2ItemInstance object to _allObjects of L2world</li>
	 * <li>Logs Item deletion according to log settings</li>
	 * </ul>
	 * @param process a string identifier of process triggering this action.
	 * @param item the item instance to be destroyed.
	 * @param actor the player requesting the item destroy.
	 * @param reference the object referencing current action like NPC selling item or previous item in transformation.
	 */
	public void destroyItem(String process, L2ItemInstance item, L2PcInstance actor, Object reference)
	{
		synchronized (item)
		{
			final long old = item.getCount();
			item.setCount(0);
			item.setOwnerId(0);
			item.setItemLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);
			
			L2World.getInstance().removeObject(item);
			IdFactory.getInstance().releaseId(item.getObjectId());
			
			if (Config.LOG_ITEMS)
			{
				if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == ADENA_ID))))
				{
					if (item.getEnchantLevel() > 0)
					{
						LOGGER_ITEMS.info("DELETE:" + String.valueOf(process) // in case of null
							+ ", item " + item.getObjectId() //
							+ ":+" + item.getEnchantLevel() //
							+ " " + item.getItem().getName() //
							+ "(" + item.getCount() //
							+ "), PrevCount(" + old //
							+ "), " + String.valueOf(actor) // in case of null
							+ ", " + String.valueOf(reference)); // in case of null
					}
					else
					{
						LOGGER_ITEMS.info("DELETE:" + String.valueOf(process) // in case of null
							+ ", item " + item.getObjectId() //
							+ ":" + item.getItem().getName() //
							+ "(" + item.getCount() //
							+ "), PrevCount(" + old //
							+ "), " + String.valueOf(actor) // in case of null
							+ ", " + String.valueOf(reference)); // in case of null
					}
				}
			}
			
			if ((actor != null) && actor.isGM())
			{
				String referenceName = "no-reference";
				if (reference instanceof L2Object)
				{
					referenceName = (((L2Object) reference).getName() != null ? ((L2Object) reference).getName() : "no-name");
				}
				else if (reference instanceof String)
				{
					referenceName = (String) reference;
				}
				final String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
				if (Config.GMAUDIT)
				{
					GMAudit.auditGMAction(actor.getName() + " [" + actor.getObjectId() + "]" //
						, String.valueOf(process) // in case of null
							+ "(id: " + item.getId() //
							+ " count: " + item.getCount() //
							+ " itemObjId: " //
							+ item.getObjectId() + ")" //
						, targetName //
						, "L2Object referencing this action is: " + referenceName);
				}
			}
			
			// if it's a pet control item, delete the pet as well
			if (item.getItem().isPetItem())
			{
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
				{
					// Delete the pet in db
					statement.setInt(1, item.getObjectId());
					statement.execute();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete pet objectid:", e);
				}
			}
		}
	}
	
	public void reload()
	{
		load();
		EnchantItemHPBonusData.getInstance().load();
	}
	
	protected static class ResetOwner implements Runnable
	{
		L2ItemInstance _item;
		
		public ResetOwner(L2ItemInstance item)
		{
			_item = item;
		}
		
		@Override
		public void run()
		{
			_item.setOwnerId(0);
			_item.setItemLootShedule(null);
		}
	}
	
	public Set<Integer> getAllArmorsId()
	{
		return _armors.keySet();
	}
	
	public Collection<L2Armor> getAllArmors()
	{
		return _armors.values();
	}
	
	public Set<Integer> getAllWeaponsId()
	{
		return _weapons.keySet();
	}
	
	public Collection<L2Weapon> getAllWeapons()
	{
		return _weapons.values();
	}
	
	public Set<Integer> getAllEtcItemsId()
	{
		return _etcItems.keySet();
	}
	
	public Collection<L2EtcItem> getAllEtcItems()
	{
		return _etcItems.values();
	}
	
	public L2Item[] getAllItems()
	{
		return _allTemplates;
	}
	
	public int getArraySize()
	{
		return _allTemplates.length;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemTable _instance = new ItemTable();
	}
}
