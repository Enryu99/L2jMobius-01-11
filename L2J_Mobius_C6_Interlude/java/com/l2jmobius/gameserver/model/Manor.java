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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.xml.ItemTable;
import com.l2jmobius.gameserver.templates.item.Item;

/**
 * Service class for manor
 * @author l3x
 */
public class Manor
{
	private static Logger LOGGER = Logger.getLogger(Manor.class.getName());
	private static Manor _instance;
	
	private static Map<Integer, SeedData> _seeds;
	
	public Manor()
	{
		_seeds = new ConcurrentHashMap<>();
		parseData();
	}
	
	public static Manor getInstance()
	{
		if (_instance == null)
		{
			_instance = new Manor();
		}
		
		return _instance;
	}
	
	public List<Integer> getAllCrops()
	{
		final List<Integer> crops = new ArrayList<>();
		
		for (SeedData seed : _seeds.values())
		{
			if (!crops.contains(seed.getCrop()) && (seed.getCrop() != 0) && !crops.contains(seed.getCrop()))
			{
				crops.add(seed.getCrop());
			}
		}
		
		return crops;
	}
	
	public int getSeedBasicPrice(int seedId)
	{
		final Item seedItem = ItemTable.getInstance().getTemplate(seedId);
		
		if (seedItem != null)
		{
			return seedItem.getReferencePrice();
		}
		return 0;
	}
	
	public int getSeedBasicPriceByCrop(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
			{
				return getSeedBasicPrice(seed.getId());
			}
		}
		
		return 0;
	}
	
	public int getCropBasicPrice(int cropId)
	{
		final Item cropItem = ItemTable.getInstance().getTemplate(cropId);
		
		if (cropItem != null)
		{
			return cropItem.getReferencePrice();
		}
		return 0;
	}
	
	public int getMatureCrop(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
			{
				return seed.getMature();
			}
		}
		return 0;
	}
	
	/**
	 * Returns price which lord pays to buy one seed
	 * @param seedId
	 * @return seed price
	 */
	public int getSeedBuyPrice(int seedId)
	{
		final int buyPrice = getSeedBasicPrice(seedId) / 10;
		
		return buyPrice > 0 ? buyPrice : 1;
	}
	
	public int getSeedMinLevel(int seedId)
	{
		final SeedData seed = _seeds.get(seedId);
		
		if (seed != null)
		{
			return seed.getLevel() - 5;
		}
		return -1;
	}
	
	public int getSeedMaxLevel(int seedId)
	{
		final SeedData seed = _seeds.get(seedId);
		
		if (seed != null)
		{
			return seed.getLevel() + 5;
		}
		return -1;
	}
	
	public int getSeedLevelByCrop(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
			{
				return seed.getLevel();
			}
		}
		return 0;
	}
	
	public int getSeedLevel(int seedId)
	{
		final SeedData seed = _seeds.get(seedId);
		
		if (seed != null)
		{
			return seed.getLevel();
		}
		return -1;
	}
	
	public boolean isAlternative(int seedId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getId() == seedId)
			{
				return seed.isAlternative();
			}
		}
		return false;
	}
	
	public int getCropType(int seedId)
	{
		final SeedData seed = _seeds.get(seedId);
		
		if (seed != null)
		{
			return seed.getCrop();
		}
		return -1;
	}
	
	public synchronized int getRewardItem(int cropId, int type)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
			{
				return seed.getReward(type); // there can be several seeds with same crop, but reward should be the same for all
			}
		}
		return -1;
	}
	
	public synchronized int getRewardItemBySeed(int seedId, int type)
	{
		final SeedData seed = _seeds.get(seedId);
		
		if (seed != null)
		{
			return seed.getReward(type);
		}
		return 0;
	}
	
	/**
	 * Return all crops which can be purchased by given castle
	 * @param castleId
	 * @return
	 */
	public List<Integer> getCropsForCastle(int castleId)
	{
		final List<Integer> crops = new ArrayList<>();
		
		for (SeedData seed : _seeds.values())
		{
			if ((seed.getManorId() == castleId) && !crops.contains(seed.getCrop()))
			{
				crops.add(seed.getCrop());
			}
		}
		
		return crops;
	}
	
	/**
	 * Return list of seed ids, which belongs to castle with given id
	 * @param castleId - id of the castle
	 * @return seedIds - list of seed ids
	 */
	public List<Integer> getSeedsForCastle(int castleId)
	{
		final List<Integer> seedsID = new ArrayList<>();
		
		for (SeedData seed : _seeds.values())
		{
			if ((seed.getManorId() == castleId) && !seedsID.contains(seed.getId()))
			{
				seedsID.add(seed.getId());
			}
		}
		
		return seedsID;
	}
	
	/**
	 * Returns castle id where seed can be sowned<br>
	 * @param seedId
	 * @return castleId
	 */
	public int getCastleIdForSeed(int seedId)
	{
		final SeedData seed = _seeds.get(seedId);
		if (seed != null)
		{
			return seed.getManorId();
		}
		return 0;
	}
	
	public int getSeedSaleLimit(int seedId)
	{
		final SeedData seed = _seeds.get(seedId);
		
		if (seed != null)
		{
			return seed.getSeedLimit();
		}
		return 0;
	}
	
	public int getCropPuchaseLimit(int cropId)
	{
		for (SeedData seed : _seeds.values())
		{
			if (seed.getCrop() == cropId)
			{
				return seed.getCropLimit();
			}
		}
		return 0;
	}
	
	private class SeedData
	{
		private int _id;
		private final int _level; // seed level
		private final int _crop; // crop type
		private final int _mature; // mature crop type
		private int _type1;
		private int _type2;
		private int _manorId; // id of manor (castle id) where seed can be farmed
		private int _isAlternative;
		private int _limitSeeds;
		private int _limitCrops;
		
		public SeedData(int level, int crop, int mature)
		{
			_level = level;
			_crop = crop;
			_mature = mature;
		}
		
		public void setData(int id, int t1, int t2, int manorId, int isAlt, int lim1, int lim2)
		{
			_id = id;
			_type1 = t1;
			_type2 = t2;
			_manorId = manorId;
			_isAlternative = isAlt;
			_limitSeeds = lim1;
			_limitCrops = lim2;
		}
		
		public int getManorId()
		{
			return _manorId;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public int getCrop()
		{
			return _crop;
		}
		
		public int getMature()
		{
			return _mature;
		}
		
		public int getReward(int type)
		{
			return type == 1 ? _type1 : _type2;
		}
		
		public int getLevel()
		{
			return _level;
		}
		
		public boolean isAlternative()
		{
			return _isAlternative == 1;
		}
		
		public int getSeedLimit()
		{
			return _limitSeeds * Config.RATE_DROP_MANOR;
		}
		
		public int getCropLimit()
		{
			return _limitCrops * Config.RATE_DROP_MANOR;
		}
	}
	
	private void parseData()
	{
		FileReader reader = null;
		BufferedReader buff = null;
		LineNumberReader lnr = null;
		
		try
		{
			final File seedData = new File(Config.DATAPACK_ROOT, "data/csv/seeds.csv");
			
			reader = new FileReader(seedData);
			buff = new BufferedReader(reader);
			lnr = new LineNumberReader(buff);
			
			String line = null;
			
			while ((line = lnr.readLine()) != null)
			{
				if ((line.trim().length() == 0) || line.startsWith("#"))
				{
					continue;
				}
				
				SeedData seed = parseList(line);
				_seeds.put(seed.getId(), seed);
			}
			
			LOGGER.info("ManorManager: Loaded " + _seeds.size() + " seeds");
		}
		catch (FileNotFoundException e)
		{
			LOGGER.info("seeds.csv is missing in data csv folder");
		}
		catch (Exception e)
		{
			LOGGER.info("error while loading seeds: " + e.getMessage());
		}
		finally
		{
			if (lnr != null)
			{
				try
				{
					lnr.close();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
			
			if (buff != null)
			{
				try
				{
					buff.close();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
			
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
			
		}
	}
	
	private SeedData parseList(String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");
		
		final int seedId = Integer.parseInt(st.nextToken()); // seed id
		final int level = Integer.parseInt(st.nextToken()); // seed level
		final int cropId = Integer.parseInt(st.nextToken()); // crop id
		final int matureId = Integer.parseInt(st.nextToken()); // mature crop id
		final int type1R = Integer.parseInt(st.nextToken()); // type I reward
		final int type2R = Integer.parseInt(st.nextToken()); // type II reward
		final int manorId = Integer.parseInt(st.nextToken()); // id of manor, where seed can be farmed
		final int isAlt = Integer.parseInt(st.nextToken()); // alternative seed
		final int limitSeeds = Integer.parseInt(st.nextToken()); // limit for seeds
		final int limitCrops = Integer.parseInt(st.nextToken()); // limit for crops
		
		final SeedData seed = new SeedData(level, cropId, matureId);
		seed.setData(seedId, type1R, type2R, manorId, isAlt, limitSeeds, limitCrops);
		
		return seed;
	}
}
