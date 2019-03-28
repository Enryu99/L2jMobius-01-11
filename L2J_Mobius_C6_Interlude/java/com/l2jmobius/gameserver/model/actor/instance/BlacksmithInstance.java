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
package com.l2jmobius.gameserver.model.actor.instance;

import com.l2jmobius.gameserver.model.multisell.Multisell;
import com.l2jmobius.gameserver.templates.creatures.NpcTemplate;

/**
 * @author zabbix Lets drink to code!
 */
public class BlacksmithInstance extends FolkInstance
{
	public BlacksmithInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		if (command.startsWith("multisell"))
		{
			final int listId = Integer.parseInt(command.substring(9).trim());
			Multisell.getInstance().SeparateAndSend(listId, player, false, getCastle().getTaxRate());
		}
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/blacksmith/" + pom + ".htm";
	}
}
