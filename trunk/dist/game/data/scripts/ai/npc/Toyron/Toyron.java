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
package ai.npc.Toyron;

import quests.Q10327_IntruderWhoWantsTheBookOfGiants.Q10327_IntruderWhoWantsTheBookOfGiants;
import ai.npc.AbstractNpcAI;

import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.instancezone.InstanceWorld;
import com.l2jserver.gameserver.model.quest.QuestState;

/**
 * @author Neanrakyr
 */
public final class Toyron extends AbstractNpcAI
{
	// NPC
	private static final int TOYRON = 33004;
	// Misc
	private static final int TEMPLATE_ID = 182;
	// Location
	private static final Location MUSEUM_OUT = new Location(-114359, 260120, -1192);
	
	private Toyron()
	{
		super(Toyron.class.getSimpleName(), "ai/npc");
		addStartNpc(TOYRON);
		addFirstTalkId(TOYRON);
		addTalkId(TOYRON);
		addSpawnId(TOYRON);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (event.equals("museum_teleport"))
		{
			if ((world != null) && (world.getTemplateId() == TEMPLATE_ID))
			{
				world.removeAllowed(player.getObjectId());
				teleportPlayer(player, MUSEUM_OUT, 0);
			}
			player.teleToLocation(MUSEUM_OUT);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		npc.setIsInvul(true);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		final QuestState qs = player.getQuestState(Q10327_IntruderWhoWantsTheBookOfGiants.class.getSimpleName());
		
		if ((world != null) && (world.getTemplateId() == TEMPLATE_ID))
		{
			if (qs != null)
			{
				switch (qs.getCond())
				{
					case 1:
					{
						return "33004-01.html";
					}
					case 2:
					{
						return "33004-02.html";
					}
					case 3:
					{
						return "33004.html";
					}
				}
			}
			else
			{
				return "33004.html";
			}
		}
		return "33004.html";
	}
	
	public static void main(String[] args)
	{
		new Toyron();
	}
}