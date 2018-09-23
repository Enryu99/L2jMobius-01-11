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

import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.datatables.sql.NpcTable;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class manages all chest.
 */
public final class L2ChestInstance extends L2MonsterInstance
{
	private volatile boolean _isInteracted;
	private volatile boolean _specialDrop;
	
	public L2ChestInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_isInteracted = false;
		_specialDrop = false;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInteracted = false;
		_specialDrop = false;
		setMustRewardExpSp(true);
	}
	
	public synchronized boolean isInteracted()
	{
		return _isInteracted;
	}
	
	public synchronized void setInteracted()
	{
		_isInteracted = true;
	}
	
	public synchronized boolean isSpecialDrop()
	{
		return _specialDrop;
	}
	
	public synchronized void setSpecialDrop()
	{
		_specialDrop = true;
	}
	
	@Override
	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character lastAttacker)
	{
		int id = getTemplate().npcId;
		
		if (!_specialDrop)
		{
			if ((id >= 18265) && (id <= 18286))
			{
				id += 3536;
			}
			else if ((id == 18287) || (id == 18288))
			{
				id = 21671;
			}
			else if ((id == 18289) || (id == 18290))
			{
				id = 21694;
			}
			else if ((id == 18291) || (id == 18292))
			{
				id = 21717;
			}
			else if ((id == 18293) || (id == 18294))
			{
				id = 21740;
			}
			else if ((id == 18295) || (id == 18296))
			{
				id = 21763;
			}
			else if ((id == 18297) || (id == 18298))
			{
				id = 21786;
			}
		}
		
		super.doItemDrop(NpcTable.getInstance().getTemplate(id), lastAttacker);
	}
	
	// cast - trap chest
	public void chestTrap(L2Character player)
	{
		int trapSkillId = 0;
		final int rnd = Rnd.get(120);
		
		if (getTemplate().level >= 61)
		{
			if (rnd >= 90)
			{
				trapSkillId = 4139; // explosion
			}
			else if (rnd >= 50)
			{
				trapSkillId = 4118; // area paralysys
			}
			else if (rnd >= 20)
			{
				trapSkillId = 1167; // poison cloud
			}
			else
			{
				trapSkillId = 223; // sting
			}
		}
		else if (getTemplate().level >= 41)
		{
			if (rnd >= 90)
			{
				trapSkillId = 4139; // explosion
			}
			else if (rnd >= 60)
			{
				trapSkillId = 96; // bleed
			}
			else if (rnd >= 20)
			{
				trapSkillId = 1167; // poison cloud
			}
			else
			{
				trapSkillId = 4118; // area paralysys
			}
		}
		else if (getTemplate().level >= 21)
		{
			if (rnd >= 80)
			{
				trapSkillId = 4139; // explosion
			}
			else if (rnd >= 50)
			{
				trapSkillId = 96; // bleed
			}
			else if (rnd >= 20)
			{
				trapSkillId = 1167; // poison cloud
			}
			else
			{
				trapSkillId = 129; // poison
			}
		}
		else if (rnd >= 80)
		{
			trapSkillId = 4139; // explosion
		}
		else if (rnd >= 50)
		{
			trapSkillId = 96; // bleed
		}
		else
		{
			trapSkillId = 129; // poison
		}
		
		player.sendPacket(SystemMessage.sendString("There was a trap!"));
		handleCast(player, trapSkillId);
	}
	
	private boolean handleCast(L2Character player, int skillId)
	{
		int skillLevel = 1;
		final byte lvl = getTemplate().level;
		if ((lvl > 20) && (lvl <= 40))
		{
			skillLevel = 3;
		}
		else if ((lvl > 40) && (lvl <= 60))
		{
			skillLevel = 5;
		}
		else if (lvl > 60)
		{
			skillLevel = 6;
		}
		
		if (player.isDead() || !player.isVisible() || !player.isInsideRadius(this, getDistanceToWatchObject(player), false, false))
		{
			return false;
		}
		
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		
		if (player.getFirstEffect(skill) == null)
		{
			skill.getEffects(this, player, false, false, false);
			broadcastPacket(new MagicSkillUse(this, player, skill.getId(), skillLevel, skill.getHitTime(), 0));
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		if (super.isMovementDisabled())
		{
			return true;
		}
		
		if (_isInteracted)
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
