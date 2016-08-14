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
package com.l2jmobius.gameserver.network.serverpackets;

import com.l2jmobius.Config;
import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.data.xml.impl.EnchantSkillGroupsData;
import com.l2jmobius.gameserver.data.xml.impl.SkillData;
import com.l2jmobius.gameserver.model.L2EnchantSkillGroup.EnchantSkillHolder;
import com.l2jmobius.gameserver.model.L2EnchantSkillLearn;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.itemcontainer.Inventory;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

/**
 * @author KenM
 */
public class ExEnchantSkillInfoDetail implements IClientOutgoingPacket
{
	private static final int TYPE_NORMAL_ENCHANT = 0;
	private static final int TYPE_SAFE_ENCHANT = 1;
	private static final int TYPE_UNTRAIN_ENCHANT = 2;
	private static final int TYPE_CHANGE_ENCHANT = 3;
	private static final int TYPE_IMMORTAL_ENCHANT = 4;
	
	private int bookId = 0;
	private int reqCount = 0;
	private int multi = 1;
	private final int _type;
	private final int _skillid;
	private final int _skilllvl;
	private final int _maxlvl;
	private final int _chance;
	private int _sp;
	private final int _adenacount;
	
	public ExEnchantSkillInfoDetail(int type, int skillid, int skilllvl, L2PcInstance ply)
	{
		_type = type;
		_skillid = skillid;
		_skilllvl = skilllvl;
		_maxlvl = SkillData.getInstance().getMaxLevel(_skillid);
		
		final L2EnchantSkillLearn enchantLearn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillid);
		EnchantSkillHolder esd = null;
		// do we have this skill?
		if (enchantLearn != null)
		{
			if (_skilllvl > 1000)
			{
				esd = enchantLearn.getEnchantSkillHolder(_skilllvl);
			}
			else
			{
				esd = enchantLearn.getFirstRouteGroup().getEnchantGroupDetails().get(0);
			}
		}
		
		if (esd == null)
		{
			throw new IllegalArgumentException("Skill " + skillid + " dont have enchant data for level " + _skilllvl);
		}
		
		if (type == 0)
		{
			multi = EnchantSkillGroupsData.NORMAL_ENCHANT_COST_MULTIPLIER;
		}
		else if (type == 1)
		{
			multi = EnchantSkillGroupsData.SAFE_ENCHANT_COST_MULTIPLIER;
		}
		if (type != TYPE_IMMORTAL_ENCHANT)
		{
			_chance = esd.getRate(ply);
			_sp = esd.getSpCost();
			if (type == TYPE_UNTRAIN_ENCHANT)
			{
				_sp = (int) (0.8 * _sp);
			}
			_adenacount = esd.getAdenaCost() * multi;
		}
		else
		{
			_chance = 100;
			_sp = 0;
			_adenacount = 0;
		}
		
		final int _elvl = ((_skilllvl % 100) - 1) / 10;
		switch (type)
		{
			case TYPE_NORMAL_ENCHANT:
			{
				if (ply.getClassId().level() < 4)
				{
					bookId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK_OLD;
				}
				else if (_elvl == 0)
				{
					bookId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK;
				}
				else if (_elvl == 1)
				{
					bookId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK_V2;
				}
				else
				{
					bookId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK_V3;
				}
				reqCount = 1;
				break;
			}
			case TYPE_SAFE_ENCHANT:
			{
				if (ply.getClassId().level() < 4)
				{
					bookId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK_OLD;
				}
				else if (_elvl == 0)
				{
					bookId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK;
				}
				else if (_elvl == 1)
				{
					bookId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK_V2;
				}
				else
				{
					bookId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK_V3;
				}
				reqCount = 1;
				break;
			}
			case TYPE_CHANGE_ENCHANT:
			{
				if (ply.getClassId().level() < 4)
				{
					bookId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK_OLD;
				}
				else if (_elvl == 0)
				{
					bookId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK;
				}
				else if (_elvl == 1)
				{
					bookId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK_V2;
				}
				else
				{
					bookId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK_V3;
				}
				reqCount = 1;
				break;
			}
			case TYPE_IMMORTAL_ENCHANT:
			{
				if (ply.getClassId().level() < 4)
				{
					bookId = EnchantSkillGroupsData.IMMORTAL_SCROLL;
				}
				else if (_elvl == 0)
				{
					bookId = EnchantSkillGroupsData.IMMORTAL_SCROLL;
				}
				else if (_elvl == 1)
				{
					bookId = EnchantSkillGroupsData.IMMORTAL_SCROLL_V2;
				}
				else
				{
					bookId = EnchantSkillGroupsData.IMMORTAL_SCROLL_V3;
				}
				reqCount = 1;
				break;
			}
			default:
			{
				return;
			}
		}
		
		if ((type != TYPE_SAFE_ENCHANT) && !Config.ES_SP_BOOK_NEEDED)
		{
			reqCount = 0;
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_ENCHANT_SKILL_INFO_DETAIL.writeId(packet);
		
		packet.writeD(_type);
		packet.writeD(_skillid);
		packet.writeH(_maxlvl);
		packet.writeH(_skilllvl);
		packet.writeQ(_sp * multi); // sp
		packet.writeD(_chance); // exp
		packet.writeD(0x02); // items count?
		packet.writeD(Inventory.ADENA_ID); // Adena
		packet.writeD(_adenacount); // Adena count
		packet.writeD(bookId); // ItemId Required
		packet.writeD(reqCount);
		return true;
	}
}
