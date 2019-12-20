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
package org.l2jmobius.gameserver.model;

public class FishData
{
	private final int _id;
	private final int _level;
	private final String _name;
	private final int _hp;
	private final int _hpRegen;
	private int _type;
	private final int _group;
	private final int _fishGuts;
	private final int _gutsCheckTime;
	private final int _waitTime;
	private final int _combatTime;
	
	public FishData(int id, int lvl, String name, int hp, int hpRegen, int type, int group, int fishGuts, int gutsCheckTime, int waitTime, int combatTime)
	{
		_id = id;
		_level = lvl;
		_name = name.intern();
		_hp = hp;
		_hpRegen = hpRegen;
		_type = type;
		_group = group;
		_fishGuts = fishGuts;
		_gutsCheckTime = gutsCheckTime;
		_waitTime = waitTime;
		_combatTime = combatTime;
	}
	
	public FishData(FishData copyOf)
	{
		_id = copyOf.getId();
		_level = copyOf.getLevel();
		_name = copyOf.getName();
		_hp = copyOf.getHP();
		_hpRegen = copyOf.getHpRegen();
		_type = copyOf.getType();
		_group = copyOf.getGroup();
		_fishGuts = copyOf.getFishGuts();
		_gutsCheckTime = copyOf.getGutsCheckTime();
		_waitTime = copyOf.getWaitTime();
		_combatTime = copyOf.getCombatTime();
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getHP()
	{
		return _hp;
	}
	
	public int getHpRegen()
	{
		return _hpRegen;
	}
	
	public int getType()
	{
		return _type;
	}
	
	public int getGroup()
	{
		return _group;
	}
	
	public int getFishGuts()
	{
		return _fishGuts;
	}
	
	public int getGutsCheckTime()
	{
		return _gutsCheckTime;
	}
	
	public int getWaitTime()
	{
		return _waitTime;
	}
	
	public int getCombatTime()
	{
		return _combatTime;
	}
	
	public void setType(int type)
	{
		_type = type;
	}
}
