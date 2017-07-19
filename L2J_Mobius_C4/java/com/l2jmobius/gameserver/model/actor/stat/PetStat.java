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
package com.l2jmobius.gameserver.model.actor.stat;

import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2PetDataTable;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.actor.instance.L2PetInstance;
import com.l2jmobius.gameserver.model.base.Experience;
import com.l2jmobius.gameserver.network.serverpackets.SocialAction;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.skills.Stats;

public class PetStat extends SummonStat
{
	// =========================================================
	// Data Field
	
	// =========================================================
	// Constructor
	public PetStat(L2PetInstance activeChar)
	{
		super(activeChar);
	}
	
	// =========================================================
	// Method - Public
	@Override
	public boolean addExp(long value)
	{
		if (!super.addExp(value))
		{
			return false;
		}
		
		getActiveChar().updateAndBroadcastStatus(1);
		
		return true;
	}
	
	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		if (!super.addExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessage.PET_EARNED_S1_EXP);
		sm.addNumber((int) addToExp);
		
		getActiveChar().updateAndBroadcastStatus(1);
		getActiveChar().getOwner().sendPacket(sm);
		
		return true;
	}
	
	@Override
	public final boolean addLevel(byte value)
	{
		if ((getLevel() + value) > (Experience.MAX_LEVEL - 1))
		{
			return false;
		}
		
		final boolean levelIncreased = super.addLevel(value);
		
		// Sync up exp with current level
		if ((getExp() > getExpForLevel(getLevel() + 1)) || (getExp() < getExpForLevel(getLevel())))
		{
			setExp(Experience.LEVEL[getLevel()]);
		}
		
		final StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		getActiveChar().broadcastPacket(su);
		
		if (levelIncreased)
		{
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), 15));
		}
		
		getActiveChar().updateAndBroadcastStatus(1);
		
		if (getActiveChar().getControlItem() != null)
		{
			getActiveChar().getControlItem().setEnchantLevel(getLevel());
		}
		
		return levelIncreased;
	}
	
	@Override
	public final long getExpForLevel(int level)
	{
		return L2PetDataTable.getInstance().getPetData(getActiveChar().getNpcId(), level).getPetMaxExp();
	}
	
	// =========================================================
	// Method - Private
	
	// =========================================================
	// Property - Public
	@Override
	public L2PetInstance getActiveChar()
	{
		return (L2PetInstance) super.getActiveChar();
	}
	
	public final int getFeedBattle()
	{
		return getActiveChar().getPetData().getPetFeedBattle();
	}
	
	public final int getFeedNormal()
	{
		return getActiveChar().getPetData().getPetFeedNormal();
	}
	
	@Override
	public void setLevel(byte value)
	{
		getActiveChar().stopFeed();
		super.setLevel(value);
		
		getActiveChar().setPetData(L2PetDataTable.getInstance().getPetData(getActiveChar().getTemplate().npcId, getLevel()));
		getActiveChar().startFeed();
		
		if (getActiveChar().getControlItem() != null)
		{
			getActiveChar().getControlItem().setEnchantLevel(getLevel());
		}
	}
	
	public final int getMaxFeed()
	{
		return getActiveChar().getPetData().getPetMaxFeed();
	}
	
	@Override
	public int getMaxHp()
	{
		return (int) calcStat(Stats.MAX_HP, getActiveChar().getPetData().getPetMaxHP(), null, null);
	}
	
	@Override
	public int getMaxMp()
	{
		return (int) calcStat(Stats.MAX_MP, getActiveChar().getPetData().getPetMaxMP(), null, null);
	}
	
	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		final double attack = getActiveChar().getPetData().getPetMAtk();
		return (int) calcStat(Stats.MAGIC_ATTACK, attack, target, skill);
	}
	
	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		final double defence = getActiveChar().getPetData().getPetMDef();
		return (int) calcStat(Stats.MAGIC_DEFENCE, defence, target, skill);
	}
	
	@Override
	public int getPAtk(L2Character target)
	{
		return (int) calcStat(Stats.POWER_ATTACK, getActiveChar().getPetData().getPetPAtk(), target, null);
	}
	
	@Override
	public int getPDef(L2Character target)
	{
		return (int) calcStat(Stats.POWER_DEFENCE, getActiveChar().getPetData().getPetPDef(), target, null);
	}
	
	@Override
	public int getAccuracy()
	{
		return (int) calcStat(Stats.ACCURACY_COMBAT, getActiveChar().getPetData().getPetAccuracy(), null, null);
	}
	
	@Override
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.CRITICAL_RATE, getActiveChar().getPetData().getPetCritical(), target, null);
	}
	
	@Override
	public int getEvasionRate(L2Character target)
	{
		return (int) calcStat(Stats.EVASION_RATE, getActiveChar().getPetData().getPetEvasion(), target, null);
	}
	
	@Override
	public int getRunSpeed()
	{
		return (int) calcStat(Stats.RUN_SPEED, getActiveChar().getPetData().getPetSpeed(), null, null);
	}
	
	@Override
	public int getWalkSpeed()
	{
		return getRunSpeed() / 2;
	}
	
	@Override
	public float getMovementSpeedMultiplier()
	{
		if (getActiveChar() == null)
		{
			return 1;
		}
		
		float val = (getRunSpeed() * 1f) / getActiveChar().getPetData().getPetSpeed();
		if (!getActiveChar().isRunning())
		{
			val = val / 2;
		}
		return val;
	}
	
	@Override
	public int getPAtkSpd()
	{
		int val = (int) calcStat(Stats.POWER_ATTACK_SPEED, getActiveChar().getPetData().getPetAtkSpeed(), null, null);
		if (!getActiveChar().isRunning())
		{
			val = val / 2;
		}
		return val;
	}
	
	@Override
	public int getMAtkSpd()
	{
		int val = (int) calcStat(Stats.MAGIC_ATTACK_SPEED, getActiveChar().getPetData().getPetCastSpeed(), null, null);
		if (!getActiveChar().isRunning())
		{
			val = val / 2;
		}
		return val;
	}
}