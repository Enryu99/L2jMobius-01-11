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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.ArmorSet;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * This class loads and stores {@link ArmorSet}s, the key being the chest item id.
 */
public class ArmorSetData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ArmorSetData.class.getName());
	
	private ArmorSet[] _armorSets;
	
	protected ArmorSetData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/ArmorSets.xml");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		final Map<Integer, ArmorSet> armorSets = new HashMap<>();
		
		// StatsSet used to feed informations. Cleaned on every entry.
		final StatSet set = new StatSet();
		
		// First element is never read.
		final Node n = doc.getFirstChild();
		for (Node node = n.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (!"armorset".equalsIgnoreCase(node.getNodeName()))
			{
				continue;
			}
			
			// Parse and feed content.
			final NamedNodeMap attrs = node.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++)
			{
				final Node attr = attrs.item(i);
				set.set(attr.getNodeName(), attr.getNodeValue());
			}
			
			// Feed the map with new data.
			final int chestId = set.getInt("chest");
			armorSets.put(chestId, new ArmorSet(chestId, set.getInt("legs"), set.getInt("head"), set.getInt("gloves"), set.getInt("feet"), set.getInt("skillId"), set.getInt("shield"), set.getInt("shieldSkillId"), set.getInt("enchant6Skill")));
		}
		
		_armorSets = new ArmorSet[Collections.max(armorSets.keySet()) + 1];
		for (Entry<Integer, ArmorSet> armorSet : armorSets.entrySet())
		{
			_armorSets[armorSet.getKey()] = armorSet.getValue();
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + armorSets.size() + " armor sets.");
		armorSets.clear();
	}
	
	public boolean setExists(int chestId)
	{
		return (_armorSets.length > chestId) && (_armorSets[chestId] != null);
	}
	
	public ArmorSet getSet(int chestId)
	{
		if (_armorSets.length > chestId)
		{
			return _armorSets[chestId];
		}
		return null;
	}
	
	public static ArmorSetData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ArmorSetData INSTANCE = new ArmorSetData();
	}
}
