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
package com.l2jmobius.gameserver.model.actor.knownlist;

import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Playable;
import com.l2jmobius.gameserver.model.actor.instance.L2CabaleBufferInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2FestivalGuideInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2FolkInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.network.serverpackets.CharMoveToLocation;

public class NpcKnownList extends CharKnownList
{
	// =========================================================
	// Data Field
	
	// =========================================================
	// Constructor
	public NpcKnownList(L2NpcInstance activeChar)
	{
		super(activeChar);
	}
	
	// Mobius: Fix for not broadcasting correct position.
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
		{
			return false;
		}
		
		if (getActiveObject().isNpc() && object.isCharacter())
		{
			// Broadcast correct walking NPC position.
			if (object.isPlayer() && getActiveChar().isMoving() && !getActiveChar().isInCombat())
			{
				((L2Character) object).broadcastPacket(new CharMoveToLocation(getActiveChar()));
			}
		}
		return true;
	}
	
	@Override
	public L2NpcInstance getActiveChar()
	{
		return (L2NpcInstance) super.getActiveChar();
	}
	
	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}
	
	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2FestivalGuideInstance)
		{
			return 10000;
		}
		
		if ((object instanceof L2FolkInstance) || !object.isCharacter())
		{
			return 0;
		}
		
		if (object instanceof L2CabaleBufferInstance)
		{
			return 900;
		}
		
		if (object instanceof L2Playable)
		{
			return 1500;
		}
		
		return 500;
	}
}
