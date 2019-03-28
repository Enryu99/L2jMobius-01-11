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

import java.util.ArrayList;
import java.util.List;

import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.geoengine.GeoEngine;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.spawn.Spawn;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import com.l2jmobius.gameserver.templates.creatures.NpcTemplate;

public class ControlTowerInstance extends NpcInstance
{
	
	private List<Spawn> _guards;
	
	public ControlTowerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean isAttackable()
	{
		// Attackable during siege by attacker only
		return (getCastle() != null) && (getCastle().getCastleId() > 0) && getCastle().getSiege().getIsInProgress();
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Attackable during siege by attacker only
		return (attacker != null) && (attacker instanceof PlayerInstance) && (getCastle() != null) && (getCastle().getCastleId() > 0) && getCastle().getSiege().getIsInProgress() && getCastle().getSiege().checkIsAttacker(((PlayerInstance) attacker).getClan());
	}
	
	@Override
	public void onForcedAttack(PlayerInstance player)
	{
		onAction(player);
	}
	
	@Override
	public void onAction(PlayerInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		// Check if the PlayerInstance already target the NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the PlayerInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the PlayerInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			// Send a Server->Client packet StatusUpdate of the NpcInstance to the PlayerInstance to update its HP bar
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
			
			// Send a Server->Client packet ValidateLocation to correct the NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else if (isAutoAttackable(player) && (Math.abs(player.getZ() - getZ()) < 100) // Less then max height difference, delete check when geo
			&& GeoEngine.getInstance().canSeeTarget(player, this))
		{
			// Notify the PlayerInstance AI with AI_INTENTION_INTERACT
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			
			// Send a Server->Client ActionFailed to the PlayerInstance in order to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public void onDeath()
	{
		if (getCastle().getSiege().getIsInProgress())
		{
			getCastle().getSiege().killedCT(this);
			
			if ((getGuards() != null) && (getGuards().size() > 0))
			{
				for (Spawn spawn : getGuards())
				{
					if (spawn == null)
					{
						continue;
					}
					spawn.stopRespawn();
				}
			}
		}
	}
	
	public void registerGuard(Spawn guard)
	{
		getGuards().add(guard);
	}
	
	public final List<Spawn> getGuards()
	{
		if (_guards == null)
		{
			_guards = new ArrayList<>();
		}
		return _guards;
	}
}
