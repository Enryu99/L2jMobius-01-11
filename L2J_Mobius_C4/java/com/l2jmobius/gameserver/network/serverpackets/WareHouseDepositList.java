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
package com.l2jmobius.gameserver.network.serverpackets;

import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * 0x53 WareHouseDepositList dh (h dddhh dhhh d)
 * @version $Revision: 1.4.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class WareHouseDepositList extends L2GameServerPacket
{
	public static final int Private = 1;
	public static final int Clan = 2;
	public static final int Castle = 3; // not sure
	public static final int Freight = 4; // not sure
	private static Logger _log = Logger.getLogger(WareHouseDepositList.class.getName());
	private static final String _S__53_WAREHOUSEDEPOSITLIST = "[S] 41 WareHouseDepositList";
	private final L2PcInstance _player;
	private final int _playerAdena;
	private final L2ItemInstance[] _items;
	private final int _whtype;
	
	public WareHouseDepositList(L2PcInstance player, int type)
	{
		_player = player;
		_whtype = type;
		_playerAdena = _player.getAdena();
		_items = _player.getInventory().getAvailableItems(true);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x41);
		/*
		 * 0x01-Private Warehouse 0x02-Clan Warehouse 0x03-Castle Warehouse 0x04-Warehouse
		 */
		writeH(_whtype);
		writeD(_playerAdena);
		final int count = _items.length;
		if (Config.DEBUG)
		{
			_log.fine("count:" + count);
		}
		writeH(count);
		
		for (int i = 0; i < count; i++)
		{
			final L2ItemInstance item = _items[i];
			
			writeH(item.getItem().getType1()); // item type1 //unconfirmed, works
			writeD(item.getObjectId()); // unconfirmed, works
			writeD(item.getItemId()); // unconfirmed, works
			writeD(item.getCount()); // unconfirmed, works
			writeH(item.getItem().getType2()); // item type2 //unconfirmed, works
			writeH(0x00); // ? 100
			writeD(item.getItem().getBodyPart()); // ?
			writeH(item.getEnchantLevel()); // enchant level -confirmed
			writeH(0x00);
			writeH(item.getCustomType2());
			writeD(item.getObjectId()); // item id - confirmed
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__53_WAREHOUSEDEPOSITLIST;
	}
}