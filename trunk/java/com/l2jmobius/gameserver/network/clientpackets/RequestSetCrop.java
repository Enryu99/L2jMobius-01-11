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

import java.util.ArrayList;
import java.util.List;

import com.l2jmobius.Config;
import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.gameserver.instancemanager.CastleManorManager;
import com.l2jmobius.gameserver.model.ClanPrivilege;
import com.l2jmobius.gameserver.model.CropProcure;
import com.l2jmobius.gameserver.model.L2Seed;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * @author l3x
 */
public final class RequestSetCrop implements IClientIncomingPacket
{
	private static final int BATCH_LENGTH = 21; // length of the one item
	
	private int _manorId;
	private List<CropProcure> _items;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_manorId = packet.readD();
		final int count = packet.readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != packet.getReadableBytes()))
		{
			return false;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int itemId = packet.readD();
			final long sales = packet.readQ();
			final long price = packet.readQ();
			final int type = packet.readC();
			if ((itemId < 1) || (sales < 0) || (price < 0))
			{
				_items.clear();
				return false;
			}
			
			if (sales > 0)
			{
				_items.add(new CropProcure(itemId, sales, type, sales, price));
			}
		}
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		if (_items.isEmpty())
		{
			return;
		}
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (!manor.isModifiablePeriod())
		{
			client.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check player privileges
		final L2PcInstance player = client.getActiveChar();
		if ((player == null) || (player.getClan() == null) || (player.getClan().getCastleId() != _manorId) || !player.hasClanPrivilege(ClanPrivilege.CS_MANOR_ADMIN) || !player.getLastFolkNPC().canInteract(player))
		{
			client.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Filter crops with start amount lower than 0 and incorrect price
		final List<CropProcure> list = new ArrayList<>(_items.size());
		for (CropProcure cp : _items)
		{
			final L2Seed s = manor.getSeedByCrop(cp.getId(), _manorId);
			if ((s != null) && (cp.getStartAmount() <= s.getCropLimit()) && (cp.getPrice() >= s.getCropMinPrice()) && (cp.getPrice() <= s.getCropMaxPrice()))
			{
				list.add(cp);
			}
		}
		
		// Save crop list
		manor.setNextCropProcure(list, _manorId);
	}
}