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
package com.l2jmobius.gameserver.datatables;

import java.util.HashMap;
import java.util.Map;

import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.skills.SkillsEngine;
import com.l2jmobius.gameserver.templates.L2WeaponType;

import gnu.trove.map.hash.TIntIntHashMap;

/**
 *
 */
public class SkillTable
{
	private static SkillTable _instance;
	
	private final Map<Integer, L2Skill> _skills;
	private final TIntIntHashMap _skillMaxLevel;
	
	public static SkillTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new SkillTable();
		}
		return _instance;
	}
	
	private SkillTable()
	{
		_skills = new HashMap<>();
		_skillMaxLevel = new TIntIntHashMap();
		reload();
	}
	
	public void reload()
	{
		_skills.clear();
		SkillsEngine.getInstance().loadAllSkills(_skills);
		
		_skillMaxLevel.clear();
		for (final L2Skill skill : _skills.values())
		{
			final int skillLvl = skill.getLevel();
			if (skillLvl > 99)
			{
				continue;
			}
			
			final int skillId = skill.getId();
			final int maxLvl = _skillMaxLevel.get(skillId);
			if (skillLvl > maxLvl)
			{
				_skillMaxLevel.put(skillId, skillLvl);
			}
		}
	}
	
	/**
	 * Provides the skill hash
	 * @param skill The L2Skill to be hashed
	 * @return getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(L2Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return (skillId * 256) + skillLevel;
	}
	
	public final L2Skill getInfo(final int skillId, final int level)
	{
		return _skills.get(getSkillHashCode(skillId, level));
	}
	
	public final int getMaxLevel(final int skillId)
	{
		return _skillMaxLevel.get(skillId);
	}
	
	private static final L2WeaponType[] weaponDbMasks =
	
	{
		L2WeaponType.ETC,
		L2WeaponType.BOW,
		L2WeaponType.POLE,
		L2WeaponType.DUALFIST,
		L2WeaponType.DUAL,
		L2WeaponType.BLUNT,
		L2WeaponType.SWORD,
		L2WeaponType.DAGGER,
		L2WeaponType.BIGSWORD,
		L2WeaponType.ROD,
		L2WeaponType.BIGBLUNT
	};
	
	public int calcWeaponsAllowed(int mask)
	{
		if (mask == 0)
		{
			return 0;
		}
		
		int weaponsAllowed = 0;
		
		for (int i = 0; i < weaponDbMasks.length; i++)
		{
			if ((mask & (1 << i)) != 0)
			{
				weaponsAllowed |= weaponDbMasks[i].mask();
			}
		}
		
		return weaponsAllowed;
	}
}