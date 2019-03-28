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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.data.xml.impl.SkillData;
import com.l2jmobius.gameserver.enums.InstanceType;
import com.l2jmobius.gameserver.instancemanager.ZoneManager;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.zone.TaskZoneSettings;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.model.zone.ZoneType;
import com.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * another type of damage zone with skills
 * @author kerberos
 */
public class EffectZone extends ZoneType
{
	int _chance;
	private int _initialDelay;
	private int _reuse;
	protected boolean _bypassConditions;
	private boolean _isShowDangerIcon;
	protected volatile Map<Integer, Integer> _skills;
	
	public EffectZone(int id)
	{
		super(id);
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		setTargetType(InstanceType.Playable); // default only playable
		_bypassConditions = false;
		_isShowDangerIcon = true;
		setSettings(ZoneManager.getSettings(getName()) == null ? new TaskZoneSettings() : ZoneManager.getSettings(getName()));
	}
	
	@Override
	public TaskZoneSettings getSettings()
	{
		return (TaskZoneSettings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("chance"))
		{
			_chance = Integer.parseInt(value);
		}
		else if (name.equals("initialDelay"))
		{
			_initialDelay = Integer.parseInt(value);
		}
		else if (name.equals("reuse"))
		{
			_reuse = Integer.parseInt(value);
		}
		else if (name.equals("bypassSkillConditions"))
		{
			_bypassConditions = Boolean.parseBoolean(value);
		}
		else if (name.equals("maxDynamicSkillCount"))
		{
			_skills = new ConcurrentHashMap<>(Integer.parseInt(value));
		}
		else if (name.equals("skillIdLvl"))
		{
			final String[] propertySplit = value.split(";");
			_skills = new ConcurrentHashMap<>(propertySplit.length);
			for (String skill : propertySplit)
			{
				final String[] skillSplit = skill.split("-");
				if (skillSplit.length != 2)
				{
					LOGGER.warning(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"" + skill + "\"");
				}
				else
				{
					try
					{
						_skills.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							LOGGER.warning(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"" + skillSplit[0] + "\"" + skillSplit[1]);
						}
					}
				}
			}
		}
		else if (name.equals("showDangerIcon"))
		{
			_isShowDangerIcon = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if ((_skills != null) && (getSettings().getTask() == null))
		{
			synchronized (this)
			{
				getSettings().setTask(ThreadPool.scheduleAtFixedRate(new ApplySkill(), _initialDelay, _reuse));
			}
		}
		if (!creature.isPlayer())
		{
			return;
		}
		creature.setInsideZone(ZoneId.ALTERED, true);
		if (!_isShowDangerIcon)
		{
			return;
		}
		creature.setInsideZone(ZoneId.DANGER_AREA, true);
		creature.sendPacket(new EtcStatusUpdate(creature.getActingPlayer()));
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.ALTERED, false);
			if (_isShowDangerIcon)
			{
				creature.setInsideZone(ZoneId.DANGER_AREA, false);
				if (!creature.isInsideZone(ZoneId.DANGER_AREA))
				{
					creature.sendPacket(new EtcStatusUpdate(creature.getActingPlayer()));
				}
			}
		}
		if (_characterList.isEmpty() && (getSettings().getTask() != null))
		{
			getSettings().clear();
		}
	}
	
	protected Skill getSkill(int skillId, int skillLvl)
	{
		return SkillData.getInstance().getSkill(skillId, skillLvl);
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public void addSkill(int skillId, int skillLvL)
	{
		if (skillLvL < 1) // remove skill
		{
			removeSkill(skillId);
			return;
		}
		
		if (_skills == null)
		{
			synchronized (this)
			{
				_skills = new ConcurrentHashMap<>(3);
			}
		}
		_skills.put(skillId, skillLvL);
	}
	
	public void removeSkill(int skillId)
	{
		if (_skills != null)
		{
			_skills.remove(skillId);
		}
	}
	
	public void clearSkills()
	{
		if (_skills != null)
		{
			_skills.clear();
		}
	}
	
	public int getSkillLevel(int skillId)
	{
		return (_skills == null) || !_skills.containsKey(skillId) ? 0 : _skills.get(skillId);
	}
	
	private final class ApplySkill implements Runnable
	{
		protected ApplySkill()
		{
			if (_skills == null)
			{
				throw new IllegalStateException("No skills defined.");
			}
		}
		
		@Override
		public void run()
		{
			if (isEnabled())
			{
				for (Creature temp : getCharactersInside())
				{
					if ((temp != null) && !temp.isDead() && (Rnd.get(100) < _chance))
					{
						for (Entry<Integer, Integer> e : _skills.entrySet())
						{
							final Skill skill = getSkill(e.getKey(), e.getValue());
							if ((skill != null) && (_bypassConditions || skill.checkCondition(temp, temp, false)) && !temp.isAffectedBySkill(e.getKey()))
							{
								skill.applyEffects(temp, temp);
							}
						}
					}
				}
			}
		}
	}
}