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
import java.util.Map;
import java.util.logging.Level;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.EventDroplist;
import com.l2jmobius.gameserver.EventDroplist.DateDrop;
import com.l2jmobius.gameserver.ItemsAutoDestroy;
import com.l2jmobius.gameserver.ai.CtrlEvent;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.ai.L2AttackableAI;
import com.l2jmobius.gameserver.ai.L2CharacterAI;
import com.l2jmobius.gameserver.ai.L2SiegeGuardAI;
import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2FolkInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2GrandBossInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PetInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PlayableInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2RaidBossInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2SummonInstance;
import com.l2jmobius.gameserver.model.actor.knownlist.AttackableKnownList;
import com.l2jmobius.gameserver.model.base.SoulCrystal;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.skills.Stats;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;
import com.l2jmobius.gameserver.util.Util;
import com.l2jmobius.util.Rnd;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * This class manages all NPC that can be attacked.<BR>
 * <BR>
 * L2Attackable :<BR>
 * <BR>
 * <li>L2ArtefactInstance</li>
 * <li>L2FriendlyMobInstance</li>
 * <li>L2MonsterInstance</li>
 * <li>L2SiegeGuardInstance</li>
 * @version $Revision: 1.24.2.3.2.16 $ $Date: 2005/04/11 19:11:21 $
 */
public class L2Attackable extends L2NpcInstance
{
	// protected static Logger _log = Logger.getLogger(L2Attackable.class.getName());
	
	/**
	 * This class contains all AggroInfo of the L2Attackable against the attacker L2Character.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>attacker : The attaker L2Character concerned by this AggroInfo of this L2Attackable</li>
	 * <li>hate : Hate level of this L2Attackable against the attaker L2Character (hate = damage)</li>
	 * <li>damage : Number of damages that the attaker L2Character gave to this L2Attackable</li><BR>
	 * <BR>
	 */
	public final class AggroInfo
	{
		/** The attaker L2Character concerned by this AggroInfo of this L2Attackable */
		L2Character attacker;
		
		/** Hate level of this L2Attackable against the attaker L2Character (hate = damage) */
		int hate;
		
		/** Number of damages that the attaker L2Character gave to this L2Attackable */
		int damage;
		
		/**
		 * Constructor of AggroInfo.<BR>
		 * <BR>
		 * @param pAttacker
		 */
		AggroInfo(L2Character pAttacker)
		{
			attacker = pAttacker;
		}
		
		/**
		 * Verify is object is equal to this AggroInfo.<BR>
		 * <BR>
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if (obj instanceof AggroInfo)
			{
				return (((AggroInfo) obj).attacker == attacker);
			}
			
			return false;
		}
		
		/**
		 * Return the Identifier of the attaker L2Character.<BR>
		 * <BR>
		 */
		@Override
		public int hashCode()
		{
			return attacker.getObjectId();
		}
		
	}
	
	/**
	 * This class contains all RewardInfo of the L2Attackable against the any attacker L2Character, based on amount of damage done.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>attacker : The attaker L2Character concerned by this RewardInfo of this L2Attackable</li>
	 * <li>dmg : Total amount of damage done by the attacker to this L2Attackable (summon + own)</li>
	 */
	protected final class RewardInfo
	{
		protected L2Character attacker;
		protected int dmg = 0;
		
		public RewardInfo(L2Character pAttacker, int pDmg)
		{
			attacker = pAttacker;
			dmg = pDmg;
		}
		
		public void addDamage(int pDmg)
		{
			dmg += pDmg;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if (obj instanceof RewardInfo)
			{
				return (((RewardInfo) obj).attacker == attacker);
			}
			
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return attacker.getObjectId();
		}
	}
	
	/**
	 * This class contains all AbsorberInfo of the L2Attackable against the absorber L2Character.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>absorber : The attaker L2Character concerned by this AbsorberInfo of this L2Attackable</li>
	 */
	public final class AbsorberInfo
	{
		/** The attaker L2Character concerned by this AbsorberInfo of this L2Attackable */
		L2PcInstance absorber;
		int crystalId;
		double absorbedHP;
		
		/**
		 * Constructor of AbsorberInfo.<BR>
		 * <BR>
		 * @param attacker
		 * @param pCrystalId
		 * @param pAbsorbedHP
		 */
		AbsorberInfo(L2PcInstance attacker, int pCrystalId, double pAbsorbedHP)
		{
			absorber = attacker;
			crystalId = pCrystalId;
			absorbedHP = pAbsorbedHP;
		}
		
		/**
		 * Verify is object is equal to this AbsorberInfo.<BR>
		 * <BR>
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if (obj instanceof AbsorberInfo)
			{
				return (((AbsorberInfo) obj).absorber == absorber);
			}
			
			return false;
		}
		
		/**
		 * Return the Identifier of the absorber L2Character.<BR>
		 * <BR>
		 */
		@Override
		public int hashCode()
		{
			return absorber.getObjectId();
		}
	}
	
	/**
	 * This class is used to create item reward lists instead of creating item instances.<BR>
	 * <BR>
	 */
	public final class RewardItem
	{
		protected int _itemId;
		protected int _count;
		
		public RewardItem(int itemId, int count)
		{
			_itemId = itemId;
			_count = count;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public int getCount()
		{
			return _count;
		}
	}
	
	private final FastMap<L2Character, AggroInfo> _aggroList = new FastMap<L2Character, AggroInfo>().shared();
	
	/**
	 * Use this to Remove Object from this Map This Should be Synchronized While Iterating over This Map - if u cant iterating and removing object at once
	 * @return
	 */
	public final FastMap<L2Character, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	private boolean _isReturningToSpawnPoint = false;
	
	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}
	
	public final void setIsReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}
	
	/** Table containing all Items that a Dwarf can Sweep on this L2Attackable */
	private RewardItem[] _sweepItems;
	
	/** crops */
	private RewardItem[] _harvestItems;
	private boolean _seeded;
	private int _seedType = 0;
	private L2PcInstance _seeder = null;
	
	/** True if an over-hit enabled skill has successfully landed on the L2Attackable */
	private boolean _overhit;
	
	/** Stores the extra (over-hit) damage done to the L2Attackable when the attacker uses an over-hit enabled skill */
	private double _overhitDamage;
	
	/** Stores the attacker who used the over-hit enabled skill on the L2Attackable */
	private L2Character _overhitAttacker;
	
	/** True if a Soul Crystal was successfuly used on the L2Attackable */
	private boolean _absorbed;
	
	/** The table containing all L2PcInstance that successfuly absorbed the soul of this L2Attackable */
	private final FastMap<L2PcInstance, AbsorberInfo> _absorbersList = new FastMap<L2PcInstance, AbsorberInfo>().shared();
	
	/** Have this L2Attackable to reward Exp and SP on Die? **/
	private boolean _mustGiveExpSp;
	
	/**
	 * Constructor of L2Attackable (use L2Character and L2NpcInstance constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2Attackable (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2Attackable</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template to apply to the NPC
	 */
	public L2Attackable(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		_mustGiveExpSp = true;
	}
	
	@Override
	public AttackableKnownList getKnownList()
	{
		if ((super.getKnownList() == null) || !(super.getKnownList() instanceof AttackableKnownList))
		{
			setKnownList(new AttackableKnownList(this));
		}
		return (AttackableKnownList) super.getKnownList();
	}
	
	/**
	 * Return the L2Character AI of the L2Attackable and if its null create a new one.<BR>
	 * <BR>
	 */
	@Override
	public L2CharacterAI getAI()
	{
		if (_ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
				{
					_ai = new L2AttackableAI(new AIAccessor());
				}
			}
		}
		return _ai;
	}
	
	// get condition to hate, actually isAggressive() is checked
	// by monster and karma by guards in motheds that overwrite this one.
	/**
	 * Not used.<BR>
	 * <BR>
	 * @param target
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public boolean getCondition2(L2Character target)
	{
		if ((target instanceof L2FolkInstance) || (target instanceof L2DoorInstance))
		{
			return false;
		}
		
		if (target.isAlikeDead() || !isInsideRadius(target, getAggroRange(), false, false) || (Math.abs(getZ() - target.getZ()) > 100))
		{
			return false;
		}
		
		return !target.isInvul();
	}
	
	/**
	 * Reduce the current HP of the L2Attackable.<BR>
	 * <BR>
	 * @param damage The HP decrease value
	 * @param attacker The L2Character who attacks
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker)
	{
		reduceCurrentHp(damage, attacker, true);
	}
	
	/**
	 * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.<BR>
	 * <BR>
	 * @param attacker The L2Character who attacks
	 * @param awake The awake state (If True : stop sleeping)
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		
		if (isEventMob)
		{
			return;
		}
		
		// Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList
		if (attacker != null)
		{
			addDamage(attacker, (int) damage);
		}
		
		// Reduce the current HP of the L2Attackable and launch the doDie Task if necessary
		super.reduceCurrentHp(damage, attacker, awake);
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
	 * Kill the L2Attackable (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members</li>
	 * <li>Notify the Quest Engine of the L2Attackable death if necessary</li>
	 * <li>Kill the L2NpcInstance (the corpse disappeared after 7 seconds)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR>
	 * <BR>
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
		
		// Enhance soul crystals of the attacker if this L2Attackable had its soul absorbed
		try
		{
			
			levelSoulCrystals(killer);
			
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
		
		// Notify the Quest Engine of the L2Attackable death if necessary
		try
		{
			final L2PcInstance player = killer.getActingPlayer();
			if (player != null)
			{
				if (getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL) != null)
				{
					for (final Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
					{
						quest.notifyKill(this, player, killer instanceof L2Summon);
					}
				}
			}
		}
		catch (final Exception e)
		{
			
			_log.log(Level.SEVERE, "", e);
		}
		
		return true;
	}
	
	/**
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the L2PcInstance owner of the L2SummonInstance (if necessary) and L2Party in progress</li>
	 * <li>Calculate the Experience and SP rewards in function of the level difference</li>
	 * <li>Add Exp and SP rewards to L2PcInstance (including Summon penalty) and to Party members in the known area of the last attacker</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR>
	 * <BR>
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	@Override
	protected void calculateRewards(L2Character lastAttacker)
	
	{
		// Creates an empty list of rewards
		FastMap<L2Character, RewardInfo> rewards = new FastMap<L2Character, RewardInfo>().shared();
		
		try
		{
			if (getAggroList().isEmpty())
			{
				return;
			}
			
			int damage;
			L2Character attacker, ddealer;
			RewardInfo reward;
			
			L2PcInstance maxDealer = null;
			@SuppressWarnings("unused")
			int maxDamage = 0;
			
			// While Iterating over This Map Removing Object is Not Allowed
			synchronized (getAggroList())
			{
				// Go through the _aggroList of the L2Attackable
				for (final AggroInfo info : getAggroList().values())
				{
					if (info == null)
					{
						continue;
					}
					
					// Get the L2Character corresponding to this attacker
					attacker = info.attacker;
					
					// Get damages done by this attacker
					damage = info.damage;
					
					// Prevent unwanted behavior
					if (damage > 1)
					{
						if ((attacker instanceof L2SummonInstance) || ((attacker instanceof L2PetInstance) && (((L2PetInstance) attacker).getPetData().getOwnerExpTaken() > 0)))
						{
							ddealer = ((L2Summon) attacker).getOwner();
						}
						else
						{
							ddealer = info.attacker;
						}
						
						if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, ddealer, true))
						{
							continue;
						}
						
						// Calculate real damages (Summoners should get own damage plus summon's damage)
						reward = rewards.get(ddealer);
						
						if (reward == null)
						{
							reward = new RewardInfo(ddealer, damage);
						}
						else
						{
							reward.addDamage(damage);
						}
						
						rewards.put(ddealer, reward);
						
						maxDealer = ddealer.getActingPlayer();
						if (maxDealer != null)
						{
							maxDamage = reward.dmg;
						}
					}
				}
			}
			
			// Manage Base, and Sweep drops of the L2Attackable
			doItemDrop((maxDealer != null) && (maxDealer.isOnline() == 1) ? maxDealer : lastAttacker);
			
			// Manage drop of Special Events created by GM for a defined period
			doEventDrop(lastAttacker);
			
			if (!getMustRewardExpSP())
			{
				return;
			}
			
			if (!rewards.isEmpty())
			{
				
				L2Party attackerParty;
				
				long exp;
				int levelDiff;
				int partyDmg;
				int partyLvl;
				float partyMul;
				float penalty;
				
				RewardInfo reward2;
				int sp;
				int[] tmp;
				
				for (java.util.Map.Entry<L2Character, RewardInfo> entry : rewards.entrySet())
				{
					if (entry == null)
					{
						continue;
					}
					
					reward = entry.getValue();
					if (reward == null)
					{
						continue;
					}
					
					// Penalty applied to the attacker's XP
					penalty = 0;
					
					// Attacker to be rewarded
					attacker = reward.attacker;
					
					// Total amount of damage done
					damage = reward.dmg;
					
					// If the attacker is a Pet, get the party of the owner
					if (attacker instanceof L2PetInstance)
					{
						attackerParty = ((L2PetInstance) attacker).getParty();
					}
					else if (attacker instanceof L2PcInstance)
					{
						attackerParty = ((L2PcInstance) attacker).getParty();
					}
					else
					{
						return;
					}
					
					// If this attacker is a L2PcInstance with a summoned L2SummonInstance, get Exp Penalty applied for the current summoned L2SummonInstance
					if ((attacker instanceof L2PcInstance) && (((L2PcInstance) attacker).getPet() instanceof L2SummonInstance))
					{
						penalty = ((L2SummonInstance) ((L2PcInstance) attacker).getPet()).getExpPenalty();
					}
					
					// We must avoid "over damage", if any
					if (damage > getMaxHp())
					{
						damage = getMaxHp();
					}
					
					// If there's NO party in progress
					if (attackerParty == null)
					{
						// Calculate Exp and SP rewards
						if (attacker.getKnownList().knowsObject(this))
						{
							// Calculate the difference of level between this attacker (L2PcInstance or L2SummonInstance owner) and the L2Attackable
							// mob = 24, atk = 10, diff = -14 (full xp)
							// mob = 24, atk = 28, diff = 4 (some xp)
							// mob = 24, atk = 50, diff = 26 (no xp)
							levelDiff = attacker.getLevel() - getLevel();
							
							tmp = calculateExpAndSp(levelDiff, damage);
							exp = tmp[0];
							exp *= 1 - penalty;
							sp = tmp[1];
							
							if (Config.CHAMPION_ENABLE && isChampion())
							{
								exp *= Config.CHAMPION_REWARDS;
								sp *= Config.CHAMPION_REWARDS;
							}
							
							// Check for an over-hit enabled strike
							if (attacker instanceof L2PcInstance)
							{
								final L2PcInstance player = (L2PcInstance) attacker;
								if (isOverhit() && (attacker == getOverhitAttacker()))
								{
									player.sendPacket(new SystemMessage(SystemMessage.OVER_HIT));
									exp += calculateOverhitExp(exp);
								}
							}
							
							// Distribute the Exp and SP between the L2PcInstance and its L2Summon
							if (!attacker.isDead())
							{
								attacker.addExpAndSp(Math.round(attacker.calcStat(Stats.EXPSP_RATE, exp, null, null)), (int) attacker.calcStat(Stats.EXPSP_RATE, sp, null, null));
							}
						}
					}
					else
					{
						// share with party members
						partyDmg = 0;
						partyMul = 1.f;
						partyLvl = 0;
						
						// Get all L2Character that can be rewarded in the party
						final List<L2PlayableInstance> rewardedMembers = new FastList<>();
						
						// Go through all L2PcInstance in the party
						for (final L2PcInstance pl : attackerParty.getPartyMembers())
						{
							if ((pl == null) || pl.isDead())
							{
								continue;
							}
							
							// Get the RewardInfo of this L2PcInstance from L2Attackable rewards
							reward2 = rewards.get(pl);
							
							// If the L2PcInstance is in the L2Attackable rewards add its damages to party damages
							if (reward2 != null)
							{
								if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, pl, true))
								{
									// Add L2PcInstance damages to party damages
									partyDmg += reward2.dmg;
									rewardedMembers.add(pl);
									
									if (pl.getLevel() > partyLvl)
									{
										partyLvl = pl.getLevel();
									}
								}
								
								// Remove the L2PcInstance from the L2Attackable rewards
								rewards.remove(pl);
							}
							else
							{
								// Add L2PcInstance of the party (that have attacked or not) to members that can be rewarded if it's not dead
								// and in range of the monster.
								if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, pl, true))
								{
									rewardedMembers.add(pl);
									
									if (pl.getLevel() > partyLvl)
									{
										partyLvl = pl.getLevel();
									}
								}
							}
							
							final L2PlayableInstance summon = pl.getPet();
							if ((summon != null) && (summon instanceof L2PetInstance))
							{
								reward2 = rewards.get(summon);
								if (reward2 != null)
								{
									
									if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, summon, true))
									{
										partyDmg += reward2.dmg;
										rewardedMembers.add(summon);
										
										if (summon.getLevel() > partyLvl)
										{
											partyLvl = summon.getLevel();
										}
									}
									
									// Remove the summon from the L2Attackable rewards
									rewards.remove(summon);
								}
							}
						}
						
						// If the party didn't killed this L2Attackable alone
						if (partyDmg < getMaxHp())
						{
							partyMul = ((float) partyDmg / (float) getMaxHp());
						}
						
						// Avoid "over damage"
						if (partyDmg > getMaxHp())
						{
							partyDmg = getMaxHp();
						}
						
						// Calculate the level difference between Party and L2Attackable
						levelDiff = partyLvl - getLevel();
						
						// Calculate Exp and SP rewards
						tmp = calculateExpAndSp(levelDiff, partyDmg);
						exp = tmp[0];
						sp = tmp[1];
						
						if (Config.CHAMPION_ENABLE && isChampion())
						{
							exp *= Config.CHAMPION_REWARDS;
							sp *= Config.CHAMPION_REWARDS;
						}
						
						exp *= partyMul;
						sp *= partyMul;
						
						// Check for an over-hit enabled strike
						// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
						if (attacker instanceof L2PcInstance)
						{
							final L2PcInstance player = (L2PcInstance) attacker;
							if (isOverhit() && (attacker == getOverhitAttacker()))
							{
								
								player.sendPacket(new SystemMessage(SystemMessage.OVER_HIT));
								exp += calculateOverhitExp(exp);
							}
						}
						
						// Distribute Experience and SP rewards to L2PcInstance Party members in the known area of the last attacker
						if (partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl);
						}
					}
				}
			}
			
			rewards = null;
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 */
	public void addDamage(L2Character attacker, int damage)
	{
		if (attacker == null)
		{
			return;
		}
		
		try
		{
			final L2PcInstance player = attacker.getActingPlayer();
			if (player != null)
			{
				
				if (getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK) != null)
				{
					for (final Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
					{
						quest.notifyAttack(this, player, damage, attacker instanceof L2Summon);
					}
				}
			}
			getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
		
		addDamageHate(attacker, damage, damage);
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 * @param aggro The hate (=damage) given by the attacker L2Character
	 */
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Get the AggroInfo of the attacker L2Character from the _aggroList of the L2Attackable
		AggroInfo ai = getAggroList().get(attacker);
		if (ai == null)
		
		{
			
			ai = new AggroInfo(attacker);
			ai.damage = 0;
			ai.hate = 0;
			
			getAggroList().put(attacker, ai);
			
			final L2PcInstance player = attacker.getActingPlayer();
			if (player != null)
			{
				if (getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER) != null)
				{
					for (final Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER))
					{
						quest.notifyAggroRangeEnter(this, player, (attacker instanceof L2Summon));
					}
				}
			}
		}
		
		// Add new damage and aggro (=damage) to the AggroInfo object
		ai.damage += damage;
		ai.hate += aggro;
		
		// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
		if ((aggro > 0) && (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
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
		if (ai == null)
		{
			return;
		}
		
		ai.hate = 0;
	}
	
	/**
	 * Return the most hated L2Character of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * @return
	 */
	public L2Character getMostHated()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		L2Character mostHated = null;
		int maxHate = 0;
		
		// While Iterating over This Map Removing Object is Not Allowed
		
		synchronized (getAggroList())
		{
			// Go through the aggroList of the L2Attackable
			for (final AggroInfo ai : getAggroList().values())
			{
				if (ai == null)
				{
					continue;
				}
				
				if (ai.attacker.isAlikeDead() || !getKnownList().knowsObject(ai.attacker) || !ai.attacker.isVisible())
				{
					ai.hate = 0;
				}
				
				if (ai.hate > maxHate)
				{
					mostHated = ai.attacker;
					maxHate = ai.hate;
				}
			}
		}
		
		return mostHated;
	}
	
	/**
	 * Return the 2 most hated L2Character of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * @return
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
		final List<L2Character> result = new FastList<>();
		
		// While Interating over This Map Removing Object is Not Allowed
		synchronized (getAggroList())
		{
			// Go through the aggroList of the L2Attackable
			for (final AggroInfo ai : getAggroList().values())
			{
				if (ai == null)
				{
					continue;
				}
				
				if (ai.attacker.isAlikeDead() || !getKnownList().knowsObject(ai.attacker) || !ai.attacker.isVisible())
				{
					ai.hate = 0;
				}
				
				if (ai.hate > maxHate)
				{
					secondMostHated = mostHated;
					mostHated = ai.attacker;
					maxHate = ai.hate;
				}
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
	
	/**
	 * Return the hate level of the L2Attackable against this L2Character contained in _aggroList.<BR>
	 * <BR>
	 * @param target The L2Character whose hate level must be returned
	 * @return
	 */
	public int getHating(L2Character target)
	{
		if (getAggroList().isEmpty())
		{
			return 0;
		}
		
		final AggroInfo ai = getAggroList().get(target);
		if (ai == null)
		{
			return 0;
		}
		
		if ((ai.attacker instanceof L2PcInstance) && (((L2PcInstance) ai.attacker).getAppearance().getInvisible() || ai.attacker.isInvul()))
		{
			// Remove Object Should Use This Method and Can be Blocked While Iterating
			getAggroList().remove(target);
			return 0;
		}
		if (!ai.attacker.isVisible())
		{
			getAggroList().remove(target);
			return 0;
		}
		if (ai.attacker.isAlikeDead())
		{
			ai.hate = 0;
			return 0;
		}
		
		return ai.hate;
	}
	
	/**
	 * Calculates quantity of items for specific drop acording to current situation <br>
	 * @param drop The L2DropData count is being calculated for
	 * @param lastAttacker The L2PcInstance that has killed the L2Attackable
	 * @param levelModifier level modifier in %'s (will be subtracted from drop chance)
	 * @param isSweep
	 * @return
	 */
	private RewardItem calculateRewardItem(L2PcInstance lastAttacker, L2DropData drop, int levelModifier, boolean isSweep)
	{
		// Get default drop chance
		float dropChance = drop.getChance();
		
		int deepBlueDrop = 1;
		if (Config.DEEPBLUE_DROP_RULES)
		{
			if (levelModifier > 0)
			{
				// We should multiply by the server's drop rate, so we always get a low chance of drop for deep blue mobs.
				// NOTE: This is valid only for adena drops! Others drops will still obey server's rate
				deepBlueDrop = 3;
				if (drop.getItemId() == 57)
				{
					deepBlueDrop *= isRaid() ? (int) Config.RATE_BOSS_DROP_ITEMS : (int) Config.RATE_DROP_ITEMS;
				}
			}
		}
		
		if (deepBlueDrop == 0)
		{
			deepBlueDrop = 1;
		}
		// Check if we should apply our maths so deep blue mobs will not drop that easy
		if (Config.DEEPBLUE_DROP_RULES)
		{
			dropChance = ((drop.getChance() - ((drop.getChance() * levelModifier) / 100)) / deepBlueDrop);
		}
		
		// Applies Drop rates
		if (drop.getItemId() == 57)
		{
			dropChance *= Config.RATE_DROP_ADENA;
		}
		else if (isSweep)
		{
			dropChance *= Config.RATE_DROP_SPOIL;
		}
		else
		{
			dropChance *= isRaid() ? Config.RATE_BOSS_DROP_ITEMS : Config.RATE_DROP_ITEMS;
		}
		
		if (Config.CHAMPION_ENABLE && isChampion())
		{
			dropChance *= Config.CHAMPION_REWARDS;
		}
		
		// Round drop chance
		dropChance = Math.round(dropChance);
		
		// Set our limits for chance of drop
		if (dropChance < 1)
		{
			dropChance = 1;
			// if (drop.getItemId() == 57 && dropChance > L2DropData.MAX_CHANCE) dropChance = L2DropData.MAX_CHANCE; // If item is adena, dont drop multiple time
		}
		
		// Get min and max Item quantity that can be dropped in one time
		final int minCount = drop.getMinDrop();
		final int maxCount = drop.getMaxDrop();
		int itemCount = 0;
		
		// Count and chance adjustment for high rate servers
		if ((dropChance > L2DropData.MAX_CHANCE) && !Config.PRECISE_DROP_CALCULATION)
		{
			final int multiplier = (int) dropChance / L2DropData.MAX_CHANCE;
			if (minCount < maxCount)
			{
				itemCount += Rnd.get(minCount * multiplier, maxCount * multiplier);
			}
			else if (minCount == maxCount)
			{
				itemCount += minCount * multiplier;
			}
			else
			{
				itemCount += multiplier;
			}
			
			dropChance = dropChance % L2DropData.MAX_CHANCE;
		}
		
		// Check if the Item must be dropped
		final int random = Rnd.get(L2DropData.MAX_CHANCE);
		while (random < dropChance)
		{
			// Get the item quantity dropped
			if (minCount < maxCount)
			{
				itemCount += Rnd.get(minCount, maxCount);
			}
			else if (minCount == maxCount)
			{
				itemCount += minCount;
			}
			else
			{
				itemCount++;
			}
			
			// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
			dropChance -= L2DropData.MAX_CHANCE;
		}
		
		if (Config.CHAMPION_ENABLE)
		{
			if (((drop.getItemId() == 57) || ((drop.getItemId() >= 6360) && (drop.getItemId() <= 6362))) && isChampion())
			{
				itemCount *= Config.CHAMPION_ADENAS_REWARDS;
			}
		}
		
		if (itemCount > 0)
		{
			return new RewardItem(drop.getItemId(), itemCount);
		}
		else if ((itemCount == 0) && Config.DEBUG)
		{
			_log.fine("Roll produced 0 items to drop...");
		}
		
		return null;
	}
	
	/**
	 * Calculates quantity of items for specific drop CATEGORY according to current situation <br>
	 * Only a max of ONE item from a category is allowed to be dropped.
	 * @param lastAttacker The L2PcInstance that has killed the L2Attackable
	 * @param categoryDrops
	 * @param levelModifier level modifier in %'s (will be subtracted from drop chance)
	 * @return
	 */
	private RewardItem calculateCategorizedRewardItem(L2PcInstance lastAttacker, L2DropCategory categoryDrops, int levelModifier)
	{
		if (categoryDrops == null)
		{
			return null;
		}
		
		// Get default drop chance for the category (that's the sum of chances for all items in the category)
		// keep track of the base category chance as it'll be used later, if an item is drop from the category.
		// for everything else, use the total "categoryDropChance"
		final int basecategoryDropChance = categoryDrops.getCategoryChance();
		int categoryDropChance = basecategoryDropChance;
		
		int deepBlueDrop = 1;
		if (Config.DEEPBLUE_DROP_RULES)
		{
			if (levelModifier > 0)
			{
				// We should multiply by the server's drop rate, so we always get a low chance of drop for deep blue mobs.
				// NOTE: This is valid only for adena drops! Others drops will still obey server's rate
				deepBlueDrop = 3;
			}
		}
		
		if (deepBlueDrop == 0)
		{
			deepBlueDrop = 1;
		}
		// Check if we should apply our maths so deep blue mobs will not drop that easy
		if (Config.DEEPBLUE_DROP_RULES)
		{
			categoryDropChance = ((categoryDropChance - ((categoryDropChance * levelModifier) / 100)) / deepBlueDrop);
		}
		
		// Applies Drop rates
		categoryDropChance *= isRaid() ? Config.RATE_BOSS_DROP_ITEMS : Config.RATE_DROP_ITEMS;
		
		if (Config.CHAMPION_ENABLE && isChampion())
		{
			categoryDropChance *= Config.CHAMPION_REWARDS;
		}
		
		// Round drop chance
		categoryDropChance = Math.round(categoryDropChance);
		
		// Set our limits for chance of drop
		if (categoryDropChance < 1)
		{
			categoryDropChance = 1;
		}
		
		// Check if an Item from this category must be dropped
		if (Rnd.get(L2DropData.MAX_CHANCE) < categoryDropChance)
		{
			final L2DropData drop = categoryDrops.dropOne(isRaid());
			if (drop == null)
			{
				return null;
			}
			
			// Now decide the quantity to drop based on the rates and penalties. To get this value
			// simply divide the modified categoryDropChance by the base category chance. This
			// results in a chance that will dictate the drops amounts: for each amount over 100
			// that it is, it will give another chance to add to the min/max quantities.
			//
			// For example, If the final chance is 120%, then the item should drop between
			// its min and max one time, and then have 20% chance to drop again. If the final
			// chance is 330%, it will similarly give 3 times the min and max, and have a 30%
			// chance to give a 4th time.
			// At least 1 item will be dropped for sure. So the chance will be adjusted to 100%
			// if smaller.
			
			int dropChance = drop.getChance();
			if (drop.getItemId() == 57)
			{
				dropChance *= Config.RATE_DROP_ADENA;
			}
			else
			{
				dropChance *= isRaid() ? Config.RATE_BOSS_DROP_ITEMS : Config.RATE_DROP_ITEMS;
			}
			
			if (Config.CHAMPION_ENABLE && isChampion())
			{
				dropChance *= Config.CHAMPION_REWARDS;
			}
			
			dropChance = Math.round(dropChance);
			
			if (dropChance < L2DropData.MAX_CHANCE)
			{
				dropChance = L2DropData.MAX_CHANCE;
			}
			
			// Get min and max Item quantity that can be dropped in one time
			final int min = drop.getMinDrop();
			final int max = drop.getMaxDrop();
			
			// Get the item quantity dropped
			int itemCount = 0;
			
			// Count and chance adjustment for high rate servers
			if ((dropChance > L2DropData.MAX_CHANCE) && !Config.PRECISE_DROP_CALCULATION)
			{
				final int multiplier = dropChance / L2DropData.MAX_CHANCE;
				if (min < max)
				{
					itemCount += Rnd.get(min * multiplier, max * multiplier);
				}
				else if (min == max)
				{
					itemCount += min * multiplier;
				}
				else
				{
					itemCount += multiplier;
				}
				
				dropChance = dropChance % L2DropData.MAX_CHANCE;
			}
			
			// Check if the Item must be dropped
			final int random = Rnd.get(L2DropData.MAX_CHANCE);
			while (random < dropChance)
			{
				// Get the item quantity dropped
				if (min < max)
				{
					itemCount += Rnd.get(min, max);
				}
				else if (min == max)
				{
					itemCount += min;
				}
				else
				{
					itemCount++;
				}
				
				// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
				dropChance -= L2DropData.MAX_CHANCE;
			}
			
			if (Config.CHAMPION_ENABLE)
			{
				if (((drop.getItemId() == 57) || ((drop.getItemId() >= 6360) && (drop.getItemId() <= 6362))) && isChampion())
				{
					itemCount *= Config.CHAMPION_ADENAS_REWARDS;
				}
			}
			
			if (itemCount > 0)
			{
				return new RewardItem(drop.getItemId(), itemCount);
			}
			else if ((itemCount == 0) && Config.DEBUG)
			{
				_log.fine("Roll produced 0 items to drop...");
			}
		}
		return null;
	}
	
	/**
	 * Calculates the level modifier for drop<br>
	 * @param lastAttacker The L2PcInstance that has killed the L2Attackable
	 * @return
	 */
	private int calculateLevelModifierForDrop(L2PcInstance lastAttacker)
	{
		if (Config.DEEPBLUE_DROP_RULES)
		{
			int highestLevel = lastAttacker.getLevel();
			
			// Check to prevent very high level player to nearly kill mob and let low level player do the last hit.
			if ((getAttackByList() != null) && !getAttackByList().isEmpty())
			{
				for (final L2Character atkChar : getAttackByList())
				{
					if ((atkChar != null) && (atkChar.getLevel() > highestLevel))
					{
						highestLevel = atkChar.getLevel();
					}
				}
			}
			
			// According to official data (Prima), deep blue mobs are 9 or more levels below players
			if ((highestLevel - 9) >= getLevel())
			{
				return ((highestLevel - (getLevel() + 8)) * 9);
			}
		}
		
		return 0;
	}
	
	public void doItemDrop(L2Character mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}
	
	/**
	 * Manage Base, and Special Events drops of L2Attackable (called by calculateRewards).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * During a Special Event all L2Attackable can drop extra Items. Those extra Items are defined in the table <B>allNpcDateDrops</B> of the EventDroplist. Each Special Event has a start and end date to stop to drop extra Items automaticaly. <BR>
	 * <BR>
	 * <B><U> Actions</U> : </B><BR>
	 * <BR>
	 * <li>Manage drop of Special Events created by GM for a defined period</li>
	 * <li>For each possible drops, calculate which one must be dropped (random)</li>
	 * <li>Get each Item quantity dropped (random)</li>
	 * <li>Create this or these L2ItemInstance corresponding to each Item Identifier dropped</li>
	 * <li>If the autoLoot mode is active and if the L2Character that has killed the L2Attackable is a L2PcInstance, give this or these Item(s) to the L2PcInstance that has killed the L2Attackable</li>
	 * <li>If the autoLoot mode isn't active or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these Item(s) in the world as a visible object at the position where mob was last</li><BR>
	 * <BR>
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
		if (player == null)
		{
			return; // Don't drop anything if the last attacker or owner isn't L2PcInstance
		}
		
		final int levelModifier = calculateLevelModifierForDrop(player); // level modifier in %'s (will be subtracted from drop chance)
		
		// now throw all categorized drops and handle spoil.
		if (npcTemplate.getDropData() != null)
		{
			for (final L2DropCategory cat : npcTemplate.getDropData())
			{
				RewardItem item = null;
				if (cat.isSweep())
				{
					// according to sh1ny, seeded mobs CAN be spoiled and swept.
					if (isSpoil())
					{
						final FastList<RewardItem> sweepList = new FastList<>();
						for (final L2DropData drop : cat.getAllDrops())
						{
							item = calculateRewardItem(player, drop, levelModifier, true);
							if (item == null)
							{
								continue;
							}
							
							if (Config.DEBUG)
							{
								_log.fine("Item id to spoil: " + item.getItemId() + " amount: " + item.getCount());
							}
							sweepList.add(item);
						}
						
						// Set the table _sweepItems of this L2Attackable
						if (!sweepList.isEmpty())
						{
							_sweepItems = sweepList.toArray(new RewardItem[sweepList.size()]);
						}
					}
				}
				else
				{
					if (isSeeded())
					{
						final L2DropData drop = cat.dropSeedAllowedDropsOnly();
						if (drop == null)
						{
							continue;
						}
						
						item = calculateRewardItem(player, drop, levelModifier, false);
					}
					else
					{
						item = calculateCategorizedRewardItem(player, cat, levelModifier);
					}
					
					if (item != null)
					{
						if (Config.DEBUG)
						{
							_log.fine("Item id to drop: " + item.getItemId() + " amount: " + item.getCount());
						}
						
						// Check if the autoLoot mode is active
						if ((isRaid() && Config.AUTO_LOOT_RAIDS) || (Config.AUTO_LOOT && !isRaid()))
						{
							player.doAutoLoot(this, item); // Give this or these Item(s) to the L2PcInstance that has killed the L2Attackable
						}
						else
						{
							dropItem(player, item); // drop the item on the ground
						}
						
						// Broadcast message if RaidBoss was defeated
						if (this instanceof L2RaidBossInstance)
						{
							SystemMessage sm;
							sm = new SystemMessage(SystemMessage.S1_DIED_DROPPED_S3_S2);
							sm.addString(getName());
							sm.addItemName(item.getItemId());
							sm.addNumber(item.getCount());
							broadcastPacket(sm);
						}
					}
				}
			}
		}
		
		// Apply Special Item drop with random(rnd) quantity(qty) for champions.
		if (Config.CHAMPION_ENABLE && isChampion() && ((Config.CHAMPION_REWARD_LOWER_CHANCE > 0) || (Config.CHAMPION_REWARD_HIGHER_CHANCE > 0)))
		{
			int champqty = Rnd.get(Config.CHAMPION_REWARD_QTY);
			champqty++; // quantity should actually vary between 1 and whatever admin specified as max, inclusive.
			
			final RewardItem item = new RewardItem(Config.CHAMPION_REWARD_ID, champqty);
			
			if ((player.getLevel() <= getLevel()) && (Rnd.get(100) < Config.CHAMPION_REWARD_LOWER_CHANCE))
			{
				if (Config.AUTO_LOOT)
				{
					player.addItem("ChampionLoot", item.getItemId(), item.getCount(), this, true); // Give this or these Item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, item);
				}
			}
			else if ((player.getLevel() > getLevel()) && (Rnd.get(100) < Config.CHAMPION_REWARD_HIGHER_CHANCE))
			{
				if (Config.AUTO_LOOT)
				{
					player.addItem("ChampionLoot", item.getItemId(), item.getCount(), this, true); // Give this or these Item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, item);
				}
			}
		}
	}
	
	/**
	 * Manage Special Events drops created by GM for a defined period.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * During a Special Event all L2Attackable can drop extra Items. Those extra Items are defined in the table <B>allNpcDateDrops</B> of the EventDroplist. Each Special Event has a start and end date to stop to drop extra Items automaticaly. <BR>
	 * <BR>
	 * <B><U> Actions</U> : <I>If an extra drop must be generated</I></B><BR>
	 * <BR>
	 * <li>Get an Item Identifier (random) from the DateDrop Item table of this Event</li>
	 * <li>Get the Item quantity dropped (random)</li>
	 * <li>Create this or these L2ItemInstance corresponding to this Item Identifier</li>
	 * <li>If the autoLoot mode is active and if the L2Character that has killed the L2Attackable is a L2PcInstance, give this or these Item(s) to the L2PcInstance that has killed the L2Attackable</li>
	 * <li>If the autoLoot mode isn't active or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these Item(s) in the world as a visible object at the position where mob was last</li><BR>
	 * <BR>
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	public void doEventDrop(L2Character lastAttacker)
	{
		final L2PcInstance player = lastAttacker.getActingPlayer();
		if (player == null)
		{
			return; // Don't drop anything if the last attacker or ownere isn't L2PcInstance
		}
		
		if ((player.getLevel() - getLevel()) > 9)
		{
			return;
		}
		
		// Go through DateDrop of EventDroplist allNpcDateDrops within the date range
		for (final DateDrop drop : EventDroplist.getInstance().getAllDrops())
		{
			if (Rnd.get(L2DropData.MAX_CHANCE) < drop.chance)
			{
				final RewardItem item = new RewardItem(drop.items[Rnd.get(drop.items.length)], Rnd.get(drop.min, drop.max));
				if ((isRaid() && Config.AUTO_LOOT_RAIDS) || (Config.AUTO_LOOT && !isRaid()))
				{
					player.doAutoLoot(this, item); // Give this or these Item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, item); // drop the item on the ground
				}
			}
		}
	}
	
	/**
	 * Drop reward item.<BR>
	 * <BR>
	 * @param mainDamageDealer
	 * @param item
	 * @return
	 */
	public L2ItemInstance dropItem(L2PcInstance mainDamageDealer, RewardItem item)
	{
		final int randDropLim = 70;
		
		L2ItemInstance ditem = null;
		for (int i = 0; i < item.getCount(); i++)
		{
			// Randomize drop position
			final int newX = (getX() + Rnd.get((randDropLim * 2) + 1)) - randDropLim;
			final int newY = (getY() + Rnd.get((randDropLim * 2) + 1)) - randDropLim;
			final int newZ = Math.max(getZ(), mainDamageDealer.getZ()) + 20; // TODO: temp hack, do something nicer when we have geodatas
			
			// Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
			ditem = ItemTable.getInstance().createItem("Loot", item.getItemId(), item.getCount(), mainDamageDealer, this);
			ditem.getDropProtection().protect(mainDamageDealer);
			ditem.dropMe(this, newX, newY, newZ);
			
			// Add drop to auto destroy item task
			if (!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			{
				if (Config.AUTODESTROY_ITEM_AFTER > 0)
				{
					ItemsAutoDestroy.getInstance().addItem(ditem);
				}
			}
			ditem.setProtected(false);
			// If stackable, end loop as entire count is included in 1 instance of item
			if (ditem.isStackable() || !Config.MULTIPLE_ITEM_DROP)
			{
				break;
			}
		}
		return ditem;
	}
	
	public L2ItemInstance dropItem(L2PcInstance lastAttacker, int itemId, int itemCount)
	{
		return dropItem(lastAttacker, new RewardItem(itemId, itemCount));
	}
	
	/**
	 * Return the active weapon of this L2Attackable (= null).<BR>
	 * <BR>
	 * @return
	 */
	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Return True if the _aggroList of this L2Attackable is Empty.<BR>
	 * <BR>
	 * @return
	 */
	public boolean noTarget()
	{
		return getAggroList().isEmpty();
	}
	
	/**
	 * Return True if the _aggroList of this L2Attackable contains the L2Character.<BR>
	 * <BR>
	 * @param player The L2Character searched in the _aggroList of the L2Attackable
	 * @return
	 */
	public boolean containsTarget(L2Character player)
	{
		return getAggroList().containsKey(player);
	}
	
	/**
	 * Clear the _aggroList of the L2Attackable.<BR>
	 * <BR>
	 */
	public void clearAggroList()
	{
		getAggroList().clear();
		
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * Return True if a Dwarf use Sweep on the L2Attackable and if item can be spoiled.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isSweepActive()
	{
		return _sweepItems != null;
	}
	
	/**
	 * Return table containing all L2ItemInstance that can be spoiled.<BR>
	 * <BR>
	 * @return
	 */
	public synchronized RewardItem[] takeSweep()
	{
		final RewardItem[] sweep = _sweepItems;
		
		_sweepItems = null;
		
		return sweep;
	}
	
	/**
	 * Return table containing all L2ItemInstance that can be harvested.<BR>
	 * <BR>
	 * @return
	 */
	public synchronized RewardItem[] takeHarvest()
	{
		final RewardItem[] harvest = _harvestItems;
		_harvestItems = null;
		return harvest;
	}
	
	/**
	 * Set the over-hit flag on the L2Attackable.<BR>
	 * <BR>
	 * @param status The status of the over-hit flag
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Set the over-hit values like the attacker who did the strike and the ammount of damage done by the skill.<BR>
	 * <BR>
	 * @param attacker The L2Character who hit on the L2Attackable using the over-hit enabled skill
	 * @param damage The ammount of damage done by the over-hit enabled skill on the L2Attackable
	 */
	public void setOverhitValues(L2Character attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
		final double overhitDmg = ((getCurrentHp() - damage) * (-1));
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
	 * Return the L2Character who hit on the L2Attackable using an over-hit enabled skill.<BR>
	 * <BR>
	 * @return L2Character attacker
	 */
	public L2Character getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Return the ammount of damage done on the L2Attackable using an over-hit enabled skill.<BR>
	 * <BR>
	 * @return double damage
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * Return True if the L2Attackable was hit by an over-hit enabled skill.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Activate the absorbed soul condition on the L2Attackable.<BR>
	 * <BR>
	 */
	public void absorbSoul()
	{
		_absorbed = true;
	}
	
	/**
	 * Return True if the L2Attackable had his soul absorbed.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}
	
	/**
	 * Adds an attacker that successfully absorbed the soul of this L2Attackable into the _absorbersList.<BR>
	 * <BR>
	 * params: attacker - a valid L2PcInstance condition - an integer indicating the event when mob dies. This should be: = 0 - "the crystal scatters"; = 1 - "the crystal failed to absorb. nothing happens"; = 2 - "the crystal resonates because you got more than 1 crystal on you"; = 3 - "the crystal
	 * cannot absorb the soul because the mob level is too low"; = 4 - "the crystal successfuly absorbed the soul";
	 * @param attacker
	 * @param crystalId
	 */
	public void addAbsorber(L2PcInstance attacker, int crystalId)
	{
		// This just works for targets like L2MonsterInstance
		if (!(this instanceof L2MonsterInstance))
		{
			return;
		}
		
		// The attacker must not be null
		if (attacker == null)
		{
			return;
		}
		
		// This L2Attackable must be of one type in the _absorbingMOBS_levelXX tables.
		// OBS: This is done so to avoid triggering the absorbed conditions for mobs that can't be absorbed.
		if (getAbsorbLevel() == 0)
		{
			return;
		}
		
		// If we have no _absorbersList initiated, do it
		AbsorberInfo ai = _absorbersList.get(attacker);
		
		// If the L2Character attacker isn't already in the _absorbersList of this L2Attackable, add it
		if (ai == null)
		{
			ai = new AbsorberInfo(attacker, crystalId, getCurrentHp());
			_absorbersList.put(attacker, ai);
		}
		else
		{
			ai.absorber = attacker;
			ai.crystalId = crystalId;
			ai.absorbedHP = getCurrentHp();
		}
		
		// Set this L2Attackable as absorbed
		absorbSoul();
	}
	
	/**
	 * Calculate the leveling chance of Soul Crystals based on the attacker that killed this L2Attackable
	 * @param attacker The player that last killed this L2Attackable $ Rewrite 06.12.06 - Yesod
	 */
	private void levelSoulCrystals(L2Character attacker)
	{
		// Only L2PcInstance can absorb a soul
		if (!(attacker instanceof L2PcInstance) && !(attacker instanceof L2Summon))
		{
			resetAbsorbList();
			return;
		}
		
		final int maxAbsorbLevel = getAbsorbLevel();
		int minAbsorbLevel = 0;
		
		// If this is not a valid L2Attackable, clears the _absorbersList and just return
		if (maxAbsorbLevel == 0)
		{
			resetAbsorbList();
			return;
		}
		// All boss mobs with maxAbsorbLevel 13 have minAbsorbLevel of 12 else 10
		if (maxAbsorbLevel > 10)
		{
			minAbsorbLevel = maxAbsorbLevel > 12 ? 12 : 10;
		}
		
		// Init some useful vars
		boolean isSuccess = true;
		boolean doLevelup = true;
		final boolean isBossMob = maxAbsorbLevel > 10 ? true : false;
		
		final L2PcInstance killer = (attacker instanceof L2Summon) ? ((L2Summon) attacker).getOwner() : (L2PcInstance) attacker;
		
		// If this mob is a boss, then skip some checkings
		if (!isBossMob)
		{
			// Fail if this L2Attackable isn't absorbed or there's no one in its _absorbersList
			if (!isAbsorbed())
			{
				resetAbsorbList();
				return;
			}
			
			// Fail if the killer isn't in the _absorbersList of this L2Attackable and mob is not boss
			final AbsorberInfo ai = _absorbersList.get(killer);
			if ((ai == null) || (ai.absorber.getObjectId() != killer.getObjectId()))
			{
				isSuccess = false;
			}
			
			// Check if the soul crystal was used when HP of this L2Attackable wasn't higher than half of it
			if ((ai != null) && (ai.absorbedHP > (getMaxHp() / 2.0)))
			{
				isSuccess = false;
			}
			
			if (!isSuccess)
			{
				resetAbsorbList();
				return;
			}
		}
		
		// ********
		String[] crystalNFO = null;
		String crystalNME = "";
		
		final int dice = Rnd.get(100);
		int crystalQTY = 0;
		int crystalLVL = 0;
		int crystalOLD = 0;
		int crystalNEW = 0;
		
		// ********
		// Now we have four choices:
		// 1- The Monster level is too low for the crystal. Nothing happens.
		// 2- Everything is correct, but it failed. Nothing happens. (57.5%)
		// 3- Everything is correct, but it failed. The crystal scatters. A sound event is played. (10%)
		// 4- Everything is correct, the crystal level up. A sound event is played. (32.5%)
		
		List<L2PcInstance> players = new FastList<>();
		
		if (isBossMob && killer.isInParty())
		{
			players = killer.getParty().getPartyMembers();
		}
		else
		{
			players.add(killer);
		}
		
		for (final L2PcInstance player : players)
		{
			if (player == null)
			{
				continue;
			}
			
			crystalQTY = 0;
			
			final L2ItemInstance[] inv = player.getInventory().getItems();
			for (final L2ItemInstance item : inv)
			{
				final int itemId = item.getItemId();
				for (final int id : SoulCrystal.SoulCrystalTable)
				{
					// Find any of the 39 possible crystals.
					if (id == itemId)
					{
						crystalQTY++;
						// Keep count but make sure the player has no more than 1 crystal
						if (crystalQTY > 1)
						{
							isSuccess = false;
							break;
						}
						
						// Validate if the crystal has already leveled
						if ((id != SoulCrystal.RED_NEW_CRYSTAL) && (id != SoulCrystal.GRN_NEW_CYRSTAL) && (id != SoulCrystal.BLU_NEW_CRYSTAL))
						{
							try
							{
								if (item.getItem().getName().contains("Grade"))
								{
									// Split the name of the crystal into 'name' & 'level'
									crystalNFO = item.getItem().getName().trim().replace(" Grade ", "-").split("-");
									// Set Level to 13
									crystalLVL = 13;
									// Get Name
									crystalNME = crystalNFO[0].toLowerCase();
								}
								else
								{
									// Split the name of the crystal into 'name' & 'level'
									crystalNFO = item.getItem().getName().trim().replace(" Stage ", "").split("-");
									// Get Level
									crystalLVL = Integer.parseInt(crystalNFO[1].trim());
									// Get Name
									crystalNME = crystalNFO[0].toLowerCase();
								}
								// Allocate current and levelup ids' for higher level crystals
								if (crystalLVL > 9)
								{
									for (final int[] element : SoulCrystal.HighSoulConvert)
									{
										// Get the next stage above 10 using array.
										if (id == element[0])
										{
											crystalNEW = element[1];
											break;
										}
									}
								}
								else
								{
									crystalNEW = id + 1;
								}
							}
							catch (final NumberFormatException nfe)
							{
								_log.log(Level.WARNING, "An attempt to identify a soul crystal failed, " + "verify the names have not changed in etcitem " + "table.", nfe);
								
								player.sendMessage("There has been an error handling your soul crystal." + " Please notify your server admin.");
								
								isSuccess = false;
								break;
							}
							catch (final Exception e)
							{
								e.printStackTrace();
								isSuccess = false;
								break;
							}
						}
						else
						{
							crystalNME = item.getItem().getName().toLowerCase().trim();
							crystalNEW = id + 1;
						}
						
						// Done
						crystalOLD = id;
						break;
					}
				}
				if (!isSuccess)
				{
					break;
				}
			}
			
			// If the crystal level is way too high for this mob, say that we can't increase it
			if ((crystalLVL < minAbsorbLevel) || (crystalLVL >= maxAbsorbLevel))
			{
				doLevelup = false;
			}
			
			// The player doesn't have any crystals with him get to the next player.
			if ((crystalQTY < 1) || (crystalQTY > 1) || !isSuccess || !doLevelup)
			{
				// Too many crystals in inventory.
				if (crystalQTY > 1)
				{
					player.sendPacket(new SystemMessage(SystemMessage.SOUL_CRYSTAL_ABSORBING_FAILED_RESONATION));
				}
				// The soul crystal stage of the player is way too high
				else if (!doLevelup && (crystalQTY > 0))
				{
					player.sendPacket(new SystemMessage(SystemMessage.SOUL_CRYSTAL_ABSORBING_REFUSED));
				}
				
				crystalQTY = 0;
				continue;
			}
			
			// Ember and Anakazel(78) are not 100% success rate and each individual
			// member of the party has a failure rate on leveling.
			if (isBossMob && ((getNpcId() == 10319) || (getNpcId() == 10338)))
			{
				doLevelup = false;
			}
			
			// If succeeds or it is a boss mob, level up the crystal.
			if ((isBossMob && doLevelup) || (dice <= SoulCrystal.LEVEL_CHANCE))
			{
				// Give staged crystal
				exchangeCrystal(player, crystalOLD, crystalNEW, false);
			}
			// If true and not a boss mob, break the crystal.
			else if (!isBossMob && (dice >= (100.0 - SoulCrystal.BREAK_CHANCE)))
			{
				// Remove current crystal and give a broken open.
				if (crystalNME.startsWith("red"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.RED_BROKEN_CRYSTAL, true);
				}
				else if (crystalNME.startsWith("gre"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.GRN_BROKEN_CYRSTAL, true);
				}
				else if (crystalNME.startsWith("blu"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.BLU_BROKEN_CRYSTAL, true);
				}
				
				resetAbsorbList();
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessage.SOUL_CRYSTAL_ABSORBING_FAILED));
			}
		}
	}
	
	private void exchangeCrystal(L2PcInstance player, int takeid, int giveid, boolean broke)
	{
		L2ItemInstance Item = player.getInventory().destroyItemByItemId("SoulCrystal", takeid, 1, player, this);
		if (Item != null)
		{
			// Prepare inventory update packet
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addRemovedItem(Item);
			
			// Add new crystal to the killer's inventory
			Item = player.getInventory().addItem("SoulCrystal", giveid, 1, player, this);
			playerIU.addItem(Item);
			
			// Send a sound event and text message to the player
			if (broke)
			{
				player.sendPacket(new SystemMessage(SystemMessage.SOUL_CRYSTAL_BROKE));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessage.SOUL_CRYSTAL_ABSORBING_SUCCEEDED));
			}
			
			// Send system message
			final SystemMessage sms = new SystemMessage(SystemMessage.EARNED_ITEM);
			sms.addItemName(giveid);
			player.sendPacket(sms);
			
			// Send inventory update packet
			player.sendPacket(playerIU);
		}
	}
	
	private void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}
	
	/**
	 * Calculate the Experience and SP to distribute to attacker (L2PcInstance, L2SummonInstance or L2Party) of the L2Attackable.<BR>
	 * <BR>
	 * @param diff The difference of level between attacker (L2PcInstance, L2SummonInstance or L2Party) and the L2Attackable
	 * @param damage The damages given by the attacker (L2PcInstance, L2SummonInstance or L2Party)
	 * @return
	 */
	private int[] calculateExpAndSp(int diff, int damage)
	{
		double xp;
		double sp;
		
		if (diff < -5)
		{
			diff = -5; // makes possible to use ALT_GAME_EXPONENT configuration
		}
		xp = ((double) getExpReward() * damage) / getMaxHp();
		if (Config.ALT_GAME_EXPONENT_XP != 0)
		{
			xp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_XP);
		}
		
		sp = ((double) getSpReward() * damage) / getMaxHp();
		if (Config.ALT_GAME_EXPONENT_SP != 0)
		{
			sp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_SP);
		}
		
		if ((Config.ALT_GAME_EXPONENT_XP == 0) && (Config.ALT_GAME_EXPONENT_SP == 0))
		{
			if (diff > 5)
			{
				final double pow = Math.pow((double) 5 / 6, diff - 5);
				xp = xp * pow;
				sp = sp * pow;
			}
			
			if (xp <= 0)
			{
				xp = 0;
				sp = 0;
			}
			else if (sp <= 0)
			{
				sp = 0;
			}
		}
		
		final int[] tmp =
		{
			(int) xp,
			(int) sp
		};
		
		return tmp;
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
		
		// Return the rounded ammount of exp points to be added to the player's normal exp reward
		final long bonusOverhit = Math.round(overhitExp);
		return bonusOverhit;
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
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		// Clear mob spoil,seed
		setSpoil(false);
		
		// Clear all aggro char from list
		clearAggroList();
		// Clear Harvester Reward List
		_harvestItems = null;
		// Clear mod Seeded stat
		setSeeded(false);
		// Clear overhit value
		overhitEnabled(false);
		
		_sweepItems = null;
		resetAbsorbList();
		
		setWalking();
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		if (!isInActiveRegion())
		{
			if (this instanceof L2SiegeGuardInstance)
			{
				((L2SiegeGuardAI) getAI()).stopAITask();
			}
			else
			{
				((L2AttackableAI) getAI()).stopAITask();
			}
		}
	}
	
	public void setSeeded()
	{
		if ((_seedType != 0) && (_seeder != null))
		{
			setSeeded(_seedType, _seeder.getLevel());
		}
	}
	
	public void setSeeded(int id, L2PcInstance seeder)
	{
		if (!_seeded)
		{
			_seedType = id;
			_seeder = seeder;
		}
	}
	
	public void setSeeded(int id, int seederLvl)
	{
		_seeded = true;
		_seedType = id;
		int count = 1;
		
		final Map<Integer, L2Skill> skills = getTemplate().getSkills();
		if (skills != null)
		{
			for (final int skillId : skills.keySet())
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
		}
		
		final int diff = (getLevel() - (L2Manor.getInstance().getSeedLevel(_seedType) - 5));
		
		// hi-lvl mobs bonus
		if (diff > 0)
		{
			count += diff;
		}
		
		final FastList<RewardItem> harvested = new FastList<>();
		
		harvested.add(new RewardItem(L2Manor.getInstance().getCropType(_seedType), count * Config.RATE_DROP_MANOR));
		
		_harvestItems = harvested.toArray(new RewardItem[harvested.size()]);
	}
	
	public void setSeeded(boolean seeded)
	{
		_seeded = seeded;
	}
	
	public L2PcInstance getSeeder()
	{
		return _seeder;
	}
	
	public int getSeedType()
	{
		return _seedType;
	}
	
	public boolean isSeeded()
	{
		return _seeded;
	}
	
	private int getAbsorbLevel()
	{
		return getTemplate().absorb_level;
	}
	
	/**
	 * Check if the server allows Random Animation.<BR>
	 * <BR>
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_MONSTER_ANIMATION > 0) && !(this instanceof L2GrandBossInstance));
	}
	
	public void seeSpell(L2PcInstance caster, L2Object[] targets, L2Skill skill)
	{
		final int actorLevel = caster.getLevel();
		double divisor = 0;
		
		if (actorLevel < 10)
		{
			divisor = 15;
		}
		else if ((actorLevel > 9) && (actorLevel < 20))
		{
			divisor = 11.5;
		}
		else if ((actorLevel > 19) && (actorLevel < 30))
		{
			divisor = 8.5;
		}
		else if ((actorLevel > 29) && (actorLevel < 40))
		{
			divisor = 6;
		}
		else if ((actorLevel > 39) && (actorLevel < 50))
		{
			divisor = 4;
		}
		else if ((actorLevel > 49) && (actorLevel < 60))
		{
			divisor = 2.5;
		}
		else if ((actorLevel > 59) && (actorLevel < 70))
		{
			divisor = 1.5;
		}
		else if (actorLevel > 69)
		{
			divisor = 1;
		}
		
		final L2Object npcTarget = getTarget();
		for (final L2Object target : targets)
		{
			if (!(target instanceof L2Character))
			{
				continue;
			}
			
			final L2Character activeTarget = (L2Character) target;
			if ((npcTarget == activeTarget) || (activeTarget == this))
			{
				int hate = 0;
				
				// Calculate hate depending on skill type
				switch (skill.getSkillType())
				{
					case HEAL:
					case HEAL_PERCENT:
					case MANAHEAL:
					case BALANCE_LIFE:
					{
						if (activeTarget.getLastHealAmount() > (getMaxHp() / 5))
						{
							activeTarget.setLastHealAmount((getMaxHp() / 5));
						}
						
						hate = (int) (activeTarget.getLastHealAmount() / divisor);
						break;
					}
					case BUFF:
					case HOT:
					case REFLECT:
						hate = (int) ((skill.getLevel() * caster.getStat().getMpConsume(skill)) / divisor);
						break;
				}
				
				// Add extra hate if target is party member
				if ((caster != activeTarget) && (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_PARTY))
				{
					if ((getMaxHp() / 3) < (int) (((getHating(activeTarget) - getHating(caster)) + 800) / divisor))
					{
						hate += (getMaxHp() / 3);
					}
					else
					{
						hate += (int) (((getHating(activeTarget) - getHating(caster)) + 800) / divisor);
					}
				}
				
				// finally apply hate
				addDamageHate(caster, 0, hate);
			}
		}
	}
}