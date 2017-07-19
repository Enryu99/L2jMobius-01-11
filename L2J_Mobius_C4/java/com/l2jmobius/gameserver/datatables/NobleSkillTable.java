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

import com.l2jmobius.gameserver.model.L2Skill;

/**
 * @author -Nemesiss-
 */
public class NobleSkillTable
{
	private static NobleSkillTable _instance;
	private static L2Skill[] _NobleSkills;
	
	private NobleSkillTable()
	{
		_NobleSkills = new L2Skill[8];
		_NobleSkills[0] = SkillTable.getInstance().getInfo(1323, 1);
		_NobleSkills[1] = SkillTable.getInstance().getInfo(325, 1);
		_NobleSkills[2] = SkillTable.getInstance().getInfo(326, 1);
		_NobleSkills[3] = SkillTable.getInstance().getInfo(327, 1);
		_NobleSkills[4] = SkillTable.getInstance().getInfo(1324, 1);
		_NobleSkills[5] = SkillTable.getInstance().getInfo(1325, 1);
		_NobleSkills[6] = SkillTable.getInstance().getInfo(1326, 1);
		_NobleSkills[7] = SkillTable.getInstance().getInfo(1327, 1);
	}
	
	public static NobleSkillTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new NobleSkillTable();
		}
		return _instance;
	}
	
	public L2Skill[] GetNobleSkills()
	{
		return _NobleSkills;
	}
}