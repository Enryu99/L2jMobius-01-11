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

import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.enums.InstanceType;
import com.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import com.l2jmobius.gameserver.instancemanager.SiegeManager;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2SiegeClan;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.status.SiegeFlagStatus;
import com.l2jmobius.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jmobius.gameserver.model.entity.Siegable;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class L2SiegeFlagInstance extends L2Npc
{
	private final L2Clan _clan;
	private Siegable _siege;
	private final boolean _isAdvanced;
	private boolean _canTalk;
	
	public L2SiegeFlagInstance(L2PcInstance player, L2NpcTemplate template, boolean advanced)
	{
		super(template);
		setInstanceType(InstanceType.L2SiegeFlagInstance);
		
		_clan = player.getClan();
		_canTalk = true;
		_siege = SiegeManager.getInstance().getSiege(player.getX(), player.getY(), player.getZ());
		if (_siege == null)
		{
			_siege = FortSiegeManager.getInstance().getSiege(player.getX(), player.getY(), player.getZ());
		}
		if ((_clan == null) || (_siege == null))
		{
			throw new NullPointerException(getClass().getSimpleName() + ": Initialization failed.");
		}
		
		final L2SiegeClan sc = _siege.getAttackerClan(_clan);
		if (sc == null)
		{
			throw new NullPointerException(getClass().getSimpleName() + ": Cannot find siege clan.");
		}
		
		sc.addFlag(this);
		_isAdvanced = advanced;
		getStatus();
		setIsInvul(false);
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return !isInvul();
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return !isInvul();
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		if ((_siege != null) && (_clan != null))
		{
			final L2SiegeClan sc = _siege.getAttackerClan(_clan);
			if (sc != null)
			{
				sc.removeFlag(this);
			}
		}
		return true;
	}
	
	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		onAction(player);
	}
	
	@Override
	public void onAction(L2PcInstance player, boolean interact)
	{
		if ((player == null) || !canTarget(player))
		{
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player) && (Math.abs(player.getZ() - getZ()) < 100))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	
	public boolean isAdvancedHeadquarter()
	{
		return _isAdvanced;
	}
	
	@Override
	public SiegeFlagStatus getStatus()
	{
		return (SiegeFlagStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new SiegeFlagStatus(this));
	}
	
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		if (canTalk())
		{
			if (((getCastle() != null) && getCastle().getSiege().isInProgress()) || ((getFort() != null) && getFort().getSiege().isInProgress()))
			{
				if (_clan != null)
				{
					// send warning to owners of headquarters that theirs base is under attack
					_clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.YOUR_BASE_IS_BEING_ATTACKED));
					setCanTalk(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTalkTask(), 20000);
				}
			}
		}
	}
	
	private class ScheduleTalkTask implements Runnable
	{
		
		public ScheduleTalkTask()
		{
		}
		
		@Override
		public void run()
		{
			setCanTalk(true);
		}
	}
	
	void setCanTalk(boolean val)
	{
		_canTalk = val;
	}
	
	private boolean canTalk()
	{
		return _canTalk;
	}
}
