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
package com.l2jmobius.gameserver.handler.skillhandlers;

import com.l2jmobius.gameserver.datatables.sql.NpcTable;
import com.l2jmobius.gameserver.handler.ISkillHandler;
import com.l2jmobius.gameserver.idfactory.IdFactory;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.FortManager;
import com.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import com.l2jmobius.gameserver.instancemanager.SiegeManager;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.model.Skill.SkillType;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.actor.instance.SiegeFlagInstance;
import com.l2jmobius.gameserver.model.entity.siege.Castle;
import com.l2jmobius.gameserver.model.entity.siege.Fort;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author _drunk_ TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code Templates
 */
public class SiegeFlag implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.SIEGEFLAG
	};
	
	@Override
	public void useSkill(Creature creature, Skill skill, WorldObject[] targets)
	{
		if ((creature == null) || !(creature instanceof PlayerInstance))
		{
			return;
		}
		
		PlayerInstance player = (PlayerInstance) creature;
		if ((player.getClan() == null) || (player.getClan().getLeaderId() != player.getObjectId()))
		{
			return;
		}
		
		Castle castle = CastleManager.getInstance().getCastle(player);
		Fort fort = FortManager.getInstance().getFort(player);
		if ((castle == null) && (fort == null))
		{
			return;
		}
		
		if (castle != null)
		{
			if (!checkIfOkToPlaceFlag(player, castle, true))
			{
				return;
			}
		}
		else if (!checkIfOkToPlaceFlag(player, fort, true))
		{
			return;
		}
		
		try
		{
			// Spawn a new flag
			SiegeFlagInstance flag = new SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(35062));
			
			if (skill.is_advancedFlag())
			{
				flag.set_advanceFlag(true);
				flag.set_advanceMultiplier(skill.get_advancedMultiplier());
			}
			
			flag.setTitle(player.getClan().getName());
			flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
			flag.setHeading(player.getHeading());
			flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
			
			if (castle != null)
			{
				castle.getSiege().getFlag(player.getClan()).add(flag);
			}
			else
			{
				fort.getSiege().getFlag(player.getClan()).add(flag);
			}
		}
		catch (Exception e)
		{
			player.sendMessage("Error placing flag:" + e);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	
	/**
	 * Return true if character clan place a flag<BR>
	 * <BR>
	 * @param creature The Creature of the creature placing the flag
	 * @param isCheckOnly if false, it will send a notification to the player telling him why it failed
	 * @return
	 */
	public static boolean checkIfOkToPlaceFlag(Creature creature, boolean isCheckOnly)
	{
		final Castle castle = CastleManager.getInstance().getCastle(creature);
		final Fort fort = FortManager.getInstance().getFort(creature);
		if ((castle == null) && (fort == null))
		{
			return false;
		}
		
		if (castle != null)
		{
			return checkIfOkToPlaceFlag(creature, castle, isCheckOnly);
		}
		return checkIfOkToPlaceFlag(creature, fort, isCheckOnly);
	}
	
	public static boolean checkIfOkToPlaceFlag(Creature creature, Castle castle, boolean isCheckOnly)
	{
		if ((creature == null) || !(creature instanceof PlayerInstance))
		{
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		final PlayerInstance player = (PlayerInstance) creature;
		
		if ((castle == null) || (castle.getCastleId() <= 0))
		{
			sm.addString("You must be on castle ground to place a flag");
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only place a flag during a siege.");
		}
		else if (castle.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm.addString("You must be an attacker to place a flag");
		}
		else if ((player.getClan() == null) || !player.isClanLeader())
		{
			sm.addString("You must be a clan leader to place a flag");
		}
		else if (castle.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= SiegeManager.getInstance().getFlagMaxCount())
		{
			sm.addString("You have already placed the maximum number of flags possible");
		}
		else if (player.isInsideZone(ZoneId.NOHQ))
		{
			sm.addString("You cannot place flag here.");
		}
		else
		{
			return true;
		}
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		return false;
	}
	
	public static boolean checkIfOkToPlaceFlag(Creature creature, Fort fort, boolean isCheckOnly)
	{
		if ((creature == null) || !(creature instanceof PlayerInstance))
		{
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		final PlayerInstance player = (PlayerInstance) creature;
		
		if ((fort == null) || (fort.getFortId() <= 0))
		{
			sm.addString("You must be on fort ground to place a flag");
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm.addString("You can only place a flag during a siege.");
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm.addString("You must be an attacker to place a flag");
		}
		else if ((player.getClan() == null) || !player.isClanLeader())
		{
			sm.addString("You must be a clan leader to place a flag");
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= FortSiegeManager.getInstance().getFlagMaxCount())
		{
			sm.addString("You have already placed the maximum number of flags possible");
		}
		else if (player.isInsideZone(ZoneId.NOHQ))
		{
			sm.addString("You cannot place flag here.");
		}
		else
		{
			return true;
		}
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		
		return false;
	}
}