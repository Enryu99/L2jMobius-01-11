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

import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;

public class RequestEvaluate extends L2GameClientPacket
{
	private static final String _C__B9_REQUESTEVALUATE = "[C] B9 RequestEvaluate";
	
	// private static Logger _log = Logger.getLogger(RequestEvaluate.class.getName());
	
	@SuppressWarnings("unused")
	private int _targetid;
	
	@Override
	protected void readImpl()
	{
		_targetid = readD();
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		SystemMessage sm;
		
		if (!(activeChar.getTarget() instanceof L2PcInstance))
		{
			sm = new SystemMessage(SystemMessage.TARGET_IS_INCORRECT);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (activeChar.getLevel() < 10)
		{
			sm = new SystemMessage(SystemMessage.ONLY_LEVEL_SUP_10_CAN_RECOMMEND);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (activeChar.getTarget() == activeChar)
		{
			sm = new SystemMessage(SystemMessage.YOU_CANNOT_RECOMMEND_YOURSELF);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (activeChar.getRecomLeft() <= 0)
		{
			sm = new SystemMessage(SystemMessage.NO_MORE_RECOMMENDATIONS_TO_HAVE);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		final L2PcInstance target = (L2PcInstance) activeChar.getTarget();
		
		if (target.getRecomHave() >= 255)
		{
			sm = new SystemMessage(SystemMessage.YOU_NO_LONGER_RECIVE_A_RECOMMENDATION);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (!activeChar.canRecom(target))
		{
			sm = new SystemMessage(SystemMessage.THAT_CHARACTER_IS_RECOMMENDED);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		activeChar.giveRecom(target);
		
		sm = new SystemMessage(SystemMessage.YOU_HAVE_RECOMMENDED);
		sm.addString(target.getName());
		sm.addNumber(activeChar.getRecomLeft());
		activeChar.sendPacket(sm);
		
		sm = new SystemMessage(SystemMessage.YOU_HAVE_BEEN_RECOMMENDED);
		sm.addString(activeChar.getName());
		target.sendPacket(sm);
		sm = null;
		
		activeChar.sendPacket(new UserInfo(activeChar));
		target.broadcastUserInfo();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__B9_REQUESTEVALUATE;
	}
}