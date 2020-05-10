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
package org.l2jmobius.gameserver.model.actor.stat;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.datatables.xml.ExperienceData;
import org.l2jmobius.gameserver.model.actor.Playable;

public class PlayableStat extends CreatureStat
{
	private static final Logger LOGGER = Logger.getLogger(PlayableStat.class.getName());
	
	public PlayableStat(Playable activeChar)
	{
		super(activeChar);
	}
	
	public boolean addExp(long amount)
	{
		if (((getExp() + amount) < 0) || ((amount > 0) && (getExp() == (getExpForLevel(ExperienceData.getInstance().getMaxLevel()) - 1))))
		{
			return true;
		}
		
		long value = amount;
		if ((getExp() + value) >= getExpForLevel(ExperienceData.getInstance().getMaxLevel()))
		{
			value = getExpForLevel(ExperienceData.getInstance().getMaxLevel()) - 1 - getExp();
		}
		
		setExp(getExp() + value);
		byte level = 1;
		for (level = 1; level <= ExperienceData.getInstance().getMaxLevel(); level++)
		{
			if (getExp() >= getExpForLevel(level))
			{
				continue;
			}
			--level;
			break;
		}
		
		if (level != getLevel())
		{
			addLevel((byte) (level - getLevel()));
		}
		
		return true;
	}
	
	public boolean removeExp(long amount)
	{
		long value = amount;
		if ((getExp() - value) < 0)
		{
			value = getExp() - 1;
		}
		
		setExp(getExp() - value);
		byte level = 0;
		for (level = 1; level <= ExperienceData.getInstance().getMaxLevel(); level++)
		{
			if (getExp() >= getExpForLevel(level))
			{
				continue;
			}
			
			level--;
			break;
		}
		
		if (level != getLevel())
		{
			addLevel((byte) (level - getLevel()));
		}
		
		return true;
	}
	
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		boolean expAdded = false;
		boolean spAdded = false;
		if (addToExp >= 0)
		{
			expAdded = addExp(addToExp);
		}
		
		if (addToSp >= 0)
		{
			spAdded = addSp(addToSp);
		}
		
		return expAdded || spAdded;
	}
	
	public boolean removeExpAndSp(long removeExp, int removeSp)
	{
		boolean expRemoved = false;
		boolean spRemoved = false;
		if (removeExp > 0)
		{
			expRemoved = removeExp(removeExp);
		}
		
		if (removeSp > 0)
		{
			spRemoved = removeSp(removeSp);
		}
		
		return expRemoved || spRemoved;
	}
	
	public boolean addLevel(byte amount)
	{
		byte value = amount;
		if ((getLevel() + value) > (ExperienceData.getInstance().getMaxLevel() - 1))
		{
			if (getLevel() < (ExperienceData.getInstance().getMaxLevel() - 1))
			{
				value = (byte) (ExperienceData.getInstance().getMaxLevel() - 1 - getLevel());
			}
			else
			{
				return false;
			}
		}
		
		final boolean levelIncreased = (getLevel() + value) > getLevel();
		value += getLevel();
		setLevel(value);
		
		// Sync up exp with current level
		if ((getExp() >= getExpForLevel(getLevel() + 1)) || (getExpForLevel(getLevel()) > getExp()))
		{
			setExp(getExpForLevel(getLevel()));
		}
		
		if (!levelIncreased)
		{
			return false;
		}
		
		getActiveChar().getStatus().setCurrentHp(getActiveChar().getStat().getMaxHp());
		getActiveChar().getStatus().setCurrentMp(getActiveChar().getStat().getMaxMp());
		
		return true;
	}
	
	public boolean addSp(int amount)
	{
		if (amount < 0)
		{
			LOGGER.info("wrong usage");
			return false;
		}
		
		final int currentSp = getSp();
		if (currentSp == Integer.MAX_VALUE)
		{
			return false;
		}
		
		int value = amount;
		if (currentSp > (Integer.MAX_VALUE - value))
		{
			value = Integer.MAX_VALUE - currentSp;
		}
		
		setSp(currentSp + value);
		return true;
	}
	
	public boolean removeSp(int amount)
	{
		final int currentSp = getSp();
		if (currentSp < amount)
		{
			setSp(getSp() - currentSp);
			return true;
		}
		setSp(getSp() - amount);
		return true;
	}
	
	public long getExpForLevel(int level)
	{
		return level;
	}
	
	@Override
	public Playable getActiveChar()
	{
		return (Playable) super.getActiveChar();
	}
}
