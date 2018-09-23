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
package com.l2jmobius.gameserver.handler.skillhandlers;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.handler.ISkillHandler;
import com.l2jmobius.gameserver.model.L2Effect;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Skill.SkillType;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2SummonInstance;
import com.l2jmobius.gameserver.model.entity.olympiad.Olympiad;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.PlaySound;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.skills.BaseStats;
import com.l2jmobius.gameserver.skills.Formulas;
import com.l2jmobius.gameserver.skills.Stats;
import com.l2jmobius.gameserver.templates.item.L2WeaponType;
import com.l2jmobius.gameserver.util.Util;

/**
 * @author Steuf-Shyla
 */
public class Blow implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.BLOW
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		final boolean bss = activeChar.checkBss();
		final boolean sps = activeChar.checkSps();
		final boolean ss = activeChar.checkSs();
		
		Formulas.getInstance();
		
		for (L2Character target : (L2Character[]) targets)
		{
			if (target.isAlikeDead())
			{
				continue;
			}
			
			// Check firstly if target dodges skill
			final boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, skill);
			
			byte _successChance = 0;
			
			if (skill.getName().equals("Backstab"))
			{
				if (activeChar.isBehindTarget())
				{
					_successChance = (byte) Config.BACKSTAB_ATTACK_BEHIND;
				}
				else if (activeChar.isFrontTarget())
				{
					_successChance = (byte) Config.BACKSTAB_ATTACK_FRONT;
				}
				else
				{
					_successChance = (byte) Config.BACKSTAB_ATTACK_SIDE;
				}
			}
			else if (activeChar.isBehindTarget())
			{
				_successChance = (byte) Config.BLOW_ATTACK_BEHIND;
			}
			else if (activeChar.isFrontTarget())
			{
				_successChance = (byte) Config.BLOW_ATTACK_FRONT;
			}
			else
			{
				_successChance = (byte) Config.BLOW_ATTACK_SIDE;
			}
			
			boolean success = true;
			
			if ((skill.getCondition() & L2Skill.COND_CRIT) != 0)
			{
				success = (success && Formulas.getInstance().calcBlow(activeChar, target, _successChance));
			}
			
			if (!skillIsEvaded && success)
			{
				if (skill.hasEffects())
				{
					target.stopSkillEffects(skill.getId());
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target, ss, sps, bss);
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(skill);
						target.sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.ATTACK_FAILED);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
						return;
					}
				}
				
				final L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
				boolean soul = false;
				if (weapon != null)
				{
					soul = (ss && (weapon.getItemType() == L2WeaponType.DAGGER));
				}
				
				final boolean shld = Formulas.calcShldUse(activeChar, target);
				
				// Critical hit
				boolean crit = false;
				
				// Critical damage condition is applied for sure if there is skill critical condition
				if ((skill.getCondition() & L2Skill.COND_CRIT) != 0)
				{
					crit = true; // if there is not critical condition, calculate critical chance
				}
				else if (Formulas.calcCrit(skill.getBaseCritRate() * 10 * BaseStats.DEX.calcBonus(activeChar)))
				{
					crit = true;
				}
				
				double damage = Formulas.calcBlowDamage(activeChar, target, skill, shld, crit, soul);
				
				if (skill.getDmgDirectlyToHP() && (target instanceof L2PcInstance))
				{
					// no vegeange implementation
					final L2Character[] ts =
					{
						target,
						activeChar
					};
					
					for (L2Character targ : ts)
					{
						final L2PcInstance player = (L2PcInstance) targ;
						if (!player.isInvul())
						{
							// Check and calculate transfered damage
							final L2Summon summon = player.getPet();
							if ((summon instanceof L2SummonInstance) && Util.checkIfInRange(900, player, summon, true))
							{
								int tDmg = ((int) damage * (int) player.getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null)) / 100;
								
								// Only transfer dmg up to current HP, it should not be killed
								if (summon.getCurrentHp() < tDmg)
								{
									tDmg = (int) summon.getCurrentHp() - 1;
								}
								if (tDmg > 0)
								{
									summon.reduceCurrentHp(tDmg, activeChar);
									damage -= tDmg;
								}
							}
							if (damage >= player.getCurrentHp())
							{
								if (player.isInDuel())
								{
									player.setCurrentHp(1);
								}
								else
								{
									player.setCurrentHp(0);
									if (player.isInOlympiadMode())
									{
										player.abortAttack();
										player.abortCast();
										player.getStatus().stopHpMpRegeneration();
										// player.setIsDead(true);
										player.setIsPendingRevive(true);
										if (player.getPet() != null)
										{
											player.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
										}
									}
									else
									{
										player.doDie(activeChar);
									}
								}
							}
							else
							{
								player.setCurrentHp(player.getCurrentHp() - damage);
							}
						}
						final SystemMessage smsg = new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG);
						smsg.addString(activeChar.getName());
						smsg.addNumber((int) damage);
						player.sendPacket(smsg);
						
						// stop if no vengeance, so only target will be effected
						if (!player.vengeanceSkill(skill))
						{
							break;
						}
					}
				}
				else
				{
					target.reduceCurrentHp(damage, activeChar);
					
					// vengeance reflected damage
					if (target.vengeanceSkill(skill))
					{
						activeChar.reduceCurrentHp(damage, target);
					}
				}
				
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				if (activeChar instanceof L2PcInstance)
				{
					final L2PcInstance activePlayer = (L2PcInstance) activeChar;
					
					activePlayer.sendDamageMessage(target, (int) damage, false, true, false);
					if (activePlayer.isInOlympiadMode() && (target instanceof L2PcInstance) && ((L2PcInstance) target).isInOlympiadMode() && (((L2PcInstance) target).getOlympiadGameId() == activePlayer.getOlympiadGameId()))
					{
						Olympiad.getInstance().notifyCompetitorDamage(activePlayer, (int) damage, activePlayer.getOlympiadGameId());
					}
				}
				
				// Possibility of a lethal strike
				Formulas.calcLethalHit(activeChar, target, skill);
				final PlaySound PlaySound = new PlaySound("skillsound.critical_hit_02");
				activeChar.sendPacket(PlaySound);
			}
			else
			{
				if (skillIsEvaded)
				{
					if (target instanceof L2PcInstance)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_S1S_ATTACK);
						sm.addString(activeChar.getName());
						((L2PcInstance) target).sendPacket(sm);
					}
				}
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.ATTACK_FAILED);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);
				return;
			}
			
			// Self Effect
			if (skill.hasSelfEffects())
			{
				final L2Effect effect = activeChar.getFirstEffect(skill.getId());
				if ((effect != null) && effect.isSelfEffect())
				{
					effect.exit(false);
				}
				skill.getEffectsSelf(activeChar);
			}
		}
		
		if (skill.isMagic())
		{
			if (bss)
			{
				activeChar.removeBss();
			}
			else if (sps)
			{
				activeChar.removeSps();
			}
		}
		else
		{
			activeChar.removeSs();
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}