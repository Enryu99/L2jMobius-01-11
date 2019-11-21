/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.ClientThread;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChangeMoveType;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;

public class RequestActionUse extends ClientBasePacket
{
	private static final String _C__45_REQUESTACTIONUSE = "[C] 45 RequestActionUse";
	
	public RequestActionUse(byte[] rawPacket, ClientThread client)
	{
		super(rawPacket);
		int actionId = readD();
		int data2 = readD();
		int data3 = readC();
		_log.fine("request Action use: id " + actionId + " 2:" + data2 + " 3:" + data3);
		PlayerInstance activeChar = client.getActiveChar();
		if (activeChar.isDead())
		{
			activeChar.sendPacket(new ActionFailed());
			return;
		}
		switch (actionId)
		{
			case 0:
			{
				int waitType = activeChar.getWaitType() ^ 1;
				_log.fine("new wait type: " + waitType);
				ChangeWaitType cmt = new ChangeWaitType(activeChar, waitType);
				activeChar.setWaitType(waitType);
				activeChar.sendPacket(cmt);
				activeChar.broadcastPacket(cmt);
				break;
			}
			case 1:
			{
				int moveType = activeChar.getMoveType() ^ 1;
				_log.fine("new move type: " + moveType);
				ChangeMoveType cmt = new ChangeMoveType(activeChar, moveType);
				activeChar.setMoveType(moveType);
				activeChar.sendPacket(cmt);
				activeChar.broadcastPacket(cmt);
				break;
			}
			case 15:
			{
				if (activeChar.getPet() == null)
				{
					break;
				}
				if (activeChar.getPet().getCurrentState() != 8)
				{
					activeChar.getPet().setCurrentState((byte) 8);
					activeChar.getPet().setFollowStatus(true);
					activeChar.getPet().followOwner(activeChar);
					break;
				}
				activeChar.getPet().setCurrentState((byte) 0);
				activeChar.getPet().setFollowStatus(false);
				activeChar.getPet().setMovingToPawn(false);
				activeChar.getPet().setPawnTarget(null);
				activeChar.getPet().stopMove();
				activeChar.getPet().broadcastPacket(new StopMove(activeChar.getPet()));
				break;
			}
			case 16:
			{
				if ((activeChar.getTarget() == null) || (activeChar.getPet() == null) || (activeChar.getPet() == activeChar.getTarget()))
				{
					break;
				}
				activeChar.getPet().startAttack((Creature) activeChar.getTarget());
				break;
			}
			case 17:
			{
				if (activeChar.getPet() == null)
				{
					break;
				}
				if (activeChar.getPet().getCurrentState() == 8)
				{
					activeChar.getPet().setFollowStatus(false);
					activeChar.getPet().setMovingToPawn(false);
					activeChar.getPet().setPawnTarget(null);
				}
				activeChar.getPet().setCurrentState((byte) 0);
				activeChar.getPet().stopMove();
				activeChar.getPet().broadcastPacket(new StopMove(activeChar.getPet()));
				break;
			}
			case 18:
			{
				_log.warning("unhandled action type 18");
				break;
			}
			case 19:
			{
				if (activeChar.getPet() == null)
				{
					break;
				}
				activeChar.getPet().unSummon(activeChar);
				break;
			}
			case 20:
			{
				_log.warning("unhandled action type 20");
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _C__45_REQUESTACTIONUSE;
	}
}
