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
package com.l2jmobius.gameserver.network.clientpackets;

import com.l2jmobius.gameserver.RecipeController;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Administrator TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code Templates
 */
public class RequestRecipeItemMakeSelf extends L2GameClientPacket
{
	private static final String _C__AF_REQUESTRECIPEITEMMAKESELF = "[C] AF RequestRecipeItemMakeSelf";
	// private static Logger _log = Logger.getLogger(RequestSellItem.class.getName());
	
	private int _id;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getManufacture().tryPerformAction("RecipeMakeSelf"))
		{
			return;
		}
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendMessage("Cannot make items while trading.");
			return;
		}
		
		if (activeChar.isInCraftMode())
		{
			activeChar.sendMessage("Currently in Craft Mode.");
			return;
		}
		
		RecipeController.getInstance().requestMakeItem(activeChar, _id);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__AF_REQUESTRECIPEITEMMAKESELF;
	}
}