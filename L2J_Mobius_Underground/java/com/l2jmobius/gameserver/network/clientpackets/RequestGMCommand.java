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
import com.l2jmobius.gameserver.data.sql.impl.ClanTable;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.GMHennaInfo;
import com.l2jmobius.gameserver.network.serverpackets.GMViewCharacterInfo;
import com.l2jmobius.gameserver.network.serverpackets.GMViewItemList;
import com.l2jmobius.gameserver.network.serverpackets.GMViewPledgeInfo;
import com.l2jmobius.gameserver.network.serverpackets.GMViewSkillInfo;
import com.l2jmobius.gameserver.network.serverpackets.GMViewWarehouseWithdrawList;
import com.l2jmobius.gameserver.network.serverpackets.GmViewQuestInfo;

/**
 * This class ...
 * @version $Revision: 1.1.2.2.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestGMCommand implements IClientIncomingPacket
{
	private String _targetName;
	private int _command;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_targetName = packet.readS();
		_command = packet.readD();
		// _unknown = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		// prevent non gm or low level GMs from vieweing player stuff
		if (!client.getActiveChar().isGM() || !client.getActiveChar().getAccessLevel().allowAltG())
		{
			return;
		}
		
		final L2PcInstance player = L2World.getInstance().getPlayer(_targetName);
		
		final L2Clan clan = ClanTable.getInstance().getClanByName(_targetName);
		
		// player name was incorrect?
		if ((player == null) && ((clan == null) || (_command != 6)))
		{
			return;
		}
		
		switch (_command)
		{
			case 1: // player status
			{
				client.sendPacket(new GMViewCharacterInfo(player));
				client.sendPacket(new GMHennaInfo(player));
				break;
			}
			case 2: // player clan
			{
				if ((player != null) && (player.getClan() != null))
				{
					client.sendPacket(new GMViewPledgeInfo(player.getClan(), player));
				}
				break;
			}
			case 3: // player skills
			{
				client.sendPacket(new GMViewSkillInfo(player));
				break;
			}
			case 4: // player quests
			{
				client.sendPacket(new GmViewQuestInfo(player));
				break;
			}
			case 5: // player inventory
			{
				client.sendPacket(new GMViewItemList(player));
				client.sendPacket(new GMHennaInfo(player));
				break;
			}
			case 6: // player warehouse
			{
				// gm warehouse view to be implemented
				if (player != null)
				{
					client.sendPacket(new GMViewWarehouseWithdrawList(player));
					// clan warehouse
				}
				else
				{
					client.sendPacket(new GMViewWarehouseWithdrawList(clan));
				}
				break;
			}
			
		}
	}
}
