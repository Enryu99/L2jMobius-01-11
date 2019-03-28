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
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.datatables.sql.TerritoryTable;
import com.l2jmobius.gameserver.geoengine.GeoEngine;
import com.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import com.l2jmobius.gameserver.model.Effect;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.model.actor.Attackable;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.Playable;
import com.l2jmobius.gameserver.model.actor.Summon;
import com.l2jmobius.gameserver.model.actor.instance.ChestInstance;
import com.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import com.l2jmobius.gameserver.model.actor.instance.FestivalMonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.FolkInstance;
import com.l2jmobius.gameserver.model.actor.instance.FriendlyMobInstance;
import com.l2jmobius.gameserver.model.actor.instance.GuardInstance;
import com.l2jmobius.gameserver.model.actor.instance.MinionInstance;
import com.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.actor.instance.PenaltyMonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.RaidBossInstance;
import com.l2jmobius.gameserver.model.actor.instance.RiftInvaderInstance;
import com.l2jmobius.gameserver.model.actor.position.Location;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.templates.item.Weapon;
import com.l2jmobius.gameserver.templates.item.WeaponType;

/**
 * This class manages AI of Attackable.<BR>
 * <BR>
 */
public class AttackableAI extends CreatureAI
{
	// protected static final Logger LOGGER = Logger.getLogger(AttackableAI.class);
	
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	// private static final int MAX_DRIFT_RANGE = 300;
	private static final int MAX_ATTACK_TIMEOUT = 300; // int ticks, i.e. 30 seconds
	
	/** The Attackable AI task executed every 1s (call onEvtThink method) */
	private Future<?> _aiTask;
	
	/** The delay after wich the attacked is stopped */
	private int _attackTimeout;
	
	/** The Attackable aggro counter */
	private int _globalAggro;
	
	/** The flag used to indicate that a thinking action is in progress */
	private boolean _thinking; // to prevent recursive thinking
	
	/**
	 * Constructor of AttackableAI.<BR>
	 * <BR>
	 * @param accessor The AI accessor of the Creature
	 */
	public AttackableAI(Creature.AIAccessor accessor)
	{
		super(accessor);
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
	}
	
	/**
	 * Return True if the target is autoattackable (depends on the actor type).<BR>
	 * <BR>
	 * <B><U> Actor is a GuardInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The PlayerInstance target has karma (=PK)</li>
	 * <li>The MonsterInstance target is aggressive</li><BR>
	 * <BR>
	 * <B><U> Actor is a SiegeGuardInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>A siege is in progress</li>
	 * <li>The PlayerInstance target isn't a Defender</li> <BR>
	 * <BR>
	 * <B><U> Actor is a FriendlyMobInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk, a Door or another NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The PlayerInstance target has karma (=PK)</li><BR>
	 * <BR>
	 * <B><U> Actor is a MonsterInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk, a Door or another NpcInstance</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The actor is Aggressive</li><BR>
	 * <BR>
	 * @param target The targeted WorldObject
	 * @return
	 */
	private boolean autoAttackCondition(Creature target)
	{
		if ((target == null) || !(_actor instanceof Attackable))
		{
			return false;
		}
		
		final Attackable me = (Attackable) _actor;
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			// However EffectInvincible requires to check GMs specially
			if ((target instanceof PlayerInstance) && ((PlayerInstance) target).isGM())
			{
				return false;
			}
			
			if ((target instanceof Summon) && ((Summon) target).getOwner().isGM())
			{
				return false;
			}
		}
		
		// Check if the target isn't a Folk or a Door
		if ((target instanceof FolkInstance) || (target instanceof DoorInstance))
		{
			return false;
		}
		
		// Check if the target isn't dead, is in the Aggro range and is at the same height
		if (target.isAlikeDead() || !me.isInsideRadius(target, me.getAggroRange(), false, false) || (Math.abs(_actor.getZ() - target.getZ()) > 300))
		{
			return false;
		}
		
		// Check if the target is a PlayerInstance
		if (target instanceof PlayerInstance)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
			if (((PlayerInstance) target).isGM() && ((PlayerInstance) target).getAccessLevel().canTakeAggro())
			{
				return false;
			}
			
			// Check if the AI isn't a Raid Boss and the target isn't in silent move mode
			if (!(me instanceof RaidBossInstance) && ((PlayerInstance) target).isSilentMoving())
			{
				return false;
			}
			
			// if in offline mode
			if (((PlayerInstance) target).isInOfflineMode())
			{
				return false;
			}
			
			// Check if player is an ally //TODO! [Nemesiss] it should be rather boolean or smth like that
			// Comparing String isnt good idea!
			if ((me.getFactionId() != null) && me.getFactionId().equals("varka") && ((PlayerInstance) target).isAlliedWithVarka())
			{
				return false;
			}
			
			if ((me.getFactionId() != null) && me.getFactionId().equals("ketra") && ((PlayerInstance) target).isAlliedWithKetra())
			{
				return false;
			}
			
			// check if the target is within the grace period for JUST getting up from fake death
			if (((PlayerInstance) target).isRecentFakeDeath())
			{
				return false;
			}
			
			// check player is in away mod
			if (((PlayerInstance) target).isAway() && !Config.AWAY_PLAYER_TAKE_AGGRO)
			{
				return false;
			}
			
			if (target.isInParty() && target.getParty().isInDimensionalRift())
			{
				final byte riftType = target.getParty().getDimensionalRift().getType();
				final byte riftRoom = target.getParty().getDimensionalRift().getCurrentRoom();
				
				if ((me instanceof RiftInvaderInstance) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
				{
					return false;
				}
			}
		}
		
		// Check if the target is a Summon
		if (target instanceof Summon)
		{
			final PlayerInstance owner = ((Summon) target).getOwner();
			if (owner != null)
			{
				// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
				if (owner.isGM() && (owner.isInvul() || !owner.getAccessLevel().canTakeAggro()))
				{
					return false;
				}
				// Check if player is an ally (comparing mem addr)
				if ((me.getFactionId() != null) && (me.getFactionId() == "varka") && owner.isAlliedWithVarka())
				{
					return false;
				}
				if ((me.getFactionId() != null) && (me.getFactionId() == "ketra") && owner.isAlliedWithKetra())
				{
					return false;
				}
			}
		}
		
		// Check if the actor is a GuardInstance
		if (_actor instanceof GuardInstance)
		{
			// Check if the PlayerInstance target has karma (=PK)
			if ((target instanceof PlayerInstance) && (((PlayerInstance) target).getKarma() > 0))
			{
				// Los Check
				return GeoEngine.getInstance().canSeeTarget(me, target);
			}
			
			// if (target instanceof Summon)
			// return ((Summon)target).getKarma() > 0;
			// Check if the MonsterInstance target is aggressive
			if (target instanceof MonsterInstance)
			{
				return ((MonsterInstance) target).isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target);
			}
			
			return false;
		}
		else if (_actor instanceof FriendlyMobInstance)
		{
			// the actor is a FriendlyMobInstance
			
			// Check if the target isn't another NpcInstance
			if (target instanceof NpcInstance)
			{
				return false;
			}
			
			// Check if the PlayerInstance target has karma (=PK)
			if ((target instanceof PlayerInstance) && (((PlayerInstance) target).getKarma() > 0))
			{
				// Los Check
				return GeoEngine.getInstance().canSeeTarget(me, target);
			}
			return false;
		}
		else
		{
			// The actor is a MonsterInstance
			
			// Check if the target isn't another NpcInstance
			if (target instanceof NpcInstance)
			{
				return false;
			}
			
			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (Creature.isInsidePeaceZone(me, target))
			{
				return false;
			}
			
			// Check if the actor is Aggressive
			return me.isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target);
		}
	}
	
	public synchronized void startAITask()
	{
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		if (_aiTask == null)
		{
			_aiTask = ThreadPool.scheduleAtFixedRate(this::onEvtThink, 1000, 1000);
		}
	}
	
	public synchronized void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
	}
	
	@Override
	protected void onEvtDead()
	{
		stopAITask();
		super.onEvtDead();
	}
	
	/**
	 * Set the Intention of this CreatureAI and create an AI Task executed every 1s (call onEvtThink method) for this Attackable.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in AI_INTENTION_ACTIVE</B></FONT><BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	public void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// Prevent thinking in non active regions.
		if (!_actor.isInActiveRegion())
		{
			return;
		}
		
		if ((intention == AI_INTENTION_IDLE) || (intention == AI_INTENTION_ACTIVE))
		{
			// Check if actor is not dead
			if (!_actor.isAlikeDead())
			{
				Attackable npc = (Attackable) _actor;
				
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (npc.getKnownList().getKnownPlayers().size() > 0)
				{
					intention = AI_INTENTION_ACTIVE;
				}
			}
			
			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE, null, null);
				
				// Stop AI task and detach AI from NPC
				stopAITask();
				
				// Cancel the AI
				_accessor.detachAI();
				
				return;
			}
		}
		
		// Set the Intention of this AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);
		
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		startAITask();
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.<BR>
	 * <BR>
	 * @param target The Creature to attack
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
		super.onIntentionAttack(target);
	}
	
	/**
	 * Manage AI standard thinks of a Attackable (called by onEvtThink).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable Creature in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor is a GuardInstance that can't attack, order to it to return to its home location</li>
	 * <li>If the actor is a MonsterInstance that can't attack, order to it to random walk (1/100)</li><BR>
	 * <BR>
	 */
	private void thinkActive()
	{
		Attackable npc = (Attackable) _actor;
		
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
		
		// Add all autoAttackable Creature in Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			// Get all visible objects inside its Aggro Range
			// WorldObject[] objects = World.getInstance().getVisibleObjects(_actor, ((NpcInstance)_actor).getAggroRange());
			// Go through visible objects
			for (WorldObject obj : npc.getKnownList().getKnownObjects().values())
			{
				if ((obj == null) || !(obj instanceof Creature))
				{
					continue;
				}
				
				Creature target = (Creature) obj;
				
				/*
				 * Check to see if this is a festival mob spawn. If it is, then check to see if the aggro trigger is a festival participant...if so, move to attack it.
				 */
				if ((_actor instanceof FestivalMonsterInstance) && (obj instanceof PlayerInstance))
				{
					PlayerInstance targetPlayer = (PlayerInstance) obj;
					if (!targetPlayer.isFestivalParticipant())
					{
						continue;
					}
				}
				
				if ((obj instanceof PlayerInstance) || (obj instanceof Summon))
				{
					if (!target.isAlikeDead() && !npc.isInsideRadius(obj, npc.getAggroRange(), true, false))
					{
						final PlayerInstance targetPlayer = obj instanceof PlayerInstance ? (PlayerInstance) obj : ((Summon) obj).getOwner();
						
						for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER))
						{
							quest.notifyAggroRangeEnter(npc, targetPlayer, obj instanceof Summon);
						}
					}
				}
				
				// For each Creature check if the target is autoattackable
				if (autoAttackCondition(target)) // check aggression
				{
					// Get the hate level of the Attackable against this Creature target contained in _aggroList
					final int hating = npc.getHating(target);
					
					// Add the attacker to the Attackable _aggroList with 0 damage and 1 hate
					if (hating == 0)
					{
						npc.addDamageHate(target, 0, 1);
					}
				}
			}
			
			// Chose a target from its aggroList
			Creature hated;
			
			// Force mobs to attak anybody if confused
			if (_actor.isConfused())
			{
				hated = getAttackTarget();
			}
			else
			{
				hated = npc.getMostHated();
			}
			
			// Order to the Attackable to attack the target
			if (hated != null)
			{
				// Get the hate level of the Attackable against this Creature target contained in _aggroList
				final int aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
					if (!_actor.isRunning())
					{
						_actor.setRunning();
					}
					
					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(AI_INTENTION_ATTACK, hated);
				}
				
				return;
			}
		}
		
		// Check if the actor is a GuardInstance
		if (_actor instanceof GuardInstance)
		{
			// Order to the GuardInstance to return to its home location because there's no target to attack
			((GuardInstance) _actor).returnHome();
		}
		
		// If this is a festival monster, then it remains in the same location.
		if (_actor instanceof FestivalMonsterInstance)
		{
			return;
		}
		
		// Check if the mob should not return to spawn point
		if (!npc.canReturnToSpawnPoint())
		{
			return;
		}
		
		// Minions following leader
		if ((_actor instanceof MinionInstance) && (((MinionInstance) _actor).getLeader() != null))
		{
			int offset;
			
			// for Raids - need correction
			if (_actor.isRaid())
			{
				offset = 500;
			}
			else
			{
				// for normal minions - need correction :)
				offset = 200;
			}
			
			if (((MinionInstance) _actor).getLeader().isRunning())
			{
				_actor.setRunning();
			}
			else
			{
				_actor.setWalking();
			}
			
			if (_actor.getPlanDistanceSq(((MinionInstance) _actor).getLeader()) > (offset * offset))
			{
				int x1;
				int y1;
				int z1;
				
				x1 = (((MinionInstance) _actor).getLeader().getX() + Rnd.get((offset - 30) * 2)) - (offset - 30);
				y1 = (((MinionInstance) _actor).getLeader().getY() + Rnd.get((offset - 30) * 2)) - (offset - 30);
				z1 = ((MinionInstance) _actor).getLeader().getZ();
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
				moveTo(x1, y1, z1);
				return;
			}
		}
		// Order to the MonsterInstance to random walk (1/100)
		else if (!(npc instanceof ChestInstance) && (npc.getSpawn() != null) && (Rnd.get(RANDOM_WALK_RATE) == 0))
		{
			int x1;
			int y1;
			int z1;
			
			// If NPC with random coord in territory
			if ((npc.getSpawn().getX() == 0) && (npc.getSpawn().getY() == 0))
			{
				// If NPC with random fixed coord, don't move
				if (TerritoryTable.getInstance().getProcMax(npc.getSpawn().getLocation()) > 0)
				{
					return;
				}
				
				// Calculate a destination point in the spawn area
				final int p[] = TerritoryTable.getInstance().getRandomPoint(npc.getSpawn().getLocation());
				x1 = p[0];
				y1 = p[1];
				z1 = p[2];
				
				// Calculate the distance between the current position of the Creature and the target (x,y)
				final double distance2 = _actor.getPlanDistanceSq(x1, y1);
				
				if (distance2 > (Config.MAX_DRIFT_RANGE * Config.MAX_DRIFT_RANGE))
				{
					npc.setisReturningToSpawnPoint(true);
					final float delay = (float) Math.sqrt(distance2) / Config.MAX_DRIFT_RANGE;
					x1 = _actor.getX() + (int) ((x1 - _actor.getX()) / delay);
					y1 = _actor.getY() + (int) ((y1 - _actor.getY()) / delay);
				}
				else
				{
					npc.setisReturningToSpawnPoint(false);
				}
			}
			else
			{
				if ((Config.MONSTER_RETURN_DELAY > 0) && (npc instanceof MonsterInstance) && !npc.isAlikeDead() && !npc.isDead() && (npc.getSpawn() != null) && !npc.isInsideRadius(npc.getSpawn().getX(), npc.getSpawn().getY(), Config.MAX_DRIFT_RANGE, false))
				{
					((MonsterInstance) _actor).returnHome();
				}
				
				// If NPC with fixed coord
				x1 = (npc.getSpawn().getX() + Rnd.get(Config.MAX_DRIFT_RANGE * 2)) - Config.MAX_DRIFT_RANGE;
				y1 = (npc.getSpawn().getY() + Rnd.get(Config.MAX_DRIFT_RANGE * 2)) - Config.MAX_DRIFT_RANGE;
				z1 = npc.getZ();
			}
			
			// LOGGER.config("Curent pos ("+getX()+", "+getY()+"), moving to ("+x1+", "+y1+").");
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
			moveTo(x1, y1, z1);
		}
		
		return;
	}
	
	/**
	 * Manage AI attack thinks of a Attackable (called by onEvtThink).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Call all WorldObject of its Faction inside the Faction Range</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li><BR>
	 * <BR>
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 */
	private void thinkAttack()
	{
		if ((_actor == null) || _actor.isCastingNow())
		{
			return;
		}
		if (_attackTimeout < GameTimeController.getGameTicks())
		{
			// Check if the actor is running
			if (_actor.isRunning())
			{
				// Set the actor movement type to walk and send Server->Client packet ChangeMoveType to all others PlayerInstance
				_actor.setWalking();
				
				// Calculate a new attack timeout
				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
			}
		}
		
		final Creature originalAttackTarget = getAttackTarget();
		// Check if target is dead or if timeout is expired to stop this attack
		if ((originalAttackTarget == null) || originalAttackTarget.isAlikeDead() || ((originalAttackTarget instanceof PlayerInstance) && (((PlayerInstance) originalAttackTarget).isInOfflineMode() || (((PlayerInstance) originalAttackTarget).isOnline() == 0))) || (_attackTimeout < GameTimeController.getGameTicks()))
		{
			// Stop hating this target after the attack timeout or if target is dead
			if (originalAttackTarget != null)
			{
				((Attackable) _actor).stopHating(originalAttackTarget);
			}
			
			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
			
			_actor.setWalking();
			return;
		}
		
		/*
		 * // Handle all WorldObject of its Faction inside the Faction Range if (_actor instanceof NpcInstance && ((NpcInstance) _actor).getFactionId() != null) { String faction_id = ((NpcInstance) _actor).getFactionId(); // Go through all WorldObject that belong to its faction Collection<WorldObject>
		 * objs = _actor.getKnownList().getKnownObjects().values(); //synchronized (_actor.getKnownList().getKnownObjects()) try { for (WorldObject obj : objs) { if (obj instanceof NpcInstance) { NpcInstance npc = (NpcInstance) obj; //Handle SevenSigns mob Factions String npcfaction =
		 * npc.getFactionId(); boolean sevenSignFaction = false; // TODO: Unhardcode this by AI scripts (DrHouse) //Catacomb mobs should assist lilim and nephilim other than dungeon if ("c_dungeon_clan".equals(faction_id) && ("c_dungeon_lilim".equals(npcfaction) ||
		 * "c_dungeon_nephi".equals(npcfaction))) sevenSignFaction = true; //Lilim mobs should assist other Lilim and catacomb mobs else if ("c_dungeon_lilim".equals(faction_id) && "c_dungeon_clan".equals(npcfaction)) sevenSignFaction = true; //Nephilim mobs should assist other Nephilim and catacomb
		 * mobs else if ("c_dungeon_nephi".equals(faction_id) && "c_dungeon_clan".equals(npcfaction)) sevenSignFaction = true; if (!faction_id.equals(npc.getFactionId()) && !sevenSignFaction) continue; // Check if the WorldObject is inside the Faction Range of // the actor if
		 * (_actor.isInsideRadius(npc, npc.getFactionRange() + npc.getTemplate().collisionRadius, true, false) && npc.getAI() != null) { if (Math.abs(originalAttackTarget.getZ() - npc.getZ()) < 600 && _actor.getAttackByList().contains(originalAttackTarget) && (npc.getAI()._intention ==
		 * CtrlIntention.AI_INTENTION_IDLE || npc.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE) && GeoEngine.getInstance().canSeeTarget(_actor, npc)) { if ((originalAttackTarget instanceof PlayerInstance) || (originalAttackTarget instanceof Summon)) { if
		 * (npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL) != null) { PlayerInstance player = (originalAttackTarget instanceof PlayerInstance) ? (PlayerInstance) originalAttackTarget : ((Summon) originalAttackTarget).getOwner(); for (Quest quest :
		 * npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL)) quest.notifyFactionCall(npc, (NpcInstance) _actor, player, (originalAttackTarget instanceof Summon)); } } else if (npc instanceof Attackable && getAttackTarget() != null && npc.getAI()._intention !=
		 * CtrlIntention.AI_INTENTION_ATTACK) { ((Attackable) npc).addDamageHate(getAttackTarget(), 0, ((Attackable) _actor).getHating(getAttackTarget())); npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getAttackTarget()); } } } } } } catch (NullPointerException e) { LOGGER.warning(
		 * "AttackableAI: thinkAttack() faction call failed: " + e.getMessage()); } }
		 */
		
		// Call all WorldObject of its Faction inside the Faction Range
		if (((NpcInstance) _actor).getFactionId() != null)
		{
			// Go through all WorldObject that belong to its faction
			for (WorldObject obj : _actor.getKnownList().getKnownObjects().values())
			{
				if (obj instanceof NpcInstance)
				{
					NpcInstance npc = (NpcInstance) obj;
					String faction_id = ((NpcInstance) _actor).getFactionId();
					
					if (!faction_id.equalsIgnoreCase(npc.getFactionId()) || (npc.getFactionRange() == 0))
					{
						continue;
					}
					
					// Check if the WorldObject is inside the Faction Range of the actor
					if ((_actor.getAttackByList() != null) && _actor.isInsideRadius(npc, npc.getFactionRange(), true, false) && (npc.getAI() != null) && _actor.getAttackByList().contains(originalAttackTarget))
					{
						if ((npc.getAI().getIntention() == AI_INTENTION_IDLE) || (npc.getAI().getIntention() == AI_INTENTION_ACTIVE))
						{
							if (GeoEngine.getInstance().canSeeTarget(_actor, npc) && (Math.abs(originalAttackTarget.getZ() - npc.getZ()) < 600))
							{
								if ((originalAttackTarget instanceof PlayerInstance) && originalAttackTarget.isInParty() && originalAttackTarget.getParty().isInDimensionalRift())
								{
									final byte riftType = originalAttackTarget.getParty().getDimensionalRift().getType();
									final byte riftRoom = originalAttackTarget.getParty().getDimensionalRift().getCurrentRoom();
									
									if ((_actor instanceof RiftInvaderInstance) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
									{
										continue;
									}
								}
								// Notify the WorldObject AI with EVT_AGGRESSION
								npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, originalAttackTarget, 1);
							}
						}
						
						if (GeoEngine.getInstance().canSeeTarget(_actor, npc) && (Math.abs(originalAttackTarget.getZ() - npc.getZ()) < 500))
						{
							if ((originalAttackTarget instanceof PlayerInstance) || (originalAttackTarget instanceof Summon))
							{
								final PlayerInstance player = originalAttackTarget instanceof PlayerInstance ? (PlayerInstance) originalAttackTarget : ((Summon) originalAttackTarget).getOwner();
								for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL))
								{
									quest.notifyFactionCall(npc, (NpcInstance) _actor, player, (originalAttackTarget instanceof Summon));
								}
							}
						}
					}
				}
			}
		}
		
		if (_actor.isAttackingDisabled())
		{
			return;
		}
		
		// Get all information needed to chose between physical or magical attack
		Skill[] skills = null;
		double dist2 = 0;
		int range = 0;
		
		try
		{
			_actor.setTarget(originalAttackTarget);
			skills = _actor.getAllSkills();
			dist2 = _actor.getPlanDistanceSq(originalAttackTarget.getX(), originalAttackTarget.getY());
			range = _actor.getPhysicalAttackRange() + _actor.getTemplate().collisionRadius + originalAttackTarget.getTemplate().collisionRadius;
		}
		catch (NullPointerException e)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		Weapon weapon = _actor.getActiveWeaponItem();
		final int collision = _actor.getTemplate().collisionRadius;
		final int combinedCollision = collision + originalAttackTarget.getTemplate().collisionRadius;
		
		// ------------------------------------------------------
		// In case many mobs are trying to hit from same place, move a bit,
		// circling around the target
		// Note from Gnacik:
		// On l2js because of that sometimes mobs don't attack player only running
		// around player without any sense, so decrease chance for now
		if (!_actor.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			for (WorldObject nearby : _actor.getKnownList().getKnownObjects().values())
			{
				if ((nearby instanceof Attackable) && _actor.isInsideRadius(nearby, collision, false, false) && (nearby != originalAttackTarget))
				{
					int newX = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
					{
						newX = originalAttackTarget.getX() + newX;
					}
					else
					{
						newX = originalAttackTarget.getX() - newX;
					}
					int newY = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
					{
						newY = originalAttackTarget.getY() + newY;
					}
					else
					{
						newY = originalAttackTarget.getY() - newY;
					}
					
					if (!_actor.isInsideRadius(newX, newY, collision, false))
					{
						final int newZ = _actor.getZ() + 30;
						if (!Config.PATHFINDING || GeoEngine.getInstance().canMoveToTarget(_actor.getX(), _actor.getY(), _actor.getZ(), newX, newY, newZ, _actor.getInstanceId()))
						{
							moveTo(newX, newY, newZ);
						}
					}
					return;
				}
			}
		}
		
		if ((weapon != null) && (weapon.getItemType() == WeaponType.BOW))
		{
			// Micht: kepping this one otherwise we should do 2 sqrt
			final double distance2 = _actor.getPlanDistanceSq(originalAttackTarget.getX(), originalAttackTarget.getY());
			if (Math.sqrt(distance2) <= (60 + combinedCollision))
			{
				final int chance = 5;
				if (chance >= Rnd.get(100))
				{
					int posX = _actor.getX();
					int posY = _actor.getY();
					final int posZ = _actor.getZ();
					final double distance = Math.sqrt(distance2); // This way, we only do the sqrt if we need it
					
					int signx = -1;
					int signy = -1;
					if (_actor.getX() > originalAttackTarget.getX())
					{
						signx = 1;
					}
					if (_actor.getY() > originalAttackTarget.getY())
					{
						signy = 1;
					}
					
					posX += Math.round((float) ((signx * ((range / 2) + Rnd.get(range))) - distance));
					posY += Math.round((float) ((signy * ((range / 2) + Rnd.get(range))) - distance));
					setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ, 0));
					return;
				}
			}
		}
		
		// Force mobs to attack anybody if confused
		Creature hated;
		if (_actor.isConfused())
		{
			hated = originalAttackTarget;
		}
		else
		{
			hated = ((Attackable) _actor).getMostHated();
		}
		
		if (hated == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		if (hated != originalAttackTarget)
		{
			setAttackTarget(hated);
		}
		// We should calculate new distance cuz mob can have changed the target
		dist2 = _actor.getPlanDistanceSq(hated.getX(), hated.getY());
		
		if (hated.isMoving())
		{
			range += 50;
		}
		
		// Check if the actor isn't far from target
		if (dist2 > (range * range))
		{
			// check for long ranged skills and heal/buff skills
			if (!_actor.isMuted() && (!Config.ALT_GAME_MOB_ATTACK_AI || ((_actor instanceof MonsterInstance) && (Rnd.get(100) <= 5))))
			{
				for (Skill sk : skills)
				{
					final int castRange = sk.getCastRange();
					
					boolean _inRange = false;
					if ((dist2 >= ((castRange * castRange) / 9.0)) && (dist2 <= (castRange * castRange)) && (castRange > 70))
					{
						_inRange = true;
					}
					
					if (((sk.getSkillType() == Skill.SkillType.BUFF) || (sk.getSkillType() == Skill.SkillType.HEAL) || _inRange) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)) && !sk.isPassive() && (Rnd.get(100) <= 5))
					{
						if ((sk.getSkillType() == Skill.SkillType.BUFF) || (sk.getSkillType() == Skill.SkillType.HEAL))
						{
							boolean useSkillSelf = true;
							
							if ((sk.getSkillType() == Skill.SkillType.HEAL) && (_actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)))
							{
								useSkillSelf = false;
								break;
							}
							
							if (sk.getSkillType() == Skill.SkillType.BUFF)
							{
								Effect[] effects = _actor.getAllEffects();
								
								for (int i = 0; (effects != null) && (i < effects.length); i++)
								{
									final Effect effect = effects[i];
									
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
						
						WorldObject OldTarget = _actor.getTarget();
						
						clientStopMoving(null);
						
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);
						return;
					}
				}
			}
			
			// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
			if (hated.isMoving())
			{
				range -= 100;
			}
			if (range < 5)
			{
				range = 5;
			}
			
			moveToPawn(originalAttackTarget, range);
			
			return;
		}
		// Else, if this is close enough to attack
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		
		// check for close combat skills && heal/buff skills
		if (!_actor.isMuted() /* && _rnd.nextInt(100) <= 5 */)
		{
			boolean useSkillSelf = true;
			
			for (Skill sk : skills)
			{
				if (/* sk.getCastRange() >= dist && sk.getCastRange() <= 70 && */!sk.isPassive() && (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)) && !_actor.isSkillDisabled(sk) && ((Rnd.get(100) <= 8) || ((_actor instanceof PenaltyMonsterInstance) && (Rnd.get(100) <= 20))))
				{
					if ((sk.getSkillType() == Skill.SkillType.BUFF) || (sk.getSkillType() == Skill.SkillType.HEAL))
					{
						useSkillSelf = true;
						
						if ((sk.getSkillType() == Skill.SkillType.HEAL) && (_actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)))
						{
							useSkillSelf = false;
							break;
						}
						
						if (sk.getSkillType() == Skill.SkillType.BUFF)
						{
							Effect[] effects = _actor.getAllEffects();
							
							for (int i = 0; (effects != null) && (i < effects.length); i++)
							{
								final Effect effect = effects[i];
								
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
					// GeoData Los Check here
					if (!useSkillSelf && !GeoEngine.getInstance().canSeeTarget(_actor, _actor.getTarget()))
					{
						return;
					}
					
					WorldObject OldTarget = _actor.getTarget();
					
					clientStopMoving(null);
					_accessor.doCast(sk);
					_actor.setTarget(OldTarget);
					return;
				}
			}
		}
		
		// Finally, physical attacks
		clientStopMoving(null);
		_accessor.doAttack(hated);
	}
	
	/**
	 * Manage AI thinking actions of a Attackable.<BR>
	 * <BR>
	 */
	@Override
	protected void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a Attackable
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
			_thinking = false;
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Attacked.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance</li>
	 * <li>Set the Intention to AI_INTENTION_ATTACK</li> <BR>
	 * <BR>
	 * @param attacker The Creature that attacks the actor
	 */
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor
		((Attackable) _actor).addDamageHate(attacker, 0, 1);
		
		// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others PlayerInstance
		if (!_actor.isRunning())
		{
			_actor.setRunning();
		}
		
		if (((!(_actor instanceof NpcInstance) || (_actor instanceof Attackable)) || (_actor instanceof Playable)))
		{
			// Set the Intention to AI_INTENTION_ATTACK
			if (getIntention() != AI_INTENTION_ATTACK)
			{
				setIntention(AI_INTENTION_ATTACK, attacker);
			}
			else if (((Attackable) _actor).getMostHated() != getAttackTarget())
			{
				setIntention(AI_INTENTION_ATTACK, attacker);
			}
		}
		
		super.onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Aggression.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is GuardInstance check if it isn't too far from its home location)</li><BR>
	 * <BR>
	 * @param target the Creature that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		Attackable me = (Attackable) _actor;
		
		// To avoid lag issue
		if (me.isDead())
		{
			return;
		}
		
		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already
			// present
			me.addDamageHate(target, 0, aggro);
			
			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != AI_INTENTION_ATTACK)
			{
				// Set the Creature movement type to run and send
				// Server->Client packet ChangeMoveType to all others
				// PlayerInstance
				if (!_actor.isRunning())
				{
					_actor.setRunning();
				}
				
				setIntention(AI_INTENTION_ATTACK, target);
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
}
