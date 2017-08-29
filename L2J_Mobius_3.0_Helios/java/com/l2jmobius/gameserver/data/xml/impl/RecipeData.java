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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jmobius.commons.util.IGameXmlReader;
import com.l2jmobius.gameserver.model.L2RecipeInstance;
import com.l2jmobius.gameserver.model.L2RecipeList;
import com.l2jmobius.gameserver.model.L2RecipeStatInstance;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * The Class RecipeData.
 * @author Zoey76
 */
public class RecipeData implements IGameXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RecipeData.class.getName());
	
	private final Map<Integer, L2RecipeList> _recipes = new HashMap<>();
	
	/**
	 * Instantiates a new recipe data.
	 */
	protected RecipeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_recipes.clear();
		parseDatapackFile("data/Recipes.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _recipes.size() + " recipes.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		// TODO: Cleanup checks enforced by XSD.
		final List<L2RecipeInstance> recipePartList = new ArrayList<>();
		final List<L2RecipeStatInstance> recipeStatUseList = new ArrayList<>();
		final List<L2RecipeStatInstance> recipeAltStatChangeList = new ArrayList<>();
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				RECIPES_FILE: for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						recipePartList.clear();
						recipeStatUseList.clear();
						recipeAltStatChangeList.clear();
						final NamedNodeMap attrs = d.getAttributes();
						Node att;
						int id = -1;
						boolean haveRare = false;
						final StatsSet set = new StatsSet();
						
						att = attrs.getNamedItem("id");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing id for recipe item, skipping");
							continue;
						}
						id = Integer.parseInt(att.getNodeValue());
						set.set("id", id);
						
						att = attrs.getNamedItem("recipeId");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing recipeId for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("recipeId", Integer.parseInt(att.getNodeValue()));
						
						att = attrs.getNamedItem("name");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing name for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("recipeName", att.getNodeValue());
						
						att = attrs.getNamedItem("craftLevel");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing level for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("craftLevel", Integer.parseInt(att.getNodeValue()));
						
						att = attrs.getNamedItem("type");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing type for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("isDwarvenRecipe", att.getNodeValue().equalsIgnoreCase("dwarven"));
						
						att = attrs.getNamedItem("successRate");
						if (att == null)
						{
							LOGGER.severe(getClass().getSimpleName() + ": Missing successRate for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("successRate", Integer.parseInt(att.getNodeValue()));
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("statUse".equalsIgnoreCase(c.getNodeName()))
							{
								final String statName = c.getAttributes().getNamedItem("name").getNodeValue();
								final int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());
								try
								{
									recipeStatUseList.add(new L2RecipeStatInstance(statName, value));
								}
								catch (Exception e)
								{
									LOGGER.severe(getClass().getSimpleName() + ": Error in StatUse parameter for recipe item id: " + id + ", skipping");
									continue RECIPES_FILE;
								}
							}
							else if ("altStatChange".equalsIgnoreCase(c.getNodeName()))
							{
								final String statName = c.getAttributes().getNamedItem("name").getNodeValue();
								final int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());
								try
								{
									recipeAltStatChangeList.add(new L2RecipeStatInstance(statName, value));
								}
								catch (Exception e)
								{
									LOGGER.severe(getClass().getSimpleName() + ": Error in AltStatChange parameter for recipe item id: " + id + ", skipping");
									continue RECIPES_FILE;
								}
							}
							else if ("ingredient".equalsIgnoreCase(c.getNodeName()))
							{
								final int ingId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
								final int ingCount = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
								recipePartList.add(new L2RecipeInstance(ingId, ingCount));
							}
							else if ("production".equalsIgnoreCase(c.getNodeName()))
							{
								set.set("itemId", Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
								set.set("count", Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue()));
							}
							else if ("productionRare".equalsIgnoreCase(c.getNodeName()))
							{
								set.set("rareItemId", Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
								set.set("rareCount", Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue()));
								set.set("rarity", Integer.parseInt(c.getAttributes().getNamedItem("rarity").getNodeValue()));
								haveRare = true;
							}
						}
						
						final L2RecipeList recipeList = new L2RecipeList(set, haveRare);
						for (L2RecipeInstance recipePart : recipePartList)
						{
							recipeList.addRecipe(recipePart);
						}
						for (L2RecipeStatInstance recipeStatUse : recipeStatUseList)
						{
							recipeList.addStatUse(recipeStatUse);
						}
						for (L2RecipeStatInstance recipeAltStatChange : recipeAltStatChangeList)
						{
							recipeList.addAltStatChange(recipeAltStatChange);
						}
						
						_recipes.put(id, recipeList);
					}
				}
			}
		}
	}
	
	/**
	 * Gets the recipe list.
	 * @param listId the list id
	 * @return the recipe list
	 */
	public L2RecipeList getRecipeList(int listId)
	{
		return _recipes.get(listId);
	}
	
	/**
	 * Gets the recipe by item id.
	 * @param itemId the item id
	 * @return the recipe by item id
	 */
	public L2RecipeList getRecipeByItemId(int itemId)
	{
		for (L2RecipeList find : _recipes.values())
		{
			if (find.getRecipeId() == itemId)
			{
				return find;
			}
		}
		return null;
	}
	
	/**
	 * Gets the all item ids.
	 * @return the all item ids
	 */
	public int[] getAllItemIds()
	{
		final int[] idList = new int[_recipes.size()];
		int i = 0;
		for (L2RecipeList rec : _recipes.values())
		{
			idList[i++] = rec.getRecipeId();
		}
		return idList;
	}
	
	/**
	 * Gets the valid recipe list.
	 * @param player the player
	 * @param id the recipe list id
	 * @return the valid recipe list
	 */
	public L2RecipeList getValidRecipeList(L2PcInstance player, int id)
	{
		final L2RecipeList recipeList = _recipes.get(id);
		if ((recipeList == null) || (recipeList.getRecipes().length == 0))
		{
			player.sendMessage("No recipe for: " + id);
			player.isInCraftMode(false);
			return null;
		}
		return recipeList;
	}
	
	/**
	 * Gets the single instance of RecipeData.
	 * @return single instance of RecipeData
	 */
	public static RecipeData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * The Class SingletonHolder.
	 */
	private static class SingletonHolder
	{
		protected static final RecipeData _instance = new RecipeData();
	}
}
