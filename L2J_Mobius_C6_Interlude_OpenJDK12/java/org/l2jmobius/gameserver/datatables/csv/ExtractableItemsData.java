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
package org.l2jmobius.gameserver.datatables.csv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.ExtractableItem;
import org.l2jmobius.gameserver.model.ExtractableProductItem;

/**
 * @author FBIagent
 */
public class ExtractableItemsData
{
	private static Logger LOGGER = Logger.getLogger(ExtractableItemsData.class.getName());
	
	// Map<itemid, ExtractableItem>
	private Map<Integer, ExtractableItem> _items;
	
	private static ExtractableItemsData _instance = null;
	
	public static ExtractableItemsData getInstance()
	{
		if (_instance == null)
		{
			_instance = new ExtractableItemsData();
		}
		
		return _instance;
	}
	
	public ExtractableItemsData()
	{
		_items = new HashMap<>();
		
		Scanner s = null;
		try
		{
			s = new Scanner(new File(Config.DATAPACK_ROOT + "/data/csv/extractable_items.csv"));
			
			int lineCount = 0;
			while (s.hasNextLine())
			{
				lineCount++;
				
				final String line = s.nextLine();
				
				if (line.startsWith("#"))
				{
					continue;
				}
				else if (line.equals(""))
				{
					continue;
				}
				
				final String[] lineSplit = line.split(";");
				int itemID = 0;
				try
				{
					itemID = Integer.parseInt(lineSplit[0]);
				}
				catch (Exception e)
				{
					LOGGER.info("Extractable items data: Error in line " + lineCount + " -> invalid item id or wrong seperator after item id!");
					LOGGER.info("		" + line);
					return;
				}
				
				final List<ExtractableProductItem> product_temp = new ArrayList<>(lineSplit.length);
				for (int i = 0; i < (lineSplit.length - 1); i++)
				{
					String[] lineSplit2 = lineSplit[i + 1].split(",");
					if (lineSplit2.length != 3)
					{
						LOGGER.info("Extractable items data: Error in line " + lineCount + " -> wrong seperator!");
						LOGGER.info("		" + line);
						continue;
					}
					
					int production = 0;
					int amount = 0;
					int chance = 0;
					
					try
					{
						production = Integer.parseInt(lineSplit2[0]);
						amount = Integer.parseInt(lineSplit2[1]);
						chance = Integer.parseInt(lineSplit2[2]);
					}
					catch (Exception e)
					{
						LOGGER.info("Extractable items data: Error in line " + lineCount + " -> incomplete/invalid production data or wrong seperator!");
						LOGGER.info("		" + line);
						continue;
					}
					
					product_temp.add(new ExtractableProductItem(production, amount, chance));
				}
				
				int fullChances = 0;
				for (ExtractableProductItem Pi : product_temp)
				{
					fullChances += Pi.getChance();
				}
				
				if (fullChances > 100)
				{
					LOGGER.info("Extractable items data: Error in line " + lineCount + " -> all chances together are more then 100!");
					LOGGER.info("		" + line);
					continue;
				}
				
				_items.put(itemID, new ExtractableItem(itemID, product_temp));
			}
			
			LOGGER.info("Extractable items data: Loaded " + _items.size() + " extractable items!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			LOGGER.info("Extractable items data: Can not find './data/csv/extractable_items.csv'");
		}
		finally
		{
			if (s != null)
			{
				try
				{
					s.close();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}
	
	public ExtractableItem getExtractableItem(int itemID)
	{
		return _items.get(itemID);
	}
	
	public int[] itemIDs()
	{
		final int size = _items.size();
		final int[] result = new int[size];
		int i = 0;
		for (ExtractableItem ei : _items.values())
		{
			result[i] = ei.getItemId();
			i++;
		}
		return result;
	}
}
