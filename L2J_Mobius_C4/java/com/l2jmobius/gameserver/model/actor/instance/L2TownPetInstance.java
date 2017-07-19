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

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.model.L2CharPosition;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;
import com.l2jmobius.util.Rnd;

/**
 * @author Kerberos
 */
public class L2TownPetInstance extends L2NpcInstance
{
	int randomX;
	int randomY;
	int spawnX;
	int spawnY;
	
	public L2TownPetInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		
		if (Config.ALLOW_PET_WALKERS)
		{
			ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RandomWalkTask(), 2000, 4000);
		}
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The color to display in the select window is White
			final MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			// Send a Server->Client packet ValidateLocation to correct the L2ArtefactInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
		}
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(new ActionFailed());
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		spawnX = getX();
		spawnY = getY();
	}
	
	public class RandomWalkTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!isInActiveRegion())
			{
				return; // but rather the AI should be turned off completely..
			}
			
			randomX = (spawnX + Rnd.get(2 * 50)) - 50;
			randomY = (spawnY + Rnd.get(2 * 50)) - 50;
			setRunning();
			
			if ((randomX != getX()) && (randomY != getY()))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(randomX, randomY, getZ(), 0));
			}
		}
	}
}