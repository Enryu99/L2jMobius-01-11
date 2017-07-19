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
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class ...
 * @version $Revision: 1.9.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class Logout extends L2GameClientPacket
{
	private static final String _C__09_LOGOUT = "[C] 09 Logout";
	private static Logger _log = Logger.getLogger(Logout.class.getName());
	
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
			_log.warning("Player " + player.getName() + " tried to logout during class change.");
			player.sendPacket(new ActionFailed());
			return;
		}
		
		// Don't allow leaving if player is fighting
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player))
		{
			if (Config.DEBUG)
			{
				_log.fine("Player " + player.getName() + " tried to logout while fighting");
			}
			
			player.sendPacket(new SystemMessage(SystemMessage.CANT_LOGOUT_WHILE_FIGHTING));
			player.sendPacket(new ActionFailed());
			return;
		}
		
		if (player.atEvent)
		{
			player.sendMessage("A superior power doesn't allow you to leave the event.");
			player.sendPacket(new ActionFailed());
			return;
		}
		
		// Prevent player from logging out if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is not longer a participant.
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage("You cannot log out while you are a participant in a festival.");
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
		
		if ((Config.OFFLINE_TRADE_ENABLE && ((player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL) || (player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL) || (player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY))) || (Config.OFFLINE_CRAFT_ENABLE && (player.isInCraftMode() || (player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE))))
		{
			player.getInventory().updateDatabase();
			player.closeNetConnection(true);
			if (player.getOfflineStartTime() == 0)
			{
				player.setOfflineStartTime(System.currentTimeMillis());
			}
			return;
		}
		
		player.logout();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__09_LOGOUT;
	}
}