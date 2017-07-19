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

import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.SevenSignsFestival;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.model.L2Party;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.L2GameClient;
import com.l2jmobius.gameserver.network.L2GameClient.GameClientState;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.CharSelectInfo;
import com.l2jmobius.gameserver.network.serverpackets.RestartResponse;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class ...
 * @version $Revision: 1.11.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestRestart extends L2GameClientPacket
{
	private static final String _C__46_REQUESTRESTART = "[C] 46 RequestRestart";
	private static Logger _log = Logger.getLogger(RequestRestart.class.getName());
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.getActiveEnchantItem() != null)
		{
			player.sendPacket(new ActionFailed());
			return;
		}
		
		if (player.isLocked())
		{
			_log.warning("Player " + player.getName() + " tried to restart during class change.");
			player.sendPacket(new ActionFailed());
			return;
		}
		
		if (player.getPrivateStoreType() != 0)
		{
			player.sendMessage("Cannot restart while trading.");
			player.sendPacket(new ActionFailed());
			return;
		}
		
		if (player.getActiveRequester() != null)
		{
			player.getActiveRequester().onTradeCancel(player);
			player.onTradeCancel(player.getActiveRequester());
		}
		
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player))
		{
			if (Config.DEBUG)
			{
				_log.fine("Player " + player.getName() + " tried to logout while fighting.");
			}
			
			player.sendPacket(new SystemMessage(SystemMessage.CANT_RESTART_WHILE_FIGHTING));
			player.sendPacket(new ActionFailed());
			return;
		}
		
		// Prevent player from restarting if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is no longer a participant.
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage("You cannot restart while being a festival participant.");
				player.sendPacket(new ActionFailed());
				return;
			}
			
			final L2Party playerParty = player.getParty();
			
			if (playerParty != null)
			{
				player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming festival."));
			}
		}
		
		if (player.isFlying())
		{
			player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
		}
		
		// Removing player from the world
		player.deleteMe();
		L2GameClient.saveCharToDisk(player, false);
		
		player.setClient(null);
		getClient().setActiveChar(null);
		
		// return the client to the authed status
		getClient().setState(GameClientState.AUTHED);
		
		final RestartResponse response = new RestartResponse();
		sendPacket(response);
		
		// send char list
		final CharSelectInfo cl = new CharSelectInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__46_REQUESTRESTART;
	}
}