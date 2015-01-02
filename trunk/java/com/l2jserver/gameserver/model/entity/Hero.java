/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.ClassListData;
import com.l2jserver.gameserver.datatables.NpcData;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.util.StringUtil;

/**
 * Hero entity.
 * @author godson
 */
public class Hero
{
	private static final Logger _log = Logger.getLogger(Hero.class.getName());
	
	private static final String GET_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId AND heroes.played = 1";
	private static final String GET_ALL_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId";
	private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
	private static final String INSERT_HERO = "INSERT INTO heroes (charId, class_id, count, played, claimed) VALUES (?,?,?,?,?)";
	private static final String UPDATE_HERO = "UPDATE heroes SET count = ?, played = ?, claimed = ? WHERE charId = ?";
	private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.charId = ?";
	// delete hero items
	private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390) AND owner_id NOT IN (SELECT charId FROM characters WHERE accesslevel > 0)";
	
	private static final Map<Integer, StatsSet> _heroes = new FastMap<>();
	private static final Map<Integer, StatsSet> _completeHeroes = new FastMap<>();
	
	private static final Map<Integer, StatsSet> _herocounts = new FastMap<>();
	private static final Map<Integer, List<StatsSet>> _herofights = new FastMap<>();
	
	private static final Map<Integer, List<StatsSet>> _herodiary = new FastMap<>();
	private static final Map<Integer, String> _heroMessage = new FastMap<>();
	
	public static final String COUNT = "count";
	public static final String PLAYED = "played";
	public static final String CLAIMED = "claimed";
	public static final String CLAN_NAME = "clan_name";
	public static final String CLAN_CREST = "clan_crest";
	public static final String ALLY_NAME = "ally_name";
	public static final String ALLY_CREST = "ally_crest";
	
	public static final int ACTION_RAID_KILLED = 1;
	public static final int ACTION_HERO_GAINED = 2;
	public static final int ACTION_CASTLE_TAKEN = 3;
	
	public static Hero getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected Hero()
	{
		init();
	}
	
	private void init()
	{
		_heroes.clear();
		_completeHeroes.clear();
		_herocounts.clear();
		_herofights.clear();
		_herodiary.clear();
		_heroMessage.clear();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement s1 = con.createStatement();
			ResultSet rset = s1.executeQuery(GET_HEROES);
			PreparedStatement ps = con.prepareStatement(GET_CLAN_ALLY);
			Statement s2 = con.createStatement();
			ResultSet rset2 = s2.executeQuery(GET_ALL_HEROES))
		{
			while (rset.next())
			{
				StatsSet hero = new StatsSet();
				int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(CLAIMED, Boolean.parseBoolean(rset.getString(CLAIMED)));
				
				loadFights(charId);
				loadDiary(charId);
				loadMessage(charId);
				
				processHeros(ps, charId, hero);
				
				_heroes.put(charId, hero);
			}
			
			while (rset2.next())
			{
				StatsSet hero = new StatsSet();
				int charId = rset2.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset2.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset2.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset2.getInt(COUNT));
				hero.set(PLAYED, rset2.getInt(PLAYED));
				hero.set(CLAIMED, Boolean.parseBoolean(rset2.getString(CLAIMED)));
				
				processHeros(ps, charId, hero);
				
				_completeHeroes.put(charId, hero);
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Hero System: Couldnt load Heroes", e);
		}
		
		_log.info("Hero System: Loaded " + _heroes.size() + " Heroes.");
		_log.info("Hero System: Loaded " + _completeHeroes.size() + " all time Heroes.");
	}
	
	private void processHeros(PreparedStatement ps, int charId, StatsSet hero) throws SQLException
	{
		ps.setInt(1, charId);
		try (ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				int clanId = rs.getInt("clanid");
				int allyId = rs.getInt("allyId");
				String clanName = "";
				String allyName = "";
				int clanCrest = 0;
				int allyCrest = 0;
				if (clanId > 0)
				{
					clanName = ClanTable.getInstance().getClan(clanId).getName();
					clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
					if (allyId > 0)
					{
						allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
						allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
					}
				}
				hero.set(CLAN_CREST, clanCrest);
				hero.set(CLAN_NAME, clanName);
				hero.set(ALLY_CREST, allyCrest);
				hero.set(ALLY_NAME, allyName);
			}
			ps.clearParameters();
		}
	}
	
	private String calcFightTime(long FightTime)
	{
		String format = String.format("%%0%dd", 2);
		FightTime = FightTime / 1000;
		String seconds = String.format(format, FightTime % 60);
		String minutes = String.format(format, (FightTime % 3600) / 60);
		String time = minutes + ":" + seconds;
		return time;
	}
	
	/**
	 * Restore hero message from Db.
	 * @param charId
	 */
	public void loadMessage(int charId)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT message FROM heroes WHERE charId=?"))
		{
			statement.setInt(1, charId);
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					_heroMessage.put(charId, rset.getString("message"));
				}
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Hero System: Couldnt load Hero Message for CharId: " + charId, e);
		}
	}
	
	public void loadDiary(int charId)
	{
		final List<StatsSet> _diary = new FastList<>();
		int diaryentries = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM  heroes_diary WHERE charId=? ORDER BY time ASC"))
		{
			statement.setInt(1, charId);
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					StatsSet _diaryentry = new StatsSet();
					
					long time = rset.getLong("time");
					int action = rset.getInt("action");
					int param = rset.getInt("param");
					
					String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(time));
					_diaryentry.set("date", date);
					
					if (action == ACTION_RAID_KILLED)
					{
						L2NpcTemplate template = NpcData.getInstance().getTemplate(param);
						if (template != null)
						{
							_diaryentry.set("action", template.getName() + " was defeated");
						}
					}
					else if (action == ACTION_HERO_GAINED)
					{
						_diaryentry.set("action", "Gained Hero status");
					}
					else if (action == ACTION_CASTLE_TAKEN)
					{
						Castle castle = CastleManager.getInstance().getCastleById(param);
						if (castle != null)
						{
							_diaryentry.set("action", castle.getName() + " Castle was successfuly taken");
						}
					}
					_diary.add(_diaryentry);
					diaryentries++;
				}
			}
			_herodiary.put(charId, _diary);
			
			_log.info("Hero System: Loaded " + diaryentries + " diary entries for Hero: " + CharNameTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Hero System: Couldnt load Hero Diary for CharId: " + charId, e);
		}
	}
	
	public void loadFights(int charId)
	{
		final List<StatsSet> _fights = new FastList<>();
		StatsSet _herocountdata = new StatsSet();
		Calendar _data = Calendar.getInstance();
		_data.set(Calendar.DAY_OF_MONTH, 1);
		_data.set(Calendar.HOUR_OF_DAY, 0);
		_data.set(Calendar.MINUTE, 0);
		_data.set(Calendar.MILLISECOND, 0);
		
		long from = _data.getTimeInMillis();
		int numberoffights = 0;
		int _victorys = 0;
		int _losses = 0;
		int _draws = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) AND start<? ORDER BY start ASC"))
		{
			statement.setInt(1, charId);
			statement.setInt(2, charId);
			statement.setLong(3, from);
			try (ResultSet rset = statement.executeQuery())
			{
				int charOneId;
				int charOneClass;
				int charTwoId;
				int charTwoClass;
				int winner;
				long start;
				long time;
				int classed;
				while (rset.next())
				{
					charOneId = rset.getInt("charOneId");
					charOneClass = rset.getInt("charOneClass");
					charTwoId = rset.getInt("charTwoId");
					charTwoClass = rset.getInt("charTwoClass");
					winner = rset.getInt("winner");
					start = rset.getLong("start");
					time = rset.getLong("time");
					classed = rset.getInt("classed");
					
					if (charId == charOneId)
					{
						String name = CharNameTable.getInstance().getNameById(charTwoId);
						String cls = ClassListData.getInstance().getClass(charTwoClass).getClientCode();
						if ((name != null) && (cls != null))
						{
							StatsSet fight = new StatsSet();
							fight.set("oponent", name);
							fight.set("oponentclass", cls);
							
							fight.set("time", calcFightTime(time));
							String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm")).format(new Date(start));
							fight.set("start", date);
							
							fight.set("classed", classed);
							if (winner == 1)
							{
								fight.set("result", "<font color=\"00ff00\">victory</font>");
								_victorys++;
							}
							else if (winner == 2)
							{
								fight.set("result", "<font color=\"ff0000\">loss</font>");
								_losses++;
							}
							else if (winner == 0)
							{
								fight.set("result", "<font color=\"ffff00\">draw</font>");
								_draws++;
							}
							
							_fights.add(fight);
							
							numberoffights++;
						}
					}
					else if (charId == charTwoId)
					{
						String name = CharNameTable.getInstance().getNameById(charOneId);
						String cls = ClassListData.getInstance().getClass(charOneClass).getClientCode();
						if ((name != null) && (cls != null))
						{
							StatsSet fight = new StatsSet();
							fight.set("oponent", name);
							fight.set("oponentclass", cls);
							
							fight.set("time", calcFightTime(time));
							String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm")).format(new Date(start));
							fight.set("start", date);
							
							fight.set("classed", classed);
							if (winner == 1)
							{
								fight.set("result", "<font color=\"ff0000\">loss</font>");
								_losses++;
							}
							else if (winner == 2)
							{
								fight.set("result", "<font color=\"00ff00\">victory</font>");
								_victorys++;
							}
							else if (winner == 0)
							{
								fight.set("result", "<font color=\"ffff00\">draw</font>");
								_draws++;
							}
							
							_fights.add(fight);
							
							numberoffights++;
						}
					}
				}
			}
			
			_herocountdata.set("victory", _victorys);
			_herocountdata.set("draw", _draws);
			_herocountdata.set("loss", _losses);
			
			_herocounts.put(charId, _herocountdata);
			_herofights.put(charId, _fights);
			
			_log.info("Hero System: Loaded " + numberoffights + " fights for Hero: " + CharNameTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Hero System: Couldnt load Hero fights history for CharId: " + charId, e);
		}
	}
	
	public Map<Integer, StatsSet> getHeroes()
	{
		return _heroes;
	}
	
	public int getHeroByClass(int classid)
	{
		for (Entry<Integer, StatsSet> e : _heroes.entrySet())
		{
			if (e.getValue().getInt(Olympiad.CLASS_ID) == classid)
			{
				return e.getKey();
			}
		}
		return 0;
	}
	
	public void resetData()
	{
		_herodiary.clear();
		_herofights.clear();
		_herocounts.clear();
		_heroMessage.clear();
	}
	
	public void showHeroDiary(L2PcInstance activeChar, int heroclass, int charid, int page)
	{
		final int perpage = 10;
		
		if (_herodiary.containsKey(charid))
		{
			List<StatsSet> _mainlist = _herodiary.get(charid);
			final NpcHtmlMessage DiaryReply = new NpcHtmlMessage();
			final String htmContent = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/olympiad/herodiary.htm");
			if ((htmContent != null) && _heroMessage.containsKey(charid))
			{
				DiaryReply.setHtml(htmContent);
				DiaryReply.replace("%heroname%", CharNameTable.getInstance().getNameById(charid));
				DiaryReply.replace("%message%", _heroMessage.get(charid));
				DiaryReply.disableValidation();
				
				if (!_mainlist.isEmpty())
				{
					FastList<StatsSet> _list = FastList.newInstance();
					_list.addAll(_mainlist);
					Collections.reverse(_list);
					
					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					for (int i = ((page - 1) * perpage); i < _list.size(); i++)
					{
						breakat = i;
						StatsSet _diaryentry = _list.get(i);
						StringUtil.append(fList, "<tr><td>");
						if (color)
						{
							StringUtil.append(fList, "<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							StringUtil.append(fList, "<table width=270>");
						}
						StringUtil.append(fList, "<tr><td width=270><font color=\"LEVEL\">" + _diaryentry.getString("date") + ":xx</font></td></tr>");
						StringUtil.append(fList, "<tr><td width=270>" + _diaryentry.getString("action") + "</td></tr>");
						StringUtil.append(fList, "<tr><td>&nbsp;</td></tr></table>");
						StringUtil.append(fList, "</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}
					
					if (breakat < (_list.size() - 1))
					{
						DiaryReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						DiaryReply.replace("%buttprev%", "");
					}
					
					if (page > 1)
					{
						DiaryReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						DiaryReply.replace("%buttnext%", "");
					}
					
					DiaryReply.replace("%list%", fList.toString());
					
					FastList.recycle(_list);
				}
				else
				{
					DiaryReply.replace("%list%", "");
					DiaryReply.replace("%buttprev%", "");
					DiaryReply.replace("%buttnext%", "");
				}
				
				activeChar.sendPacket(DiaryReply);
			}
		}
	}
	
	public void showHeroFights(L2PcInstance activeChar, int heroclass, int charid, int page)
	{
		final int perpage = 20;
		int _win = 0;
		int _loss = 0;
		int _draw = 0;
		
		if (_herofights.containsKey(charid))
		{
			List<StatsSet> _list = _herofights.get(charid);
			
			final NpcHtmlMessage FightReply = new NpcHtmlMessage();
			final String htmContent = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/olympiad/herohistory.htm");
			if (htmContent != null)
			{
				FightReply.setHtml(htmContent);
				FightReply.replace("%heroname%", CharNameTable.getInstance().getNameById(charid));
				
				if (!_list.isEmpty())
				{
					if (_herocounts.containsKey(charid))
					{
						StatsSet _herocount = _herocounts.get(charid);
						_win = _herocount.getInt("victory");
						_loss = _herocount.getInt("loss");
						_draw = _herocount.getInt("draw");
					}
					
					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					for (int i = ((page - 1) * perpage); i < _list.size(); i++)
					{
						breakat = i;
						StatsSet fight = _list.get(i);
						StringUtil.append(fList, "<tr><td>");
						if (color)
						{
							StringUtil.append(fList, "<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							StringUtil.append(fList, "<table width=270>");
						}
						StringUtil.append(fList, "<tr><td width=220><font color=\"LEVEL\">" + fight.getString("start") + "</font>&nbsp;&nbsp;" + fight.getString("result") + "</td><td width=50 align=right>" + (fight.getInt("classed") > 0 ? "<font color=\"FFFF99\">cls</font>" : "<font color=\"999999\">non-cls<font>") + "</td></tr>");
						StringUtil.append(fList, "<tr><td width=220>vs " + fight.getString("oponent") + " (" + fight.getString("oponentclass") + ")</td><td width=50 align=right>(" + fight.getString("time") + ")</td></tr>");
						StringUtil.append(fList, "<tr><td colspan=2>&nbsp;</td></tr></table>");
						StringUtil.append(fList, "</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}
					
					if (breakat < (_list.size() - 1))
					{
						FightReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _match?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						FightReply.replace("%buttprev%", "");
					}
					
					if (page > 1)
					{
						FightReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _match?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						FightReply.replace("%buttnext%", "");
					}
					
					FightReply.replace("%list%", fList.toString());
				}
				else
				{
					FightReply.replace("%list%", "");
					FightReply.replace("%buttprev%", "");
					FightReply.replace("%buttnext%", "");
				}
				
				FightReply.replace("%win%", String.valueOf(_win));
				FightReply.replace("%draw%", String.valueOf(_draw));
				FightReply.replace("%loos%", String.valueOf(_loss));
				
				activeChar.sendPacket(FightReply);
			}
		}
	}
	
	public synchronized void computeNewHeroes(List<StatsSet> newHeroes)
	{
		updateHeroes(true);
		
		for (Integer objectId : _heroes.keySet())
		{
			final L2PcInstance player = L2World.getInstance().getPlayer(objectId);
			if (player == null)
			{
				continue;
			}
			
			player.setHero(false);
			
			for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
			{
				L2ItemInstance equippedItem = player.getInventory().getPaperdollItem(i);
				if ((equippedItem != null) && equippedItem.isHeroItem())
				{
					player.getInventory().unEquipItemInSlot(i);
				}
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance item : player.getInventory().getAvailableItems(false, false, false))
			{
				if ((item != null) && item.isHeroItem())
				{
					player.destroyItem("Hero", item, null, true);
					iu.addRemovedItem(item);
				}
			}
			
			if (!iu.getItems().isEmpty())
			{
				player.sendPacket(iu);
			}
			
			player.broadcastUserInfo();
		}
		
		if (newHeroes.isEmpty())
		{
			_heroes.clear();
			return;
		}
		
		Map<Integer, StatsSet> heroes = new FastMap<>();
		
		for (StatsSet hero : newHeroes)
		{
			int charId = hero.getInt(Olympiad.CHAR_ID);
			
			if ((_completeHeroes != null) && _completeHeroes.containsKey(charId))
			{
				StatsSet oldHero = _completeHeroes.get(charId);
				int count = oldHero.getInt(COUNT);
				oldHero.set(COUNT, count + 1);
				oldHero.set(PLAYED, 1);
				oldHero.set(CLAIMED, false);
				heroes.put(charId, oldHero);
			}
			else
			{
				StatsSet newHero = new StatsSet();
				newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
				newHero.set(Olympiad.CLASS_ID, hero.getInt(Olympiad.CLASS_ID));
				newHero.set(COUNT, 1);
				newHero.set(PLAYED, 1);
				newHero.set(CLAIMED, false);
				heroes.put(charId, newHero);
			}
		}
		
		deleteItemsInDb();
		
		_heroes.clear();
		_heroes.putAll(heroes);
		
		heroes.clear();
		
		updateHeroes(false);
	}
	
	public void updateHeroes(boolean setDefault)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			if (setDefault)
			{
				try (PreparedStatement update_all = con.prepareStatement(UPDATE_ALL))
				{
					update_all.execute();
				}
			}
			else
			{
				StatsSet hero;
				int heroId;
				for (Entry<Integer, StatsSet> entry : _heroes.entrySet())
				{
					hero = entry.getValue();
					heroId = entry.getKey();
					if (_completeHeroes.isEmpty() || !_completeHeroes.containsKey(heroId))
					{
						try (PreparedStatement insert = con.prepareStatement(INSERT_HERO))
						{
							insert.setInt(1, heroId);
							insert.setInt(2, hero.getInt(Olympiad.CLASS_ID));
							insert.setInt(3, hero.getInt(COUNT));
							insert.setInt(4, hero.getInt(PLAYED));
							insert.setString(5, String.valueOf(hero.getBoolean(CLAIMED)));
							insert.execute();
							insert.close();
						}
						
						try (PreparedStatement statement = con.prepareStatement(GET_CLAN_ALLY))
						{
							statement.setInt(1, heroId);
							try (ResultSet rset = statement.executeQuery())
							{
								if (rset.next())
								{
									int clanId = rset.getInt("clanid");
									int allyId = rset.getInt("allyId");
									
									String clanName = "";
									String allyName = "";
									int clanCrest = 0;
									int allyCrest = 0;
									
									if (clanId > 0)
									{
										clanName = ClanTable.getInstance().getClan(clanId).getName();
										clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
										
										if (allyId > 0)
										{
											allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
											allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
										}
									}
									
									hero.set(CLAN_CREST, clanCrest);
									hero.set(CLAN_NAME, clanName);
									hero.set(ALLY_CREST, allyCrest);
									hero.set(ALLY_NAME, allyName);
								}
							}
						}
						_heroes.put(heroId, hero);
						
						_completeHeroes.put(heroId, hero);
					}
					else
					{
						try (PreparedStatement statement = con.prepareStatement(UPDATE_HERO))
						{
							statement.setInt(1, hero.getInt(COUNT));
							statement.setInt(2, hero.getInt(PLAYED));
							statement.setString(3, String.valueOf(hero.getBoolean(CLAIMED)));
							statement.setInt(4, heroId);
							statement.execute();
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Hero System: Couldnt update Heroes", e);
		}
	}
	
	public void setHeroGained(int charId)
	{
		setDiaryData(charId, ACTION_HERO_GAINED, 0);
	}
	
	public void setRBkilled(int charId, int npcId)
	{
		setDiaryData(charId, ACTION_RAID_KILLED, npcId);
		
		L2NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		
		if (_herodiary.containsKey(charId) && (template != null))
		{
			// Get Data
			List<StatsSet> _list = _herodiary.get(charId);
			// Clear old data
			_herodiary.remove(charId);
			// Prepare new data
			StatsSet _diaryentry = new StatsSet();
			String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
			_diaryentry.set("date", date);
			_diaryentry.set("action", template.getName() + " was defeated");
			// Add to old list
			_list.add(_diaryentry);
			// Put new list into diary
			_herodiary.put(charId, _list);
		}
	}
	
	public void setCastleTaken(int charId, int castleId)
	{
		setDiaryData(charId, ACTION_CASTLE_TAKEN, castleId);
		
		Castle castle = CastleManager.getInstance().getCastleById(castleId);
		if ((castle != null) && _herodiary.containsKey(charId))
		{
			// Get Data
			List<StatsSet> _list = _herodiary.get(charId);
			// Clear old data
			_herodiary.remove(charId);
			// Prepare new data
			StatsSet _diaryentry = new StatsSet();
			String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
			_diaryentry.set("date", date);
			_diaryentry.set("action", castle.getName() + " Castle was successfuly taken");
			// Add to old list
			_list.add(_diaryentry);
			// Put new list into diary
			_herodiary.put(charId, _list);
		}
	}
	
	public void setDiaryData(int charId, int action, int param)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO heroes_diary (charId, time, action, param) values(?,?,?,?)"))
		{
			statement.setInt(1, charId);
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, action);
			statement.setInt(4, param);
			statement.execute();
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, "SQL exception while saving DiaryData.", e);
		}
	}
	
	/**
	 * Set new hero message for hero
	 * @param player the player instance
	 * @param message String to set
	 */
	public void setHeroMessage(L2PcInstance player, String message)
	{
		_heroMessage.put(player.getObjectId(), message);
	}
	
	/**
	 * Update hero message in database
	 * @param charId character objid
	 */
	public void saveHeroMessage(int charId)
	{
		if (_heroMessage.get(charId) == null)
		{
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE heroes SET message=? WHERE charId=?;"))
		{
			statement.setString(1, _heroMessage.get(charId));
			statement.setInt(2, charId);
			statement.execute();
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, "SQL exception while saving HeroMessage.", e);
		}
	}
	
	private void deleteItemsInDb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_ITEMS))
		{
			statement.execute();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * Saving task for {@link Hero}<BR>
	 * Save all hero messages to DB.
	 */
	public void shutdown()
	{
		for (int charId : _heroMessage.keySet())
		{
			saveHeroMessage(charId);
		}
	}
	
	/**
	 * Verifies if the given object ID belongs to a claimed hero.
	 * @param objectId the player's object ID to verify
	 * @return {@code true} if there are heros and the player is in the list, {@code false} otherwise
	 */
	public boolean isHero(int objectId)
	{
		return _heroes.containsKey(objectId) && _heroes.get(objectId).getBoolean(CLAIMED);
	}
	
	/**
	 * Verifies if the given object ID belongs to an unclaimed hero.
	 * @param objectId the player's object ID to verify
	 * @return {@code true} if player is unclaimed hero
	 */
	public boolean isUnclaimedHero(int objectId)
	{
		return _heroes.containsKey(objectId) && !_heroes.get(objectId).getBoolean(CLAIMED);
	}
	
	/**
	 * Claims the hero status for the given player.
	 * @param player the player to become hero
	 */
	public void claimHero(L2PcInstance player)
	{
		StatsSet hero = _heroes.get(player.getObjectId());
		if (hero == null)
		{
			hero = new StatsSet();
			_heroes.put(player.getObjectId(), hero);
		}
		
		hero.set(CLAIMED, true);
		
		final L2Clan clan = player.getClan();
		if ((clan != null) && (clan.getLevel() >= 5))
		{
			clan.addReputationScore(Config.HERO_POINTS, true);
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_C1_WAS_NAMED_A_HERO_S2_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION);
			sm.addString(CharNameTable.getInstance().getNameById(player.getObjectId()));
			sm.addInt(Config.HERO_POINTS);
			clan.broadcastToOnlineMembers(sm);
		}
		
		player.setHero(true);
		player.broadcastPacket(new SocialAction(player.getObjectId(), 20016)); // Hero Animation
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
		player.broadcastUserInfo();
		// Set Gained hero and reload data
		setHeroGained(player.getObjectId());
		loadFights(player.getObjectId());
		loadDiary(player.getObjectId());
		_heroMessage.put(player.getObjectId(), "");
		
		updateHeroes(false);
	}
	
	private static class SingletonHolder
	{
		protected static final Hero _instance = new Hero();
	}
}
