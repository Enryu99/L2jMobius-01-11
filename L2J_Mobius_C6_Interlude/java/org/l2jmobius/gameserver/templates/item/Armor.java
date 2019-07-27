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
package org.l2jmobius.gameserver.templates.item;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.datatables.SkillTable;
import org.l2jmobius.gameserver.model.Skill;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.ItemInstance;
import org.l2jmobius.gameserver.skills.Env;
import org.l2jmobius.gameserver.skills.funcs.Func;
import org.l2jmobius.gameserver.skills.funcs.FuncTemplate;
import org.l2jmobius.gameserver.templates.StatsSet;

/**
 * This class is dedicated to the management of armors.
 * @version $Revision: 1.2.2.1.2.6 $ $Date: 2005/03/27 15:30:10 $
 */
public final class Armor extends Item
{
	private final int _avoidModifier;
	private final int _pDef;
	private final int _mDef;
	private final int _mpBonus;
	private final int _hpBonus;
	private Skill _itemSkill = null; // for passive skill
	
	/**
	 * Constructor for Armor.<BR>
	 * <BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_avoidModifier</LI>
	 * <LI>_pDef & _mDef</LI>
	 * <LI>_mpBonus & _hpBonus</LI>
	 * @param type : ArmorType designating the type of armor
	 * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see Item constructor
	 */
	public Armor(ArmorType type, StatsSet set)
	{
		super(type, set);
		_avoidModifier = set.getInteger("avoid_modify");
		_pDef = set.getInteger("p_def");
		_mDef = set.getInteger("m_def");
		_mpBonus = set.getInteger("mp_bonus", 0);
		_hpBonus = set.getInteger("hp_bonus", 0);
		
		final int sId = set.getInteger("item_skill_id");
		final int sLv = set.getInteger("item_skill_lvl");
		if ((sId > 0) && (sLv > 0))
		{
			_itemSkill = SkillTable.getInstance().getInfo(sId, sLv);
		}
	}
	
	/**
	 * Returns the type of the armor.
	 * @return ArmorType
	 */
	@Override
	public ArmorType getItemType()
	{
		return (ArmorType) super._type;
	}
	
	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * Returns the magical defense of the armor
	 * @return int : value of the magic defense
	 */
	public final int getMDef()
	{
		return _mDef;
	}
	
	/**
	 * Returns the physical defense of the armor
	 * @return int : value of the physical defense
	 */
	public final int getPDef()
	{
		return _pDef;
	}
	
	/**
	 * Returns avoid modifier given by the armor
	 * @return int : avoid modifier
	 */
	public final int getAvoidModifier()
	{
		return _avoidModifier;
	}
	
	/**
	 * Returns magical bonus given by the armor
	 * @return int : value of the magical bonus
	 */
	public final int getMpBonus()
	{
		return _mpBonus;
	}
	
	/**
	 * Returns physical bonus given by the armor
	 * @return int : value of the physical bonus
	 */
	public final int getHpBonus()
	{
		return _hpBonus;
	}
	
	/**
	 * Returns passive skill linked to that armor
	 * @return
	 */
	public Skill getSkill()
	{
		return _itemSkill;
	}
	
	/**
	 * Returns array of Func objects containing the list of functions used by the armor
	 * @param instance : ItemInstance pointing out the armor
	 * @param creature : Creature pointing out the player
	 * @return Func[] : array of functions
	 */
	@Override
	public Func[] getStatFuncs(ItemInstance instance, Creature creature)
	{
		final List<Func> funcs = new ArrayList<>();
		if (_funcTemplates != null)
		{
			for (FuncTemplate t : _funcTemplates)
			{
				final Env env = new Env();
				env.player = creature;
				env.item = instance;
				final Func f = t.getFunc(env, instance);
				if (f != null)
				{
					funcs.add(f);
				}
			}
		}
		return funcs.toArray(new Func[funcs.size()]);
	}
}
