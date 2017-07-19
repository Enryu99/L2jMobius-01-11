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

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.CastleManorManager;
import com.l2jmobius.gameserver.instancemanager.CastleManorManager.SeedProduction;

import javolution.util.FastList;

/**
 * Format: (ch) dd [ddd]
 * @author l3x
 */
public class RequestSetSeed extends L2GameClientPacket
{
	private static final String _C__D0_0A_REQUESTSETSEED = "[C] D0:0A RequestSetSeed";
	
	private int _size;
	private int _manorId;
	private int[] _items; // _size*3
	
	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_size = readD();
		if (((_size * 12) > _buf.remaining()) || (_size > 500))
		{
			_size = 0;
			return;
		}
		
		_items = new int[_size * 3];
		for (int i = 0; i < _size; i++)
		{
			final int itemId = readD();
			_items[(i * 3) + 0] = itemId;
			final int sales = readD();
			_items[(i * 3) + 1] = sales;
			final int price = readD();
			_items[(i * 3) + 2] = price;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
	 */
	@Override
	public void runImpl()
	{
		if (_size < 1)
		{
			return;
		}
		
		final FastList<SeedProduction> seeds = new FastList<>();
		for (int i = 0; i < _size; i++)
		{
			final int id = _items[(i * 3) + 0];
			final int sales = _items[(i * 3) + 1];
			final int price = _items[(i * 3) + 2];
			if (id > 0)
			{
				seeds.add(CastleManorManager.getInstance().getNewSeedProduction(id, sales, price, sales));
			}
		}
		
		CastleManager.getInstance().getCastleById(_manorId).setSeedProduction(seeds, CastleManorManager.PERIOD_NEXT);
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			CastleManager.getInstance().getCastleById(_manorId).saveSeedData(CastleManorManager.PERIOD_NEXT);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_0A_REQUESTSETSEED;
	}
}