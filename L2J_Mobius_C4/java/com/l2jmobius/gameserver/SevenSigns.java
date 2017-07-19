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
package com.l2jmobius.gameserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.model.AutoChatHandler;
import com.l2jmobius.gameserver.model.AutoSpawnHandler;
import com.l2jmobius.gameserver.model.AutoSpawnHandler.AutoSpawnInstance;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.SignsSky;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.templates.StatsSet;

import javolution.util.FastMap;

/**
 * Seven Signs Engine TODO: - Implementation of the Seal of Strife for sieges.
 * @author Tempy
 */
public class SevenSigns
{
	protected static Logger _log = Logger.getLogger(SevenSigns.class.getName());
	private static SevenSigns _instance;
	
	// Basic Seven Signs Constants
	public static final String SEVEN_SIGNS_DATA_FILE = "config/signs.properties";
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
	
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;
	
	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;
	
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;
	
	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 00;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	
	// The quest event and seal validation periods last for approximately one week
	// with a 15-minute "interval" period sandwiched between them.
	public static final int PERIOD_MINOR_LENGTH = 900000;
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;
	
	public static final int ANCIENT_ADENA_ID = 5575;
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	public static final int CERTIFICATE_OF_APPROVAL_ID = 6388;
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	public static final int ADENA_JOIN_DAWN_COST = 50000;
	
	// NPC Related Constants \\
	public static final int ORATOR_NPC_ID = 8094;
	public static final int PREACHER_NPC_ID = 8093;
	public static final int MAMMON_MERCHANT_ID = 8113;
	public static final int MAMMON_BLACKSMITH_ID = 8126;
	public static final int MAMMON_MARKETEER_ID = 8092;
	public static final int SPIRIT_IN_ID = 8111;
	public static final int SPIRIT_OUT_ID = 8112;
	public static final int LILITH_NPC_ID = 10283;
	public static final int ANAKIM_NPC_ID = 10286;
	
	public static final int CREST_OF_DAWN_ID = 8170;
	public static final int CREST_OF_DUSK_ID = 8171;
	
	// Seal Stone Related Constants \\
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;
	
	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;
	
	private final Calendar _calendar = Calendar.getInstance();
	
	protected int _activePeriod;
	protected int _currentCycle;
	protected double _dawnStoneScore;
	protected double _duskStoneScore;
	protected int _dawnFestivalScore;
	protected int _duskFestivalScore;
	protected int _compWinner;
	protected int _previousWinner;
	
	protected int _bossId = 0;
	
	private final Map<Integer, StatsSet> _signsPlayerData;
	
	private final Map<Integer, Integer> _signsSealOwners;
	private final Map<Integer, Integer> _signsDuskSealTotals;
	private final Map<Integer, Integer> _signsDawnSealTotals;
	
	private static AutoSpawnInstance _merchantSpawn;
	private static AutoSpawnInstance _blacksmithSpawn;
	private static AutoSpawnInstance _spiritInSpawn;
	private static AutoSpawnInstance _spiritOutSpawn;
	private static AutoSpawnInstance _crestofdawnspawn;
	private static AutoSpawnInstance _crestofduskspawn;
	private static Map<Integer, AutoSpawnInstance> _oratorSpawns;
	private static Map<Integer, AutoSpawnInstance> _preacherSpawns;
	private static Map<Integer, AutoSpawnInstance> _marketeerSpawns;
	
	public SevenSigns()
	{
		_signsPlayerData = new FastMap<>();
		_signsSealOwners = new FastMap<>();
		_signsDuskSealTotals = new FastMap<>();
		_signsDawnSealTotals = new FastMap<>();
		
		try
		{
			restoreSevenSignsData();
		}
		catch (final Exception e)
		{
			_log.severe("SevenSigns: Failed to load configuration: " + e);
		}
		
		_log.info("SevenSigns: Currently in the " + getCurrentPeriodName() + " period!");
		initializeSeals();
		
		if (isSealValidationPeriod())
		{
			if (getCabalHighestScore() == CABAL_NULL)
			{
				_log.info("SevenSigns: The competition ended with a tie last week.");
			}
			else
			{
				_log.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " were victorious last week.");
			}
		}
		else if (getCabalHighestScore() == CABAL_NULL)
		{
			_log.info("SevenSigns: The competition, if the current trend continues, will end in a tie this week.");
		}
		else
		{
			_log.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " are in the lead this week.");
		}
		
		setCalendarForNextPeriodChange();
		final long milliToChange = getMilliToPeriodChange();
		
		// Schedule a time for the next period change.
		final SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
		ThreadPoolManager.getInstance().scheduleGeneral(sspc, milliToChange);
		
		// Thanks to http://rainbow.arch.scriptmania.com/scripts/timezone_countdown.html for help with this.
		final double numSecs = (milliToChange / 1000) % 60;
		double countDown = ((milliToChange / 1000) - numSecs) / 60;
		final int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		final int numHours = (int) Math.floor(countDown % 24);
		final int numDays = (int) Math.floor((countDown - numHours) / 24);
		
		_log.info("SevenSigns: Next period begins in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
	}
	
	/**
	 * Registers all random spawns and auto-chats for Seven Signs NPCs, along with spawns for the Preachers of Doom and Orators of Revelations at the beginning of the Seal Validation period.
	 */
	public void spawnSevenSignsNPC()
	{
		_merchantSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_MERCHANT_ID, false);
		_blacksmithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_BLACKSMITH_ID, false);
		_marketeerSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(MAMMON_MARKETEER_ID);
		_spiritInSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SPIRIT_IN_ID, false);
		_spiritOutSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SPIRIT_OUT_ID, false);
		_crestofdawnspawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(CREST_OF_DAWN_ID, false);
		_crestofduskspawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(CREST_OF_DUSK_ID, false);
		_oratorSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(ORATOR_NPC_ID);
		_preacherSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(PREACHER_NPC_ID);
		
		if (isSealValidationPeriod() || isCompResultsPeriod())
		{
			for (final AutoSpawnInstance spawnInst : _marketeerSpawns.values())
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
			}
			
			if ((getSealOwner(SEAL_GNOSIS) == getCabalHighestScore()) && (getSealOwner(SEAL_GNOSIS) != CABAL_NULL))
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_blacksmithSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_blacksmithSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_blacksmithSpawn, true);
				}
				
				for (final AutoSpawnInstance spawnInst : _oratorSpawns.values())
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
				
				for (final AutoSpawnInstance spawnInst : _preacherSpawns.values())
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
				
				if (!AutoChatHandler.getInstance().getAutoChatInstance(8093, false).isActive() && !AutoChatHandler.getInstance().getAutoChatInstance(8094, false).isActive())
				{
					AutoChatHandler.getInstance().setAutoChatActive(true);
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(_blacksmithSpawn, false);
				
				for (final AutoSpawnInstance spawnInst : _oratorSpawns.values())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
				
				for (final AutoSpawnInstance spawnInst : _preacherSpawns.values())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
				
				AutoChatHandler.getInstance().setAutoChatActive(false);
			}
			
			if ((getSealOwner(SEAL_AVARICE) == getCabalHighestScore()) && (getSealOwner(SEAL_AVARICE) != CABAL_NULL))
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_merchantSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_merchantSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_merchantSpawn, true);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_spiritInSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_spiritInSpawn, true);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_spiritOutSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(_spiritOutSpawn, true);
				}
				
				switch (getCabalHighestScore())
				{
					case CABAL_DAWN:
						// Lilith
						_bossId = LILITH_NPC_ID;
						
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_crestofdawnspawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(_crestofdawnspawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(_crestofduskspawn, false);
						break;
					case CABAL_DUSK:
						// Anakim
						_bossId = ANAKIM_NPC_ID;
						
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(_crestofduskspawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(_crestofduskspawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(_crestofdawnspawn, false);
						break;
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(_merchantSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_crestofdawnspawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_crestofduskspawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_spiritInSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(_spiritOutSpawn, false);
			}
		}
		else
		{
			AutoSpawnHandler.getInstance().setSpawnActive(_merchantSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_blacksmithSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_crestofdawnspawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_crestofduskspawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_spiritInSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(_spiritOutSpawn, false);
			
			for (final AutoSpawnInstance spawnInst : _oratorSpawns.values())
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (final AutoSpawnInstance spawnInst : _preacherSpawns.values())
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (final AutoSpawnInstance spawnInst : _marketeerSpawns.values())
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			AutoChatHandler.getInstance().setAutoChatActive(false);
		}
	}
	
	public static SevenSigns getInstance()
	{
		if (_instance == null)
		{
			_instance = new SevenSigns();
		}
		
		return _instance;
	}
	
	public static int calcContributionScore(int blueCount, int greenCount, int redCount)
	{
		int contrib = blueCount * BLUE_CONTRIB_POINTS;
		contrib += greenCount * GREEN_CONTRIB_POINTS;
		contrib += redCount * RED_CONTRIB_POINTS;
		
		return contrib;
	}
	
	public static int calcAncientAdenaReward(int blueCount, int greenCount, int redCount)
	{
		int reward = blueCount * SEAL_STONE_BLUE_VALUE;
		reward += greenCount * SEAL_STONE_GREEN_VALUE;
		reward += redCount * SEAL_STONE_RED_VALUE;
		
		return reward;
	}
	
	public static final String getCabalShortName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return "dawn";
			case CABAL_DUSK:
				return "dusk";
		}
		
		return "No Cabal";
	}
	
	public static final String getCabalName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return "Lords of Dawn";
			case CABAL_DUSK:
				return "Revolutionaries of Dusk";
		}
		
		return "No Cabal";
	}
	
	public static final String getSealName(int seal, boolean shortName)
	{
		String sealName = (!shortName) ? "Seal of " : "";
		
		switch (seal)
		{
			case SEAL_AVARICE:
				sealName += "Avarice";
				break;
			case SEAL_GNOSIS:
				sealName += "Gnosis";
				break;
			case SEAL_STRIFE:
				sealName += "Strife";
				break;
		}
		
		return sealName;
	}
	
	public final int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public final int getCurrentPeriod()
	{
		return _activePeriod;
	}
	
	private final int getDaysToPeriodChange()
	{
		final int numDays = _calendar.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		
		if (numDays < 0)
		{
			return 0 - numDays;
		}
		
		return 7 - numDays;
	}
	
	public final long getMilliToPeriodChange()
	{
		final long currTimeMillis = System.currentTimeMillis();
		final long changeTimeMillis = _calendar.getTimeInMillis();
		
		return (changeTimeMillis - currTimeMillis);
	}
	
	protected void setCalendarForNextPeriodChange()
	{
		// Calculate the number of days until the next period
		// A period starts at 18:00 pm (local time), like on official servers.
		switch (getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
				int daysToChange = getDaysToPeriodChange();
				
				if (daysToChange == 7)
				{
					if (_calendar.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
					{
						daysToChange = 0;
					}
					else if ((_calendar.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR) && (_calendar.get(Calendar.MINUTE) < PERIOD_START_MINS))
					{
						daysToChange = 0;
					}
				}
				
				// Otherwise...
				if (daysToChange > 0)
				{
					_calendar.add(Calendar.DATE, daysToChange);
				}
				
				_calendar.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_calendar.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
				_calendar.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
		}
	}
	
	public final String getCurrentPeriodName()
	{
		String periodName = null;
		
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				periodName = "Quest Event Initialization";
				break;
			case PERIOD_COMPETITION:
				periodName = "Competition (Quest Event)";
				break;
			case PERIOD_COMP_RESULTS:
				periodName = "Quest Event Results";
				break;
			case PERIOD_SEAL_VALIDATION:
				periodName = "Seal Validation";
				break;
		}
		
		return periodName;
	}
	
	public final boolean isSealValidationPeriod()
	{
		return (_activePeriod == PERIOD_SEAL_VALIDATION);
	}
	
	public final boolean isCompResultsPeriod()
	{
		return (_activePeriod == PERIOD_COMP_RESULTS);
	}
	
	public final int getCurrentScore(int cabal)
	{
		final double totalStoneScore = _dawnStoneScore + _duskStoneScore;
		
		switch (cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return Math.round((float) (_dawnStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _dawnFestivalScore;
			case CABAL_DUSK:
				return Math.round((float) (_duskStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _duskFestivalScore;
		}
		
		return 0;
	}
	
	public final double getCurrentStoneScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return _dawnStoneScore;
			case CABAL_DUSK:
				return _duskStoneScore;
		}
		
		return 0;
	}
	
	public final int getCurrentFestivalScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return _dawnFestivalScore;
			case CABAL_DUSK:
				return _duskFestivalScore;
		}
		
		return 0;
	}
	
	public final int getCabalHighestScore()
	{
		if (getCurrentScore(CABAL_DUSK) == getCurrentScore(CABAL_DAWN))
		{
			return CABAL_NULL;
		}
		else if (getCurrentScore(CABAL_DUSK) > getCurrentScore(CABAL_DAWN))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_DAWN;
		}
	}
	
	public final int getSealOwner(int seal)
	{
		return _signsSealOwners.get(seal);
	}
	
	public final int getSealProportion(int seal, int cabal)
	{
		if (cabal == CABAL_NULL)
		{
			return 0;
		}
		else if (cabal == CABAL_DUSK)
		{
			return _signsDuskSealTotals.get(seal);
		}
		else
		{
			return _signsDawnSealTotals.get(seal);
		}
	}
	
	public final int getTotalMembers(int cabal)
	{
		int cabalMembers = 0;
		final String cabalName = getCabalShortName(cabal);
		
		for (final StatsSet sevenDat : _signsPlayerData.values())
		{
			if (sevenDat.getString("cabal").equals(cabalName))
			{
				cabalMembers++;
			}
		}
		
		return cabalMembers;
	}
	
	public final StatsSet getPlayerData(L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return null;
		}
		
		return _signsPlayerData.get(player.getObjectId());
	}
	
	public int getPlayerStoneContrib(L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		int stoneCount = 0;
		
		final StatsSet currPlayer = getPlayerData(player);
		
		stoneCount += currPlayer.getInteger("red_stones");
		stoneCount += currPlayer.getInteger("green_stones");
		stoneCount += currPlayer.getInteger("blue_stones");
		
		return stoneCount;
	}
	
	public int getPlayerContribScore(L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		final StatsSet currPlayer = getPlayerData(player);
		
		return currPlayer.getInteger("contribution_score");
	}
	
	public int getPlayerAdenaCollect(L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		return _signsPlayerData.get(player.getObjectId()).getInteger("ancient_adena_amount");
	}
	
	public int getPlayerSeal(L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return SEAL_NULL;
		}
		
		return getPlayerData(player).getInteger("seal");
	}
	
	public int getPlayerCabal(L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return CABAL_NULL;
		}
		
		final String playerCabal = getPlayerData(player).getString("cabal");
		
		if (playerCabal.equalsIgnoreCase("dawn"))
		{
			return CABAL_DAWN;
		}
		else if (playerCabal.equalsIgnoreCase("dusk"))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_NULL;
		}
	}
	
	public int getBossId()
	{
		return _bossId;
	}
	
	/**
	 * Restores all Seven Signs data and settings, usually called at server startup.
	 */
	protected void restoreSevenSignsData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT char_obj_id, cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount, contribution_score FROM seven_signs");
				ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int charObjId = rset.getInt("char_obj_id");
					
					final StatsSet sevenDat = new StatsSet();
					sevenDat.set("char_obj_id", charObjId);
					sevenDat.set("cabal", rset.getString("cabal"));
					sevenDat.set("seal", rset.getInt("seal"));
					sevenDat.set("red_stones", rset.getInt("red_stones"));
					sevenDat.set("green_stones", rset.getInt("green_stones"));
					sevenDat.set("blue_stones", rset.getInt("blue_stones"));
					sevenDat.set("ancient_adena_amount", rset.getDouble("ancient_adena_amount"));
					sevenDat.set("contribution_score", rset.getDouble("contribution_score"));
					
					if (Config.DEBUG)
					{
						_log.info("SevenSigns: Loaded data from DB for char ID " + charObjId + " (" + sevenDat.getString("cabal") + ")");
					}
					
					_signsPlayerData.put(charObjId, sevenDat);
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement("SELECT * FROM seven_signs_status WHERE id=0");
				ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					_currentCycle = rset.getInt("current_cycle");
					_activePeriod = rset.getInt("active_period");
					_previousWinner = rset.getInt("previous_winner");
					
					_dawnStoneScore = rset.getDouble("dawn_stone_score");
					_dawnFestivalScore = rset.getInt("dawn_festival_score");
					_duskStoneScore = rset.getDouble("dusk_stone_score");
					_duskFestivalScore = rset.getInt("dusk_festival_score");
					
					_signsSealOwners.put(SEAL_AVARICE, rset.getInt("avarice_owner"));
					_signsSealOwners.put(SEAL_GNOSIS, rset.getInt("gnosis_owner"));
					_signsSealOwners.put(SEAL_STRIFE, rset.getInt("strife_owner"));
					
					_signsDawnSealTotals.put(SEAL_AVARICE, rset.getInt("avarice_dawn_score"));
					_signsDawnSealTotals.put(SEAL_GNOSIS, rset.getInt("gnosis_dawn_score"));
					_signsDawnSealTotals.put(SEAL_STRIFE, rset.getInt("strife_dawn_score"));
					_signsDuskSealTotals.put(SEAL_AVARICE, rset.getInt("avarice_dusk_score"));
					_signsDuskSealTotals.put(SEAL_GNOSIS, rset.getInt("gnosis_dusk_score"));
					_signsDuskSealTotals.put(SEAL_STRIFE, rset.getInt("strife_dusk_score"));
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement("UPDATE seven_signs_status SET date=? WHERE id=0"))
			{
				statement.setInt(1, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
				statement.execute();
			}
		}
		catch (final SQLException e)
		{
			_log.severe("SevenSigns: Unable to load Seven Signs data from database: " + e);
		}
	}
	
	/**
	 * Saves all Seven Signs data, both to the database and properties file (if updateSettings = True). Often called to preserve data integrity and synchronization with DB, in case of errors. <BR>
	 * If player != null, just that player's data is updated in the database, otherwise all player's data is sequentially updated.
	 * @param player
	 * @param updateSettings
	 */
	public void saveSevenSignsData(L2PcInstance player, boolean updateSettings)
	{
		if (Config.DEBUG)
		{
			_log.info("SevenSigns: Saving data to disk.");
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			for (final StatsSet sevenDat : _signsPlayerData.values())
			{
				if (player != null)
				{
					if (sevenDat.getInteger("char_obj_id") != player.getObjectId())
					{
						continue;
					}
				}
				
				try (PreparedStatement statement = con.prepareStatement("UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, green_stones=?, blue_stones=?, ancient_adena_amount=?, contribution_score=? WHERE char_obj_id=?"))
				{
					statement.setString(1, sevenDat.getString("cabal"));
					statement.setInt(2, sevenDat.getInteger("seal"));
					statement.setInt(3, sevenDat.getInteger("red_stones"));
					statement.setInt(4, sevenDat.getInteger("green_stones"));
					statement.setInt(5, sevenDat.getInteger("blue_stones"));
					statement.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
					statement.setDouble(7, sevenDat.getDouble("contribution_score"));
					statement.setInt(8, sevenDat.getInteger("char_obj_id"));
					statement.execute();
				}
				
				if (Config.DEBUG)
				{
					_log.info("SevenSigns: Updated data in database for char ID " + sevenDat.getInteger("char_obj_id") + " (" + sevenDat.getString("cabal") + ")");
				}
			}
			
			if (updateSettings)
			{
				String sqlQuery = "UPDATE seven_signs_status SET current_cycle=?, active_period=?, previous_winner=?, " + "dawn_stone_score=?, dawn_festival_score=?, dusk_stone_score=?, dusk_festival_score=?, " + "avarice_owner=?, gnosis_owner=?, strife_owner=?, avarice_dawn_score=?, gnosis_dawn_score=?, " + "strife_dawn_score=?, avarice_dusk_score=?, gnosis_dusk_score=?, strife_dusk_score=?, " + "festival_cycle=?, ";
				
				for (int i = 0; i < (SevenSignsFestival.FESTIVAL_COUNT); i++)
				{
					sqlQuery += "accumulated_bonus" + String.valueOf(i) + "=?, ";
				}
				
				sqlQuery += "date=? WHERE id=0";
				
				try (PreparedStatement statement = con.prepareStatement(sqlQuery))
				{
					statement.setInt(1, _currentCycle);
					statement.setInt(2, _activePeriod);
					statement.setInt(3, _previousWinner);
					statement.setDouble(4, _dawnStoneScore);
					statement.setInt(5, _dawnFestivalScore);
					statement.setDouble(6, _duskStoneScore);
					statement.setInt(7, _duskFestivalScore);
					statement.setInt(8, _signsSealOwners.get(SEAL_AVARICE));
					statement.setInt(9, _signsSealOwners.get(SEAL_GNOSIS));
					statement.setInt(10, _signsSealOwners.get(SEAL_STRIFE));
					statement.setInt(11, _signsDawnSealTotals.get(SEAL_AVARICE));
					statement.setInt(12, _signsDawnSealTotals.get(SEAL_GNOSIS));
					statement.setInt(13, _signsDawnSealTotals.get(SEAL_STRIFE));
					statement.setInt(14, _signsDuskSealTotals.get(SEAL_AVARICE));
					statement.setInt(15, _signsDuskSealTotals.get(SEAL_GNOSIS));
					statement.setInt(16, _signsDuskSealTotals.get(SEAL_STRIFE));
					statement.setInt(17, SevenSignsFestival.getInstance().getCurrentFestivalCycle());
					
					for (int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
					{
						statement.setInt(18 + i, SevenSignsFestival.getInstance().getAccumulatedBonus(i));
					}
					
					statement.setInt(18 + SevenSignsFestival.FESTIVAL_COUNT, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
					statement.execute();
				}
				
				if (Config.DEBUG)
				{
					_log.info("SevenSigns: Updated data in database.");
				}
			}
		}
		catch (final SQLException e)
		{
			_log.severe("SevenSigns: Unable to save data to database: " + e);
		}
	}
	
	/**
	 * Used to reset the cabal details of all players, and update the database.<BR>
	 * Primarily used when beginning a new cycle, and should otherwise never be called.
	 */
	protected void resetPlayerData()
	{
		if (Config.DEBUG)
		{
			_log.info("SevenSigns: Resetting player data for new event period.");
		}
		
		// Reset each player's contribution data as well as seal and cabal.
		for (final StatsSet sevenDat : _signsPlayerData.values())
		{
			final int charObjId = sevenDat.getInteger("char_obj_id");
			
			// Reset the player's cabal and seal information
			sevenDat.set("cabal", "");
			sevenDat.set("seal", SEAL_NULL);
			sevenDat.set("contribution_score", 0);
			
			_signsPlayerData.put(charObjId, sevenDat);
		}
	}
	
	/**
	 * Tests whether the specified player has joined a cabal in the past.
	 * @param player
	 * @return boolean hasRegistered
	 */
	private boolean hasRegisteredBefore(L2PcInstance player)
	{
		return _signsPlayerData.containsKey(player.getObjectId());
	}
	
	/**
	 * Used to specify cabal-related details for the specified player. This method checks to see if the player has registered before and will update the database if necessary. <BR>
	 * Returns the cabal ID the player has joined.
	 * @param player
	 * @param chosenCabal
	 * @param chosenSeal
	 * @return int cabal
	 */
	public int setPlayerInfo(L2PcInstance player, int chosenCabal, int chosenSeal)
	{
		final int charObjId = player.getObjectId();
		StatsSet currPlayerData = getPlayerData(player);
		
		if (currPlayerData != null)
		{
			// If the seal validation period has passed,
			// cabal information was removed and so "re-register" player
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			
			_signsPlayerData.put(charObjId, currPlayerData);
		}
		else
		{
			currPlayerData = new StatsSet();
			currPlayerData.set("char_obj_id", charObjId);
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			currPlayerData.set("red_stones", 0);
			currPlayerData.set("green_stones", 0);
			currPlayerData.set("blue_stones", 0);
			currPlayerData.set("ancient_adena_amount", 0);
			currPlayerData.set("contribution_score", 0);
			
			_signsPlayerData.put(charObjId, currPlayerData);
			
			// Update data in database, as we have a new player signing up.
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO seven_signs (char_obj_id, cabal, seal) VALUES (?,?,?)"))
			{
				statement.setInt(1, charObjId);
				statement.setString(2, getCabalShortName(chosenCabal));
				statement.setInt(3, chosenSeal);
				statement.execute();
				
				if (Config.DEBUG)
				{
					_log.info("SevenSigns: Inserted data in DB for char ID " + currPlayerData.getInteger("char_obj_id") + " (" + currPlayerData.getString("cabal") + ")");
				}
			}
			catch (final SQLException e)
			{
				_log.severe("SevenSigns: Failed to save data: " + e);
			}
		}
		
		// Increasing Seal total score for the player chosen Seal.
		if (currPlayerData.getString("cabal").equals("dawn"))
		{
			_signsDawnSealTotals.put(chosenSeal, _signsDawnSealTotals.get(chosenSeal) + 1);
		}
		else
		{
			_signsDuskSealTotals.put(chosenSeal, _signsDuskSealTotals.get(chosenSeal) + 1);
		}
		
		saveSevenSignsData(player, true);
		
		if (Config.DEBUG)
		{
			_log.info("SevenSigns: " + player.getName() + " has joined the " + getCabalName(chosenCabal) + " for the " + getSealName(chosenSeal, false) + "!");
		}
		
		return chosenCabal;
	}
	
	/**
	 * Returns the amount of ancient adena the specified player can claim, if any.<BR>
	 * If removeReward = True, all the ancient adena owed to them is removed, then DB is updated.
	 * @param player
	 * @param removeReward
	 * @return int rewardAmount
	 */
	public int getAncientAdenaReward(L2PcInstance player, boolean removeReward)
	{
		final StatsSet currPlayer = getPlayerData(player);
		final int rewardAmount = currPlayer.getInteger("ancient_adena_amount");
		
		currPlayer.set("red_stones", 0);
		currPlayer.set("green_stones", 0);
		currPlayer.set("blue_stones", 0);
		currPlayer.set("ancient_adena_amount", 0);
		
		if (removeReward)
		{
			_signsPlayerData.put(player.getObjectId(), currPlayer);
			saveSevenSignsData(player, true);
		}
		
		return rewardAmount;
	}
	
	/**
	 * Used to add the specified player's seal stone contribution points to the current total for their cabal. Returns the point score the contribution was worth. Each stone count <B>must be</B> broken down and specified by the stone's color.
	 * @param player
	 * @param blueCount
	 * @param greenCount
	 * @param redCount
	 * @return int contribScore
	 */
	public int addPlayerStoneContrib(L2PcInstance player, int blueCount, int greenCount, int redCount)
	{
		final StatsSet currPlayer = getPlayerData(player);
		
		final int contribScore = calcContributionScore(blueCount, greenCount, redCount);
		final int totalAncientAdena = currPlayer.getInteger("ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
		final int totalContribScore = currPlayer.getInteger("contribution_score") + contribScore;
		
		if (totalContribScore > Config.ALT_MAXIMUM_PLAYER_CONTRIB)
		{
			return -1;
		}
		
		currPlayer.set("red_stones", currPlayer.getInteger("red_stones") + redCount);
		currPlayer.set("green_stones", currPlayer.getInteger("green_stones") + greenCount);
		currPlayer.set("blue_stones", currPlayer.getInteger("blue_stones") + blueCount);
		currPlayer.set("ancient_adena_amount", totalAncientAdena);
		currPlayer.set("contribution_score", totalContribScore);
		_signsPlayerData.put(player.getObjectId(), currPlayer);
		
		switch (getPlayerCabal(player))
		{
			case CABAL_DAWN:
				_dawnStoneScore += contribScore;
				break;
			case CABAL_DUSK:
				_duskStoneScore += contribScore;
				break;
		}
		
		saveSevenSignsData(player, true);
		
		if (Config.DEBUG)
		{
			_log.info("SevenSigns: " + player.getName() + " contributed " + contribScore + " seal stone points to their cabal.");
		}
		
		return contribScore;
	}
	
	/**
	 * Adds the specified number of festival points to the specified cabal. Remember, the same number of points are <B>deducted from the rival cabal</B> to maintain proportionality.
	 * @param cabal
	 * @param amount
	 */
	public void addFestivalScore(int cabal, int amount)
	{
		if (cabal == CABAL_DUSK)
		{
			_duskFestivalScore += amount;
			
			// To prevent negative scores!
			if (_dawnFestivalScore >= amount)
			{
				_dawnFestivalScore -= amount;
			}
		}
		else
		{
			_dawnFestivalScore += amount;
			
			if (_duskFestivalScore >= amount)
			{
				_duskFestivalScore -= amount;
			}
		}
	}
	
	/**
	 * Send info on the current Seven Signs period to the specified player.
	 * @param player
	 */
	public void sendCurrentPeriodMsg(L2PcInstance player)
	{
		SystemMessage sm = null;
		
		switch (getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING:
				sm = new SystemMessage(SystemMessage.PREPARATIONS_PERIOD_BEGUN);
				break;
			case PERIOD_COMPETITION:
				sm = new SystemMessage(SystemMessage.COMPETITION_PERIOD_BEGUN);
				break;
			case PERIOD_COMP_RESULTS:
				sm = new SystemMessage(SystemMessage.RESULTS_PERIOD_BEGUN);
				break;
			case PERIOD_SEAL_VALIDATION:
				sm = new SystemMessage(SystemMessage.VALIDATION_PERIOD_BEGUN);
				break;
		}
		
		player.sendPacket(sm);
	}
	
	/**
	 * Sends the built-in system message specified by sysMsgId to all online players.
	 * @param sysMsgId
	 */
	public void sendMessageToAll(int sysMsgId)
	{
		final SystemMessage sm = new SystemMessage(sysMsgId);
		
		for (final L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			player.sendPacket(sm);
		}
	}
	
	/**
	 * Used to initialize the seals for each cabal. (Used at startup or at beginning of a new cycle). This method should be called after <B>resetSeals()</B> and <B>calcNewSealOwners()</B> on a new cycle.
	 */
	protected void initializeSeals()
	{
		for (final Integer currSeal : _signsSealOwners.keySet())
		{
			final int sealOwner = _signsSealOwners.get(currSeal);
			
			if (sealOwner != CABAL_NULL)
			{
				if (isSealValidationPeriod())
				{
					_log.info("SevenSigns: The " + getCabalName(sealOwner) + " has won the " + getSealName(currSeal, false) + ".");
				}
				else
				{
					_log.info("SevenSigns: The " + getSealName(currSeal, false) + " is currently owned by " + getCabalName(sealOwner) + ".");
				}
			}
			else
			{
				_log.info("SevenSigns: The " + getSealName(currSeal, false) + " remains unclaimed.");
			}
		}
	}
	
	/**
	 * Only really used at the beginning of a new cycle, this method resets all seal-related data.
	 */
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}
	
	/**
	 * Calculates the ownership of the three Seals of the Seven Signs, based on various criterion. <BR>
	 * <BR>
	 * Should only ever called at the beginning of a new cycle.
	 */
	protected void calcNewSealOwners()
	{
		if (Config.DEBUG)
		{
			_log.info("SevenSigns: (Avarice) Dawn = " + _signsDawnSealTotals.get(SEAL_AVARICE) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_AVARICE));
			_log.info("SevenSigns: (Gnosis) Dawn = " + _signsDawnSealTotals.get(SEAL_GNOSIS) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_GNOSIS));
			_log.info("SevenSigns: (Strife) Dawn = " + _signsDawnSealTotals.get(SEAL_STRIFE) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_STRIFE));
		}
		
		for (final Integer currSeal : _signsDawnSealTotals.keySet())
		{
			final int prevSealOwner = _signsSealOwners.get(currSeal);
			int newSealOwner = CABAL_NULL;
			final int dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			final int totalDawnMembers = getTotalMembers(CABAL_DAWN) == 0 ? 1 : getTotalMembers(CABAL_DAWN);
			final int dawnPercent = Math.round(((float) dawnProportion / (float) totalDawnMembers) * 100);
			final int duskProportion = getSealProportion(currSeal, CABAL_DUSK);
			final int totalDuskMembers = getTotalMembers(CABAL_DUSK) == 0 ? 1 : getTotalMembers(CABAL_DUSK);
			final int duskPercent = Math.round(((float) duskProportion / (float) totalDuskMembers) * 100);
			
			/*
			 * - If a Seal was already closed or owned by the opponent and the new winner wants to assume ownership of the Seal, 35% or more of the members of the Cabal must have chosen the Seal. If they chose less than 35%, they cannot own the Seal. - If the Seal was owned by the winner in the
			 * previous Seven Signs, they can retain that seal if 10% or more members have chosen it. If they want to possess a new Seal, at least 35% of the members of the Cabal must have chosen the new Seal.
			 */
			switch (prevSealOwner)
			{
				case CABAL_NULL:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							newSealOwner = CABAL_NULL;
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DAWN:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DUSK:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
			}
			
			_signsSealOwners.put(currSeal, newSealOwner);
			
			// Alert all online players to new seal status.
			switch (currSeal)
			{
				case SEAL_AVARICE:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessage.DAWN_OBTAINED_AVARICE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessage.DUSK_OBTAINED_AVARICE);
					}
					break;
				case SEAL_GNOSIS:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessage.DAWN_OBTAINED_GNOSIS);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessage.DUSK_OBTAINED_GNOSIS);
					}
					break;
				case SEAL_STRIFE:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessage.DAWN_OBTAINED_STRIFE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessage.DUSK_OBTAINED_STRIFE);
					}
					
					CastleManager.getInstance().validateTaxes(newSealOwner);
					break;
			}
		}
	}
	
	/**
	 * The primary controller of period change of the Seven Signs system. This runs all related tasks depending on the period that is about to begin.
	 * @author Tempy
	 */
	protected class SevenSignsPeriodChange implements Runnable
	{
		@Override
		public void run()
		{
			/*
			 * Remember the period check here refers to the period just ENDED!
			 */
			final int periodEnded = getCurrentPeriod();
			_activePeriod++;
			
			switch (periodEnded)
			{
				case PERIOD_COMP_RECRUITING: // Initialization
					
					// Start the Festival of Darkness cycle.
					SevenSignsFestival.getInstance().startFestivalManager();
					
					// Send message that Competition has begun.
					sendMessageToAll(SystemMessage.QUEST_EVENT_PERIOD_BEGUN);
					break;
				case PERIOD_COMPETITION: // Results Calculation
					
					// Send message that Competition has ended.
					sendMessageToAll(SystemMessage.QUEST_EVENT_PERIOD_ENDED);
					
					final int compWinner = getCabalHighestScore();
					
					// Schedule a stop of the festival engine.
					SevenSignsFestival.getInstance().getFestivalManagerSchedule().cancel(false);
					
					calcNewSealOwners();
					
					switch (compWinner)
					{
						case CABAL_DAWN:
							sendMessageToAll(SystemMessage.DAWN_WON);
							break;
						case CABAL_DUSK:
							sendMessageToAll(SystemMessage.DUSK_WON);
							break;
					}
					
					_previousWinner = compWinner;
					break;
				case PERIOD_COMP_RESULTS: // Seal Validation
					
					// Perform initial Seal Validation set up.
					initializeSeals();
					
					// Send message that Seal Validation has begun.
					sendMessageToAll(SystemMessage.SEAL_VALIDATION_PERIOD_BEGUN);
					
					_log.info("SevenSigns: The " + getCabalName(_previousWinner) + " has won the competition with " + getCurrentScore(_previousWinner) + " points!");
					break;
				case PERIOD_SEAL_VALIDATION: // Reset for New Cycle
					
					// Ensure a cycle restart when this period ends.
					_activePeriod = PERIOD_COMP_RECRUITING;
					
					// Send message that Seal Validation has ended.
					sendMessageToAll(SystemMessage.SEAL_VALIDATION_PERIOD_ENDED);
					
					// Reset all data
					resetPlayerData();
					resetSeals();
					
					// Reset all Festival-related data and remove any unused blood offerings.
					// NOTE: A full update of Festival data in the database is also performed.
					SevenSignsFestival.getInstance().resetFestivalData(false);
					
					_dawnStoneScore = 0;
					_duskStoneScore = 0;
					
					_dawnFestivalScore = 0;
					_duskFestivalScore = 0;
					
					_currentCycle++;
					break;
			}
			
			// Make sure all Seven Signs data is saved for future use.
			saveSevenSignsData(null, true);
			
			final SignsSky ss = new SignsSky();
			
			for (final L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				player.sendPacket(ss);
			}
			
			spawnSevenSignsNPC();
			
			_log.info("SevenSigns: The " + getCurrentPeriodName() + " period has begun!");
			
			setCalendarForNextPeriodChange();
			
			final SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().scheduleGeneral(sspc, getMilliToPeriodChange());
		}
	}
}