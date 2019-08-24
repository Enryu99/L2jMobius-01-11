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
package org.l2jmobius.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.TradeController;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.datatables.SkillTable;
import org.l2jmobius.gameserver.datatables.sql.NpcTable;
import org.l2jmobius.gameserver.datatables.xml.ItemTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.DropCategory;
import org.l2jmobius.gameserver.model.DropData;
import org.l2jmobius.gameserver.model.Skill;
import org.l2jmobius.gameserver.model.StatsSet;
import org.l2jmobius.gameserver.model.StoreTradeList;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.BoxInstance;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;

/**
 * @author terry Window - Preferences - Java - Code Style - Code Templates
 */
public class AdminEditNpc implements IAdminCommandHandler
{
	private static Logger LOGGER = Logger.getLogger(AdminEditChar.class.getName());
	private static final int PAGE_LIMIT = 7;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_edit_npc",
		"admin_save_npc",
		"admin_show_droplist",
		"admin_edit_drop",
		"admin_add_drop",
		"admin_del_drop",
		"admin_showShop",
		"admin_showShopList",
		"admin_addShopItem",
		"admin_delShopItem",
		"admin_box_access",
		"admin_editShopItem",
		"admin_close_window",
		"admin_show_skilllist_npc",
		"admin_add_skill_npc",
		"admin_edit_skill_npc",
		"admin_del_skill_npc",
		"admin_load_npc"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		if (command.startsWith("admin_showShop "))
		{
			String[] args = command.split(" ");
			
			if (args.length > 1)
			{
				showShop(activeChar, Integer.parseInt(command.split(" ")[1]));
			}
		}
		else if (command.startsWith("admin_showShopList "))
		{
			String[] args = command.split(" ");
			if (args.length > 2)
			{
				showShopList(activeChar, Integer.parseInt(command.split(" ")[1]), Integer.parseInt(command.split(" ")[2]));
			}
		}
		else if (command.startsWith("admin_edit_npc ") || command.equals("admin_edit_npc"))
		{
			if (command.startsWith("admin_edit_npc "))
			{
				try
				{
					String[] commandSplit = command.split(" ");
					
					final int npcId = Integer.valueOf(commandSplit[1]);
					
					NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
					Show_Npc_Property(activeChar, npc);
				}
				catch (Exception e)
				{
					BuilderUtil.sendSysMessage(activeChar, "Wrong usage: //edit_npc <npcId>");
				}
			}
			else if (activeChar.getTarget() instanceof NpcInstance)
			{
				final int npcId = Integer.valueOf(((NpcInstance) activeChar.getTarget()).getNpcId());
				
				NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
				Show_Npc_Property(activeChar, npc);
			}
		}
		else if (command.startsWith("admin_load_npc"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			int id = 0;
			try
			{
				id = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //load_npc <id>");
			}
			if (id > 0)
			{
				NpcTable.getInstance().reloadNpc(id);
			}
		}
		else if (command.startsWith("admin_show_droplist "))
		{
			int npcId = 0;
			
			try
			{
				npcId = Integer.parseInt(command.substring(20).trim());
			}
			catch (Exception e)
			{
			}
			
			if (npcId > 0)
			{
				showNpcDropList(activeChar, npcId);
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //show_droplist <npc_id>");
			}
		}
		else if (command.startsWith("admin_addShopItem "))
		{
			String[] args = command.split(" ");
			
			if (args.length > 1)
			{
				addShopItem(activeChar, args);
			}
		}
		else if (command.startsWith("admin_delShopItem "))
		{
			String[] args = command.split(" ");
			
			if (args.length > 2)
			{
				delShopItem(activeChar, args);
			}
		}
		else if (command.startsWith("admin_editShopItem "))
		{
			String[] args = command.split(" ");
			
			if (args.length > 2)
			{
				editShopItem(activeChar, args);
			}
		}
		else if (command.startsWith("admin_save_npc "))
		{
			final String[] commandSplit = command.split(" ");
			if (commandSplit.length >= 4)
			{
				save_npc_property(activeChar, commandSplit);
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //save_npc <npc_id> <npc_stat> <npc_stat_value>");
			}
		}
		else if (command.startsWith("admin_show_skilllist_npc "))
		{
			final StringTokenizer st = new StringTokenizer(command.substring(25), " ");
			try
			{
				int npcId = -1;
				int page = 0;
				if (st.countTokens() <= 2)
				{
					if (st.hasMoreTokens())
					{
						npcId = Integer.parseInt(st.nextToken());
					}
					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}
				}
				
				if (npcId > 0)
				{
					showNpcSkillList(activeChar, npcId, page);
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //show_skilllist_npc <npc_id> <page>");
				}
			}
			catch (Exception e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //show_skilllist_npc <npc_id> <page>");
			}
		}
		else if (command.startsWith("admin_edit_skill_npc "))
		{
			int npcId = -1;
			int skillId = -1;
			try
			{
				final StringTokenizer st = new StringTokenizer(command.substring(21).trim(), " ");
				if (st.countTokens() == 2)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						skillId = Integer.parseInt(st.nextToken());
						showNpcSkillEdit(activeChar, npcId, skillId);
					}
					catch (Exception e)
					{
					}
				}
				else if (st.countTokens() == 3)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						skillId = Integer.parseInt(st.nextToken());
						final int level = Integer.parseInt(st.nextToken());
						
						updateNpcSkillData(activeChar, npcId, skillId, level);
					}
					catch (Exception e)
					{
						LOGGER.warning("admin_edit_skill_npc parements error: " + command);
					}
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //edit_skill_npc <npc_id> <item_id> [<level>]");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //edit_skill_npc <npc_id> <item_id> [<level>]");
			}
		}
		else if (command.startsWith("admin_add_skill_npc "))
		{
			int npcId = -1;
			int skillId = -1;
			try
			{
				final StringTokenizer st = new StringTokenizer(command.substring(20).trim(), " ");
				if (st.countTokens() == 1)
				{
					try
					{
						final String[] input = command.substring(20).split(" ");
						if (input.length < 1)
						{
							return true;
						}
						npcId = Integer.parseInt(input[0]);
					}
					catch (Exception e)
					{
					}
					
					if (npcId > 0)
					{
						final NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
						showNpcSkillAdd(activeChar, npcData);
					}
				}
				else if (st.countTokens() == 3)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						skillId = Integer.parseInt(st.nextToken());
						final int level = Integer.parseInt(st.nextToken());
						
						addNpcSkillData(activeChar, npcId, skillId, level);
					}
					catch (Exception e)
					{
						LOGGER.warning("admin_add_skill_npc parements error: " + command);
					}
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //add_skill_npc <npc_id> [<level>]");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //add_skill_npc <npc_id> [<level>]");
			}
		}
		else if (command.startsWith("admin_del_skill_npc "))
		{
			int npcId = -1;
			int skillId = -1;
			try
			{
				final String[] input = command.substring(20).split(" ");
				if (input.length >= 2)
				{
					npcId = Integer.parseInt(input[0]);
					skillId = Integer.parseInt(input[1]);
				}
			}
			catch (Exception e)
			{
			}
			
			if (npcId > 0)
			{
				deleteNpcSkillData(activeChar, npcId, skillId);
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //del_skill_npc <npc_id> <skill_id>");
			}
		}
		else if (command.startsWith("admin_edit_drop "))
		{
			int npcId = -1;
			int itemId = 0;
			int category = -1000;
			try
			{
				StringTokenizer st = new StringTokenizer(command.substring(16).trim());
				if (st.countTokens() == 3)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						showEditDropData(activeChar, npcId, itemId, category);
					}
					catch (Exception e)
					{
					}
				}
				else if (st.countTokens() == 6)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						final int min = Integer.parseInt(st.nextToken());
						final int max = Integer.parseInt(st.nextToken());
						final int chance = Integer.parseInt(st.nextToken());
						
						updateDropData(activeChar, npcId, itemId, min, max, category, chance);
					}
					catch (Exception e)
					{
						LOGGER.warning("admin_edit_drop parements error: " + command);
					}
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
			}
		}
		else if (command.startsWith("admin_add_drop "))
		{
			int npcId = -1;
			try
			{
				StringTokenizer st = new StringTokenizer(command.substring(15).trim());
				if (st.countTokens() == 1)
				{
					try
					{
						String[] input = command.substring(15).split(" ");
						
						if (input.length < 1)
						{
							return true;
						}
						
						npcId = Integer.parseInt(input[0]);
					}
					catch (Exception e)
					{
					}
					
					if (npcId > 0)
					{
						NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
						showAddDropData(activeChar, npcData);
					}
				}
				else if (st.countTokens() == 6)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						final int itemId = Integer.parseInt(st.nextToken());
						final int category = Integer.parseInt(st.nextToken());
						final int min = Integer.parseInt(st.nextToken());
						final int max = Integer.parseInt(st.nextToken());
						final int chance = Integer.parseInt(st.nextToken());
						
						addDropData(activeChar, npcId, itemId, min, max, category, chance);
					}
					catch (Exception e)
					{
						LOGGER.warning("admin_add_drop parements error: " + command);
					}
				}
				else
				{
					BuilderUtil.sendSysMessage(activeChar, "Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
			}
		}
		else if (command.startsWith("admin_del_drop "))
		{
			int npcId = -1;
			int itemId = -1;
			int category = -1000;
			
			try
			{
				String[] input = command.substring(15).split(" ");
				if (input.length >= 3)
				{
					npcId = Integer.parseInt(input[0]);
					itemId = Integer.parseInt(input[1]);
					category = Integer.parseInt(input[2]);
				}
			}
			catch (Exception e)
			{
			}
			
			if (npcId > 0)
			{
				deleteDropData(activeChar, npcId, itemId, category);
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "Usage: //del_drop <npc_id> <item_id> <category>");
			}
		}
		else if (command.startsWith("admin_box_access"))
		{
			WorldObject target = activeChar.getTarget();
			String[] players = command.split(" ");
			
			if (target instanceof BoxInstance)
			{
				BoxInstance box = (BoxInstance) target;
				
				if (players.length > 1)
				{
					boolean access = true;
					for (int i = 1; i < players.length; i++)
					{
						if (players[i].equals("no"))
						{
							access = false;
							continue;
						}
						box.grantAccess(players[i], access);
					}
				}
				else
				{
					try
					{
						String msg = "Access:";
						
						for (Object p : box.getAccess())
						{
							msg += " " + p;
						}
						
						activeChar.sendMessage(msg);
					}
					catch (Exception e)
					{
						LOGGER.info("box_access: " + e);
					}
				}
			}
		}
		
		return true;
	}
	
	private void editShopItem(PlayerInstance activeChar, String[] args)
	{
		final int tradeListID = Integer.parseInt(args[1]);
		final int itemID = Integer.parseInt(args[2]);
		
		StoreTradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		Item item = ItemTable.getInstance().getTemplate(itemID);
		
		if (tradeList.getPriceForItemId(itemID) < 0)
		{
			return;
		}
		
		if (args.length > 3)
		{
			final int price = Integer.parseInt(args[3]);
			final int order = findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);
			
			tradeList.replaceItem(itemID, Integer.parseInt(args[3]));
			updateTradeList(itemID, price, tradeListID, order);
			
			BuilderUtil.sendSysMessage(activeChar, "Updated price for " + item.getName() + " in Trade List " + tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Edit</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Edit an entry in merchantList.");
		replyMSG.append("<br>Editing Item: " + item.getName());
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td width=100>Edit Field</td><td width=100>Old Value</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>Price</td><td><edit var=\"price\" width=80></td><td>" + tradeList.getPriceForItemId(itemID) + "</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Save\" action=\"bypass -h admin_editShopItem " + tradeListID + " " + itemID + " $price\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID + " 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void delShopItem(PlayerInstance activeChar, String[] args)
	{
		final int tradeListID = Integer.parseInt(args[1]);
		final int itemID = Integer.parseInt(args[2]);
		
		StoreTradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		
		if (tradeList.getPriceForItemId(itemID) < 0)
		{
			return;
		}
		
		if (args.length > 3)
		{
			final int order = findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);
			
			tradeList.removeItem(itemID);
			deleteTradeList(tradeListID, order);
			
			BuilderUtil.sendSysMessage(activeChar, "Deleted " + ItemTable.getInstance().getTemplate(itemID).getName() + " from Trade List " + tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Delete</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Delete entry in merchantList.");
		replyMSG.append("<br>Item to Delete: " + ItemTable.getInstance().getTemplate(itemID).getName());
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td width=100>Value</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>Price</td><td>" + tradeList.getPriceForItemId(itemID) + "</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Confirm\" action=\"bypass -h admin_delShopItem " + tradeListID + " " + itemID + " 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID + " 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void addShopItem(PlayerInstance activeChar, String[] args)
	{
		final int tradeListID = Integer.parseInt(args[1]);
		
		StoreTradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		
		if (tradeList == null)
		{
			BuilderUtil.sendSysMessage(activeChar, "TradeList not found!");
			return;
		}
		
		if (args.length > 3)
		{
			final int order = tradeList.getItems().size() + 1; // last item order + 1
			final int itemID = Integer.parseInt(args[2]);
			int price = Integer.parseInt(args[3]);
			
			ItemInstance newItem = ItemTable.getInstance().createDummyItem(itemID);
			
			if (price < newItem.getReferencePrice())
			{
				LOGGER.warning("TradeList " + tradeList.getListId() + " itemId  " + itemID + " has an ADENA sell price lower then reference price.. Automatically Updating it..");
				price = newItem.getReferencePrice();
			}
			newItem.setPriceToSell(price);
			newItem.setCount(-1);
			tradeList.addItem(newItem);
			storeTradeList(itemID, price, tradeListID, order);
			
			BuilderUtil.sendSysMessage(activeChar, "Added " + newItem.getItem().getName() + " to Trade List " + tradeList.getListId());
			showShopList(activeChar, tradeListID, 1);
			
			return;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Add</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Add a new entry in merchantList.");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td>Edit Field</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>ItemID</td><td><edit var=\"itemID\" width=80></td></tr>");
		replyMSG.append("<tr><td>Price</td><td><edit var=\"price\" width=80></td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Save\" action=\"bypass -h admin_addShopItem " + tradeListID + " $itemID $price\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID + " 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showShopList(PlayerInstance activeChar, int tradeListID, int page)
	{
		StoreTradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		
		if ((page > ((tradeList.getItems().size() / PAGE_LIMIT) + 1)) || (page < 1))
		{
			return;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		StringBuilder html = itemListHtml(tradeList, page);
		
		adminReply.setHtml(html.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private StringBuilder itemListHtml(StoreTradeList tradeList, int page)
	{
		final StringBuilder replyMSG = new StringBuilder();
		
		replyMSG.append("<html><title>Merchant Shop List Page: " + page + "</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Edit, add or delete entries in a merchantList.");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=150>Item Name</td><td width=60>Price</td><td width=40>Delete</td></tr>");
		
		final int start = (page - 1) * PAGE_LIMIT;
		final int end = Math.min((((page - 1) * PAGE_LIMIT) + PAGE_LIMIT) - 1, tradeList.getItems().size() - 1);
		
		for (ItemInstance item : tradeList.getItems(start, end + 1))
		{
			replyMSG.append("<tr><td><a action=\"bypass -h admin_editShopItem " + tradeList.getListId() + " " + item.getItemId() + "\">" + item.getItem().getName() + "</a></td>");
			replyMSG.append("<td>" + item.getPriceToSell() + "</td>");
			replyMSG.append("<td><button value=\"Del\" action=\"bypass -h admin_delShopItem " + tradeList.getListId() + " " + item.getItemId() + "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr>");
		}
		
		replyMSG.append("<tr>");
		
		final int min = 1;
		final int max = (tradeList.getItems().size() / PAGE_LIMIT) + 1;
		
		if (page > 1)
		{
			replyMSG.append("<td><button value=\"Page" + (page - 1) + "\" action=\"bypass -h admin_showShopList " + tradeList.getListId() + " " + (page - 1) + "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		}
		
		if (page < max)
		{
			if (page <= min)
			{
				replyMSG.append("<td></td>");
			}
			
			replyMSG.append("<td><button value=\"Page" + (page + 1) + "\" action=\"bypass -h admin_showShopList " + tradeList.getListId() + " " + (page + 1) + "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		}
		
		replyMSG.append("</tr><tr><td>.</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Add\" action=\"bypass -h admin_addShopItem " + tradeList.getListId() + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");
		
		return replyMSG;
	}
	
	private void showShop(PlayerInstance activeChar, int merchantID)
	{
		List<StoreTradeList> tradeLists = getTradeLists(merchantID);
		
		if (tradeLists == null)
		{
			BuilderUtil.sendSysMessage(activeChar, "Unknown npc template ID" + merchantID);
			return;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		StringBuilder replyMSG = new StringBuilder("<html><title>Merchant Shop Lists</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Select a list to view");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>Mecrchant List ID</td></tr>");
		
		for (StoreTradeList tradeList : tradeLists)
		{
			if (tradeList != null)
			{
				replyMSG.append("<tr><td><a action=\"bypass -h admin_showShopList " + tradeList.getListId() + " 1\">Trade List " + tradeList.getListId() + "</a></td></tr>");
			}
		}
		
		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void storeTradeList(int itemID, int price, int tradeListID, int order)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("INSERT INTO merchant_buylists (`item_id`,`price`,`shop_id`,`order`) values (" + itemID + "," + price + "," + tradeListID + "," + order + ")");
			stmt.execute();
			stmt.close();
		}
		catch (SQLException esql)
		{
			esql.printStackTrace();
		}
	}
	
	private void updateTradeList(int itemID, int price, int tradeListID, int order)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("UPDATE merchant_buylists SET `price`='" + price + "' WHERE `shop_id`='" + tradeListID + "' AND `order`='" + order + "'");
			stmt.execute();
			stmt.close();
		}
		catch (SQLException esql)
		{
			esql.printStackTrace();
		}
	}
	
	private void deleteTradeList(int tradeListID, int order)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("DELETE FROM merchant_buylists WHERE `shop_id`='" + tradeListID + "' AND `order`='" + order + "'");
			stmt.execute();
			stmt.close();
		}
		catch (SQLException esql)
		{
			esql.printStackTrace();
		}
	}
	
	private int findOrderTradeList(int itemID, int price, int tradeListID)
	{
		int order = 0;
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM merchant_buylists WHERE `shop_id`='" + tradeListID + "' AND `item_id` ='" + itemID + "' AND `price` = '" + price + "'");
			ResultSet rs = stmt.executeQuery();
			rs.first();
			
			order = rs.getInt("order");
			
			stmt.close();
			rs.close();
		}
		catch (SQLException esql)
		{
			esql.printStackTrace();
		}
		return order;
	}
	
	private List<StoreTradeList> getTradeLists(int merchantID)
	{
		String target = "npc_%objectId%_Buy";
		
		String content = HtmCache.getInstance().getHtm("data/html/merchant/" + merchantID + ".htm");
		
		if (content == null)
		{
			content = HtmCache.getInstance().getHtm("data/html/merchant/30001.htm");
			
			if (content == null)
			{
				return null;
			}
		}
		
		final List<StoreTradeList> tradeLists = new ArrayList<>();
		String[] lines = content.split("\n");
		
		int pos = 0;
		
		for (String line : lines)
		{
			pos = line.indexOf(target);
			
			if (pos >= 0)
			{
				final int tradeListID = Integer.decode(line.substring(pos + target.length() + 1).split("\"")[0]);
				
				tradeLists.add(TradeController.getInstance().getBuyList(tradeListID));
			}
		}
		
		return tradeLists;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void Show_Npc_Property(PlayerInstance activeChar, NpcTemplate npc)
	{
		if (npc.isCustom())
		{
			BuilderUtil.sendSysMessage(activeChar, "You are going to modify Custom NPC");
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		String content = HtmCache.getInstance().getHtm("data/html/admin/editnpc.htm");
		
		if (content != null)
		{
			adminReply.setHtml(content);
			adminReply.replace("%npcId%", String.valueOf(npc.npcId));
			adminReply.replace("%templateId%", String.valueOf(npc.idTemplate));
			adminReply.replace("%name%", npc.name);
			adminReply.replace("%serverSideName%", npc.serverSideName ? "1" : "0");
			adminReply.replace("%title%", npc.title);
			adminReply.replace("%serverSideTitle%", npc.serverSideTitle ? "1" : "0");
			adminReply.replace("%collisionRadius%", String.valueOf(npc.collisionRadius));
			adminReply.replace("%collisionHeight%", String.valueOf(npc.collisionHeight));
			adminReply.replace("%level%", String.valueOf(npc.level));
			adminReply.replace("%sex%", npc.sex);
			adminReply.replace("%type%", npc.type);
			adminReply.replace("%attackRange%", String.valueOf(npc.baseAtkRange));
			adminReply.replace("%hp%", String.valueOf(npc.baseHpMax));
			adminReply.replace("%mp%", String.valueOf(npc.baseMpMax));
			adminReply.replace("%hpRegen%", String.valueOf(npc.baseHpReg));
			adminReply.replace("%mpRegen%", String.valueOf(npc.baseMpReg));
			adminReply.replace("%str%", String.valueOf(npc.baseSTR));
			adminReply.replace("%con%", String.valueOf(npc.baseCON));
			adminReply.replace("%dex%", String.valueOf(npc.baseDEX));
			adminReply.replace("%int%", String.valueOf(npc.baseINT));
			adminReply.replace("%wit%", String.valueOf(npc.baseWIT));
			adminReply.replace("%men%", String.valueOf(npc.baseMEN));
			adminReply.replace("%exp%", String.valueOf(npc.rewardExp));
			adminReply.replace("%sp%", String.valueOf(npc.rewardSp));
			adminReply.replace("%pAtk%", String.valueOf(npc.basePAtk));
			adminReply.replace("%pDef%", String.valueOf(npc.basePDef));
			adminReply.replace("%mAtk%", String.valueOf(npc.baseMAtk));
			adminReply.replace("%mDef%", String.valueOf(npc.baseMDef));
			adminReply.replace("%pAtkSpd%", String.valueOf(npc.basePAtkSpd));
			adminReply.replace("%aggro%", String.valueOf(npc.aggroRange));
			adminReply.replace("%mAtkSpd%", String.valueOf(npc.baseMAtkSpd));
			adminReply.replace("%rHand%", String.valueOf(npc.rhand));
			adminReply.replace("%lHand%", String.valueOf(npc.lhand));
			adminReply.replace("%armor%", String.valueOf(npc.armor));
			adminReply.replace("%walkSpd%", String.valueOf(npc.baseWalkSpd));
			adminReply.replace("%runSpd%", String.valueOf(npc.baseRunSpd));
			adminReply.replace("%factionId%", npc.factionId == null ? "" : npc.factionId);
			adminReply.replace("%factionRange%", String.valueOf(npc.factionRange));
			adminReply.replace("%isUndead%", npc.isUndead ? "1" : "0");
			adminReply.replace("%absorbLevel%", String.valueOf(npc.absorbLevel));
		}
		else
		{
			adminReply.setHtml("<html><head><body>File not found: data/html/admin/editnpc.htm</body></html>");
		}
		
		activeChar.sendPacket(adminReply);
	}
	
	private void save_npc_property(PlayerInstance activeChar, String[] commandSplit)
	{
		StatsSet newNpcData = new StatsSet();
		
		try
		{
			newNpcData.set("npcId", commandSplit[1]);
			String statToSet = commandSplit[2];
			String value = "";
			
			for (int i = 3; i < commandSplit.length; i++)
			{
				if (i == 3)
				{
					value += commandSplit[i];
				}
				else
				{
					value += " " + commandSplit[i];
				}
			}
			
			switch (statToSet)
			{
				case "templateId":
				{
					newNpcData.set("idTemplate", Integer.valueOf(value));
					break;
				}
				case "name":
				{
					newNpcData.set("name", value);
					break;
				}
				case "serverSideName":
				{
					newNpcData.set("serverSideName", Integer.valueOf(value));
					break;
				}
				case "title":
				{
					newNpcData.set("title", value);
					break;
				}
				case "serverSideTitle":
				{
					newNpcData.set("serverSideTitle", Integer.valueOf(value) == 1 ? 1 : 0);
					break;
				}
				case "collisionRadius":
				{
					newNpcData.set("collision_radius", Integer.valueOf(value));
					break;
				}
				case "collisionHeight":
				{
					newNpcData.set("collision_height", Integer.valueOf(value));
					break;
				}
				case "level":
				{
					newNpcData.set("level", Integer.valueOf(value));
					break;
				}
				case "sex":
				{
					final int intValue = Integer.valueOf(value);
					newNpcData.set("sex", intValue == 0 ? "male" : intValue == 1 ? "female" : "etc");
					break;
				}
				case "type":
				{
					Class.forName("org.l2jmobius.gameserver.model.actor.instance." + value + "Instance");
					newNpcData.set("type", value);
					break;
				}
				case "attackRange":
				{
					newNpcData.set("attackrange", Integer.valueOf(value));
					break;
				}
				case "hp":
				{
					newNpcData.set("hp", Integer.valueOf(value));
					break;
				}
				case "mp":
				{
					newNpcData.set("mp", Integer.valueOf(value));
					break;
				}
				case "hpRegen":
				{
					newNpcData.set("hpreg", Integer.valueOf(value));
					break;
				}
				case "mpRegen":
				{
					newNpcData.set("mpreg", Integer.valueOf(value));
					break;
				}
				case "str":
				{
					newNpcData.set("str", Integer.valueOf(value));
					break;
				}
				case "con":
				{
					newNpcData.set("con", Integer.valueOf(value));
					break;
				}
				case "dex":
				{
					newNpcData.set("dex", Integer.valueOf(value));
					break;
				}
				case "int":
				{
					newNpcData.set("int", Integer.valueOf(value));
					break;
				}
				case "wit":
				{
					newNpcData.set("wit", Integer.valueOf(value));
					break;
				}
				case "men":
				{
					newNpcData.set("men", Integer.valueOf(value));
					break;
				}
				case "exp":
				{
					newNpcData.set("exp", Integer.valueOf(value));
					break;
				}
				case "sp":
				{
					newNpcData.set("sp", Integer.valueOf(value));
					break;
				}
				case "pAtk":
				{
					newNpcData.set("patk", Integer.valueOf(value));
					break;
				}
				case "pDef":
				{
					newNpcData.set("pdef", Integer.valueOf(value));
					break;
				}
				case "mAtk":
				{
					newNpcData.set("matk", Integer.valueOf(value));
					break;
				}
				case "mDef":
				{
					newNpcData.set("mdef", Integer.valueOf(value));
					break;
				}
				case "pAtkSpd":
				{
					newNpcData.set("atkspd", Integer.valueOf(value));
					break;
				}
				case "aggro":
				{
					newNpcData.set("aggro", Integer.valueOf(value));
					break;
				}
				case "mAtkSpd":
				{
					newNpcData.set("matkspd", Integer.valueOf(value));
					break;
				}
				case "rHand":
				{
					newNpcData.set("rhand", Integer.valueOf(value));
					break;
				}
				case "lHand":
				{
					newNpcData.set("lhand", Integer.valueOf(value));
					break;
				}
				case "armor":
				{
					newNpcData.set("armor", Integer.valueOf(value));
					break;
				}
				case "runSpd":
				{
					newNpcData.set("runspd", Integer.valueOf(value));
					break;
				}
				case "factionId":
				{
					newNpcData.set("faction_id", value);
					break;
				}
				case "factionRange":
				{
					newNpcData.set("faction_range", Integer.valueOf(value));
					break;
				}
				case "isUndead":
				{
					newNpcData.set("isUndead", Integer.valueOf(value) == 1 ? 1 : 0);
					break;
				}
				case "absorbLevel":
				{
					final int intVal = Integer.valueOf(value);
					newNpcData.set("absorb_level", intVal < 0 ? 0 : intVal > 12 ? 0 : intVal);
					break;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error saving new npc value: " + e);
		}
		
		final int npcId = newNpcData.getInt("npcId");
		final NpcTemplate old = NpcTable.getInstance().getTemplate(npcId);
		
		if (old.isCustom())
		{
			BuilderUtil.sendSysMessage(activeChar, "You are going to save Custom NPC");
		}
		
		NpcTable.getInstance().saveNpc(newNpcData);
		
		NpcTable.getInstance().reloadNpc(npcId);
		
		Show_Npc_Property(activeChar, NpcTable.getInstance().getTemplate(npcId));
	}
	
	private void showNpcDropList(PlayerInstance activeChar, int npcId)
	{
		NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		
		if (npcData == null)
		{
			BuilderUtil.sendSysMessage(activeChar, "unknown npc template id" + npcId);
			return;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		StringBuilder replyMSG = new StringBuilder("<html><title>NPC: " + npcData.name + "(" + npcData.npcId + ") 's drop manage</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Notes: click[drop_id]to show the detail of drop data,click[del] to delete the drop data!");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>npc_id itemId category</td><td>item[id]</td><td>type</td><td>del</td></tr>");
		Item itemTemplate;
		for (DropCategory cat : npcData.getDropData())
		{
			for (DropData drop : cat.getAllDrops())
			{
				itemTemplate = ItemTable.getInstance().getTemplate(drop.getItemId());
				if (itemTemplate == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Unkown item Id: " + drop.getItemId() + " for NPC: " + npcData.npcId);
					continue;
				}
				replyMSG.append("<tr><td><a action=\"bypass -h admin_edit_drop " + npcData.npcId + " " + drop.getItemId() + " " + cat.getCategoryType() + "\">" + npcData.npcId + " " + drop.getItemId() + " " + cat.getCategoryType() + "</a></td><td>" + itemTemplate.getName() + "[" + drop.getItemId() + "]</td><td>" + (drop.isQuestDrop() ? "Q" : cat.isSweep() ? "S" : "D") + "</td><td><a action=\"bypass -h admin_del_drop " + npcData.npcId + " " + drop.getItemId() + " " + cat.getCategoryType() + "\">del</a></td></tr>");
			}
		}
		
		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Add DropData\" action=\"bypass -h admin_add_drop " + npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showEditDropData(PlayerInstance activeChar, int npcId, int itemId, int category)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT mobId, itemId, min, max, category, chance FROM droplist WHERE mobId=" + npcId + " AND itemId=" + itemId + " AND category=" + category);
			ResultSet dropData = statement.executeQuery();
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			
			StringBuilder replyMSG = new StringBuilder("<html><title>the detail of dropdata: (" + npcId + " " + itemId + " " + category + ")</title>");
			replyMSG.append("<body>");
			
			if (dropData.next())
			{
				replyMSG.append("<table>");
				replyMSG.append("<tr><td>Appertain of NPC</td><td>" + NpcTable.getInstance().getTemplate(dropData.getInt("mobId")).name + "</td></tr>");
				replyMSG.append("<tr><td>ItemName</td><td>" + ItemTable.getInstance().getTemplate(dropData.getInt("itemId")).getName() + "(" + dropData.getInt("itemId") + ")</td></tr>");
				replyMSG.append("<tr><td>Category</td><td>" + (category == -1 ? "sweep" : Integer.toString(category)) + "</td></tr>");
				replyMSG.append("<tr><td>MIN(" + dropData.getInt("min") + ")</td><td><edit var=\"min\" width=80></td></tr>");
				replyMSG.append("<tr><td>MAX(" + dropData.getInt("max") + ")</td><td><edit var=\"max\" width=80></td></tr>");
				replyMSG.append("<tr><td>CHANCE(" + dropData.getInt("chance") + ")</td><td><edit var=\"chance\" width=80></td></tr>");
				replyMSG.append("</table>");
				replyMSG.append("<center>");
				replyMSG.append("<button value=\"Save Modify\" action=\"bypass -h admin_edit_drop " + npcId + " " + itemId + " " + category + " $min $max $chance\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				replyMSG.append("<br><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + dropData.getInt("mobId") + "\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				replyMSG.append("</center>");
			}
			
			dropData.close();
			statement.close();
			
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			
			activeChar.sendPacket(adminReply);
		}
		catch (Exception e)
		{
		}
	}
	
	private void showAddDropData(PlayerInstance activeChar, NpcTemplate npcData)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		StringBuilder replyMSG = new StringBuilder("<html><title>Add dropdata to " + npcData.name + "(" + npcData.npcId + ")</title>");
		replyMSG.append("<body>");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>Item-Id</td><td><edit var=\"itemId\" width=80></td></tr>");
		replyMSG.append("<tr><td>MIN</td><td><edit var=\"min\" width=80></td></tr>");
		replyMSG.append("<tr><td>MAX</td><td><edit var=\"max\" width=80></td></tr>");
		replyMSG.append("<tr><td>CATEGORY(sweep=-1)</td><td><edit var=\"category\" width=80></td></tr>");
		replyMSG.append("<tr><td>CHANCE(0-1000000)</td><td><edit var=\"chance\" width=80></td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"SAVE\" action=\"bypass -h admin_add_drop " + npcData.npcId + " $itemId $category $min $max $chance\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcData.npcId + "\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		
		activeChar.sendPacket(adminReply);
	}
	
	private void updateDropData(PlayerInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE droplist SET min=?, max=?, chance=? WHERE mobId=? AND itemId=? AND category=?");
			statement.setInt(1, min);
			statement.setInt(2, max);
			statement.setInt(3, chance);
			statement.setInt(4, npcId);
			statement.setInt(5, itemId);
			statement.setInt(6, category);
			
			statement.execute();
			statement.close();
			
			PreparedStatement statement2 = con.prepareStatement("SELECT mobId FROM droplist WHERE mobId=? AND itemId=? AND category=?");
			statement2.setInt(1, npcId);
			statement2.setInt(2, itemId);
			statement2.setInt(3, category);
			
			ResultSet npcIdRs = statement2.executeQuery();
			
			if (npcIdRs.next())
			{
				npcId = npcIdRs.getInt("mobId");
			}
			
			npcIdRs.close();
			statement2.close();
			
			if (npcId > 0)
			{
				reLoadNpcDropList(npcId);
				
				NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				StringBuilder replyMSG = new StringBuilder("<html><title>Drop data modify complete!</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");
				
				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "unknown error!");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void addDropData(PlayerInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO droplist(mobId, itemId, min, max, category, chance) values(?,?,?,?,?,?)");
			statement.setInt(1, npcId);
			statement.setInt(2, itemId);
			statement.setInt(3, min);
			statement.setInt(4, max);
			statement.setInt(5, category);
			statement.setInt(6, chance);
			statement.execute();
			statement.close();
			
			reLoadNpcDropList(npcId);
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			StringBuilder replyMSG = new StringBuilder("<html><title>Add drop data complete!</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Continue add\" action=\"bypass -h admin_add_drop " + npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("<br><br><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</center></body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch (Exception e)
		{
		}
	}
	
	private void deleteDropData(PlayerInstance activeChar, int npcId, int itemId, int category)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (npcId > 0)
			{
				PreparedStatement statement2 = con.prepareStatement("DELETE FROM droplist WHERE mobId=? AND itemId=? AND category=?");
				statement2.setInt(1, npcId);
				statement2.setInt(2, itemId);
				statement2.setInt(3, category);
				statement2.execute();
				statement2.close();
				
				reLoadNpcDropList(npcId);
				
				NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				StringBuilder replyMSG = new StringBuilder("<html><title>Delete drop data(" + npcId + ", " + itemId + ", " + category + ")complete</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");
				
				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
		}
		catch (Exception e)
		{
		}
	}
	
	private void reLoadNpcDropList(int npcId)
	{
		NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		
		if (npcData == null)
		{
			return;
		}
		
		// reset the drop lists
		npcData.clearAllDropData();
		
		// get the drops
		try (Connection con = DatabaseFactory.getConnection())
		{
			DropData dropData = null;
			
			npcData.getDropData().clear();
			
			PreparedStatement statement = con.prepareStatement("SELECT mobId,itemId,min,max,category,chance FROM droplist WHERE mobId=?");
			statement.setInt(1, npcId);
			ResultSet dropDataList = statement.executeQuery();
			
			while (dropDataList.next())
			{
				dropData = new DropData();
				
				dropData.setItemId(dropDataList.getInt("itemId"));
				dropData.setMinDrop(dropDataList.getInt("min"));
				dropData.setMaxDrop(dropDataList.getInt("max"));
				dropData.setChance(dropDataList.getInt("chance"));
				
				final int category = dropDataList.getInt("category");
				
				npcData.addDropData(dropData, category);
			}
			dropDataList.close();
			statement.close();
		}
		catch (Exception e)
		{
		}
	}
	
	private void showNpcSkillList(PlayerInstance activeChar, int npcId, int page)
	{
		final NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			BuilderUtil.sendSysMessage(activeChar, "Template id unknown: " + npcId);
			return;
		}
		
		final Map<Integer, Skill> skills = npcData.getSkills();
		
		final int _skillsize = Integer.valueOf(skills.size());
		
		final int MaxSkillsPerPage = 10;
		int MaxPages = _skillsize / MaxSkillsPerPage;
		if (_skillsize > (MaxSkillsPerPage * MaxPages))
		{
			MaxPages++;
		}
		
		if (page > MaxPages)
		{
			page = MaxPages;
		}
		
		final int SkillsStart = MaxSkillsPerPage * page;
		int SkillsEnd = _skillsize;
		if ((SkillsEnd - SkillsStart) > MaxSkillsPerPage)
		{
			SkillsEnd = SkillsStart + MaxSkillsPerPage;
		}
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		final StringBuffer replyMSG = new StringBuffer("");
		replyMSG.append("<html><title>" + npcData.getName() + " Skillist");
		replyMSG.append(" (ID:" + npcData.getNpcId() + "Skills " + Integer.valueOf(_skillsize) + ")</title>");
		replyMSG.append("<body>");
		String pages = "<center><table width=270><tr>";
		for (int x = 0; x < MaxPages; x++)
		{
			final int pagenr = x + 1;
			if (page == x)
			{
				pages += "<td>Page " + pagenr + "</td>";
			}
			else
			{
				pages += "<td><a action=\"bypass -h admin_show_skilllist_npc " + npcData.getNpcId() + " " + x + "\">Page " + pagenr + "</a></td>";
			}
		}
		pages += "</tr></table></center>";
		replyMSG.append(pages);
		
		replyMSG.append("<table width=270>");
		
		final Set<Integer> skillset = skills.keySet();
		final Iterator<Integer> skillite = skillset.iterator();
		Object skillobj = null;
		
		for (int i = 0; i < SkillsStart; i++)
		{
			if (skillite.hasNext())
			{
				skillobj = skillite.next();
			}
		}
		
		int cnt = SkillsStart;
		while (skillite.hasNext())
		{
			cnt++;
			if (cnt > SkillsEnd)
			{
				break;
			}
			skillobj = skillite.next();
			replyMSG.append("<tr><td><a action=\"bypass -h admin_edit_skill_npc " + npcData.getNpcId() + " " + skills.get(skillobj).getId() + "\">" + skills.get(skillobj).getName() + " [" + skills.get(skillobj).getId() + "]</a></td><td>" + skills.get(skillobj).getLevel() + "</td><td><a action=\"bypass -h admin_del_skill_npc " + npcData.getNpcId() + " " + skillobj + "\">Delete</a></td></tr>");
		}
		replyMSG.append("</table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<button value=\"Droplist\" action=\"bypass -h admin_show_droplist " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showNpcSkillEdit(PlayerInstance activeChar, int npcId, int skillId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills WHERE npcid=" + npcId + " AND skillid=" + skillId);
			final ResultSet skillData = statement.executeQuery();
			
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			
			final StringBuffer replyMSG = new StringBuffer("<html><title>(NPC:" + npcId + " SKILL:" + skillId + ")</title>");
			replyMSG.append("<body>");
			
			if (skillData.next())
			{
				final Skill skill = SkillTable.getInstance().getInfo(skillData.getInt("skillid"), skillData.getInt("level"));
				
				replyMSG.append("<table>");
				replyMSG.append("<tr><td>NPC</td><td>" + NpcTable.getInstance().getTemplate(skillData.getInt("npcid")).getName() + "</td></tr>");
				replyMSG.append("<tr><td>SKILL</td><td>" + skill.getName() + "(" + skillData.getInt("skillid") + ")</td></tr>");
				replyMSG.append("<tr><td>Lv(" + skill.getLevel() + ")</td><td><edit var=\"level\" width=50></td></tr>");
				replyMSG.append("</table>");
				
				replyMSG.append("<center>");
				replyMSG.append("<button value=\"Edit Skill\" action=\"bypass -h admin_edit_skill_npc " + npcId + " " + skillId + " $level\"  width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				replyMSG.append("<br><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId + "\"  width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				replyMSG.append("</center>");
			}
			
			skillData.close();
			statement.close();
			
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			
			activeChar.sendPacket(adminReply);
		}
		catch (Exception e)
		{
		}
	}
	
	private void updateNpcSkillData(PlayerInstance activeChar, int npcId, int skillId, int level)
	{
		final Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
		if (skillData == null)
		{
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			final StringBuffer replyMSG = new StringBuffer("<html><title>Update Npc Skill Data</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("</body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("UPDATE npcskills SET level=? WHERE npcid=? AND skillid=?");
			statement.setInt(1, level);
			statement.setInt(2, npcId);
			statement.setInt(3, skillId);
			
			statement.execute();
			statement.close();
			
			if (npcId > 0)
			{
				reLoadNpcSkillList(npcId);
				
				final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				final StringBuffer replyMSG = new StringBuffer("<html><title>Update Npc Skill Data</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");
				
				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
			else
			{
				BuilderUtil.sendSysMessage(activeChar, "Unknown error");
			}
		}
		catch (Exception e)
		{
		}
	}
	
	private void showNpcSkillAdd(PlayerInstance activeChar, NpcTemplate npcData)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		final StringBuffer replyMSG = new StringBuffer("<html><title>Add Skill to " + npcData.getName() + "(ID:" + npcData.getNpcId() + ")</title>");
		replyMSG.append("<body>");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>SkillId</td><td><edit var=\"skillId\" width=80></td></tr>");
		replyMSG.append("<tr><td>Level</td><td><edit var=\"level\" width=80></td></tr>");
		replyMSG.append("</table>");
		
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc " + npcData.getNpcId() + " $skillId $level\"  width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcData.getNpcId() + "\"  width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		
		activeChar.sendPacket(adminReply);
	}
	
	private void addNpcSkillData(PlayerInstance activeChar, int npcId, int skillId, int level)
	{
		// skill check
		final Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
		if (skillData == null)
		{
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			final StringBuffer replyMSG = new StringBuffer("<html><title>Add Skill to Npc</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("</body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("INSERT INTO npcskills(npcid, skillid, level) values(?,?,?)");
			statement.setInt(1, npcId);
			statement.setInt(2, skillId);
			statement.setInt(3, level);
			statement.execute();
			statement.close();
			
			reLoadNpcSkillList(npcId);
			
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			final StringBuffer replyMSG = new StringBuffer("<html><title>Add Skill to Npc (" + npcId + ", " + skillId + ", " + level + ")</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("<br><br><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</center></body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch (Exception e)
		{
		}
	}
	
	private void deleteNpcSkillData(PlayerInstance activeChar, int npcId, int skillId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (npcId > 0)
			{
				final PreparedStatement statement2 = con.prepareStatement("DELETE FROM npcskills WHERE npcid=? AND skillid=?");
				statement2.setInt(1, npcId);
				statement2.setInt(2, skillId);
				statement2.execute();
				statement2.close();
				
				reLoadNpcSkillList(npcId);
				
				final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				final StringBuffer replyMSG = new StringBuffer("<html><title>Delete Skill (" + npcId + ", " + skillId + ")</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");
				
				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
		}
		catch (Exception e)
		{
		}
	}
	
	private void reLoadNpcSkillList(int npcId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
			npcData.getSkills().clear();
			
			Skill skillData = null;
			
			// with out race
			final String _sql = "SELECT npcid, skillid, level FROM npcskills WHERE npcid=? AND (skillid NOT BETWEEN 4290 AND 4302)";
			
			final PreparedStatement statement = con.prepareStatement(_sql);
			statement.setInt(1, npcId);
			final ResultSet skillDataList = statement.executeQuery();
			
			while (skillDataList.next())
			{
				final int idval = skillDataList.getInt("skillid");
				final int levelval = skillDataList.getInt("level");
				skillData = SkillTable.getInstance().getInfo(idval, levelval);
				if (skillData != null)
				{
					npcData.addSkill(skillData);
				}
			}
			skillDataList.close();
			statement.close();
		}
		catch (Exception e)
		{
		}
	}
}
