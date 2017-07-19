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
 * 0x54 WarehouseWithdrawalList dh (h dddhh dhhh d)
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/29 23:15:10 $
 */
public class WareHouseWithdrawalList extends L2GameServerPacket
{
	public static final int Private = 1;
	public static final int Clan = 2;
	public static final int Castle = 3; // not sure
	public static final int Freight = 4; // not sure
	private static Logger _log = Logger.getLogger(WareHouseWithdrawalList.class.getName());
	private static final String _S__54_WAREHOUSEWITHDRAWALLIST = "[S] 42 WareHouseWithdrawalList";
	private L2PcInstance _player;
	private int _playerAdena;
	private L2ItemInstance[] _items;
	private int _whtype;
	
	public WareHouseWithdrawalList(L2PcInstance player, int type)
	{
		_player = player;
		_whtype = type;
		_playerAdena = _player.getAdena();
		if (_player.getActiveWarehouse() == null)
		{
			// Something went wrong!
			_log.warning("error while sending withdraw request to: " + _player.getName());
			return;
		}
		_items = _player.getActiveWarehouse().getItems();
		
		if (Config.DEBUG)
		{
			for (final L2ItemInstance item : _items)
			{
				_log.fine("item:" + item.getItem().getName() + " type1:" + item.getItem().getType1() + " type2:" + item.getItem().getType2());
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x42);
		/*
		 * 0x01-Private Warehouse 0x02-Clan Warehouse 0x03-Castle Warehouse 0x04-Warehouse
		 */
		writeH(_whtype);
		writeD(_playerAdena);
		writeH(_items.length);
		
		for (final L2ItemInstance item : _items)
		{
			writeH(item.getItem().getType1()); // item type1 //unconfirmed, works
			writeD(item.getObjectId()); // unconfirmed, works
			writeD(item.getItemId()); // unconfirmed, works
			writeD(item.getCount()); // unconfirmed, works
			writeH(item.getItem().getType2()); // item type2 //unconfirmed, works
			writeH(0x00); // ?
			writeD(item.getItem().getBodyPart()); // ?
			writeH(item.getEnchantLevel()); // enchant level -confirmed
			writeH(0x00);
			writeH(item.getCustomType2());
			writeD(item.getObjectId()); // item id - confimed
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__54_WAREHOUSEWITHDRAWALLIST;
	}
}