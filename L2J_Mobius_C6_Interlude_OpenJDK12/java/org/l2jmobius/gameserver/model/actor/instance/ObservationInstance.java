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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.instancemanager.SiegeManager;
import org.l2jmobius.gameserver.model.entity.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.templates.creatures.NpcTemplate;

/**
 * The Class L2ObservationInstance.
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 */
public final class ObservationInstance extends FolkInstance
{
	/**
	 * Instantiates a new l2 observation instance.
	 * @param objectId the object id
	 * @param template the template
	 */
	public ObservationInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		if (command.startsWith("observeSiege"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken(); // Bypass cost
			
			if (Olympiad.getInstance().isRegistered(player) || player.isInOlympiadMode())
			{
				player.sendMessage("You already participated in Olympiad!");
				return;
			}
			
			if (player._inEventTvT || player._inEventDM || player._inEventCTF)
			{
				player.sendMessage("You already participated in Event!");
				return;
			}
			
			if (player.isInCombat() || (player.getPvpFlag() > 0))
			{
				player.sendMessage("You are in combat now!");
				return;
			}
			
			if (SiegeManager.getInstance().getSiege(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())) != null)
			{
				doObserve(player, val);
			}
			else
			{
				player.sendPacket(SystemMessageId.ONLY_VIEW_SIEGE);
			}
		}
		else if (command.startsWith("observe"))
		{
			if (Olympiad.getInstance().isRegistered(player) || player.isInOlympiadMode())
			{
				player.sendMessage("You already participated in Olympiad!");
				return;
			}
			
			if (player._inEventTvT || player._inEventDM || player._inEventCTF)
			{
				player.sendMessage("You already participated in Event!");
				return;
			}
			
			if (player.isInCombat() || (player.getPvpFlag() > 0))
			{
				player.sendMessage("You are in combat now!");
				return;
			}
			
			doObserve(player, command.substring(8));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
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
		
		return "data/html/observation/" + pom + ".htm";
	}
	
	/**
	 * Do observe.
	 * @param player the player
	 * @param val the val
	 */
	private void doObserve(PlayerInstance player, String val)
	{
		StringTokenizer st = new StringTokenizer(val);
		final int cost = Integer.parseInt(st.nextToken());
		final int x = Integer.parseInt(st.nextToken());
		final int y = Integer.parseInt(st.nextToken());
		final int z = Integer.parseInt(st.nextToken());
		if (player.reduceAdena("Broadcast", cost, this, true))
		{
			// enter mode
			player.enterObserverMode(x, y, z);
			final ItemList il = new ItemList(player, false);
			player.sendPacket(il);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
