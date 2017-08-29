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
package com.l2jmobius.gameserver.engines.items;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2jmobius.commons.util.IGameXmlReader;
import com.l2jmobius.gameserver.engines.DocumentBase;
import com.l2jmobius.gameserver.enums.ItemSkillType;
import com.l2jmobius.gameserver.model.L2ExtractableProduct;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.conditions.Condition;
import com.l2jmobius.gameserver.model.holders.ItemChanceHolder;
import com.l2jmobius.gameserver.model.holders.ItemSkillHolder;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.stats.Stats;
import com.l2jmobius.gameserver.model.stats.functions.FuncTemplate;

/**
 * @author mkizub, JIV
 */
public final class DocumentItem extends DocumentBase implements IGameXmlReader
{
	private Item _currentItem = null;
	private final List<L2Item> _itemsInFile = new LinkedList<>();
	
	/**
	 * @param file
	 */
	public DocumentItem(File file)
	{
		super(file);
	}
	
	@Override
	protected StatsSet getStatsSet()
	{
		return _currentItem.set;
	}
	
	@Override
	protected String getTableValue(String name)
	{
		return _tables.get(name)[_currentItem.currentLevel];
	}
	
	@Override
	protected String getTableValue(String name, int idx)
	{
		return _tables.get(name)[idx - 1];
	}
	
	@Override
	protected void parseDocument(Document doc)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						try
						{
							_currentItem = new Item();
							parseItem(d);
							_itemsInFile.add(_currentItem.item);
							resetTable();
						}
						catch (Exception e)
						{
							_log.log(Level.WARNING, "Cannot create item " + _currentItem.id, e);
						}
					}
				}
			}
		}
	}
	
	protected void parseItem(Node n) throws InvocationTargetException
	{
		final int itemId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		final String className = n.getAttributes().getNamedItem("type").getNodeValue();
		final String itemName = n.getAttributes().getNamedItem("name").getNodeValue();
		final String additionalName = n.getAttributes().getNamedItem("additionalName") != null ? n.getAttributes().getNamedItem("additionalName").getNodeValue() : null;
		_currentItem.id = itemId;
		_currentItem.name = itemName;
		_currentItem.type = className;
		_currentItem.set = new StatsSet();
		_currentItem.set.set("item_id", itemId);
		_currentItem.set.set("name", itemName);
		_currentItem.set.set("additionalName", additionalName);
		
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but table node found! Item " + itemId);
				}
				parseTable(n);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but set node found! Item " + itemId);
				}
				parseBeanSet(n, _currentItem.set, 1);
			}
			else if ("stats".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("stat".equalsIgnoreCase(b.getNodeName()))
					{
						final Stats type = Stats.valueOfXml(b.getAttributes().getNamedItem("type").getNodeValue());
						final double value = Double.valueOf(b.getTextContent());
						_currentItem.item.addFunctionTemplate(new FuncTemplate(null, null, "add", 0x00, type, value));
					}
				}
			}
			else if ("skills".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(b.getNodeName()))
					{
						final int id = parseInteger(b.getAttributes(), "id");
						final int level = parseInteger(b.getAttributes(), "level");
						final ItemSkillType type = parseEnum(b.getAttributes(), ItemSkillType.class, "type", ItemSkillType.NORMAL);
						final int chance = parseInteger(b.getAttributes(), "type_chance", 0);
						final int value = parseInteger(b.getAttributes(), "type_value", 0);
						_currentItem.item.addSkill(new ItemSkillHolder(id, level, type, chance, value));
					}
				}
			}
			else if ("capsuled_items".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("item".equals(b.getNodeName()))
					{
						final int id = parseInteger(b.getAttributes(), "id");
						final int min = parseInteger(b.getAttributes(), "min");
						final int max = parseInteger(b.getAttributes(), "max");
						final double chance = parseDouble(b.getAttributes(), "chance");
						final int minEnchant = parseInteger(b.getAttributes(), "minEnchant", 0);
						final int maxEnchant = parseInteger(b.getAttributes(), "maxEnchant", 0);
						_currentItem.item.addCapsuledItem(new L2ExtractableProduct(id, min, max, chance, minEnchant, maxEnchant));
					}
				}
			}
			else if ("createItems".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("item".equals(b.getNodeName()))
					{
						final int id = parseInteger(b.getAttributes(), "id");
						final int count = parseInteger(b.getAttributes(), "count");
						final double chance = parseDouble(b.getAttributes(), "chance");
						_currentItem.item.addCreateItem(new ItemChanceHolder(id, chance, count));
					}
				}
			}
			else if ("cond".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				final Condition condition = parseCondition(n.getFirstChild(), _currentItem.item);
				final Node msg = n.getAttributes().getNamedItem("msg");
				final Node msgId = n.getAttributes().getNamedItem("msgId");
				if ((condition != null) && (msg != null))
				{
					condition.setMessage(msg.getNodeValue());
				}
				else if ((condition != null) && (msgId != null))
				{
					condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
					final Node addName = n.getAttributes().getNamedItem("addName");
					if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
					{
						condition.addName();
					}
				}
				_currentItem.item.attachCondition(condition);
			}
		}
		// bah! in this point item doesn't have to be still created
		makeItem();
	}
	
	private void makeItem() throws InvocationTargetException
	{
		// If item exists just reload the data.
		if (_currentItem.item != null)
		{
			_currentItem.item.set(_currentItem.set);
			return;
		}
		
		try
		{
			final Constructor<?> itemClass = Class.forName("com.l2jmobius.gameserver.model.items.L2" + _currentItem.type).getConstructor(StatsSet.class);
			_currentItem.item = (L2Item) itemClass.newInstance(_currentItem.set);
		}
		catch (Exception e)
		{
			throw new InvocationTargetException(e);
		}
	}
	
	public List<L2Item> getItemList()
	{
		return _itemsInFile;
	}
	
	@Override
	public void load()
	{
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
	}
}
