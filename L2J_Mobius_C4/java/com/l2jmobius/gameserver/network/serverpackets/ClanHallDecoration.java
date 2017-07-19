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

import com.l2jmobius.gameserver.model.entity.ClanHall;
import com.l2jmobius.gameserver.model.entity.ClanHall.ClanHallFunction;

/**
 * @author Steuf
 */
public class ClanHallDecoration extends L2GameServerPacket
{
	private static final String _S__F7_AGITDECOINFO = "[S] F7 AgitDecoInfo";
	private final ClanHall clanHall;
	private ClanHallFunction Function;
	
	public ClanHallDecoration(ClanHall ClanHall)
	{
		clanHall = ClanHall;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xf7);
		writeD(clanHall.getId()); // clanhall id
		
		// FUNC_RESTORE_HP
		Function = clanHall.getFunction(ClanHall.FUNC_RESTORE_HP);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
		}
		else if (((clanHall.getGrade() == 0) && (Function.getLvl() < 220)) || ((clanHall.getGrade() == 1) && (Function.getLvl() < 160)) || ((clanHall.getGrade() == 2) && (Function.getLvl() < 260)) || ((clanHall.getGrade() == 3) && (Function.getLvl() < 300)))
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_RESTORE_MP
		Function = clanHall.getFunction(ClanHall.FUNC_RESTORE_MP);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
			writeC(0);
		}
		else if ((((clanHall.getGrade() == 0) || (clanHall.getGrade() == 1)) && (Function.getLvl() < 25)) || ((clanHall.getGrade() == 2) && (Function.getLvl() < 30)) || ((clanHall.getGrade() == 3) && (Function.getLvl() < 40)))
		{
			writeC(1);
			writeC(1);
		}
		else
		{
			writeC(2);
			writeC(2);
		}
		
		// FUNC_RESTORE_EXP
		Function = clanHall.getFunction(ClanHall.FUNC_RESTORE_EXP);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
		}
		else if (((clanHall.getGrade() == 0) && (Function.getLvl() < 25)) || ((clanHall.getGrade() == 1) && (Function.getLvl() < 30)) || ((clanHall.getGrade() == 2) && (Function.getLvl() < 40)) || ((clanHall.getGrade() == 3) && (Function.getLvl() < 50)))
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_TELEPORT
		Function = clanHall.getFunction(ClanHall.FUNC_TELEPORT);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
		}
		else if (Function.getLvl() < 2)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		writeC(0);
		
		// CURTAINS
		Function = clanHall.getFunction(ClanHall.FUNC_DECO_CURTAINS);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
		}
		else if (Function.getLvl() <= 1)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_ITEM_CREATE
		Function = clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
		}
		else if (((clanHall.getGrade() == 0) && (Function.getLvl() < 2)) || (Function.getLvl() < 3))
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_SUPPORT
		Function = clanHall.getFunction(ClanHall.FUNC_SUPPORT);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
			writeC(0);
		}
		else if (((clanHall.getGrade() == 0) && (Function.getLvl() < 2)) || ((clanHall.getGrade() == 1) && (Function.getLvl() < 4)) || ((clanHall.getGrade() == 2) && (Function.getLvl() < 5)) || ((clanHall.getGrade() == 3) && (Function.getLvl() < 8)))
		{
			writeC(1);
			writeC(1);
		}
		else
		{
			writeC(2);
			writeC(2);
		}
		
		// Front Platform
		Function = clanHall.getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
		}
		else if (Function.getLvl() <= 1)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_ITEM_CREATE
		Function = clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if ((Function == null) || (Function.getLvl() == 0))
		{
			writeC(0);
		}
		else if (((clanHall.getGrade() == 0) && (Function.getLvl() < 2)) || (Function.getLvl() < 3))
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		writeD(0);
		writeD(0);
	}
	
	@Override
	public String getType()
	{
		return _S__F7_AGITDECOINFO;
	}
}