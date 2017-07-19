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
package com.l2jmobius.gameserver.model.base;

import com.l2jmobius.Config;

/**
 * Character Sub-Class Definition <BR>
 * Used to store key information about a character's sub-class.
 * @author Tempy
 */
public final class SubClass
{
	private PlayerClass _class;
	private long _exp = Experience.LEVEL[Config.STARTING_SUB_LEVEL];
	private int _sp = 0;
	private byte _level = Config.STARTING_SUB_LEVEL;
	private int _classIndex = 1;
	
	public SubClass(int classId, long exp, int sp, byte level, int classIndex)
	{
		_class = PlayerClass.values()[classId];
		_exp = exp;
		_sp = sp;
		_level = level;
		_classIndex = classIndex;
	}
	
	public SubClass(int classId, int classIndex)
	{
		// Used for defining a sub class using default values for XP, SP and player level.
		_class = PlayerClass.values()[classId];
		_classIndex = classIndex;
	}
	
	public SubClass()
	{
		// Used for specifying ALL attributes of a sub class directly,
		// using the preset default values.
	}
	
	public PlayerClass getClassDefinition()
	{
		return _class;
	}
	
	public int getClassId()
	{
		return _class.ordinal();
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	public void setClassId(int classId)
	{
		_class = PlayerClass.values()[classId];
	}
	
	public void setExp(long expValue)
	{
		if (expValue > Experience.LEVEL[Experience.MAX_LEVEL])
		{
			expValue = Experience.LEVEL[Experience.MAX_LEVEL];
		}
		
		_exp = expValue;
	}
	
	public void setSp(int spValue)
	{
		_sp = spValue;
	}
	
	public void setClassIndex(int classIndex)
	{
		_classIndex = classIndex;
	}
	
	public void setLevel(byte levelValue)
	{
		if (levelValue > 78)
		{
			levelValue = 78;
		}
		else if (levelValue < 40)
		{
			levelValue = 40;
		}
		
		_level = levelValue;
	}
	
	public void incLevel()
	{
		if (getLevel() == 78)
		{
			return;
		}
		
		_level++;
		setExp(Experience.LEVEL[getLevel()]);
	}
	
	public void decLevel()
	{
		if (getLevel() == 40)
		{
			return;
		}
		
		_level--;
		setExp(Experience.LEVEL[getLevel()]);
	}
}