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
package com.l2jmobius.gameserver.model.events;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.events.timers.IEventTimerCancel;
import com.l2jmobius.gameserver.model.events.timers.IEventTimerEvent;
import com.l2jmobius.gameserver.model.events.timers.TimerHolder;

/**
 * @author UnAfraid
 * @param <T>
 */
public final class TimerExecutor<T>
{
	private final Map<T, Set<TimerHolder<T>>> _timers = new ConcurrentHashMap<>();
	private final IEventTimerEvent<T> _eventListener;
	private final IEventTimerCancel<T> _cancelListener;
	
	public TimerExecutor(IEventTimerEvent<T> eventListener, IEventTimerCancel<T> cancelListener)
	{
		_eventListener = eventListener;
		_cancelListener = cancelListener;
	}
	
	/**
	 * Adds timer
	 * @param holder
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(TimerHolder<T> holder)
	{
		final Set<TimerHolder<T>> timers = _timers.computeIfAbsent(holder.getEvent(), key -> ConcurrentHashMap.newKeySet());
		for (TimerHolder<T> timer : timers)
		{
			if (timer.equals(holder))
			{
				timer.cancelTimer();
			}
		}
		return timers.add(holder);
	}
	
	/**
	 * Adds non-repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, StatsSet params, long time, L2Npc npc, L2PcInstance player, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, false, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Adds non-repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param time
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, long time, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, null, time, null, null, false, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Adds non-repeating timer
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, StatsSet params, long time, L2Npc npc, L2PcInstance player)
	{
		return addTimer(event, params, time, npc, player, _eventListener);
	}
	
	/**
	 * Adds non-repeating timer
	 * @param event
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, long time, L2Npc npc, L2PcInstance player)
	{
		return addTimer(event, null, time, npc, player, _eventListener);
	}
	
	/**
	 * Adds repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addRepeatingTimer(T event, StatsSet params, long time, L2Npc npc, L2PcInstance player, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, true, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Adds repeating timer
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addRepeatingTimer(T event, StatsSet params, long time, L2Npc npc, L2PcInstance player)
	{
		return addRepeatingTimer(event, params, time, npc, player, _eventListener);
	}
	
	/**
	 * Adds repeating timer
	 * @param event
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addRepeatingTimer(T event, long time, L2Npc npc, L2PcInstance player)
	{
		return addRepeatingTimer(event, null, time, npc, player, _eventListener);
	}
	
	/**
	 * That method is executed right after notification to {@link IEventTimerEvent#onTimerEvent(TimerHolder)} in order to remove the holder from the _timers map
	 * @param holder
	 */
	public void onTimerPostExecute(TimerHolder<T> holder)
	{
		// Remove non repeating timer upon execute
		if (!holder.isRepeating())
		{
			final Set<TimerHolder<T>> timers = _timers.get(holder.getEvent());
			if ((timers == null) || timers.isEmpty())
			{
				return;
			}
			
			// Remove the timer
			timers.removeIf(holder::equals);
			
			// If there's no events inside that set remove it
			if (timers.isEmpty())
			{
				_timers.remove(holder.getEvent());
			}
		}
	}
	
	/**
	 * @param timer
	 */
	public void onTimerCancel(TimerHolder<T> timer)
	{
		final Set<TimerHolder<T>> timers = _timers.get(timer.getEvent());
		if ((timers != null) && timers.remove(timer))
		{
			_eventListener.onTimerEvent(timer);
			if (timers.isEmpty())
			{
				_timers.remove(timer.getEvent());
			}
		}
	}
	
	/**
	 * Cancels and removes all timers from the _timers map
	 */
	public void cancelAllTimers()
	{
		_timers.values().stream().flatMap(Set::stream).forEach(TimerHolder::cancelTimer);
		_timers.clear();
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code true} if there is a timer with the given event npc and player parameters, {@code false} otherwise
	 */
	public boolean hasTimer(T event, L2Npc npc, L2PcInstance player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		return timers.stream().anyMatch(holder -> holder.isEqual(event, npc, player));
	}
	
	/**
	 * @param event
	 * @return {@code true} if there is at least one timer with the given event, {@code false} otherwise
	 */
	public boolean hasTimers(T event)
	{
		return _timers.containsKey(event);
	}
	
	/**
	 * @param event
	 * @return {@code true} if at least one timer for the given event were stopped, {@code false} otherwise
	 */
	public boolean cancelTimers(T event)
	{
		final Set<TimerHolder<T>> timers = _timers.remove(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		timers.forEach(TimerHolder::cancelTimer);
		return true;
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code true} if timer for the given event, npc, player were stopped, {@code false} otheriwse
	 */
	public boolean cancelTimer(T event, L2Npc npc, L2PcInstance player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		final Iterator<TimerHolder<T>> holders = timers.iterator();
		while (holders.hasNext())
		{
			final TimerHolder<T> holder = holders.next();
			if (holder.isEqual(event, npc, player))
			{
				holders.remove();
				return holder.cancelTimer();
			}
		}
		return false;
	}
	
	/**
	 * Cancel all timers of specified npc
	 * @param npc
	 */
	public void cancelTimersOf(L2Npc npc)
	{
		_timers.values().forEach(timers -> timers.stream().filter(holder -> holder.getNpc() == npc).forEach(TimerHolder::cancelTimer));
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return the remaining time of the timer, or -1 in case it doesn't exists
	 */
	public long getRemainingTime(T event, L2Npc npc, L2PcInstance player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return -1;
		}
		
		final Iterator<TimerHolder<T>> holders = timers.iterator();
		while (holders.hasNext())
		{
			final TimerHolder<T> holder = holders.next();
			if (holder.isEqual(event, npc, player))
			{
				holders.remove();
				return holder.getRemainingTime();
			}
		}
		
		return -1;
	}
}
