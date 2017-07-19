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
import static com.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.concurrent.Future;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.geodata.GeoData;
import com.l2jmobius.gameserver.model.L2Attackable;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Effect;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2FolkInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PlayableInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.l2jmobius.gameserver.util.Util;
import com.l2jmobius.util.Rnd;

/**
 * This class manages AI of L2Attackable.<BR>
 * <BR>
 */
public class L2SiegeGuardAI extends L2CharacterAI implements Runnable
{
	// protected static final Logger _log = Logger.getLogger(L2SiegeGuardAI.class.getName());
	
	private static final int MAX_ATTACK_TIMEOUT = 300; // int ticks, i.e. 30 seconds
	
	/** The L2Attackable AI task executed every 1s (call onEvtThink method) */
	private Future<?> aiTask;
	
	/** For attack AI, analysis of mob and its targets */
	private final SelfAnalysis _selfAnalysis = new SelfAnalysis();
	
	/** The delay after which the attack is stopped */
	private int _attackTimeout;
	
	/** The L2Attackable aggro counter */
	private int _globalAggro;
	
	/** The flag used to indicate that a thinking action is in progress */
	private boolean thinking; // to prevent recursive thinking
	
	private final int attackRange;
	
	/**
	 * Constructor of L2AttackableAI.<BR>
	 * <BR>
	 * @param accessor The AI accessor of the L2Character
	 */
	public L2SiegeGuardAI(L2Character.AIAccessor accessor)
	{
		super(accessor);
		
		_selfAnalysis.Init();
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
		attackRange = ((L2Attackable) _actor).getPhysicalAttackRange();
	}
	
	@Override
	public void run()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}
	
	/**
	 * Return True if the target is autoattackable (depends on the actor type).<BR>
	 * <BR>
	 * <B><U> Actor is a L2GuardInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The L2PcInstance target has karma (=PK)</li>
	 * <li>The L2MonsterInstance target is aggressive</li><BR>
	 * <BR>
	 * <B><U> Actor is a L2SiegeGuardInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>A siege is in progress</li>
	 * <li>The L2PcInstance target isn't a Defender</li><BR>
	 * <BR>
	 * <B><U> Actor is a L2FriendlyMobInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk, a Door or another L2NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The L2PcInstance target has karma (=PK)</li><BR>
	 * <BR>
	 * <B><U> Actor is a L2MonsterInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk, a Door or another L2NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The actor is Aggressive</li><BR>
	 * <BR>
	 * @param target The targeted L2Object
	 * @return
	 */
	private boolean autoAttackCondition(L2Character target)
	{
		// Check if the target isn't another guard, folk or a door
		if ((target == null) || (target instanceof L2SiegeGuardInstance) || (target instanceof L2FolkInstance) || (target instanceof L2DoorInstance))
		{
			return false;
		}
		
		// Check if the target isn't dead
		if (target.isAlikeDead())
		{
			return false;
		}
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			if ((target.getActingPlayer() != null) && target.getActingPlayer().isGM())
			{
				return false;
			}
		}
		
		// Get the owner if the target is a summon
		if (target instanceof L2Summon)
		{
			final L2PcInstance owner = ((L2Summon) target).getOwner();
			if (_actor.isInsideRadius(owner, 1000, true, false))
			{
				target = owner;
			}
		}
		
		// Check if the target is a L2PcInstance
		if (target instanceof L2PlayableInstance)
		{
			// Check if the target isn't in silent move mode AND too far (>250)
			if (((L2PlayableInstance) target).isSilentMoving() && !_actor.isInsideRadius(target, 250, false, false))
			{
				return false;
			}
		}
		
		// Los Check Here
		return (_actor.isAutoAttackable(target) && GeoData.getInstance().canSeeTarget(_actor, target));
	}
	
	/**
	 * Set the Intention of this L2CharacterAI and create an AI Task executed every 1s (call onEvtThink method) for this L2Attackable.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in AI_INTENTION_ACTIVE</B></FONT><BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (Config.DEBUG)
		{
			_log.info("L2SiegeAI.changeIntention(" + intention + ", " + arg0 + ", " + arg1 + ")");
		}
		
		if (intention == AI_INTENTION_IDLE /* || intention == AI_INTENTION_ACTIVE */) // active becomes idle if only a summon is present
		{
			// Check if actor is not dead
			if (!_actor.isAlikeDead())
			{
				final L2Attackable npc = (L2Attackable) _actor;
				
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (npc.getKnownList().getKnownPlayers().size() > 0)
				{
					intention = AI_INTENTION_ACTIVE;
				}
				else
				{
					intention = AI_INTENTION_IDLE;
				}
			}
			
			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this L2AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE, null, null);
				
				// Stop AI task and detach AI from NPC
				if (aiTask != null)
				{
					aiTask.cancel(true);
					aiTask = null;
				}
				
				// Cancel the AI
				_accessor.detachAI();
				return;
			}
		}
		
		// Set the Intention of this L2AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);
		
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		if (aiTask == null)
		{
			aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
		}
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.<BR>
	 * <BR>
	 * @param target The L2Character to attack
	 */
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
		// if (_actor.getTarget() != null)
		super.onIntentionAttack(target);
	}
	
	/**
	 * Manage AI standard thinks of a L2Attackable (called by onEvtThink).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable L2Character in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor can't attack, order to it to return to its home location</li>
	 */
	private void thinkActive()
	{
		final L2Attackable npc = (L2Attackable) _actor;
		
		// Update every 1s the _globalAggro counter to come close to 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		// Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			for (final L2Character target : npc.getKnownList().getKnownCharactersInRadius(attackRange))
			{
				if (target == null)
				{
					continue;
				}
				if (autoAttackCondition(target)) // check aggression
				{
					// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
					final int hating = npc.getHating(target);
					
					// Add the attacker to the L2Attackable _aggroList with 0 damage and 1 hate
					if (hating == 0)
					{
						npc.addDamageHate(target, 0, 1);
					}
				}
			}
			
			// Chose a target from its aggroList
			L2Character hated;
			if (_actor.isConfused())
			{
				hated = _attack_target; // Force mobs to attack anybody if confused
			}
			else
			{
				hated = npc.getMostHated();
			}
			
			// Order to the L2Attackable to attack the target
			if (hated != null)
			{
				// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
				final int aggro = npc.getHating(hated);
				
				if ((aggro + _globalAggro) > 0)
				{
					// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
					if (!_actor.isRunning())
					{
						_actor.setRunning();
					}
					
					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated, null);
				}
				
				return;
			}
		}
		
		// Order to the L2SiegeGuardInstance to return to its home location because there's no target to attack
		((L2SiegeGuardInstance) _actor).returnHome();
	}
	
	/**
	 * Manage AI attack thinks of a L2Attackable (called by onEvtThink).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Call all L2Object of its Faction inside the Faction Range</li>
	 * <li>Choose a target and order to attack it with magic skill or physical attack</li><BR>
	 * <BR>
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 */
	private void thinkAttack()
	{
		if (Config.DEBUG)
		{
			_log.info("L2SiegeGuardAI.thinkAttack(); timeout=" + (_attackTimeout - GameTimeController.getGameTicks()));
		}
		
		if (_attackTimeout < GameTimeController.getGameTicks())
		{
			// Check if the actor is running
			if (_actor.isRunning())
			{
				// Set the actor movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance
				_actor.setWalking();
				
				// Calculate a new attack timeout
				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
			}
		}
		
		// Check if target is dead or if timeout is expired to stop this attack
		if ((_attack_target == null) || _attack_target.isAlikeDead() || (_attackTimeout < GameTimeController.getGameTicks()))
		{
			// Stop hating this target after the attack timeout or if target is dead
			if (_attack_target != null)
			{
				final L2Attackable npc = (L2Attackable) _actor;
				npc.stopHating(_attack_target);
			}
			
			// Cancel target and timeout
			_attackTimeout = Integer.MAX_VALUE;
			_attack_target = null;
			
			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE, null, null);
			
			_actor.setWalking();
			return;
		}
		
		factionNotifyAndSupport();
		attackPrepare();
	}
	
	private final void factionNotifyAndSupport()
	{
		final L2Character target = getAttackTarget();
		// Call all L2Object of its Faction inside the Faction Range
		if ((((L2NpcInstance) _actor).getFactionId() == null) || (target == null) || (_actor == null))
		{
			return;
		}
		
		if (target.isInvul())
		{
			return; // speeding it up for siege guards
		}
		
		final String faction_id = ((L2NpcInstance) _actor).getFactionId();
		
		// Go through all L2Character that belong to its faction
		for (final L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(1000))
		{
			if (cha == null)
			{
				continue;
			}
			
			if (!(cha instanceof L2NpcInstance))
			{
				if (_selfAnalysis.hasHealOrResurrect && (cha instanceof L2PcInstance) && ((L2NpcInstance) _actor).getCastle().getSiege().checkIsDefender(((L2PcInstance) cha).getClan()))
				{
					// heal friends
					if (!_actor.isAttackingDisabled() && (cha.getCurrentHp() < (cha.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && cha.isInCombat())
					{
						for (final L2Skill sk : _selfAnalysis.healSkills)
						{
							if (_actor.getCurrentMp() < sk.getMpConsume())
							{
								continue;
							}
							
							if (_actor.isSkillDisabled(sk.getId()))
							{
								continue;
							}
							
							if (!Util.checkIfInRange(sk.getCastRange(), _actor, cha, true))
							{
								continue;
							}
							
							final int chance = 5;
							if (chance >= Rnd.get(100))
							{
								continue;
							}
							
							if (!GeoData.getInstance().canSeeTarget(_actor, cha))
							{
								break;
							}
							
							final L2Object OldTarget = _actor.getTarget();
							_actor.setTarget(cha);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(OldTarget);
							return;
						}
					}
				}
				continue;
			}
			
			final L2NpcInstance npc = (L2NpcInstance) cha;
			if (faction_id != npc.getFactionId())
			{
				continue;
			}
			
			if (npc.getAI() != null) // TODO: possibly check not needed
			{
				if ((Math.abs(target.getZ() - npc.getZ()) < 600) && ((npc.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE) || (npc.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE))
				// limiting aggro for siege guards
					&& target.isInsideRadius(npc, 1500, true, false) && GeoData.getInstance().canSeeTarget(npc, target))
				{
					// Notify the L2Object AI with EVT_AGGRESSION
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
				}
				
				// heal friends
				if (_selfAnalysis.hasHealOrResurrect && !_actor.isAttackingDisabled() && (npc.getCurrentHp() < (npc.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && npc.isInCombat())
				{
					for (final L2Skill sk : _selfAnalysis.healSkills)
					{
						if (_actor.getCurrentMp() < sk.getMpConsume())
						{
							continue;
						}
						
						if (_actor.isSkillDisabled(sk.getId()))
						{
							continue;
						}
						
						if (!Util.checkIfInRange(sk.getCastRange(), _actor, npc, true))
						{
							continue;
						}
						
						final int chance = 4;
						if (chance >= Rnd.get(100))
						{
							continue;
						}
						
						if (!GeoData.getInstance().canSeeTarget(_actor, npc))
						{
							break;
						}
						
						final L2Object OldTarget = _actor.getTarget();
						_actor.setTarget(npc);
						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);
						return;
					}
				}
			}
		}
	}
	
	private void attackPrepare()
	{
		// Get all information needed to choose between physical or magical attack
		L2Skill[] skills = null;
		double dist_2 = 0;
		int range = 0;
		final L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;
		
		try
		{
			_actor.setTarget(_attack_target);
			skills = _actor.getAllSkills();
			dist_2 = _actor.getPlanDistanceSq(_attack_target.getX(), _attack_target.getY());
			range = _actor.getPhysicalAttackRange() + (int) (_actor.getTemplate().collisionRadius + _attack_target.getTemplate().collisionRadius);
			if (_attack_target.isMoving())
			{
				range += 50;
			}
		}
		catch (final NullPointerException e)
		{
			// _log.warning("AttackableAI: Attack target is NULL.");
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		// never attack defenders
		if ((_attack_target instanceof L2PcInstance) && sGuard.getCastle().getSiege().checkIsDefender(((L2PcInstance) _attack_target).getClan()))
		{
			// Cancel the target
			sGuard.stopHating(_attack_target);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		if (!GeoData.getInstance().canSeeTarget(_actor, _attack_target))
		{
			// Siege guards differ from normal mobs currently:
			// If target cannot be seen, don't attack any more
			sGuard.stopHating(_attack_target);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}
		
		// Check if the actor isn't muted and if it is far from target
		if (!_actor.isMuted() && (dist_2 > (range * range)))
		{
			// check for long ranged skills and heal/buff skills
			for (final L2Skill sk : skills)
			{
				final int castRange = sk.getCastRange();
				
				if ((dist_2 <= (castRange * castRange)) && (castRange > 70) && !_actor.isSkillDisabled(sk.getId()) && (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)) && !sk.isPassive())
				{
					final L2Object OldTarget = _actor.getTarget();
					if ((sk.getSkillType() == L2Skill.SkillType.BUFF) || (sk.getSkillType() == L2Skill.SkillType.HEAL))
					{
						boolean useSkillSelf = true;
						if ((sk.getSkillType() == L2Skill.SkillType.HEAL) && (_actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)))
						{
							useSkillSelf = false;
							break;
						}
						
						if (sk.getSkillType() == L2Skill.SkillType.BUFF)
						{
							final L2Effect[] effects = _actor.getAllEffects();
							for (int i = 0; (effects != null) && (i < effects.length); i++)
							{
								final L2Effect effect = effects[i];
								if (effect.getSkill() == sk)
								{
									useSkillSelf = false;
									break;
								}
							}
						}
						
						if (useSkillSelf)
						{
							_actor.setTarget(_actor);
						}
					}
					
					clientStopMoving(null);
					_accessor.doCast(sk);
					_actor.setTarget(OldTarget);
					return;
				}
			}
			
			// Check if the L2SiegeGuardInstance is attacking, knows the target and can't run
			if (!(_actor.isAttackingNow()) && (_actor.getRunSpeed() == 0) && (_actor.getKnownList().knowsObject(_attack_target)))
			{
				// Cancel the target
				_actor.getKnownList().removeKnownObject(_attack_target);
				_actor.setTarget(null);
				setIntention(AI_INTENTION_IDLE, null, null);
			}
			else
			{
				
				final double dx = _actor.getX() - _attack_target.getX();
				final double dy = _actor.getY() - _attack_target.getY();
				final double dz = _actor.getZ() - _attack_target.getZ();
				final double homeX = _attack_target.getX() - sGuard.getSpawn().getLocx();
				final double homeY = _attack_target.getY() - sGuard.getSpawn().getLocy();
				
				// Check if the L2SiegeGuardInstance isn't too far from it's home location
				if ((((dx * dx) + (dy * dy)) > 10000) && (((homeX * homeX) + (homeY * homeY)) > 3240000) // 1800 * 1800
					&& (_actor.getKnownList().knowsObject(_attack_target)))
				{
					// Cancel the target
					_actor.getKnownList().removeKnownObject(_attack_target);
					_actor.setTarget(null);
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				else // Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
				{
					// Temporary hack for preventing guards jumping off towers,
					// before replacing this with effective geodata checks and AI modification
					if ((dz * dz) < (170 * 170)) // normally 130 if guard z coordinates correct
					{
						if (_selfAnalysis.isMage)
						{
							range = _selfAnalysis.maxCastRange - 50;
						}
						if (_attack_target.isMoving())
						{
							moveToPawn(_attack_target, range - 70);
						}
						else
						{
							moveToPawn(_attack_target, range);
						}
					}
				}
			}
			
			return;
			
		}
		// Else, if the actor is muted and far from target, just "move to pawn"
		else if (_actor.isMuted() && (dist_2 > (range * range)))
		{
			// Temporary hack for preventing guards jumping off towers,
			// before replacing this with effective geodata checks and AI modification
			final double dz = _actor.getZ() - _attack_target.getZ();
			if ((dz * dz) < (170 * 170)) // normally 130 if guard z coordinates correct
			{
				if (_selfAnalysis.isMage)
				{
					range = _selfAnalysis.maxCastRange - 50;
				}
				if (_attack_target.isMoving())
				{
					moveToPawn(_attack_target, range - 70);
				}
				else
				{
					moveToPawn(_attack_target, range);
				}
			}
			return;
		}
		// Else, if this is close enough to attack
		else if (dist_2 <= (range * range))
		{
			// Force mobs to attack anybody if confused
			L2Character hated = null;
			if (_actor.isConfused())
			{
				hated = _attack_target;
			}
			else
			{
				hated = ((L2Attackable) _actor).getMostHated();
			}
			
			if (hated == null)
			{
				setIntention(AI_INTENTION_ACTIVE, null, null);
				return;
			}
			if (hated != _attack_target)
			{
				_attack_target = hated;
			}
			
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
			
			// check for close combat skills && heal/buff skills
			if (!_actor.isMuted() && (Rnd.nextInt(100) <= 5))
			{
				for (final L2Skill sk : skills)
				{
					final int castRange = sk.getCastRange();
					
					if (((castRange * castRange) >= dist_2) && !sk.isPassive() && (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)) && !_actor.isSkillDisabled(sk.getId()))
					{
						final L2Object OldTarget = _actor.getTarget();
						if ((sk.getSkillType() == L2Skill.SkillType.BUFF) || (sk.getSkillType() == L2Skill.SkillType.HEAL))
						{
							boolean useSkillSelf = true;
							if ((sk.getSkillType() == L2Skill.SkillType.HEAL) && (_actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)))
							{
								useSkillSelf = false;
								break;
							}
							if (sk.getSkillType() == L2Skill.SkillType.BUFF)
							{
								final L2Effect[] effects = _actor.getAllEffects();
								for (int i = 0; (effects != null) && (i < effects.length); i++)
								{
									final L2Effect effect = effects[i];
									if (effect.getSkill() == sk)
									{
										useSkillSelf = false;
										break;
									}
								}
							}
							if (useSkillSelf)
							{
								_actor.setTarget(_actor);
							}
						}
						
						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);
						return;
					}
				}
			}
			// Finally, do the physical attack itself
			_accessor.doAttack(_attack_target);
		}
	}
	
	/**
	 * Manage AI thinking actions of a L2Attackable.<BR>
	 * <BR>
	 */
	@Override
	protected void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (thinking || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		thinking = true;
		
		try
		{
			// Manage AI thinks of a L2Attackable
			if (getIntention() == AI_INTENTION_ACTIVE)
			{
				thinkActive();
			}
			else if (getIntention() == AI_INTENTION_ATTACK)
			{
				thinkAttack();
			}
		}
		finally
		{
			// Stop thinking action
			thinking = false;
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Attacked.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance</li>
	 * <li>Set the Intention to AI_INTENTION_ATTACK</li><BR>
	 * <BR>
	 * @param attacker The L2Character that attacks the actor
	 */
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor
		((L2Attackable) _actor).addDamageHate(attacker, 0, 1);
		
		// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!_actor.isRunning())
		{
			_actor.setRunning();
		}
		
		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker, null);
		}
		
		super.onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Aggression.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is L2GuardInstance check if it isn't too far from its home location)</li><BR>
	 * <BR>
	 * @param target The L2Character that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		if (_actor == null)
		{
			return;
		}
		
		final L2Attackable me = (L2Attackable) _actor;
		
		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);
			
			// Get the hate of the actor against the target
			aggro = me.getHating(target);
			
			if (aggro <= 0)
			{
				if (me.getMostHated() == null)
				{
					_globalAggro = -25;
					me.clearAggroList();
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				return;
			}
			
			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
				if (!_actor.isRunning())
				{
					_actor.setRunning();
				}
				
				final L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;
				final double homeX = target.getX() - sGuard.getSpawn().getLocx();
				final double homeY = target.getY() - sGuard.getSpawn().getLocy();
				
				// Check if the L2SiegeGuardInstance is not too far from its home location
				if (((homeX * homeX) + (homeY * homeY)) < 3240000)
				{
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, target, null);
				}
			}
		}
		else
		{
			// currently only for setting lower general aggro
			if (aggro >= 0)
			{
				return;
			}
			
			final L2Character mostHated = me.getMostHated();
			if (mostHated == null)
			{
				_globalAggro = -25;
				return;
			}
			for (final L2Character aggroed : me.getAggroList().keySet())
			{
				me.addDamageHate(aggroed, 0, aggro);
			}
			
			aggro = me.getHating(mostHated);
			if (aggro <= 0)
			{
				_globalAggro = -25;
				me.clearAggroList();
				setIntention(AI_INTENTION_IDLE, null, null);
			}
		}
	}
	
	@Override
	protected void onEvtDead()
	{
		stopAITask();
		super.onEvtDead();
	}
	
	public void stopAITask()
	{
		if (aiTask != null)
		{
			aiTask.cancel(false);
			aiTask = null;
		}
		_accessor.detachAI();
	}
}