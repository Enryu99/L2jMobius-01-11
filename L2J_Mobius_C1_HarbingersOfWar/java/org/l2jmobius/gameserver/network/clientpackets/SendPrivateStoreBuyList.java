/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.ClientThread;
import org.l2jmobius.gameserver.model.TradeItem;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;

public class SendPrivateStoreBuyList extends ClientBasePacket
{
	private static final String _C__79_SENDPRIVATESTOREBUYLIST = "[C] 79 SendPrivateStoreBuyList";
	
	public SendPrivateStoreBuyList(byte[] decrypt, ClientThread client)
	{
		super(decrypt);
		int sellerID = readD();
		int count = readD();
		World world = World.getInstance();
		PlayerInstance seller = (PlayerInstance) world.findObject(sellerID);
		PlayerInstance buyer = client.getActiveChar();
		List<TradeItem> buyerlist = new ArrayList<>();
		List<TradeItem> sellerlist = seller.getSellList();
		int cost = 0;
		for (int i = 0; i < count; ++i)
		{
			TradeItem temp = new TradeItem();
			temp.setObjectId(readD());
			temp.setCount(readD());
			temp.setOwnersPrice(readD());
			temp.setItemId(seller.getInventory().getItem(temp.getObjectId()).getItemId());
			cost += temp.getOwnersPrice() * temp.getCount();
			buyerlist.add(temp);
		}
		if ((buyer.getAdena() >= cost) && (count > 0) && (seller.getPrivateStoreType() == 1))
		{
			seller.getTradeList().BuySellItems(buyer, buyerlist, seller, sellerlist);
			if (seller.getSellList().size() == 0)
			{
				seller.setPrivateStoreType(0);
				seller.sendPacket(new ChangeWaitType(seller, 1));
				seller.broadcastPacket(new ChangeWaitType(seller, 1));
				seller.sendPacket(new UserInfo(seller));
				seller.broadcastPacket(new UserInfo(seller));
			}
		}
		else
		{
			SystemMessage msg = new SystemMessage(279);
			buyer.sendPacket(msg);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__79_SENDPRIVATESTOREBUYLIST;
	}
}
