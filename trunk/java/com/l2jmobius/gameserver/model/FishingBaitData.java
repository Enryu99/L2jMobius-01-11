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

import java.util.ArrayList;
import java.util.List;

/**
 * @author bit
 */
public class FishingBaitData
{
	private final int _itemId;
	private final int _level;
	private final double _chance;
	private final List<Integer> _rewards = new ArrayList<>();
	
	public FishingBaitData(int itemId, int level, double chance)
	{
		_itemId = itemId;
		_level = level;
		_chance = chance;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public double getChance()
	{
		return _chance;
	}
	
	public List<Integer> getRewards()
	{
		return _rewards;
	}
	
	public void addReward(int itemId)
	{
		_rewards.add(itemId);
	}
}
