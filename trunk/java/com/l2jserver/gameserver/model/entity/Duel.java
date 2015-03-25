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

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.enums.DuelResult;
import com.l2jserver.gameserver.enums.Team;
import com.l2jserver.gameserver.instancemanager.DuelManager;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ExDuelEnd;
import com.l2jserver.gameserver.network.serverpackets.ExDuelReady;
import com.l2jserver.gameserver.network.serverpackets.ExDuelStart;
import com.l2jserver.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.PlaySound;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

public class Duel
{
	protected static final Logger _log = Logger.getLogger(Duel.class.getName());
	
	public static final int DUELSTATE_NODUEL = 0;
	public static final int DUELSTATE_DUELLING = 1;
	public static final int DUELSTATE_DEAD = 2;
	public static final int DUELSTATE_WINNER = 3;
	public static final int DUELSTATE_INTERRUPTED = 4;
	
	private static final PlaySound B04_S01 = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
	
	private final int _duelId;
	private L2PcInstance _playerA;
	private L2PcInstance _playerB;
	private final boolean _partyDuel;
	private final Calendar _duelEndTime;
	private int _surrenderRequest = 0;
	private int _countdown = 4;
	private boolean _finished = false;
	
	private final List<PlayerCondition> _playerConditions = new CopyOnWriteArrayList<>();
	
	public Duel(L2PcInstance playerA, L2PcInstance playerB, int partyDuel, int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = partyDuel == 1 ? true : false;
		
		_duelEndTime = Calendar.getInstance();
		if (_partyDuel)
		{
			_duelEndTime.add(Calendar.SECOND, 300);
		}
		else
		{
			_duelEndTime.add(Calendar.SECOND, 120);
		}
		
		setFinished(false);
		
		if (_partyDuel)
		{
			// increase countdown so that start task can teleport players
			_countdown++;
			// inform players that they will be ported shortly
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
		}
		// Schedule duel start
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartDuelTask(this), 3000);
	}
	
	public static class PlayerCondition
	{
		private L2PcInstance _player;
		private double _hp;
		private double _mp;
		private double _cp;
		private boolean _paDuel;
		private int _x, _y, _z;
		private List<Skill> _debuffs;
		
		public PlayerCondition(L2PcInstance player, boolean partyDuel)
		{
			if (player == null)
			{
				return;
			}
			_player = player;
			_hp = _player.getCurrentHp();
			_mp = _player.getCurrentMp();
			_cp = _player.getCurrentCp();
			_paDuel = partyDuel;
			
			if (_paDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}
		
		public void restoreCondition()
		{
			if (_player == null)
			{
				return;
			}
			_player.setCurrentHp(_hp);
			_player.setCurrentMp(_mp);
			_player.setCurrentCp(_cp);
			
			if (_paDuel)
			{
				teleportBack();
			}
			if (_debuffs != null) // Debuff removal
			{
				for (Skill skill : _debuffs)
				{
					if (skill != null)
					{
						_player.stopSkillEffects(true, skill.getId());
					}
				}
			}
		}
		
		public void registerDebuff(Skill debuff)
		{
			if (_debuffs == null)
			{
				_debuffs = new CopyOnWriteArrayList<>();
			}
			
			_debuffs.add(debuff);
		}
		
		public void teleportBack()
		{
			if (_paDuel)
			{
				_player.teleToLocation(new Location(_x, _y, _z));
			}
		}
		
		public L2PcInstance getPlayer()
		{
			return _player;
		}
	}
	
	public class ScheduleDuelTask implements Runnable
	{
		private final Duel _duel;
		
		public ScheduleDuelTask(Duel duel)
		{
			_duel = duel;
		}
		
		@Override
		public void run()
		{
			try
			{
				DuelResult status = _duel.checkEndDuelCondition();
				
				if (status == DuelResult.Canceled)
				{
					// do not schedule duel end if it was interrupted
					setFinished(true);
					_duel.endDuel(status);
				}
				else if (status != DuelResult.Continue)
				{
					setFinished(true);
					playKneelAnimation();
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndDuelTask(_duel, status), 5000);
				}
				else
				{
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public static class ScheduleStartDuelTask implements Runnable
	{
		private final Duel _duel;
		
		public ScheduleStartDuelTask(Duel duel)
		{
			_duel = duel;
		}
		
		@Override
		public void run()
		{
			try
			{
				// start/continue countdown
				int count = _duel.countdown();
				
				if (count == 4)
				{
					// players need to be teleportet first
					// TODO: stadia manager needs a function to return an unused stadium for duels
					// currently only teleports to the same stadium
					_duel.teleportPlayers(-83760, -238825, -3331);
					
					// give players 20 seconds to complete teleport and get ready (its ought to be 30 on offical..)
					ThreadPoolManager.getInstance().scheduleGeneral(this, 20000);
				}
				else if (count > 0) // duel not started yet - continue countdown
				{
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				}
				else
				{
					_duel.startDuel();
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public static class ScheduleEndDuelTask implements Runnable
	{
		private final Duel _duel;
		private final DuelResult _result;
		
		public ScheduleEndDuelTask(Duel duel, DuelResult result)
		{
			_duel = duel;
			_result = result;
		}
		
		@Override
		public void run()
		{
			try
			{
				_duel.endDuel(_result);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Stops all players from attacking. Used for duel timeout / interrupt.
	 */
	private void stopFighting()
	{
		ActionFailed af = ActionFailed.STATIC_PACKET;
		if (_partyDuel)
		{
			for (L2PcInstance temp : _playerA.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
			}
			for (L2PcInstance temp : _playerB.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
			}
		}
		else
		{
			_playerA.abortCast();
			_playerB.abortCast();
			_playerA.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			_playerA.setTarget(null);
			_playerB.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			_playerB.setTarget(null);
			_playerA.sendPacket(af);
			_playerB.sendPacket(af);
		}
	}
	
	/**
	 * Check if a player engaged in pvp combat (only for 1on1 duels)
	 * @param sendMessage
	 * @return returns true if a duelist is engaged in Pvp combat
	 */
	public boolean isDuelistInPvp(boolean sendMessage)
	{
		if (_partyDuel)
		{
			// Party duels take place in arenas - should be no other players there
			return false;
		}
		else if ((_playerA.getPvpFlag() != 0) || (_playerB.getPvpFlag() != 0))
		{
			if (sendMessage)
			{
				String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
				_playerA.sendMessage(engagedInPvP);
				_playerB.sendMessage(engagedInPvP);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Starts the duel
	 */
	public void startDuel()
	{
		// Save player Conditions
		savePlayerConditions();
		
		if ((_playerA == null) || (_playerB == null) || _playerA.isInDuel() || _playerB.isInDuel())
		{
			_playerConditions.clear();
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		if (_partyDuel)
		{
			// set isInDuel() state
			// cancel all active trades, just in case? xD
			for (L2PcInstance temp : _playerA.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(Team.BLUE);
				temp.broadcastUserInfo();
				broadcastToTeam2(new ExDuelUpdateUserInfo(temp));
			}
			for (L2PcInstance temp : _playerB.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(Team.RED);
				temp.broadcastUserInfo();
				broadcastToTeam1(new ExDuelUpdateUserInfo(temp));
			}
			
			// Send duel Start packets
			ExDuelReady ready = new ExDuelReady(1);
			ExDuelStart start = new ExDuelStart(1);
			
			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
		}
		else
		{
			// set isInDuel() state
			_playerA.setIsInDuel(_duelId);
			_playerA.setTeam(Team.BLUE);
			_playerB.setIsInDuel(_duelId);
			_playerB.setTeam(Team.RED);
			
			// Send duel Start packets
			ExDuelReady ready = new ExDuelReady(0);
			ExDuelStart start = new ExDuelStart(0);
			
			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
			
			broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
			broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));
			
			_playerA.broadcastUserInfo();
			_playerB.broadcastUserInfo();
		}
		
		// play sound
		broadcastToTeam1(B04_S01);
		broadcastToTeam2(B04_S01);
		
		// start duelling task
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleDuelTask(this), 1000);
	}
	
	/**
	 * Save the current player condition: hp, mp, cp, location
	 */
	public void savePlayerConditions()
	{
		if (_partyDuel)
		{
			for (L2PcInstance player : _playerA.getParty().getMembers())
			{
				_playerConditions.add(new PlayerCondition(player, _partyDuel));
			}
			for (L2PcInstance player : _playerB.getParty().getMembers())
			{
				_playerConditions.add(new PlayerCondition(player, _partyDuel));
			}
		}
		else
		{
			_playerConditions.add(new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.add(new PlayerCondition(_playerB, _partyDuel));
		}
	}
	
	/**
	 * Restore player conditions
	 * @param abnormalDuelEnd true if the duel was the duel canceled
	 */
	public void restorePlayerConditions(boolean abnormalDuelEnd)
	{
		// update isInDuel() state for all players
		if (_partyDuel)
		{
			for (L2PcInstance temp : _playerA.getParty().getMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(Team.NONE);
				temp.broadcastUserInfo();
			}
			for (L2PcInstance temp : _playerB.getParty().getMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(Team.NONE);
				temp.broadcastUserInfo();
			}
		}
		else
		{
			_playerA.setIsInDuel(0);
			_playerA.setTeam(Team.NONE);
			_playerA.broadcastUserInfo();
			_playerB.setIsInDuel(0);
			_playerB.setTeam(Team.NONE);
			_playerB.broadcastUserInfo();
		}
		
		// if it is an abnormal DuelEnd do not restore hp, mp, cp
		if (abnormalDuelEnd)
		{
			return;
		}
		
		// restore player conditions
		_playerConditions.forEach(c -> c.restoreCondition());
	}
	
	/**
	 * Get the duel id
	 * @return id
	 */
	public int getId()
	{
		return _duelId;
	}
	
	/**
	 * Returns the remaining time
	 * @return remaining time
	 */
	public int getRemainingTime()
	{
		return (int) (_duelEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}
	
	/**
	 * Get the player that requested the duel
	 * @return duel requester
	 */
	public L2PcInstance getPlayerA()
	{
		return _playerA;
	}
	
	/**
	 * Get the player that was challenged
	 * @return challenged player
	 */
	public L2PcInstance getPlayerB()
	{
		return _playerB;
	}
	
	/**
	 * Returns whether this is a party duel or not
	 * @return is party duel
	 */
	public boolean isPartyDuel()
	{
		return _partyDuel;
	}
	
	public void setFinished(boolean mode)
	{
		_finished = mode;
	}
	
	public boolean getFinished()
	{
		return _finished;
	}
	
	/**
	 * teleport all players to the given coordinates
	 * @param x
	 * @param y
	 * @param z
	 */
	public void teleportPlayers(int x, int y, int z)
	{
		// TODO: adjust the values if needed... or implement something better (especially using more then 1 arena)
		if (!_partyDuel)
		{
			return;
		}
		int offset = 0;
		
		for (L2PcInstance temp : _playerA.getParty().getMembers())
		{
			temp.teleToLocation(new Location((x + offset) - 180, y - 150, z));
			offset += 40;
		}
		offset = 0;
		for (L2PcInstance temp : _playerB.getParty().getMembers())
		{
			temp.teleToLocation(new Location((x + offset) - 180, y + 150, z));
			offset += 40;
		}
	}
	
	/**
	 * Broadcast a packet to the challenger team
	 * @param packet
	 */
	public void broadcastToTeam1(L2GameServerPacket packet)
	{
		if (_playerA == null)
		{
			return;
		}
		
		if (_partyDuel && (_playerA.getParty() != null))
		{
			for (L2PcInstance temp : _playerA.getParty().getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerA.sendPacket(packet);
		}
	}
	
	/**
	 * Broadcast a packet to the challenged team
	 * @param packet
	 */
	public void broadcastToTeam2(L2GameServerPacket packet)
	{
		if (_playerB == null)
		{
			return;
		}
		
		if (_partyDuel && (_playerB.getParty() != null))
		{
			for (L2PcInstance temp : _playerB.getParty().getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerB.sendPacket(packet);
		}
	}
	
	/**
	 * Get the duel winner
	 * @return winner
	 */
	public L2PcInstance getWinner()
	{
		if (!getFinished() || (_playerA == null) || (_playerB == null))
		{
			return null;
		}
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		return null;
	}
	
	/**
	 * Get the duel looser
	 * @return looser
	 */
	public L2PcInstance getLooser()
	{
		if (!getFinished() || (_playerA == null) || (_playerB == null))
		{
			return null;
		}
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		return null;
	}
	
	/**
	 * Playback the bow animation for all loosers
	 */
	public void playKneelAnimation()
	{
		L2PcInstance looser = getLooser();
		
		if (looser == null)
		{
			return;
		}
		
		if (_partyDuel && (looser.getParty() != null))
		{
			for (L2PcInstance temp : looser.getParty().getMembers())
			{
				temp.broadcastPacket(new SocialAction(temp.getObjectId(), 7));
			}
		}
		else
		{
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
		}
	}
	
	/**
	 * Do the countdown and send message to players if necessary
	 * @return current count
	 */
	public int countdown()
	{
		_countdown--;
		
		if (_countdown > 3)
		{
			return _countdown;
		}
		
		// Broadcast countdown to duelists
		SystemMessage sm = null;
		if (_countdown > 0)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECOND_S);
			sm.addInt(_countdown);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);
		}
		
		broadcastToTeam1(sm);
		broadcastToTeam2(sm);
		
		return _countdown;
	}
	
	/**
	 * The duel has reached a state in which it can no longer continue
	 * @param result the duel result.
	 */
	public void endDuel(DuelResult result)
	{
		if ((_playerA == null) || (_playerB == null))
		{
			// clean up
			_playerConditions.clear();
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		// inform players of the result
		SystemMessage sm = null;
		switch (result)
		{
			case Team1Win:
			case Team2Surrender:
				restorePlayerConditions(false);
				// send SystemMessage
				if (_partyDuel)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_S_PARTY_HAS_WON_THE_DUEL);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_DUEL);
				}
				sm.addString(_playerA.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Team1Surrender:
			case Team2Win:
				restorePlayerConditions(false);
				// send SystemMessage
				if (_partyDuel)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_S_PARTY_HAS_WON_THE_DUEL);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_DUEL);
				}
				sm.addString(_playerB.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Canceled:
				stopFighting();
				// Don't restore hp, mp, cp
				restorePlayerConditions(true);
				// TODO: is there no other message for a canceled duel?
				// send SystemMessage
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Timeout:
				stopFighting();
				// hp,mp,cp seem to be restored in a timeout too...
				restorePlayerConditions(false);
				// send SystemMessage
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
		}
		
		// Send end duel packet
		ExDuelEnd duelEnd = null;
		if (_partyDuel)
		{
			duelEnd = new ExDuelEnd(1);
		}
		else
		{
			duelEnd = new ExDuelEnd(0);
		}
		
		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
		
		// clean up
		_playerConditions.clear();
		DuelManager.getInstance().removeDuel(this);
	}
	
	/**
	 * Did a situation occur in which the duel has to be ended?
	 * @return DuelResult duel status
	 */
	public DuelResult checkEndDuelCondition()
	{
		// one of the players might leave during duel
		if ((_playerA == null) || (_playerB == null))
		{
			return DuelResult.Canceled;
		}
		
		// got a duel surrender request?
		if (_surrenderRequest != 0)
		{
			if (_surrenderRequest == 1)
			{
				return DuelResult.Team1Surrender;
			}
			return DuelResult.Team2Surrender;
		}
		// duel timed out
		else if (getRemainingTime() <= 0)
		{
			return DuelResult.Timeout;
		}
		// Has a player been declared winner yet?
		else if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.Team1Win;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.Team2Win;
		}
		
		// More end duel conditions for 1on1 duels
		else if (!_partyDuel)
		{
			// Duel was interrupted e.g.: player was attacked by mobs / other players
			if ((_playerA.getDuelState() == DUELSTATE_INTERRUPTED) || (_playerB.getDuelState() == DUELSTATE_INTERRUPTED))
			{
				return DuelResult.Canceled;
			}
			
			// Are the players too far apart?
			if (!_playerA.isInsideRadius(_playerB, 1600, false, false))
			{
				return DuelResult.Canceled;
			}
			
			// Did one of the players engage in PvP combat?
			if (isDuelistInPvp(true))
			{
				return DuelResult.Canceled;
			}
			
			// is one of the players in a Siege, Peace or PvP zone?
			if (_playerA.isInsideZone(ZoneId.PEACE) || _playerB.isInsideZone(ZoneId.PEACE) || _playerA.isInsideZone(ZoneId.SIEGE) || _playerB.isInsideZone(ZoneId.SIEGE) || _playerA.isInsideZone(ZoneId.PVP) || _playerB.isInsideZone(ZoneId.PVP))
			{
				return DuelResult.Canceled;
			}
		}
		
		return DuelResult.Continue;
	}
	
	/**
	 * Register a surrender request
	 * @param player the player that surrenders.
	 */
	public void doSurrender(L2PcInstance player)
	{
		// already received a surrender request
		if (_surrenderRequest != 0)
		{
			return;
		}
		
		// stop the fight
		stopFighting();
		
		// TODO: Can every party member cancel a party duel? or only the party leaders?
		if (_partyDuel)
		{
			if (_playerA.getParty().getMembers().contains(player))
			{
				_surrenderRequest = 1;
				for (L2PcInstance temp : _playerA.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				for (L2PcInstance temp : _playerB.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
			else if (_playerB.getParty().getMembers().contains(player))
			{
				_surrenderRequest = 2;
				for (L2PcInstance temp : _playerB.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				for (L2PcInstance temp : _playerA.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
				
			}
		}
		else
		{
			if (player == _playerA)
			{
				_surrenderRequest = 1;
				_playerA.setDuelState(DUELSTATE_DEAD);
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else if (player == _playerB)
			{
				_surrenderRequest = 2;
				_playerB.setDuelState(DUELSTATE_DEAD);
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}
	
	/**
	 * This function is called whenever a player was defeated in a duel
	 * @param player the player defeated.
	 */
	public void onPlayerDefeat(L2PcInstance player)
	{
		// Set player as defeated
		player.setDuelState(DUELSTATE_DEAD);
		
		if (_partyDuel)
		{
			boolean teamdefeated = true;
			for (L2PcInstance temp : player.getParty().getMembers())
			{
				if (temp.getDuelState() == DUELSTATE_DUELLING)
				{
					teamdefeated = false;
					break;
				}
			}
			
			if (teamdefeated)
			{
				L2PcInstance winner = _playerA;
				if (_playerA.getParty().getMembers().contains(player))
				{
					winner = _playerB;
				}
				
				for (L2PcInstance temp : winner.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
		}
		else
		{
			if ((player != _playerA) && (player != _playerB))
			{
				_log.warning("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");
			}
			
			if (_playerA == player)
			{
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else
			{
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}
	
	/**
	 * This function is called whenever a player leaves a party
	 * @param player the player quitting.
	 */
	public void onRemoveFromParty(L2PcInstance player)
	{
		// if it isn't a party duel ignore this
		if (!_partyDuel)
		{
			return;
		}
		
		// this player is leaving his party during party duel
		// if he's either playerA or playerB cancel the duel and port the players back
		if ((player == _playerA) || (player == _playerB))
		{
			for (PlayerCondition cond : _playerConditions)
			{
				cond.teleportBack();
				cond.getPlayer().setIsInDuel(0);
			}
			
			_playerA = null;
			_playerB = null;
		}
		else
		// teleport the player back & delete his PlayerCondition record
		{
			final PlayerCondition cond = _playerConditions.stream().filter(c -> c.getPlayer() == player).findFirst().orElse(null);
			if (cond != null)
			{
				cond.teleportBack();
				_playerConditions.remove(cond);
			}
			player.setIsInDuel(0);
		}
	}
	
	public void onBuff(L2PcInstance player, Skill debuff)
	{
		final PlayerCondition cond = _playerConditions.stream().filter(c -> c.getPlayer() == player).findFirst().orElse(null);
		if (cond != null)
		{
			cond.registerDebuff(debuff);
		}
	}
}
