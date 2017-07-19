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

import java.util.StringTokenizer;

import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.datatables.MapRegionTable;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import com.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;

/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2CastleTeleporterInstance extends L2FolkInstance
{
	private boolean _currentTask = false;
	
	/**
	 * @param objectId
	 * @param template
	 */
	public L2CastleTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		
		final String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.equalsIgnoreCase("tele"))
		{
			
			int delay;
			if (!getTask())
			{
				if (getCastle().getSiege().getIsInProgress() && (getCastle().getSiege().getControlTowerCount() == 0))
				{
					delay = 480000;
				}
				else
				{
					delay = 30000;
				}
				
				setTask(true);
				ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay);
			}
			
			final String filename = "data/html/teleporter/MassGK-1.htm";
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			html.setFile(filename);
			
			player.sendPacket(html);
			
			return;
			
		}
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename;
		if (!getTask())
		{
			
			if (getCastle().getSiege().getIsInProgress() && (getCastle().getSiege().getControlTowerCount() == 0))
			{
				filename = "data/html/teleporter/MassGK-2.htm";
			}
			else
			{
				filename = "data/html/teleporter/MassGK.htm";
			}
		}
		else
		{
			filename = "data/html/teleporter/MassGK-1.htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		html.setFile(filename);
		
		html.replace("%objectId%", String.valueOf(getObjectId()));
		
		player.sendPacket(html);
		
	}
	
	public void oustAllPlayers()
	{
		getCastle().oustAllPlayers();
	}
	
	class oustAllPlayers implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if (getCastle().getSiege().getIsInProgress())
				{
					final CreatureSay cs = new CreatureSay(getObjectId(), 1, getName(), "The defenders of " + getCastle().getName() + " castle will be teleported to the inner castle.");
					final int region = MapRegionTable.getInstance().getMapRegion(getX(), getY());
					for (final L2PcInstance player : L2World.getInstance().getAllPlayers())
					{
						if (region == MapRegionTable.getInstance().getMapRegion(player.getX(), player.getY()))
						{
							player.sendPacket(cs);
						}
					}
				}
				oustAllPlayers();
				setTask(false);
			}
			catch (final NullPointerException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This is called when a player interacts with this NPC
	 * @param player
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		player.setLastFolkNPC(this);
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			final MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			
			if (!canInteract(player))
			
			{
				
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				
			}
			else
			{
				showChatWindow(player);
			}
		}
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		
		player.sendPacket(new ActionFailed());
	}
	
	public boolean getTask()
	{
		return _currentTask;
	}
	
	public void setTask(boolean state)
	{
		_currentTask = state;
	}
}