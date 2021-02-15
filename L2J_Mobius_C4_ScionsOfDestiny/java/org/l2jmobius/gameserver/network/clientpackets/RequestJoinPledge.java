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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinPledge;

public class RequestJoinPledge extends GameClientPacket
{
	private int _target;
	// private int _pledgeType;
	
	@Override
	protected void readImpl()
	{
		_target = readD();
		// _pledgeType = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!(World.getInstance().findObject(_target) instanceof PlayerInstance))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}
		
		final PlayerInstance target = (PlayerInstance) World.getInstance().findObject(_target);
		final Clan clan = player.getClan();
		if (!clan.checkClanJoinCondition(player, target, 0))
		{
			return;
		}
		
		if (!player.getRequest().setRequest(target, this))
		{
			return;
		}
		
		target.sendPacket(new AskJoinPledge(player.getObjectId(), player.getClan().getName()));
	}
	
	public int getPledgeType()
	{
		return 0;
	}
}