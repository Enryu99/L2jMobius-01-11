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

import org.l2jmobius.gameserver.datatables.AccessLevel;
import org.l2jmobius.gameserver.datatables.sql.AccessLevels;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.SiegeClan;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.entity.event.CTF;
import org.l2jmobius.gameserver.model.entity.event.DM;
import org.l2jmobius.gameserver.model.entity.event.TvT;
import org.l2jmobius.gameserver.model.entity.siege.Castle;
import org.l2jmobius.gameserver.model.entity.siege.Fort;

/**
 * sample 0b 952a1048 objectId 00000000 00000000 00000000 00000000 00000000 00000000 format dddddd rev 377 format ddddddd rev 417
 * @version $Revision: 1.3.3 $ $Date: 2009/04/29 00:46:18 $
 */
public class Die extends GameServerPacket
{
	private final int _objectId;
	private final boolean _fake;
	private boolean _sweepable;
	private boolean _canTeleport;
	private AccessLevel _access = AccessLevels.getInstance()._userAccessLevel;
	private org.l2jmobius.gameserver.model.clan.Clan _clan;
	Creature _creature;
	
	/**
	 * @param creature
	 */
	public Die(Creature creature)
	{
		_creature = creature;
		if (creature instanceof PlayerInstance)
		{
			final PlayerInstance player = (PlayerInstance) creature;
			_access = player.getAccessLevel();
			_clan = player.getClan();
			_canTeleport = ((!TvT.is_started() || !player._inEventTvT) && (!DM.is_started() || !player._inEventDM) && (!CTF.is_started() || !player._inEventCTF) && !player.isInFunEvent() && !player.isPendingRevive());
		}
		_objectId = creature.getObjectId();
		_fake = !creature.isDead();
		if (creature instanceof Attackable)
		{
			_sweepable = ((Attackable) creature).isSweepActive();
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_fake)
		{
			return;
		}
		
		writeC(0x06);
		
		writeD(_objectId);
		// NOTE:
		// 6d 00 00 00 00 - to nearest village
		// 6d 01 00 00 00 - to hide away
		// 6d 02 00 00 00 - to castle
		// 6d 03 00 00 00 - to siege HQ
		// sweepable
		// 6d 04 00 00 00 - FIXED
		
		writeD(_canTeleport ? 0x01 : 0); // 6d 00 00 00 00 - to nearest village
		
		if (_canTeleport && (_clan != null))
		{
			SiegeClan siegeClan = null;
			Boolean isInDefense = false;
			final Castle castle = CastleManager.getInstance().getCastle(_creature);
			final Fort fort = FortManager.getInstance().getFort(_creature);
			
			if ((castle != null) && castle.getSiege().getIsInProgress())
			{
				// siege in progress
				siegeClan = castle.getSiege().getAttackerClan(_clan);
				if ((siegeClan == null) && castle.getSiege().checkIsDefender(_clan))
				{
					isInDefense = true;
				}
			}
			else if ((fort != null) && fort.getSiege().getIsInProgress())
			{
				// siege in progress
				siegeClan = fort.getSiege().getAttackerClan(_clan);
				if ((siegeClan == null) && fort.getSiege().checkIsDefender(_clan))
				{
					isInDefense = true;
				}
			}
			
			writeD(_clan.getHasHideout() > 0 ? 0x01 : 0x00); // 6d 01 00 00 00 - to hide away
			writeD((_clan.getHasCastle() > 0) || (_clan.getHasFort() > 0) || isInDefense ? 0x01 : 0x00); // 6d 02 00 00 00 - to castle
			writeD((siegeClan != null) && !isInDefense && (siegeClan.getFlag().size() > 0) ? 0x01 : 0x00); // 6d 03 00 00 00 - to siege HQ
		}
		else
		{
			writeD(0x00); // 6d 01 00 00 00 - to hide away
			writeD(0x00); // 6d 02 00 00 00 - to castle
			writeD(0x00); // 6d 03 00 00 00 - to siege HQ
		}
		
		writeD(_sweepable ? 0x01 : 0x00); // sweepable (blue glow)
		writeD(_access.allowFixedRes() ? 0x01 : 0x00); // 6d 04 00 00 00 - to FIXED
	}
}
