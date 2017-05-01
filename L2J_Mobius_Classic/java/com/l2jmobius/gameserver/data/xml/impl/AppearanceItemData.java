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
package com.l2jmobius.gameserver.data.xml.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jmobius.commons.util.IGameXmlReader;
import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.enums.Race;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.items.appearance.AppearanceStone;
import com.l2jmobius.gameserver.model.items.appearance.AppearanceTargetType;
import com.l2jmobius.gameserver.model.items.type.CrystalType;

/**
 * @author UnAfraid
 */
public class AppearanceItemData implements IGameXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AppearanceItemData.class.getName());
	
	private final Map<Integer, AppearanceStone> _stones = new HashMap<>();
	
	protected AppearanceItemData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/AppearanceStones.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded: " + _stones.size() + " Stones");
		
		//@formatter:off
		/*
		for (L2Item item : ItemTable.getInstance().getAllItems())
		{
			if ((item == null) || !item.getName().contains("Appearance Stone"))
			{
				continue;
			}
			if (item.getName().contains("Pack") || _stones.containsKey(item.getId()))
			{
				continue;
			}
			
			System.out.println("Unhandled appearance stone: " + item);
		}
		*/
		//@formatter:on
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		StatsSet set;
		Node att;
		NamedNodeMap attrs;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("appearance_stone".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						set = new StatsSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final AppearanceStone stone = new AppearanceStone(set);
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							switch (c.getNodeName())
							{
								case "grade":
								{
									final CrystalType type = CrystalType.valueOf(c.getTextContent());
									stone.addCrystalType(type);
									break;
								}
								case "targetType":
								{
									final AppearanceTargetType type = AppearanceTargetType.valueOf(c.getTextContent());
									stone.addTargetType(type);
									break;
								}
								case "bodyPart":
								{
									final int part = ItemTable.SLOTS.get(c.getTextContent());
									stone.addBodyPart(part);
									break;
								}
								case "race":
								{
									final Race race = Race.valueOf(c.getTextContent());
									stone.addRace(race);
									break;
								}
								case "raceNot":
								{
									final Race raceNot = Race.valueOf(c.getTextContent());
									stone.addRaceNot(raceNot);
									break;
								}
							}
						}
						_stones.put(stone.getId(), stone);
					}
				}
			}
		}
	}
	
	public AppearanceStone getStone(int stone)
	{
		return _stones.get(stone);
	}
	
	/**
	 * Gets the single instance of AppearanceItemData.
	 * @return single instance of AppearanceItemData
	 */
	public static AppearanceItemData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AppearanceItemData _instance = new AppearanceItemData();
	}
}
