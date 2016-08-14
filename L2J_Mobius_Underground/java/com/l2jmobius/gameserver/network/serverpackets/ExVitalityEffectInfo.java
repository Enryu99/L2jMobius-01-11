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

import com.l2jmobius.Config;
import com.l2jmobius.commons.network.PacketWriter;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.client.OutgoingPackets;

/**
 * @author Sdw
 */
public class ExVitalityEffectInfo implements IClientOutgoingPacket
{
	private final int _vitalityBonus;
	private final int _vitalityItemsRemaining;
	private final int _points;
	
	public ExVitalityEffectInfo(L2PcInstance cha)
	{
		_points = cha.getVitalityPoints();
		_vitalityBonus = (int) cha.getStat().getVitalityExpBonus() * 100;
		_vitalityItemsRemaining = Config.VITALITY_MAX_ITEMS_ALLOWED - cha.getVitalityItemsUsed();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_VITALITY_EFFECT_INFO.writeId(packet);
		
		packet.writeD(_points);
		packet.writeD(_vitalityBonus); // Vitality Bonus
		packet.writeH(0x00); // Vitality additional bonus in %
		packet.writeH(_vitalityItemsRemaining); // How much vitality items remaining for use
		packet.writeH(Config.VITALITY_MAX_ITEMS_ALLOWED); // Max number of items for use
		return true;
	}
}