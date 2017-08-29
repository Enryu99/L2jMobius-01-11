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
package com.l2jmobius.gameserver.model.zone.type;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.data.xml.impl.SkillData;
import com.l2jmobius.gameserver.enums.MountType;
import com.l2jmobius.gameserver.instancemanager.FortManager;
import com.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import com.l2jmobius.gameserver.instancemanager.ZoneManager;
import com.l2jmobius.gameserver.model.TeleportWhereType;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.entity.Fort;
import com.l2jmobius.gameserver.model.entity.FortSiege;
import com.l2jmobius.gameserver.model.entity.Siegable;
import com.l2jmobius.gameserver.model.skills.BuffInfo;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.zone.AbstractZoneSettings;
import com.l2jmobius.gameserver.model.zone.L2ZoneType;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.SystemMessageId;

/**
 * A siege zone
 * @author durgus
 */
public class L2SiegeZone extends L2ZoneType
{
	private static final int DISMOUNT_DELAY = 5;
	
	public L2SiegeZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
	}
	
	public final class Settings extends AbstractZoneSettings
	{
		private int _siegableId = -1;
		private Siegable _siege = null;
		private boolean _isActiveSiege = false;
		
		protected Settings()
		{
		}
		
		public int getSiegeableId()
		{
			return _siegableId;
		}
		
		protected void setSiegeableId(int id)
		{
			_siegableId = id;
		}
		
		public Siegable getSiege()
		{
			return _siege;
		}
		
		public void setSiege(Siegable s)
		{
			_siege = s;
		}
		
		public boolean isActiveSiege()
		{
			return _isActiveSiege;
		}
		
		public void setActiveSiege(boolean val)
		{
			_isActiveSiege = val;
		}
		
		@Override
		public void clear()
		{
			_siegableId = -1;
			_siege = null;
			_isActiveSiege = false;
		}
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
		}
		else if (name.equals("fortId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (getSettings().isActiveSiege())
		{
			character.setInsideZone(ZoneId.PVP, true);
			character.setInsideZone(ZoneId.SIEGE, true);
			character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true); // FIXME: Custom ?
			
			if (character.isPlayer())
			{
				final L2PcInstance plyer = character.getActingPlayer();
				if (plyer.isRegisteredOnThisSiegeField(getSettings().getSiegeableId()))
				{
					plyer.setIsInSiege(true); // in siege
					if (getSettings().getSiege().giveFame() && (getSettings().getSiege().getFameFrequency() > 0))
					{
						plyer.startFameTask(getSettings().getSiege().getFameFrequency() * 1000, getSettings().getSiege().getFameAmount());
					}
				}
				
				character.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
				if (!Config.ALLOW_WYVERN_DURING_SIEGE && (plyer.getMountType() == MountType.WYVERN))
				{
					plyer.sendPacket(SystemMessageId.THIS_AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_ATOP_OF_A_WYVERN_YOU_WILL_BE_DISMOUNTED_FROM_YOUR_WYVERN_IF_YOU_DO_NOT_LEAVE);
					plyer.enteredNoLanding(DISMOUNT_DELAY);
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(ZoneId.PVP, false);
		character.setInsideZone(ZoneId.SIEGE, false);
		character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false); // FIXME: Custom ?
		if (getSettings().isActiveSiege())
		{
			if (character.isPlayer())
			{
				final L2PcInstance player = character.getActingPlayer();
				character.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
				if (player.getMountType() == MountType.WYVERN)
				{
					player.exitedNoLanding();
				}
				// Set pvp flag
				if (player.getPvpFlag() == 0)
				{
					player.startPvPFlag();
				}
			}
		}
		if (character.isPlayer())
		{
			final L2PcInstance activeChar = character.getActingPlayer();
			activeChar.stopFameTask();
			activeChar.setIsInSiege(false);
			
			if ((getSettings().getSiege() instanceof FortSiege) && (activeChar.getInventory().getItemByItemId(9819) != null))
			{
				// drop combat flag
				final Fort fort = FortManager.getInstance().getFortById(getSettings().getSiegeableId());
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(activeChar, fort.getResidenceId());
				}
				else
				{
					final int slot = activeChar.getInventory().getSlotFromItem(activeChar.getInventory().getItemByItemId(9819));
					activeChar.getInventory().unEquipItemInBodySlot(slot);
					activeChar.destroyItem("CombatFlag", activeChar.getInventory().getItemByItemId(9819), null, true);
				}
			}
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
		if (getSettings().isActiveSiege())
		{
			// debuff participants only if they die inside siege zone
			if (character.isPlayer() && character.getActingPlayer().isRegisteredOnThisSiegeField(getSettings().getSiegeableId()))
			{
				int lvl = 1;
				final BuffInfo info = character.getEffectList().getBuffInfoBySkillId(5660);
				if (info != null)
				{
					lvl = Math.min(lvl + info.getSkill().getLevel(), 5);
				}
				
				final Skill skill = SkillData.getInstance().getSkill(5660, lvl);
				if (skill != null)
				{
					skill.applyEffects(character, character);
				}
			}
		}
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getSettings().isActiveSiege())
		{
			for (L2Character character : getCharactersInside())
			{
				if (character != null)
				{
					onEnter(character);
				}
			}
		}
		else
		{
			L2PcInstance player;
			for (L2Character character : getCharactersInside())
			{
				if (character == null)
				{
					continue;
				}
				
				character.setInsideZone(ZoneId.PVP, false);
				character.setInsideZone(ZoneId.SIEGE, false);
				character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
				
				if (character.isPlayer())
				{
					player = character.getActingPlayer();
					character.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
					player.stopFameTask();
					if (player.getMountType() == MountType.WYVERN)
					{
						player.exitedNoLanding();
					}
				}
			}
		}
	}
	
	/**
	 * Sends a message to all players in this zone
	 * @param message
	 */
	public void announceToPlayers(String message)
	{
		for (L2PcInstance player : getPlayersInside())
		{
			if (player != null)
			{
				player.sendMessage(message);
			}
		}
	}
	
	public int getSiegeObjectId()
	{
		return getSettings().getSiegeableId();
	}
	
	public boolean isActive()
	{
		return getSettings().isActiveSiege();
	}
	
	public void setIsActive(boolean val)
	{
		getSettings().setActiveSiege(val);
	}
	
	public void setSiegeInstance(Siegable siege)
	{
		getSettings().setSiege(siege);
	}
	
	/**
	 * Removes all foreigners from the zone
	 * @param owningClanId
	 */
	public void banishForeigners(int owningClanId)
	{
		final TeleportWhereType type = TeleportWhereType.TOWN;
		for (L2PcInstance temp : getPlayersInside())
		{
			if (temp.getClanId() == owningClanId)
			{
				continue;
			}
			
			temp.teleToLocation(type);
		}
	}
}
