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
package com.l2jmobius.gameserver.handler.itemhandlers;

import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.handler.IItemHandler;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PlayableInstance;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * This class ...
 * @version $Revision: 1.0.0.0.0.0 $ $Date: 2005/09/02 19:41:13 $
 */

public class Firework implements IItemHandler
{
	// Modified by Baghak (Prograsso): Added Firework support
	private static int[] _itemIds =
	{
		6403,
		6406,
		6407
	};
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return; // prevent Class cast exception
		}
		
		final L2PcInstance activeChar = (L2PcInstance) playable;
		final int itemId = item.getItemId();
		
		if (!activeChar.getFloodProtectors().getFirework().tryPerformAction("firework"))
		{
			return;
		}
		
		/*
		 * Elven Firecracker
		 */
		if (itemId == 6403) // elven_firecracker, xml: 2023
		{
			final MagicSkillUse MSU = new MagicSkillUse(playable, activeChar, 2023, 1, 1, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			useFw(activeChar, item, 2023, 1);
			
		}
		
		/*
		 * Firework
		 */
		else if (itemId == 6406) // firework, xml: 2024
		{
			final MagicSkillUse MSU = new MagicSkillUse(playable, activeChar, 2024, 1, 1, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			useFw(activeChar, item, 2024, 1);
			
		}
		
		/*
		 * Lage Firework
		 */
		else if (itemId == 6407) // large_firework, xml: 2025
		{
			final MagicSkillUse MSU = new MagicSkillUse(playable, activeChar, 2025, 1, 1, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			useFw(activeChar, item, 2025, 1);
			
		}
	}
	
	public void useFw(L2PcInstance activeChar, L2ItemInstance item, int magicId, int level)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
		if (skill != null)
		{
			if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
			{
				return;
			}
			
			activeChar.useMagic(skill, false, false);
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return _itemIds;
	}
}