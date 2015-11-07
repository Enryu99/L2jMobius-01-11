/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.data.xml.impl.EnchantSkillGroupsData;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup.EnchantSkillHolder;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;

public final class ExEnchantSkillInfo extends L2GameServerPacket
{
	private final List<Integer> _routes = new ArrayList<>(); // skill lvls for each route
	
	private final int _id;
	private final int _lvl;
	private final int _maxlvl;
	private boolean _maxEnchanted = false;
	
	public ExEnchantSkillInfo(int id, int lvl)
	{
		_id = id;
		_lvl = lvl;
		_maxlvl = SkillData.getInstance().getMaxLevel(_id);
		
		L2EnchantSkillLearn enchantLearn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(_id);
		// do we have this skill?
		if (enchantLearn != null)
		{
			// skill already enchanted?
			if (_lvl > 1000)
			{
				_maxEnchanted = enchantLearn.isMaxEnchant(_lvl);
				
				// get detail for next level
				EnchantSkillHolder esd = enchantLearn.getEnchantSkillHolder(_lvl);
				
				// if it exists add it
				if ((esd != null) && !_maxEnchanted)
				{
					_routes.add(_lvl + 1); // current enchant add firts
				}
				
				int skillLvL = (_lvl % 1000);
				
				for (int route : enchantLearn.getAllRoutes())
				{
					if (((route * 1000) + skillLvL) == _lvl)
					{
						continue;
					}
					// add other levels of all routes - same lvl as enchanted
					// lvl
					_routes.add((route * 1000) + skillLvL);
				}
				
			}
			else
			// not already enchanted
			{
				for (int route : enchantLearn.getAllRoutes())
				{
					// add first level (+1) of all routes
					_routes.add((route * 1000) + 1);
				}
			}
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x2A);
		writeD(_id);
		if (_lvl < 100)
		{
			writeD(_lvl);
		}
		else
		{
			writeH(_maxlvl);
			writeH(_lvl);
		}
		writeD(_maxEnchanted ? 0 : 1);
		writeD(_lvl > 1000 ? 1 : 0); // enchanted?
		writeD(_routes.size());
		
		for (int level : _routes)
		{
			writeH(_maxlvl);
			writeH(level);
		}
	}
}
