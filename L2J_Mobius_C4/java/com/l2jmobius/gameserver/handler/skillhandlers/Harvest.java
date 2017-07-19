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

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.handler.ISkillHandler;
import com.l2jmobius.gameserver.model.L2Attackable;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Skill.SkillType;
import com.l2jmobius.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.ItemList;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.util.Rnd;

/**
 * @author l3x
 */
public class Harvest implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.HARVEST
	};
	
	private L2PcInstance _activeChar;
	private L2MonsterInstance _target;
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets, boolean crit)
	{
		if (!(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		_activeChar = (L2PcInstance) activeChar;
		
		final L2Object[] targetList = skill.getTargetList(activeChar);
		
		final InventoryUpdate iu = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		
		if (targetList == null)
		{
			return;
		}
		
		for (int index = 0; index < targetList.length; index++)
		{
			if (!(targetList[index] instanceof L2MonsterInstance))
			{
				continue;
			}
			
			_target = (L2MonsterInstance) targetList[index];
			
			if (_activeChar != _target.getSeeder())
			{
				final SystemMessage sm = new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED_TO_HARVEST);
				_activeChar.sendPacket(sm);
				continue;
			}
			
			boolean send = false;
			int total = 0;
			int cropId = 0;
			
			// TODO: check items and amount of items player harvest
			if (_target.isSeeded())
			{
				if (calcSuccess())
				{
					final L2Attackable.RewardItem[] items = _target.takeHarvest();
					if ((items != null) && (items.length > 0))
					{
						for (final L2Attackable.RewardItem ritem : items)
						{
							cropId = ritem.getItemId(); // always got 1 type of crop as reward
							if (_activeChar.isInParty())
							{
								_activeChar.getParty().distributeItem(_activeChar, ritem, true, _target);
							}
							else
							{
								final L2ItemInstance item = _activeChar.getInventory().addItem("Manor", ritem.getItemId(), ritem.getCount(), _activeChar, _target);
								if (iu != null)
								{
									iu.addItem(item);
								}
								
								send = true;
								total += ritem.getCount();
							}
						}
						
						if (send)
						{
							SystemMessage smsg = new SystemMessage(SystemMessage.YOU_PICKED_UP_S1_S2);
							smsg.addNumber(total);
							smsg.addItemName(cropId);
							_activeChar.sendPacket(smsg);
							
							if (_activeChar.getParty() != null)
							{
								smsg = new SystemMessage(SystemMessage.S1_HARVESTED_S3_S2S);
								smsg.addString(_activeChar.getName());
								smsg.addNumber(total);
								smsg.addItemName(cropId);
								_activeChar.getParty().broadcastToPartyMembers(_activeChar, smsg);
							}
							
							if (iu != null)
							{
								_activeChar.sendPacket(iu);
							}
							else
							{
								_activeChar.sendPacket(new ItemList(_activeChar, false));
							}
						}
					}
				}
				else
				{
					_activeChar.sendPacket(new SystemMessage(SystemMessage.THE_HARVEST_HAS_FAILED));
				}
			}
			else
			{
				_activeChar.sendPacket(new SystemMessage(SystemMessage.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN));
			}
		}
	}
	
	private boolean calcSuccess()
	{
		int basicSuccess = 100;
		final int levelPlayer = _activeChar.getLevel();
		final int levelTarget = _target.getLevel();
		
		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
		{
			diff = -diff;
		}
		
		// apply penalty, target <=> player levels
		// 5% penalty for each level
		if (diff > 5)
		{
			basicSuccess -= (diff - 5) * 5;
		}
		
		// success rate cant be less than 1%
		if (basicSuccess < 1)
		{
			basicSuccess = 1;
		}
		
		final int rate = Rnd.nextInt(99);
		
		if (rate < basicSuccess)
		{
			return true;
		}
		
		return false;
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}