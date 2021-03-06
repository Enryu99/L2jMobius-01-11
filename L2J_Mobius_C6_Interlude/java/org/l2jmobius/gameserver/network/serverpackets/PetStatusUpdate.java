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

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.SummonInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

/**
 * @version $Revision: 1.5.2.3.2.5 $ $Date: 2005/03/29 23:15:10 $
 */
public class PetStatusUpdate implements IClientOutgoingPacket
{
	private final Summon _summon;
	private final int _maxHp;
	private final int _maxMp;
	private int _maxFed;
	private int _curFed;
	
	public PetStatusUpdate(Summon summon)
	{
		_summon = summon;
		_maxHp = _summon.getMaxHp();
		_maxMp = _summon.getMaxMp();
		if (_summon instanceof PetInstance)
		{
			final PetInstance pet = (PetInstance) _summon;
			_curFed = pet.getCurrentFed(); // how fed it is
			_maxFed = pet.getMaxFed(); // max fed it can be
		}
		else if (_summon instanceof SummonInstance)
		{
			final SummonInstance sum = (SummonInstance) _summon;
			_curFed = sum.getTimeRemaining();
			_maxFed = sum.getTotalLifeTime();
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PET_STATUS_UPDATE.writeId(packet);
		packet.writeD(_summon.getSummonType());
		packet.writeD(_summon.getObjectId());
		packet.writeD(_summon.getX());
		packet.writeD(_summon.getY());
		packet.writeD(_summon.getZ());
		packet.writeS(_summon.getOwner().getName());
		packet.writeD(_curFed);
		packet.writeD(_maxFed);
		packet.writeD((int) _summon.getCurrentHp());
		packet.writeD(_maxHp);
		packet.writeD((int) _summon.getCurrentMp());
		packet.writeD(_maxMp);
		packet.writeD(_summon.getLevel());
		packet.writeQ(_summon.getStat().getExp());
		packet.writeQ(_summon.getExpForThisLevel()); // 0% absolute value
		packet.writeQ(_summon.getExpForNextLevel()); // 100% absolute value
		return true;
	}
}
