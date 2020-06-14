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

import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;

/**
 * format SdSS dddddddd d (Sddddd)
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class GMViewPledgeInfo extends GameServerPacket
{
	private final Clan _clan;
	private final PlayerInstance _player;
	
	public GMViewPledgeInfo(Clan clan, PlayerInstance player)
	{
		_clan = clan;
		_player = player;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x90);
		writeS(_player.getName());
		writeD(_clan.getClanId());
		writeS(_clan.getName());
		writeS(_clan.getLeaderName());
		writeD(_clan.getCrestId()); // -> no, it's no longer used (nuocnam) fix by game
		writeD(_clan.getLevel());
		writeD(_clan.getHasCastle());
		writeD(_clan.getHasHideout());
		writeD(0);
		writeD(_player.getLevel());
		writeD(_clan.getDissolvingExpiryTime() > System.currentTimeMillis() ? 3 : 0);
		writeD(0);
		
		writeD(_clan.getAllyId()); // c2
		writeS(_clan.getAllyName()); // c2
		writeD(_clan.getAllyCrestId()); // c2
		writeD(_clan.isAtWar()); // c3
		
		ClanMember[] members = _clan.getMembers();
		writeD(members.length);
		
		for (ClanMember m : members)
		{
			writeS(m.getName());
			writeD(m.getLevel());
			writeD(m.getClassId());
			writeD(0);
			writeD(1);
			writeD(m.isOnline() ? m.getObjectId() : 0);
		}
	}
}
