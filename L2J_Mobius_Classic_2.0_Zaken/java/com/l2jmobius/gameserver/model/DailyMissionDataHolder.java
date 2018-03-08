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
package com.l2jmobius.gameserver.model;

import java.util.List;
import java.util.function.Function;

import com.l2jmobius.gameserver.enums.DailyMissionStatus;
import com.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import com.l2jmobius.gameserver.handler.DailyMissionHandler;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.base.ClassId;
import com.l2jmobius.gameserver.model.holders.ItemHolder;

/**
 * @author Sdw
 */
public class DailyMissionDataHolder
{
	private final int _id;
	private final int _rewardId;
	private final List<ItemHolder> _rewardsItems;
	private final List<ClassId> _classRestriction;
	private final int _requiredCompletions;
	private final StatsSet _params;
	private final boolean _isOneTime;
	private final AbstractDailyMissionHandler _handler;
	
	public DailyMissionDataHolder(StatsSet set)
	{
		final Function<DailyMissionDataHolder, AbstractDailyMissionHandler> handler = DailyMissionHandler.getInstance().getHandler(set.getString("handler"));
		
		_id = set.getInt("id");
		_rewardId = set.getInt("reward_id");
		_requiredCompletions = set.getInt("requiredCompletion", 0);
		_rewardsItems = set.getList("items", ItemHolder.class);
		_classRestriction = set.getList("classRestriction", ClassId.class);
		_params = set.getObject("params", StatsSet.class);
		_isOneTime = set.getBoolean("isOneTime", true);
		_handler = handler != null ? handler.apply(this) : null;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getRewardId()
	{
		return _rewardId;
	}
	
	public List<ClassId> getClassRestriction()
	{
		return _classRestriction;
	}
	
	public List<ItemHolder> getRewards()
	{
		return _rewardsItems;
	}
	
	public int getRequiredCompletions()
	{
		return _requiredCompletions;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public boolean isOneTime()
	{
		return _isOneTime;
	}
	
	public boolean isDisplayable(L2PcInstance player)
	{
		return (!_isOneTime || (getStatus(player) != DailyMissionStatus.COMPLETED.getClientId())) && (_classRestriction.isEmpty() || _classRestriction.contains(player.getClassId()));
	}
	
	public void requestReward(L2PcInstance player)
	{
		if ((_handler != null) && isDisplayable(player))
		{
			_handler.requestReward(player);
		}
	}
	
	public int getStatus(L2PcInstance player)
	{
		return _handler != null ? _handler.getStatus(player) : DailyMissionStatus.NOT_AVAILABLE.getClientId();
	}
	
	public int getProgress(L2PcInstance player)
	{
		return _handler != null ? _handler.getProgress(player) : DailyMissionStatus.NOT_AVAILABLE.getClientId();
	}
	
	public void reset()
	{
		if (_handler != null)
		{
			_handler.reset();
		}
	}
}
