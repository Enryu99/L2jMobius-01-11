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

import java.util.Arrays;
import java.util.List;

import com.l2jmobius.gameserver.datatables.SkillData;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.L2Character;

/**
 * MagicSkillLaunched server packet implementation.
 * @author UnAfraid
 */
public class MagicSkillLaunched extends L2GameServerPacket
{
	private final int _charObjId;
	private final int _skillId;
	private final int _skillLevel;
	private final int _maxLevel;
	private final List<L2Object> _targets;
	
	public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel, L2Object... targets)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_maxLevel = SkillData.getInstance().getMaxLevel(_skillId);
		
		//@formatter:off
		if (targets == null)
		{
			targets = new L2Object[] { cha };
		}
		//@formatter:on
		_targets = Arrays.asList(targets);
	}
	
	public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel)
	{
		this(cha, skillId, skillId, cha);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x54);
		writeD(0x00); // TODO: Find me!
		writeD(_charObjId);
		writeD(_skillId);
		if (_skillLevel < 100)
		{
			writeD(_skillLevel);
		}
		else
		{
			writeH(_maxLevel);
			writeH(_skillLevel);
		}
		writeD(_targets.size());
		for (L2Object target : _targets)
		{
			writeD(target.getObjectId());
		}
	}
}
