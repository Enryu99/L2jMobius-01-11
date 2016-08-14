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
package com.l2jmobius.gameserver.ai;

import static com.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static com.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;

import java.util.ArrayList;
import java.util.List;

import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.MobGroup;
import com.l2jmobius.gameserver.model.MobGroupTable;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.L2Playable;
import com.l2jmobius.gameserver.model.actor.instance.L2ControllableMobInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.util.Util;

/**
 * AI for controllable mobs
 * @author littlecrow
 */
public final class L2ControllableMobAI extends L2AttackableAI
{
	public static final int AI_IDLE = 1;
	public static final int AI_NORMAL = 2;
	public static final int AI_FORCEATTACK = 3;
	public static final int AI_FOLLOW = 4;
	public static final int AI_CAST = 5;
	public static final int AI_ATTACK_GROUP = 6;
	
	private int _alternateAI;
	
	private boolean _isThinking; // to prevent thinking recursively
	private boolean _isNotMoving;
	
	private L2Character _forcedTarget;
	private MobGroup _targetGroup;
	
	protected void thinkFollow()
	{
		final L2Attackable me = (L2Attackable) _actor;
		
		if (!Util.checkIfInRange(MobGroupTable.FOLLOW_RANGE, me, getForcedTarget(), true))
		{
			final int signX = (Rnd.nextInt(2) == 0) ? -1 : 1;
			final int signY = (Rnd.nextInt(2) == 0) ? -1 : 1;
			final int randX = Rnd.nextInt(MobGroupTable.FOLLOW_RANGE);
			final int randY = Rnd.nextInt(MobGroupTable.FOLLOW_RANGE);
			
			moveTo(getForcedTarget().getX() + (signX * randX), getForcedTarget().getY() + (signY * randY), getForcedTarget().getZ());
		}
	}
	
	@Override
	protected void onEvtThink()
	{
		if (isThinking())
		{
			return;
		}
		
		setThinking(true);
		
		try
		{
			switch (getAlternateAI())
			{
				case AI_IDLE:
				{
					if (getIntention() != CtrlIntention.AI_INTENTION_ACTIVE)
					{
						setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					}
					break;
				}
				case AI_FOLLOW:
				{
					thinkFollow();
					break;
				}
				case AI_CAST:
				{
					thinkCast();
					break;
				}
				case AI_FORCEATTACK:
				{
					thinkForceAttack();
					break;
				}
				case AI_ATTACK_GROUP:
				{
					thinkAttackGroup();
					break;
				}
				default:
				{
					if (getIntention() == AI_INTENTION_ACTIVE)
					{
						thinkActive();
					}
					else if (getIntention() == AI_INTENTION_ATTACK)
					{
						thinkAttack();
					}
					break;
				}
			}
		}
		finally
		{
			setThinking(false);
		}
	}
	
	@Override
	protected void thinkCast()
	{
		L2Object target = _skill.getTarget(_actor, _forceUse, _dontMove, false);
		if ((target == null) || !target.isCharacter() || ((L2Character) target).isAlikeDead())
		{
			target = _skill.getTarget(_actor, findNextRndTarget(), _forceUse, _dontMove, false);
		}
		
		if (target == null)
		{
			return;
		}
		
		setTarget(target);
		
		if (!_actor.isMuted())
		{
			int max_range = 0;
			// check distant skills
			
			for (Skill sk : _actor.getAllSkills())
			{
				if (Util.checkIfInRange(sk.getCastRange(), _actor, target, true) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
				{
					_actor.doCast(sk);
					return;
				}
				
				max_range = Math.max(max_range, sk.getCastRange());
			}
			
			if (!isNotMoving())
			{
				moveToPawn(target, max_range);
			}
			
			return;
		}
	}
	
	protected void thinkAttackGroup()
	{
		final L2Character target = getForcedTarget();
		if ((target == null) || target.isAlikeDead())
		{
			// try to get next group target
			setForcedTarget(findNextGroupTarget());
			clientStopMoving(null);
		}
		
		if (target == null)
		{
			return;
		}
		
		setTarget(target);
		// as a response, we put the target in a forcedattack mode
		final L2ControllableMobInstance theTarget = (L2ControllableMobInstance) target;
		final L2ControllableMobAI ctrlAi = (L2ControllableMobAI) theTarget.getAI();
		ctrlAi.forceAttack(_actor);
		
		final double dist2 = _actor.calculateDistance(target, false, true);
		final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
		int max_range = range;
		
		if (!_actor.isMuted() && (dist2 > ((range + 20) * (range + 20))))
		{
			// check distant skills
			for (Skill sk : _actor.getAllSkills())
			{
				final int castRange = sk.getCastRange();
				
				if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
				{
					_actor.doCast(sk);
					return;
				}
				
				max_range = Math.max(max_range, castRange);
			}
			
			if (!isNotMoving())
			{
				moveToPawn(target, range);
			}
			
			return;
		}
		_actor.doAttack(target);
	}
	
	protected void thinkForceAttack()
	{
		if ((getForcedTarget() == null) || getForcedTarget().isAlikeDead())
		{
			clientStopMoving(null);
			setIntention(AI_INTENTION_ACTIVE);
			setAlternateAI(AI_IDLE);
		}
		
		setTarget(getForcedTarget());
		final double dist2 = _actor.calculateDistance(getForcedTarget(), false, true);
		final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + getForcedTarget().getTemplate().getCollisionRadius();
		int max_range = range;
		
		if (!_actor.isMuted() && (dist2 > ((range + 20) * (range + 20))))
		{
			// check distant skills
			for (Skill sk : _actor.getAllSkills())
			{
				final int castRange = sk.getCastRange();
				
				if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
				{
					_actor.doCast(sk);
					return;
				}
				
				max_range = Math.max(max_range, castRange);
			}
			
			if (!isNotMoving())
			{
				moveToPawn(getForcedTarget(), _actor.getPhysicalAttackRange()/* range */);
			}
			
			return;
		}
		
		_actor.doAttack(getForcedTarget());
	}
	
	@Override
	protected void thinkAttack()
	{
		L2Character target = getForcedTarget();
		if ((target == null) || target.isAlikeDead())
		{
			if (target != null)
			{
				// stop hating
				final L2Attackable npc = (L2Attackable) _actor;
				npc.stopHating(target);
			}
			
			setIntention(AI_INTENTION_ACTIVE);
		}
		else
		{
			// notify aggression
			final L2Character finalTarget = target;
			if (((L2Npc) _actor).getTemplate().getClans() != null)
			{
				L2World.getInstance().forEachVisibleObject(_actor, L2Npc.class, npc ->
				{
					if (!npc.isInMyClan((L2Npc) _actor))
					{
						return;
					}
					
					if (_actor.isInsideRadius(npc, npc.getTemplate().getClanHelpRange(), true, true))
					{
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, finalTarget, 1);
					}
				});
			}
			
			setTarget(target);
			final double dist2 = _actor.calculateDistance(target, false, true);
			final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
			int max_range = range;
			
			if (!_actor.isMuted() && (dist2 > ((range + 20) * (range + 20))))
			{
				// check distant skills
				for (Skill sk : _actor.getAllSkills())
				{
					final int castRange = sk.getCastRange();
					
					if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
					{
						_actor.doCast(sk);
						return;
					}
					
					max_range = Math.max(max_range, castRange);
				}
				
				moveToPawn(target, range);
				return;
			}
			
			// Force mobs to attack anybody if confused.
			L2Character hated;
			
			if (_actor.isConfused())
			{
				hated = findNextRndTarget();
			}
			else
			{
				hated = target;
			}
			
			if (hated == null)
			{
				setIntention(AI_INTENTION_ACTIVE);
				return;
			}
			
			if (hated != target)
			{
				target = hated;
			}
			
			if (!_actor.isMuted() && (Rnd.nextInt(5) == 3))
			{
				for (Skill sk : _actor.getAllSkills())
				{
					final int castRange = sk.getCastRange();
					
					if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() < _actor.getStat().getMpConsume(sk)))
					{
						_actor.doCast(sk);
						return;
					}
				}
			}
			
			_actor.doAttack(target);
		}
	}
	
	@Override
	protected void thinkActive()
	{
		L2Character hated;
		
		if (_actor.isConfused())
		{
			hated = findNextRndTarget();
		}
		else
		{
			final L2Object target = _actor.getTarget();
			hated = (target != null) && target.isCharacter() ? (L2Character) target : null;
		}
		
		if (hated != null)
		{
			_actor.setRunning();
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
		}
	}
	
	private boolean checkAutoAttackCondition(L2Character target)
	{
		if ((target == null) || !_actor.isAttackable())
		{
			return false;
		}
		final L2Attackable me = (L2Attackable) _actor;
		
		if (target.isNpc() || target.isDoor())
		{
			return false;
		}
		
		if (target.isAlikeDead() || !me.isInsideRadius(target, me.getAggroRange(), false, false) || (Math.abs(_actor.getZ() - target.getZ()) > 100))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			return false;
		}
		
		// Spawn protection (only against mobs)
		if (target.isPlayer() && ((L2PcInstance) target).isSpawnProtected())
		{
			return false;
		}
		
		// Check if the target is a L2Playable
		if (target.isPlayable())
		{
			// Check if the target isn't in silent move mode
			if (((L2Playable) target).isSilentMovingAffected())
			{
				return false;
			}
		}
		
		if (target.isNpc())
		{
			return false;
		}
		
		return me.isAggressive();
	}
	
	private L2Character findNextRndTarget()
	{
		final List<L2Character> potentialTarget = new ArrayList<>();
		L2World.getInstance().forEachVisibleObject(_actor, L2Character.class, target ->
		{
			if (Util.checkIfInShortRange(((L2Attackable) _actor).getAggroRange(), _actor, target, true) && checkAutoAttackCondition(target))
			{
				potentialTarget.add(target);
			}
		});
		
		return !potentialTarget.isEmpty() ? potentialTarget.get(Rnd.nextInt(potentialTarget.size())) : null;
	}
	
	private L2ControllableMobInstance findNextGroupTarget()
	{
		return getGroupTarget().getRandomMob();
	}
	
	public L2ControllableMobAI(L2ControllableMobInstance controllableMob)
	{
		super(controllableMob);
		setAlternateAI(AI_IDLE);
	}
	
	public int getAlternateAI()
	{
		return _alternateAI;
	}
	
	public void setAlternateAI(int _alternateai)
	{
		_alternateAI = _alternateai;
	}
	
	public void forceAttack(L2Character target)
	{
		setAlternateAI(AI_FORCEATTACK);
		setForcedTarget(target);
	}
	
	public void forceAttackGroup(MobGroup group)
	{
		setForcedTarget(null);
		setGroupTarget(group);
		setAlternateAI(AI_ATTACK_GROUP);
	}
	
	public void stop()
	{
		setAlternateAI(AI_IDLE);
		clientStopMoving(null);
	}
	
	public void move(int x, int y, int z)
	{
		moveTo(x, y, z);
	}
	
	public void follow(L2Character target)
	{
		setAlternateAI(AI_FOLLOW);
		setForcedTarget(target);
	}
	
	public boolean isThinking()
	{
		return _isThinking;
	}
	
	public boolean isNotMoving()
	{
		return _isNotMoving;
	}
	
	public void setNotMoving(boolean isNotMoving)
	{
		_isNotMoving = isNotMoving;
	}
	
	public void setThinking(boolean isThinking)
	{
		_isThinking = isThinking;
	}
	
	private L2Character getForcedTarget()
	{
		return _forcedTarget;
	}
	
	private MobGroup getGroupTarget()
	{
		return _targetGroup;
	}
	
	private void setForcedTarget(L2Character forcedTarget)
	{
		_forcedTarget = forcedTarget;
	}
	
	private void setGroupTarget(MobGroup targetGroup)
	{
		_targetGroup = targetGroup;
	}
}
