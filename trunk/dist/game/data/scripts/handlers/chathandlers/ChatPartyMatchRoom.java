/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.chathandlers;

import com.l2jserver.Config;
import com.l2jserver.gameserver.enums.ChatType;
import com.l2jserver.gameserver.handler.IChatHandler;
import com.l2jserver.gameserver.model.PartyMatchRoom;
import com.l2jserver.gameserver.model.PartyMatchRoomList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.CreatureSay;

/**
 * Party Match Room chat handler.
 * @author Gnacik
 */
public class ChatPartyMatchRoom implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.PARTYMATCH_ROOM,
	};
	
	@Override
	public void handleChat(ChatType type, L2PcInstance activeChar, String target, String text)
	{
		if (activeChar.isInPartyMatchRoom())
		{
			final PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
			if (_room != null)
			{
				if (activeChar.isChatBanned() && Config.BAN_CHAT_CHANNELS.contains(type))
				{
					activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_INCREASE_EVEN_FURTHER);
					return;
				}
				
				final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
				for (L2PcInstance _member : _room.getPartyMembers())
				{
					if (Config.FACTION_SYSTEM_ENABLED)
					{
						if (Config.FACTION_SPECIFIC_CHAT)
						{
							if ((activeChar.isGood() && _member.isGood()) || (activeChar.isEvil() && _member.isEvil()))
							{
								_member.sendPacket(cs);
							}
						}
						else
						{
							_member.sendPacket(cs);
						}
					}
					else
					{
						_member.sendPacket(cs);
					}
				}
			}
		}
	}
	
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}