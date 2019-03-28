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
package com.l2jmobius.gameserver.model.zone.type;

import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.gameserver.enums.InstanceType;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.ZoneManager;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.entity.Castle;
import com.l2jmobius.gameserver.model.stats.Stats;
import com.l2jmobius.gameserver.model.zone.TaskZoneSettings;
import com.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * A damage zone
 * @author durgus
 */
public class DamageZone extends ZoneType
{
	private int _damageHPPerSec;
	private int _damageMPPerSec;
	
	private int _castleId;
	private Castle _castle;
	
	private int _startTask;
	private int _reuseTask;
	
	public DamageZone(int id)
	{
		super(id);
		
		// Setup default damage
		_damageHPPerSec = 200;
		_damageMPPerSec = 0;
		
		// Setup default start / reuse time
		_startTask = 10;
		_reuseTask = 5000;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
		
		setTargetType(InstanceType.Playable); // default only playabale
		setSettings(ZoneManager.getSettings(getName()) == null ? new TaskZoneSettings() : ZoneManager.getSettings(getName()));
	}
	
	@Override
	public TaskZoneSettings getSettings()
	{
		return (TaskZoneSettings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgHPSec"))
		{
			_damageHPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("dmgMPSec"))
		{
			_damageMPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("initialDelay"))
		{
			_startTask = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("reuse"))
		{
			_reuseTask = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if ((getSettings().getTask() != null) || ((_damageHPPerSec == 0) && (_damageMPPerSec == 0)))
		{
			return;
		}
		
		final PlayerInstance player = creature.getActingPlayer();
		if ((getCastle() != null) && (!getCastle().getSiege().isInProgress() || (player == null) || (player.getSiegeState() == 2)))
		{
			return;
		}
		
		synchronized (this)
		{
			if (getSettings().getTask() == null)
			{
				getSettings().setTask(ThreadPool.scheduleAtFixedRate(new ApplyDamage(this), _startTask, _reuseTask));
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (_characterList.isEmpty() && (getSettings().getTask() != null))
		{
			getSettings().clear();
		}
	}
	
	protected int getHPDamagePerSecond()
	{
		return _damageHPPerSec;
	}
	
	protected int getMPDamagePerSecond()
	{
		return _damageMPPerSec;
	}
	
	protected Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		
		return _castle;
	}
	
	private final class ApplyDamage implements Runnable
	{
		private final DamageZone _dmgZone;
		private final Castle _castle;
		
		protected ApplyDamage(DamageZone zone)
		{
			_dmgZone = zone;
			_castle = zone.getCastle();
		}
		
		@Override
		public void run()
		{
			if (!isEnabled())
			{
				return;
			}
			
			boolean siege = false;
			
			if (_castle != null)
			{
				siege = _castle.getSiege().isInProgress();
				// castle zones active only during siege
				if (!siege)
				{
					_dmgZone.getSettings().clear();
					return;
				}
			}
			
			for (Creature temp : _dmgZone.getCharactersInside())
			{
				if ((temp != null) && !temp.isDead())
				{
					if (siege)
					{
						// during siege defenders not affected
						final PlayerInstance player = temp.getActingPlayer();
						if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
						{
							continue;
						}
					}
					
					final double multiplier = 1 + (temp.calcStat(Stats.DAMAGE_ZONE_VULN, 0, null, null) / 100);
					
					if (getHPDamagePerSecond() != 0)
					{
						temp.reduceCurrentHp(_dmgZone.getHPDamagePerSecond() * multiplier, temp, null);
					}
					if (getMPDamagePerSecond() != 0)
					{
						temp.reduceCurrentMp(_dmgZone.getMPDamagePerSecond() * multiplier);
					}
				}
			}
		}
	}
}
