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
import com.l2jmobius.gameserver.model.L2Macro;
import com.l2jmobius.gameserver.model.L2Macro.L2MacroCmd;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestMakeMacro extends L2GameClientPacket
{
	private L2Macro _macro;
	private int _commands_lenght = 0;
	
	private static final String _C__C1_REQUESTMAKEMACRO = "[C] C1 RequestMakeMacro";
	
	@Override
	protected void readImpl()
	{
		final int _id = readD();
		final String _name = readS();
		final String _desc = readS();
		final String _acronym = readS();
		final int _icon = readC();
		int _count = readC();
		if (_count > 12)
		{
			_count = 12;
		}
		
		final L2MacroCmd[] commands = new L2MacroCmd[_count];
		if (Config.DEBUG)
		{
			System.out.println("Make macro id:" + _id + "\tname:" + _name + "\tdesc:" + _desc + "\tacronym:" + _acronym + "\ticon:" + _icon + "\tcount:" + _count);
		}
		for (int i = 0; i < _count; i++)
		{
			final int entry = readC();
			final int type = readC(); // 1 = skill, 3 = action, 4 = shortcut
			final int d1 = readD(); // skill or page number for shortcuts
			final int d2 = readC();
			final String command = readS();
			_commands_lenght += command.length();
			commands[i] = new L2MacroCmd(entry, type, d1, d2, command);
			if (Config.DEBUG)
			{
				System.out.println("entry:" + entry + "\ttype:" + type + "\td1:" + d1 + "\td2:" + d2 + "\tcommand:" + command);
			}
		}
		_macro = new L2Macro(_id, _icon, _name, _desc, _acronym, commands);
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (_commands_lenght > 255)
		{
			// Invalid macro. Refer to the Help file for instructions.
			player.sendPacket(new SystemMessage(810));
			return;
		}
		if (player.getMacroses().getAllMacroses().length > 24)
		{
			// You may create up to 24 macros.
			player.sendPacket(new SystemMessage(797));
			return;
		}
		if (_macro.name.length() == 0)
		{
			// Enter the name of the macro.
			player.sendPacket(new SystemMessage(838));
			return;
		}
		if (_macro.descr.length() > 32)
		{
			// Macro descriptions may contain up to 32 characters.
			player.sendPacket(new SystemMessage(837));
			return;
		}
		player.registerMacro(_macro);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__C1_REQUESTMAKEMACRO;
	}
}