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
package com.l2jmobius.gameserver.model.stats;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import com.l2jmobius.commons.util.MathUtil;
import com.l2jmobius.gameserver.enums.AttributeType;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.stats.finalizers.AttributeFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.BaseStatsFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MAccuracyFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MAttackFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MAttackSpeedFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MCritRateFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MDefenseFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MEvasionRateFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MaxCpFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MaxHpFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.MaxMpFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.PAccuracyFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.PAttackFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.PAttackSpeedFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.PCriticalRateFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.PDefenseFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.PEvasionRateFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.PRangeFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.RandomDamageFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.RegenCPFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.RegenHPFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.RegenMPFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.ShotsBonusFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.SpeedFinalizer;
import com.l2jmobius.gameserver.model.stats.finalizers.VampiricChanceFinalizer;

/**
 * Enum of basic stats.
 * @author mkizub, UnAfraid, NosBit, Sdw
 */
public enum Stats
{
	// HP, MP & CP
	MAX_HP("maxHp", new MaxHpFinalizer()),
	MAX_MP("maxMp", new MaxMpFinalizer()),
	MAX_CP("maxCp", new MaxCpFinalizer()),
	MAX_RECOVERABLE_HP("maxRecoverableHp"), // The maximum HP that is able to be recovered trough heals
	MAX_RECOVERABLE_MP("maxRecoverableMp"),
	MAX_RECOVERABLE_CP("maxRecoverableCp"),
	REGENERATE_HP_RATE("regHp", new RegenHPFinalizer()),
	REGENERATE_CP_RATE("regCp", new RegenCPFinalizer()),
	REGENERATE_MP_RATE("regMp", new RegenMPFinalizer()),
	MANA_CHARGE("manaCharge"),
	HEAL_EFFECT("healEffect"),
	
	// ATTACK & DEFENCE
	PHYSICAL_DEFENCE("pDef", new PDefenseFinalizer()),
	MAGICAL_DEFENCE("mDef", new MDefenseFinalizer()),
	PHYSICAL_ATTACK("pAtk", new PAttackFinalizer()),
	MAGIC_ATTACK("mAtk", new MAttackFinalizer()),
	PHYSICAL_ATTACK_SPEED("pAtkSpd", new PAttackSpeedFinalizer()),
	MAGIC_ATTACK_SPEED("mAtkSpd", new MAttackSpeedFinalizer()), // Magic Skill Casting Time Rate
	ATK_REUSE("atkReuse"), // Bows Hits Reuse Rate
	SHIELD_DEFENCE("sDef"),
	CRITICAL_DAMAGE("cAtk"),
	CRITICAL_DAMAGE_ADD("cAtkAdd"), // this is another type for special critical damage mods - vicious stance, critical power and critical damage SA
	HATE_ATTACK("attackHate"),
	
	// PVP BONUS
	PVP_PHYSICAL_ATTACK_DAMAGE("pvpPhysDmg"),
	PVP_MAGICAL_SKILL_DAMAGE("pvpMagicalDmg"),
	PVP_PHYSICAL_SKILL_DAMAGE("pvpPhysSkillsDmg"),
	PVP_PHYSICAL_ATTACK_DEFENCE("pvpPhysDef"),
	PVP_MAGICAL_SKILL_DEFENCE("pvpMagicalDef"),
	PVP_PHYSICAL_SKILL_DEFENCE("pvpPhysSkillsDef"),
	
	// PVE BONUS
	PVE_PHYSICAL_ATTACK_DAMAGE("pvePhysDmg"),
	PVE_PHYSICAL_SKILL_DAMAGE("pvePhysSkillsDmg"),
	PVE_MAGICAL_SKILL_DAMAGE("pveMagicalDmg"),
	PVE_PHYSICAL_ATTACK_DEFENCE("pvePhysDef"),
	PVE_PHYSICAL_SKILL_DEFENCE("pvePhysSkillsDef"),
	PVE_MAGICAL_SKILL_DEFENCE("pveMagicalDef"),
	PVE_RAID_PHYSICAL_ATTACK_DEFENCE("pveRaidPhysDef"),
	PVE_RAID_PHYSICAL_SKILL_DEFENCE("pveRaidPhysSkillsDef"),
	PVE_RAID_MAGICAL_SKILL_DEFENCE("pveRaidMagicalDef"),
	
	// ATTACK & DEFENCE RATES
	MAGIC_CRITICAL_DAMAGE("mCritPower"),
	PHYSICAL_SKILL_POWER("physicalSkillPower"), // Adding skill power (not multipliers) results in points added directly to final value unmodified by defence, traits, elements, criticals etc.
												// Even when damage is 0 due to general trait immune multiplier, added skill power is active and clearly visible (damage not being 0 but at the value of added skill power).
	CRITICAL_DAMAGE_SKILL("cAtkSkill"),
	CRITICAL_DAMAGE_SKILL_ADD("cAtkSkillAdd"),
	MAGIC_CRITICAL_DAMAGE_ADD("mCritPowerAdd"),
	SHIELD_DEFENCE_RATE("rShld"),
	CRITICAL_RATE("rCrit", new PCriticalRateFinalizer(), MathUtil::add, MathUtil::add, null, 1d),
	CRITICAL_RATE_SKILL("rCritSkill", Stats::defaultValue, MathUtil::add, MathUtil::add, null, 1d),
	MAGIC_CRITICAL_RATE("mCritRate", new MCritRateFinalizer()),
	BLOW_RATE("blowRate"),
	DEFENCE_CRITICAL_RATE("defCritRate"),
	DEFENCE_CRITICAL_RATE_ADD("defCritRateAdd"),
	DEFENCE_MAGIC_CRITICAL_RATE("defMCritRate"),
	DEFENCE_MAGIC_CRITICAL_RATE_ADD("defMCritRateAdd"),
	DEFENCE_CRITICAL_DAMAGE("defCritDamage"),
	DEFENCE_MAGIC_CRITICAL_DAMAGE("defMCritDamage"),
	DEFENCE_MAGIC_CRITICAL_DAMAGE_ADD("defMCritDamageAdd"),
	DEFENCE_CRITICAL_DAMAGE_ADD("defCritDamageAdd"), // Resistance to critical damage in value (Example: +100 will be 100 more critical damage, NOT 100% more).
	DEFENCE_CRITICAL_DAMAGE_SKILL("defCAtkSkill"),
	DEFENCE_CRITICAL_DAMAGE_SKILL_ADD("defCAtkSkillAdd"),
	INSTANT_KILL_RESIST("instantKillResist"),
	EXPSP_RATE("rExp"),
	BONUS_EXP("bonusExp"),
	BONUS_SP("bonusSp"),
	ATTACK_CANCEL("cancel"),
	
	// ACCURACY & RANGE
	ACCURACY_COMBAT("accCombat", new PAccuracyFinalizer()),
	ACCURACY_MAGIC("accMagic", new MAccuracyFinalizer()),
	EVASION_RATE("rEvas", new PEvasionRateFinalizer()),
	MAGIC_EVASION_RATE("mEvas", new MEvasionRateFinalizer()),
	PHYSICAL_ATTACK_RANGE("pAtkRange", new PRangeFinalizer()),
	MAGIC_ATTACK_RANGE("mAtkRange"),
	ATTACK_COUNT_MAX("atkCountMax"),
	// Run speed, walk & escape speed are calculated proportionally, magic speed is a buff
	MOVE_SPEED("moveSpeed"),
	RUN_SPEED("runSpd", new SpeedFinalizer()),
	WALK_SPEED("walkSpd", new SpeedFinalizer()),
	SWIM_RUN_SPEED("fastSwimSpd", new SpeedFinalizer()),
	SWIM_WALK_SPEED("slowSimSpd", new SpeedFinalizer()),
	FLY_RUN_SPEED("fastFlySpd", new SpeedFinalizer()),
	FLY_WALK_SPEED("slowFlySpd", new SpeedFinalizer()),
	
	// BASIC STATS
	STAT_STR("STR", new BaseStatsFinalizer()),
	STAT_CON("CON", new BaseStatsFinalizer()),
	STAT_DEX("DEX", new BaseStatsFinalizer()),
	STAT_INT("INT", new BaseStatsFinalizer()),
	STAT_WIT("WIT", new BaseStatsFinalizer()),
	STAT_MEN("MEN", new BaseStatsFinalizer()),
	STAT_LUC("LUC", new BaseStatsFinalizer()),
	STAT_CHA("CHA", new BaseStatsFinalizer()),
	
	// Special stats, share one slot in Calculator
	
	// VARIOUS
	BREATH("breath"),
	FALL("fall"),
	
	// VULNERABILITIES
	DAMAGE_ZONE_VULN("damageZoneVuln"),
	RESIST_DISPEL_BUFF("cancelVuln"), // Resistance for cancel type skills
	RESIST_ABNORMAL_DEBUFF("debuffVuln"),
	
	// RESISTANCES
	FIRE_RES("fireRes", new AttributeFinalizer(AttributeType.FIRE, false)),
	WIND_RES("windRes", new AttributeFinalizer(AttributeType.WIND, false)),
	WATER_RES("waterRes", new AttributeFinalizer(AttributeType.WATER, false)),
	EARTH_RES("earthRes", new AttributeFinalizer(AttributeType.EARTH, false)),
	HOLY_RES("holyRes", new AttributeFinalizer(AttributeType.HOLY, false)),
	DARK_RES("darkRes", new AttributeFinalizer(AttributeType.DARK, false)),
	BASE_ATTRIBUTE_RES("baseAttrRes"),
	MAGIC_SUCCESS_RES("magicSuccRes"),
	// BUFF_IMMUNITY("buffImmunity"), //TODO: Implement me
	ABNORMAL_RESIST_PHYSICAL("abnormalResPhysical"),
	ABNORMAL_RESIST_MAGICAL("abnormalResMagical"),
	
	// ELEMENT POWER
	FIRE_POWER("firePower", new AttributeFinalizer(AttributeType.FIRE, true)),
	WATER_POWER("waterPower", new AttributeFinalizer(AttributeType.WATER, true)),
	WIND_POWER("windPower", new AttributeFinalizer(AttributeType.WIND, true)),
	EARTH_POWER("earthPower", new AttributeFinalizer(AttributeType.EARTH, true)),
	HOLY_POWER("holyPower", new AttributeFinalizer(AttributeType.HOLY, true)),
	DARK_POWER("darkPower", new AttributeFinalizer(AttributeType.DARK, true)),
	
	// PROFICIENCY
	REFLECT_DAMAGE_PERCENT("reflectDam"),
	REFLECT_DAMAGE_PERCENT_DEFENSE("reflectDamDef"),
	REFLECT_SKILL_MAGIC("reflectSkillMagic"), // Need rework
	REFLECT_SKILL_PHYSIC("reflectSkillPhysic"), // Need rework
	VENGEANCE_SKILL_MAGIC_DAMAGE("vengeanceMdam"),
	VENGEANCE_SKILL_PHYSICAL_DAMAGE("vengeancePdam"),
	ABSORB_DAMAGE_PERCENT("absorbDam"),
	ABSORB_DAMAGE_CHANCE("absorbDamChance", new VampiricChanceFinalizer()),
	ABSORB_DAMAGE_DEFENCE("absorbDamDefence"),
	TRANSFER_DAMAGE_SUMMON_PERCENT("transDam"),
	MANA_SHIELD_PERCENT("manaShield"),
	TRANSFER_DAMAGE_TO_PLAYER("transDamToPlayer"),
	ABSORB_MANA_DAMAGE_PERCENT("absorbDamMana"),
	
	WEIGHT_LIMIT("weightLimit"),
	WEIGHT_PENALTY("weightPenalty"),
	
	// ExSkill
	INVENTORY_NORMAL("inventoryLimit"),
	STORAGE_PRIVATE("whLimit"),
	TRADE_SELL("PrivateSellLimit"),
	TRADE_BUY("PrivateBuyLimit"),
	RECIPE_DWARVEN("DwarfRecipeLimit"),
	RECIPE_COMMON("CommonRecipeLimit"),
	
	// Skill mastery
	SKILL_CRITICAL("skillCritical"),
	SKILL_CRITICAL_PROBABILITY("skillCriticalProbability"),
	
	// Vitality
	VITALITY_CONSUME_RATE("vitalityConsumeRate"),
	
	// Souls
	MAX_SOULS("maxSouls"),
	
	REDUCE_EXP_LOST_BY_PVP("reduceExpLostByPvp"),
	REDUCE_EXP_LOST_BY_MOB("reduceExpLostByMob"),
	REDUCE_EXP_LOST_BY_RAID("reduceExpLostByRaid"),
	
	REDUCE_DEATH_PENALTY_BY_PVP("reduceDeathPenaltyByPvp"),
	REDUCE_DEATH_PENALTY_BY_MOB("reduceDeathPenaltyByMob"),
	REDUCE_DEATH_PENALTY_BY_RAID("reduceDeathPenaltyByRaid"),
	
	// Brooches
	BROOCH_JEWELS("broochJewels"),
	
	// Summon Points
	MAX_SUMMON_POINTS("summonPoints"),
	
	// Cubic Count
	MAX_CUBIC("cubicCount"),
	
	// The maximum allowed range to be damaged/debuffed from.
	SPHERIC_BARRIER_RANGE("sphericBarrier"),
	
	// Blocks given amount of debuffs.
	DEBUFF_BLOCK("debuffBlock"),
	
	// Affects the random weapon damage.
	RANDOM_DAMAGE("randomDamage", new RandomDamageFinalizer()),
	
	// Affects the random weapon damage.
	DAMAGE_LIMIT("damageCap"),
	
	// Maximun momentum one can charge
	MAX_MOMENTUM("maxMomentum"),
	
	// Which base stat ordinal should alter skill critical formula.
	STAT_BONUS_SKILL_CRITICAL("statSkillCritical"),
	STAT_BONUS_SPEED("statSpeed"),
	SHOTS_BONUS("shotBonus", new ShotsBonusFinalizer());
	
	static final Logger LOGGER = Logger.getLogger(Stats.class.getName());
	public static final int NUM_STATS = values().length;
	
	private final String _value;
	private final IStatsFunction _valueFinalizer;
	private final BiFunction<Double, Double, Double> _addFunction;
	private final BiFunction<Double, Double, Double> _mulFunction;
	private final Double _resetAddValue;
	private final Double _resetMulValue;
	
	public String getValue()
	{
		return _value;
	}
	
	Stats(String xmlString)
	{
		this(xmlString, Stats::defaultValue, MathUtil::add, MathUtil::mul, null, null);
	}
	
	Stats(String xmlString, IStatsFunction valueFinalizer)
	{
		this(xmlString, valueFinalizer, MathUtil::add, MathUtil::mul, null, null);
		
	}
	
	Stats(String xmlString, IStatsFunction valueFinalizer, BiFunction<Double, Double, Double> addFunction, BiFunction<Double, Double, Double> mulFunction, Double resetAddValue, Double resetMulValue)
	{
		_value = xmlString;
		_valueFinalizer = valueFinalizer;
		_addFunction = addFunction;
		_mulFunction = mulFunction;
		_resetAddValue = resetAddValue;
		_resetMulValue = resetMulValue;
	}
	
	public static Stats valueOfXml(String name)
	{
		name = name.intern();
		for (Stats s : values())
		{
			if (s.getValue().equals(name))
			{
				return s;
			}
		}
		
		throw new NoSuchElementException("Unknown name '" + name + "' for enum " + Stats.class.getSimpleName());
	}
	
	/**
	 * @param creature
	 * @param baseValue
	 * @return the final value
	 */
	public Double finalize(L2Character creature, Optional<Double> baseValue)
	{
		try
		{
			return _valueFinalizer.calc(creature, baseValue, this);
		}
		catch (Exception e)
		{
			// LOGGER.log(Level.WARNING, "Exception during finalization for : " + creature + " stat: " + toString() + " : ", e);
			return defaultValue(creature, baseValue, this);
		}
	}
	
	public double functionAdd(double oldValue, double value)
	{
		return _addFunction.apply(oldValue, value);
	}
	
	public double functionMul(double oldValue, double value)
	{
		return _mulFunction.apply(oldValue, value);
	}
	
	public Double getResetAddValue()
	{
		return _resetAddValue;
	}
	
	public Double getResetMulValue()
	{
		return _resetMulValue;
	}
	
	public static double weaponBaseValue(L2Character creature, Stats stat)
	{
		return stat._valueFinalizer.calcWeaponBaseValue(creature, stat);
	}
	
	public static double defaultValue(L2Character creature, Optional<Double> base, Stats stat)
	{
		final double mul = creature.getStat().getMul(stat);
		final double add = creature.getStat().getAdd(stat);
		return base.isPresent() ? defaultValue(creature, stat, base.get()) : mul * (add + creature.getStat().getMoveTypeValue(stat, creature.getMoveType()));
	}
	
	public static double defaultValue(L2Character creature, Stats stat, double baseValue)
	{
		final double mul = creature.getStat().getMul(stat);
		final double add = creature.getStat().getAdd(stat);
		return (baseValue * mul) + add + creature.getStat().getMoveTypeValue(stat, creature.getMoveType());
	}
}
