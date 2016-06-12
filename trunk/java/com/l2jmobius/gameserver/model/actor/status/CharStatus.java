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
package com.l2jmobius.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.stat.CharStat;
import com.l2jmobius.gameserver.model.events.EventDispatcher;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureHpChange;
import com.l2jmobius.gameserver.model.skills.AbnormalType;
import com.l2jmobius.gameserver.model.stats.Formulas;
import com.l2jmobius.gameserver.model.stats.Stats;

public class CharStatus
{
	protected static final Logger _log = Logger.getLogger(CharStatus.class.getName());
	
	private final L2Character _activeChar;
	
	private double _currentHp = 0; // Current HP of the L2Character
	private double _currentMp = 0; // Current MP of the L2Character
	
	/** Array containing all clients that need to be notified about hp/mp updates of the L2Character */
	private Set<L2Character> _StatusListener;
	
	private Future<?> _regTask;
	
	protected byte _flagsRegenActive = 0;
	
	protected static final byte REGEN_FLAG_CP = 4;
	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;
	
	private final AtomicInteger _previousHpPercent = new AtomicInteger();
	
	public CharStatus(L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	/**
	 * Add the object to the list of L2Character that must be informed of HP/MP updates of this L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.<br>
	 * Players who must be informed are players that target this L2Character.<br>
	 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<br>
	 * <B><U>Example of use</U>:</B>
	 * <ul>
	 * <li>Target a PC or NPC</li>
	 * <ul>
	 * @param object L2Character to add to the listener
	 */
	public final void addStatusListener(L2Character object)
	{
		if (object == getActiveChar())
		{
			return;
		}
		
		getStatusListener().add(object);
	}
	
	/**
	 * Remove the object from the list of L2Character that must be informed of HP/MP updates of this L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.<br>
	 * Players who must be informed are players that target this L2Character.<br>
	 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<br>
	 * <B><U>Example of use </U>:</B>
	 * <ul>
	 * <li>Untarget a PC or NPC</li>
	 * </ul>
	 * @param object L2Character to add to the listener
	 */
	public final void removeStatusListener(L2Character object)
	{
		getStatusListener().remove(object);
	}
	
	/**
	 * Return the list of L2Character that must be informed of HP/MP updates of this L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.<br>
	 * Players who must be informed are players that target this L2Character.<br>
	 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.
	 * @return The list of L2Character to inform or null if empty
	 */
	public final Set<L2Character> getStatusListener()
	{
		if (_StatusListener == null)
		{
			_StatusListener = ConcurrentHashMap.newKeySet();
		}
		return _StatusListener;
	}
	
	// place holder, only PcStatus has CP
	public void reduceCp(int value)
	{
	}
	
	/**
	 * Reduce the current HP of the L2Character and launch the doDie Task if necessary.
	 * @param value
	 * @param attacker
	 */
	public void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	public void reduceHp(double value, L2Character attacker, boolean isHpConsumption)
	{
		reduceHp(value, attacker, true, false, isHpConsumption);
	}
	
	public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		final L2Character activeChar = getActiveChar();
		if (activeChar.isDead())
		{
			return;
		}
		
		// invul handling
		if (activeChar.isHpBlocked() && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (attacker != null)
		{
			final L2PcInstance attackerPlayer = attacker.getActingPlayer();
			if ((attackerPlayer != null) && attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
			{
				return;
			}
		}
		
		if (!isDOT && !isHPConsumption)
		{
			if (awake)
			{
				activeChar.stopEffectsOnDamage();
			}
			if (Formulas.calcStunBreak(activeChar))
			{
				activeChar.stopStunning(true);
			}
			if (Formulas.calcRealTargetBreak())
			{
				getActiveChar().getEffectList().stopSkillEffects(true, AbnormalType.REAL_TARGET);
			}
		}
		
		if (value > 0)
		{
			final double oldHp = getCurrentHp();
			final double newHp = Math.max(getCurrentHp() - value, activeChar.isUndying() ? 1 : 0);
			setCurrentHp(newHp);
			EventDispatcher.getInstance().notifyEventAsync(new OnCreatureHpChange(activeChar, oldHp, newHp), activeChar);
		}
		
		if ((activeChar.getCurrentHp() < 0.5)) // Die
		{
			activeChar.doDie(attacker);
		}
	}
	
	public void reduceMp(double value)
	{
		setCurrentMp(Math.max(getCurrentMp() - value, 0));
	}
	
	/**
	 * Start the HP/MP/CP Regeneration task.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Calculate the regen task period</li>
	 * <li>Launch the HP/MP/CP Regeneration task with Medium priority</li>
	 * </ul>
	 */
	public final synchronized void startHpMpRegeneration()
	{
		if ((_regTask == null) && !getActiveChar().isDead())
		{
			if (Config.DEBUG)
			{
				_log.finer("HP/MP regen started");
			}
			
			// Get the Regeneration period
			final int period = Formulas.getRegeneratePeriod(getActiveChar());
			
			// Create the HP/MP/CP Regeneration task
			_regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
		}
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * </ul>
	 */
	public final synchronized void stopHpMpRegeneration()
	{
		if (_regTask != null)
		{
			if (Config.DEBUG)
			{
				_log.finer("HP/MP regen stop");
			}
			
			// Stop the HP/MP/CP Regeneration task
			_regTask.cancel(false);
			_regTask = null;
			
			// Set the RegenActive flag to false
			_flagsRegenActive = 0;
		}
	}
	
	// place holder, only PcStatus has CP
	public double getCurrentCp()
	{
		return 0;
	}
	
	// place holder, only PcStatus has CP
	public void setCurrentCp(double newCp)
	{
	}
	
	// place holder, only PcStatus has CP
	public void setCurrentCp(double newCp, boolean broadcastPacket)
	{
		
	}
	
	public final double getCurrentHp()
	{
		return _currentHp;
	}
	
	public final void setCurrentHp(double newHp)
	{
		setCurrentHp(newHp, true);
	}
	
	/**
	 * Sets the current hp of this character.
	 * @param newHp the new hp
	 * @param broadcastPacket if true StatusUpdate packet will be broadcasted.
	 * @return @{code true} if hp was changed, @{code false} otherwise.
	 */
	public boolean setCurrentHp(double newHp, boolean broadcastPacket)
	{
		// Get the Max HP of the L2Character
		final int currentHp = (int) getCurrentHp();
		final double maxHp = getActiveChar().getStat().getMaxHp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
			{
				return false;
			}
			
			if (newHp >= maxHp)
			{
				// Set the RegenActive flag to false
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		final boolean hpWasChanged = currentHp != _currentHp;
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (hpWasChanged)
		{
			final int lastHpPercent = _previousHpPercent.get();
			final int currentHpPercent = (int) ((_currentHp * 100) / maxHp);
			//@formatter:off
			if (((lastHpPercent >= 60) && (currentHpPercent <= 60))
				|| ((currentHpPercent >= 60) && (lastHpPercent <= 60))
				|| ((lastHpPercent >= 30) && (currentHpPercent <= 30))
				|| ((currentHpPercent >= 30) && (lastHpPercent <= 30)))
			//@formatter:on
			{
				if (_previousHpPercent.compareAndSet(lastHpPercent, currentHpPercent))
				{
					_activeChar.getStat().recalculateStats(true);
				}
			}
			
			if (broadcastPacket)
			{
				getActiveChar().broadcastStatusUpdate();
			}
		}
		
		return hpWasChanged;
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		boolean hpOrMpWasChanged = setCurrentHp(newHp, false);
		hpOrMpWasChanged |= setCurrentMp(newMp, false);
		if (hpOrMpWasChanged)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	public final double getCurrentMp()
	{
		return _currentMp;
	}
	
	public final void setCurrentMp(double newMp)
	{
		setCurrentMp(newMp, true);
	}
	
	/**
	 * Sets the current mp of this character.
	 * @param newMp the new mp
	 * @param broadcastPacket if true StatusUpdate packet will be broadcasted.
	 * @return @{code true} if mp was changed, @{code false} otherwise.
	 */
	public final boolean setCurrentMp(double newMp, boolean broadcastPacket)
	{
		// Get the Max MP of the L2Character
		final int currentMp = (int) getCurrentMp();
		final int maxMp = getActiveChar().getStat().getMaxMp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
			{
				return false;
			}
			
			if (newMp >= maxMp)
			{
				// Set the RegenActive flag to false
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		final boolean mpWasChanged = currentMp != _currentMp;
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (mpWasChanged && broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
		
		return mpWasChanged;
	}
	
	protected void doRegeneration()
	{
		final CharStat charstat = getActiveChar().getStat();
		
		// Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
		if (getCurrentHp() < charstat.getMaxRecoverableHp())
		{
			setCurrentHp(getCurrentHp() + getActiveChar().getStat().getValue(Stats.REGENERATE_HP_RATE), false);
		}
		
		// Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
		if (getCurrentMp() < charstat.getMaxRecoverableMp())
		{
			setCurrentMp(getCurrentMp() + getActiveChar().getStat().getValue(Stats.REGENERATE_MP_RATE), false);
		}
		
		if (!getActiveChar().isInActiveRegion())
		{
			// no broadcast necessary for characters that are in inactive regions.
			// stop regeneration for characters who are filled up and in an inactive region.
			if ((getCurrentHp() == charstat.getMaxRecoverableHp()) && (getCurrentMp() == charstat.getMaxMp()))
			{
				stopHpMpRegeneration();
			}
		}
		else
		{
			getActiveChar().broadcastStatusUpdate(); // send the StatusUpdate packet
		}
	}
	
	/** Task of HP/MP regeneration */
	class RegenTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				doRegeneration();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public L2Character getActiveChar()
	{
		return _activeChar;
	}
}
