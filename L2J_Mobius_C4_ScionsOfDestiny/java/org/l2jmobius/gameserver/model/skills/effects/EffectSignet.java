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
package org.l2jmobius.gameserver.model.skills.effects;

import org.l2jmobius.gameserver.data.SkillTable;
import org.l2jmobius.gameserver.model.Effect;
import org.l2jmobius.gameserver.model.Skill;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.EffectPointInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.Env;
import org.l2jmobius.gameserver.model.skills.handlers.SkillSignet;
import org.l2jmobius.gameserver.model.skills.handlers.SkillSignetCasttime;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class EffectSignet extends Effect
{
	private Skill _skill;
	private EffectPointInstance _actor;
	
	public EffectSignet(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SIGNET_EFFECT;
	}
	
	@Override
	public void onStart()
	{
		if (getSkill() instanceof SkillSignet)
		{
			_skill = SkillTable.getInstance().getSkill(((SkillSignet) getSkill()).effectId, getLevel());
		}
		else if (getSkill() instanceof SkillSignetCasttime)
		{
			_skill = SkillTable.getInstance().getSkill(((SkillSignetCasttime) getSkill()).effectId, getLevel());
		}
		_actor = (EffectPointInstance) getEffected();
	}
	
	@Override
	public boolean onActionTime()
	{
		if (_skill == null)
		{
			return true;
		}
		final int mpConsume = _skill.getMpConsume();
		final PlayerInstance caster = (PlayerInstance) getEffector();
		if (mpConsume > getEffector().getStatus().getCurrentMp())
		{
			getEffector().sendPacket(new SystemMessage(SystemMessageId.YOUR_SKILL_WAS_REMOVED_DUE_TO_A_LACK_OF_MP));
			return false;
		}
		
		getEffector().reduceCurrentMp(mpConsume);
		
		for (Creature creature : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if ((creature == null) || (creature == caster) || creature.isDead())
			{
				continue;
			}
			
			if (_skill.isOffensive())
			{
				if ((creature instanceof PlayerInstance) && (((((PlayerInstance) creature).getClanId() > 0) && (caster.getClanId() > 0) && (((PlayerInstance) creature).getClanId() != caster.getClanId())) || ((((PlayerInstance) creature).getAllyId() > 0) && (caster.getAllyId() > 0) && (((PlayerInstance) creature).getAllyId() != caster.getAllyId())) || ((creature.getParty() != null) && (caster.getParty() != null) && !creature.getParty().equals(caster.getParty()))))
				{
					_skill.applyEffects(_actor, creature, false, false, false);
				}
			}
			else if (creature instanceof PlayerInstance)
			{
				if (((creature.getParty() != null) && (caster.getParty() != null) && creature.getParty().equals(caster.getParty())) || ((((PlayerInstance) creature).getClanId() > 0) && (caster.getClanId() > 0) && (((PlayerInstance) creature).getClanId() == caster.getClanId())) || ((((PlayerInstance) creature).getAllyId() > 0) && (caster.getAllyId() > 0) && (((PlayerInstance) creature).getAllyId() == caster.getAllyId())))
				{
					_skill.applyEffects(_actor, creature, false, false, false);
					_skill.applyEffects(_actor, caster, false, false, false); // Affect caster too.
				}
			}
		}
		return true;
	}
	
	@Override
	public void onExit()
	{
		if (_actor != null)
		{
			_actor.deleteMe();
		}
	}
}