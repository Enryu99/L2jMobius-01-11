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
package com.l2jmobius.gameserver.model.drops;

import java.util.Collection;

import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.holders.ItemHolder;

/**
 * @author NosBit
 */
public interface IDropItem
{
	/**
	 * Calculates drops of this drop item.
	 * @param victim the victim
	 * @param killer the killer
	 * @return {@code null} or empty collection if there are no drops, a collection containing all items to drop otherwise
	 */
	Collection<ItemHolder> calculateDrops(L2Character victim, L2Character killer);
}
