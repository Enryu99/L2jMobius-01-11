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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.Skill;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAskJoinMPCC;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) S
 * @author chris_00 D0 0D 00 5A 00 77 00 65 00 72 00 67 00 00 00
 */
public final class RequestExAskJoinMPCC extends GameClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInstance target = World.getInstance().getPlayer(_name);
		if (target == null)
		{
			return;
			// invite yourself? ;)
		}
		
		if (player.isInParty() && target.isInParty() && player.getParty().equals(target.getParty()))
		{
			return;
		}
		
		// activeChar is in a Party?
		if (player.isInParty())
		{
			final Party activeParty = player.getParty();
			// activeChar is PartyLeader? && activeChars Party is already in a CommandChannel?
			if (activeParty.getLeader().equals(player))
			{
				// if activeChars Party is in CC, is activeChar CCLeader?
				if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getChannelLeader().equals(player))
				{
					// in CC and the CCLeader
					// target in a party?
					if (target.isInParty())
					{
						// targets party already in a CChannel?
						if (target.getParty().isInCommandChannel())
						{
							player.sendPacket(new SystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL).addString(target.getName()));
						}
						else
						{
							askJoinMPCC(player, target);
						}
					}
					else
					{
						player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
					}
				}
				else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getChannelLeader().equals(player))
				{
					player.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
				}
				else // target in a party?
				if (target.isInParty())
				{
					// targets party already in a CChannel?
					if (target.getParty().isInCommandChannel())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL).addString(target.getName()));
					}
					else
					{
						askJoinMPCC(player, target);
					}
				}
				else
				{
					player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
			}
		}
	}
	
	private void askJoinMPCC(PlayerInstance requestor, PlayerInstance target)
	{
		boolean hasRight = false;
		if ((requestor.getClan() != null) && (requestor.getClan().getLeaderId() == requestor.getObjectId()) && (requestor.getClan().getLevel() >= 5)) // Clanleader of lvl5 Clan or higher
		{
			hasRight = true;
		}
		else if (requestor.getInventory().getItemByItemId(8871) != null)
		{
			hasRight = true;
		}
		else if (requestor.getPledgeClass() >= 5)
		{
			for (Skill skill : requestor.getAllSkills())
			{
				// Skill Clan Imperium
				if (skill.getId() == 391)
				{
					hasRight = true;
					break;
				}
			}
		}
		
		if (!hasRight)
		{
			requestor.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER);
			return;
		}
		
		final PlayerInstance targetLeader = target.getParty().getLeader();
		if (!targetLeader.isProcessingRequest())
		{
			requestor.onTransactionRequest(targetLeader);
			targetLeader.sendPacket(new SystemMessage(SystemMessageId.COMMAND_CHANNEL_CONFIRM).addString(requestor.getName()));
			targetLeader.sendPacket(new ExAskJoinMPCC(requestor.getName()));
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(targetLeader.getName()));
		}
	}
}
