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
package com.l2jmobius.gameserver.skills;

import java.util.NoSuchElementException;

/**
 * Enum of basic stats.
 * @author mkizub
 */
public enum Stats
{
	//
	// Base stats, for each in Calculator a slot is allocated
	//
	
	// HP & MP
	MAX_HP("maxHp"),
	MAX_MP("maxMp"),
	MAX_CP("maxCp"),
	REGENERATE_HP_RATE("regHp"),
	REGENERATE_CP_RATE("regCp"),
	REGENERATE_MP_RATE("regMp"),
	RECHARGE_MP_RATE("gainMp"),
	HEAL_EFFECTIVNESS("gainHp"),
	
	// Atk & Def
	POWER_DEFENCE("pDef"),
	MAGIC_DEFENCE("mDef"),
	POWER_ATTACK("pAtk"),
	MAGIC_ATTACK("mAtk"),
	POWER_ATTACK_SPEED("pAtkSpd"),
	MAGIC_ATTACK_SPEED("mAtkSpd"), // how fast a spell is casted (including animation)
	MAGIC_REUSE_RATE("mReuse"), // how fast spells becomes ready to reuse
	PHYSICAL_REUSE_RATE("pReuse"), // how fast physical skill becomes ready to reuse
	SHIELD_DEFENCE("sDef"),
	CRITICAL_DAMAGE("cAtk"),
	CRITICAL_DAMAGE_ADD("cAtkAdd"),
	PVP_PHYSICAL_DMG("pvpPhysDmg"),
	PVP_MAGICAL_DMG("pvpMagicalDmg"),
	PVP_PHYS_SKILL_DMG("pvpPhysSkillsDmg"),
	VALAKAS_PHYSICAL_DMG("valakasPhysDmg"),
	
	// Atk & Def rates
	EVASION_RATE("rEvas"),
	SHIELD_RATE("rShld"),
	SHIELD_DEFENCE_ANGLE("shieldDefAngle"),
	CRITICAL_RATE("rCrit"),
	MCRITICAL_RATE("mCritRate"),
	EXPSP_RATE("rExp"),
	BLOW_RATE("blowRate"),
	ATTACK_CANCEL("cancel"),
	
	// Accuracy and range
	ACCURACY_COMBAT("accCombat"),
	POWER_ATTACK_RANGE("pAtkRange"),
	MAGIC_ATTACK_RANGE("mAtkRange"),
	POWER_ATTACK_ANGLE("pAtkAngle"),
	ATTACK_COUNT_MAX("atkCountMax"),
	
	WALK_SPEED("walkSpd"),
	RUN_SPEED("runSpd"),
	
	//
	// Player-only stats
	//
	STAT_STR("STR"),
	STAT_CON("CON"),
	STAT_DEX("DEX"),
	STAT_INT("INT"),
	STAT_WIT("WIT"),
	STAT_MEN("MEN"),
	
	//
	// Special stats, share one slot in Calculator
	//
	
	// Water breath
	BREATH("breath"),
	
	// Terrain damage
	FALL("fall"),
	
	// Various stats
	AGGRESSION("aggression"), // locks a mob on tank caster
	BLEED("bleed"), // by daggers, like poison
	POISON("poison"), // by magic, hp dmg over time
	STUN("stun"), // disable move/ATTACK for a period of time
	ROOT("root"), // disable movement, but not ATTACK
	MOVEMENT("movement"), // slowdown movement, debuff
	CONFUSION("confusion"), // mob changes target, opposite to aggression/hate
	SLEEP("sleep"), // sleep (don't move/ATTACK) until attacked
	FIRE("fire"),
	WIND("wind"),
	WATER("water"),
	EARTH("earth"),
	HOLY("holy"),
	DARK("dark"),
	
	// Resists
	AGGRESSION_VULN("aggressionVuln"),
	BLEED_VULN("bleedVuln"),
	POISON_VULN("poisonVuln"),
	STUN_VULN("stunVuln"),
	PARALYZE_VULN("paralyzeVuln"),
	ROOT_VULN("rootVuln"),
	SLEEP_VULN("sleepVuln"),
	CONFUSION_VULN("confusionVuln"),
	MOVEMENT_VULN("movementVuln"),
	FIRE_VULN("fireVuln"),
	WIND_VULN("windVuln"),
	WATER_VULN("waterVuln"),
	EARTH_VULN("earthVuln"),
	HOLY_VULN("holyVuln"),
	DARK_VULN("darkVuln"),
	CANCEL_VULN("cancelVuln"),
	DERANGEMENT_VULN("derangementVuln"),
	DEBUFF_VULN("debuffVuln"),
	VALAKAS_VULN("valakasVuln"),
	
	NONE_WPN_VULN("noneWpnVuln"), // Shields!!!
	SWORD_WPN_VULN("swordWpnVuln"),
	BLUNT_WPN_VULN("bluntWpnVuln"),
	DAGGER_WPN_VULN("daggerWpnVuln"),
	BOW_WPN_VULN("bowWpnVuln"),
	POLE_WPN_VULN("poleWpnVuln"),
	ETC_WPN_VULN("etcWpnVuln"),
	FIST_WPN_VULN("fistWpnVuln"),
	DUAL_WPN_VULN("dualWpnVuln"),
	DUALFIST_WPN_VULN("dualFistWpnVuln"),
	
	REFLECT_DAMAGE_PERCENT("reflectDam"),
	REFLECT_SKILL_MAGIC("reflectSkillMagic"),
	REFLECT_SKILL_PHYSIC("reflectSkillPhysic"),
	ABSORB_DAMAGE_PERCENT("absorbDam"),
	TRANSFER_DAMAGE_PERCENT("transDam"),
	
	MAX_LOAD("maxLoad"),
	
	PATK_PLANTS("pAtk-plants"),
	PATK_INSECTS("pAtk-insects"),
	PATK_ANIMALS("pAtk-animals"),
	PATK_MONSTERS("pAtk-monsters"),
	PATK_DRAGONS("pAtk-dragons"),
	PATK_UNDEAD("pAtk-undead"),
	PATK_GIANTS("pAtk-giants"),
	PATK_MCREATURES("pAtk-mcreatures"),
	PDEF_UNDEAD("pDef-undead"),
	
	ATK_REUSE("atkReuse"),
	
	// ExSkill :)
	INV_LIM("inventoryLimit"),
	WH_LIM("whLimit"),
	FREIGHT_LIM("FreightLimit"),
	P_SELL_LIM("PrivateSellLimit"),
	P_BUY_LIM("PrivateBuyLimit"),
	REC_D_LIM("DwarfRecipeLimit"),
	REC_C_LIM("CommonRecipeLimit"),
	
	// C4 Stats
	PHYSICAL_MP_CONSUME_RATE("PhysicalMpConsumeRate"),
	MAGICAL_MP_CONSUME_RATE("MagicalMpConsumeRate"),
	HP_CONSUME_RATE("HpConsumeRate"),
	MP_CONSUME("MpConsume"),
	SOULSHOT_COUNT("soulShotCount"),
	
	// Skill mastery
	SKILL_MASTERY("skillMastery");
	
	public static final int NUM_STATS = values().length;
	
	private String _value;
	
	public String getValue()
	{
		return _value;
	}
	
	private Stats(String s)
	{
		_value = s;
	}
	
	public static Stats valueOfXml(String name)
	{
		name = name.intern();
		for (final Stats s : values())
		{
			if (s.getValue().equals(name))
			{
				return s;
			}
		}
		
		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}
}
