/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.npc.Zenya;

import ai.npc.AbstractNpcAI;

import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Zenya AI.
 * @author Stayway
 */
public final class Zenya extends AbstractNpcAI
{
	// NPC
	private static final int ZENYA = 32140;
	// Location
	private static final Location IMPERIAL_TOMB = new Location(184775, -76988, -2732);
	// Misc
	private static final int MIN_LEVEL = 80;
	
	private Zenya()
	{
		super(Zenya.class.getSimpleName(), "ai/npc");
		addStartNpc(ZENYA);
		addFirstTalkId(ZENYA);
		addTalkId(ZENYA);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32140.html":
			case "32140-1.html":
			case "32140-2.html":
			case "32140-4.html":
			{
				htmltext = event;
				break;
			}
			case "teleport":
			{
				player.teleToLocation(IMPERIAL_TOMB);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		if (player.getLevel() < MIN_LEVEL)
		{
			return "32140-3.html";
		}
		return "32140.html";
	}
	
	public static void main(String[] args)
	{
		new Zenya();
	}
}