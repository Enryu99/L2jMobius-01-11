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
import com.l2jmobius.gameserver.handler.SkillHandler;
import com.l2jmobius.gameserver.instancemanager.GrandBossManager;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.model.Skill.SkillType;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author earendil
 * @version $Revision: 1.1.2.2.2.4 $ $Date: 2005/04/06 16:13:48 $
 */
public class BalanceLife implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.BALANCE_LIFE
	};
	
	@Override
	public void useSkill(Creature creature, Skill skill, WorldObject[] targets)
	{
		// check for other effects
		try
		{
			final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.BUFF);
			
			if (handler != null)
			{
				handler.useSkill(creature, skill, targets);
			}
		}
		catch (Exception e)
		{
		}
		
		Creature target = null;
		
		PlayerInstance player = null;
		if (creature instanceof PlayerInstance)
		{
			player = (PlayerInstance) creature;
		}
		
		double fullHP = 0;
		double currentHPs = 0;
		
		for (WorldObject target2 : targets)
		{
			target = (Creature) target2;
			
			// We should not heal if char is dead
			if ((target == null) || target.isDead())
			{
				continue;
			}
			
			// Avoid characters heal inside Baium lair from outside
			if (((GrandBossManager.getInstance().getZone(creature) == null) && (GrandBossManager.getInstance().getZone(target) != null)) || ((GrandBossManager.getInstance().getZone(target) == null) && (GrandBossManager.getInstance().getZone(creature) != null)))
			{
				continue;
			}
			
			// Player holding a cursed weapon can't be healed and can't heal
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
			
			fullHP += target.getMaxHp();
			currentHPs += target.getCurrentHp();
		}
		
		final double percentHP = currentHPs / fullHP;
		
		for (WorldObject target2 : targets)
		{
			target = (Creature) target2;
			
			if ((target == null) || target.isDead())
			{
				continue;
			}
			
			final double newHP = target.getMaxHp() * percentHP;
			final double totalHeal = newHP - target.getCurrentHp();
			
			target.setCurrentHp(newHP);
			
			if (totalHeal > 0)
			{
				target.setLastHealAmount((int) totalHeal);
			}
			
			StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);
			
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			sm.addString("HP of the party has been balanced.");
			target.sendPacket(sm);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
