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
package org.l2jmobius.gameserver.handler.skillhandlers;

import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.ISkillHandler;
import org.l2jmobius.gameserver.model.Skill;
import org.l2jmobius.gameserver.model.Skill.SkillType;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Attackable.RewardItem;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author _drunk_ TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code Templates
 */
public class Sweep implements ISkillHandler
{
	private static final SkillType[] SKILL_TYPES =
	{
		SkillType.SWEEP
	};
	
	@Override
	public void useSkill(Creature creature, Skill skill, List<Creature> targets)
	{
		if (!(creature instanceof PlayerInstance))
		{
			return;
		}
		
		final PlayerInstance player = (PlayerInstance) creature;
		final InventoryUpdate iu = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		boolean send = false;
		for (WorldObject target1 : targets)
		{
			if (!(target1 instanceof Attackable))
			{
				continue;
			}
			
			final Attackable target = (Attackable) target1;
			List<RewardItem> items = null;
			boolean isSweeping = false;
			synchronized (target)
			{
				if (target.isSweepActive())
				{
					items = target.takeSweep();
					isSweeping = true;
				}
			}
			
			if (isSweeping)
			{
				if ((items == null) || items.isEmpty())
				{
					continue;
				}
				for (Attackable.RewardItem ritem : items)
				{
					if (player.isInParty())
					{
						player.getParty().distributeItem(player, ritem, true, target);
					}
					else
					{
						final ItemInstance item = player.getInventory().addItem("Sweep", ritem.getItemId(), ritem.getCount(), player, target);
						if (iu != null)
						{
							iu.addItem(item);
						}
						send = true;
						SystemMessage smsg;
						if (ritem.getCount() > 1)
						{
							smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
							smsg.addItemName(ritem.getItemId());
							smsg.addNumber(ritem.getCount());
						}
						else
						{
							smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
							smsg.addItemName(ritem.getItemId());
						}
						player.sendPacket(smsg);
					}
				}
			}
			target.endDecayTask();
			
			if (send)
			{
				if (iu != null)
				{
					player.sendPacket(iu);
				}
				else
				{
					player.sendPacket(new ItemList(player, false));
				}
			}
		}
	}
	
	@Override
	public SkillType[] getSkillTypes()
	{
		return SKILL_TYPES;
	}
}
