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
package com.l2jmobius.gameserver.model.actor.instance;

import java.util.Map;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.MapRegionTable;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.PcFreight;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.PackageToList;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.WareHouseDepositList;
import com.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;

/**
 * This class ...
 * @version $Revision: 1.3.4.10 $ $Date: 2005/04/06 16:13:41 $
 */
public final class L2WarehouseInstance extends L2FolkInstance
{
	// private static Logger _log = Logger.getLogger(L2WarehouseInstance.class.getName());
	
	/**
	 * @param objectId
	 * @param template
	 */
	public L2WarehouseInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/warehouse/" + pom + ".htm";
	}
	
	private void showRetrieveWindow(L2PcInstance player)
	{
		
		player.sendPacket(new ActionFailed());
		player.setActiveWarehouse(player.getWarehouse());
		
		if (player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.NOTHING_IN_WAREHOUSE));
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.fine("Showing stored items");
		}
		
		player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.Private));
	}
	
	private void showDepositWindow(L2PcInstance player)
	{
		player.sendPacket(new ActionFailed());
		player.setActiveWarehouse(player.getWarehouse());
		player.tempInventoryDisable();
		
		if (Config.DEBUG)
		{
			_log.fine("Showing items to deposit");
		}
		
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.Private));
	}
	
	private void showDepositWindowClan(L2PcInstance player)
	{
		player.sendPacket(new ActionFailed());
		
		if (player.getClan() != null)
		{
			if (player.getClan().getLevel() == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
				return;
			}
			
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.tempInventoryDisable();
			
			if (Config.DEBUG)
			{
				_log.fine("Showing items to deposit - clan");
			}
			
			final WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.Clan);
			player.sendPacket(dl);
			
		}
	}
	
	private void showWithdrawWindowClan(L2PcInstance player)
	{
		player.sendPacket(new ActionFailed());
		if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE));
			return;
		}
		
		if (player.getClan().getLevel() == 0)
		{
			
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
			return;
		}
		
		player.setActiveWarehouse(player.getClan().getWarehouse());
		
		if (Config.DEBUG)
		{
			_log.fine("Showing items to withdraw - clan");
		}
		
		player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.Clan));
		
	}
	
	private void showWithdrawWindowFreight(L2PcInstance player)
	{
		player.sendPacket(new ActionFailed());
		if (Config.DEBUG)
		{
			_log.fine("Showing freightened items");
		}
		
		final PcFreight freight = player.getFreight();
		
		if (freight != null)
		{
			if (freight.getSize() > 0)
			{
				if (!Config.ALT_GAME_FREIGHTS)
				
				{
					final int region = 1 + MapRegionTable.getInstance().getClosestTownNumber(player);
					
					freight.setActiveLocation(region);
					
				}
				
				if (freight.getAvailablePackages() == 0)
				{
					player.sendPacket(new SystemMessage(SystemMessage.PACKAGE_IN_ANOTHER_WAREHOUSE));
					return;
				}
				
				player.setActiveWarehouse(freight);
				player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.Freight));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessage.NO_PACKAGES_ARRIVED));
			}
		}
		else
		{
			if (Config.DEBUG)
			{
				_log.fine("no items freightened");
			}
		}
	}
	
	private void showDepositWindowFreight(L2PcInstance player)
	{
		// No other chars in the account of this player
		if (player.getAccountChars().size() == 0)
		{
			player.sendPacket(new SystemMessage(873));
			
			// One or more chars other than this player for this account
		}
		else
		{
			final Map<Integer, String> chars = player.getAccountChars();
			
			if (chars.size() < 1)
			{
				player.sendPacket(new ActionFailed());
				return;
			}
			
			player.sendPacket(new PackageToList(chars));
			
			if (Config.DEBUG)
			{
				_log.fine("Showing destination chars to freight - char src: " + player.getName());
			}
		}
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// little check to prevent enchant exploit
		if (player.getActiveEnchantItem() != null)
		{
			_log.info("Player " + player.getName() + " trying to use enchant exploit, ban this player!");
			player.logout();
			return;
		}
		
		if (command.startsWith("WithdrawP"))
		{
			showRetrieveWindow(player);
		}
		else if (command.equals("DepositP"))
		{
			showDepositWindow(player);
		}
		else if (command.equals("WithdrawC"))
		{
			showWithdrawWindowClan(player);
		}
		else if (command.equals("DepositC"))
		{
			showDepositWindowClan(player);
		}
		else if (command.startsWith("WithdrawF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				showWithdrawWindowFreight(player);
			}
			
		}
		else if (command.startsWith("DepositF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				showDepositWindowFreight(player);
			}
			
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			
			super.onBypassFeedback(player, command);
		}
	}
}