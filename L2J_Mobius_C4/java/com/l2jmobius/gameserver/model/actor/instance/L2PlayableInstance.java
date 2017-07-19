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

import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Effect;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.knownlist.PlayableKnownList;
import com.l2jmobius.gameserver.model.actor.stat.PlayableStat;
import com.l2jmobius.gameserver.model.actor.status.PlayableStatus;
import com.l2jmobius.gameserver.templates.L2CharTemplate;

/**
 * This class represents all Playable characters in the world.<BR>
 * <BR>
 * L2PlayableInstance :<BR>
 * <BR>
 * <li>L2PcInstance</li>
 * <li>L2Summon</li><BR>
 * <BR>
 */
public abstract class L2PlayableInstance extends L2Character
{
	private boolean _IsNoblesseBlessed = false; // for Noblesse Blessing skill, restores buffs after death
	private boolean _getCharmOfLuck = false; // Charm of Luck - During a Raid/Boss war, decreased chance of dropping items
	private boolean _IsSilentMoving = false; // Silent Move
	
	/**
	 * Constructor of L2PlayableInstance (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2PlayableInstance</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the L2PlayableInstance
	 */
	public L2PlayableInstance(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		
		getKnownList();
		getStat();
		getStatus();
	}
	
	@Override
	public PlayableKnownList getKnownList()
	{
		if ((super.getKnownList() == null) || !(super.getKnownList() instanceof PlayableKnownList))
		{
			setKnownList(new PlayableKnownList(this));
		}
		return (PlayableKnownList) super.getKnownList();
	}
	
	@Override
	public PlayableStat getStat()
	{
		if ((super.getStat() == null) || !(super.getStat() instanceof PlayableStat))
		{
			setStat(new PlayableStat(this));
		}
		return (PlayableStat) super.getStat();
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		if ((super.getStatus() == null) || !(super.getStatus() instanceof PlayableStatus))
		{
			setStatus(new PlayableStatus(this));
		}
		return (PlayableStatus) super.getStatus();
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (killer != null)
		{
			final L2PcInstance player = killer.getActingPlayer();
			if ((player != null) && (player.getEventTeam() == 0))
			{
				player.onKillUpdatePvPKarma(this);
			}
		}
		return true;
	}
	
	public boolean checkIfPvP(L2Character target)
	{
		if (target == null)
		{
			return false; // Target is null
		}
		if (target == this)
		{
			return false; // Target is self
		}
		if (!(target instanceof L2PlayableInstance))
		{
			return false; // Target is not a L2PlayableInstance
		}
		
		final L2PcInstance player = getActingPlayer();
		if (player == null)
		{
			return false; // Active player is null
		}
		
		if (player.getKarma() != 0)
		{
			return false; // Active player has karma
		}
		
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return false; // Target player is null
		}
		
		if (targetPlayer == this)
		{
			return false; // Target player is self
		}
		
		if (targetPlayer.getKarma() != 0)
		{
			return false; // Target player has karma
		}
		
		return true;
		
	}
	
	/**
	 * Return True.<BR>
	 * <BR>
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	public final boolean isNoblesseBlessed()
	{
		return _IsNoblesseBlessed;
	}
	
	public final void setIsNoblesseBlessed(boolean value)
	{
		_IsNoblesseBlessed = value;
	}
	
	public final boolean getCharmOfLuck()
	{
		return _getCharmOfLuck;
	}
	
	public final void setCharmOfLuck(boolean value)
	{
		_getCharmOfLuck = value;
	}
	
	public boolean isSilentMoving()
	{
		return _IsSilentMoving;
	}
	
	public void setSilentMoving(boolean value)
	{
		_IsSilentMoving = value;
	}
	
	public final void startNoblesseBlessing()
	{
		setIsNoblesseBlessed(true);
		updateAbnormalEffect();
	}
	
	public final void stopNoblesseBlessing(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.NOBLESSE_BLESSING);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsNoblesseBlessed(false);
		updateAbnormalEffect();
	}
	
	public final void startCharmOfLuck()
	{
		setCharmOfLuck(true);
		updateAbnormalEffect();
	}
	
	public final void stopCharmOfLuck(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.CHARM_OF_LUCK);
		}
		else
		{
			removeEffect(effect);
		}
		
		setCharmOfLuck(false);
		updateAbnormalEffect();
	}
	
	public abstract boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage);
	
	public abstract boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage);
}