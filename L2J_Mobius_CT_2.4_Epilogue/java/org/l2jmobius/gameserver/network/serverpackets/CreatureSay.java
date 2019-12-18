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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.OutgoingPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class CreatureSay implements IClientOutgoingPacket
{
	private final int _objectId;
	private final ChatType _textType;
	private String _charName = null;
	private int _charId = 0;
	private String _text = null;
	private int _msgId = 0;
	private List<String> _parameters;
	
	/**
	 * Used by fake players.
	 * @param sender
	 * @param name
	 * @param messageType
	 * @param text
	 */
	public CreatureSay(Npc sender, String name, ChatType messageType, String text)
	{
		_objectId = sender.getObjectId();
		_textType = messageType;
		_charName = name;
		_text = text;
	}
	
	/**
	 * @param objectId
	 * @param messageType
	 * @param charName
	 * @param text
	 */
	public CreatureSay(int objectId, ChatType messageType, String charName, String text)
	{
		_objectId = objectId;
		_textType = messageType;
		_charName = charName;
		_text = text;
	}
	
	public CreatureSay(int objectId, ChatType messageType, int charId, NpcStringId npcString)
	{
		_objectId = objectId;
		_textType = messageType;
		_charId = charId;
		_text = npcString.getText();
	}
	
	public CreatureSay(int objectId, ChatType messageType, String charName, NpcStringId npcString)
	{
		_objectId = objectId;
		_textType = messageType;
		_charName = charName;
		_text = npcString.getText();
	}
	
	public CreatureSay(int objectId, ChatType messageType, int charId, SystemMessageId sysString)
	{
		_objectId = objectId;
		_textType = messageType;
		_charId = charId;
		_msgId = sysString.getId();
	}
	
	/**
	 * String parameter for argument S1,S2,.. in npcstring-e.dat
	 * @param text
	 */
	public void addStringParameter(String text)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		_parameters.add(text);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.SAY2.writeId(packet);
		packet.writeD(_objectId);
		packet.writeD(_textType.getClientId());
		if (_charName != null)
		{
			packet.writeS(_charName);
		}
		else
		{
			packet.writeD(_charId);
		}
		if (_msgId != 0)
		{
			packet.writeD(_msgId);
		}
		else if (_text != null)
		{
			packet.writeS(_text);
		}
		else if (_parameters != null)
		{
			for (String s : _parameters)
			{
				packet.writeS(s);
			}
		}
		return true;
	}
	
	@Override
	public void runImpl(PlayerInstance player)
	{
		if (player != null)
		{
			player.broadcastSnoop(_textType, _charName, _text);
		}
	}
}
