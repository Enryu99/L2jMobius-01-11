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
package handlers.targethandlers;

import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.skills.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * @author UnAfraid
 */
public class EnemySummon implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		if ((target != null) && target.isSummon())
		{
			final Summon targetSummon = (Summon) target;
			if ((creature.isPlayer() && (creature.getSummon() != targetSummon) && !targetSummon.isDead() && ((targetSummon.getOwner().getPvpFlag() != 0) || (targetSummon.getOwner().getKarma() > 0))) || (targetSummon.getOwner().isInsideZone(ZoneId.PVP) && creature.getActingPlayer().isInsideZone(ZoneId.PVP)) || (targetSummon.getOwner().isInDuel() && creature.getActingPlayer().isInDuel() && (targetSummon.getOwner().getDuelId() == creature.getActingPlayer().getDuelId())))
			{
				return new Creature[]
				{
					targetSummon
				};
			}
		}
		return EMPTY_TARGET_LIST;
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.ENEMY_SUMMON;
	}
}
