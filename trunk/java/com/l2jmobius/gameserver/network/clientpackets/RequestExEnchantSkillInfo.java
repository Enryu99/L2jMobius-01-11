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
package com.l2jmobius.gameserver.network.clientpackets;

import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.data.xml.impl.EnchantSkillGroupsData;
import com.l2jmobius.gameserver.data.xml.impl.SkillData;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfo;

/**
 * Format (ch) dd c: (id) 0xD0 h: (subid) 0x06 d: skill id d: skill lvl
 * @author -Wooden-
 */
public final class RequestExEnchantSkillInfo implements IClientIncomingPacket
{
	private int _skillId;
	private int _skillLvl;
	private int _fullLvl;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_skillId = packet.readD();
		_fullLvl = packet.readD();
		if (_fullLvl < 100)
		{
			_skillLvl = _fullLvl;
		}
		else
		{
			_skillLvl = _fullLvl >> 16;
		}
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		if ((_skillId <= 0) || (_skillLvl <= 0))
		{
			return;
		}
		
		final L2PcInstance activeChar = client.getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.getLevel() < 76)
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLvl);
		if ((skill == null) || (skill.getId() != _skillId))
		{
			return;
		}
		
		if (EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(_skillId) == null)
		{
			return;
		}
		
		final int playerSkillLvl = activeChar.getSkillLevel(_skillId);
		if ((playerSkillLvl == -1) || (playerSkillLvl != _skillLvl))
		{
			return;
		}
		
		activeChar.sendPacket(new ExEnchantSkillInfo(_skillId, _skillLvl));
	}
}