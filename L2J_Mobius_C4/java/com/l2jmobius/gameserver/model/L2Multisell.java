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

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.MultiSellList;
import com.l2jmobius.gameserver.templates.L2Armor;
import com.l2jmobius.gameserver.templates.L2Item;
import com.l2jmobius.gameserver.templates.L2Weapon;

import javolution.util.FastList;

/**
 * Multisell list manager
 */
public class L2Multisell
{
	private static Logger _log = Logger.getLogger(L2Multisell.class.getName());
	private final List<MultiSellListContainer> entries = new FastList<>();
	private static L2Multisell _instance;
	
	public MultiSellListContainer getList(int id)
	{
		synchronized (entries)
		{
			for (final MultiSellListContainer list : entries)
			{
				if (list.getListId() == id)
				{
					return list;
				}
			}
		}
		
		_log.warning("[L2Multisell] cant find list with id: " + id);
		return null;
	}
	
	public L2Multisell()
	{
		parseData();
	}
	
	public void reload()
	{
		parseData();
	}
	
	public static L2Multisell getInstance()
	{
		if (_instance == null)
		{
			_instance = new L2Multisell();
		}
		return _instance;
	}
	
	private void parseData()
	{
		entries.clear();
		parse();
		_log.config("L2Multisell: Loaded " + entries.size() + " lists.");
	}
	
	public class MultiSellEntry
	{
		private int _entryId;
		
		private final List<MultiSellIngredient> _products = new FastList<>();
		private final List<MultiSellIngredient> _ingredients = new FastList<>();
		
		/**
		 * @param entryId The entryId to set.
		 */
		public void setEntryId(int entryId)
		{
			_entryId = entryId;
		}
		
		/**
		 * @return Returns the entryId.
		 */
		public int getEntryId()
		{
			return _entryId;
		}
		
		/**
		 * @param product The product to add.
		 */
		public void addProduct(MultiSellIngredient product)
		{
			_products.add(product);
		}
		
		/**
		 * @return Returns the products.
		 */
		public List<MultiSellIngredient> getProducts()
		{
			return _products;
		}
		
		/**
		 * @param ingredient The ingredients to set.
		 */
		public void addIngredient(MultiSellIngredient ingredient)
		{
			_ingredients.add(ingredient);
		}
		
		/**
		 * @return Returns the ingredients.
		 */
		public List<MultiSellIngredient> getIngredients()
		{
			return _ingredients;
		}
	}
	
	public class MultiSellIngredient
	{
		private int _itemId;
		private long _itemCount;
		private int _enchantmentLevel;
		
		public MultiSellIngredient(int itemId, int itemCount)
		{
			this(itemId, itemCount, 0);
		}
		
		public MultiSellIngredient(int itemId, long itemCount, int enchantmentLevel)
		{
			setItemId(itemId);
			setItemCount(itemCount);
			setEnchantmentLevel(enchantmentLevel);
		}
		
		public MultiSellIngredient(MultiSellIngredient e)
		{
			_itemId = e.getItemId();
			_itemCount = e.getItemCount();
			_enchantmentLevel = e.getEnchantmentLevel();
		}
		
		/**
		 * @param itemId The itemId to set.
		 */
		public void setItemId(int itemId)
		{
			_itemId = itemId;
		}
		
		/**
		 * @return Returns the itemId.
		 */
		public int getItemId()
		{
			return _itemId;
		}
		
		/**
		 * @param itemCount The itemCount to set.
		 */
		public void setItemCount(long itemCount)
		{
			_itemCount = itemCount;
		}
		
		/**
		 * @return Returns the itemCount.
		 */
		public long getItemCount()
		{
			return _itemCount;
		}
		
		/**
		 * @param enchantmentLevel The itemCount to set.
		 */
		public void setEnchantmentLevel(int enchantmentLevel)
		{
			_enchantmentLevel = enchantmentLevel;
		}
		
		/**
		 * @return Returns the itemCount.
		 */
		public int getEnchantmentLevel()
		{
			return _enchantmentLevel;
		}
	}
	
	public class MultiSellListContainer
	{
		private int _listId;
		private boolean _applyTaxes = false;
		private boolean _maintainEnchantment = false;
		private List<Integer> _npcIds;
		
		List<MultiSellEntry> entriesC;
		
		public MultiSellListContainer()
		{
			entriesC = new FastList<>();
		}
		
		/**
		 * @param listId The listId to set.
		 */
		public void setListId(int listId)
		{
			_listId = listId;
		}
		
		public void setApplyTaxes(boolean applyTaxes)
		{
			_applyTaxes = applyTaxes;
		}
		
		public void setMaintainEnchantment(boolean maintainEnchantment)
		{
			_maintainEnchantment = maintainEnchantment;
		}
		
		public void addNpcId(int objId)
		{
			_npcIds.add(objId);
		}
		
		/**
		 * @return Returns the listId.
		 */
		public int getListId()
		{
			return _listId;
		}
		
		public boolean getApplyTaxes()
		{
			return _applyTaxes;
		}
		
		public boolean getMaintainEnchantment()
		{
			return _maintainEnchantment;
		}
		
		public boolean checkNpcId(int npcId)
		{
			if (_npcIds == null)
			{
				synchronized (this)
				{
					if (_npcIds == null)
					{
						_npcIds = new FastList<>();
					}
				}
				return false;
			}
			return _npcIds.contains(npcId);
		}
		
		public void addEntry(MultiSellEntry e)
		{
			entriesC.add(e);
		}
		
		public List<MultiSellEntry> getEntries()
		{
			return entriesC;
		}
	}
	
	private void hashFiles(String dirname, List<File> hash)
	{
		final File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);
		if (!dir.exists())
		{
			_log.config("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		final File[] files = dir.listFiles();
		for (final File f : files)
		{
			if (f.getName().endsWith(".xml"))
			{
				hash.add(f);
			}
		}
	}
	
	private void parse()
	{
		Document doc = null;
		int id = 0;
		final List<File> files = new FastList<>();
		hashFiles("multisell", files);
		
		for (final File f : files)
		{
			id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
			try
			{
				
				final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				doc = factory.newDocumentBuilder().parse(f);
			}
			catch (final Exception e)
			{
				_log.log(Level.SEVERE, "Error loading file " + f, e);
			}
			try
			{
				final MultiSellListContainer list = parseDocument(doc);
				list.setListId(id);
				entries.add(list);
			}
			catch (final Exception e)
			{
				_log.log(Level.SEVERE, "Error in file " + f, e);
			}
		}
	}
	
	protected MultiSellListContainer parseDocument(Document doc)
	{
		final MultiSellListContainer list = new MultiSellListContainer();
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				attribute = n.getAttributes().getNamedItem("applyTaxes");
				if (attribute == null)
				{
					list.setApplyTaxes(false);
				}
				else
				{
					list.setApplyTaxes(Boolean.parseBoolean(attribute.getNodeValue()));
				}
				attribute = n.getAttributes().getNamedItem("maintainEnchantment");
				if (attribute == null)
				{
					list.setMaintainEnchantment(false);
				}
				else
				{
					list.setMaintainEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
				}
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						final MultiSellEntry e = parseEntry(d);
						list.addEntry(e);
					}
				}
			}
			else if ("item".equalsIgnoreCase(n.getNodeName()))
			{
				final MultiSellEntry e = parseEntry(n);
				list.addEntry(e);
			}
		}
		
		return list;
	}
	
	protected MultiSellEntry parseEntry(Node n)
	{
		final int entryId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		
		final Node first = n.getFirstChild();
		final MultiSellEntry entry = new MultiSellEntry();
		
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				final int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				final int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				
				final MultiSellIngredient e = new MultiSellIngredient(id, count);
				entry.addIngredient(e);
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				final int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				final int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				
				final MultiSellIngredient e = new MultiSellIngredient(id, count);
				entry.addProduct(e);
			}
		}
		
		entry.setEntryId(entryId);
		
		return entry;
	}
	
	/**
	 * This will generate the multisell list for the items. There exist various parameters in multisells that affect the way they will appear: 1) inventory only: * if true, only show items of the multisell for which the "primary" ingredients are already in the player's inventory. By "primary"
	 * ingredients we mean weapon and armor. * if false, show the entire list. 2) maintain enchantment: presumably, only lists with "inventory only" set to true should sometimes have this as true. This makes no sense otherwise... * If true, then the product will match the enchantment level of the
	 * ingredient. if the player has multiple items that match the ingredient list but the enchantment levels differ, then the entries need to be duplicated to show the products and ingredients for each enchantment level. For example: If the player has a crystal staff +1 and a crystal staff +3 and
	 * goes to exchange it at the mammon, the list should have all exchange possibilities for the +1 staff, followed by all possibilities for the +3 staff. * If false, then any level ingredient will be considered equal and product will always be at +0 3) apply taxes: affects the amount of adena and
	 * ancient adena in ingredients.
	 * @param listId
	 * @param inventoryOnly
	 * @param player
	 * @param merchant
	 * @return
	 */
	private MultiSellListContainer generateMultiSell(int listId, boolean inventoryOnly, L2PcInstance player, L2NpcInstance merchant)
	{
		
		final MultiSellListContainer listTemplate = L2Multisell.getInstance().getList(listId);
		MultiSellListContainer list = new MultiSellListContainer();
		
		if (listTemplate == null)
		{
			return list;
		}
		
		list = L2Multisell.getInstance().new MultiSellListContainer();
		list.setListId(listId);
		if ((merchant != null) && !listTemplate.checkNpcId(merchant.getNpcId()))
		{
			listTemplate.addNpcId(merchant.getNpcId());
		}
		
		if (inventoryOnly)
		{
			if (player == null)
			{
				return list;
			}
			
			L2ItemInstance[] items;
			if (listTemplate.getMaintainEnchantment())
			{
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false);
			}
			else
			{
				items = player.getInventory().getUniqueItems(false, false);
			}
			
			int enchantLevel;
			for (final L2ItemInstance item : items)
			{
				// only do the matchup on equipable items that are not currently equipped
				// so for each appropriate item, produce a set of entries for the multisell list.
				if (!item.isWear() && ((item.getItem() instanceof L2Armor) || (item.getItem() instanceof L2Weapon)))
				{
					enchantLevel = (listTemplate.getMaintainEnchantment() ? item.getEnchantLevel() : 0);
					// loop through the entries to see which ones we wish to include
					for (final MultiSellEntry ent : listTemplate.getEntries())
					{
						boolean doInclude = false;
						
						// check ingredients of this entry to see if it's an entry we'd like to include.
						for (final MultiSellIngredient ing : ent.getIngredients())
						{
							if (item.getItemId() == ing.getItemId())
							{
								doInclude = true;
								break;
							}
						}
						
						// manipulate the ingredients of the template entry for this particular instance shown
						// i.e: Assign enchant levels and/or apply taxes as needed.
						if (doInclude)
						{
							list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.getMaintainEnchantment(), enchantLevel, merchant));
						}
					}
				}
			} // end for each inventory item.
		} // end if "inventory-only"
		else // this is a list-all type
		{
			// if no taxes are applied, no modifications are needed
			for (final MultiSellEntry ent : listTemplate.getEntries())
			{
				list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, merchant));
			}
		}
		
		return list;
	}
	
	// Regarding taxation, the following appears to be the case:
	// a) The count of aa remains unchanged (taxes do not affect aa directly).
	// b) 5/6 of the amount of aa is taxed by the normal tax rate.
	// c) the resulting taxes are added as normal adena value.
	// d) normal adena are taxed fully.
	// e) Items other than adena and ancient adena are not taxed even when the list is taxable.
	// example: If the template has an item worth 120aa, and the tax is 10%,
	// then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
	// so the final price will be 120aa and 10a!
	private MultiSellEntry prepareEntry(MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, L2NpcInstance merchant)
	{
		final MultiSellEntry newEntry = L2Multisell.getInstance().new MultiSellEntry();
		newEntry.setEntryId((templateEntry.getEntryId() * 100000) + enchantLevel);
		
		for (final MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			
			// if taxes are to be applied, modify/add the adena count based on the template adena/ancient adena count
			if (applyTaxes && ((ing.getItemId() == 57) || (ing.getItemId() == 5575)))
			{
				
				double taxRate = 0.0;
				if ((merchant != null) && (merchant.getCastle() != null))
				{
					taxRate = merchant.getCastle().getTaxRate();
				}
				
				if (ing.getItemId() == 57)
				{
					final int taxAmount = (int) Math.round(ing.getItemCount() * taxRate);
					newIngredient.setItemCount(ing.getItemCount() + taxAmount);
				}
				else // ancient adena
				{
					// add the ancient adena count normally
					newEntry.addIngredient(newIngredient);
					final double taxableCount = (ing.getItemCount() * 5.0) / 6;
					if (taxRate == 0)
					{
						continue;
					}
					newIngredient = L2Multisell.getInstance().new MultiSellIngredient(57, (int) Math.round(taxableCount * taxRate));
				}
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			else if (maintainEnchantment)
			{
				final L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// Now modify the enchantment level of products, if necessary
		for (final MultiSellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			final MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			
			if (maintainEnchantment)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				final L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	public void createMultiSell(int listId, L2PcInstance player, boolean inventoryOnly, L2NpcInstance merchant)
	{
		final MultiSellListContainer list = generateMultiSell(listId, inventoryOnly, player, merchant);
		MultiSellListContainer temp = new MultiSellListContainer();
		int page = 1;
		
		temp.setListId(list.getListId());
		for (final MultiSellEntry e : list.getEntries())
		{
			if (temp.getEntries().size() == 40)
			{
				player.sendPacket(new MultiSellList(temp, page, 0));
				page++;
				temp = new MultiSellListContainer();
				temp.setListId(list.getListId());
			}
			temp.addEntry(e);
		}
		player.sendPacket(new MultiSellList(temp, page, 1));
	}
}