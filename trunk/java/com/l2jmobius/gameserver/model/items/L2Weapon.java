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
package com.l2jmobius.gameserver.model.items;

import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.enums.ItemSkillType;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.StatsSet;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.events.EventDispatcher;
import com.l2jmobius.gameserver.model.events.impl.character.npc.OnNpcSkillSee;
import com.l2jmobius.gameserver.model.items.type.WeaponType;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.stats.Formulas;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.util.Util;

/**
 * This class is dedicated to the management of weapons.
 */
public final class L2Weapon extends L2Item
{
	private WeaponType _type;
	private boolean _isMagicWeapon;
	private int _soulShotCount;
	private int _spiritShotCount;
	private int _mpConsume;
	private int _baseAttackRange;
	private int _baseAttackRadius;
	private int _baseAttackAngle;
	private int _changeWeaponId;
	
	private int _reducedSoulshot;
	private int _reducedSoulshotChance;
	
	private int _reducedMpConsume;
	private int _reducedMpConsumeChance;
	
	private boolean _isForceEquip;
	private boolean _isAttackWeapon;
	private boolean _useWeaponSkillsOnly;
	
	/**
	 * Constructor for Weapon.
	 * @param set the StatsSet designating the set of couples (key,value) characterizing the weapon.
	 */
	public L2Weapon(StatsSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatsSet set)
	{
		super.set(set);
		_type = WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
		_type1 = L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
		_type2 = L2Item.TYPE2_WEAPON;
		_isMagicWeapon = set.getBoolean("is_magic_weapon", false);
		_soulShotCount = set.getInt("soulshots", 0);
		_spiritShotCount = set.getInt("spiritshots", 0);
		_mpConsume = set.getInt("mp_consume", 0);
		_baseAttackRange = set.getInt("attack_range", 40);
		final String[] damgeRange = set.getString("damage_range", "").split(";"); // 0?;0?;fan sector;base attack angle
		if ((damgeRange.length > 1) && Util.isDigit(damgeRange[2]) && Util.isDigit(damgeRange[3]))
		{
			_baseAttackRadius = Integer.parseInt(damgeRange[2]);
			_baseAttackAngle = Integer.parseInt(damgeRange[3]);
		}
		else
		{
			_baseAttackRadius = 40;
			_baseAttackAngle = 120;
		}
		
		final String[] reduced_soulshots = set.getString("reduced_soulshot", "").split(",");
		_reducedSoulshotChance = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[0]) : 0;
		_reducedSoulshot = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[1]) : 0;
		
		final String[] reduced_mpconsume = set.getString("reduced_mp_consume", "").split(",");
		_reducedMpConsumeChance = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[0]) : 0;
		_reducedMpConsume = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[1]) : 0;
		
		_changeWeaponId = set.getInt("change_weaponId", 0);
		_isForceEquip = set.getBoolean("isForceEquip", false);
		_isAttackWeapon = set.getBoolean("isAttackWeapon", true);
		_useWeaponSkillsOnly = set.getBoolean("useWeaponSkillsOnly", false);
	}
	
	/**
	 * @return the type of Weapon
	 */
	@Override
	public WeaponType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the Etc item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * @return {@code true} if the weapon is magic, {@code false} otherwise.
	 */
	@Override
	public boolean isMagicWeapon()
	{
		return _isMagicWeapon;
	}
	
	/**
	 * @return the quantity of SoulShot used.
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}
	
	/**
	 * @return the quantity of SpiritShot used.
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}
	
	/**
	 * @return the reduced quantity of SoultShot used.
	 */
	public int getReducedSoulShot()
	{
		return _reducedSoulshot;
	}
	
	/**
	 * @return the chance to use Reduced SoultShot.
	 */
	public int getReducedSoulShotChance()
	{
		return _reducedSoulshotChance;
	}
	
	/**
	 * @return the MP consumption with the weapon.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	public int getBaseAttackRadius()
	{
		return _baseAttackRadius;
	}
	
	public int getBaseAttackAngle()
	{
		return _baseAttackAngle;
	}
	
	/**
	 * @return the reduced MP consumption with the weapon.
	 */
	public int getReducedMpConsume()
	{
		return _reducedMpConsume;
	}
	
	/**
	 * @return the chance to use getReducedMpConsume()
	 */
	public int getReducedMpConsumeChance()
	{
		return _reducedMpConsumeChance;
	}
	
	/**
	 * @return the Id in which weapon this weapon can be changed.
	 */
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}
	
	/**
	 * @return {@code true} if the weapon is force equip, {@code false} otherwise.
	 */
	public boolean isForceEquip()
	{
		return _isForceEquip;
	}
	
	/**
	 * @return {@code true} if the weapon is attack weapon, {@code false} otherwise.
	 */
	public boolean isAttackWeapon()
	{
		return _isAttackWeapon;
	}
	
	/**
	 * @return {@code true} if the weapon is skills only, {@code false} otherwise.
	 */
	public boolean useWeaponSkillsOnly()
	{
		return _useWeaponSkillsOnly;
	}
	
	/**
	 * @param caster the L2Character pointing out the caster
	 * @param target the L2Character pointing out the target
	 * @param trigger
	 * @param type
	 */
	public void applyConditionalSkills(L2Character caster, L2Character target, Skill trigger, ItemSkillType type)
	{
		forEachSkill(type, holder ->
		{
			final Skill skill = holder.getSkill();
			if (Rnd.get(100) >= holder.getChance())
			{
				return;
			}
			
			if (type == ItemSkillType.ON_MAGIC_SKILL)
			{
				// Trigger only if both are good or bad magic.
				if (trigger.isBad() != skill.isBad())
				{
					return;
				}
				
				// No Trigger if not Magic Skill or is toggle
				if (trigger.isMagic() != skill.isMagic())
				{
					return;
				}
				
				// No Trigger if skill is toggle
				if (trigger.isToggle())
				{
					return;
				}
				
				if (skill.isBad() && (Formulas.calcShldUse(caster, target) == Formulas.SHIELD_DEFENSE_PERFECT_BLOCK))
				{
					return;
				}
			}
			
			// Skill condition not met
			if (!skill.checkCondition(caster, target))
			{
				return;
			}
			
			skill.activateSkill(caster, target);
			
			// TODO: Verify if this applies ONLY to ON_MAGIC_SKILL!
			if (type == ItemSkillType.ON_MAGIC_SKILL)
			{
				// notify quests of a skill use
				if (caster instanceof L2PcInstance)
				{
					L2World.getInstance().forEachVisibleObjectInRange(caster, L2Npc.class, 1000, npc ->
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npc, caster.getActingPlayer(), skill, false, target), npc);
					});
				}
				if (caster.isPlayer())
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED);
					sm.addSkillName(skill);
					caster.sendPacket(sm);
				}
			}
		});
	}
}
