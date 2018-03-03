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
package com.l2jmobius.gameserver.model.quest;

import java.util.concurrent.ScheduledFuture;

import com.l2jmobius.commons.concurrent.ThreadPoolManager;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

public class QuestTimer
{
	// =========================================================
	// Schedule Task
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!getIsActive())
			{
				return;
			}
			
			try
			{
				if (!getIsRepeating())
				{
					cancel();
				}
				getQuest().notifyEvent(getName(), getNpc(), getPlayer());
			}
			catch (Throwable t)
			{
			}
		}
	}
	
	// =========================================================
	// Data Field
	private boolean _isActive = true;
	private final String _name;
	private final Quest _quest;
	private final L2NpcInstance _npc;
	private final L2PcInstance _player;
	private final boolean _isRepeating;
	private ScheduledFuture<?> _schedular;
	
	// =========================================================
	// Constructor
	public QuestTimer(Quest quest, String name, long time, L2NpcInstance npc, L2PcInstance player, boolean repeating)
	{
		_name = name;
		_quest = quest;
		_player = player;
		_npc = npc;
		_isRepeating = repeating;
		if (repeating)
		{
			_schedular = ThreadPoolManager.scheduleAtFixedRate(new ScheduleTimerTask(), time, time); // Prepare auto end task
		}
		else
		{
			_schedular = ThreadPoolManager.schedule(new ScheduleTimerTask(), time); // Prepare auto end task
		}
	}
	
	public QuestTimer(Quest quest, String name, long time, L2NpcInstance npc, L2PcInstance player)
	{
		this(quest, name, time, npc, player, false);
	}
	
	public QuestTimer(QuestState qs, String name, long time)
	{
		this(qs.getQuest(), name, time, null, qs.getPlayer(), false);
	}
	
	// =========================================================
	// Method - Public
	public void cancel()
	{
		cancel(true);
	}
	
	public void cancel(boolean removeTimer)
	{
		_isActive = false;
		
		if (_schedular != null)
		{
			_schedular.cancel(false);
		}
		
		if (removeTimer)
		{
			getQuest().removeQuestTimer(this);
		}
	}
	
	// public method to compare if this timer matches with the key attributes passed.
	// a quest and a name are required.
	// null npc or player act as wildcards for the match
	public boolean isMatch(Quest quest, String name, L2NpcInstance npc, L2PcInstance player)
	{
		/*
		 * if (quest instanceof Frintezza_l2j) { LOGGER.info("#### INPUT Parameters ####"); LOGGER.info("Quest Name: " + quest.getName()); LOGGER.info("Quest Timer Name: " + name); LOGGER.info("Quest NPC: " + npc); if (npc != null) { LOGGER.info(" NPC Name: " + npc.getName());
		 * LOGGER.info(" NPC Id: " + npc.getNpcId()); LOGGER.info(" NPC Instance: " + npc.getInstanceId()); } LOGGER.info("Quest Player: " + player); if (player != null) { LOGGER.info(" Player Name: " + player.getName()); LOGGER.info(" Player Instance: " + player.getInstanceId()); }
		 * LOGGER.info("\n#### LOCAL Parameters ####"); LOGGER.info("Quest Name: " + getQuest().getName()); LOGGER.info("Quest Timer Name: " + getName()); LOGGER.info("Quest NPC: " + getNpc()); if (getNpc() != null) { LOGGER.info(" NPC Name: " + getNpc().getName()); LOGGER.info(" NPC Id: " +
		 * getNpc().getNpcId()); LOGGER.info(" NPC Instance: " + getNpc().getInstanceId()); } LOGGER.info("Quest Player: " + getPlayer()); if (getPlayer() != null) { LOGGER.info(" Player Name: " + getPlayer().getName()); LOGGER.info(" Player Instance: " + getPlayer().getInstanceId()); } }
		 */
		
		if ((quest == null) || (name == null))
		{
			return false;
		}
		
		if ((quest != getQuest()) || (name.compareToIgnoreCase(getName()) != 0))
		{
			return false;
		}
		
		return (npc == getNpc()) && (player == getPlayer());
	}
	
	// =========================================================
	// Property - Public
	public final boolean getIsActive()
	{
		return _isActive;
	}
	
	public final boolean getIsRepeating()
	{
		return _isRepeating;
	}
	
	public final Quest getQuest()
	{
		return _quest;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final L2NpcInstance getNpc()
	{
		return _npc;
	}
	
	public final L2PcInstance getPlayer()
	{
		return _player;
	}
	
	@Override
	public final String toString()
	{
		return _name;
	}
}
