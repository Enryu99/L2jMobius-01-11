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
package com.l2jmobius.gameserver.model.actor.instance;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.l2jmobius.gameserver.enums.InstanceType;
import com.l2jmobius.gameserver.model.L2Spawn;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Tower;
import com.l2jmobius.gameserver.model.actor.templates.L2NpcTemplate;

/**
 * Class for Control Tower instance.
 */
public class L2ControlTowerInstance extends L2Tower
{
	private volatile Set<L2Spawn> _guards;
	
	public L2ControlTowerInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2ControlTowerInstance);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (getCastle().getSiege().isInProgress())
		{
			getCastle().getSiege().killedCT(this);
			
			if ((_guards != null) && !_guards.isEmpty())
			{
				for (L2Spawn spawn : _guards)
				{
					if (spawn == null)
					{
						continue;
					}
					try
					{
						spawn.stopRespawn();
						// spawn.getLastSpawn().doDie(spawn.getLastSpawn());
					}
					catch (Exception e)
					{
						_log.log(Level.WARNING, "Error at L2ControlTowerInstance", e);
					}
				}
				_guards.clear();
			}
		}
		return super.doDie(killer);
	}
	
	public void registerGuard(L2Spawn guard)
	{
		getGuards().add(guard);
	}
	
	private Set<L2Spawn> getGuards()
	{
		if (_guards == null)
		{
			synchronized (this)
			{
				if (_guards == null)
				{
					_guards = ConcurrentHashMap.newKeySet();
				}
			}
		}
		return _guards;
	}
}
