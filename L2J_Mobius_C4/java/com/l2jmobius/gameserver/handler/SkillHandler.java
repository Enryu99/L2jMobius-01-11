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
package com.l2jmobius.gameserver.handler;

import java.util.Map;
import java.util.TreeMap;

import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Skill.SkillType;

/**
 * This class ...
 * @version $Revision: 1.1.4.4 $ $Date: 2005/04/03 15:55:06 $
 */
public class SkillHandler
{
	// private static Logger _log = Logger.getLogger(SkillHandler.class.getName());
	
	private static SkillHandler _instance;
	
	private final Map<L2Skill.SkillType, ISkillHandler> _datatable;
	
	public static SkillHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new SkillHandler();
		}
		return _instance;
	}
	
	private SkillHandler()
	{
		_datatable = new TreeMap<>();
	}
	
	public void registerSkillHandler(ISkillHandler handler)
	{
		final SkillType[] types = handler.getSkillIds();
		for (final SkillType t : types)
		{
			_datatable.put(t, handler);
		}
	}
	
	public ISkillHandler getSkillHandler(SkillType skillType)
	{
		return _datatable.get(skillType);
	}
	
	/**
	 * @return
	 */
	public int size()
	{
		return _datatable.size();
	}
}