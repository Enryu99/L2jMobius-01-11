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

import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Object;

/**
 * sample 06 8f19904b 2522d04b 00000000 80 950c0000 4af50000 08f2ffff 0000 - 0 damage (missed 0x80) 06 85071048 bc0e504b 32000000 10 fc41ffff fd240200 a6f5ffff 0100 bc0e504b 33000000 10 3.... format dddc dddh (ddc)
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class Attack extends L2GameServerPacket
{
	private static final String _S__06_ATTACK = "[S] 06 Attack";
	public final int _attackerId;
	public final boolean _soulshot;
	protected int _grade;
	private final int _x;
	private final int _y;
	private final int _z;
	private Hit[] hits;
	
	private class Hit
	{
		int _targetId;
		int _damage;
		int _flags;
		
		Hit(L2Object target, int damage, boolean miss, boolean crit, boolean shld)
		{
			_targetId = target.getObjectId();
			_damage = damage;
			if (_soulshot)
			{
				_flags |= 0x10 | _grade;
			}
			if (crit)
			{
				_flags |= 0x20;
			}
			if (shld)
			{
				_flags |= 0x40;
			}
			if (miss)
			{
				_flags |= 0x80;
			}
			
		}
	}
	
	/**
	 * @param attacker the attacker L2Character
	 * @param ss true if using SoulShots
	 * @param grade
	 */
	public Attack(L2Character attacker, boolean ss, int grade)
	{
		_attackerId = attacker.getObjectId();
		_soulshot = ss;
		_grade = grade;
		_x = attacker.getX();
		_y = attacker.getY();
		_z = attacker.getZ();
		hits = new Hit[0];
	}
	
	/**
	 * Add this hit (target, damage, miss, critical, shield) to the Server-Client packet Attack.<BR>
	 * <BR>
	 * @param target
	 * @param damage
	 * @param miss
	 * @param crit
	 * @param shld
	 */
	public void addHit(L2Object target, int damage, boolean miss, boolean crit, boolean shld)
	{
		// Get the last position in the hits table
		final int pos = hits.length;
		
		// Create a new Hit object
		final Hit[] tmp = new Hit[pos + 1];
		
		// Add the new Hit object to hits table
		for (int i = 0; i < hits.length; i++)
		{
			tmp[i] = hits[i];
		}
		tmp[pos] = new Hit(target, damage, miss, crit, shld);
		hits = tmp;
	}
	
	/**
	 * Return True if the Server-Client packet Attack contains at least 1 hit.<BR>
	 * <BR>
	 * @return
	 */
	public boolean hasHits()
	{
		return hits.length > 0;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x05);
		
		writeD(_attackerId);
		writeD(hits[0]._targetId);
		writeD(hits[0]._damage);
		writeC(hits[0]._flags);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeH(hits.length - 1);
		for (int i = 1; i < hits.length; i++)
		{
			writeD(hits[i]._targetId);
			writeD(hits[i]._damage);
			writeC(hits[i]._flags);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__06_ATTACK;
	}
}