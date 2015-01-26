/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.itemhandlers;

import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.network.serverpackets.itemappearance.ExChoose_Shape_Shifting_Item;

/**
 * @author Erlandys
 */
public class ItemAppearance implements IItemHandler
{
	@Override
	public boolean useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return false;
		}
		final L2PcInstance activeChar = (L2PcInstance) playable;
		if ((item == null) || !item.isEtcItem() || (item.isEtcItem() && (item.getEtcItem().getAppearanceStone() == null)))
		{
			return false;
		}
		activeChar.sendPacket(new ExChoose_Shape_Shifting_Item(item.getEtcItem().getAppearanceStone()));
		activeChar.setUsingAppearanceStone(item);
		return true;
	}
}
