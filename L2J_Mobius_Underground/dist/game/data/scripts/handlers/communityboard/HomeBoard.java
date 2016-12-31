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
package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import com.l2jmobius.Config;
import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.gameserver.cache.HtmCache;
import com.l2jmobius.gameserver.data.sql.impl.ClanTable;
import com.l2jmobius.gameserver.data.xml.impl.BuyListData;
import com.l2jmobius.gameserver.data.xml.impl.MultisellData;
import com.l2jmobius.gameserver.data.xml.impl.SkillData;
import com.l2jmobius.gameserver.handler.CommunityBoardHandler;
import com.l2jmobius.gameserver.handler.IParseBoardHandler;
import com.l2jmobius.gameserver.instancemanager.PremiumManager;
import com.l2jmobius.gameserver.model.actor.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.skills.SkillCaster;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.serverpackets.BuyList;
import com.l2jmobius.gameserver.network.serverpackets.ExBuySellList;
import com.l2jmobius.gameserver.network.serverpackets.ShowBoard;

/**
 * Home board.
 * @author Zoey76, Mobius
 */
public final class HomeBoard implements IParseBoardHandler
{
	// SQL Queries
	private static final String COUNT_FAVORITES = "SELECT COUNT(*) AS favorites FROM `bbs_favorites` WHERE `playerId`=?";
	
	private static final String[] COMMANDS =
	{
		"_bbshome",
		"_bbstop",
		"_bbsmultisell",
		"_bbssell",
		"_bbsteleport",
		"_bbspremium",
		"_bbsbuff",
		"_bbsheal"
	};
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("_bbshome") || command.equals("_bbstop"))
		{
			final String customPath = Config.CUSTOM_CB_ENABLED ? "Custom/" : "";
			CommunityBoardHandler.getInstance().addBypass(activeChar, "Home", command);
			
			String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/" + customPath + "home.html");
			html = html.replaceAll("%fav_count%", Integer.toString(getFavoriteCount(activeChar)));
			html = html.replaceAll("%region_count%", Integer.toString(getRegionCount(activeChar)));
			html = html.replaceAll("%clan_count%", Integer.toString(ClanTable.getInstance().getClanCount()));
			CommunityBoardHandler.separateAndSend(html, activeChar);
		}
		else if (command.startsWith("_bbstop;"))
		{
			final String customPath = Config.CUSTOM_CB_ENABLED ? "Custom/" : "";
			final String path = command.replace("_bbstop;", "");
			if ((path.length() > 0) && path.endsWith(".html"))
			{
				CommunityBoardHandler.separateAndSend(HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/" + customPath + path), activeChar);
			}
		}
		
		// ------------------------
		// Custom Community Board
		// ------------------------
		
		if (Config.CUSTOM_CB_ENABLED)
		{
			if (Config.COMMUNITYBOARD_COMBAT_DISABLED && (activeChar.isInCombat() || activeChar.isInDuel() || activeChar.isInOlympiadMode() || activeChar.isInsideZone(ZoneId.SIEGE) || activeChar.isInsideZone(ZoneId.PVP)))
			{
				activeChar.sendMessage("You can't use the Community Board right now.");
				return false;
			}
			if (Config.COMMUNITYBOARD_KARMA_DISABLED && (activeChar.getReputation() < 0))
			{
				activeChar.sendMessage("Players with Karma cannot use the Community Board.");
				return false;
			}
		}
		else
		{
			return false;
		}
		
		if (Config.COMMUNITYBOARD_ENABLE_MULTISELLS && command.startsWith("_bbsmultisell"))
		{
			final String fullBypass = command.replace("_bbsmultisell;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int multisellId = Integer.parseInt(buypassOptions[0]);
			final String page = buypassOptions[1];
			final String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/Custom/" + page + ".html");
			CommunityBoardHandler.separateAndSend(html, activeChar);
			MultisellData.getInstance().separateAndSend(multisellId, activeChar, null, false);
			return true;
		}
		else if (Config.COMMUNITYBOARD_ENABLE_MULTISELLS && command.startsWith("_bbssell"))
		{
			final String page = command.replace("_bbssell;", "");
			final String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/Custom/" + page + ".html");
			CommunityBoardHandler.separateAndSend(html, activeChar);
			activeChar.sendPacket(new BuyList(BuyListData.getInstance().getBuyList(423), activeChar.getAdena(), 0));
			activeChar.sendPacket(new ExBuySellList(activeChar, false));
			return true;
		}
		else if (Config.COMMUNITYBOARD_ENABLE_TELEPORTS && command.startsWith("_bbsteleport"))
		{
			final String fullBypass = command.replace("_bbsteleport;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int x = Integer.parseInt(buypassOptions[0]);
			final int y = Integer.parseInt(buypassOptions[1]);
			final int z = Integer.parseInt(buypassOptions[2]);
			if (activeChar.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < Config.COMMUNITYBOARD_TELEPORT_PRICE)
			{
				activeChar.sendMessage("Not enough currency!");
			}
			else
			{
				activeChar.sendPacket(new ShowBoard());
				activeChar.destroyItemByItemId("CB_Teleport", Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_TELEPORT_PRICE, activeChar, true);
				activeChar.teleToLocation(x, y, z, 0);
			}
		}
		else if (Config.COMMUNITYBOARD_ENABLE_BUFFS && command.startsWith("_bbsbuff"))
		{
			final String fullBypass = command.replace("_bbsbuff;", "");
			final String[] buypassOptions = fullBypass.split(";");
			final int buffCount = buypassOptions.length - 1;
			final String page = buypassOptions[buffCount];
			if (activeChar.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < (Config.COMMUNITYBOARD_BUFF_PRICE * buffCount))
			{
				activeChar.sendMessage("Not enough currency!");
			}
			else
			{
				activeChar.destroyItemByItemId("CB_Buff", Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_BUFF_PRICE * buffCount, activeChar, true);
				for (int i = 0; i < buffCount; i++)
				{
					final Skill skill = SkillData.getInstance().getSkill(Integer.parseInt(buypassOptions[i].split(",")[0]), Integer.parseInt(buypassOptions[i].split(",")[1]));
					if (Config.COMMUNITYBOARD_CAST_ANIMATIONS)
					{
						SkillCaster.triggerCast(activeChar, activeChar, skill);
						if (activeChar.getServitors().size() > 0)
						{
							for (L2Summon summon : activeChar.getServitors().values())
							{
								SkillCaster.triggerCast(summon, summon, skill);
							}
						}
						if (activeChar.hasPet())
						{
							SkillCaster.triggerCast(activeChar.getPet(), activeChar.getPet(), skill);
						}
					}
					else
					{
						skill.applyEffects(activeChar, activeChar);
						if (activeChar.getServitors().size() > 0)
						{
							for (L2Summon summon : activeChar.getServitors().values())
							{
								skill.applyEffects(summon, summon);
							}
						}
						if (activeChar.hasPet())
						{
							skill.applyEffects(activeChar.getPet(), activeChar.getPet());
						}
					}
				}
			}
			CommunityBoardHandler.separateAndSend(HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/Custom/" + page + ".html"), activeChar);
		}
		else if (Config.COMMUNITYBOARD_ENABLE_HEAL && command.startsWith("_bbsheal"))
		{
			final String page = command.replace("_bbsheal;", "");
			if (activeChar.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < (Config.COMMUNITYBOARD_HEAL_PRICE))
			{
				activeChar.sendMessage("Not enough currency!");
			}
			else
			{
				activeChar.destroyItemByItemId("CB_Heal", Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_HEAL_PRICE, activeChar, true);
				activeChar.setCurrentHp(activeChar.getMaxHp());
				activeChar.setCurrentMp(activeChar.getMaxMp());
				activeChar.setCurrentCp(activeChar.getMaxCp());
				if (activeChar.hasPet())
				{
					activeChar.getPet().setCurrentHp(activeChar.getPet().getMaxHp());
					activeChar.getPet().setCurrentMp(activeChar.getPet().getMaxMp());
					activeChar.getPet().setCurrentCp(activeChar.getPet().getMaxCp());
				}
				for (L2Summon summon : activeChar.getServitors().values())
				{
					summon.setCurrentHp(summon.getMaxHp());
					summon.setCurrentMp(summon.getMaxMp());
					summon.setCurrentCp(summon.getMaxCp());
				}
				activeChar.sendMessage("You used heal!");
			}
			CommunityBoardHandler.separateAndSend(HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/Custom/" + page + ".html"), activeChar);
		}
		else if (Config.PREMIUM_SYSTEM_ENABLED && Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED && command.startsWith("_bbspremium"))
		{
			final String fullBypass = command.replace("_bbspremium;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int premiumDays = Integer.parseInt(buypassOptions[0]);
			if (activeChar.getInventory().getInventoryItemCount(Config.COMMUNITY_PREMIUM_COIN_ID, -1) < (Config.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays))
			{
				activeChar.sendMessage("Not enough currency!");
			}
			else
			{
				activeChar.destroyItemByItemId("CB_Premium", Config.COMMUNITY_PREMIUM_COIN_ID, Config.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays, activeChar, true);
				PremiumManager.getInstance().addPremiumDays(premiumDays, activeChar.getAccountName());
				activeChar.sendMessage("Your account will now have premium status until " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(PremiumManager.getInstance().getPremiumEndDate(activeChar.getAccountName())) + ".");
				CommunityBoardHandler.separateAndSend(HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/Custom/premium/thankyou.html"), activeChar);
			}
		}
		return false;
	}
	
	/**
	 * Gets the Favorite links for the given player.
	 * @param player the player
	 * @return the favorite links count
	 */
	private static int getFavoriteCount(L2PcInstance player)
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(COUNT_FAVORITES))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					count = rs.getInt("favorites");
				}
			}
		}
		catch (Exception e)
		{
			LOG.warning(FavoriteBoard.class.getSimpleName() + ": Coudn't load favorites count for player " + player.getName());
		}
		return count;
	}
	
	/**
	 * Gets the registered regions count for the given player.
	 * @param player the player
	 * @return the registered regions count
	 */
	private static int getRegionCount(L2PcInstance player)
	{
		return 0; // TODO: Implement.
	}
}
