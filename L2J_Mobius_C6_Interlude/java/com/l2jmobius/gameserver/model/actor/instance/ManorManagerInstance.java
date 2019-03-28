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

import java.util.List;
import java.util.StringTokenizer;

import com.l2jmobius.gameserver.TradeController;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.datatables.xml.ItemTable;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.CastleManorManager;
import com.l2jmobius.gameserver.instancemanager.CastleManorManager.SeedProduction;
import com.l2jmobius.gameserver.model.StoreTradeList;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.BuyList;
import com.l2jmobius.gameserver.network.serverpackets.BuyListSeed;
import com.l2jmobius.gameserver.network.serverpackets.ExShowCropInfo;
import com.l2jmobius.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import com.l2jmobius.gameserver.network.serverpackets.ExShowProcureCropDetail;
import com.l2jmobius.gameserver.network.serverpackets.ExShowSeedInfo;
import com.l2jmobius.gameserver.network.serverpackets.ExShowSellCropList;
import com.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import com.l2jmobius.gameserver.templates.creatures.NpcTemplate;

public class ManorManagerInstance extends MerchantInstance
{
	public ManorManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onAction(PlayerInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		player.setLastFolkNPC(this);
		
		// Check if the PlayerInstance already target the NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the PlayerInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the PlayerInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			// Send a Server->Client packet ValidateLocation to correct the NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else if (!canInteract(player)) // Calculate the distance between the PlayerInstance and the NpcInstance
		{
			// Notify the PlayerInstance AI with AI_INTENTION_INTERACT
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
		}
		else // If player is a lord of this manor, alternative message from NPC
		if (CastleManorManager.getInstance().isDisabled())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/npcdefault.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcname%", getName());
			player.sendPacket(html);
		}
		else if (!player.isGM() // Player is not GM
			&& (getCastle() != null) && (getCastle().getCastleId() > 0 // Verification of castle
			) && (player.getClan() != null // Player have clan
			) && (getCastle().getOwnerId() == player.getClanId() // Player's clan owning the castle
			) && player.isClanLeader() // Player is clan leader of clan (then he is the lord)
		)
		{
			showMessageWindow(player, "manager-lord.htm");
		}
		else
		{
			showMessageWindow(player, "manager.htm");
		}
		// Send a Server->Client ActionFailed to the PlayerInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showBuyWindow(PlayerInstance player, String val)
	{
		final double taxRate = 0;
		player.tempInvetoryDisable();
		
		StoreTradeList list = TradeController.getInstance().getBuyList(Integer.parseInt(val));
		
		if (list != null)
		{
			list.getItems().get(0).setCount(1);
			final BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else
		{
			LOGGER.info("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2ManorManagerIntance)");
			LOGGER.info("buylist id:" + val);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if ((player.getLastFolkNPC() == null) || (player.getLastFolkNPC().getObjectId() != getObjectId()))
		{
			return;
		}
		
		if (command.startsWith("manor_menu_select"))
		{
			if (CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.sendPacket(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
				return;
			}
			
			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			final int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			final int state = Integer.parseInt(st.nextToken().split("=")[1]);
			final int time = Integer.parseInt(st.nextToken().split("=")[1]);
			
			int castleId;
			if (state == -1)
			{
				castleId = getCastle().getCastleId();
			}
			else
			{
				// info for requested manor
				castleId = state;
			}
			
			switch (ask)
			{
				case 1: // Seed purchase
				{
					if (castleId != getCastle().getCastleId())
					{
						player.sendPacket(SystemMessageId.HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR);
					}
					else
					{
						StoreTradeList tradeList = new StoreTradeList(0);
						List<SeedProduction> seeds = getCastle().getSeedProduction(CastleManorManager.PERIOD_CURRENT);
						for (SeedProduction s : seeds)
						{
							final ItemInstance item = ItemTable.getInstance().createDummyItem(s.getId());
							int price = s.getPrice();
							if (price < (item.getReferencePrice() / 2))
							{
								LOGGER.warning("TradeList " + tradeList.getListId() + " itemId  " + s.getId() + " has an ADENA sell price lower then reference price.. Automatically Updating it..");
								price = item.getReferencePrice();
							}
							item.setPriceToSell(price);
							item.setCount(s.getCanProduce());
							if ((item.getCount() > 0) && (item.getPriceToSell() > 0))
							{
								tradeList.addItem(item);
							}
						}
						BuyListSeed bl = new BuyListSeed(tradeList, castleId, player.getAdena());
						player.sendPacket(bl);
					}
					break;
				}
				case 2: // Crop sales
				{
					player.sendPacket(new ExShowSellCropList(player, castleId, getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT)));
					break;
				}
				case 3: // Current seeds (Manor info)
				{
					if ((time == 1) && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					{
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					}
					else
					{
						player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
					}
					break;
				}
				case 4: // Current crops (Manor info)
				{
					if ((time == 1) && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					{
						player.sendPacket(new ExShowCropInfo(castleId, null));
					}
					else
					{
						player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
					}
					break;
				}
				case 5: // Basic info (Manor info)
				{
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				}
				case 6: // Buy harvester
				{
					showBuyWindow(player, "3" + getNpcId());
					break;
				}
				case 9: // Edit sales (Crop sales)
				{
					player.sendPacket(new ExShowProcureCropDetail(state));
					break;
				}
			}
		}
		else if (command.startsWith("help"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // discard first
			String filename = "manor_client_help00" + st.nextToken() + ".htm";
			showMessageWindow(player, filename);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public String getHtmlPath()
	{
		return "data/html/manormanager/";
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/manormanager/manager.htm"; // Used only in parent method to return from "Territory status" to initial screen.
	}
	
	private void showMessageWindow(PlayerInstance player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(getHtmlPath() + filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
