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
package com.l2jmobius.gameserver.model.actor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.l2jmobius.Config;
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.ai.CtrlEvent;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.ai.L2AttackableAI;
import com.l2jmobius.gameserver.ai.L2CharacterAI;
import com.l2jmobius.gameserver.ai.L2FortSiegeGuardAI;
import com.l2jmobius.gameserver.ai.L2SiegeGuardAI;
import com.l2jmobius.gameserver.data.xml.impl.ExtendDropData;
import com.l2jmobius.gameserver.datatables.EventDroplist;
import com.l2jmobius.gameserver.datatables.EventDroplist.DateDrop;
import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.enums.ChatType;
import com.l2jmobius.gameserver.enums.InstanceType;
import com.l2jmobius.gameserver.enums.Team;
import com.l2jmobius.gameserver.instancemanager.CursedWeaponsManager;
import com.l2jmobius.gameserver.instancemanager.PcCafePointsManager;
import com.l2jmobius.gameserver.instancemanager.WalkingManager;
import com.l2jmobius.gameserver.model.AggroInfo;
import com.l2jmobius.gameserver.model.DamageDoneInfo;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2CommandChannel;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Party;
import com.l2jmobius.gameserver.model.L2Seed;
import com.l2jmobius.gameserver.model.actor.instance.L2GrandBossInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2ServitorInstance;
import com.l2jmobius.gameserver.model.actor.status.AttackableStatus;
import com.l2jmobius.gameserver.model.actor.tasks.attackable.CommandChannelTimer;
import com.l2jmobius.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jmobius.gameserver.model.drops.DropListScope;
import com.l2jmobius.gameserver.model.entity.Hero;
import com.l2jmobius.gameserver.model.events.EventDispatcher;
import com.l2jmobius.gameserver.model.events.impl.character.npc.OnAttackableAggroRangeEnter;
import com.l2jmobius.gameserver.model.events.impl.character.npc.OnAttackableAttack;
import com.l2jmobius.gameserver.model.events.impl.character.npc.OnAttackableKill;
import com.l2jmobius.gameserver.model.holders.ItemHolder;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.skills.SkillCaster;
import com.l2jmobius.gameserver.model.stats.Stats;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.taskmanager.DecayTaskManager;
import com.l2jmobius.gameserver.util.Util;

public class L2Attackable extends L2Npc
{
	// Raid
	private boolean _isRaid = false;
	private boolean _isRaidMinion = false;
	//
	private boolean _champion = false;
	private volatile Map<L2Character, AggroInfo> _aggroList = null;
	private boolean _isReturningToSpawnPoint = false;
	private boolean _canReturnToSpawnPoint = true;
	private boolean _seeThroughSilentMove = false;
	private boolean _canStopAttackByTime = true;
	// Manor
	private boolean _seeded = false;
	private L2Seed _seed = null;
	private int _seederObjId = 0;
	private final AtomicReference<ItemHolder> _harvestItem = new AtomicReference<>();
	// Spoil
	private int _spoilerObjectId;
	private final AtomicReference<Collection<ItemHolder>> _sweepItems = new AtomicReference<>();
	// Over-hit
	private boolean _overhit;
	private double _overhitDamage;
	private L2Character _overhitAttacker;
	// Command channel
	private volatile L2CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;
	private long _commandChannelLastAttack = 0;
	// Misc
	private boolean _mustGiveExpSp;
	
	/**
	 * Constructor of L2Attackable (use L2Character and L2NpcInstance constructor).<br>
	 * Actions:<br>
	 * Call the L2Character constructor to set the _template of the L2Attackable (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)<br>
	 * Set the name of the L2Attackable<br>
	 * Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it.
	 * @param template the template to apply to the NPC.
	 */
	public L2Attackable(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2Attackable);
		setIsInvul(false);
		_mustGiveExpSp = true;
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2AttackableAI(this);
	}
	
	public final Map<L2Character, AggroInfo> getAggroList()
	{
		return _aggroList != null ? _aggroList : Collections.emptyMap();
	}
	
	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}
	
	public final void setisReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}
	
	public final boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}
	
	public final void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}
	
	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}
	
	public void setSeeThroughSilentMove(boolean val)
	{
		_seeThroughSilentMove = val;
	}
	
	public boolean canStopAttackByTime()
	{
		return _canStopAttackByTime;
	}
	
	public void setCanStopAttackByTime(boolean val)
	{
		_canStopAttackByTime = val;
	}
	
	/**
	 * Use the skill if minimum checks are pass.
	 * @param skill the skill
	 */
	public void useMagic(Skill skill)
	{
		if (!SkillCaster.checkUseConditions(this, skill))
		{
			return;
		}
		
		final L2Object target = skill.getTarget(this, false, false, false);
		if (target != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
		}
	}
	
	/**
	 * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.
	 * @param attacker The L2Character who attacks
	 * @param isDOT
	 * @param skill
	 */
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		if (isRaid() && !isMinion() && (attacker != null) && (attacker.getParty() != null) && attacker.getParty().isInCommandChannel() && attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
		{
			if (_firstCommandChannelAttacked == null) // looting right isn't set
			{
				synchronized (this)
				{
					if (_firstCommandChannelAttacked == null)
					{
						_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
						if (_firstCommandChannelAttacked != null)
						{
							_commandChannelTimer = new CommandChannelTimer(this);
							_commandChannelLastAttack = System.currentTimeMillis();
							ThreadPoolManager.getInstance().scheduleGeneral(_commandChannelTimer, 10000); // check for last attack
							_firstCommandChannelAttacked.broadcastPacket(new CreatureSay(0, ChatType.PARTYROOM_ALL, "", "You have looting rights!")); // TODO: retail msg
						}
					}
				}
			}
			else if (attacker.getParty().getCommandChannel().equals(_firstCommandChannelAttacked)) // is in same channel
			{
				_commandChannelLastAttack = System.currentTimeMillis(); // update last attack time
			}
		}
		
		// Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList
		if (attacker != null)
		{
			addDamage(attacker, (int) value, skill);
		}
		
		// If this L2Attackable is a L2MonsterInstance and it has spawned minions, call its minions to battle
		if (isMonster())
		{
			L2MonsterInstance master = (L2MonsterInstance) this;
			
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
		}
		// Reduce the current HP of the L2Attackable and launch the doDie Task if necessary
		super.reduceCurrentHp(value, attacker, skill, isDOT, directlyToHp, critical, reflect);
	}
	
	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}
	
	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp;
	}
	
	/**
	 * Kill the L2Attackable (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.<br>
	 * Actions:<br>
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members<br>
	 * Notify the Quest Engine of the L2Attackable death if necessary.<br>
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds)<br>
	 * Caution: This method DOESN'T GIVE rewards to L2PetInstance.
	 * @param killer The L2Character that has killed the L2Attackable
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2NpcInstance (the corpse disappeared after 7 seconds)
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if ((killer != null) && killer.isPlayable())
		{
			// Delayed notification
			EventDispatcher.getInstance().notifyEventAsync(new OnAttackableKill(killer.getActingPlayer(), this, killer.isSummon()), this);
		}
		
		// Notify to minions if there are.
		if (isMonster())
		{
			final L2MonsterInstance mob = (L2MonsterInstance) this;
			if ((mob.getLeader() != null) && mob.getLeader().hasMinions())
			{
				final int respawnTime = Config.MINIONS_RESPAWN_TIME.containsKey(getId()) ? Config.MINIONS_RESPAWN_TIME.get(getId()) * 1000 : -1;
				mob.getLeader().getMinionList().onMinionDie(mob, respawnTime);
			}
			
			if (mob.hasMinions())
			{
				mob.getMinionList().onMasterDie(false);
			}
		}
		
		return true;
	}
	
	/**
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members.<br>
	 * Actions:<br>
	 * Get the L2PcInstance owner of the L2ServitorInstance (if necessary) and L2Party in progress.<br>
	 * Calculate the Experience and SP rewards in function of the level difference.<br>
	 * Add Exp and SP rewards to L2PcInstance (including Summon penalty) and to Party members in the known area of the last attacker.<br>
	 * Caution : This method DOESN'T GIVE rewards to L2PetInstance.
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	@Override
	protected void calculateRewards(L2Character lastAttacker)
	{
		try
		{
			if (getAggroList().isEmpty())
			{
				return;
			}
			
			// NOTE: Concurrent-safe map is used because while iterating to verify all conditions sometimes an entry must be removed.
			final Map<L2PcInstance, DamageDoneInfo> rewards = new ConcurrentHashMap<>();
			
			L2PcInstance maxDealer = null;
			int maxDamage = 0;
			long totalDamage = 0;
			// While Iterating over This Map Removing Object is Not Allowed
			// Go through the _aggroList of the L2Attackable
			for (AggroInfo info : getAggroList().values())
			{
				// Get the L2Character corresponding to this attacker
				final L2PcInstance attacker = info.getAttacker().getActingPlayer();
				if (attacker != null)
				{
					// Get damages done by this attacker
					final int damage = info.getDamage();
					
					// Prevent unwanted behavior
					if (damage > 1)
					{
						// Check if damage dealer isn't too far from this (killed monster)
						if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, attacker, true))
						{
							continue;
						}
						
						totalDamage += damage;
						
						// Calculate real damages (Summoners should get own damage plus summon's damage)
						final DamageDoneInfo reward = rewards.computeIfAbsent(attacker, DamageDoneInfo::new);
						reward.addDamage(damage);
						
						if (reward.getDamage() > maxDamage)
						{
							maxDealer = attacker;
							maxDamage = reward.getDamage();
						}
					}
				}
			}
			
			// Calculate raidboss points
			if (isRaid() && !isRaidMinion())
			{
				final L2PcInstance player = (maxDealer != null) && maxDealer.isOnline() ? maxDealer : lastAttacker.getActingPlayer();
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL));
				final int raidbossPoints = (int) (getTemplate().getRaidPoints() * Config.RATE_RAIDBOSS_POINTS);
				final L2Party party = player.getParty();
				
				if (party != null)
				{
					final L2CommandChannel command = party.getCommandChannel();
					//@formatter:off
					final List<L2PcInstance> members = command != null ? 
						command.getMembers().stream().filter(p -> p.calculateDistance(this, true, false) < Config.ALT_PARTY_RANGE).collect(Collectors.toList()) :
						player.getParty().getMembers().stream().filter(p -> p.calculateDistance(this, true, false) < Config.ALT_PARTY_RANGE).collect(Collectors.toList());
					//@formatter:on
					
					members.forEach(p ->
					{
						final int points = Math.max(raidbossPoints / members.size(), 1);
						p.increaseRaidbossPoints(points);
						p.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_RAID_POINT_S).addInt(points));
						
						if (p.isNoble())
						{
							Hero.getInstance().setRBkilled(p.getObjectId(), getId());
						}
					});
				}
				else
				{
					final int points = Math.max(raidbossPoints, 1);
					player.increaseRaidbossPoints(points);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_RAID_POINT_S).addInt(points));
					if (player.isNoble())
					{
						Hero.getInstance().setRBkilled(player.getObjectId(), getId());
					}
				}
			}
			
			// Manage Base, Quests and Sweep drops of the L2Attackable
			doItemDrop((maxDealer != null) && maxDealer.isOnline() ? maxDealer : lastAttacker);
			
			// Manage drop of Special Events created by GM for a defined period
			doEventDrop(lastAttacker);
			
			if (!getMustRewardExpSP())
			{
				return;
			}
			
			if (!rewards.isEmpty())
			{
				for (DamageDoneInfo reward : rewards.values())
				{
					if (reward == null)
					{
						continue;
					}
					
					// Attacker to be rewarded
					final L2PcInstance attacker = reward.getAttacker();
					
					// Total amount of damage done
					final int damage = reward.getDamage();
					
					// Get party
					final L2Party attackerParty = attacker.getParty();
					
					// Penalty applied to the attacker's XP
					// If this attacker have servitor, get Exp Penalty applied for the servitor.
					float penalty = 1;
					
					final Optional<L2Summon> summon = attacker.getServitors().values().stream().filter(s -> ((L2ServitorInstance) s).getExpMultiplier() > 1).findFirst();
					if (summon.isPresent())
					{
						penalty = ((L2ServitorInstance) summon.get()).getExpMultiplier();
						
					}
					
					// If there's NO party in progress
					if (attackerParty == null)
					{
						// Calculate Exp and SP rewards
						if (isInSurroundingRegion(attacker))
						{
							final int[] expSp = calculateExpAndSp(attacker.getLevel(), damage, totalDamage);
							long exp = expSp[0];
							int sp = expSp[1];
							
							if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
							{
								exp *= Config.L2JMOD_CHAMPION_REWARDS_EXP_SP;
								sp *= Config.L2JMOD_CHAMPION_REWARDS_EXP_SP;
							}
							
							exp *= penalty;
							
							// Check for an over-hit enabled strike
							final L2Character overhitAttacker = getOverhitAttacker();
							if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
							{
								attacker.sendPacket(SystemMessageId.OVER_HIT);
								exp += calculateOverhitExp(exp);
							}
							
							// Distribute the Exp and SP between the L2PcInstance and its L2Summon
							if (!attacker.isDead())
							{
								final long addexp = Math.round(attacker.getStat().getValue(Stats.EXPSP_RATE, exp));
								final int addsp = (int) attacker.getStat().getValue(Stats.EXPSP_RATE, sp);
								
								attacker.addExpAndSp(addexp, addsp, useVitalityRate());
								if (addexp > 0)
								{
									final L2Clan clan = attacker.getClan();
									if (clan != null)
									{
										long finalExp = addexp;
										if (useVitalityRate())
										{
											finalExp *= attacker.getStat().getExpBonusMultiplier();
										}
										clan.addHuntingPoints(attacker, this, finalExp);
									}
									attacker.updateVitalityPoints(getVitalityPoints(attacker.getLevel(), addexp, isRaid()), true, false);
									PcCafePointsManager.getInstance().givePcCafePoint(attacker, addexp);
								}
							}
						}
					}
					else
					{
						// share with party members
						int partyDmg = 0;
						float partyMul = 1;
						int partyLvl = 0;
						
						// Get all L2Character that can be rewarded in the party
						final List<L2PcInstance> rewardedMembers = new ArrayList<>();
						// Go through all L2PcInstance in the party
						final List<L2PcInstance> groupMembers = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers();
						for (L2PcInstance partyPlayer : groupMembers)
						{
							if ((partyPlayer == null) || partyPlayer.isDead())
							{
								continue;
							}
							
							// Get the RewardInfo of this L2PcInstance from L2Attackable rewards
							final DamageDoneInfo reward2 = rewards.get(partyPlayer);
							
							// If the L2PcInstance is in the L2Attackable rewards add its damages to party damages
							if (reward2 != null)
							{
								if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, partyPlayer, true))
								{
									partyDmg += reward2.getDamage(); // Add L2PcInstance damages to party damages
									rewardedMembers.add(partyPlayer);
									
									if (partyPlayer.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = partyPlayer.getLevel();
										}
									}
								}
								rewards.remove(partyPlayer); // Remove the L2PcInstance from the L2Attackable rewards
							}
							else if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, partyPlayer, true))
							{
								rewardedMembers.add(partyPlayer);
								if (partyPlayer.getLevel() > partyLvl)
								{
									if (attackerParty.isInCommandChannel())
									{
										partyLvl = attackerParty.getCommandChannel().getLevel();
									}
									else
									{
										partyLvl = partyPlayer.getLevel();
									}
								}
							}
						}
						
						// If the party didn't killed this L2Attackable alone
						if (partyDmg < totalDamage)
						{
							partyMul = ((float) partyDmg / totalDamage);
						}
						
						// Calculate Exp and SP rewards
						final int[] expSp = calculateExpAndSp(partyLvl, partyDmg, totalDamage);
						long exp = expSp[0];
						int sp = expSp[1];
						
						if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
						{
							exp *= Config.L2JMOD_CHAMPION_REWARDS_EXP_SP;
							sp *= Config.L2JMOD_CHAMPION_REWARDS_EXP_SP;
						}
						
						exp *= partyMul;
						sp *= partyMul;
						
						// Check for an over-hit enabled strike
						// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
						final L2Character overhitAttacker = getOverhitAttacker();
						if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
						{
							attacker.sendPacket(SystemMessageId.OVER_HIT);
							exp += calculateOverhitExp(exp);
						}
						
						// Distribute Experience and SP rewards to L2PcInstance Party members in the known area of the last attacker
						if (partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, partyDmg, this);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
	
	@Override
	public void addAttackerToAttackByList(L2Character player)
	{
		if ((player == null) || (player == this) || getAttackByList().contains(player))
		{
			return;
		}
		getAttackByList().add(new WeakReference<>(player));
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 * @param skill
	 */
	public void addDamage(L2Character attacker, int damage, Skill skill)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Notify the L2Attackable AI with EVT_ATTACKED
		if (!isDead())
		{
			try
			{
				// If monster is on walk - stop it
				if (isWalker() && !isCoreAIDisabled() && WalkingManager.getInstance().isOnWalk(this))
				{
					WalkingManager.getInstance().stopMoving(this, false, true);
				}
				
				getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);
				
				// Calculate the amount of hate this attackable receives from this attack.
				double hateValue = (damage * 100) / (getLevel() + 7);
				
				if (skill == null)
				{
					hateValue *= attacker.getStat().getValue(Stats.HATE_ATTACK, 1);
				}
				
				addDamageHate(attacker, damage, (int) hateValue);
				
				final L2PcInstance player = attacker.getActingPlayer();
				if (player != null)
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(player, this, damage, skill, attacker.isSummon()), this);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 * @param aggro The hate (=damage) given by the attacker L2Character
	 */
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if ((attacker == null) || (attacker == this))
		{
			return;
		}
		
		L2PcInstance targetPlayer = attacker.getActingPlayer();
		final L2Character summoner = attacker.getSummoner();
		if (attacker.isNpc() && (summoner != null) && summoner.isPlayer() && !attacker.isTargetable())
		{
			targetPlayer = summoner.getActingPlayer();
			attacker = summoner;
		}
		
		// Get the AggroInfo of the attacker L2Character from the _aggroList of the L2Attackable
		if (_aggroList == null)
		{
			synchronized (this)
			{
				if (_aggroList == null)
				{
					_aggroList = new ConcurrentHashMap<>();
				}
			}
		}
		final AggroInfo ai = _aggroList.computeIfAbsent(attacker, AggroInfo::new);
		ai.addDamage(damage);
		
		// traps does not cause aggro
		// making this hack because not possible to determine if damage made by trap
		// so just check for triggered trap here
		if ((targetPlayer == null) || (targetPlayer.getTrap() == null) || !targetPlayer.getTrap().isTriggered())
		{
			ai.addHate(aggro);
		}
		
		if ((targetPlayer != null) && (aggro == 0))
		{
			addDamageHate(attacker, 0, 1);
			
			// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
			if (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			}
			
			// Notify to scripts
			EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAggroRangeEnter(this, targetPlayer, attacker.isSummon()), this);
		}
		else if ((targetPlayer == null) && (aggro == 0))
		{
			aggro = 1;
			ai.addHate(1);
		}
		
		// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
		if ((aggro != 0) && (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}
	
	public void reduceHate(L2Character target, int amount)
	{
		if ((getAI() instanceof L2SiegeGuardAI) || (getAI() instanceof L2FortSiegeGuardAI))
		{
			// TODO: this just prevents error until siege guards are handled properly
			stopHating(target);
			setTarget(null);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			return;
		}
		
		if (target == null) // whole aggrolist
		{
			final L2Character mostHated = getMostHated();
			if (mostHated == null) // makes target passive for a moment more
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			
			for (AggroInfo ai : getAggroList().values())
			{
				ai.addHate(amount);
			}
			
			amount = getHating(mostHated);
			if (amount >= 0)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
			return;
		}
		
		final AggroInfo ai = getAggroList().get(target);
		if (ai == null)
		{
			_log.info("target " + target + " not present in aggro list of " + this);
			return;
		}
		
		ai.addHate(amount);
		if ((ai.getHate() >= 0) && (getMostHated() == null))
		{
			((L2AttackableAI) getAI()).setGlobalAggro(-25);
			clearAggroList();
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			setWalking();
		}
	}
	
	/**
	 * Clears _aggroList hate of the L2Character without removing from the list.
	 * @param target
	 */
	public void stopHating(L2Character target)
	{
		if (target == null)
		{
			return;
		}
		
		final AggroInfo ai = getAggroList().get(target);
		if (ai != null)
		{
			ai.stopHate();
		}
	}
	
	/**
	 * @return the most hated L2Character of the L2Attackable _aggroList.
	 */
	public L2Character getMostHated()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		return getAggroList().values().stream().filter(Objects::nonNull).sorted(Comparator.comparingInt(AggroInfo::getHate).reversed()).map(AggroInfo::getAttacker).findFirst().orElse(null);
	}
	
	/**
	 * @return the 2 most hated L2Character of the L2Attackable _aggroList.
	 */
	public List<L2Character> get2MostHated()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		L2Character mostHated = null;
		L2Character secondMostHated = null;
		int maxHate = 0;
		final List<L2Character> result = new ArrayList<>();
		
		// While iterating over this map removing objects is not allowed
		// Go through the aggroList of the L2Attackable
		for (AggroInfo ai : getAggroList().values())
		{
			if (ai.checkHate(this) > maxHate)
			{
				secondMostHated = mostHated;
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		result.add(mostHated);
		
		if (getAttackByList().contains(secondMostHated))
		{
			result.add(secondMostHated);
		}
		else
		{
			result.add(null);
		}
		return result;
	}
	
	public List<L2Character> getHateList()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		final List<L2Character> result = new ArrayList<>();
		for (AggroInfo ai : getAggroList().values())
		{
			ai.checkHate(this);
			
			result.add(ai.getAttacker());
		}
		return result;
	}
	
	/**
	 * @param target The L2Character whose hate level must be returned
	 * @return the hate level of the L2Attackable against this L2Character contained in _aggroList.
	 */
	public int getHating(L2Character target)
	{
		if (getAggroList().isEmpty() || (target == null))
		{
			return 0;
		}
		
		final AggroInfo ai = getAggroList().get(target);
		if (ai == null)
		{
			return 0;
		}
		
		if (ai.getAttacker() instanceof L2PcInstance)
		{
			final L2PcInstance act = (L2PcInstance) ai.getAttacker();
			if (act.isInvisible() || ai.getAttacker().isInvul() || act.isSpawnProtected())
			{
				// Remove Object Should Use This Method and Can be Blocked While Interacting
				getAggroList().remove(target);
				return 0;
			}
		}
		
		if (!ai.getAttacker().isSpawned() || ai.getAttacker().isInvisible())
		{
			getAggroList().remove(target);
			return 0;
		}
		
		if (ai.getAttacker().isAlikeDead())
		{
			ai.stopHate();
			return 0;
		}
		return ai.getHate();
	}
	
	public void doItemDrop(L2Character mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}
	
	/**
	 * Manage Base, Quests and Special Events drops of L2Attackable (called by calculateRewards).<br>
	 * Concept:<br>
	 * During a Special Event all L2Attackable can drop extra Items.<br>
	 * Those extra Items are defined in the table allNpcDateDrops of the EventDroplist.<br>
	 * Each Special Event has a start and end date to stop to drop extra Items automatically.<br>
	 * Actions:<br>
	 * Manage drop of Special Events created by GM for a defined period.<br>
	 * Get all possible drops of this L2Attackable from L2NpcTemplate and add it Quest drops.<br>
	 * For each possible drops (base + quests), calculate which one must be dropped (random).<br>
	 * Get each Item quantity dropped (random).<br>
	 * Create this or these L2ItemInstance corresponding to each Item Identifier dropped.<br>
	 * If the autoLoot mode is actif and if the L2Character that has killed the L2Attackable is a L2PcInstance, Give the item(s) to the L2PcInstance that has killed the L2Attackable.<br>
	 * If the autoLoot mode isn't actif or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these item(s) in the world as a visible object at the position where mob was last.
	 * @param npcTemplate
	 * @param mainDamageDealer
	 */
	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character mainDamageDealer)
	{
		if (mainDamageDealer == null)
		{
			return;
		}
		
		final L2PcInstance player = mainDamageDealer.getActingPlayer();
		
		// Don't drop anything if the last attacker or owner isn't L2PcInstance
		if (player == null)
		{
			return;
		}
		
		CursedWeaponsManager.getInstance().checkDrop(this, player);
		
		npcTemplate.getExtendDrop().stream().map(ExtendDropData.getInstance()::getExtendDropById).filter(Objects::nonNull).forEach(e -> e.reward(player, this));
		
		if (isSpoiled())
		{
			_sweepItems.set(npcTemplate.calculateDrops(DropListScope.CORPSE, this, player));
		}
		
		final Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropListScope.DEATH, this, player);
		if (deathItems != null)
		{
			for (ItemHolder drop : deathItems)
			{
				final L2Item item = ItemTable.getInstance().getTemplate(drop.getId());
				// Check if the autoLoot mode is active
				if (isFlying() || (!item.hasExImmediateEffect() && ((!isRaid() && Config.AUTO_LOOT) || (isRaid() && Config.AUTO_LOOT_RAIDS))) || (item.hasExImmediateEffect() && Config.AUTO_LOOT_HERBS))
				{
					player.doAutoLoot(this, drop); // Give the item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, drop); // drop the item on the ground
				}
				
				// Broadcast message if RaidBoss was defeated
				if (isRaid() && !isRaidMinion())
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DIED_AND_DROPPED_S3_S2_S);
					sm.addCharName(this);
					sm.addItemName(item);
					sm.addLong(drop.getCount());
					broadcastPacket(sm);
				}
			}
		}
		
		// Apply Special Item drop with random(rnd) quantity(qty) for champions.
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && ((Config.L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE > 0) || (Config.L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE > 0)))
		{
			int champqty = Rnd.get(Config.L2JMOD_CHAMPION_REWARD_QTY);
			final ItemHolder item = new ItemHolder(Config.L2JMOD_CHAMPION_REWARD_ID, ++champqty);
			
			if ((player.getLevel() <= getLevel()) && (Rnd.get(100) < Config.L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE))
			{
				if (Config.AUTO_LOOT || isFlying())
				{
					player.addItem("ChampionLoot", item.getId(), item.getCount(), this, true); // Give the item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, item);
				}
			}
			else if ((player.getLevel() > getLevel()) && (Rnd.get(100) < Config.L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE))
			{
				if (Config.AUTO_LOOT || isFlying())
				{
					player.addItem("ChampionLoot", item.getId(), item.getCount(), this, true); // Give the item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, item);
				}
			}
		}
	}
	
	/**
	 * Manage Special Events drops created by GM for a defined period.<br>
	 * Concept:<br>
	 * During a Special Event all L2Attackable can drop extra Items.<br>
	 * Those extra Items are defined in the table allNpcDateDrops of the EventDroplist.<br>
	 * Each Special Event has a start and end date to stop to drop extra Items automatically.<br>
	 * Actions: <I>If an extra drop must be generated</I><br>
	 * Get an Item Identifier (random) from the DateDrop Item table of this Event.<br>
	 * Get the Item quantity dropped (random).<br>
	 * Create this or these L2ItemInstance corresponding to this Item Identifier.<br>
	 * If the autoLoot mode is actif and if the L2Character that has killed the L2Attackable is a L2PcInstance, Give the item(s) to the L2PcInstance that has killed the L2Attackable<br>
	 * If the autoLoot mode isn't actif or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these item(s) in the world as a visible object at the position where mob was last
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	public void doEventDrop(L2Character lastAttacker)
	{
		if (lastAttacker == null)
		{
			return;
		}
		
		final L2PcInstance player = lastAttacker.getActingPlayer();
		
		// Don't drop anything if the last attacker or owner isn't L2PcInstance
		if (player == null)
		{
			return;
		}
		
		if ((player.getLevel() - getLevel()) > 9)
		{
			return;
		}
		
		// Go through DateDrop of EventDroplist allNpcDateDrops within the date range
		for (DateDrop drop : EventDroplist.getInstance().getAllDrops())
		{
			if (Rnd.get(1000000) < drop.getEventDrop().getDropChance())
			{
				final int itemId = drop.getEventDrop().getItemIdList()[Rnd.get(drop.getEventDrop().getItemIdList().length)];
				final long itemCount = Rnd.get(drop.getEventDrop().getMinCount(), drop.getEventDrop().getMaxCount());
				if (Config.AUTO_LOOT || isFlying())
				{
					player.doAutoLoot(this, itemId, itemCount); // Give the item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, itemId, itemCount); // drop the item on the ground
				}
			}
		}
	}
	
	/**
	 * @return the active weapon of this L2Attackable (= null).
	 */
	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * @param player The L2Character searched in the _aggroList of the L2Attackable
	 * @return True if the _aggroList of this L2Attackable contains the L2Character.
	 */
	public boolean containsTarget(L2Character player)
	{
		return getAggroList().containsKey(player);
	}
	
	/**
	 * Clear the _aggroList of the L2Attackable.
	 */
	public void clearAggroList()
	{
		_aggroList = null;
		
		// clear overhit values
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * @return {@code true} if there is a loot to sweep, {@code false} otherwise.
	 */
	@Override
	public boolean isSweepActive()
	{
		return _sweepItems.get() != null;
	}
	
	/**
	 * @return a copy of dummy items for the spoil loot.
	 */
	public List<L2Item> getSpoilLootItems()
	{
		final Collection<ItemHolder> sweepItems = _sweepItems.get();
		final List<L2Item> lootItems = new LinkedList<>();
		if (sweepItems != null)
		{
			for (ItemHolder item : sweepItems)
			{
				lootItems.add(ItemTable.getInstance().getTemplate(item.getId()));
			}
		}
		return lootItems;
	}
	
	/**
	 * @return table containing all L2ItemInstance that can be spoiled.
	 */
	public Collection<ItemHolder> takeSweep()
	{
		return _sweepItems.getAndSet(null);
	}
	
	/**
	 * @return table containing all L2ItemInstance that can be harvested.
	 */
	public ItemHolder takeHarvest()
	{
		return _harvestItem.getAndSet(null);
	}
	
	/**
	 * Checks if the corpse is too old.
	 * @param attacker the player to validate
	 * @param remainingTime the time to check
	 * @param sendMessage if {@code true} will send a message of corpse too old
	 * @return {@code true} if the corpse is too old
	 */
	public boolean isOldCorpse(L2PcInstance attacker, int remainingTime, boolean sendMessage)
	{
		if (isDead() && (DecayTaskManager.getInstance().getRemainingTime(this) < remainingTime))
		{
			if (sendMessage && (attacker != null))
			{
				attacker.sendPacket(SystemMessageId.THE_CORPSE_IS_TOO_OLD_THE_SKILL_CANNOT_BE_USED);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * @param sweeper the player to validate.
	 * @param sendMessage sendMessage if {@code true} will send a message of sweep not allowed.
	 * @return {@code true} if is the spoiler or is in the spoiler party.
	 */
	public boolean checkSpoilOwner(L2PcInstance sweeper, boolean sendMessage)
	{
		if ((sweeper.getObjectId() != getSpoilerObjectId()) && !sweeper.isInLooterParty(getSpoilerObjectId()))
		{
			if (sendMessage)
			{
				sweeper.sendPacket(SystemMessageId.THERE_ARE_NO_PRIORITY_RIGHTS_ON_A_SWEEPER);
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Set the over-hit flag on the L2Attackable.
	 * @param status The status of the over-hit flag
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Set the over-hit values like the attacker who did the strike and the amount of damage done by the skill.
	 * @param attacker The L2Character who hit on the L2Attackable using the over-hit enabled skill
	 * @param damage The amount of damage done by the over-hit enabled skill on the L2Attackable
	 */
	public void setOverhitValues(L2Character attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
		final double overhitDmg = -(getCurrentHp() - damage);
		if (overhitDmg < 0)
		{
			// we didn't killed the mob with the over-hit strike. (it wasn't really an over-hit strike)
			// let's just clear all the over-hit related values
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}
	
	/**
	 * Return the L2Character who hit on the L2Attackable using an over-hit enabled skill.
	 * @return L2Character attacker
	 */
	public L2Character getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Return the amount of damage done on the L2Attackable using an over-hit enabled skill.
	 * @return double damage
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * @return True if the L2Attackable was hit by an over-hit enabled skill.
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Calculate the Experience and SP to distribute to attacker (L2PcInstance, L2ServitorInstance or L2Party) of the L2Attackable.
	 * @param charLevel The killer level
	 * @param damage The damages given by the attacker (L2PcInstance, L2ServitorInstance or L2Party)
	 * @param totalDamage The total damage done
	 * @return
	 */
	private int[] calculateExpAndSp(int charLevel, int damage, long totalDamage)
	{
		final int levelDiff = Math.abs(charLevel - getLevel());
		double xp = Math.max(0, ((double) getExpReward() * damage) / totalDamage);
		double sp = Math.max(0, ((double) getSpReward() * damage) / totalDamage);
		double mul;
		switch (levelDiff)
		{
			case 0:
			case 1:
			case 2:
			{
				mul = 1.;
				break;
			}
			case 3:
			{
				mul = 0.97;
				break;
			}
			case 4:
			{
				mul = 0.67;
				break;
			}
			case 5:
			{
				mul = 0.42;
				break;
			}
			case 6:
			{
				mul = 0.25;
				break;
			}
			case 7:
			{
				mul = 0.15;
				break;
			}
			case 8:
			{
				mul = 0.09;
				break;
			}
			case 9:
			{
				mul = 0.05;
				break;
			}
			case 10:
			{
				mul = 0.03;
				break;
			}
			default:
			{
				mul = 0.;
				break;
			}
		}
		xp *= mul;
		sp *= mul;
		
		return new int[]
		{
			(int) xp,
			(int) sp
		};
	}
	
	public long calculateOverhitExp(long normalExp)
	{
		// Get the percentage based on the total of extra (over-hit) damage done relative to the total (maximum) ammount of HP on the L2Attackable
		double overhitPercentage = ((getOverhitDamage() * 100) / getMaxHp());
		
		// Over-hit damage percentages are limited to 25% max
		if (overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}
		
		// Get the overhit exp bonus according to the above over-hit damage percentage
		// (1/1 basis - 13% of over-hit damage, 13% of extra exp is given, and so on...)
		final double overhitExp = ((overhitPercentage / 100) * normalExp);
		
		// Return the rounded amount of exp points to be added to the player's normal exp reward
		return Math.round(overhitExp);
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Clear mob spoil, seed
		setSpoilerObjectId(0);
		
		// Clear all aggro list and overhit
		clearAggroList();
		
		// Clear Harvester reward
		_harvestItem.set(null);
		_sweepItems.set(null);
		
		// Clear mod Seeded stat
		_seeded = false;
		_seed = null;
		_seederObjId = 0;
		
		setWalking();
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		if (!isInActiveRegion())
		{
			if (hasAI())
			{
				getAI().stopAITask();
			}
		}
	}
	
	@Override
	public void onRespawn()
	{
		// Reset champion state
		_champion = false;
		
		if (Config.L2JMOD_CHAMPION_ENABLE)
		{
			// Set champion on next spawn
			if (isMonster() && !getTemplate().isUndying() && !isRaid() && !isRaidMinion() && (Config.L2JMOD_CHAMPION_FREQUENCY > 0) && (getLevel() >= Config.L2JMOD_CHAMP_MIN_LVL) && (getLevel() <= Config.L2JMOD_CHAMP_MAX_LVL) && (Config.L2JMOD_CHAMPION_ENABLE_IN_INSTANCES || (getInstanceId() == 0)))
			{
				if (Rnd.get(100) < Config.L2JMOD_CHAMPION_FREQUENCY)
				{
					_champion = true;
					setTeam(Team.RED);
				}
			}
		}
		
		// Reset the rest of NPC related states
		super.onRespawn();
	}
	
	/**
	 * Checks if its spoiled.
	 * @return {@code true} if its spoiled, {@code false} otherwise
	 */
	public boolean isSpoiled()
	{
		return _spoilerObjectId != 0;
	}
	
	/**
	 * Gets the spoiler object ID.
	 * @return the spoiler object ID if its spoiled, 0 otherwise
	 */
	public final int getSpoilerObjectId()
	{
		return _spoilerObjectId;
	}
	
	/**
	 * Sets the spoiler object ID.
	 * @param spoilerObjectId spoilerObjectId the spoiler object ID
	 */
	public final void setSpoilerObjectId(int spoilerObjectId)
	{
		_spoilerObjectId = spoilerObjectId;
	}
	
	/**
	 * Sets state of the mob to seeded. Parameters needed to be set before.
	 * @param seeder
	 */
	public final void setSeeded(L2PcInstance seeder)
	{
		if ((_seed != null) && (_seederObjId == seeder.getObjectId()))
		{
			_seeded = true;
			
			int count = 1;
			for (int skillId : getTemplate().getSkills().keySet())
			{
				switch (skillId)
				{
					case 4303: // Strong type x2
						count *= 2;
						break;
					case 4304: // Strong type x3
						count *= 3;
						break;
					case 4305: // Strong type x4
						count *= 4;
						break;
					case 4306: // Strong type x5
						count *= 5;
						break;
					case 4307: // Strong type x6
						count *= 6;
						break;
					case 4308: // Strong type x7
						count *= 7;
						break;
					case 4309: // Strong type x8
						count *= 8;
						break;
					case 4310: // Strong type x9
						count *= 9;
						break;
				}
			}
			
			// hi-lvl mobs bonus
			final int diff = getLevel() - _seed.getLevel() - 5;
			if (diff > 0)
			{
				count += diff;
			}
			_harvestItem.set(new ItemHolder(_seed.getCropId(), count * Config.RATE_DROP_MANOR));
		}
	}
	
	/**
	 * Sets the seed parameters, but not the seed state
	 * @param seed - instance {@link L2Seed} of used seed
	 * @param seeder - player who sows the seed
	 */
	public final void setSeeded(L2Seed seed, L2PcInstance seeder)
	{
		if (!_seeded)
		{
			_seed = seed;
			_seederObjId = seeder.getObjectId();
		}
	}
	
	public final int getSeederId()
	{
		return _seederObjId;
	}
	
	public final L2Seed getSeed()
	{
		return _seed;
	}
	
	public final boolean isSeeded()
	{
		return _seeded;
	}
	
	/**
	 * Check if the server allows Random Animation.
	 */
	// This is located here because L2Monster and L2FriendlyMob both extend this class. The other non-pc instances extend either L2NpcInstance or L2MonsterInstance.
	@Override
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_MONSTER_ANIMATION > 0) && isRandomAnimationEnabled() && !(this instanceof L2GrandBossInstance));
	}
	
	public void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}
	
	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}
	
	public L2CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}
	
	public void setFirstCommandChannelAttacked(L2CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}
	
	/**
	 * @return the _commandChannelLastAttack
	 */
	public long getCommandChannelLastAttack()
	{
		return _commandChannelLastAttack;
	}
	
	/**
	 * @param channelLastAttack the _commandChannelLastAttack to set
	 */
	public void setCommandChannelLastAttack(long channelLastAttack)
	{
		_commandChannelLastAttack = channelLastAttack;
	}
	
	public void returnHome()
	{
		clearAggroList();
		
		if (hasAI() && (getSpawn() != null))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, getSpawn().getLocation());
		}
	}
	
	/*
	 * Return vitality points decrease (if positive) or increase (if negative) based on damage. Maximum for damage = maxHp.
	 */
	public int getVitalityPoints(int level, long exp, boolean isBoss)
	{
		if ((getLevel() <= 0) || (getExpReward() <= 0))
		{
			return 0;
		}
		
		int points;
		if (level < 85)
		{
			points = (int) ((exp / 1000) * Math.max(level - getLevel(), 1));
		}
		else
		{
			points = (int) ((exp / (isBoss ? Config.VITALITY_CONSUME_BY_BOSS : Config.VITALITY_CONSUME_BY_MOB)) * Math.max(level - getLevel(), 1));
		}
		
		return -points;
	}
	
	/*
	 * True if vitality rate for exp and sp should be applied
	 */
	public boolean useVitalityRate()
	{
		return !isChampion() || Config.L2JMOD_CHAMPION_ENABLE_VITALITY;
	}
	
	/** Return True if the L2Character is RaidBoss or his minion. */
	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	/**
	 * Set this Npc as a Raid instance.
	 * @param isRaid
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	/**
	 * Set this Npc as a Minion instance.
	 * @param val
	 */
	public void setIsRaidMinion(boolean val)
	{
		_isRaid = val;
		_isRaidMinion = val;
	}
	
	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}
	
	@Override
	public boolean isMinion()
	{
		return getLeader() != null;
	}
	
	/**
	 * @return leader of this minion or null.
	 */
	public L2Attackable getLeader()
	{
		return null;
	}
	
	@Override
	public boolean isChampion()
	{
		return _champion;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public void setTarget(L2Object object)
	{
		if (object == null)
		{
			final L2Object target = getTarget();
			final Map<L2Character, AggroInfo> aggroList = _aggroList;
			if (target != null)
			{
				if (aggroList != null)
				{
					aggroList.remove(target);
				}
			}
			if ((aggroList != null) && aggroList.isEmpty())
			{
				if (getAI() instanceof L2AttackableAI)
				{
					((L2AttackableAI) getAI()).setGlobalAggro(-25);
				}
				setWalking();
				clearAggroList();
			}
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
		super.setTarget(object);
	}
}
