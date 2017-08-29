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
package handlers.chathandlers;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.handler.IChatHandler;
import com.l2jmobius.gameserver.instancemanager.MapRegionManager;
import com.l2jmobius.gameserver.model.BlockList;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.PcCondOverride;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Trade chat handler.
 * @author durgus
 */
public final class ChatTrade implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.TRADE,
	};
	
	@Override
	public void handleChat(ChatType type, L2PcInstance activeChar, String target, String text)
	{
		if (activeChar.isChatBanned() && Config.BAN_CHAT_CHANNELS.contains(type))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_INCREASE_EVEN_FURTHER);
			return;
		}
		
		if (activeChar.getLevel() < 20)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PLAYERS_CAN_USE_TRADE_CHAT_AFTER_LV_S1).addInt(20));
			return;
		}
		
		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		if (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("on") || (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("gm") && activeChar.canOverrideCond(PcCondOverride.CHAT_CONDITIONS)))
		{
			final int region = MapRegionManager.getInstance().getMapRegionLocId(activeChar);
			for (L2PcInstance player : L2World.getInstance().getPlayers())
			{
				if ((region == MapRegionManager.getInstance().getMapRegionLocId(player)) && !BlockList.isBlocked(player, activeChar) && (player.getInstanceId() == activeChar.getInstanceId()))
				{
					if (Config.FACTION_SYSTEM_ENABLED)
					{
						if (Config.FACTION_SPECIFIC_CHAT)
						{
							if ((activeChar.isGood() && player.isGood()) || (activeChar.isEvil() && player.isEvil()))
							{
								player.sendPacket(cs);
							}
						}
						else
						{
							player.sendPacket(cs);
						}
					}
					else
					{
						player.sendPacket(cs);
					}
				}
			}
		}
		else if (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("global"))
		{
			if (!activeChar.canOverrideCond(PcCondOverride.CHAT_CONDITIONS) && !activeChar.getFloodProtectors().getGlobalChat().tryPerformAction("global chat"))
			{
				activeChar.sendMessage("Do not spam trade channel.");
				return;
			}
			
			for (L2PcInstance player : L2World.getInstance().getPlayers())
			{
				if (!BlockList.isBlocked(player, activeChar))
				{
					if (Config.FACTION_SYSTEM_ENABLED)
					{
						if (Config.FACTION_SPECIFIC_CHAT)
						{
							if ((activeChar.isGood() && player.isGood()) || (activeChar.isEvil() && player.isEvil()))
							{
								player.sendPacket(cs);
							}
						}
						else
						{
							player.sendPacket(cs);
						}
					}
					else
					{
						player.sendPacket(cs);
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