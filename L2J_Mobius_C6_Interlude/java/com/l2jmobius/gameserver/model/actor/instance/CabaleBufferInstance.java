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

import java.util.concurrent.ScheduledFuture;

import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.entity.sevensigns.SevenSigns;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import com.l2jmobius.gameserver.templates.creatures.NpcTemplate;

/**
 * @author Layane
 */
public class CabaleBufferInstance extends NpcInstance
{
	@Override
	public void onAction(PlayerInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			// Set the target of the PlayerInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the PlayerInstance player
			// The color to display in the select window is White
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			// Send a Server->Client packet ValidateLocation to correct the ArtefactInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else if (!canInteract(player)) // Calculate the distance between the PlayerInstance and the NpcInstance
		{
			// Notify the PlayerInstance AI with AI_INTENTION_INTERACT
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
		}
		// Send a Server->Client ActionFailed to the PlayerInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private ScheduledFuture<?> _aiTask;
	
	private class CabalaAI implements Runnable
	{
		private final CabaleBufferInstance _caster;
		
		protected CabalaAI(CabaleBufferInstance caster)
		{
			_caster = caster;
		}
		
		@Override
		public void run()
		{
			boolean isBuffAWinner = false;
			boolean isBuffALoser = false;
			
			final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
			int losingCabal = SevenSigns.CABAL_NULL;
			
			if (winningCabal == SevenSigns.CABAL_DAWN)
			{
				losingCabal = SevenSigns.CABAL_DUSK;
			}
			else if (winningCabal == SevenSigns.CABAL_DUSK)
			{
				losingCabal = SevenSigns.CABAL_DAWN;
			}
			
			/**
			 * For each known player in range, cast either the positive or negative buff. <BR>
			 * The stats affected depend on the player type, either a fighter or a mystic. <BR>
			 * <BR>
			 * Curse of Destruction (Loser)<BR>
			 * - Fighters: -25% Accuracy, -25% Effect Resistance<BR>
			 * - Mystics: -25% Casting Speed, -25% Effect Resistance<BR>
			 * <BR>
			 * <BR>
			 * Blessing of Prophecy (Winner) - Fighters: +25% Max Load, +25% Effect Resistance<BR>
			 * - Mystics: +25% Magic Cancel Resist, +25% Effect Resistance<BR>
			 */
			for (PlayerInstance player : getKnownList().getKnownPlayers().values())
			{
				final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
				
				if ((playerCabal == winningCabal) && (playerCabal != SevenSigns.CABAL_NULL) && (_caster.getNpcId() == SevenSigns.ORATOR_NPC_ID))
				{
					if (!player.isMageClass())
					{
						if (handleCast(player, 4364))
						{
							isBuffAWinner = true;
							continue;
						}
					}
					else if (handleCast(player, 4365))
					{
						isBuffAWinner = true;
						continue;
					}
				}
				else if ((playerCabal == losingCabal) && (playerCabal != SevenSigns.CABAL_NULL) && (_caster.getNpcId() == SevenSigns.PREACHER_NPC_ID))
				{
					if (!player.isMageClass())
					{
						if (handleCast(player, 4361))
						{
							isBuffALoser = true;
							continue;
						}
					}
					else if (handleCast(player, 4362))
					{
						isBuffALoser = true;
						continue;
					}
				}
				
				if (isBuffAWinner && isBuffALoser)
				{
					break;
				}
			}
		}
		
		private boolean handleCast(PlayerInstance player, int skillId)
		{
			final int skillLevel = player.getLevel() > 40 ? 1 : 2;
			
			if (player.isDead() || !player.isVisible() || !isInsideRadius(player, getDistanceToWatchObject(player), false, false))
			{
				return false;
			}
			
			Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
			if (player.getFirstEffect(skill) == null)
			{
				skill.getEffects(_caster, player, false, false, false);
				broadcastPacket(new MagicSkillUse(_caster, player, skill.getId(), skillLevel, skill.getHitTime(), 0));
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				sm.addSkillName(skillId);
				player.sendPacket(sm);
				return true;
			}
			
			return false;
		}
	}
	
	public CabaleBufferInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		if (_aiTask != null)
		{
			_aiTask.cancel(true);
		}
		
		_aiTask = ThreadPool.scheduleAtFixedRate(new CabalaAI(this), 3000, 3000);
	}
	
	@Override
	public void deleteMe()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(true);
			_aiTask = null;
		}
		
		super.deleteMe();
	}
	
	@Override
	public int getDistanceToWatchObject(WorldObject object)
	{
		return 900;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
}
