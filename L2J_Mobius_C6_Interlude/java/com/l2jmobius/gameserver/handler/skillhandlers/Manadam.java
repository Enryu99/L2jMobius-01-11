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

import com.l2jmobius.gameserver.handler.ISkillHandler;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.model.Skill.SkillType;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.Summon;
import com.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.skills.Formulas;

/**
 * Class handling the Mana damage skill
 * @author slyce
 */
public class Manadam implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.MANADAM
	};
	
	@Override
	public void useSkill(Creature creature, Skill skill, WorldObject[] targets)
	{
		Creature target = null;
		
		if (creature.isAlikeDead())
		{
			return;
		}
		
		final boolean sps = creature.checkSps();
		final boolean bss = creature.checkBss();
		
		for (WorldObject target2 : targets)
		{
			target = (Creature) target2;
			
			if (target.reflectSkill(skill))
			{
				target = creature;
			}
			
			if (target == null)
			{
				continue;
			}
			
			final boolean acted = Formulas.getInstance().calcMagicAffected(creature, target, skill);
			if (target.isInvul() || !acted)
			{
				creature.sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET));
			}
			else
			{
				final double damage = Formulas.getInstance().calcManaDam(creature, target, skill, sps, bss);
				
				final double mp = (damage > target.getCurrentMp() ? target.getCurrentMp() : damage);
				target.reduceCurrentMp(mp);
				
				if (damage > 0)
				{
					if (target.isSleeping())
					{
						target.stopSleeping(null);
					}
				}
				
				StatusUpdate sump = new StatusUpdate(target.getObjectId());
				sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
				target.sendPacket(sump);
				
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_MP_HAS_BEEN_DRAINED_BY_S1);
				
				if (creature instanceof NpcInstance)
				{
					final int mobId = ((NpcInstance) creature).getNpcId();
					sm.addNpcName(mobId);
				}
				else if (creature instanceof Summon)
				{
					final int mobId = ((Summon) creature).getNpcId();
					sm.addNpcName(mobId);
				}
				else
				{
					sm.addString(creature.getName());
				}
				sm.addNumber((int) mp);
				target.sendPacket(sm);
				
				if (creature instanceof PlayerInstance)
				{
					final SystemMessage sm2 = new SystemMessage(SystemMessageId.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1);
					sm2.addNumber((int) mp);
					creature.sendPacket(sm2);
				}
			}
		}
		
		if (bss)
		{
			creature.removeBss();
		}
		else if (sps)
		{
			creature.removeSps();
		}
		
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
