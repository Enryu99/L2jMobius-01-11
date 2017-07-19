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
package com.l2jmobius.gameserver.network.serverpackets;

import java.util.List;

import javolution.util.FastList;

/**
 * sample a3 05000000 03000000 03000000 06000000 3c000000 00000000 power strike 10000000 02000000 06000000 3c000000 00000000 mortal blow 38000000 04000000 06000000 36010000 00000000 power shot 4d000000 01000000 01000000 98030000 01000000 ATTACK aura 920sp 8e000000 03000000 03000000 cc010000 00000000
 * Armor Mastery format d (ddddd) skillid, level, maxlevel?, C4 format changes: 0000: [8a] [00 00 00 00] [35 00 00 00] 92 00 00 00 01 00 00 .....5.......... ^^^^^^^^^^^^^ 0010: 00 2d 00 00 00 04 01 00 00 00 00 00 00 a4 00 00 .-.............. 0020: 00 01 00 00 00 03 00 00 00 e4 0c 00 00 00 00 00
 * ................ 0030: 00 d4 00 00 00 01 00 00 00 06 00 00 00 08 52 00 ..............R.
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/27 15:29:57 $
 */
public class AquireSkillList extends L2GameServerPacket
{
	// private static Logger _log = Logger.getLogger(AquireSkillList.class.getName());
	
	private static final String _S__A3_AQUIRESKILLLIST = "[S] 8a AquireSkillList";
	private final List<Skill> _skills;
	private final boolean _fishingskills;
	
	private class Skill
	{
		public int _id;
		public int _nextLevel;
		public int _maxLevel;
		public int _spCost;
		public int _requirements;
		
		Skill(int id, int nextLevel, int maxLevel, int spCost, int requirements)
		{
			_id = id;
			_nextLevel = nextLevel;
			_maxLevel = maxLevel;
			_spCost = spCost;
			_requirements = requirements;
		}
	}
	
	public AquireSkillList(boolean FishingSkill)
	{
		_skills = new FastList<>();
		_fishingskills = FishingSkill;
	}
	
	public void addSkill(int id, int nextLevel, int maxLevel, int spCost, int requirements)
	{
		_skills.add(new Skill(id, nextLevel, maxLevel, spCost, requirements));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x8a);
		writeD(_fishingskills ? 1 : 0); // c4
		writeD(_skills.size());
		
		for (final Skill temp : _skills)
		{
			writeD(temp._id);
			writeD(temp._nextLevel);
			writeD(temp._maxLevel);
			writeD(temp._spCost);
			writeD(temp._requirements);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__A3_AQUIRESKILLLIST;
	}
}