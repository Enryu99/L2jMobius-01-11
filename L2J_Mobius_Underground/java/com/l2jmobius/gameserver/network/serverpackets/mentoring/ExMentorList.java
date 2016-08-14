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
package com.l2jmobius.gameserver.network.serverpackets.mentoring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.enums.CategoryType;
import com.l2jmobius.gameserver.instancemanager.MentorManager;
import com.l2jmobius.gameserver.model.L2Mentee;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;
import com.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;

/**
 * @author UnAfraid
 */
public class ExMentorList implements IClientOutgoingPacket
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
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_MENTOR_LIST.writeId(packet);
		
		packet.writeD(_type);
		packet.writeD(0x00);
		packet.writeD(_mentees.size());
		for (L2Mentee mentee : _mentees)
		{
			packet.writeD(mentee.getObjectId());
			packet.writeS(mentee.getName());
			packet.writeD(mentee.getClassId());
			packet.writeD(mentee.getLevel());
			packet.writeD(mentee.isOnlineInt());
		}
		return true;
	}
}
