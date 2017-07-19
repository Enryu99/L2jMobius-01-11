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

import com.l2jmobius.gameserver.handler.IItemHandler;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PlayableInstance;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.templates.L2Item;
import com.l2jmobius.gameserver.templates.L2Weapon;
import com.l2jmobius.gameserver.templates.L2WeaponType;
import com.l2jmobius.gameserver.util.Broadcast;

/**
 * @author -Nemesiss-
 */
public class FishShots implements IItemHandler
{
	// All the item IDs that this handler knows.
	private static int[] _itemIds =
	{
		6535,
		6536,
		6537,
		6538,
		6539,
		6540
	};
	private static int[] _skillIds =
	{
		2181,
		2182,
		2183,
		2184,
		2185,
		2186
	};
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.handler.IItemHandler#useItem(com.l2jmobius.gameserver.model.L2PcInstance, com.l2jmobius.gameserver.model.L2ItemInstance)
	 */
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		final L2PcInstance activeChar = (L2PcInstance) playable;
		final L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		final L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		
		if ((weaponInst == null) || (weaponItem.getItemType() != L2WeaponType.ROD))
		{
			return;
		}
		
		if (weaponInst.getChargedFishshot())
		{
			// spiritshot is already active
			return;
		}
		
		final int FishshotId = item.getItemId();
		final int grade = weaponItem.getCrystalType();
		final int count = item.getCount();
		
		if (((grade == L2Item.CRYSTAL_NONE) && (FishshotId != 6535)) || ((grade == L2Item.CRYSTAL_D) && (FishshotId != 6536)) || ((grade == L2Item.CRYSTAL_C) && (FishshotId != 6537)) || ((grade == L2Item.CRYSTAL_B) && (FishshotId != 6538)) || ((grade == L2Item.CRYSTAL_A) && (FishshotId != 6539)) || ((grade == L2Item.CRYSTAL_S) && (FishshotId != 6540)))
		{
			// 1479 - This fishing shot is not fit for the fishing pole crystal.
			activeChar.sendPacket(new SystemMessage(SystemMessage.WRONG_FISHINGSHOT_GRADE));
			return;
		}
		
		if (count < 1)
		{
			return;
		}
		
		weaponInst.setChargedFishshot(true);
		activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false);
		final L2Object oldTarget = activeChar.getTarget();
		activeChar.setTarget(activeChar);
		
		// activeChar.sendPacket(new SystemMessage(SystemMessage.ENABLED_SPIRITSHOT));
		
		final MagicSkillUse MSU = new MagicSkillUse(activeChar, _skillIds[grade], 1, 0, 0);
		Broadcast.toSelfAndKnownPlayers(activeChar, MSU);
		activeChar.setTarget(oldTarget);
	}
	
	@Override
	public int[] getItemIds()
	{
		return _itemIds;
	}
}
