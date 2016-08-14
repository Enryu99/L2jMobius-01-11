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
package ai.others.CastleAmbassador;

import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.entity.Castle;
import com.l2jmobius.gameserver.model.entity.Fort;

import ai.AbstractNpcAI;

/**
 * Castle Ambassador AI.
 * @author St3eT
 */
public final class CastleAmbassador extends AbstractNpcAI
{
	// NPCs
	// @formatter:off
	private static final int[] CASTLE_AMBASSADOR =
	{
		36393, 36394, 36437, 36435, // Gludio
		36395, 36436, 36439, 36441, // Dion
		36396, 36440, 36444, 36449, 36451, // Giran
		36397, 36438, 36442, 36443, 36446, // Oren
		36398, 36399, 36445, 36448, // Aden
		36400, 36450, // Innadril
		36401, 36447, 36453, // Goddard
		36433, 36452, 36454, // Rune
		36434, 36455, // Schuttgart
	};
	// @formatter:on
	
	private CastleAmbassador()
	{
		addStartNpc(CASTLE_AMBASSADOR);
		addTalkId(CASTLE_AMBASSADOR);
		addFirstTalkId(CASTLE_AMBASSADOR);
		addEventReceivedId(CASTLE_AMBASSADOR);
		addSpawnId(CASTLE_AMBASSADOR);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = null;
		
		if (npc != null)
		{
			final Fort fortresss = npc.getFort();
			
			switch (event)
			{
				case "signed":
				{
					if (fortresss.getFortState() == 0)
					{
						fortresss.setFortState(2, fortresss.getCastleIdByAmbassador(npc.getId()));
						cancelQuestTimer("DESPAWN", npc, null);
						startQuestTimer("DESPAWN", 3000, npc, null);
						htmltext = "ambassador-05.html";
					}
					else if (fortresss.getFortState() == 1)
					{
						htmltext = "ambassador-04.html";
					}
					break;
				}
				case "rejected":
				{
					if (fortresss.getFortState() == 0)
					{
						fortresss.setFortState(1, fortresss.getCastleIdByAmbassador(npc.getId()));
						cancelQuestTimer("DESPAWN", npc, null);
						startQuestTimer("DESPAWN", 3000, npc, null);
						htmltext = "ambassador-02.html";
					}
					else if (fortresss.getFortState() == 2)
					{
						htmltext = "ambassador-02.html";
					}
					break;
				}
				case "DESPAWN":
				{
					if (fortresss.getFortState() == 0)
					{
						fortresss.setFortState(1, fortresss.getCastleIdByAmbassador(npc.getId()));
					}
					cancelQuestTimer("DESPAWN", npc, null);
					npc.broadcastEvent("DESPAWN", 1000, null);
					npc.deleteMe();
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onEventReceived(String eventName, L2Npc sender, L2Npc receiver, L2Object reference)
	{
		if ((receiver != null) && eventName.equals("DESPAWN"))
		{
			receiver.deleteMe();
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		final Fort fortresss = npc.getFort();
		final int fortOwner = fortresss.getOwnerClan() == null ? 0 : fortresss.getOwnerClan().getId();
		String htmltext = null;
		
		if (player.isClanLeader() && (player.getClan() != null) && (player.getClanId() == fortOwner))
		{
			htmltext = (fortresss.isBorderFortress()) ? "ambassador-01.html" : "ambassador.html";
		}
		else
		{
			htmltext = "ambassador-03.html";
		}
		
		htmltext = getHtm(player.getHtmlPrefix(), htmltext);
		htmltext = htmltext.replace("%castleName%", String.valueOf(fortresss.getCastleByAmbassador(npc.getId()).getName()));
		return htmltext;
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		final Fort fort = npc.getFort();
		if (fort == null)
		{
			npc.deleteMe();
			_log.warning("Spawned: " + npc + " location: " + npc.getLocation() + " without fort!");
			return null;
		}
		
		final Castle castle = fort.getCastleByAmbassador(npc.getId());
		if ((castle == null) || (castle.getOwnerId() == 0))
		{
			npc.deleteMe();
		}
		else
		{
			startQuestTimer("DESPAWN", 3600000, npc, null);
		}
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new CastleAmbassador();
	}
}