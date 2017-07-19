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
package com.l2jmobius.gameserver.util;

import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.clientpackets.Say2;
import com.l2jmobius.gameserver.network.serverpackets.CharInfo;
import com.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jmobius.gameserver.network.serverpackets.RelationChanged;

/**
 * This class ...
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public final class Broadcast
{
	private static Logger _log = Logger.getLogger(Broadcast.class.getName());
	
	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character that have the Character targetted.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 * @param character
	 * @param mov
	 */
	public static void toPlayersTargettingMyself(L2Character character, L2GameServerPacket mov)
	{
		if (Config.DEBUG)
		{
			_log.fine("players to notify:" + character.getKnownList().getKnownPlayers().size() + " packet:" + mov.getType());
		}
		
		for (final L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if ((player == null) || (player.getTarget() != character))
			{
				continue;
			}
			
			player.sendPacket(mov);
		}
	}
	
	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 * @param character
	 * @param mov
	 */
	public static void toKnownPlayers(L2Character character, L2GameServerPacket mov)
	{
		if (Config.DEBUG)
		{
			_log.fine("players to notify:" + character.getKnownList().getKnownPlayers().size() + " packet:" + mov.getType());
		}
		
		for (final L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			try
			{
				if (player == null)
				{
					continue;
				}
				
				player.sendPacket(mov);
				if ((mov instanceof CharInfo) && (character instanceof L2PcInstance))
				{
					final int relation = ((L2PcInstance) character).getRelation(player);
					if ((character.getKnownList().getKnownRelations().get(player.getObjectId()) != null) && (character.getKnownList().getKnownRelations().get(player.getObjectId()) != relation))
					{
						
						player.sendPacket(new RelationChanged((L2PcInstance) character, relation, player.isAutoAttackable(character)));
						
						if (((L2PcInstance) character).getPet() != null)
						{
							player.sendPacket(new RelationChanged(((L2PcInstance) character).getPet(), relation, player.isAutoAttackable(character)));
						}
					}
				}
			}
			catch (final NullPointerException e)
			{
			}
		}
	}
	
	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers (in the specified radius) of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just needs to go through _knownPlayers to send Server->Client Packet and check the distance between the targets.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 * @param character
	 * @param mov
	 * @param radius
	 */
	public static void toKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius)
	{
		if (radius < 0)
		{
			radius = 1500;
		}
		
		for (final L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if (player == null)
			{
				continue;
			}
			
			if (character.isInsideRadius(player, radius, false, false))
			{
				player.sendPacket(mov);
			}
		}
	}
	
	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character and to the specified character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * @param character
	 * @param mov
	 */
	public static void toSelfAndKnownPlayers(L2Character character, L2GameServerPacket mov)
	{
		if (character instanceof L2PcInstance)
		{
			character.sendPacket(mov);
		}
		
		toKnownPlayers(character, mov);
	}
	
	// To improve performance we are comparing values of radius^2 instead of calculating sqrt all the time
	public static void toSelfAndKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, long radiusSq)
	{
		if (radiusSq < 0)
		{
			radiusSq = 360000;
		}
		
		if (character instanceof L2PcInstance)
		{
			character.sendPacket(mov);
		}
		
		for (final L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if ((player != null) && (character.getDistanceSq(player) <= radiusSq))
			{
				player.sendPacket(mov);
			}
		}
	}
	
	/**
	 * Send a packet to all L2PcInstance present in the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _allPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 * @param mov
	 */
	public static void toAllOnlinePlayers(L2GameServerPacket mov)
	{
		if (Config.DEBUG)
		{
			_log.fine("Players to notify: " + L2World.getInstance().getAllPlayersCount() + " (with packet " + mov.getType() + ")");
		}
		
		for (final L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers())
		{
			if (onlinePlayer == null)
			{
				continue;
			}
			
			onlinePlayer.sendPacket(mov);
		}
	}
	
	public static void announceToOnlinePlayers(String text)
	{
		final CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", text);
		toAllOnlinePlayers(cs);
	}
}