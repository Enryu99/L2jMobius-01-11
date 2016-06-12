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

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import com.l2jmobius.Config;
import com.l2jmobius.commons.util.MathUtil;
import com.l2jmobius.gameserver.enums.AttributeType;
import com.l2jmobius.gameserver.enums.Position;
import com.l2jmobius.gameserver.model.CharEffectList;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.skills.BuffInfo;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.skills.SkillConditionScope;
import com.l2jmobius.gameserver.model.stats.MoveType;
import com.l2jmobius.gameserver.model.stats.Stats;
import com.l2jmobius.gameserver.model.stats.StatsHolder;
import com.l2jmobius.gameserver.model.stats.TraitType;
import com.l2jmobius.gameserver.model.zone.ZoneId;

public class CharStat
{
	private final L2Character _activeChar;
	private long _exp = 0;
	private long _sp = 0;
	private byte _level = 1;
	private final float[] _attackTraits = new float[TraitType.values().length];
	private final int[] _attackTraitsCount = new int[TraitType.values().length];
	private final float[] _defenceTraits = new float[TraitType.values().length];
	private final int[] _defenceTraitsCount = new int[TraitType.values().length];
	private final int[] _traitsInvul = new int[TraitType.values().length];
	/** Creature's maximum buff count. */
	private int _maxBuffCount = Config.BUFFS_MAX_AMOUNT;
	private int _vampiricSum = 0;
	
	private final Map<Stats, Double> _statsAdd = new EnumMap<>(Stats.class);
	private final Map<Stats, Double> _statsMul = new EnumMap<>(Stats.class);
	private final Map<Stats, Map<MoveType, Double>> _moveTypeStats = new ConcurrentHashMap<>();
	private final Map<Integer, Double> _reuseStat = new ConcurrentHashMap<>();
	private final Map<Integer, Double> _mpConsumeStat = new ConcurrentHashMap<>();
	private final Map<Integer, LinkedList<Double>> _skillEvasionStat = new ConcurrentHashMap<>();
	private final Map<Stats, Map<Position, Double>> _positionStats = new ConcurrentHashMap<>();
	private final Deque<StatsHolder> _additionalAdd = new ConcurrentLinkedDeque<>();
	private final Deque<StatsHolder> _additionalMul = new ConcurrentLinkedDeque<>();
	private final Map<Stats, Double> _fixedValue = new ConcurrentHashMap<>();
	
	private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
	
	public CharStat(L2Character activeChar)
	{
		_activeChar = activeChar;
		Arrays.fill(_attackTraits, 1.0f);
		Arrays.fill(_defenceTraits, 1.0f);
	}
	
	/**
	 * @return the Accuracy (base+modifier) of the L2Character in function of the Weapon Expertise Penalty.
	 */
	public int getAccuracy()
	{
		return (int) getValue(Stats.ACCURACY_COMBAT);
	}
	
	/**
	 * @return the Magic Accuracy (base+modifier) of the L2Character
	 */
	public int getMagicAccuracy()
	{
		return (int) getValue(Stats.ACCURACY_MAGIC);
	}
	
	public L2Character getActiveChar()
	{
		return _activeChar;
	}
	
	/**
	 * @return the Attack Speed multiplier (base+modifier) of the L2Character to get proper animations.
	 */
	public final float getAttackSpeedMultiplier()
	{
		return (float) (((1.1) * getPAtkSpd()) / _activeChar.getTemplate().getBasePAtkSpd());
	}
	
	/**
	 * @return the CON of the L2Character (base+modifier).
	 */
	public final int getCON()
	{
		return (int) getValue(Stats.STAT_CON);
	}
	
	/**
	 * @param init
	 * @return the Critical Damage rate (base+modifier) of the L2Character.
	 */
	public final double getCriticalDmg(double init)
	{
		return getValue(Stats.CRITICAL_DAMAGE, init);
	}
	
	/**
	 * @return the Critical Hit rate (base+modifier) of the L2Character.
	 */
	public int getCriticalHit()
	{
		return (int) getValue(Stats.CRITICAL_RATE);
	}
	
	/**
	 * @return the DEX of the L2Character (base+modifier).
	 */
	public final int getDEX()
	{
		return (int) getValue(Stats.STAT_DEX);
	}
	
	/**
	 * @return the Attack Evasion rate (base+modifier) of the L2Character.
	 */
	public int getEvasionRate()
	{
		return (int) getValue(Stats.EVASION_RATE);
	}
	
	/**
	 * @return the Attack Evasion rate (base+modifier) of the L2Character.
	 */
	public int getMagicEvasionRate()
	{
		return (int) getValue(Stats.MAGIC_EVASION_RATE);
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public void setExp(long value)
	{
		_exp = value;
	}
	
	/**
	 * @return the INT of the L2Character (base+modifier).
	 */
	public int getINT()
	{
		return (int) getValue(Stats.STAT_INT);
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public void setLevel(byte value)
	{
		_level = value;
	}
	
	/**
	 * @param skill
	 * @return the Magical Attack range (base+modifier) of the L2Character.
	 */
	public final int getMagicalAttackRange(Skill skill)
	{
		if (skill != null)
		{
			return (int) getValue(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange());
		}
		
		return _activeChar.getTemplate().getBaseAttackRange();
	}
	
	public int getMaxCp()
	{
		return (int) getValue(Stats.MAX_CP);
	}
	
	public int getMaxRecoverableCp()
	{
		return (int) getValue(Stats.MAX_RECOVERABLE_CP, getMaxCp());
	}
	
	public int getMaxHp()
	{
		return (int) getValue(Stats.MAX_HP);
	}
	
	public int getMaxRecoverableHp()
	{
		return (int) getValue(Stats.MAX_RECOVERABLE_HP, getMaxHp());
	}
	
	public int getMaxMp()
	{
		return (int) getValue(Stats.MAX_MP);
	}
	
	public int getMaxRecoverableMp()
	{
		return (int) getValue(Stats.MAX_RECOVERABLE_MP, getMaxMp());
	}
	
	/**
	 * Return the MAtk (base+modifier) of the L2Character.<br>
	 * <B><U>Example of use</U>: Calculate Magic damage
	 * @return
	 */
	public int getMAtk()
	{
		return (int) getValue(Stats.MAGIC_ATTACK);
	}
	
	/**
	 * @return the MAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getMAtkSpd()
	{
		return (int) getValue(Stats.MAGIC_ATTACK_SPEED);
	}
	
	/**
	 * @return the Magic Critical Hit rate (base+modifier) of the L2Character.
	 */
	public final int getMCriticalHit()
	{
		return (int) getValue(Stats.MAGIC_CRITICAL_RATE);
	}
	
	/**
	 * <B><U>Example of use </U>: Calculate Magic damage.
	 * @return the MDef (base+modifier) of the L2Character against a skill in function of abnormal effects in progress.
	 */
	public int getMDef()
	{
		return (int) getValue(Stats.MAGICAL_DEFENCE);
	}
	
	/**
	 * @return the MEN of the L2Character (base+modifier).
	 */
	public final int getMEN()
	{
		return (int) getValue(Stats.STAT_MEN);
	}
	
	public final int getLUC()
	{
		return (int) getValue(Stats.STAT_LUC);
	}
	
	public final int getCHA()
	{
		return (int) getValue(Stats.STAT_CHA);
	}
	
	public double getMovementSpeedMultiplier()
	{
		double baseSpeed;
		if (_activeChar.isInsideZone(ZoneId.WATER))
		{
			baseSpeed = _activeChar.getTemplate().getBaseValue(_activeChar.isRunning() ? Stats.SWIM_RUN_SPEED : Stats.SWIM_WALK_SPEED, 0);
		}
		else
		{
			baseSpeed = _activeChar.getTemplate().getBaseValue(_activeChar.isRunning() ? Stats.RUN_SPEED : Stats.WALK_SPEED, 0);
		}
		return getMoveSpeed() * (1. / baseSpeed);
	}
	
	/**
	 * @return the RunSpeed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public double getRunSpeed()
	{
		return getValue(_activeChar.isInsideZone(ZoneId.WATER) ? Stats.SWIM_RUN_SPEED : Stats.RUN_SPEED);
	}
	
	/**
	 * @return the WalkSpeed (base+modifier) of the L2Character.
	 */
	public double getWalkSpeed()
	{
		return getValue(_activeChar.isInsideZone(ZoneId.WATER) ? Stats.SWIM_WALK_SPEED : Stats.WALK_SPEED);
	}
	
	/**
	 * @return the SwimRunSpeed (base+modifier) of the L2Character.
	 */
	public double getSwimRunSpeed()
	{
		return getValue(Stats.SWIM_RUN_SPEED);
	}
	
	/**
	 * @return the SwimWalkSpeed (base+modifier) of the L2Character.
	 */
	public double getSwimWalkSpeed()
	{
		return getValue(Stats.SWIM_WALK_SPEED);
	}
	
	/**
	 * @return the RunSpeed (base+modifier) or WalkSpeed (base+modifier) of the L2Character in function of the movement type.
	 */
	public double getMoveSpeed()
	{
		if (_activeChar.isInsideZone(ZoneId.WATER))
		{
			return _activeChar.isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		return _activeChar.isRunning() ? getRunSpeed() : getWalkSpeed();
	}
	
	/**
	 * @return the PAtk (base+modifier) of the L2Character.
	 */
	public int getPAtk()
	{
		return (int) getValue(Stats.PHYSICAL_ATTACK);
	}
	
	/**
	 * @return the PAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getPAtkSpd()
	{
		return (int) getValue(Stats.PHYSICAL_ATTACK_SPEED);
	}
	
	/**
	 * @return the PDef (base+modifier) of the L2Character.
	 */
	public int getPDef()
	{
		return (int) getValue(Stats.PHYSICAL_DEFENCE);
	}
	
	/**
	 * @return the Physical Attack range (base+modifier) of the L2Character.
	 */
	public final int getPhysicalAttackRange()
	{
		return (int) getValue(Stats.PHYSICAL_ATTACK_RANGE);
	}
	
	public int getPhysicalAttackRadius()
	{
		return 40;
	}
	
	public int getPhysicalAttackAngle()
	{
		return 120;
	}
	
	/**
	 * @return the weapon reuse modifier.
	 */
	public final double getWeaponReuseModifier()
	{
		return getValue(Stats.ATK_REUSE, 1);
	}
	
	/**
	 * @return the ShieldDef rate (base+modifier) of the L2Character.
	 */
	public final int getShldDef()
	{
		return (int) getValue(Stats.SHIELD_DEFENCE, 0);
	}
	
	public long getSp()
	{
		return _sp;
	}
	
	public void setSp(long value)
	{
		_sp = value;
	}
	
	/**
	 * @return the STR of the L2Character (base+modifier).
	 */
	public final int getSTR()
	{
		return (int) getValue(Stats.STAT_STR);
	}
	
	/**
	 * @return the WIT of the L2Character (base+modifier).
	 */
	public final int getWIT()
	{
		return (int) getValue(Stats.STAT_WIT);
	}
	
	/**
	 * @param skill
	 * @return the mpConsume.
	 */
	public final int getMpConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		double mpConsume = skill.getMpConsume();
		final double nextDanceMpCost = Math.ceil(skill.getMpConsume() / 2.);
		if (skill.isDance())
		{
			if (Config.DANCE_CONSUME_ADDITIONAL_MP && (_activeChar != null) && (_activeChar.getDanceCount() > 0))
			{
				mpConsume += _activeChar.getDanceCount() * nextDanceMpCost;
			}
		}
		
		return (int) (mpConsume * getMpConsumeTypeValue(skill.getMagicType()));
	}
	
	/**
	 * @param skill
	 * @return the mpInitialConsume.
	 */
	public final int getMpInitialConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		return skill.getMpInitialConsume();
	}
	
	public AttributeType getAttackElement()
	{
		final L2ItemInstance weaponInstance = _activeChar.getActiveWeaponInstance();
		// 1st order - weapon element
		if ((weaponInstance != null) && (weaponInstance.getAttackAttributeType() != AttributeType.NONE))
		{
			return weaponInstance.getAttackAttributeType();
		}
		
		// temp fix starts
		int tempVal = 0;
		final int stats[] =
		{
			getAttackElementValue(AttributeType.FIRE),
			getAttackElementValue(AttributeType.WATER),
			getAttackElementValue(AttributeType.WIND),
			getAttackElementValue(AttributeType.EARTH),
			getAttackElementValue(AttributeType.HOLY),
			getAttackElementValue(AttributeType.DARK)
		};
		
		AttributeType returnVal = AttributeType.NONE;
		
		for (byte x = 0; x < stats.length; x++)
		{
			if (stats[x] > tempVal)
			{
				returnVal = AttributeType.findByClientId(x);
				tempVal = stats[x];
			}
		}
		
		return returnVal;
	}
	
	public int getAttackElementValue(AttributeType attackAttribute)
	{
		switch (attackAttribute)
		{
			case FIRE:
				return (int) getValue(Stats.FIRE_POWER);
			case WATER:
				return (int) getValue(Stats.WATER_POWER);
			case WIND:
				return (int) getValue(Stats.WIND_POWER);
			case EARTH:
				return (int) getValue(Stats.EARTH_POWER);
			case HOLY:
				return (int) getValue(Stats.HOLY_POWER);
			case DARK:
				return (int) getValue(Stats.DARK_POWER);
			default:
				return 0;
		}
	}
	
	public int getDefenseElementValue(AttributeType defenseAttribute)
	{
		switch (defenseAttribute)
		{
			case FIRE:
				return (int) getValue(Stats.FIRE_RES);
			case WATER:
				return (int) getValue(Stats.WATER_RES);
			case WIND:
				return (int) getValue(Stats.WIND_RES);
			case EARTH:
				return (int) getValue(Stats.EARTH_RES);
			case HOLY:
				return (int) getValue(Stats.HOLY_RES);
			case DARK:
				return (int) getValue(Stats.DARK_RES);
			default:
				return (int) getValue(Stats.BASE_ATTRIBUTE_RES);
		}
	}
	
	public float getAttackTrait(TraitType traitType)
	{
		return _attackTraits[traitType.getId()];
	}
	
	public float[] getAttackTraits()
	{
		return _attackTraits;
	}
	
	public boolean hasAttackTrait(TraitType traitType)
	{
		return _attackTraitsCount[traitType.getId()] > 0;
	}
	
	public int[] getAttackTraitsCount()
	{
		return _attackTraitsCount;
	}
	
	public float getDefenceTrait(TraitType traitType)
	{
		return _defenceTraits[traitType.getId()];
	}
	
	public float[] getDefenceTraits()
	{
		return _defenceTraits;
	}
	
	public boolean hasDefenceTrait(TraitType traitType)
	{
		return _defenceTraitsCount[traitType.getId()] > 0;
	}
	
	public int[] getDefenceTraitsCount()
	{
		return _defenceTraitsCount;
	}
	
	public boolean isTraitInvul(TraitType traitType)
	{
		return _traitsInvul[traitType.getId()] > 0;
	}
	
	public int[] getTraitsInvul()
	{
		return _traitsInvul;
	}
	
	/**
	 * Gets the maximum buff count.
	 * @return the maximum buff count
	 */
	public int getMaxBuffCount()
	{
		return _maxBuffCount;
	}
	
	/**
	 * Sets the maximum buff count.
	 * @param buffCount the buff count
	 */
	public void setMaxBuffCount(int buffCount)
	{
		_maxBuffCount = buffCount;
	}
	
	/**
	 * Merges the stat's value with the values within the map of adds
	 * @param stat
	 * @param val
	 */
	public void mergeAdd(Stats stat, double val)
	{
		_statsAdd.merge(stat, val, stat::functionAdd);
	}
	
	/**
	 * Merges the stat's value with the values within the map of muls
	 * @param stat
	 * @param val
	 */
	public void mergeMul(Stats stat, double val)
	{
		_statsMul.merge(stat, val, stat::functionMul);
	}
	
	/**
	 * @param stat
	 * @return the add value
	 */
	public double getAdd(Stats stat)
	{
		return getAdd(stat, 0d);
	}
	
	/**
	 * @param stat
	 * @param defaultValue
	 * @return the add value
	 */
	public double getAdd(Stats stat, double defaultValue)
	{
		_lock.readLock().lock();
		try
		{
			return _statsAdd.getOrDefault(stat, defaultValue);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * @param stat
	 * @return the mul value
	 */
	public double getMul(Stats stat)
	{
		return getMul(stat, 1d);
	}
	
	/**
	 * @param stat
	 * @param defaultValue
	 * @return the mul value
	 */
	public double getMul(Stats stat, double defaultValue)
	{
		_lock.readLock().lock();
		try
		{
			return _statsMul.getOrDefault(stat, defaultValue);
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * @param stat
	 * @param baseValue
	 * @return the final value of the stat
	 */
	public double getValue(Stats stat, double baseValue)
	{
		final Double fixedValue = _fixedValue.get(stat);
		return fixedValue != null ? fixedValue : stat.finalize(_activeChar, Optional.of(baseValue));
	}
	
	/**
	 * @param stat
	 * @return the final value of the stat
	 */
	public double getValue(Stats stat)
	{
		final Double fixedValue = _fixedValue.get(stat);
		return fixedValue != null ? fixedValue : stat.finalize(_activeChar, Optional.empty());
	}
	
	protected void resetStats()
	{
		_statsAdd.clear();
		_statsMul.clear();
		_vampiricSum = 0;
		
		// Initialize default values
		for (Stats stat : Stats.values())
		{
			if (stat.getResetAddValue() != null)
			{
				_statsAdd.put(stat, stat.getResetAddValue());
			}
			if (stat.getResetMulValue() != null)
			{
				_statsMul.put(stat, stat.getResetMulValue());
			}
		}
	}
	
	/**
	 * Locks and resets all stats and recalculates all
	 * @param broadcast
	 */
	public final void recalculateStats(boolean broadcast)
	{
		_lock.writeLock().lock();
		try
		{
			// Copy old data before wiping it out
			final Map<Stats, Double> adds = !broadcast ? Collections.emptyMap() : new HashMap<>(_statsAdd);
			final Map<Stats, Double> muls = !broadcast ? Collections.emptyMap() : new HashMap<>(_statsMul);
			
			// Wipe all the data
			resetStats();
			
			// Collect all necessary effects
			final CharEffectList effectList = _activeChar.getEffectList();
			final Stream<BuffInfo> passives = effectList.hasPassives() ? effectList.getPassives().stream().filter(info -> info.getSkill().checkConditions(SkillConditionScope.PASSIVE, _activeChar, _activeChar)) : null;
			final Stream<BuffInfo> options = effectList.hasOptions() ? effectList.getOptions().stream() : null;
			final Stream<BuffInfo> effectsStream = Stream.concat(effectList.getEffects().stream(), Stream.concat(passives != null ? passives : Stream.empty(), options != null ? options : Stream.empty()));
			
			// Call pump to each effect
			//@formatter:off
			effectsStream.forEach(info -> info.getEffects().stream()
				.filter(effect -> effect.canStart(info))
				.filter(effect -> effect.canPump(info.getEffector(), info.getEffected(), info.getSkill()))
				.forEach(effect -> effect.pump(info.getEffected(), info.getSkill())));
			//@formatter:on
			
			// Merge with additional stats
			_additionalAdd.stream().filter(holder -> holder.verifyCondition(_activeChar)).forEach(holder -> mergeAdd(holder.getStat(), holder.getValue()));
			_additionalMul.stream().filter(holder -> holder.verifyCondition(_activeChar)).forEach(holder -> mergeMul(holder.getStat(), holder.getValue()));
			
			// Notify recalculation to child classes
			onRecalculateStats(broadcast);
			
			if (broadcast)
			{
				// Calculate the difference between old and new stats
				final Set<Stats> changed = new HashSet<>();
				for (Stats stat : Stats.values())
				{
					if (_statsAdd.getOrDefault(stat, stat.getResetAddValue()) != adds.getOrDefault(stat, stat.getResetAddValue()))
					{
						changed.add(stat);
					}
					else if (_statsMul.getOrDefault(stat, stat.getResetMulValue()) != muls.getOrDefault(stat, stat.getResetMulValue()))
					{
						changed.add(stat);
					}
				}
				
				_activeChar.broadcastModifiedStats(changed);
			}
		}
		finally
		{
			_lock.writeLock().unlock();
		}
	}
	
	protected void onRecalculateStats(boolean broadcast)
	{
		// Check if Max HP/MP/CP is lower than current due to new stats.
		if (_activeChar.getCurrentCp() > getMaxCp())
		{
			_activeChar.setCurrentCp(getMaxCp());
		}
		if (_activeChar.getCurrentHp() > getMaxHp())
		{
			_activeChar.setCurrentHp(getMaxHp());
		}
		if (_activeChar.getCurrentMp() > getMaxMp())
		{
			_activeChar.setCurrentMp(getMaxMp());
		}
	}
	
	public double getPositionTypeValue(Stats stat, Position position)
	{
		return _positionStats.getOrDefault(stat, Collections.emptyMap()).getOrDefault(position, 1d);
	}
	
	public void mergePositionTypeValue(Stats stat, Position position, double value, BiFunction<? super Double, ? super Double, ? extends Double> func)
	{
		_positionStats.computeIfAbsent(stat, key -> new ConcurrentHashMap<>()).merge(position, value, func);
	}
	
	public double getMoveTypeValue(Stats stat, MoveType type)
	{
		return _moveTypeStats.getOrDefault(stat, Collections.emptyMap()).getOrDefault(type, 0d);
	}
	
	public void mergeMoveTypeValue(Stats stat, MoveType type, double value)
	{
		_moveTypeStats.computeIfAbsent(stat, key -> new ConcurrentHashMap<>()).merge(type, value, MathUtil::add);
	}
	
	public double getReuseTypeValue(int magicType)
	{
		return _reuseStat.getOrDefault(magicType, 1d);
	}
	
	public void mergeReuseTypeValue(int magicType, double value, BiFunction<? super Double, ? super Double, ? extends Double> func)
	{
		_reuseStat.merge(magicType, value, func);
	}
	
	public double getMpConsumeTypeValue(int magicType)
	{
		return _mpConsumeStat.getOrDefault(magicType, 1d);
	}
	
	public void mergeMpConsumeTypeValue(int magicType, double value, BiFunction<? super Double, ? super Double, ? extends Double> func)
	{
		_mpConsumeStat.merge(magicType, value, func);
	}
	
	public double getSkillEvasionTypeValue(int magicType)
	{
		final LinkedList<Double> skillEvasions = _skillEvasionStat.get(magicType);
		if ((skillEvasions != null) && !skillEvasions.isEmpty())
		{
			return skillEvasions.peekLast();
		}
		return 0d;
	}
	
	public void addSkillEvasionTypeValue(int magicType, double value)
	{
		_skillEvasionStat.computeIfAbsent(magicType, k -> new LinkedList<>()).add(value);
	}
	
	public void removeSkillEvasionTypeValue(int magicType, double value)
	{
		_skillEvasionStat.computeIfPresent(magicType, (k, v) ->
		{
			v.remove(value);
			return !v.isEmpty() ? v : null;
		});
	}
	
	public void addToVampiricSum(int sum)
	{
		_vampiricSum += sum;
	}
	
	public int getVampiricSum()
	{
		_lock.readLock().lock();
		try
		{
			return _vampiricSum;
		}
		finally
		{
			_lock.readLock().unlock();
		}
	}
	
	/**
	 * Calculates the time required for this skill to be used again.
	 * @param skill the skill from which reuse time will be calculated.
	 * @return the time in milliseconds this skill is being under reuse.
	 */
	public int getReuseTime(Skill skill)
	{
		return (skill.isStaticReuse() || skill.isStatic()) ? skill.getReuseDelay() : (int) (skill.getReuseDelay() * getReuseTypeValue(skill.getMagicType()));
	}
	
	/**
	 * Adds static value to the 'add' map of the stat everytime recalculation happens
	 * @param stat
	 * @param value
	 * @param condition
	 * @return
	 */
	public boolean addAdditionalStat(Stats stat, double value, BiPredicate<L2Character, StatsHolder> condition)
	{
		return _additionalAdd.add(new StatsHolder(stat, value, condition));
	}
	
	/**
	 * Adds static value to the 'add' map of the stat everytime recalculation happens
	 * @param stat
	 * @param value
	 * @return
	 */
	public boolean addAdditionalStat(Stats stat, double value)
	{
		return _additionalAdd.add(new StatsHolder(stat, value));
	}
	
	/**
	 * @param stat
	 * @param value
	 * @return {@code true} if 'add' was removed, {@code false} in case there wasn't such stat and value
	 */
	public boolean removeAddAdditionalStat(Stats stat, double value)
	{
		final Iterator<StatsHolder> it = _additionalAdd.iterator();
		while (it.hasNext())
		{
			final StatsHolder holder = it.next();
			if ((holder.getStat() == stat) && (holder.getValue() == value))
			{
				it.remove();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Adds static multiplier to the 'mul' map of the stat everytime recalculation happens
	 * @param stat
	 * @param value
	 * @param condition
	 * @return
	 */
	public boolean mulAdditionalStat(Stats stat, double value, BiPredicate<L2Character, StatsHolder> condition)
	{
		return _additionalMul.add(new StatsHolder(stat, value, condition));
	}
	
	/**
	 * Adds static multiplier to the 'mul' map of the stat everytime recalculation happens
	 * @param stat
	 * @param value
	 * @return {@code true}
	 */
	public boolean mulAdditionalStat(Stats stat, double value)
	{
		return _additionalMul.add(new StatsHolder(stat, value));
	}
	
	/**
	 * @param stat
	 * @param value
	 * @return {@code true} if 'mul' was removed, {@code false} in case there wasn't such stat and value
	 */
	public boolean removeMulAdditionalStat(Stats stat, double value)
	{
		final Iterator<StatsHolder> it = _additionalMul.iterator();
		while (it.hasNext())
		{
			final StatsHolder holder = it.next();
			if ((holder.getStat() == stat) && (holder.getValue() == value))
			{
				it.remove();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param stat
	 * @param value
	 * @return true if the there wasn't previously set fixed value, {@code false} otherwise
	 */
	public boolean addFixedValue(Stats stat, Double value)
	{
		return _fixedValue.put(stat, value) == null;
	}
	
	/**
	 * @param stat
	 * @return {@code true} if fixed value is removed, {@code false} otherwise
	 */
	public boolean removeFixedValue(Stats stat)
	{
		return _fixedValue.remove(stat) != null;
	}
}
