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
package org.l2jmobius.gameserver.skills.handlers;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.datatables.sql.NpcTable;
import org.l2jmobius.gameserver.datatables.xml.ExperienceData;
import org.l2jmobius.gameserver.idfactory.IdFactory;
import org.l2jmobius.gameserver.model.Skill;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.CubicInstance;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.SiegeSummonInstance;
import org.l2jmobius.gameserver.model.actor.instance.SummonInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PetInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.templates.StatsSet;
import org.l2jmobius.gameserver.templates.creatures.NpcTemplate;

public class SkillSummon extends Skill
{
	public static final int SKILL_CUBIC_MASTERY = 143;
	
	private final int _npcId;
	private final float _expPenalty;
	private final boolean _isCubic;
	
	// cubic AI
	// Activation time for a cubic
	private final int _activationtime;
	// Activation chance for a cubic.
	private final int _activationchance;
	
	// What is the total lifetime of summons (in millisecs)
	private final int _summonTotalLifeTime;
	
	public SkillSummon(StatsSet set)
	{
		super(set);
		
		_npcId = set.getInteger("npcId", 0); // default for undescribed skills
		_expPenalty = set.getFloat("expPenalty", 0.f);
		_isCubic = set.getBool("isCubic", false);
		
		_activationtime = set.getInteger("activationtime", 8);
		_activationchance = set.getInteger("activationchance", 30);
		
		_summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000); // 20 minutes default
		_summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);
		
		_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
		_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
		_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
	}
	
	public boolean checkCondition(Creature creature)
	{
		if (creature instanceof PlayerInstance)
		{
			final PlayerInstance player = (PlayerInstance) creature;
			
			if (_isCubic)
			{
				if (getTargetType() != Skill.SkillTargetType.TARGET_SELF)
				{
					return true; // Player is always able to cast mass cubic skill
				}
				int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
				final int count = player.getCubics().size();
				if (count > mastery)
				{
					player.sendMessage("You already have " + count + " cubic(s).");
					return false;
				}
			}
			else
			{
				if (player.inObserverMode())
				{
					return false;
				}
				if (player.getPet() != null)
				{
					creature.sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
					return false;
				}
			}
		}
		return super.checkCondition(creature, null, false);
	}
	
	@Override
	public void useSkill(Creature caster, WorldObject[] targets)
	{
		if (caster.isAlikeDead() || !(caster instanceof PlayerInstance))
		{
			return;
		}
		
		final PlayerInstance activeChar = (PlayerInstance) caster;
		
		// Skill 2046 only used for animation
		if (getId() == 2046)
		{
			return;
		}
		
		if (_npcId == 0)
		{
			activeChar.sendMessage("Summon skill " + getId() + " not described yet");
			return;
		}
		
		if (_isCubic)
		{
			// Gnacik :
			// If skill is enchanted calculate cubic skill level based on enchant
			// 8 at 101 (+1 Power)
			// 12 at 130 (+30 Power)
			// Because 12 is max 5115-5117 skills
			// TODO: make better method of calculation, dunno how its calculated on offi
			int _cubicSkillLevel = getLevel();
			if (_cubicSkillLevel > 100)
			{
				_cubicSkillLevel = ((getLevel() - 100) / 7) + 8;
			}
			
			if (targets.length > 1) // Mass cubic skill
			{
				for (WorldObject obj : targets)
				{
					if (!(obj instanceof PlayerInstance))
					{
						continue;
					}
					final PlayerInstance player = ((PlayerInstance) obj);
					int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
					if ((mastery == 0) && !player.getCubics().isEmpty())
					{
						// Player can have only 1 cubic - we shuld replace old cubic with new one
						player.unsummonAllCubics();
					}
					// TODO: Should remove first cubic summoned and replace with new cubic
					if (player.getCubics().containsKey(_npcId))
					{
						final CubicInstance cubic = player.getCubic(_npcId);
						cubic.stopAction();
						cubic.cancelDisappear();
						player.delCubic(_npcId);
					}
					if (player.getCubics().size() > mastery)
					{
						continue;
					}
					if (player == activeChar)
					{
						player.addCubic(_npcId, _cubicSkillLevel, getPower(), _activationtime, _activationchance, _summonTotalLifeTime, false);
					}
					else
					{
						// given by other player
						player.addCubic(_npcId, _cubicSkillLevel, getPower(), _activationtime, _activationchance, _summonTotalLifeTime, true);
					}
					player.broadcastUserInfo();
				}
				return;
			}
			
			int mastery = activeChar.getSkillLevel(SKILL_CUBIC_MASTERY);
			if (activeChar.getCubics().containsKey(_npcId))
			{
				final CubicInstance cubic = activeChar.getCubic(_npcId);
				cubic.stopAction();
				cubic.cancelDisappear();
				activeChar.delCubic(_npcId);
			}
			if (activeChar.getCubics().size() > mastery)
			{
				activeChar.sendPacket(SystemMessageId.CUBIC_SUMMONING_FAILED);
				return;
			}
			activeChar.addCubic(_npcId, _cubicSkillLevel, getPower(), _activationtime, _activationchance, _summonTotalLifeTime, false);
			activeChar.broadcastUserInfo();
			return;
		}
		
		if ((activeChar.getPet() != null) || activeChar.isMounted())
		{
			return;
		}
		
		SummonInstance summon;
		final NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(_npcId);
		if (summonTemplate == null)
		{
			LOGGER.warning("Summon attempt for nonexisting NPC ID:" + _npcId + ", skill ID:" + getId());
			return; // npcID doesn't exist
		}
		if (summonTemplate.type.equalsIgnoreCase("SiegeSummon"))
		{
			summon = new SiegeSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
		}
		else
		{
			summon = new SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
		}
		
		summon.setName(summonTemplate.name);
		summon.setTitle(activeChar.getName());
		summon.setExpPenalty(_expPenalty);
		if (summon.getLevel() >= ExperienceData.getInstance().getMaxLevel())
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(ExperienceData.getInstance().getMaxPetLevel() - 1));
			LOGGER.warning("Summon (" + summon.getName() + ") NpcID: " + summon.getNpcId() + " has a level above 75. Please rectify.");
		}
		else
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(summon.getLevel() % ExperienceData.getInstance().getMaxPetLevel()));
		}
		summon.setCurrentHp(summon.getMaxHp());
		summon.setCurrentMp(summon.getMaxMp());
		summon.setHeading(activeChar.getHeading());
		summon.setRunning();
		activeChar.setPet(summon);
		
		World.getInstance().storeObject(summon);
		
		// Check to see if we should do the decay right after the cast
		if (getTargetType() == SkillTargetType.TARGET_CORPSE_MOB)
		{
			final Creature target = (Creature) targets[0];
			if (target.isDead() && (target instanceof NpcInstance))
			{
				summon.spawnMe(target.getX(), target.getY(), target.getZ() + 5);
				((NpcInstance) target).endDecayTask();
			}
		}
		else
		{
			summon.spawnMe((activeChar.getX() + Rnd.get(40)) - 20, (activeChar.getY() + Rnd.get(40)) - 20, activeChar.getZ());
		}
		
		summon.setFollowStatus(true);
		summon.setShowSummonAnimation(false); // addVisibleObject created the info packets with summon animation
		// if someone comes into range now, the animation shouldnt show any more
		activeChar.sendPacket(new PetInfo(summon));
	}
	
	public final boolean isCubic()
	{
		return _isCubic;
	}
	
}
