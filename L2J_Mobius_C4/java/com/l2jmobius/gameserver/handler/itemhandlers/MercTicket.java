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
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.MercTicketManager;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PlayableInstance;
import com.l2jmobius.gameserver.model.entity.Castle;

public class MercTicket implements IItemHandler
{
	private static String[] _Messages =
	{
		"To arms!.",
		"I am ready to serve you my lord when the time comes.",
		"You summon me."
	};
	
	/**
	 * handler for using mercenary tickets. Things to do: 1) Check constraints: 1.a) Tickets may only be used in a castle 1.b) Only specific tickets may be used in each castle (different tickets for each castle) 1.c) only the owner of that castle may use them 1.d) tickets cannot be used during siege
	 * 1.e) Check if max number of tickets has been reached 1.f) Check if max number of tickets from this ticket's TYPE has been reached 2) If allowed, call the MercTicketManager to add the item and spawn in the world 3) Remove the item from the person's inventory
	 */
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		final int itemId = item.getItemId();
		final L2PcInstance activeChar = (L2PcInstance) playable;
		final Castle castle = CastleManager.getInstance().getCastle(activeChar);
		int castleId = -1;
		if (castle != null)
		{
			castleId = castle.getCastleId();
		}
		
		if (castleId == -1)
		{
			activeChar.sendMessage("Mercenary Tickets can only be used in castles.");
			
			return;
		}
		
		// add check that certain tickets can only be placed in certain castles
		if (MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
		{
			String castleName = null;
			switch (MercTicketManager.getInstance().getTicketCastleId(itemId))
			{
				case 1:
					castleName = "Gludio";
					break;
				case 2:
					castleName = "Dion";
					break;
				case 3:
					castleName = "Giran";
					break;
				case 4:
					castleName = "Oren";
					break;
				case 5:
					castleName = "Aden";
					break;
				case 6:
					castleName = "Innadril";
					break;
				case 7:
					castleName = "Goddard";
					break;
			}
			
			activeChar.sendMessage("This Mercenary Ticket can only be used in " + castleName + " castle.");
			return;
			
		}
		
		if (!activeChar.isCastleLord(castleId))
		{
			activeChar.sendMessage("You are not the lord of this castle.");
			return;
		}
		
		if ((castle != null) && castle.getSiege().getIsInProgress())
		{
			activeChar.sendMessage("You cannot hire a mercenary while siege is in progress.");
			return;
		}
		
		if (MercTicketManager.getInstance().isAtCastleLimit(item.getItemId()))
		{
			activeChar.sendMessage("You cannot hire any more mercenaries.");
			return;
		}
		
		if (MercTicketManager.getInstance().isAtTypeLimit(item.getItemId()))
		{
			activeChar.sendMessage("You cannot hire any more mercenaries of this type.You may still hire other types of mercenaries.");
			return;
		}
		
		if (MercTicketManager.getInstance().isTooCloseToAnotherTicket(activeChar.getX(), activeChar.getY(), activeChar.getZ()))
		{
			activeChar.sendMessage("The distance between mercenaries is too short.");
			return;
		}
		
		final int npcId = MercTicketManager.getInstance().addTicket(item.getItemId(), activeChar, _Messages);
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false); // Remove item from char's inventory
		activeChar.sendMessage("Hired mercenary (" + itemId + "," + npcId + ") at coords:" + activeChar.getX() + "," + activeChar.getY() + "," + activeChar.getZ() + " heading:" + activeChar.getHeading());
	}
	
	// left in here for backward compatibility
	@Override
	public int[] getItemIds()
	{
		return MercTicketManager.getInstance().getItemIds();
	}
}