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

import com.l2jmobius.commons.util.Rnd;

//

import com.l2jmobius.gameserver.ai.CtrlEvent;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.handler.ISkillHandler;
import com.l2jmobius.gameserver.instancemanager.DuelManager;
import com.l2jmobius.gameserver.model.Effect;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.model.Skill.SkillType;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.actor.Attackable;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.Playable;
import com.l2jmobius.gameserver.model.actor.Summon;
import com.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import com.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.skills.Formulas;

public class Continuous implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.BUFF,
		SkillType.DEBUFF,
		SkillType.DOT,
		SkillType.MDOT,
		SkillType.POISON,
		SkillType.BLEED,
		SkillType.HOT,
		SkillType.CPHOT,
		SkillType.MPHOT,
		// Skill.SkillType.MANAHEAL,
		// Skill.SkillType.MANA_BY_LEVEL,
		SkillType.FEAR,
		SkillType.CONT,
		SkillType.WEAKNESS,
		SkillType.REFLECT,
		SkillType.UNDEAD_DEFENSE,
		SkillType.AGGDEBUFF,
		SkillType.FORCE_BUFF
	};
	private Skill _skill;
	
	@Override
	public void useSkill(Creature creature, Skill skill2, WorldObject[] targets)
	{
		if (creature == null)
		{
			return;
		}
		
		PlayerInstance player = null;
		if (creature instanceof PlayerInstance)
		{
			player = (PlayerInstance) creature;
		}
		
		if (skill2.getEffectId() != 0)
		{
			final int skillLevel = skill2.getEffectLvl();
			final int skillEffectId = skill2.getEffectId();
			if (skillLevel == 0)
			{
				_skill = SkillTable.getInstance().getInfo(skillEffectId, 1);
			}
			else
			{
				_skill = SkillTable.getInstance().getInfo(skillEffectId, skillLevel);
			}
			
			if (_skill != null)
			{
				skill2 = _skill;
			}
		}
		
		final Skill skill = skill2;
		if (skill == null)
		{
			return;
		}
		
		final boolean bss = creature.checkBss();
		final boolean sps = creature.checkSps();
		final boolean ss = creature.checkSs();
		
		for (WorldObject target2 : targets)
		{
			Creature target = (Creature) target2;
			
			if (target == null)
			{
				continue;
			}
			
			if ((target instanceof PlayerInstance) && (creature instanceof Playable) && skill.isOffensive())
			{
				final PlayerInstance _char = (creature instanceof PlayerInstance) ? (PlayerInstance) creature : ((Summon) creature).getOwner();
				final PlayerInstance _attacked = (PlayerInstance) target;
				if ((_attacked.getClanId() != 0) && (_char.getClanId() != 0) && (_attacked.getClanId() == _char.getClanId()) && (_attacked.getPvpFlag() == 0))
				{
					continue;
				}
				if ((_attacked.getAllyId() != 0) && (_char.getAllyId() != 0) && (_attacked.getAllyId() == _char.getAllyId()) && (_attacked.getPvpFlag() == 0))
				{
					continue;
				}
			}
			
			if ((skill.getSkillType() != SkillType.BUFF) && (skill.getSkillType() != SkillType.HOT) && (skill.getSkillType() != SkillType.CPHOT) && (skill.getSkillType() != SkillType.MPHOT) && (skill.getSkillType() != SkillType.UNDEAD_DEFENSE) && (skill.getSkillType() != SkillType.AGGDEBUFF) && (skill.getSkillType() != SkillType.CONT))
			{
				if (target.reflectSkill(skill))
				{
					target = creature;
				}
			}
			
			// Walls and Door should not be buffed
			if ((target instanceof DoorInstance) && ((skill.getSkillType() == SkillType.BUFF) || (skill.getSkillType() == SkillType.HOT)))
			{
				continue;
			}
			
			// Anti-Buff Protection prevents you from getting buffs by other players
			if ((creature instanceof Playable) && (target != creature) && target.isBuffProtected() && !skill.isHeroSkill() && ((skill.getSkillType() == SkillType.BUFF) || (skill.getSkillType() == SkillType.HEAL_PERCENT) || (skill.getSkillType() == SkillType.FORCE_BUFF) || (skill.getSkillType() == SkillType.MANAHEAL_PERCENT) || (skill.getSkillType() == SkillType.COMBATPOINTHEAL) || (skill.getSkillType() == SkillType.REFLECT)))
			{
				continue;
			}
			
			// Player holding a cursed weapon can't be buffed and can't buff
			if (skill.getSkillType() == SkillType.BUFF)
			{
				if (target != creature)
				{
					if ((target instanceof PlayerInstance) && ((PlayerInstance) target).isCursedWeaponEquiped())
					{
						continue;
					}
					else if ((player != null) && player.isCursedWeaponEquiped())
					{
						continue;
					}
				}
			}
			
			// Possibility of a lethal strike
			if (!target.isRaid() && (!(target instanceof NpcInstance) || (((NpcInstance) target).getNpcId() != 35062)))
			{
				final int chance = Rnd.get(1000);
				Formulas.getInstance();
				if ((skill.getLethalChance2() > 0) && (chance < Formulas.calcLethal(creature, target, skill.getLethalChance2())))
				{
					if (target instanceof NpcInstance)
					{
						target.reduceCurrentHp(target.getCurrentHp() - 1, creature);
						creature.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE));
					}
				}
				else
				{
					Formulas.getInstance();
					if ((skill.getLethalChance1() > 0) && (chance < Formulas.calcLethal(creature, target, skill.getLethalChance1())))
					{
						if (target instanceof NpcInstance)
						{
							target.reduceCurrentHp(target.getCurrentHp() / 2, creature);
							creature.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE));
						}
					}
				}
			}
			
			if (skill.isOffensive())
			{
				final boolean acted = Formulas.getInstance().calcSkillSuccess(creature, target, skill, ss, sps, bss);
				
				if (!acted)
				{
					creature.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
					continue;
				}
			}
			else if (skill.getSkillType() == SkillType.BUFF)
			{
				if (!Formulas.getInstance().calcBuffSuccess(target, skill))
				{
					if (player != null)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
						sm.addString(target.getName());
						sm.addSkillName(skill.getDisplayId());
						creature.sendPacket(sm);
					}
					continue;
				}
			}
			
			if (skill.isToggle())
			{
				boolean stopped = false;
				
				final Effect[] effects = target.getAllEffects();
				if (effects != null)
				{
					for (Effect e : effects)
					{
						if (e != null)
						{
							if (e.getSkill().getId() == skill.getId())
							{
								e.exit(false);
								stopped = true;
							}
						}
					}
				}
				
				if (stopped)
				{
					break;
				}
			}
			
			// If target is not in game anymore...
			if ((target instanceof PlayerInstance) && (((PlayerInstance) target).isOnline() == 0))
			{
				continue;
			}
			
			// if this is a debuff let the duel manager know about it so the debuff can be removed after the duel (player & target must be in the same duel)
			if ((target instanceof PlayerInstance) && (player != null) && ((PlayerInstance) target).isInDuel() && ((skill.getSkillType() == SkillType.DEBUFF) || (skill.getSkillType() == SkillType.BUFF)) && (player.getDuelId() == ((PlayerInstance) target).getDuelId()))
			{
				DuelManager dm = DuelManager.getInstance();
				if (dm != null)
				{
					final Effect[] effects = skill.getEffects(creature, target, ss, sps, bss);
					if (effects != null)
					{
						for (Effect buff : effects)
						{
							if (buff != null)
							{
								dm.onBuff(((PlayerInstance) target), buff);
							}
						}
					}
				}
			}
			else
			{
				skill.getEffects(creature, target, ss, sps, bss);
			}
			
			if (skill.getSkillType() == SkillType.AGGDEBUFF)
			{
				if (target instanceof Attackable)
				{
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, creature, (int) skill.getPower());
				}
				else if (target instanceof Playable)
				{
					if (target.getTarget() == creature)
					{
						target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, creature);
					}
					else
					{
						target.setTarget(creature);
					}
				}
			}
			
			if (target.isDead() && (skill.getTargetType() == Skill.SkillTargetType.TARGET_AREA_CORPSE_MOB) && (target instanceof NpcInstance))
			{
				((NpcInstance) target).endDecayTask();
			}
		}
		
		if (!skill.isToggle())
		{
			if (skill.isMagic() && skill.useSpiritShot())
			{
				if (bss)
				{
					creature.removeBss();
				}
				else if (sps)
				{
					creature.removeSps();
				}
			}
			else if (skill.useSoulShot())
			{
				creature.removeSs();
			}
		}
		
		skill.getEffectsSelf(creature);
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
