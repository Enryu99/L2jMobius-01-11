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

import java.util.Collection;
import java.util.concurrent.Future;

import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.zone.L2ZoneType;

/**
 * A damage zone
 * @author durgus
 */
public class L2DamageZone extends L2ZoneType
{
	private int _damagePerSec;
	private Future<?> _task;
	
	public L2DamageZone(int id)
	{
		super(id);
		
		// Setup default damage
		_damagePerSec = 100;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgSec"))
		{
			_damagePerSec = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (_task == null)
		{
			_task = ThreadPool.scheduleAtFixedRate(new ApplyDamage(this), 10, 1000);
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (_characterList.isEmpty())
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	protected Collection<L2Character> getCharacterList()
	{
		return _characterList.values();
	}
	
	protected int getDamagePerSecond()
	{
		return _damagePerSec;
	}
	
	class ApplyDamage implements Runnable
	{
		private final L2DamageZone _dmgZone;
		
		ApplyDamage(L2DamageZone zone)
		{
			_dmgZone = zone;
		}
		
		@Override
		public void run()
		{
			for (L2Character temp : _dmgZone.getCharacterList())
			{
				if ((temp != null) && !temp.isDead() && (temp instanceof L2PcInstance))
				{
					temp.reduceCurrentHp(_dmgZone.getDamagePerSecond(), null);
				}
			}
		}
	}
	
	@Override
	protected void onDieInside(L2Character character)
	{
	}
	
	@Override
	protected void onReviveInside(L2Character character)
	{
	}
}
