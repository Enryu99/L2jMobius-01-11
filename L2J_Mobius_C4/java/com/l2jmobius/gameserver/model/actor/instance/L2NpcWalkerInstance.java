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

import java.util.Map;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.ai.L2CharacterAI;
import com.l2jmobius.gameserver.ai.L2NpcWalkerAI;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;

/**
 * This class manages some npcs can walk in the city. <br>
 * It inherits all methods from L2NpcInstance. <br>
 * <br>
 * @original author Rayan RPG for L2Emu Project
 * @since 819
 */
public class L2NpcWalkerInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2NpcWalkerInstance.class.getName());
	
	/**
	 * Constructor of L2NpcWalkerInstance (use L2Character and L2NpcInstance constructor).<BR>
	 * <BR>
	 * @param objectId
	 * @param template
	 */
	public L2NpcWalkerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setAI(new L2NpcWalkerAI(new L2NpcWalkerAIAccessor()));
	}
	
	/**
	 * AI can't be deattached, npc must move always with the same AI instance.
	 * @param newAI AI to set for this L2NpcWalkerInstance
	 */
	@Override
	public void setAI(L2CharacterAI newAI)
	{
		if ((_ai == null) || !(_ai instanceof L2NpcWalkerAI))
		{
			_ai = newAI;
		}
	}
	
	@Override
	public void onSpawn()
	{
		getAI().setHomeX(getX());
		getAI().setHomeY(getY());
		getAI().setHomeZ(getZ());
	}
	
	/**
	 * Sends a chat to all _knowObjects
	 * @param chat message to say
	 */
	public void broadcastChat(String chat)
	{
		final Map<Integer, L2PcInstance> _knownPlayers = getKnownList().getKnownPlayers();
		
		if (_knownPlayers == null)
		{
			if (Config.DEVELOPER)
			{
				_log.info("broadcastChat _players == null");
			}
			return;
		}
		
		// we send message to known players only!
		if (_knownPlayers.size() > 0)
		{
			final CreatureSay cs = new CreatureSay(getObjectId(), 0, getName(), chat);
			
			// we interact and list players here
			for (final L2PcInstance players : _knownPlayers.values())
			{
				// finally send packet :D
				players.sendPacket(cs);
			}
		}
	}
	
	/**
	 * NPCs are immortal
	 * @param i ignore it
	 * @param attacker ignore it
	 * @param awake ignore it
	 */
	@Override
	public void reduceCurrentHp(double i, L2Character attacker, boolean awake)
	{
	}
	
	/**
	 * NPCs are immortal
	 * @param killer ignore it
	 * @return false
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		return false;
	}
	
	@Override
	public L2NpcWalkerAI getAI()
	{
		return (L2NpcWalkerAI) _ai;
	}
	
	private class L2NpcWalkerAIAccessor extends L2Character.AIAccessor
	{
		public L2NpcWalkerAIAccessor()
		{
		}
		
		/**
		 * AI can't be deattached.
		 */
		@Override
		public void detachAI()
		{
		}
	}
}