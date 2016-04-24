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
package com.l2jmobius.gameserver.model.actor.status;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.enums.DuelState;
import com.l2jmobius.gameserver.enums.PrivateStoreType;
import com.l2jmobius.gameserver.instancemanager.DuelManager;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Playable;
import com.l2jmobius.gameserver.model.actor.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.stat.PcStat;
import com.l2jmobius.gameserver.model.stats.Formulas;
import com.l2jmobius.gameserver.model.stats.Stats;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.util.Util;
import com.l2jmobius.util.Rnd;

public class PcStatus extends PlayableStatus
{
	private double _currentCp = 0; // Current CP of the L2PcInstance
	
	public PcStatus(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public final void reduceCp(int value)
	{
		setCurrentCp(getCurrentCp() > value ? getCurrentCp() - value : 0);
	}
	
	@Override
	public final void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true, false, false, false);
	}
	
	@Override
	public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
	}
	
	public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean ignoreCP)
	{
		if (getActiveChar().isDead())
		{
			return;
		}
		
		// If OFFLINE_MODE_NO_DAMAGE is enabled and player is offline and he is in store/craft mode, no damage is taken.
		if (Config.OFFLINE_MODE_NO_DAMAGE && (getActiveChar().getClient() != null) && getActiveChar().getClient().isDetached() && ((Config.OFFLINE_TRADE_ENABLE && ((getActiveChar().getPrivateStoreType() == PrivateStoreType.SELL) || (getActiveChar().getPrivateStoreType() == PrivateStoreType.BUY))) || (Config.OFFLINE_CRAFT_ENABLE && (getActiveChar().isInCraftMode() || (getActiveChar().getPrivateStoreType() == PrivateStoreType.MANUFACTURE)))))
		{
			return;
		}
		
		if ((getActiveChar().isInvul() || getActiveChar().isHpBlocked()) && !getActiveChar().isVulnerableFor(attacker) && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (!isHPConsumption)
		{
			getActiveChar().stopEffectsOnDamage(awake);
			// Attacked players in craft/shops stand up.
			if (getActiveChar().isInCraftMode() || getActiveChar().isInStoreMode())
			{
				getActiveChar().setPrivateStoreType(PrivateStoreType.NONE);
				getActiveChar().standUp();
				getActiveChar().broadcastUserInfo();
			}
			else if (getActiveChar().isSitting())
			{
				getActiveChar().standUp();
			}
			
			if (!isDOT && getActiveChar().isStunned() && (Rnd.get(10) == 0))
			{
				getActiveChar().stopStunning(true);
			}
		}
		
		int fullValue = (int) value;
		int tDmg = 0;
		int mpDam = 0;
		
		if ((attacker != null) && (attacker != getActiveChar()))
		{
			final L2PcInstance attackerPlayer = attacker.getActingPlayer();
			
			if (attackerPlayer != null)
			{
				if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
				{
					return;
				}
				
				if (getActiveChar().isInDuel())
				{
					if (getActiveChar().getDuelState() == DuelState.DEAD)
					{
						return;
					}
					else if (getActiveChar().getDuelState() == DuelState.WINNER)
					{
						return;
					}
					
					// cancel duel if player got hit by another player, that is not part of the duel
					if (attackerPlayer.getDuelId() != getActiveChar().getDuelId())
					{
						getActiveChar().setDuelState(DuelState.INTERRUPTED);
					}
				}
			}
			
			// Check and calculate transfered damage
			final L2Summon summon = getActiveChar().getAnyServitor();
			if ((summon != null) && Util.checkIfInRange(1000, getActiveChar(), summon, true))
			{
				tDmg = ((int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null)) / 100;
				
				// Only transfer dmg up to current HP, it should not be killed
				tDmg = Math.min((int) summon.getCurrentHp() - 1, tDmg);
				if (tDmg > 0)
				{
					summon.reduceCurrentHp(tDmg, attacker, null);
					value -= tDmg;
					fullValue = (int) value; // reduce the announced value here as player will get a message about summon damage
				}
			}
			
			mpDam = ((int) value * (int) getActiveChar().getStat().calcStat(Stats.MANA_SHIELD_PERCENT, 0, null, null)) / 100;
			
			if (mpDam > 0)
			{
				mpDam = (int) (value - mpDam);
				if (mpDam > getActiveChar().getCurrentMp())
				{
					getActiveChar().sendPacket(SystemMessageId.MP_BECAME_0_AND_THE_ARCANE_SHIELD_IS_DISAPPEARING);
					getActiveChar().stopSkillEffects(true, 1556);
					value = mpDam - getActiveChar().getCurrentMp();
					getActiveChar().setCurrentMp(0);
				}
				else
				{
					getActiveChar().reduceCurrentMp(mpDam);
					final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.ARCANE_SHIELD_DECREASED_YOUR_MP_BY_S1_INSTEAD_OF_HP);
					smsg.addInt(mpDam);
					getActiveChar().sendPacket(smsg);
					return;
				}
			}
			
			final L2PcInstance caster = getActiveChar().getTransferingDamageTo();
			if ((caster != null) && (getActiveChar().getParty() != null) && Util.checkIfInRange(1000, getActiveChar(), caster, true) && !caster.isDead() && (getActiveChar() != caster) && getActiveChar().getParty().getMembers().contains(caster))
			{
				int transferDmg = Math.min((int) caster.getCurrentHp() - 1, (((int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null)) / 100));
				
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					for (L2PcInstance member : caster.getParty().getMembers())
					{
						if (Util.checkIfInRange(1000, member, caster, false) && (member != caster))
						{
							membersInRange++;
						}
					}
					
					if ((attacker instanceof L2Playable) && (caster.getCurrentCp() > 0))
					{
						if (caster.getCurrentCp() > transferDmg)
						{
							caster.getStatus().reduceCp(transferDmg);
						}
						else
						{
							transferDmg = (int) (transferDmg - caster.getCurrentCp());
							caster.getStatus().reduceCp((int) caster.getCurrentCp());
						}
					}
					
					if (membersInRange > 0)
					{
						caster.reduceCurrentHp(transferDmg / membersInRange, attacker, null);
						value -= transferDmg;
						fullValue = (int) value;
					}
				}
			}
			
			if (!ignoreCP && (attacker instanceof L2Playable))
			{
				if (getCurrentCp() >= value)
				{
					setCurrentCp(getCurrentCp() - value); // Set Cp to diff of Cp vs value
					value = 0; // No need to subtract anything from Hp
				}
				else
				{
					value -= getCurrentCp(); // Get diff from value vs Cp; will apply diff to Hp
					setCurrentCp(0, false); // Set Cp to 0
				}
			}
			
			if ((fullValue > 0) && !isDOT)
			{
				SystemMessage smsg;
				// Send a System Message to the L2PcInstance
				smsg = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
				smsg.addString(getActiveChar().getName());
				smsg.addCharName(attacker);
				smsg.addInt(fullValue);
				getActiveChar().sendPacket(smsg);
				
				if ((tDmg > 0) && (summon != null))
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
					smsg.addString(summon.getName());
					smsg.addCharName(attacker);
					smsg.addInt(tDmg);
					getActiveChar().sendPacket(smsg);
					
					if (attackerPlayer != null)
					{
						smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_DEALT_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_THE_SERVITOR);
						smsg.addInt(fullValue);
						smsg.addInt(tDmg);
						attackerPlayer.sendPacket(smsg);
					}
				}
			}
		}
		
		if (value > 0)
		{
			value = getCurrentHp() - value;
			if (value <= 0)
			{
				if (getActiveChar().isInDuel())
				{
					getActiveChar().disableAllSkills();
					stopHpMpRegeneration();
					if (attacker != null)
					{
						attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						attacker.sendPacket(ActionFailed.STATIC_PACKET);
						attacker.setTarget(null);
						attacker.abortAttack();
					}
					
					// let the DuelManager know of his defeat
					DuelManager.getInstance().onPlayerDefeat(getActiveChar());
					value = 1;
				}
				else
				{
					value = 0;
				}
			}
			setCurrentHp(value);
		}
		
		if ((getActiveChar().getCurrentHp() >= 0.5) || isHPConsumption || !getActiveChar().isMortal())
		{
			return;
		}
		
		getActiveChar().abortAttack();
		getActiveChar().abortCast();
		
		if (getActiveChar().isInOlympiadMode())
		{
			stopHpMpRegeneration();
			getActiveChar().setIsDead(true);
			getActiveChar().setIsPendingRevive(true);
			final L2Summon pet = getActiveChar().getPet();
			if (pet != null)
			{
				pet.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
			}
			getActiveChar().getServitors().values().forEach(s -> s.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null));
			return;
		}
		
		if ((attacker != null) && (attacker.isPlayer()))
		{
			final int hpRestore = (int) attacker.getStat().calcStat(Stats.HP_RESTORE_ON_KILL, 0, null, null);
			if (hpRestore > 0)
			{
				final double amount = Math.max(Math.min((attacker.getMaxHp() * hpRestore) / 100, attacker.getMaxRecoverableHp() - attacker.getCurrentHp()), 0);
				if (amount != 0)
				{
					attacker.setCurrentHp(amount + attacker.getCurrentHp());
				}
			}
		}
		getActiveChar().doDie(attacker);
	}
	
	@Override
	public final double getCurrentCp()
	{
		return _currentCp;
	}
	
	@Override
	public final void setCurrentCp(double newCp)
	{
		setCurrentCp(newCp, true);
	}
	
	public final void setCurrentCp(double newCp, boolean broadcastPacket)
	{
		// Get the Max CP of the L2Character
		final int currentCp = (int) getCurrentCp();
		final int maxCp = getActiveChar().getStat().getMaxCp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
			{
				return;
			}
			
			if (newCp < 0)
			{
				newCp = 0;
			}
			
			if (newCp >= maxCp)
			{
				// Set the RegenActive flag to false
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if ((currentCp != _currentCp) && broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	@Override
	protected void doRegeneration()
	{
		final PcStat charstat = getActiveChar().getStat();
		
		// Modify the current CP of the L2Character and broadcast Server->Client packet StatusUpdate
		if (getCurrentCp() < charstat.getMaxRecoverableCp())
		{
			setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()), false);
		}
		
		// Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
		if (getCurrentHp() < charstat.getMaxRecoverableHp())
		{
			setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false);
		}
		
		// Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
		if (getCurrentMp() < charstat.getMaxRecoverableMp())
		{
			setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false);
		}
		
		getActiveChar().broadcastStatusUpdate(); // send the StatusUpdate packet
	}
	
	@Override
	public L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}
}
