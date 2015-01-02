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
package com.l2jserver.gameserver.network.serverpackets.mentoring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.l2jserver.gameserver.enums.CategoryType;
import com.l2jserver.gameserver.instancemanager.MentorManager;
import com.l2jserver.gameserver.model.L2Mentee;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author UnAfraid
 */
public class ExMentorList extends L2GameServerPacket
{
	private final int _type;
	private final Collection<L2Mentee> _mentees;
	
	public ExMentorList(L2PcInstance activeChar)
	{
		if (activeChar.isMentor())
		{
			_type = 0x01;
			_mentees = MentorManager.getInstance().getMentees(activeChar.getObjectId());
		}
		else if (activeChar.isMentee())
		{
			_type = 0x02;
			_mentees = Arrays.asList(MentorManager.getInstance().getMentor(activeChar.getObjectId()));
		}
		else if (activeChar.isInCategory(CategoryType.AWAKEN_GROUP)) // Not a mentor, Not a mentee, so can be a mentor
		{
			_mentees = Collections.emptyList();
			_type = 0x01;
		}
		else
		{
			_mentees = Collections.emptyList();
			_type = 0x00;
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x11B);
		writeD(_type);
		writeD(0x00);
		writeD(_mentees.size());
		for (L2Mentee mentee : _mentees)
		{
			writeD(mentee.getObjectId());
			writeS(mentee.getName());
			writeD(mentee.getClassId());
			writeD(mentee.getLevel());
			writeD(mentee.isOnlineInt());
		}
	}
}
