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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.gameserver.datatables.csv.RecipeTable;
import org.l2jmobius.gameserver.model.RecipeList;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

/**
 * format dddd
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class RecipeItemMakeInfo extends GameServerPacket
{
	private final int _id;
	private final PlayerInstance _player;
	private final boolean _success;
	
	public RecipeItemMakeInfo(int id, PlayerInstance player, boolean success)
	{
		_id = id;
		_player = player;
		_success = success;
	}
	
	public RecipeItemMakeInfo(int id, PlayerInstance player)
	{
		_id = id;
		_player = player;
		_success = true;
	}
	
	@Override
	protected final void writeImpl()
	{
		final RecipeList recipe = RecipeTable.getInstance().getRecipeById(_id);
		if (recipe != null)
		{
			writeC(0xD7);
			
			writeD(_id);
			writeD(recipe.isDwarvenRecipe() ? 0 : 1); // 0 = Dwarven - 1 = Common
			writeD((int) _player.getCurrentMp());
			writeD(_player.getMaxMp());
			writeD(_success ? 1 : 0); // item creation success/failed
		}
	}
}
