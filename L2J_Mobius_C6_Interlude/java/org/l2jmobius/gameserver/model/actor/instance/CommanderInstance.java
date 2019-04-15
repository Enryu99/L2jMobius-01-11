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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.knownlist.CommanderKnownList;
import org.l2jmobius.gameserver.model.actor.position.Location;
import org.l2jmobius.gameserver.templates.creatures.NpcTemplate;

/**
 * @author programmos
 */
public class CommanderInstance extends Attackable
{
	private int _homeX;
	private int _homeY;
	private int _homeZ;
	
	public CommanderInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
	}
	
	/**
	 * Return True if a siege is in progress and the Creature attacker isn't a Defender.<BR>
	 * <BR>
	 * @param attacker The Creature that the CommanderInstance try to attack
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Attackable during siege by all except defenders
		return (attacker != null) && (attacker instanceof PlayerInstance) && (getFort() != null) && (getFort().getFortId() > 0) && getFort().getSiege().getIsInProgress() && !getFort().getSiege().checkIsDefender(((PlayerInstance) attacker).getClan());
	}
	
	@Override
	public final CommanderKnownList getKnownList()
	{
		if ((super.getKnownList() == null) || !(super.getKnownList() instanceof CommanderKnownList))
		{
			setKnownList(new CommanderKnownList(this));
		}
		return (CommanderKnownList) super.getKnownList();
	}
	
	@Override
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof CommanderInstance))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (getFort().getSiege().getIsInProgress())
		{
			getFort().getSiege().killedCommander(this);
		}
		
		return true;
	}
	
	/**
	 * Sets home location of guard. Guard will always try to return to this location after it has killed all PK's in range.
	 */
	public void getHomeLocation()
	{
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
	}
	
	public int getHomeX()
	{
		return _homeX;
	}
	
	public int getHomeY()
	{
		return _homeY;
	}
	
	/**
	 * This method forces guard to return to home location previously set
	 */
	public void returnHome()
	{
		if (!isInsideRadius(_homeX, _homeY, 40, false))
		{
			setisReturningToSpawnPoint(true);
			clearAggroList();
			
			if (hasAI())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_homeX, _homeY, _homeZ, 0));
			}
		}
	}
}
