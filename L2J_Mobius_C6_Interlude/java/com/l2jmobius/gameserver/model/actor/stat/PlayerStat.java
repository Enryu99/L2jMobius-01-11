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
package com.l2jmobius.gameserver.model.actor.stat;

import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.xml.ExperienceData;
import com.l2jmobius.gameserver.model.actor.instance.ClassMasterInstance;
import com.l2jmobius.gameserver.model.actor.instance.PetInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.base.ClassLevel;
import com.l2jmobius.gameserver.model.base.PlayerClass;
import com.l2jmobius.gameserver.model.base.SubClass;
import com.l2jmobius.gameserver.model.entity.event.TvT;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SocialAction;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;

public class PlayerStat extends PlayableStat
{
	private static Logger LOGGER = Logger.getLogger(PlayerStat.class.getName());
	
	private int _oldMaxHp; // stats watch
	private int _oldMaxMp; // stats watch
	private int _oldMaxCp; // stats watch
	
	public PlayerStat(PlayerInstance player)
	{
		super(player);
	}
	
	@Override
	public boolean addExp(long value)
	{
		PlayerInstance player = getActiveChar();
		
		// Player is Gm and access level is below or equal to canGainExp and is in party, don't give XP
		if (!getActiveChar().getAccessLevel().canGainExp() && getActiveChar().isInParty())
		{
			return false;
		}
		
		if (!super.addExp(value))
		{
			return false;
		}
		
		// Set new karma
		if (!player.isCursedWeaponEquiped() && (player.getKarma() > 0) && (player.isGM() || !player.isInsideZone(ZoneId.PVP)))
		{
			final int karmaLost = player.calculateKarmaLost(value);
			
			if (karmaLost > 0)
			{
				player.setKarma(player.getKarma() - karmaLost);
			}
		}
		
		player.sendPacket(new UserInfo(player));
		return true;
	}
	
	/**
	 * Add Experience and SP rewards to the PlayerInstance, remove its Karma (if necessary) and Launch increase level task.<BR>
	 * <BR>
	 * <B><U> Actions </U> :</B><BR>
	 * <BR>
	 * <li>Remove Karma when the player kills MonsterInstance</li>
	 * <li>Send a Server->Client packet StatusUpdate to the PlayerInstance</li>
	 * <li>Send a Server->Client System Message to the PlayerInstance</li>
	 * <li>If the PlayerInstance increases it's level, send a Server->Client packet SocialAction (broadcast)</li>
	 * <li>If the PlayerInstance increases it's level, manage the increase level task (Max MP, Max MP, Recommendation, Expertise and beginner skills...)</li>
	 * <li>If the PlayerInstance increases it's level, send a Server->Client packet UserInfo to the PlayerInstance</li><BR>
	 * <BR>
	 * @param addToExp The Experience value to add
	 * @param addToSp The SP value to add
	 */
	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		float ratioTakenByPet = 0;
		
		// Player is Gm and acces level is below or equal to GM_DONT_TAKE_EXPSP and is in party, don't give Xp/Sp
		PlayerInstance player = getActiveChar();
		if (!player.getAccessLevel().canGainExp() && player.isInParty())
		{
			return false;
		}
		
		// if this player has a pet that takes from the owner's Exp, give the pet Exp now
		
		if (player.getPet() instanceof PetInstance)
		{
			PetInstance pet = (PetInstance) player.getPet();
			ratioTakenByPet = pet.getPetData().getOwnerExpTaken();
			
			// only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
			// allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
			if ((ratioTakenByPet > 0) && !pet.isDead())
			{
				pet.addExpAndSp((long) (addToExp * ratioTakenByPet), (int) (addToSp * ratioTakenByPet));
			}
			
			// now adjust the max ratio to avoid the owner earning negative exp/sp
			if (ratioTakenByPet > 1)
			{
				ratioTakenByPet = 1;
			}
			
			addToExp = (long) (addToExp * (1 - ratioTakenByPet));
			addToSp = (int) (addToSp * (1 - ratioTakenByPet));
		}
		
		if (!super.addExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		// Send a Server->Client System Message to the PlayerInstance
		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP);
		sm.addNumber((int) addToExp);
		sm.addNumber(addToSp);
		getActiveChar().sendPacket(sm);
		
		return true;
	}
	
	@Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
	{
		if (!super.removeExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		// Send a Server->Client System Message to the PlayerInstance
		SystemMessage sm = new SystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
		sm.addNumber((int) addToExp);
		getActiveChar().sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
		sm.addNumber(addToSp);
		getActiveChar().sendPacket(sm);
		
		return true;
	}
	
	@Override
	public final boolean addLevel(byte value)
	{
		if ((getLevel() + value) > (ExperienceData.getInstance().getMaxLevel() - 1))
		{
			return false;
		}
		
		final boolean levelIncreased = super.addLevel(value);
		
		if (Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS)
		{
			final ClassMasterInstance master_instance = ClassMasterInstance.getInstance();
			
			if (master_instance != null)
			{
				
				final ClassLevel lvlnow = PlayerClass.values()[getActiveChar().getClassId().getId()].getLevel();
				if ((getLevel() >= 20) && (lvlnow == ClassLevel.First))
				{
					ClassMasterInstance.getInstance().onAction(getActiveChar());
				}
				else if ((getLevel() >= 40) && (lvlnow == ClassLevel.Second))
				{
					ClassMasterInstance.getInstance().onAction(getActiveChar());
				}
				else if ((getLevel() >= 76) && (lvlnow == ClassLevel.Third))
				{
					ClassMasterInstance.getInstance().onAction(getActiveChar());
				}
				
			}
			else
			{
				LOGGER.info("Attention: Remote ClassMaster is Enabled, but not inserted into DataBase. Remember to install 31288 Custom_Npc...");
			}
		}
		
		if (levelIncreased)
		{
			if ((getActiveChar().getLevel() >= Config.MAX_LEVEL_NEWBIE_STATUS) && getActiveChar().isNewbie())
			{
				getActiveChar().setNewbie(false);
			}
			
			QuestState qs = getActiveChar().getQuestState("255_Tutorial");
			
			if ((qs != null) && (qs.getQuest() != null))
			{
				qs.getQuest().notifyEvent("CE40", null, getActiveChar());
			}
			
			getActiveChar().setCurrentCp(getMaxCp());
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), 15));
			getActiveChar().sendPacket(SystemMessageId.YOU_INCREASED_YOUR_LEVEL);
		}
		
		if (getActiveChar().isInFunEvent())
		{
			if (getActiveChar()._inEventTvT && (TvT.get_maxlvl() == getLevel()) && !TvT.is_started())
			{
				TvT.removePlayer(getActiveChar());
			}
			getActiveChar().sendMessage("Your event sign up was canceled.");
		}
		
		getActiveChar().rewardSkills(); // Give Expertise skill of this level
		
		if (getActiveChar().getClan() != null)
		{
			getActiveChar().getClan().updateClanMember(getActiveChar());
			getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
		}
		
		if (getActiveChar().isInParty())
		{
			getActiveChar().getParty().recalculatePartyLevel(); // Recalculate the party level
		}
		
		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		getActiveChar().sendPacket(su);
		
		// Update the overloaded status of the PlayerInstance
		getActiveChar().refreshOverloaded();
		// Update the expertise status of the PlayerInstance
		getActiveChar().refreshExpertisePenalty();
		// Send a Server->Client packet UserInfo to the PlayerInstance
		getActiveChar().sendPacket(new UserInfo(getActiveChar()));
		// getActiveChar().setLocked(false);
		return levelIncreased;
	}
	
	@Override
	public boolean addSp(int value)
	{
		if (!super.addSp(value))
		{
			return false;
		}
		
		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.SP, getSp());
		getActiveChar().sendPacket(su);
		
		return true;
	}
	
	@Override
	public final long getExpForLevel(int level)
	{
		return ExperienceData.getInstance().getExpForLevel(level);
	}
	
	@Override
	public final PlayerInstance getActiveChar()
	{
		return (PlayerInstance) super.getActiveChar();
	}
	
	@Override
	public final long getExp()
	{
		final PlayerInstance player = getActiveChar();
		if ((player != null) && player.isSubClassActive())
		{
			
			final int class_index = player.getClassIndex();
			
			SubClass player_subclass = player.getSubClasses().get(class_index);
			if (player_subclass != null)
			{
				return player_subclass.getExp();
			}
			
		}
		
		return super.getExp();
	}
	
	@Override
	public final void setExp(long value)
	{
		final PlayerInstance player = getActiveChar();
		
		if (player.isSubClassActive())
		{
			final int class_index = player.getClassIndex();
			
			SubClass player_subclass = player.getSubClasses().get(class_index);
			if (player_subclass != null)
			{
				player_subclass.setExp(value);
			}
		}
		else
		{
			super.setExp(value);
		}
	}
	
	@Override
	public final int getLevel()
	{
		try
		{
			final PlayerInstance player = getActiveChar();
			if (player.isSubClassActive())
			{
				final int class_index = player.getClassIndex();
				
				SubClass player_subclass = player.getSubClasses().get(class_index);
				if (player_subclass != null)
				{
					return player_subclass.getLevel();
				}
			}
			return super.getLevel();
		}
		catch (NullPointerException e)
		{
			return -1;
		}
	}
	
	@Override
	public final void setLevel(int value)
	{
		if (value > (ExperienceData.getInstance().getMaxLevel() - 1))
		{
			value = ExperienceData.getInstance().getMaxLevel() - 1;
		}
		
		final PlayerInstance player = getActiveChar();
		
		if (player.isSubClassActive())
		{
			final int class_index = player.getClassIndex();
			
			SubClass player_subclass = player.getSubClasses().get(class_index);
			if (player_subclass != null)
			{
				player_subclass.setLevel(value);
			}
		}
		else
		{
			super.setLevel(value);
		}
	}
	
	@Override
	public final int getMaxCp()
	{
		final int val = super.getMaxCp();
		
		if (val != _oldMaxCp)
		{
			_oldMaxCp = val;
			
			final PlayerInstance player = getActiveChar();
			
			if (player.getStatus().getCurrentCp() != val)
			{
				player.getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp());
			}
		}
		return val;
	}
	
	@Override
	public final int getMaxHp()
	{
		// Get the Max HP (base+modifier) of the PlayerInstance
		final int val = super.getMaxHp();
		
		if (val != _oldMaxHp)
		{
			_oldMaxHp = val;
			
			final PlayerInstance player = getActiveChar();
			
			// Launch a regen task if the new Max HP is higher than the old one
			if (player.getStatus().getCurrentHp() != val)
			{
				player.getStatus().setCurrentHp(player.getStatus().getCurrentHp()); // trigger start of regeneration
			}
		}
		
		return val;
	}
	
	@Override
	public final int getMaxMp()
	{
		// Get the Max MP (base+modifier) of the PlayerInstance
		final int val = super.getMaxMp();
		
		if (val != _oldMaxMp)
		{
			_oldMaxMp = val;
			
			final PlayerInstance player = getActiveChar();
			
			// Launch a regen task if the new Max MP is higher than the old one
			if (player.getStatus().getCurrentMp() != val)
			{
				player.getStatus().setCurrentMp(player.getStatus().getCurrentMp()); // trigger start of regeneration
			}
		}
		
		return val;
	}
	
	@Override
	public final int getSp()
	{
		final PlayerInstance player = getActiveChar();
		if (player.isSubClassActive())
		{
			final int class_index = player.getClassIndex();
			
			SubClass player_subclass = player.getSubClasses().get(class_index);
			if (player_subclass != null)
			{
				return player_subclass.getSp();
			}
		}
		return super.getSp();
	}
	
	@Override
	public final void setSp(int value)
	{
		
		final PlayerInstance player = getActiveChar();
		
		if (player.isSubClassActive())
		{
			final int class_index = player.getClassIndex();
			
			SubClass player_subclass = player.getSubClasses().get(class_index);
			if (player_subclass != null)
			{
				player_subclass.setSp(value);
			}
		}
		else
		{
			super.setSp(value);
		}
	}
}
