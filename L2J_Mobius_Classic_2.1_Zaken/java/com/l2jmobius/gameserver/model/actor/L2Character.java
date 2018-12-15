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

import static com.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.l2jmobius.Config;
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.commons.util.EmptyQueue;
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.ai.CtrlEvent;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.ai.L2AttackableAI;
import com.l2jmobius.gameserver.ai.L2CharacterAI;
import com.l2jmobius.gameserver.data.xml.impl.CategoryData;
import com.l2jmobius.gameserver.data.xml.impl.SkillData;
import com.l2jmobius.gameserver.data.xml.impl.TransformData;
import com.l2jmobius.gameserver.enums.AttributeType;
import com.l2jmobius.gameserver.enums.BasicProperty;
import com.l2jmobius.gameserver.enums.CategoryType;
import com.l2jmobius.gameserver.enums.InstanceType;
import com.l2jmobius.gameserver.enums.ItemSkillType;
import com.l2jmobius.gameserver.enums.Race;
import com.l2jmobius.gameserver.enums.ShotType;
import com.l2jmobius.gameserver.enums.StatusUpdateType;
import com.l2jmobius.gameserver.enums.Team;
import com.l2jmobius.gameserver.enums.UserInfoType;
import com.l2jmobius.gameserver.geoengine.GeoEngine;
import com.l2jmobius.gameserver.idfactory.IdFactory;
import com.l2jmobius.gameserver.instancemanager.MapRegionManager;
import com.l2jmobius.gameserver.instancemanager.QuestManager;
import com.l2jmobius.gameserver.instancemanager.TimersManager;
import com.l2jmobius.gameserver.instancemanager.ZoneManager;
import com.l2jmobius.gameserver.model.CharEffectList;
import com.l2jmobius.gameserver.model.CreatureContainer;
import com.l2jmobius.gameserver.model.Hit;
import com.l2jmobius.gameserver.model.L2AccessLevel;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Party;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.L2WorldRegion;
import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.PcCondOverride;
import com.l2jmobius.gameserver.model.TeleportWhereType;
import com.l2jmobius.gameserver.model.TimeStamp;
import com.l2jmobius.gameserver.model.actor.instance.FriendlyNpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2TrapInstance;
import com.l2jmobius.gameserver.model.actor.stat.CharStat;
import com.l2jmobius.gameserver.model.actor.status.CharStatus;
import com.l2jmobius.gameserver.model.actor.tasks.character.NotifyAITask;
import com.l2jmobius.gameserver.model.actor.templates.L2CharTemplate;
import com.l2jmobius.gameserver.model.actor.transform.Transform;
import com.l2jmobius.gameserver.model.effects.EffectFlag;
import com.l2jmobius.gameserver.model.events.Containers;
import com.l2jmobius.gameserver.model.events.EventDispatcher;
import com.l2jmobius.gameserver.model.events.EventType;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureAttack;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureAttackAvoid;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureAttacked;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureDamageDealt;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureDamageReceived;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureDeath;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureKilled;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureTeleport;
import com.l2jmobius.gameserver.model.events.impl.character.OnCreatureTeleported;
import com.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import com.l2jmobius.gameserver.model.events.returns.DamageReturn;
import com.l2jmobius.gameserver.model.events.returns.LocationReturn;
import com.l2jmobius.gameserver.model.holders.IgnoreSkillHolder;
import com.l2jmobius.gameserver.model.holders.SkillHolder;
import com.l2jmobius.gameserver.model.instancezone.Instance;
import com.l2jmobius.gameserver.model.interfaces.IDeletable;
import com.l2jmobius.gameserver.model.interfaces.ILocational;
import com.l2jmobius.gameserver.model.interfaces.ISkillsHolder;
import com.l2jmobius.gameserver.model.itemcontainer.Inventory;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.model.items.L2Weapon;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.items.type.EtcItemType;
import com.l2jmobius.gameserver.model.items.type.WeaponType;
import com.l2jmobius.gameserver.model.options.OptionsSkillHolder;
import com.l2jmobius.gameserver.model.options.OptionsSkillType;
import com.l2jmobius.gameserver.model.skills.AbnormalType;
import com.l2jmobius.gameserver.model.skills.BuffInfo;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.model.skills.SkillCaster;
import com.l2jmobius.gameserver.model.skills.SkillCastingType;
import com.l2jmobius.gameserver.model.skills.SkillChannelized;
import com.l2jmobius.gameserver.model.skills.SkillChannelizer;
import com.l2jmobius.gameserver.model.stats.BaseStats;
import com.l2jmobius.gameserver.model.stats.BasicPropertyResist;
import com.l2jmobius.gameserver.model.stats.Formulas;
import com.l2jmobius.gameserver.model.stats.MoveType;
import com.l2jmobius.gameserver.model.stats.Stats;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.model.zone.ZoneRegion;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.Attack;
import com.l2jmobius.gameserver.network.serverpackets.ChangeMoveType;
import com.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import com.l2jmobius.gameserver.network.serverpackets.ExTeleportToLocationActivate;
import com.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import com.l2jmobius.gameserver.network.serverpackets.IClientOutgoingPacket;
import com.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import com.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import com.l2jmobius.gameserver.network.serverpackets.Revive;
import com.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import com.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import com.l2jmobius.gameserver.network.serverpackets.SocialAction;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.StopMove;
import com.l2jmobius.gameserver.network.serverpackets.TeleportToLocation;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;
import com.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;
import com.l2jmobius.gameserver.util.Util;

/**
 * Mother class of all character objects of the world (PC, NPC...)<br>
 * L2Character:<br>
 * <ul>
 * <li>L2DoorInstance</li>
 * <li>L2Playable</li>
 * <li>L2Npc</li>
 * <li>L2StaticObjectInstance</li>
 * <li>L2Trap</li>
 * <li>L2Vehicle</li>
 * </ul>
 * <br>
 * <b>Concept of L2CharTemplate:</b><br>
 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
 * All of those properties are stored in a different template for each type of L2Character.<br>
 * Each template is loaded once in the server cache memory (reduce memory use).<br>
 * When a new instance of L2Character is spawned, server just create a link between the instance and the template.<br>
 * This link is stored in {@link #_template}
 * @version $Revision: 1.53.2.45.2.34 $ $Date: 2005/04/11 10:06:08 $
 */
public abstract class L2Character extends L2Object implements ISkillsHolder, IDeletable
{
	public static final Logger LOGGER = Logger.getLogger(L2Character.class.getName());
	
	private volatile Set<WeakReference<L2Character>> _attackByList;
	
	private boolean _isDead = false;
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false; // the char is carrying too much
	private boolean _isPendingRevive = false;
	private boolean _isRunning = isPlayer();
	protected boolean _showSummonAnimation = false;
	protected boolean _isTeleporting = false;
	private boolean _isInvul = false;
	private boolean _isUndying = false;
	private boolean _isFlying = false;
	
	private boolean _blockActions = false;
	private volatile Map<Integer, AtomicInteger> _blockActionsAllowedSkills;
	
	private CharStat _stat;
	private CharStatus _status;
	private L2CharTemplate _template; // The link on the L2CharTemplate object containing generic and static properties of this L2Character type (ex : Max HP, Speed...)
	private String _title;
	
	public static final double MAX_HP_BAR_PX = 352.0;
	
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	
	private int _reputation = 0;
	
	/** Map containing all skills of this character. */
	private final Map<Integer, Skill> _skills = new ConcurrentSkipListMap<>();
	/** Map containing the skill reuse time stamps. */
	private volatile Map<Long, TimeStamp> _reuseTimeStampsSkills = null;
	/** Map containing the item reuse time stamps. */
	private volatile Map<Integer, TimeStamp> _reuseTimeStampsItems = null;
	/** Map containing all the disabled skills. */
	private volatile Map<Long, Long> _disabledSkills = null;
	private boolean _allSkillsDisabled;
	
	private final byte[] _zones = new byte[ZoneId.getZoneCount()];
	protected byte _zoneValidateCounter = 4;
	
	private final StampedLock _attackLock = new StampedLock();
	
	private Team _team = Team.NONE;
	
	protected long _exceptions = 0;
	
	private boolean _lethalable = true;
	
	private volatile Map<Integer, OptionsSkillHolder> _triggerSkills;
	
	private volatile Map<Integer, IgnoreSkillHolder> _ignoreSkillEffects;
	/** Creatures effect list. */
	private final CharEffectList _effectList = new CharEffectList(this);
	/** The character that summons this character. */
	private L2Character _summoner = null;
	
	/** Map of summoned NPCs by this creature. */
	private volatile Map<Integer, L2Npc> _summonedNpcs = null;
	
	private SkillChannelizer _channelizer = null;
	
	private SkillChannelized _channelized = null;
	
	private Optional<Transform> _transform = Optional.empty();
	
	/** Movement data of this L2Character */
	protected MoveData _move;
	private boolean _cursorKeyMovement = false;
	private boolean _cursorKeyMovementActive = true;
	
	/** This creature's target. */
	private L2Object _target;
	
	// set by the start of attack, in game ticks
	private volatile long _attackEndTime;
	private volatile long _disableRangedAttackEndTime;
	
	private volatile L2CharacterAI _ai = null;
	
	/** Future Skill Cast */
	protected Map<SkillCastingType, SkillCaster> _skillCasters = new ConcurrentHashMap<>();
	
	private final AtomicInteger _abnormalShieldBlocks = new AtomicInteger();
	
	private final Map<Integer, Integer> _knownRelations = new ConcurrentHashMap<>();
	
	private volatile CreatureContainer _seenCreatures;
	
	private final Map<StatusUpdateType, Integer> _statusUpdates = new ConcurrentHashMap<>();
	
	/** A map holding info about basic property mesmerizing system. */
	private volatile Map<BasicProperty, BasicPropertyResist> _basicPropertyResists;
	
	private ScheduledFuture<?> _hitTask = null;
	/** A set containing the shot types currently charged. */
	private Set<ShotType> _chargedShots = EnumSet.noneOf(ShotType.class);
	
	/** A list containing the dropped items of this fake player. */
	private final List<L2ItemInstance> _fakePlayerDrops = new CopyOnWriteArrayList<>();
	
	/**
	 * Creates a creature.
	 * @param template the creature template
	 */
	public L2Character(L2CharTemplate template)
	{
		this(IdFactory.getInstance().getNextId(), template);
	}
	
	/**
	 * Constructor of L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
	 * All of those properties are stored in a different template for each type of L2Character. Each template is loaded once in the server cache memory (reduce memory use).<br>
	 * When a new instance of L2Character is spawned, server just create a link between the instance and the template This link is stored in <B>_template</B><br>
	 * <B><U> Actions</U>:</B>
	 * <ul>
	 * <li>Set the _template of the L2Character</li>
	 * <li>Set _overloaded to false (the character can take more items)</li>
	 * <li>If L2Character is a L2NPCInstance, copy skills from template to object</li>
	 * <li>If L2Character is a L2NPCInstance, link _calculators to NPC_STD_CALCULATOR</li>
	 * <li>If L2Character is NOT a L2NPCInstance, create an empty _skills slot</li>
	 * <li>If L2Character is a L2PcInstance or L2Summon, copy basic Calculator set to object</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the object
	 */
	public L2Character(int objectId, L2CharTemplate template)
	{
		super(objectId);
		if (template == null)
		{
			throw new NullPointerException("Template is null!");
		}
		
		setInstanceType(InstanceType.L2Character);
		// Set its template to the new L2Character
		_template = template;
		initCharStat();
		initCharStatus();
		
		if (isNpc())
		{
			// Copy the skills of the L2NPCInstance from its template to the L2Character Instance
			// The skills list can be affected by spell effects so it's necessary to make a copy
			// to avoid that a spell affecting a L2NpcInstance, affects others L2NPCInstance of the same type too.
			for (Skill skill : template.getSkills().values())
			{
				addSkill(skill);
			}
		}
		else if (isSummon())
		{
			// Copy the skills of the L2Summon from its template to the L2Character Instance
			// The skills list can be affected by spell effects so it's necessary to make a copy
			// to avoid that a spell affecting a L2Summon, affects others L2Summon of the same type too.
			for (Skill skill : template.getSkills().values())
			{
				addSkill(skill);
			}
		}
		
		setIsInvul(true);
	}
	
	public final CharEffectList getEffectList()
	{
		return _effectList;
	}
	
	/**
	 * @return character inventory, default null, overridden in L2Playable types and in L2NPcInstance
	 */
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		// Default: NPCs consume virtual items for their skills
		// TODO: should be logged if even happens.. should be false
		return true;
	}
	
	/**
	 * Check if the character is in the given zone Id.
	 * @param zone the zone Id to check
	 * @return {code true} if the character is in that zone
	 */
	@Override
	public final boolean isInsideZone(ZoneId zone)
	{
		final Instance instance = getInstanceWorld();
		switch (zone)
		{
			case PVP:
			{
				if ((instance != null) && instance.isPvP())
				{
					return true;
				}
				return (_zones[ZoneId.PVP.ordinal()] > 0) && (_zones[ZoneId.PEACE.ordinal()] == 0);
			}
			case PEACE:
			{
				if ((instance != null) && instance.isPvP())
				{
					return false;
				}
			}
		}
		return _zones[zone.ordinal()] > 0;
	}
	
	/**
	 * @param zone
	 * @param state
	 */
	public final void setInsideZone(ZoneId zone, boolean state)
	{
		synchronized (_zones)
		{
			if (state)
			{
				_zones[zone.ordinal()]++;
			}
			else if (_zones[zone.ordinal()] > 0)
			{
				_zones[zone.ordinal()]--;
			}
		}
	}
	
	/**
	 * @return {@code true} if this creature is transformed including stance transformation {@code false} otherwise.
	 */
	public boolean isTransformed()
	{
		return _transform.isPresent();
	}
	
	/**
	 * @param filter any conditions to be checked for the transformation, {@code null} otherwise.
	 * @return {@code true} if this creature is transformed under the given filter conditions, {@code false} otherwise.
	 */
	public boolean checkTransformed(Predicate<Transform> filter)
	{
		return _transform.filter(filter).isPresent();
	}
	
	/**
	 * Tries to transform this creature with the specified template id.
	 * @param id the id of the transformation template
	 * @param addSkills {@code true} if skills of this transformation template should be added, {@code false} otherwise.
	 * @return {@code true} if template is found and transformation is done, {@code false} otherwise.
	 */
	public boolean transform(int id, boolean addSkills)
	{
		final Transform transform = TransformData.getInstance().getTransform(id);
		if (transform != null)
		{
			transform(transform, addSkills);
			return true;
		}
		
		return false;
	}
	
	public void transform(Transform transformation, boolean addSkills)
	{
		if (!Config.ALLOW_MOUNTS_DURING_SIEGE && transformation.isRiding() && isInsideZone(ZoneId.SIEGE))
		{
			return;
		}
		
		_transform = Optional.of(transformation);
		transformation.onTransform(this, addSkills);
	}
	
	public void untransform()
	{
		_transform.ifPresent(t -> t.onUntransform(this));
		_transform = Optional.empty();
		
		// Mobius: Tempfix for untransform not showing stats.
		// Resend UserInfo to player.
		if (isPlayer())
		{
			sendPacket(new UserInfo((L2PcInstance) this));
		}
	}
	
	public Optional<Transform> getTransformation()
	{
		return _transform;
	}
	
	/**
	 * This returns the transformation Id of the current transformation. For example, if a player is transformed as a Buffalo, and then picks up the Zariche, the transform Id returned will be that of the Zariche, and NOT the Buffalo.
	 * @return Transformation Id
	 */
	public int getTransformationId()
	{
		return _transform.map(Transform::getId).orElse(0);
	}
	
	public int getTransformationDisplayId()
	{
		return _transform.filter(transform -> !transform.isStance()).map(Transform::getDisplayId).orElse(0);
	}
	
	public double getCollisionRadius()
	{
		final double defaultCollisionRadius = _template.getCollisionRadius();
		return _transform.map(transform -> transform.getCollisionRadius(this, defaultCollisionRadius)).orElse(defaultCollisionRadius);
	}
	
	public double getCollisionHeight()
	{
		final double defaultCollisionHeight = _template.getCollisionHeight();
		return _transform.map(transform -> transform.getCollisionHeight(this, defaultCollisionHeight)).orElse(defaultCollisionHeight);
	}
	
	/**
	 * This will return true if the player is GM,<br>
	 * but if the player is not GM it will return false.
	 * @return GM status
	 */
	public boolean isGM()
	{
		return false;
	}
	
	/**
	 * Overridden in L2PcInstance.
	 * @return the access level.
	 */
	public L2AccessLevel getAccessLevel()
	{
		return null;
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateIncCheck = _stat.getMaxHp();
		_hpUpdateInterval = _hpUpdateIncCheck / MAX_HP_BAR_PX;
		_hpUpdateDecCheck = _hpUpdateIncCheck - _hpUpdateInterval;
	}
	
	/**
	 * Remove the L2Character from the world when the decay task is launched.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT>
	 */
	public void onDecay()
	{
		decayMe();
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.removeFromZones(this);
		}
		
		// Removes itself from the summoned list.
		if ((_summoner != null))
		{
			_summoner.removeSummonedNpc(getObjectId());
		}
		
		// Stop on creature see task and clear the data
		if (_seenCreatures != null)
		{
			_seenCreatures.stop();
			_seenCreatures.reset();
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone(true);
		
		// restart task
		if (_seenCreatures != null)
		{
			_seenCreatures.start();
		}
	}
	
	public synchronized void onTeleported()
	{
		if (!_isTeleporting)
		{
			return;
		}
		spawnMe(getX(), getY(), getZ());
		setIsTeleporting(false);
		EventDispatcher.getInstance().notifyEventAsync(new OnCreatureTeleported(this), this);
	}
	
	/**
	 * Add L2Character instance that is attacking to the attacker list.
	 * @param player The L2Character that attacks this one
	 */
	public void addAttackerToAttackByList(L2Character player)
	{
		// DS: moved to L2Attackable
	}
	
	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<br>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet
	 * @param mov
	 */
	public void broadcastPacket(IClientOutgoingPacket mov)
	{
		L2World.getInstance().forEachVisibleObject(this, L2PcInstance.class, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the radius (max knownlist radius) from the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<br>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet
	 * @param mov
	 * @param radiusInKnownlist
	 */
	public void broadcastPacket(IClientOutgoingPacket mov, int radiusInKnownlist)
	{
		L2World.getInstance().forEachVisibleObjectInRange(this, L2PcInstance.class, radiusInKnownlist, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(mov);
			}
		});
	}
	
	/**
	 * @return true if hp update should be done, false if not
	 */
	protected boolean needHpUpdate()
	{
		final double currentHp = _status.getCurrentHp();
		final double maxHp = _stat.getMaxHp();
		
		if ((currentHp <= 1.0) || (maxHp < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentHp < _hpUpdateDecCheck) || (Math.abs(currentHp - _hpUpdateDecCheck) <= 1e-6) || (currentHp > _hpUpdateIncCheck) || (Math.abs(currentHp - _hpUpdateIncCheck) <= 1e-6))
		{
			if (Math.abs(currentHp - maxHp) <= 1e-6)
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	public final void broadcastStatusUpdate()
	{
		broadcastStatusUpdate(null);
	}
	
	/**
	 * Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Create the Server->Client packet StatusUpdate with current HP and MP</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all L2Character called _statusListener that must be informed of HP/MP updates of this L2Character</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: This method DOESN'T SEND CP information</B></FONT>
	 * @param caster TODO
	 */
	public void broadcastStatusUpdate(L2Character caster)
	{
		final StatusUpdate su = new StatusUpdate(this);
		if (caster != null)
		{
			su.addCaster(caster);
		}
		
		// HP
		su.addUpdate(StatusUpdateType.MAX_HP, _stat.getMaxHp());
		su.addUpdate(StatusUpdateType.CUR_HP, (int) _status.getCurrentHp());
		
		// MP
		computeStatusUpdate(su, StatusUpdateType.MAX_MP);
		computeStatusUpdate(su, StatusUpdateType.CUR_MP);
		
		broadcastPacket(su);
	}
	
	/**
	 * @param text
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}
	
	/**
	 * Teleport a L2Character and its pet if necessary.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Stop the movement of the L2Character</li>
	 * <li>Set the x,y,z position of the L2Object and if necessary modify its _worldRegion</li>
	 * <li>Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in its _KnownPlayers</li>
	 * <li>Modify the position of the pet if necessary</li>
	 * </ul>
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param instance
	 */
	public void teleToLocation(int x, int y, int z, int heading, Instance instance)
	{
		final LocationReturn term = EventDispatcher.getInstance().notifyEvent(new OnCreatureTeleport(this, x, y, z, heading, instance), this, LocationReturn.class);
		if (term != null)
		{
			if (term.terminate())
			{
				return;
			}
			else if (term.overrideLocation())
			{
				x = term.getX();
				y = term.getY();
				z = term.getZ();
				heading = term.getHeading();
				instance = term.getInstance();
			}
		}
		
		// Prepare creature for teleport
		if (_isPendingRevive)
		{
			doRevive();
		}
		
		// Abort any client actions, casting and remove target.
		sendPacket(ActionFailed.get(SkillCastingType.NORMAL));
		sendPacket(ActionFailed.get(SkillCastingType.NORMAL_SECOND));
		if (isMoving())
		{
			stopMove(null);
		}
		abortCast();
		setTarget(null);
		
		setIsTeleporting(true);
		
		getAI().setIntention(AI_INTENTION_ACTIVE);
		
		// Adjust position a bit
		z += 5;
		
		// Send teleport packet to player and visible players
		broadcastPacket(new TeleportToLocation(this, x, y, z, heading));
		
		// remove the object from its old location
		decayMe();
		
		// Change instance world
		if (getInstanceWorld() != instance)
		{
			setInstance(instance);
		}
		
		// Set the x,y,z position of the L2Object and if necessary modify its _worldRegion
		setXYZ(x, y, z);
		
		// temporary fix for heading on teleport
		if (heading != 0)
		{
			setHeading(heading);
		}
		
		// Send teleport finished packet to player
		sendPacket(new ExTeleportToLocationActivate(this));
		
		// allow recall of the detached characters
		if (!isPlayer() || ((getActingPlayer().getClient() != null) && getActingPlayer().getClient().isDetached()))
		{
			onTeleported();
		}
		revalidateZone(true);
	}
	
	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, 0, getInstanceWorld());
	}
	
	public void teleToLocation(int x, int y, int z, Instance instance)
	{
		teleToLocation(x, y, z, 0, instance);
	}
	
	public void teleToLocation(int x, int y, int z, int heading)
	{
		teleToLocation(x, y, z, heading, getInstanceWorld());
	}
	
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset)
	{
		teleToLocation(x, y, z, heading, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0, getInstanceWorld());
	}
	
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset, Instance instance)
	{
		teleToLocation(x, y, z, heading, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0, instance);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int randomOffset)
	{
		teleToLocation(x, y, z, heading, randomOffset, getInstanceWorld());
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int randomOffset, Instance instance)
	{
		if (Config.OFFSET_ON_TELEPORT_ENABLED && (randomOffset > 0))
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		teleToLocation(x, y, z, heading, instance);
	}
	
	public void teleToLocation(ILocational loc)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading());
	}
	
	public void teleToLocation(ILocational loc, Instance instance)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), instance);
	}
	
	public void teleToLocation(ILocational loc, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset);
	}
	
	public void teleToLocation(ILocational loc, int randomOffset, Instance instance)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, instance);
	}
	
	public void teleToLocation(ILocational loc, boolean randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(ILocational loc, boolean randomOffset, Instance instance)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, instance);
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(teleportWhere, getInstanceWorld());
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere, Instance instance)
	{
		teleToLocation(MapRegionManager.getInstance().getTeleToLocation(this, teleportWhere), true, instance);
	}
	
	/**
	 * Launch a physical attack against a target (Simple, Bow, Pole or Dual).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Get the active weapon (always equipped in the right hand)</li>
	 * <li>If weapon is a bow, check for arrows, MP and bow re-use delay (if necessary, equip the L2PcInstance with arrows in left hand)</li>
	 * <li>If weapon is a bow, consume MP and set the new period of bow non re-use</li>
	 * <li>Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)</li>
	 * <li>Select the type of attack to start (Simple, Bow, Pole or Dual) and verify if SoulShot are charged then start calculation</li>
	 * <li>If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character</li>
	 * <li>Notify AI with EVT_READY_TO_ACT</li>
	 * </ul>
	 * @param target The L2Character targeted
	 */
	public void doAutoAttack(L2Character target)
	{
		final long stamp = _attackLock.tryWriteLock();
		if (stamp == 0)
		{
			return;
		}
		try
		{
			if ((target == null) || isAttackingDisabled() || !target.isTargetable())
			{
				return;
			}
			
			if (!isAlikeDead())
			{
				if ((isNpc() && target.isAlikeDead()) || !isInSurroundingRegion(target))
				{
					getAI().setIntention(AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if (isPlayer())
				{
					if (target.isDead())
					{
						getAI().setIntention(AI_INTENTION_ACTIVE);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
				
				if (checkTransformed(transform -> !transform.canAttack()))
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			// Get the active weapon item corresponding to the active weapon instance (always equipped in the right hand)
			final L2Weapon weaponItem = getActiveWeaponItem();
			final WeaponType weaponType = getAttackType();
			
			if (getActingPlayer() != null)
			{
				if (getActingPlayer().inObserverMode())
				{
					sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if ((target.getActingPlayer() != null) && (getActingPlayer().getSiegeState() > 0) && isInsideZone(ZoneId.SIEGE) && (target.getActingPlayer().getSiegeState() == getActingPlayer().getSiegeState()) && (target.getActingPlayer() != this) && (target.getActingPlayer().getSiegeSide() == getActingPlayer().getSiegeSide()))
				{
					sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Checking if target has moved to peace zone
				else if (target.isInsidePeaceZone(getActingPlayer()))
				{
					getAI().setIntention(AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (isInsidePeaceZone(this, target))
			{
				getAI().setIntention(AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			stopEffectsOnAction();
			
			// GeoData Los Check here (or dz > 1000)
			if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// BOW and CROSSBOW checks
			if (weaponItem != null)
			{
				if (!weaponItem.isAttackWeapon() && !isGM())
				{
					if (weaponItem.getItemType() == WeaponType.FISHINGROD)
					{
						sendPacket(SystemMessageId.YOU_LOOK_ODDLY_AT_THE_FISHING_POLE_IN_DISBELIEF_AND_REALIZE_THAT_YOU_CAN_T_ATTACK_ANYTHING_WITH_THIS);
					}
					else
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_PERFORM_ANY_ATTACKS);
					}
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Ranged weapon checks.
				if (weaponItem.getItemType().isRanged())
				{
					// Check if bow delay is still active.
					if (_disableRangedAttackEndTime > System.nanoTime())
					{
						if (isPlayer())
						{
							ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
							sendPacket(ActionFailed.STATIC_PACKET);
						}
						return;
					}
					
					// Check for arrows and MP
					if (isPlayer())
					{
						// Check if there are arrows to use or else cancel the attack.
						if (!checkAndEquipAmmunition(weaponItem.getItemType().isCrossbow() ? EtcItemType.BOLT : EtcItemType.ARROW))
						{
							// Cancel the action because the L2PcInstance have no arrow
							getAI().setIntention(AI_INTENTION_ACTIVE);
							sendPacket(ActionFailed.STATIC_PACKET);
							sendPacket(SystemMessageId.YOU_HAVE_RUN_OUT_OF_ARROWS);
							return;
						}
						
						// Checking if target has moved to peace zone - only for player-bow attacks at the moment
						// Other melee is checked in movement code and for offensive spells a check is done every time
						if (target.isInsidePeaceZone(getActingPlayer()))
						{
							getAI().setIntention(AI_INTENTION_ACTIVE);
							sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_IN_A_PEACEFUL_ZONE);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						// Check if player has enough MP to shoot.
						int mpConsume = weaponItem.getMpConsume();
						if ((weaponItem.getReducedMpConsume() > 0) && (Rnd.get(100) < weaponItem.getReducedMpConsumeChance()))
						{
							mpConsume = weaponItem.getReducedMpConsume();
						}
						mpConsume = isAffected(EffectFlag.CHEAPSHOT) ? 0 : mpConsume;
						if (_status.getCurrentMp() < mpConsume)
						{
							// If L2PcInstance doesn't have enough MP, stop the attack
							ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT), 1000);
							sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						// If L2PcInstance have enough MP, the bow consumes it
						if (mpConsume > 0)
						{
							_status.reduceMp(mpConsume);
						}
					}
				}
			}
			
			// Mobius: Do not move when attack is launched.
			if (isMoving())
			{
				stopMove(getLocation());
			}
			
			final WeaponType attackType = getAttackType();
			final boolean isTwoHanded = (weaponItem != null) && (weaponItem.getBodyPart() == L2Item.SLOT_LR_HAND);
			final int timeAtk = Formulas.calculateTimeBetweenAttacks(_stat.getPAtkSpd());
			final int timeToHit = Formulas.calculateTimeToHit(timeAtk, weaponType, isTwoHanded, false);
			_attackEndTime = System.nanoTime() + (TimeUnit.MILLISECONDS.toNanos(timeAtk));
			
			// Make sure that char is facing selected target
			// also works: setHeading(Util.convertDegreeToClientHeading(Util.calculateAngleFrom(this, target)));
			setHeading(Util.calculateHeadingFrom(this, target));
			
			// Get the Attack Reuse Delay of the L2Weapon
			final Attack attack = generateAttackTargetData(target, weaponItem, attackType);
			boolean crossbow = false;
			switch (attackType)
			{
				case CROSSBOW:
				case TWOHANDCROSSBOW:
				{
					crossbow = true;
				}
				case BOW:
				{
					final int reuse = Formulas.calculateReuseTime(this, weaponItem);
					
					// Consume arrows
					final Inventory inventory = getInventory();
					if (inventory != null)
					{
						inventory.reduceArrowCount(crossbow ? EtcItemType.BOLT : EtcItemType.ARROW);
					}
					
					// Check if the L2Character is a L2PcInstance
					if (isPlayer())
					{
						if (crossbow)
						{
							sendPacket(SystemMessageId.YOUR_CROSSBOW_IS_PREPARING_TO_FIRE);
						}
						
						sendPacket(new SetupGauge(getObjectId(), SetupGauge.RED, reuse));
					}
					
					// Calculate and set the disable delay of the bow in function of the Attack Speed
					_disableRangedAttackEndTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(reuse);
					_hitTask = ThreadPool.schedule(() -> onHitTimeNotDual(weaponItem, attack, timeToHit, timeAtk), timeToHit);
					break;
				}
				case FIST:
				{
					if (!isPlayer())
					{
						_hitTask = ThreadPool.schedule(() -> onHitTimeNotDual(weaponItem, attack, timeToHit, timeAtk), timeToHit);
						break;
					}
				}
				case DUAL:
				case DUALFIST:
				case DUALBLUNT:
				case DUALDAGGER:
				{
					final int timeToHit2 = Formulas.calculateTimeToHit(timeAtk, weaponType, isTwoHanded, true) - timeToHit;
					_hitTask = ThreadPool.schedule(() -> onFirstHitTimeForDual(weaponItem, attack, timeToHit, timeAtk, timeToHit2), timeToHit);
					break;
				}
				default:
				{
					_hitTask = ThreadPool.schedule(() -> onHitTimeNotDual(weaponItem, attack, timeToHit, timeAtk), timeToHit);
					break;
				}
			}
			
			// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
			// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
			if (attack.hasHits())
			{
				broadcastPacket(attack);
			}
			
			// Flag the attacker if it's a L2PcInstance outside a PvP area
			final L2PcInstance player = getActingPlayer();
			if (player != null)
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
				player.updatePvPStatus(target);
			}
			
			if (isFakePlayer() && (target.isPlayable() || target.isFakePlayer()))
			{
				final L2Npc npc = ((L2Npc) this);
				if (!npc.isScriptValue(1))
				{
					npc.setScriptValue(1); // in combat
					broadcastInfo(); // update flag status
					QuestManager.getInstance().getQuest("PvpFlaggingStopTask").notifyEvent("FLAG_CHECK" + npc.getObjectId(), npc, null);
				}
			}
		}
		finally
		{
			_attackLock.unlockWrite(stamp);
		}
	}
	
	private Attack generateAttackTargetData(L2Character target, L2Weapon weapon, WeaponType weaponType)
	{
		final boolean isDual = (WeaponType.DUAL == weaponType) || (WeaponType.DUALBLUNT == weaponType) || (WeaponType.DUALDAGGER == weaponType) || (WeaponType.DUALFIST == weaponType);
		final Attack attack = new Attack(this, target);
		boolean shotConsumed = false;
		
		// Calculate the main target hit.
		Hit hit = generateHit(target, weapon, shotConsumed, isDual);
		attack.addHit(hit);
		shotConsumed = hit.isShotUsed();
		
		// Second hit for the dual attack.
		if (isDual)
		{
			hit = generateHit(target, weapon, shotConsumed, isDual);
			attack.addHit(hit);
			shotConsumed = hit.isShotUsed();
		}
		
		// H5 Changes: without Polearm Mastery (skill 216) max simultaneous attacks is 3 (1 by default + 2 in skill 3599).
		int attackCountMax = (int) _stat.getValue(Stats.ATTACK_COUNT_MAX, 1);
		if ((attackCountMax > 1) && !(_stat.getValue(Stats.PHYSICAL_POLEARM_TARGET_SINGLE, 0) > 0))
		{
			final double headingAngle = Util.convertHeadingToDegree(getHeading());
			final int maxRadius = _stat.getPhysicalAttackRadius();
			final int physicalAttackAngle = _stat.getPhysicalAttackAngle();
			for (L2Character obj : L2World.getInstance().getVisibleObjectsInRange(this, L2Character.class, maxRadius))
			{
				// Skip main target.
				if (obj == target)
				{
					continue;
				}
				
				// Skip dead or fake dead target.
				if (obj.isAlikeDead())
				{
					continue;
				}
				
				// Check if target is auto attackable.
				if (!obj.isAutoAttackable(this))
				{
					continue;
				}
				
				// Check if target is within attack angle.
				if (Math.abs(calculateDirectionTo(obj) - headingAngle) > physicalAttackAngle)
				{
					continue;
				}
				
				// Launch a simple attack against the additional target.
				hit = generateHit(obj, weapon, shotConsumed, false);
				attack.addHit(hit);
				shotConsumed = hit.isShotUsed();
				if (--attackCountMax <= 0)
				{
					break;
				}
			}
		}
		
		return attack;
	}
	
	private Hit generateHit(L2Character target, L2Weapon weapon, boolean shotConsumed, boolean halfDamage)
	{
		int damage = 0;
		byte shld = 0;
		boolean crit = false;
		boolean miss = Formulas.calcHitMiss(this, target);
		if (!shotConsumed)
		{
			shotConsumed = !miss && unchargeShot(ShotType.SOULSHOTS);
		}
		
		int ssGrade = (shotConsumed && (weapon != null)) ? weapon.getItemGrade().ordinal() : 0;
		
		// Check if hit isn't missed
		if (!miss)
		{
			shld = Formulas.calcShldUse(this, target);
			crit = Formulas.calcCrit(_stat.getCriticalHit(), this, target, null);
			damage = (int) Formulas.calcAutoAttackDamage(this, target, shld, crit, shotConsumed);
			if (halfDamage)
			{
				damage /= 2;
			}
		}
		
		return new Hit(target, damage, miss, crit, shld, shotConsumed, ssGrade);
	}
	
	public void doCast(Skill skill)
	{
		doCast(skill, null, false, false);
	}
	
	/**
	 * Manage the casting task (casting and interrupt time, re-use delay...) and display the casting bar and animation on client.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Verify the possibility of the the cast : skill is a spell, caster isn't muted...</li>
	 * <li>Get the list of all targets (ex : area effects) and define the L2Charcater targeted (its stats will be used in calculation)</li>
	 * <li>Calculate the casting time (base + modifier of MAtkSpd), interrupt time and re-use delay</li>
	 * <li>Send a Server->Client packet MagicSkillUser (to display casting animation), a packet SetupGauge (to display casting bar) and a system message</li>
	 * <li>Disable all skills during the casting time (create a task EnableAllSkills)</li>
	 * <li>Disable the skill during the re-use delay (create a task EnableSkill)</li>
	 * <li>Create a task MagicUseTask (that will call method onMagicUseTimer) to launch the Magic Skill at the end of the casting time</li>
	 * </ul>
	 * @param skill The L2Skill to use
	 * @param item the referenced item of this skill cast
	 * @param ctrlPressed if the player has pressed ctrl key during casting, aka force use.
	 * @param shiftPressed if the player has pressed shift key during casting, aka dont move.
	 */
	public synchronized void doCast(Skill skill, L2ItemInstance item, boolean ctrlPressed, boolean shiftPressed)
	{
		// Get proper casting type.
		SkillCastingType castingType = SkillCastingType.NORMAL;
		if (skill.canDoubleCast() && isAffected(EffectFlag.DOUBLE_CAST) && isCastingNow(castingType))
		{
			castingType = SkillCastingType.NORMAL_SECOND;
		}
		
		// Try casting the skill
		final SkillCaster skillCaster = SkillCaster.castSkill(this, _target, skill, item, castingType, ctrlPressed, shiftPressed);
		if ((skillCaster == null) && isPlayer())
		{
			// Skill casting failed, notify player.
			sendPacket(ActionFailed.get(castingType));
			getAI().setIntention(AI_INTENTION_ACTIVE);
		}
	}
	
	/**
	 * Gets the item reuse time stamps map.
	 * @return the item reuse time stamps map
	 */
	public final Map<Integer, TimeStamp> getItemReuseTimeStamps()
	{
		return _reuseTimeStampsItems;
	}
	
	/**
	 * Adds a item reuse time stamp.
	 * @param item the item
	 * @param reuse the reuse
	 */
	public final void addTimeStampItem(L2ItemInstance item, long reuse)
	{
		addTimeStampItem(item, reuse, -1);
	}
	
	/**
	 * Adds a item reuse time stamp.<br>
	 * Used for restoring purposes.
	 * @param item the item
	 * @param reuse the reuse
	 * @param systime the system time
	 */
	public final void addTimeStampItem(L2ItemInstance item, long reuse, long systime)
	{
		if (_reuseTimeStampsItems == null)
		{
			synchronized (this)
			{
				if (_reuseTimeStampsItems == null)
				{
					_reuseTimeStampsItems = new ConcurrentHashMap<>();
				}
			}
		}
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
	}
	
	/**
	 * Gets the item remaining reuse time for a given item object ID.
	 * @param itemObjId the item object ID
	 * @return if the item has a reuse time stamp, the remaining time, otherwise -1
	 */
	public final synchronized long getItemRemainingReuseTime(int itemObjId)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsItems != null) ? _reuseTimeStampsItems.get(itemObjId) : null;
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Gets the item remaining reuse time for a given shared reuse item group.
	 * @param group the shared reuse item group
	 * @return if the shared reuse item group has a reuse time stamp, the remaining time, otherwise -1
	 */
	public final long getReuseDelayOnGroup(int group)
	{
		if ((group > 0) && (_reuseTimeStampsItems != null))
		{
			for (TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if ((ts.getSharedReuseGroup() == group) && ts.hasNotPassed())
				{
					return ts.getRemaining();
				}
			}
		}
		return -1;
	}
	
	/**
	 * Gets the skill reuse time stamps map.
	 * @return the skill reuse time stamps map
	 */
	public final Map<Long, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	/**
	 * Adds the skill reuse time stamp.
	 * @param skill the skill
	 * @param reuse the delay
	 */
	public final void addTimeStamp(Skill skill, long reuse)
	{
		addTimeStamp(skill, reuse, -1);
	}
	
	/**
	 * Adds the skill reuse time stamp.<br>
	 * Used for restoring purposes.
	 * @param skill the skill
	 * @param reuse the reuse
	 * @param systime the system time
	 */
	public final void addTimeStamp(Skill skill, long reuse, long systime)
	{
		if (_reuseTimeStampsSkills == null)
		{
			synchronized (this)
			{
				if (_reuseTimeStampsSkills == null)
				{
					_reuseTimeStampsSkills = new ConcurrentHashMap<>();
				}
			}
		}
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}
	
	/**
	 * Removes a skill reuse time stamp.
	 * @param skill the skill to remove
	 */
	public final synchronized void removeTimeStamp(Skill skill)
	{
		if (_reuseTimeStampsSkills != null)
		{
			_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
		}
	}
	
	/**
	 * Removes all skill reuse time stamps.
	 */
	public final synchronized void resetTimeStamps()
	{
		if (_reuseTimeStampsSkills != null)
		{
			_reuseTimeStampsSkills.clear();
		}
	}
	
	/**
	 * Gets the skill remaining reuse time for a given skill hash code.
	 * @param hashCode the skill hash code
	 * @return if the skill has a reuse time stamp, the remaining time, otherwise -1
	 */
	public final synchronized long getSkillRemainingReuseTime(long hashCode)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsSkills != null) ? _reuseTimeStampsSkills.get(hashCode) : null;
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Verifies if the skill is under reuse time.
	 * @param hashCode the skill hash code
	 * @return {@code true} if the skill is under reuse time, {@code false} otherwise
	 */
	public final synchronized boolean hasSkillReuse(long hashCode)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsSkills != null) ? _reuseTimeStampsSkills.get(hashCode) : null;
		return (reuseStamp != null) && reuseStamp.hasNotPassed();
	}
	
	/**
	 * Gets the skill reuse time stamp.
	 * @param hashCode the skill hash code
	 * @return if the skill has a reuse time stamp, the skill reuse time stamp, otherwise {@code null}
	 */
	public final synchronized TimeStamp getSkillReuseTimeStamp(long hashCode)
	{
		return _reuseTimeStampsSkills != null ? _reuseTimeStampsSkills.get(hashCode) : null;
	}
	
	/**
	 * Gets the disabled skills map.
	 * @return the disabled skills map
	 */
	public Map<Long, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	/**
	 * Enables a skill.
	 * @param skill the skill to enable
	 */
	public void enableSkill(Skill skill)
	{
		if ((skill == null) || (_disabledSkills == null))
		{
			return;
		}
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Disables a skill for a given time.<br>
	 * If delay is lesser or equal than zero, skill will be disabled "forever".
	 * @param skill the skill to disable
	 * @param delay delay in milliseconds
	 */
	public void disableSkill(Skill skill, long delay)
	{
		if (skill == null)
		{
			return;
		}
		
		if (_disabledSkills == null)
		{
			synchronized (this)
			{
				if (_disabledSkills == null)
				{
					_disabledSkills = new ConcurrentHashMap<>();
				}
			}
		}
		
		_disabledSkills.put(skill.getReuseHashCode(), delay > 0 ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	/**
	 * Removes all the disabled skills.
	 */
	public final synchronized void resetDisabledSkills()
	{
		if (_disabledSkills != null)
		{
			_disabledSkills.clear();
		}
	}
	
	/**
	 * Verifies if the skill is disabled.
	 * @param skill the skill
	 * @return {@code true} if the skill is disabled, {@code false} otherwise
	 */
	public boolean isSkillDisabled(Skill skill)
	{
		if (skill == null)
		{
			return false;
		}
		
		if (_allSkillsDisabled || (!skill.canCastWhileDisabled() && isAllSkillsDisabled()))
		{
			return true;
		}
		
		if (isAffected(EffectFlag.CONDITIONAL_BLOCK_ACTIONS) && !isBlockedActionsAllowedSkill(skill))
		{
			return true;
		}
		
		final long hashCode = skill.getReuseHashCode();
		if (hasSkillReuse(hashCode))
		{
			return true;
		}
		
		if (_disabledSkills == null)
		{
			return false;
		}
		final Long stamp = _disabledSkills.get(hashCode);
		if (stamp == null)
		{
			return false;
		}
		if (stamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Disables all skills.
	 */
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enables all skills, except those under reuse time or previously disabled.
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	/**
	 * Kill the L2Character.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the L2Character</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform</li>
	 * <li>Notify L2Character AI</li>
	 * </ul>
	 * @param killer The L2Character who killed it
	 * @return false if the creature hasn't been killed.
	 */
	public boolean doDie(L2Character killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (_isDead)
			{
				return false;
			}
			
			// now reset currentHp to zero
			setCurrentHp(0);
			setIsDead(true);
		}
		EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(killer, this), this);
		EventDispatcher.getInstance().notifyEvent(new OnCreatureKilled(killer, this), killer);
		
		abortAttack();
		abortCast();
		
		calculateRewards(killer);
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		_status.stopHpMpRegeneration();
		
		if (isMonster())
		{
			stopAllEffects();
		}
		else
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		
		// Notify L2Character AI
		if (hasAI())
		{
			getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		}
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		getAttackByList().clear();
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		return true;
	}
	
	@Override
	public boolean deleteMe()
	{
		if (hasAI())
		{
			getAI().stopAITask();
		}
		
		// Removes itself from the summoned list.
		if ((_summoner != null))
		{
			_summoner.removeSummonedNpc(getObjectId());
		}
		
		// Remove all effects, do not broadcast changes.
		_effectList.stopAllEffects(false);
		
		// Cancel all timers related to this Creature
		TimersManager.getInstance().cancelTimers(getObjectId());
		
		// Set world region to null.
		setWorldRegion(null);
		
		return true;
	}
	
	protected void calculateRewards(L2Character killer)
	{
	}
	
	/** Sets HP, MP and CP and revives the L2Character. */
	public void doRevive()
	{
		if (!_isDead)
		{
			return;
		}
		if (!_isTeleporting)
		{
			setIsPendingRevive(false);
			setIsDead(false);
			
			if ((Config.RESPAWN_RESTORE_CP > 0) && (_status.getCurrentCp() < (_stat.getMaxCp() * Config.RESPAWN_RESTORE_CP)))
			{
				_status.setCurrentCp(_stat.getMaxCp() * Config.RESPAWN_RESTORE_CP);
			}
			if ((Config.RESPAWN_RESTORE_HP > 0) && (_status.getCurrentHp() < (_stat.getMaxHp() * Config.RESPAWN_RESTORE_HP)))
			{
				_status.setCurrentHp(_stat.getMaxHp() * Config.RESPAWN_RESTORE_HP);
			}
			if ((Config.RESPAWN_RESTORE_MP > 0) && (_status.getCurrentMp() < (_stat.getMaxMp() * Config.RESPAWN_RESTORE_MP)))
			{
				_status.setCurrentMp(_stat.getMaxMp() * Config.RESPAWN_RESTORE_MP);
			}
			
			// Start broadcast status
			broadcastPacket(new Revive(this));
			
			ZoneManager.getInstance().getRegion(this).onRevive(this);
		}
		else
		{
			setIsPendingRevive(true);
		}
	}
	
	/**
	 * Revives the L2Character using skill.
	 * @param revivePower
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	/**
	 * Gets this creature's AI.
	 * @return the AI
	 */
	public final L2CharacterAI getAI()
	{
		if (_ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
				{
					return _ai = initAI();
				}
			}
		}
		return _ai;
	}
	
	/**
	 * Initialize this creature's AI.<br>
	 * OOP approach to be overridden in child classes.
	 * @return the new AI
	 */
	protected L2CharacterAI initAI()
	{
		return new L2CharacterAI(this);
	}
	
	public void detachAI()
	{
		if (isWalker())
		{
			return;
		}
		setAI(null);
	}
	
	public void setAI(L2CharacterAI newAI)
	{
		final L2CharacterAI oldAI = _ai;
		if ((oldAI != null) && (oldAI != newAI) && (oldAI instanceof L2AttackableAI))
		{
			oldAI.stopAITask();
		}
		_ai = newAI;
	}
	
	/**
	 * Verifies if this creature has an AI,
	 * @return {@code true} if this creature has an AI, {@code false} otherwise
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/**
	 * @return True if the L2Character is RaidBoss or his minion.
	 */
	public boolean isRaid()
	{
		return false;
	}
	
	/**
	 * @return True if the L2Character is minion.
	 */
	public boolean isMinion()
	{
		return false;
	}
	
	/**
	 * @return True if the L2Character is minion of RaidBoss.
	 */
	public boolean isRaidMinion()
	{
		return false;
	}
	
	/**
	 * @return a list of L2Character that attacked.
	 */
	public final Set<WeakReference<L2Character>> getAttackByList()
	{
		if (_attackByList == null)
		{
			synchronized (this)
			{
				if (_attackByList == null)
				{
					_attackByList = ConcurrentHashMap.newKeySet();
				}
			}
		}
		return _attackByList;
	}
	
	public final boolean isControlBlocked()
	{
		return isAffected(EffectFlag.BLOCK_CONTROL);
	}
	
	/**
	 * @return True if the L2Character can't use its skills (ex : stun, sleep...).
	 */
	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || hasBlockActions();
	}
	
	/**
	 * @return True if the L2Character can't attack (stun, sleep, attackEndTime, fakeDeath, paralyze, attackMute).
	 */
	public boolean isAttackingDisabled()
	{
		return _isFlying || hasBlockActions() || isAttackingNow() || isAlikeDead() || isPhysicalAttackMuted() || _AIdisabled;
	}
	
	public final boolean isConfused()
	{
		return isAffected(EffectFlag.CONFUSED);
	}
	
	/**
	 * @return True if the L2Character is dead or use fake death.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	/**
	 * @return True if the L2Character is dead.
	 */
	public final boolean isDead()
	{
		return _isDead;
	}
	
	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	public final boolean isMuted()
	{
		return isAffected(EffectFlag.MUTED);
	}
	
	public final boolean isPhysicalMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_MUTED);
	}
	
	public final boolean isPhysicalAttackMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_ATTACK_MUTED);
	}
	
	/**
	 * @return True if the L2Character can't move (stun, root, sleep, overload, paralyzed).
	 */
	public boolean isMovementDisabled()
	{
		// check for isTeleporting to prevent teleport cheating (if appear packet not received)
		return hasBlockActions() || isRooted() || _isOverloaded || _isImmobilized || isAlikeDead() || _isTeleporting;
	}
	
	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	/**
	 * Set the overloaded status of the L2Character is overloaded (if True, the L2PcInstance can't take more item).
	 * @param value
	 */
	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	public final boolean isPendingRevive()
	{
		return _isDead && _isPendingRevive;
	}
	
	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}
	
	public final boolean isDisarmed()
	{
		return isAffected(EffectFlag.DISARMED);
	}
	
	/**
	 * @return the summon
	 */
	public L2Summon getPet()
	{
		return null;
	}
	
	/**
	 * @return the summon
	 */
	public Map<Integer, L2Summon> getServitors()
	{
		return Collections.emptyMap();
	}
	
	public L2Summon getServitor(int objectId)
	{
		return null;
	}
	
	/**
	 * @return {@code true} if the character has a summon, {@code false} otherwise
	 */
	public final boolean hasSummon()
	{
		return (getPet() != null) || !getServitors().isEmpty();
	}
	
	/**
	 * @return {@code true} if the character has a pet, {@code false} otherwise
	 */
	public final boolean hasPet()
	{
		return getPet() != null;
	}
	
	public final boolean hasServitor(int objectId)
	{
		return getServitors().containsKey(objectId);
	}
	
	/**
	 * @return {@code true} if the character has a servitor, {@code false} otherwise
	 */
	public final boolean hasServitors()
	{
		return !getServitors().isEmpty();
	}
	
	public void removeServitor(int objectId)
	{
		getServitors().remove(objectId);
	}
	
	public final boolean isRooted()
	{
		return isAffected(EffectFlag.ROOTED);
	}
	
	/**
	 * @return True if the L2Character is running.
	 */
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	private final void setIsRunning(boolean value)
	{
		if (_isRunning == value)
		{
			return;
		}
		
		_isRunning = value;
		if (_stat.getRunSpeed() != 0)
		{
			broadcastPacket(new ChangeMoveType(this));
		}
		if (isPlayer())
		{
			getActingPlayer().broadcastUserInfo();
		}
		else if (isSummon())
		{
			broadcastStatusUpdate();
		}
		else if (isNpc())
		{
			L2World.getInstance().forEachVisibleObject(this, L2PcInstance.class, player ->
			{
				if (!isVisibleFor(player))
				{
					return;
				}
				
				if (isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo((L2Npc) this));
				}
				else if (_stat.getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo((L2Npc) this, player));
				}
				else
				{
					player.sendPacket(new NpcInfo((L2Npc) this));
				}
			});
		}
	}
	
	/** Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
	public final void setRunning()
	{
		setIsRunning(true);
	}
	
	public final boolean hasBlockActions()
	{
		return _blockActions || isAffected(EffectFlag.BLOCK_ACTIONS) || isAffected(EffectFlag.CONDITIONAL_BLOCK_ACTIONS);
	}
	
	public final void setBlockActions(boolean blockActions)
	{
		_blockActions = blockActions;
	}
	
	public final boolean isBetrayed()
	{
		return isAffected(EffectFlag.BETRAYED);
	}
	
	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}
	
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}
	
	public void setUndying(boolean undying)
	{
		_isUndying = undying;
	}
	
	public boolean isUndying()
	{
		return _isUndying || isInvul() || isAffected(EffectFlag.IGNORE_DEATH) || isInsideZone(ZoneId.UNDYING);
	}
	
	public boolean isHpBlocked()
	{
		return isInvul() || isAffected(EffectFlag.HP_BLOCK);
	}
	
	public boolean isMpBlocked()
	{
		return isInvul() || isAffected(EffectFlag.MP_BLOCK);
	}
	
	public boolean isBuffBlocked()
	{
		return isAffected(EffectFlag.BUFF_BLOCK);
	}
	
	public boolean isDebuffBlocked()
	{
		return isInvul() || isAffected(EffectFlag.DEBUFF_BLOCK);
	}
	
	public boolean isUndead()
	{
		return false;
	}
	
	public boolean isResurrectionBlocked()
	{
		return isAffected(EffectFlag.BLOCK_RESURRECTION);
	}
	
	public final boolean isFlying()
	{
		return _isFlying;
	}
	
	public final void setIsFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	public CharStat getStat()
	{
		return _stat;
	}
	
	/**
	 * Initializes the CharStat class of the L2Object, is overwritten in classes that require a different CharStat Type.<br>
	 * Removes the need for instanceof checks.
	 */
	public void initCharStat()
	{
		_stat = new CharStat(this);
	}
	
	public final void setStat(CharStat value)
	{
		_stat = value;
	}
	
	public CharStatus getStatus()
	{
		return _status;
	}
	
	/**
	 * Initializes the CharStatus class of the L2Object, is overwritten in classes that require a different CharStatus Type.<br>
	 * Removes the need for instanceof checks.
	 */
	public void initCharStatus()
	{
		_status = new CharStatus(this);
	}
	
	public final void setStatus(CharStatus value)
	{
		_status = value;
	}
	
	public L2CharTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Set the template of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).<br>
	 * All of those properties are stored in a different template for each type of L2Character.<br>
	 * Each template is loaded once in the server cache memory (reduce memory use).<br>
	 * When a new instance of L2Character is spawned, server just create a link between the instance and the template This link is stored in <B>_template</B>.
	 * @param template
	 */
	protected final void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}
	
	/**
	 * @return the Title of the L2Character.
	 */
	public final String getTitle()
	{
		// Champion titles
		if (isChampion())
		{
			return Config.CHAMP_TITLE;
		}
		// Custom level titles
		if (Config.SHOW_NPC_LVL && isMonster())
		{
			String t = "Lv " + getLevel() + (((L2MonsterInstance) this).isAggressive() ? "*" : "");
			if (_title != null)
			{
				t += " " + _title;
			}
			return t;
		}
		// Set trap title
		if (isTrap() && (((L2TrapInstance) this).getOwner() != null))
		{
			_title = ((L2TrapInstance) this).getOwner().getName();
		}
		return _title != null ? _title : "";
	}
	
	/**
	 * Set the Title of the Creature.
	 * @param value
	 */
	public final void setTitle(String value)
	{
		if (value == null)
		{
			_title = "";
		}
		else
		{
			_title = value.length() > 21 ? value.substring(0, 20) : value;
		}
	}
	
	/**
	 * Set the L2Character movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance.
	 */
	public final void setWalking()
	{
		setIsRunning(false);
	}
	
	/**
	 * Active the abnormal effect Fake Death flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.
	 */
	public final void startFakeDeath()
	{
		if (!isPlayer())
		{
			return;
		}
		
		// Aborts any attacks/casts if fake dead
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}
	
	public final void startParalyze()
	{
		// Aborts any attacks/casts if paralyzed
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_ACTION_BLOCKED);
	}
	
	/**
	 * Stop all active skills effects in progress on the L2Character.
	 */
	public void stopAllEffects()
	{
		_effectList.stopAllEffects(true);
	}
	
	/**
	 * Stops all effects, except those that last through death.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effectList.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Stop and remove the effects corresponding to the skill ID.
	 * @param removed if {@code true} the effect will be set as removed, and a system message will be sent
	 * @param skillId the skill Id
	 */
	public void stopSkillEffects(boolean removed, int skillId)
	{
		_effectList.stopSkillEffects(removed, skillId);
	}
	
	public void stopSkillEffects(Skill skill)
	{
		_effectList.stopSkillEffects(true, skill.getId());
	}
	
	public final void stopEffects(EffectFlag effectFlag)
	{
		_effectList.stopEffects(effectFlag);
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set.<br>
	 * Called on any action except movement (attack, cast).
	 */
	public final void stopEffectsOnAction()
	{
		_effectList.stopEffectsOnAction();
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnDamage" set.<br>
	 * Called on decreasing HP and mana burn.
	 */
	public final void stopEffectsOnDamage()
	{
		_effectList.stopEffectsOnDamage();
	}
	
	/**
	 * Stop a specified/all Fake Death abnormal L2Effect.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Delete a specified/all (if effect=null) Fake Death abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _fake_death to False</li>
	 * <li>Notify the L2Character AI</li>
	 * </ul>
	 * @param removeEffects
	 */
	public final void stopFakeDeath(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectFlag.FAKE_DEATH);
		}
		
		// if this is a player instance, start the grace period for this character (grace from mobs only)!
		if (isPlayer())
		{
			getActingPlayer().setRecentFakeDeath(true);
		}
		
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		// TODO: Temp hack: players see FD on ppl that are moving: Teleport to someone who uses FD - if he gets up he will fall down again for that client -
		// even tho he is actually standing... Probably bad info in CharInfo packet?
		broadcastPacket(new Revive(this));
	}
	
	/**
	 * Stop all block actions (stun) effects.<br>
	 * @param removeEffects {@code true} removes all block actions effects, {@code false} only notifies AI to think.
	 */
	public final void stopStunning(boolean removeEffects)
	{
		if (removeEffects)
		{
			_effectList.stopEffects(AbnormalType.STUN);
		}
		
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
	}
	
	/**
	 * Stop L2Effect: Transformation.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Remove Transformation Effect</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li>
	 * </ul>
	 * @param removeEffects
	 */
	public final void stopTransformation(boolean removeEffects)
	{
		if (removeEffects)
		{
			_effectList.stopEffects(AbnormalType.TRANSFORM);
			_effectList.stopEffects(AbnormalType.CHANGEBODY);
		}
		
		if (_transform.isPresent())
		{
			untransform();
		}
		
		if (!isPlayer())
		{
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
		updateAbnormalVisualEffects();
	}
	
	/**
	 * Updates the visual abnormal state of this character. <br>
	 */
	public void updateAbnormalVisualEffects()
	{
		// overridden
	}
	
	/**
	 * Update active skills in progress (In Use and Not In Use because stacked) icons on client.<br>
	 * <B><U>Concept</U>:</B><br>
	 * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icon on the client.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the client of the player and not clients of all players in the party.</B></FONT>
	 */
	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Updates Effect Icons for this character(player/summon) and his party if any.
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}
	
	public boolean isAffectedBySkill(SkillHolder skill)
	{
		return isAffectedBySkill(skill.getSkillId());
	}
	
	public boolean isAffectedBySkill(int skillId)
	{
		return _effectList.isAffectedBySkill(skillId);
	}
	
	public int getAffectedSkillLevel(int skillId)
	{
		final BuffInfo info = _effectList.getBuffInfoBySkillId(skillId);
		return info == null ? 0 : info.getSkill().getLevel();
	}
	
	/**
	 * This class group all movement data.<br>
	 * <B><U> Data</U> :</B>
	 * <ul>
	 * <li>_moveTimestamp : Last time position update</li>
	 * <li>_xDestination, _yDestination, _zDestination : Position of the destination</li>
	 * <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of the origin</li>
	 * <li>_moveStartTime : Start time of the movement</li>
	 * <li>_ticksToMove : Number of ticks between the start and the destination</li>
	 * <li>_xSpeedTicks, _ySpeedTicks : Speed in unit/ticks</li>
	 * </ul>
	 */
	public static class MoveData
	{
		// when we retrieve x/y/z we use GameTimeControl.getGameTicks()
		// if we are moving, but move timestamp==gameticks, we don't need
		// to recalculate position
		public int _moveStartTime;
		public int _moveTimestamp; // last update
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate; // otherwise there would be rounding errors
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;
		
		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public List<Location> geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}
	
	public void broadcastModifiedStats(Set<Stats> changed)
	{
		if (!isSpawned())
		{
			return;
		}
		
		if ((changed == null) || changed.isEmpty())
		{
			return;
		}
		
		// Don't broadcast modified stats on login.
		if (isPlayer() && !getActingPlayer().isOnline())
		{
			return;
		}
		
		// If this creature was previously moving, but now due to stat change can no longer move, broadcast StopMove packet.
		if (isMoving() && (_stat.getMoveSpeed() <= 0))
		{
			stopMove(null);
		}
		
		if (isSummon())
		{
			final L2Summon summon = (L2Summon) this;
			if (summon.getOwner() != null)
			{
				summon.updateAndBroadcastStatus(1);
			}
		}
		else
		{
			final boolean broadcastFull = true;
			final StatusUpdate su = new StatusUpdate(this);
			UserInfo info = null;
			if (isPlayer())
			{
				info = new UserInfo(getActingPlayer(), false);
				info.addComponentType(UserInfoType.SLOTS, UserInfoType.ENCHANTLEVEL);
			}
			for (Stats stat : changed)
			{
				if (info != null)
				{
					switch (stat)
					{
						case MOVE_SPEED:
						case RUN_SPEED:
						case WALK_SPEED:
						case SWIM_RUN_SPEED:
						case SWIM_WALK_SPEED:
						case FLY_RUN_SPEED:
						case FLY_WALK_SPEED:
						{
							info.addComponentType(UserInfoType.MULTIPLIER);
							break;
						}
						case PHYSICAL_ATTACK_SPEED:
						{
							info.addComponentType(UserInfoType.MULTIPLIER, UserInfoType.STATS);
							break;
						}
						case PHYSICAL_ATTACK:
						case PHYSICAL_DEFENCE:
						case EVASION_RATE:
						case ACCURACY_COMBAT:
						case CRITICAL_RATE:
						case MAGIC_CRITICAL_RATE:
						case MAGIC_EVASION_RATE:
						case ACCURACY_MAGIC:
						case MAGIC_ATTACK:
						case MAGIC_ATTACK_SPEED:
						case MAGICAL_DEFENCE:
						case HIT_AT_NIGHT:
						{
							info.addComponentType(UserInfoType.STATS);
							break;
						}
						case MAX_CP:
						{
							if (isPlayer())
							{
								info.addComponentType(UserInfoType.MAX_HPCPMP);
							}
							else
							{
								su.addUpdate(StatusUpdateType.MAX_CP, _stat.getMaxCp());
							}
							break;
						}
						case MAX_HP:
						{
							if (isPlayer())
							{
								info.addComponentType(UserInfoType.MAX_HPCPMP);
							}
							else
							{
								su.addUpdate(StatusUpdateType.MAX_HP, _stat.getMaxHp());
							}
							break;
						}
						case MAX_MP:
						{
							if (isPlayer())
							{
								info.addComponentType(UserInfoType.MAX_HPCPMP);
							}
							else
							{
								su.addUpdate(StatusUpdateType.MAX_CP, _stat.getMaxMp());
							}
							break;
						}
						case STAT_STR:
						case STAT_CON:
						case STAT_DEX:
						case STAT_INT:
						case STAT_WIT:
						case STAT_MEN:
						{
							info.addComponentType(UserInfoType.BASE_STATS);
							break;
						}
						case FIRE_RES:
						case WATER_RES:
						case WIND_RES:
						case EARTH_RES:
						case HOLY_RES:
						case DARK_RES:
						{
							info.addComponentType(UserInfoType.ELEMENTALS);
							break;
						}
						case FIRE_POWER:
						case WATER_POWER:
						case WIND_POWER:
						case EARTH_POWER:
						case HOLY_POWER:
						case DARK_POWER:
						{
							info.addComponentType(UserInfoType.ATK_ELEMENTAL);
							break;
						}
					}
				}
			}
			
			if (isPlayer())
			{
				final L2PcInstance player = getActingPlayer();
				player.refreshOverloaded(true);
				player.refreshExpertisePenalty();
				sendPacket(info);
				
				if (broadcastFull)
				{
					player.broadcastCharInfo();
				}
				else if (su.hasUpdates())
				{
					broadcastPacket(su);
				}
				if (hasServitors() && hasAbnormalType(AbnormalType.ABILITY_CHANGE))
				{
					getServitors().values().forEach(L2Summon::broadcastStatusUpdate);
				}
			}
			else if (isNpc())
			{
				if (broadcastFull)
				{
					L2World.getInstance().forEachVisibleObject(this, L2PcInstance.class, player ->
					{
						if (!isVisibleFor(player))
						{
							return;
						}
						
						if (isFakePlayer())
						{
							player.sendPacket(new FakePlayerInfo((L2Npc) this));
						}
						else if (_stat.getRunSpeed() == 0)
						{
							player.sendPacket(new ServerObjectInfo((L2Npc) this, player));
						}
						else
						{
							player.sendPacket(new NpcInfo((L2Npc) this));
						}
					});
				}
				else if (su.hasUpdates())
				{
					broadcastPacket(su);
				}
			}
			else if (su.hasUpdates())
			{
				broadcastPacket(su);
			}
		}
	}
	
	public final int getXdestination()
	{
		final MoveData m = _move;
		
		if (m != null)
		{
			return m._xDestination;
		}
		
		return getX();
	}
	
	/**
	 * @return the Y destination of the L2Character or the Y position if not in movement.
	 */
	public final int getYdestination()
	{
		final MoveData m = _move;
		
		if (m != null)
		{
			return m._yDestination;
		}
		
		return getY();
	}
	
	/**
	 * @return the Z destination of the L2Character or the Z position if not in movement.
	 */
	public final int getZdestination()
	{
		final MoveData m = _move;
		
		if (m != null)
		{
			return m._zDestination;
		}
		
		return getZ();
	}
	
	/**
	 * @return True if the L2Character is in combat.
	 */
	public boolean isInCombat()
	{
		return hasAI() && getAI().isAutoAttacking();
	}
	
	/**
	 * @return True if the L2Character is moving.
	 */
	public final boolean isMoving()
	{
		return _move != null;
	}
	
	/**
	 * @return True if the L2Character is travelling a calculated path.
	 */
	public final boolean isOnGeodataPath()
	{
		final MoveData m = _move;
		if (m == null)
		{
			return false;
		}
		if (m.onGeodataPathIndex == -1)
		{
			return false;
		}
		if (m.onGeodataPathIndex == (m.geoPath.size() - 1))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @return True if the Creature is casting any kind of skill, including simultaneous skills like potions.
	 */
	public final boolean isCastingNow()
	{
		return !_skillCasters.isEmpty();
	}
	
	public final boolean isCastingNow(SkillCastingType skillCastingType)
	{
		return _skillCasters.containsKey(skillCastingType);
	}
	
	public final boolean isCastingNow(Predicate<SkillCaster> filter)
	{
		return _skillCasters.values().stream().anyMatch(filter);
	}
	
	/**
	 * @return True if the L2Character is attacking.
	 */
	public final boolean isAttackingNow()
	{
		return _attackEndTime > System.nanoTime();
	}
	
	/**
	 * Abort the attack of the L2Character and send Server->Client ActionFailed packet.
	 */
	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			final ScheduledFuture<?> hitTask = _hitTask;
			if (hitTask != null)
			{
				hitTask.cancel(false);
				_hitTask = null;
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Abort the cast of normal non-simultaneous skills.
	 * @return {@code true} if a skill casting has been aborted, {@code false} otherwise.
	 */
	public final boolean abortCast()
	{
		return abortCast(SkillCaster::isAnyNormalType);
	}
	
	/**
	 * Try to break this character's casting using the given filters.
	 * @param filter
	 * @return {@code true} if a skill casting has been aborted, {@code false} otherwise.
	 */
	public final boolean abortCast(Predicate<SkillCaster> filter)
	{
		final SkillCaster skillCaster = getSkillCaster(SkillCaster::canAbortCast, filter);
		if (skillCaster != null)
		{
			skillCaster.stopCasting(true);
			if (isPlayer())
			{
				getActingPlayer().setQueuedSkill(null, null, false, false);
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Update the position of the L2Character during a movement and return True if the movement is finished.<br>
	 * <B><U>Concept</U>:</B><br>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character.<br>
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<br>
	 * When the movement is started (ex : by MovetoLocation), this method will be called each 0.1 sec to estimate and update the L2Character position on the server.<br>
	 * Note, that the current server position can differe from the current client position even if each movement is straight foward.<br>
	 * That's why, client send regularly a Client->Server ValidatePosition packet to eventually correct the gap on the server.<br>
	 * But, it's always the server position that is used in range calculation. At the end of the estimated movement time,<br>
	 * the L2Character position is automatically set to the destination position even if the movement is not finished.<br>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: The current Z position is obtained FROM THE CLIENT by the Client->Server ValidatePosition Packet.<br>
	 * But x and y positions must be calculated to avoid that players try to modify their movement speed.</B></FONT>
	 * @return True if the movement is finished
	 */
	public boolean updatePosition()
	{
		// Get movement data
		final MoveData m = _move;
		
		if (m == null)
		{
			return true;
		}
		
		if (!isSpawned())
		{
			_move = null;
			return true;
		}
		
		// Check if this is the first update
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}
		
		final int gameTicks = GameTimeController.getInstance().getGameTicks();
		
		// Check if the position has already been calculated
		if (m._moveTimestamp == gameTicks)
		{
			return false;
		}
		
		final int xPrev = getX();
		final int yPrev = getY();
		int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations
		
		double dx;
		double dy;
		double dz;
		if (Config.COORD_SYNCHRONIZE == 1)
		// the only method that can modify x,y while moving (otherwise _move would/should be set null)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else
		// otherwise we need saved temporary values to avoid rounding errors
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		
		// Z coordinate will follow client values
		dz = m._zDestination - zPrev;
		
		if (isPlayer())
		{
			final double distance = Math.hypot(dx, dy);
			if (_cursorKeyMovement // In case of cursor movement, avoid moving through obstacles.
				|| (distance > 3000)) // Stop movement when player has clicked far away and intersected with an obstacle.
			{
				final double angle = Util.convertHeadingToDegree(getHeading());
				final double radian = Math.toRadians(angle);
				final double course = Math.toRadians(180);
				final double frontDistance = 10 * (_stat.getMoveSpeed() / 100);
				final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
				final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
				final int x = xPrev + x1;
				final int y = yPrev + y1;
				if (!GeoEngine.getInstance().canMoveToTarget(xPrev, yPrev, zPrev, x, y, zPrev, getInstanceWorld()))
				{
					_move.onGeodataPathIndex = -1;
					stopMove(getActingPlayer().getLastServerPosition());
					_cursorKeyMovementActive = false;
					return false;
				}
			}
			// Prevent player moving on ledges.
			if ((dz > 100) && (distance < 300))
			{
				_move.onGeodataPathIndex = -1;
				stopMove(getActingPlayer().getLastServerPosition());
				return false;
			}
		}
		
		final boolean isFloating = _isFlying || isInsideZone(ZoneId.WATER);
		double delta = (dx * dx) + (dy * dy);
		if ((delta < 10000) && ((dz * dz) > 2500) // close enough, allows error between client and server geodata if it cannot be avoided
			&& !isFloating)
		{
			delta = Math.sqrt(delta);
		}
		else
		{
			delta = Math.sqrt(delta + (dz * dz));
		}
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = (_stat.getMoveSpeed() * (gameTicks - m._moveTimestamp)) / GameTimeController.TICKS_PER_SECOND;
			distFraction = distPassed / delta;
		}
		
		// if (Config.DEVELOPER) LOGGER.warning("Move Ticks:" + (gameTicks - m._moveTimestamp) + ", distPassed:" + distPassed + ", distFraction:" + distFraction);
		
		if (distFraction > 1)
		{
			// Set the position of the L2Character to the destination
			super.setXYZ(m._xDestination, m._yDestination, m._zDestination);
		}
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;
			
			// Set the position of the L2Character to estimated after parcial move
			super.setXYZ((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) ((dz * distFraction) + 0.5));
		}
		revalidateZone(false);
		
		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;
		
		if (distFraction > 1)
		{
			ThreadPool.execute(() -> getAI().notifyEvent(CtrlEvent.EVT_ARRIVED));
			return true;
		}
		
		return false;
	}
	
	public void revalidateZone(boolean force)
	{
		// This function is called too often from movement code
		if (force)
		{
			_zoneValidateCounter = 4;
		}
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
			{
				_zoneValidateCounter = 4;
			}
			else
			{
				return;
			}
		}
		
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.revalidateZones(this);
		}
		else // Precaution. Moved at invalid region?
		{
			L2World.getInstance().disposeOutOfBoundsObject(this);
		}
	}
	
	/**
	 * Stop movement of the L2Character (Called by AI Accessor only).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Delete movement data of the L2Character</li>
	 * <li>Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading</li>
	 * <li>Remove the L2Object object from _gmList of GmListTable</li>
	 * <li>Remove object from _knownObjects and _knownPlayer of all surrounding L2WorldRegion L2Characters</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: This method DOESN'T send Server->Client packet StopMove/StopRotation</B></FONT>
	 * @param loc
	 */
	public void stopMove(Location loc)
	{
		// Delete movement data of the L2Character
		_move = null;
		_cursorKeyMovement = false;
		
		// All data are contained in a Location object
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		broadcastPacket(new StopMove(this));
	}
	
	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Target a L2Object (add the target to the L2Character _target, _knownObject and L2Character to _KnownObject of the L2Object).<br>
	 * <B><U>Concept</U>:</B><br>
	 * The L2Object (including L2Character) targeted is identified in <B>_target</B> of the L2Character.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Set the _target of L2Character to L2Object</li>
	 * <li>If necessary, add L2Object to _knownObject of the L2Character</li>
	 * <li>If necessary, add L2Character to _KnownObject of the L2Object</li>
	 * <li>If object==null, cancel Attak or Cast</li>
	 * </ul>
	 * @param object L2object to target
	 */
	public void setTarget(L2Object object)
	{
		if ((object != null) && !object.isSpawned())
		{
			object = null;
		}
		
		_target = object;
	}
	
	/**
	 * @return the identifier of the L2Object targeted or -1.
	 */
	public final int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}
		return 0;
	}
	
	/**
	 * @return the L2Object targeted or null.
	 */
	public final L2Object getTarget()
	{
		return _target;
	}
	
	// called from AIAccessor only
	
	/**
	 * Calculate movement data for a move to location action and add the L2Character to movingObjects of GameTimeController (only called by AI Accessor).<br>
	 * <B><U>Concept</U>:</B><br>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character.<br>
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<br>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController that will call the updatePosition method of those L2Character each 0.1s.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Get current position of the L2Character</li>
	 * <li>Calculate distance (dx,dy) between current position and destination including offset</li>
	 * <li>Create and Init a MoveData object</li>
	 * <li>Set the L2Character _move object to MoveData object</li>
	 * <li>Add the L2Character to movingObjects of the GameTimeController</li>
	 * <li>Create a task to notify the AI that L2Character arrives at a check point of the movement</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: This method DOESN'T send Server->Client packet MoveToPawn/CharMoveToLocation.</B></FONT><br>
	 * <B><U>Example of use</U>:</B>
	 * <ul>
	 * <li>AI : onIntentionMoveTo(Location), onIntentionPickUp(L2Object), onIntentionInteract(L2Object)</li>
	 * <li>FollowTask</li>
	 * </ul>
	 * @param x The X position of the destination
	 * @param y The Y position of the destination
	 * @param z The Y position of the destination
	 * @param offset The size of the interaction area of the L2Character targeted
	 */
	public void moveToLocation(int x, int y, int z, int offset)
	{
		// Get the Move Speed of the L2Charcater
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			return;
		}
		
		// Get current position of the L2Character
		final int curX = getX();
		final int curY = getY();
		final int curZ = getZ();
		
		// Calculate distance (dx,dy) between current position and destination
		// TODO: improve Z axis move/follow support when dx,dy are small compared to dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.hypot(dx, dy);
		
		if (!_cursorKeyMovementActive && (distance > 200))
		{
			return;
		}
		
		final boolean verticalMovementOnly = _isFlying && (distance == 0) && (dz != 0);
		if (verticalMovementOnly)
		{
			distance = Math.abs(dz);
		}
		
		// Make water move short and use no geodata checks for swimming chars distance in a click can easily be over 3000.
		final boolean isInWater = isInsideZone(ZoneId.WATER);
		if (isInWater && (distance > 700))
		{
			final double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.hypot(dx, dy);
		}
		
		// @formatter:off
		// Define movement angles needed
		// ^
		// |    X (x,y)
		// |   /
		// |  / distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)
		// @formatter:on
		
		double cos;
		double sin;
		
		// Check if a movement offset is defined or no distance to go through
		if ((offset > 0) || (distance < 1))
		{
			// approximation for moving closer when z coordinates are different
			// TODO: handle Z axis movement better
			offset -= Math.abs(dz);
			if (offset < 5)
			{
				offset = 5;
			}
			
			// If no distance to go through, the movement is canceled
			if ((distance < 1) || ((distance - offset) <= 0))
			{
				// Notify the AI that the L2Character is arrived at destination
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return;
			}
			
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
			
			distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range
			
			// Calculate the new destination with offset included
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}
		
		// Create and Init a MoveData object
		final MoveData m = new MoveData();
		
		// GEODATA MOVEMENT CHECKS AND PATHFINDING
		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m.disregardingGeodata = false;
		
		if (!_isFlying && !isInWater && !isVehicle() && !_cursorKeyMovement)
		{
			final boolean isInVehicle = isPlayer() && (getActingPlayer().getVehicle() != null);
			if (isInVehicle)
			{
				m.disregardingGeodata = true;
			}
			
			// Movement checks.
			if (Config.PATHFINDING && !(this instanceof FriendlyNpcInstance))
			{
				final double originalDistance = distance;
				final int originalX = x;
				final int originalY = y;
				final int originalZ = z;
				final int gtx = (originalX - L2World.MAP_MIN_X) >> 4;
				final int gty = (originalY - L2World.MAP_MIN_Y) >> 4;
				
				if (isOnGeodataPath())
				{
					try
					{
						if ((gtx == _move.geoPathGtx) && (gty == _move.geoPathGty))
						{
							return;
						}
						_move.onGeodataPathIndex = -1; // Set not on geodata path.
					}
					catch (NullPointerException e)
					{
						// nothing
					}
				}
				
				if (!isInVehicle // Not in vehicle.
					&& !(isPlayer() && (distance > 3000)) // Should be able to click far away and move.
					&& !(isMonster() && (Math.abs(dz) > 100)) // Monsters can move on ledges.
					&& !(((curZ - z) > 300) && (distance < 300))) // Prohibit correcting destination if character wants to fall.
				{
					// location different if destination wasn't reached (or just z coord is different)
					final Location destiny = GeoEngine.getInstance().canMoveToTargetLoc(curX, curY, curZ, x, y, z, getInstanceWorld());
					x = destiny.getX();
					y = destiny.getY();
					dx = x - curX;
					dy = y - curY;
					dz = z - curZ;
					distance = verticalMovementOnly ? Math.pow(dz, 2) : Math.hypot(dx, dy);
				}
				
				// Pathfinding checks.
				if (((originalDistance - distance) > 30) && !isControlBlocked() && !isInVehicle)
				{
					// Path calculation -- overrides previous movement check
					m.geoPath = GeoEngine.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceWorld());
					if ((m.geoPath == null) || (m.geoPath.size() < 2)) // No path found
					{
						m.disregardingGeodata = true;
						
						// Mobius: Verify destination. Prevents wall collision issues.
						final Location newDestination = GeoEngine.getInstance().canMoveToTargetLoc(curX, curY, curZ, originalX, originalY, originalZ, getInstanceWorld());
						x = newDestination.getX();
						y = newDestination.getY();
						z = newDestination.getZ();
					}
					else
					{
						m.onGeodataPathIndex = 0; // on first segment
						m.geoPathGtx = gtx;
						m.geoPathGty = gty;
						m.geoPathAccurateTx = originalX;
						m.geoPathAccurateTy = originalY;
						
						x = m.geoPath.get(m.onGeodataPathIndex).getX();
						y = m.geoPath.get(m.onGeodataPathIndex).getY();
						z = m.geoPath.get(m.onGeodataPathIndex).getZ();
						
						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? Math.pow(dz, 2) : Math.hypot(dx, dy);
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			
			// If no distance to go through, the movement is canceled
			if ((distance < 1) && (Config.PATHFINDING || isPlayable()))
			{
				if (isSummon())
				{
					((L2Summon) this).setFollowStatus(false);
				}
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				return;
			}
		}
		
		// Apply Z distance for flying or swimming for correct timing calculations
		if ((_isFlying || isInWater) && !verticalMovementOnly)
		{
			distance = Math.hypot(distance, dz);
		}
		
		// Calculate the number of ticks between the current position and the destination
		// One tick added for rounding reasons
		final int ticksToMove = 1 + (int) ((GameTimeController.TICKS_PER_SECOND * distance) / speed);
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client
		
		// Calculate and set the heading of the L2Character
		m._heading = 0; // initial value for coordinate sync
		// Does not broke heading on vertical movements
		if (!verticalMovementOnly)
		{
			setHeading(Util.calculateHeadingFrom(cos, sin));
		}
		
		m._moveStartTime = GameTimeController.getInstance().getGameTicks();
		
		// Set the L2Character _move object to MoveData object
		_move = m;
		
		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive to destination by GameTimeController
	}
	
	public boolean moveToNextRoutePoint()
	{
		if (!isOnGeodataPath())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		
		// Get the Move Speed of the L2Charcater
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		
		final MoveData md = _move;
		if (md == null)
		{
			return false;
		}
		
		// Get current position of the L2Character
		final int curX = getX();
		final int curY = getY();
		
		// Create and Init a MoveData object
		final MoveData m = new MoveData();
		
		// Update MoveData object
		m.onGeodataPathIndex = md.onGeodataPathIndex + 1; // next segment
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;
		
		if (md.onGeodataPathIndex == (md.geoPath.size() - 2))
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		else
		{
			m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
			m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		
		final double distance = Math.hypot(m._xDestination - curX, m._yDestination - curY);
		// Calculate and set the heading of the L2Character
		if (distance != 0)
		{
			setHeading(Util.calculateHeadingFrom(curX, curY, m._xDestination, m._yDestination));
		}
		
		// Calculate the number of ticks between the current position and the destination
		// One tick added for rounding reasons
		final int ticksToMove = 1 + (int) ((GameTimeController.TICKS_PER_SECOND * distance) / speed);
		
		m._heading = 0; // initial value for coordinate sync
		
		m._moveStartTime = GameTimeController.getInstance().getGameTicks();
		
		// Set the L2Character _move object to MoveData object
		_move = m;
		
		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
		
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController
		
		// Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
		broadcastPacket(new MoveToLocation(this));
		
		return true;
	}
	
	public boolean validateMovementHeading(int heading)
	{
		final MoveData m = _move;
		
		if (m == null)
		{
			return true;
		}
		
		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0); // initial value or false
			m._heading = heading;
		}
		
		return result;
	}
	
	/**
	 * Check if this object is inside the given 2D radius around the given point.
	 * @param loc Location of the target
	 * @param radius the radius around the target
	 * @return true if the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius2D(ILocational loc, int radius)
	{
		return isInsideRadius2D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Check if this object is inside the given 2D radius around the given point.
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @param radius the radius around the target
	 * @return true if the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius2D(int x, int y, int z, int radius)
	{
		return calculateDistanceSq2D(x, y, z) < (radius * radius);
	}
	
	/**
	 * Check if this object is inside the given 3D radius around the given point.
	 * @param loc Location of the target
	 * @param radius the radius around the target
	 * @return true if the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius3D(ILocational loc, int radius)
	{
		return isInsideRadius3D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Check if this object is inside the given 3D radius around the given point.
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @param radius the radius around the target
	 * @return true if the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius3D(int x, int y, int z, int radius)
	{
		return calculateDistanceSq3D(x, y, z) < (radius * radius);
	}
	
	/**
	 * <B><U> Overridden in </U> :</B>
	 * <li>L2PcInstance</li>
	 * @return True if arrows are available.
	 * @param type
	 */
	protected boolean checkAndEquipAmmunition(EtcItemType type)
	{
		return true;
	}
	
	/**
	 * Add Exp and Sp to the L2Character.<br>
	 * <B><U> Overridden in </U> :</B>
	 * <li>L2PcInstance</li>
	 * <li>L2PetInstance</li>
	 * @param addToExp
	 * @param addToSp
	 */
	public void addExpAndSp(double addToExp, double addToSp)
	{
		// Dummy method (overridden by players and pets)
	}
	
	/**
	 * <B><U> Overridden in </U> :</B>
	 * <li>L2PcInstance</li>
	 * @return the active weapon instance (always equiped in the right hand).
	 */
	public abstract L2ItemInstance getActiveWeaponInstance();
	
	/**
	 * <B><U> Overridden in </U> :</B>
	 * <li>L2PcInstance</li>
	 * @return the active weapon item (always equiped in the right hand).
	 */
	public abstract L2Weapon getActiveWeaponItem();
	
	/**
	 * <B><U> Overridden in </U> :</B>
	 * <li>L2PcInstance</li>
	 * @return the secondary weapon instance (always equiped in the left hand).
	 */
	public abstract L2ItemInstance getSecondaryWeaponInstance();
	
	/**
	 * <B><U> Overridden in </U> :</B>
	 * <li>L2PcInstance</li>
	 * @return the secondary {@link L2Item} item (always equiped in the left hand).
	 */
	public abstract L2Item getSecondaryWeaponItem();
	
	/**
	 * Manage hit process (called by Hit Task).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
	 * </ul>
	 * @param weapon the weapon used for the hit
	 * @param attack the attack data of targets to hit
	 * @param hitTime the time it took for this hit to occur
	 * @param attackTime the time it takes for the whole attack to complete
	 */
	public void onHitTimeNotDual(L2Weapon weapon, Attack attack, int hitTime, int attackTime)
	{
		if (_isDead)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		for (Hit hit : attack.getHits())
		{
			final L2Character target = ((L2Character) hit.getTarget());
			if ((target == null) || target.isDead() || !isInSurroundingRegion(target))
			{
				continue;
			}
			
			if (hit.isMiss())
			{
				notifyAttackAvoid(target, false);
			}
			else
			{
				onHitTarget(target, weapon, hit);
			}
		}
		
		_hitTask = ThreadPool.schedule(() -> onAttackFinish(attack), attackTime - hitTime);
	}
	
	public void onFirstHitTimeForDual(L2Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack)
	{
		if (_isDead)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		_hitTask = ThreadPool.schedule(() -> onSecondHitTimeForDual(weapon, attack, hitTime, delayForSecondAttack, attackTime), delayForSecondAttack);
		
		// First dual attack is the first hit only.
		final Hit hit = attack.getHits().get(0);
		final L2Character target = ((L2Character) hit.getTarget());
		
		if ((target == null) || target.isDead() || !isInSurroundingRegion(target))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if (hit.isMiss())
		{
			notifyAttackAvoid(target, false);
		}
		else
		{
			onHitTarget(target, weapon, hit);
		}
	}
	
	public void onSecondHitTimeForDual(L2Weapon weapon, Attack attack, int hitTime1, int hitTime2, int attackTime)
	{
		if (_isDead)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		// Second dual attack is the remaining hits (first hit not included)
		for (int i = 1; i < attack.getHits().size(); i++)
		{
			final Hit hit = attack.getHits().get(i);
			final L2Character target = ((L2Character) hit.getTarget());
			if ((target == null) || target.isDead() || !isInSurroundingRegion(target))
			{
				continue;
			}
			
			if (hit.isMiss())
			{
				notifyAttackAvoid(target, false);
			}
			else
			{
				onHitTarget(target, weapon, hit);
			}
		}
		
		_hitTask = ThreadPool.schedule(() -> onAttackFinish(attack), attackTime - (hitTime1 + hitTime2));
	}
	
	public void onHitTarget(L2Character target, L2Weapon weapon, Hit hit)
	{
		// reduce targets HP
		doAttack(hit.getDamage(), target, null, false, false, hit.isCritical(), false);
		
		// Notify to scripts when the attack has been done.
		EventDispatcher.getInstance().notifyEvent(new OnCreatureAttack(this, target, null), this);
		EventDispatcher.getInstance().notifyEvent(new OnCreatureAttacked(this, target, null), target);
		
		if (_triggerSkills != null)
		{
			for (OptionsSkillHolder holder : _triggerSkills.values())
			{
				if ((!hit.isCritical() && (holder.getSkillType() == OptionsSkillType.ATTACK)) || ((holder.getSkillType() == OptionsSkillType.CRITICAL) && hit.isCritical()))
				{
					if (Rnd.get(100) < holder.getChance())
					{
						SkillCaster.triggerCast(this, target, holder.getSkill(), null, false);
					}
				}
			}
		}
		
		// Launch weapon Special ability effect if available
		if (hit.isCritical() && (weapon != null))
		{
			weapon.applyConditionalSkills(this, target, null, ItemSkillType.ON_CRITICAL_SKILL);
		}
		
		if (isPlayer() && !target.isHpBlocked())
		{
			if (((L2PcInstance) this).isCursedWeaponEquipped())
			{
				// If hit by a cursed weapon, CP is reduced to 0
				target.setCurrentCp(0);
			}
			else if (((L2PcInstance) this).isHero() && target.isPlayer() && target.getActingPlayer().isCursedWeaponEquipped())
			{
				// If a cursed weapon is hit by a Hero, CP is reduced to 0
				target.setCurrentCp(0);
			}
		}
	}
	
	private void onAttackFinish(Attack attack)
	{
		// Recharge any active auto-soulshot tasks for current creature after the attack has successfully hit.
		if (attack.getHits().stream().anyMatch(h -> !h.isMiss()))
		{
			rechargeShots(true, false, false);
		}
		
		// Notify that this character is ready to act for the next attack
		getAI().notifyEvent(CtrlEvent.EVT_READY_TO_ACT);
	}
	
	/**
	 * Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character.
	 */
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			// Abort the attack of the L2Character and send Server->Client ActionFailed packet
			abortAttack();
			if (isPlayer())
			{
				// Send a system message
				sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
			}
		}
	}
	
	/**
	 * Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character.
	 */
	public void breakCast()
	{
		// Break only one skill at a time while casting.
		final SkillCaster skillCaster = getSkillCaster(SkillCaster::isAnyNormalType);
		if ((skillCaster != null) && skillCaster.getSkill().isMagic())
		{
			// Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
			skillCaster.stopCasting(true);
			
			if (isPlayer())
			{
				// Send a system message
				sendPacket(SystemMessageId.YOUR_CASTING_HAS_BEEN_INTERRUPTED);
			}
		}
	}
	
	/**
	 * Manage Forced attack (shift + select target).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>If L2Character or target is in a town area, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed</li>
	 * <li>If target is confused, send a Server->Client packet ActionFailed</li>
	 * <li>If L2Character is a L2ArtefactInstance, send a Server->Client packet ActionFailed</li>
	 * <li>Send a Server->Client packet MyTargetSelected to start attack and Notify AI with AI_INTENTION_ATTACK</li>
	 * </ul>
	 * @param player The L2PcInstance to attack
	 */
	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		if (isInsidePeaceZone(player))
		{
			// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInOlympiadMode() && (player.getTarget() != null) && player.getTarget().isPlayable())
		{
			L2PcInstance target = null;
			final L2Object object = player.getTarget();
			if ((object != null) && object.isPlayable())
			{
				target = object.getActingPlayer();
			}
			
			if ((target == null) || (target.isInOlympiadMode() && (!player.isOlympiadStart() || (player.getOlympiadGameId() != target.getOlympiadGameId()))))
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		if ((player.getTarget() != null) && !player.getTarget().canBeAttacked() && !player.getAccessLevel().allowPeaceAttack())
		{
			// If target is not attackable, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isConfused())
		{
			// If target is confused, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// GeoData Los Check or dz > 1000
		if (!GeoEngine.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.getBlockCheckerArena() != -1)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// Notify AI with AI_INTENTION_ATTACK
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
	}
	
	/**
	 * @param attacker
	 * @return True if inside peace zone.
	 */
	public boolean isInsidePeaceZone(L2Object attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}
	
	public boolean isInsidePeaceZone(L2Object attacker, L2Object target)
	{
		final Instance instanceWorld = getInstanceWorld();
		if ((target == null) || !((target.isPlayable() || target.isFakePlayer()) && attacker.isPlayable()) || ((instanceWorld != null) && instanceWorld.isPvP()))
		{
			return false;
		}
		
		if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			// allows red to be attacked and red to attack flagged players
			if ((target.getActingPlayer() != null) && (target.getActingPlayer().getReputation() < 0))
			{
				return false;
			}
			if ((attacker.getActingPlayer() != null) && (attacker.getActingPlayer().getReputation() < 0) && (target.getActingPlayer() != null) && (target.getActingPlayer().getPvpFlag() > 0))
			{
				return false;
			}
		}
		
		if ((attacker.getActingPlayer() != null) && attacker.getActingPlayer().getAccessLevel().allowPeaceAttack())
		{
			return false;
		}
		
		return (target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE));
	}
	
	/**
	 * @return true if this character is inside an active grid.
	 */
	public boolean isInActiveRegion()
	{
		final L2WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}
	
	/**
	 * @return True if the L2Character has a Party in progress.
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * @return the L2Party object of the L2Character.
	 */
	public L2Party getParty()
	{
		return null;
	}
	
	/**
	 * Add a skill to the L2Character _skills and its Func objects to the calculator set of the L2Character.<br>
	 * <B><U>Concept</U>:</B><br>
	 * All skills own by a L2Character are identified in <B>_skills</B><br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character</li>
	 * </ul>
	 * <B><U>Overridden in</U>:</B>
	 * <ul>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li>
	 * </ul>
	 * @param newSkill The L2Skill to add to the L2Character
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	@Override
	public Skill addSkill(Skill newSkill)
	{
		Skill oldSkill = null;
		if (newSkill != null)
		{
			// Mobius: Keep sublevel on skill level increase.
			final Skill existingSkill = _skills.get(newSkill.getId());
			if ((existingSkill != null) && (existingSkill.getSubLevel() > 0) && (newSkill.getSubLevel() == 0) && (existingSkill.getLevel() < newSkill.getLevel()))
			{
				newSkill = SkillData.getInstance().getSkill(newSkill.getId(), newSkill.getLevel(), existingSkill.getSubLevel());
			}
			
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			// If an old skill has been replaced, remove all its Func objects
			if (oldSkill != null)
			{
				// Stop all effects of that skill
				if (oldSkill.isPassive())
				{
					_effectList.stopSkillEffects(true, oldSkill);
				}
				
				_stat.recalculateStats(true);
			}
			
			if (newSkill.isPassive())
			{
				newSkill.applyEffects(this, this, false, true, false, 0, null);
			}
		}
		return oldSkill;
	}
	
	public Skill removeSkill(Skill skill, boolean cancelEffect)
	{
		return (skill != null) ? removeSkill(skill.getId(), cancelEffect) : null;
	}
	
	public Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	public Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove the skill from the L2Character _skills
		final Skill oldSkill = _skills.remove(skillId);
		// Remove all its Func objects from the L2Character calculator set
		if (oldSkill != null)
		{
			// Stop casting if this skill is used right now
			abortCast(s -> s.getSkill().getId() == skillId);
			
			// Stop effects.
			if (cancelEffect || oldSkill.isToggle() || oldSkill.isPassive())
			{
				stopSkillEffects(true, oldSkill.getId());
				_stat.recalculateStats(true);
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * @return all skills this creature currently has.
	 */
	public final Collection<Skill> getAllSkills()
	{
		return _skills.values();
	}
	
	/**
	 * @return the map containing this character skills.
	 */
	@Override
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Return the level of a skill owned by the L2Character.
	 * @param skillId The identifier of the L2Skill whose level must be returned
	 * @return The level of the L2Skill identified by skillId
	 */
	@Override
	public int getSkillLevel(int skillId)
	{
		final Skill skill = getKnownSkill(skillId);
		return (skill == null) ? 0 : skill.getLevel();
	}
	
	/**
	 * @param skillId The identifier of the L2Skill to check the knowledge
	 * @return the skill from the known skill.
	 */
	@Override
	public Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	/**
	 * Return the number of buffs affecting this L2Character.
	 * @return The number of Buffs affecting this L2Character
	 */
	public int getBuffCount()
	{
		return _effectList.getBuffCount();
	}
	
	public int getDanceCount()
	{
		return _effectList.getDanceCount();
	}
	
	// Quest event ON_SPELL_FNISHED
	public void notifyQuestEventSkillFinished(Skill skill, L2Object target)
	{
		
	}
	
	/**
	 * @return the Level Modifier ((level + 89) / 100).
	 */
	public double getLevelMod()
	{
		// Untested: (lvl + 89 + unk5,5forSkill4.0Else * odyssey_lvl_mod) / 100; odyssey_lvl_mod = (lvl-99) min 0.
		final double defaultLevelMod = ((getLevel() + 89) / 100d);
		return _transform.filter(transform -> !transform.isStance()).map(transform -> transform.getLevelMod(this)).orElse(defaultLevelMod);
	}
	
	private boolean _AIdisabled = false;
	
	/**
	 * Dummy value that gets overriden in Playable.
	 * @return 0
	 */
	public byte getPvpFlag()
	{
		return 0;
	}
	
	public void updatePvPFlag(int value)
	{
		// Overridden in L2PcInstance
	}
	
	/**
	 * @return a multiplier based on weapon random damage
	 */
	public final double getRandomDamageMultiplier()
	{
		final int random = (int) _stat.getValue(Stats.RANDOM_DAMAGE);
		return (1 + ((double) Rnd.get(-random, random) / 100));
	}
	
	public final long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	public long getRangedAttackEndTime()
	{
		return _disableRangedAttackEndTime;
	}
	
	/**
	 * Not Implemented.
	 * @return
	 */
	public abstract int getLevel();
	
	public int getAccuracy()
	{
		return _stat.getAccuracy();
	}
	
	public int getMagicAccuracy()
	{
		return _stat.getMagicAccuracy();
	}
	
	public int getMagicEvasionRate()
	{
		return _stat.getMagicEvasionRate();
	}
	
	public final double getAttackSpeedMultiplier()
	{
		return _stat.getAttackSpeedMultiplier();
	}
	
	public final double getCriticalDmg(int init)
	{
		return _stat.getCriticalDmg(init);
	}
	
	public int getCriticalHit()
	{
		return _stat.getCriticalHit();
	}
	
	public int getEvasionRate()
	{
		return _stat.getEvasionRate();
	}
	
	public final int getMagicalAttackRange(Skill skill)
	{
		return _stat.getMagicalAttackRange(skill);
	}
	
	public final int getMaxCp()
	{
		return _stat.getMaxCp();
	}
	
	public final int getMaxRecoverableCp()
	{
		return _stat.getMaxRecoverableCp();
	}
	
	public int getMAtk()
	{
		return _stat.getMAtk();
	}
	
	public int getMAtkSpd()
	{
		return _stat.getMAtkSpd();
	}
	
	public int getMaxMp()
	{
		return _stat.getMaxMp();
	}
	
	public int getMaxRecoverableMp()
	{
		return _stat.getMaxRecoverableMp();
	}
	
	public int getMaxHp()
	{
		return _stat.getMaxHp();
	}
	
	public int getMaxRecoverableHp()
	{
		return _stat.getMaxRecoverableHp();
	}
	
	public final int getMCriticalHit()
	{
		return _stat.getMCriticalHit();
	}
	
	public int getMDef()
	{
		return _stat.getMDef();
	}
	
	public int getPAtk()
	{
		return _stat.getPAtk();
	}
	
	public int getPAtkSpd()
	{
		return _stat.getPAtkSpd();
	}
	
	public int getPDef()
	{
		return _stat.getPDef();
	}
	
	public final int getPhysicalAttackRange()
	{
		return _stat.getPhysicalAttackRange();
	}
	
	public double getMovementSpeedMultiplier()
	{
		return _stat.getMovementSpeedMultiplier();
	}
	
	public double getRunSpeed()
	{
		return _stat.getRunSpeed();
	}
	
	public double getWalkSpeed()
	{
		return _stat.getWalkSpeed();
	}
	
	public final double getSwimRunSpeed()
	{
		return _stat.getSwimRunSpeed();
	}
	
	public final double getSwimWalkSpeed()
	{
		return _stat.getSwimWalkSpeed();
	}
	
	public double getMoveSpeed()
	{
		return _stat.getMoveSpeed();
	}
	
	public final int getShldDef()
	{
		return _stat.getShldDef();
	}
	
	public int getSTR()
	{
		return _stat.getSTR();
	}
	
	public int getDEX()
	{
		return _stat.getDEX();
	}
	
	public int getCON()
	{
		return _stat.getCON();
	}
	
	public int getINT()
	{
		return _stat.getINT();
	}
	
	public int getWIT()
	{
		return _stat.getWIT();
	}
	
	public int getMEN()
	{
		return _stat.getMEN();
	}
	
	// Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
	public void addStatusListener(L2Character object)
	{
		_status.addStatusListener(object);
	}
	
	public void doAttack(double damage, L2Character target, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		// Check if fake players should aggro each other.
		if (isFakePlayer() && !Config.FAKE_PLAYER_AGGRO_FPC && target.isFakePlayer())
		{
			return;
		}
		
		// Start attack stance and notify being attacked.
		if (target.hasAI())
		{
			target.getAI().clientStartAutoAttack();
			target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
		}
		
		getAI().clientStartAutoAttack();
		
		if (!reflect && !isDOT)
		{
			// RearDamage effect bonus.
			if (isBehind(target))
			{
				damage *= _stat.getValue(Stats.REAR_DAMAGE_RATE, 1);
			}
			
			// Counterattacks happen before damage received.
			if (!target.isDead() && (skill != null))
			{
				Formulas.calcCounterAttack(this, target, skill, true);
				
				// Shield Deflect Magic: Reflect all damage on caster.
				if (skill.isMagic() && (target.getStat().getValue(Stats.VENGEANCE_SKILL_MAGIC_DAMAGE, 0) > Rnd.get(100)))
				{
					reduceCurrentHp(damage, target, skill, isDOT, directlyToHp, critical, true);
					return;
				}
			}
		}
		
		// Target receives the damage.
		target.reduceCurrentHp(damage, this, skill, isDOT, directlyToHp, critical, reflect);
		
		// Check if damage should be reflected or absorbed. When killing blow is made, the target doesn't reflect (vamp too?).
		if (!reflect && !isDOT && !target.isDead())
		{
			int reflectedDamage = 0;
			
			// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
			double reflectPercent = target.getStat().getValue(Stats.REFLECT_DAMAGE_PERCENT, 0) - getStat().getValue(Stats.REFLECT_DAMAGE_PERCENT_DEFENSE, 0);
			if (reflectPercent > 0)
			{
				reflectedDamage = (int) ((reflectPercent / 100.) * damage);
				reflectedDamage = Math.min(reflectedDamage, target.getMaxHp());
				
				// Reflected damage is limited by P.Def/M.Def
				if ((skill != null) && skill.isMagic())
				{
					reflectedDamage = (int) Math.min(reflectedDamage, target.getStat().getMDef() * 1.5);
				}
				else
				{
					reflectedDamage = Math.min(reflectedDamage, target.getStat().getPDef());
				}
			}
			
			// Absorb HP from the damage inflicted
			double absorbPercent = getStat().getValue(Stats.ABSORB_DAMAGE_PERCENT, 0) * target.getStat().getValue(Stats.ABSORB_DAMAGE_DEFENCE, 1);
			if ((absorbPercent > 0) && (Rnd.nextDouble() < _stat.getValue(Stats.ABSORB_DAMAGE_CHANCE)))
			{
				int absorbDamage = (int) Math.min(absorbPercent * damage, _stat.getMaxRecoverableHp() - _status.getCurrentHp());
				absorbDamage = Math.min(absorbDamage, (int) target.getCurrentHp());
				if (absorbDamage > 0)
				{
					setCurrentHp(_status.getCurrentHp() + absorbDamage);
				}
			}
			
			// Absorb MP from the damage inflicted.
			absorbPercent = _stat.getValue(Stats.ABSORB_MANA_DAMAGE_PERCENT, 0);
			if (absorbPercent > 0)
			{
				int absorbDamage = (int) Math.min((absorbPercent / 100.) * damage, _stat.getMaxRecoverableMp() - _status.getCurrentMp());
				absorbDamage = Math.min(absorbDamage, (int) target.getCurrentMp());
				if (absorbDamage > 0)
				{
					setCurrentMp(_status.getCurrentMp() + absorbDamage);
				}
			}
			
			if (reflectedDamage > 0)
			{
				target.doAttack(reflectedDamage, this, skill, isDOT, directlyToHp, critical, true);
			}
		}
		
		// Break casting of target during attack.
		if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
		{
			target.breakAttack();
			target.breakCast();
		}
	}
	
	public void reduceCurrentHp(double value, L2Character attacker, Skill skill)
	{
		reduceCurrentHp(value, attacker, skill, false, false, false, false);
	}
	
	public void reduceCurrentHp(double value, L2Character attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		// Notify of this attack only if there is an attacking creature.
		if (attacker != null)
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnCreatureDamageDealt(attacker, this, value, skill, critical, isDOT, reflect), attacker);
		}
		
		final DamageReturn term = EventDispatcher.getInstance().notifyEvent(new OnCreatureDamageReceived(attacker, this, value, skill, critical, isDOT, reflect), this, DamageReturn.class);
		if (term != null)
		{
			if (term.terminate())
			{
				return;
			}
			else if (term.override())
			{
				value = term.getDamage();
			}
		}
		
		final double damageCap = _stat.getValue(Stats.DAMAGE_LIMIT);
		if (damageCap > 0)
		{
			value = Math.min(value, damageCap);
		}
		
		// Calculate PvP/PvE damage received. It is a post-attack stat.
		if (attacker != null)
		{
			if (attacker.isPlayable())
			{
				value *= (100 + _stat.getValue(Stats.PVP_DAMAGE_TAKEN)) / 100;
			}
			else
			{
				value *= (100 + _stat.getValue(Stats.PVE_DAMAGE_TAKEN)) / 100;
			}
		}
		
		if (Config.CHAMPION_ENABLE && isChampion() && (Config.CHAMPION_HP != 0))
		{
			_status.reduceHp(value / Config.CHAMPION_HP, attacker, (skill == null) || !skill.isToggle(), isDOT, false);
		}
		else if (isPlayer())
		{
			getActingPlayer().getStatus().reduceHp(value, attacker, (skill == null) || !skill.isToggle(), isDOT, false, directlyToHp);
		}
		else
		{
			_status.reduceHp(value, attacker, (skill == null) || !skill.isToggle(), isDOT, false);
		}
		
		if (attacker != null)
		{
			attacker.sendDamageMessage(this, skill, (int) value, critical, false);
		}
	}
	
	public void reduceCurrentMp(double i)
	{
		_status.reduceMp(i);
	}
	
	@Override
	public void removeStatusListener(L2Character object)
	{
		_status.removeStatusListener(object);
	}
	
	protected void stopHpMpRegeneration()
	{
		_status.stopHpMpRegeneration();
	}
	
	public final double getCurrentCp()
	{
		return _status.getCurrentCp();
	}
	
	public final int getCurrentCpPercent()
	{
		return (int) ((_status.getCurrentCp() * 100) / _stat.getMaxCp());
	}
	
	public final void setCurrentCp(double newCp)
	{
		_status.setCurrentCp(newCp);
	}
	
	public final void setCurrentCp(double newCp, boolean broadcast)
	{
		_status.setCurrentCp(newCp, broadcast);
	}
	
	public final double getCurrentHp()
	{
		return _status.getCurrentHp();
	}
	
	public final int getCurrentHpPercent()
	{
		return (int) ((_status.getCurrentHp() * 100) / _stat.getMaxHp());
	}
	
	public final void setCurrentHp(double newHp)
	{
		_status.setCurrentHp(newHp);
	}
	
	public final void setCurrentHp(double newHp, boolean broadcast)
	{
		_status.setCurrentHp(newHp, broadcast);
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		_status.setCurrentHpMp(newHp, newMp);
	}
	
	public final double getCurrentMp()
	{
		return _status.getCurrentMp();
	}
	
	public final int getCurrentMpPercent()
	{
		return (int) ((_status.getCurrentMp() * 100) / _stat.getMaxMp());
	}
	
	public final void setCurrentMp(double newMp)
	{
		_status.setCurrentMp(newMp);
	}
	
	public final void setCurrentMp(double newMp, boolean broadcast)
	{
		_status.setCurrentMp(newMp, false);
	}
	
	/**
	 * @return the max weight that the L2Character can load.
	 */
	public int getMaxLoad()
	{
		if (isPlayer() || isPet())
		{
			// Weight Limit = (CON Modifier*69000) * Skills
			// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
			final double baseLoad = Math.floor(BaseStats.CON.calcBonus(this) * 69000 * Config.ALT_WEIGHT_LIMIT);
			return (int) _stat.getValue(Stats.WEIGHT_LIMIT, baseLoad);
		}
		return 0;
	}
	
	public int getBonusWeightPenalty()
	{
		if (isPlayer() || isPet())
		{
			return (int) _stat.getValue(Stats.WEIGHT_PENALTY, 1);
		}
		return 0;
	}
	
	/**
	 * @return the current weight of the L2Character.
	 */
	public int getCurrentLoad()
	{
		if (isPlayer() || isPet())
		{
			return getInventory().getTotalWeight();
		}
		return 0;
	}
	
	public boolean isChampion()
	{
		return false;
	}
	
	/**
	 * Send system message about damage.
	 * @param target
	 * @param skill
	 * @param damage
	 * @param crit
	 * @param miss
	 */
	public void sendDamageMessage(L2Character target, Skill skill, int damage, boolean crit, boolean miss)
	{
		
	}
	
	public AttributeType getAttackElement()
	{
		return _stat.getAttackElement();
	}
	
	public int getAttackElementValue(AttributeType attackAttribute)
	{
		return _stat.getAttackElementValue(attackAttribute);
	}
	
	public int getDefenseElementValue(AttributeType defenseAttribute)
	{
		return _stat.getDefenseElementValue(defenseAttribute);
	}
	
	public final void startPhysicalAttackMuted()
	{
		abortAttack();
	}
	
	public void disableCoreAI(boolean val)
	{
		_AIdisabled = val;
	}
	
	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}
	
	/**
	 * @return true
	 */
	public boolean giveRaidCurse()
	{
		return true;
	}
	
	/**
	 * Check if target is affected with special buff
	 * @param flag int
	 * @return boolean
	 * @see CharEffectList#isAffected(EffectFlag)
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return _effectList.isAffected(flag);
	}
	
	public void broadcastSocialAction(int id)
	{
		broadcastPacket(new SocialAction(getObjectId(), id));
	}
	
	public Team getTeam()
	{
		return _team;
	}
	
	public void setTeam(Team team)
	{
		_team = team;
	}
	
	public void addOverrideCond(PcCondOverride... excs)
	{
		for (PcCondOverride exc : excs)
		{
			_exceptions |= exc.getMask();
		}
	}
	
	public void removeOverridedCond(PcCondOverride... excs)
	{
		for (PcCondOverride exc : excs)
		{
			_exceptions &= ~exc.getMask();
		}
	}
	
	public boolean canOverrideCond(PcCondOverride excs)
	{
		return (_exceptions & excs.getMask()) == excs.getMask();
	}
	
	public void setOverrideCond(long masks)
	{
		_exceptions = masks;
	}
	
	public void setLethalable(boolean val)
	{
		_lethalable = val;
	}
	
	public boolean isLethalable()
	{
		return _lethalable;
	}
	
	public boolean hasTriggerSkills()
	{
		return (_triggerSkills != null) && !_triggerSkills.isEmpty();
	}
	
	public Map<Integer, OptionsSkillHolder> getTriggerSkills()
	{
		if (_triggerSkills == null)
		{
			synchronized (this)
			{
				if (_triggerSkills == null)
				{
					_triggerSkills = new ConcurrentHashMap<>();
				}
			}
		}
		return _triggerSkills;
	}
	
	public void addTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().put(holder.getSkillId(), holder);
	}
	
	public void removeTriggerSkill(OptionsSkillHolder holder)
	{
		getTriggerSkills().remove(holder.getSkillId());
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @return {@code true} if current player can revive and shows 'To Village' button upon death, {@code false} otherwise.
	 */
	public boolean canRevive()
	{
		return true;
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @param val
	 */
	public void setCanRevive(boolean val)
	{
	}
	
	/**
	 * Dummy method overriden in {@link L2Attackable}
	 * @return {@code true} if there is a loot to sweep, {@code false} otherwise.
	 */
	public boolean isSweepActive()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @return {@code true} if player is on event, {@code false} otherwise.
	 */
	public boolean isOnEvent()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @return the clan id of current character.
	 */
	public int getClanId()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @return the clan of current character.
	 */
	public L2Clan getClan()
	{
		return null;
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @return {@code true} if player is in academy, {@code false} otherwise.
	 */
	public boolean isAcademyMember()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @return the pledge type of current character.
	 */
	public int getPledgeType()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link L2PcInstance}
	 * @return the alliance id of current character.
	 */
	public int getAllyId()
	{
		return 0;
	}
	
	/**
	 * Notifies to listeners that current character avoid attack.
	 * @param target
	 * @param isDot
	 */
	public void notifyAttackAvoid(L2Character target, boolean isDot)
	{
		EventDispatcher.getInstance().notifyEventAsync(new OnCreatureAttackAvoid(this, target, isDot), target);
	}
	
	/**
	 * @return {@link WeaponType} of current character's weapon or basic weapon type.
	 */
	public final WeaponType getAttackType()
	{
		final L2Weapon weapon = getActiveWeaponItem();
		if (weapon != null)
		{
			return weapon.getItemType();
		}
		
		final WeaponType defaultWeaponType = _template.getBaseAttackType();
		return _transform.map(transform -> transform.getBaseAttackType(this, defaultWeaponType)).orElse(defaultWeaponType);
	}
	
	public final boolean isInCategory(CategoryType type)
	{
		return CategoryData.getInstance().isInCategory(type, getId());
	}
	
	public final boolean isInOneOfCategory(CategoryType... types)
	{
		for (CategoryType type : types)
		{
			if (CategoryData.getInstance().isInCategory(type, getId()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @return the character that summoned this NPC.
	 */
	public L2Character getSummoner()
	{
		return _summoner;
	}
	
	/**
	 * @param summoner the summoner of this NPC.
	 */
	public void setSummoner(L2Character summoner)
	{
		_summoner = summoner;
	}
	
	/**
	 * Adds a summoned NPC.
	 * @param npc the summoned NPC
	 */
	public final void addSummonedNpc(L2Npc npc)
	{
		if (_summonedNpcs == null)
		{
			synchronized (this)
			{
				if (_summonedNpcs == null)
				{
					_summonedNpcs = new ConcurrentHashMap<>();
				}
			}
		}
		
		_summonedNpcs.put(npc.getObjectId(), npc);
		
		npc.setSummoner(this);
	}
	
	/**
	 * Removes a summoned NPC by object ID.
	 * @param objectId the summoned NPC object ID
	 */
	public final void removeSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.remove(objectId);
		}
	}
	
	/**
	 * Gets the summoned NPCs.
	 * @return the summoned NPCs
	 */
	public final Collection<L2Npc> getSummonedNpcs()
	{
		return _summonedNpcs != null ? _summonedNpcs.values() : Collections.emptyList();
	}
	
	/**
	 * Gets the summoned NPC by object ID.
	 * @param objectId the summoned NPC object ID
	 * @return the summoned NPC
	 */
	public final L2Npc getSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			return _summonedNpcs.get(objectId);
		}
		return null;
	}
	
	/**
	 * Gets the summoned NPC count.
	 * @return the summoned NPC count
	 */
	public final int getSummonedNpcCount()
	{
		return _summonedNpcs != null ? _summonedNpcs.size() : 0;
	}
	
	/**
	 * Resets the summoned NPCs list.
	 */
	public final void resetSummonedNpcs()
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.clear();
		}
	}
	
	@Override
	public boolean isCharacter()
	{
		return true;
	}
	
	public int getMinShopDistance()
	{
		return 0;
	}
	
	public Collection<SkillCaster> getSkillCasters()
	{
		return _skillCasters.values();
	}
	
	public SkillCaster addSkillCaster(SkillCastingType castingType, SkillCaster skillCaster)
	{
		return _skillCasters.put(castingType, skillCaster);
	}
	
	public SkillCaster removeSkillCaster(SkillCastingType castingType)
	{
		return _skillCasters.remove(castingType);
	}
	
	@SafeVarargs
	public final List<SkillCaster> getSkillCasters(Predicate<SkillCaster> filter, Predicate<SkillCaster>... filters)
	{
		for (Predicate<SkillCaster> additionalFilter : filters)
		{
			filter = filter.and(additionalFilter);
		}
		
		return _skillCasters.values().stream().filter(filter).collect(Collectors.toList());
	}
	
	@SafeVarargs
	public final SkillCaster getSkillCaster(Predicate<SkillCaster> filter, Predicate<SkillCaster>... filters)
	{
		for (Predicate<SkillCaster> additionalFilter : filters)
		{
			filter = filter.and(additionalFilter);
		}
		
		return _skillCasters.values().stream().filter(filter).findAny().orElse(null);
	}
	
	/**
	 * @return {@code true} if current character is casting channeling skill, {@code false} otherwise.
	 */
	public final boolean isChanneling()
	{
		return (_channelizer != null) && _channelizer.isChanneling();
	}
	
	public final SkillChannelizer getSkillChannelizer()
	{
		if (_channelizer == null)
		{
			_channelizer = new SkillChannelizer(this);
		}
		return _channelizer;
	}
	
	/**
	 * @return {@code true} if current character is affected by channeling skill, {@code false} otherwise.
	 */
	public final boolean isChannelized()
	{
		return (_channelized != null) && !_channelized.isChannelized();
	}
	
	public final SkillChannelized getSkillChannelized()
	{
		if (_channelized == null)
		{
			_channelized = new SkillChannelized();
		}
		return _channelized;
	}
	
	public void addIgnoreSkillEffects(SkillHolder holder)
	{
		final IgnoreSkillHolder ignoreSkillHolder = getIgnoreSkillEffects().get(holder.getSkillId());
		if (ignoreSkillHolder != null)
		{
			ignoreSkillHolder.increaseInstances();
			return;
		}
		getIgnoreSkillEffects().put(holder.getSkillId(), new IgnoreSkillHolder(holder));
	}
	
	public void removeIgnoreSkillEffects(SkillHolder holder)
	{
		final IgnoreSkillHolder ignoreSkillHolder = getIgnoreSkillEffects().get(holder.getSkillId());
		if ((ignoreSkillHolder != null) && (ignoreSkillHolder.decreaseInstances() < 1))
		{
			getIgnoreSkillEffects().remove(holder.getSkillId());
		}
	}
	
	public boolean isIgnoringSkillEffects(int skillId, int skillLvl)
	{
		if (_ignoreSkillEffects != null)
		{
			final SkillHolder holder = getIgnoreSkillEffects().get(skillId);
			return ((holder != null) && ((holder.getSkillLevel() < 1) || (holder.getSkillLevel() == skillLvl)));
		}
		return false;
	}
	
	private Map<Integer, IgnoreSkillHolder> getIgnoreSkillEffects()
	{
		if (_ignoreSkillEffects == null)
		{
			synchronized (this)
			{
				if (_ignoreSkillEffects == null)
				{
					_ignoreSkillEffects = new ConcurrentHashMap<>();
				}
			}
		}
		return _ignoreSkillEffects;
	}
	
	@Override
	public Queue<AbstractEventListener> getListeners(EventType type)
	{
		final Queue<AbstractEventListener> objectListenres = super.getListeners(type);
		final Queue<AbstractEventListener> templateListeners = _template.getListeners(type);
		final Queue<AbstractEventListener> globalListeners = isNpc() && !isMonster() ? Containers.Npcs().getListeners(type) : isMonster() ? Containers.Monsters().getListeners(type) : isPlayer() ? Containers.Players().getListeners(type) : EmptyQueue.emptyQueue();
		
		// Attempt to do not create collection
		if (objectListenres.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return EmptyQueue.emptyQueue();
		}
		else if (!objectListenres.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return objectListenres;
		}
		else if (!templateListeners.isEmpty() && objectListenres.isEmpty() && globalListeners.isEmpty())
		{
			return templateListeners;
		}
		else if (!globalListeners.isEmpty() && objectListenres.isEmpty() && templateListeners.isEmpty())
		{
			return globalListeners;
		}
		
		final Queue<AbstractEventListener> both = new LinkedBlockingDeque<>(objectListenres.size() + templateListeners.size() + globalListeners.size());
		both.addAll(objectListenres);
		both.addAll(templateListeners);
		both.addAll(globalListeners);
		return both;
	}
	
	public Race getRace()
	{
		return _template.getRace();
	}
	
	@Override
	public final void setXYZ(int newX, int newY, int newZ)
	{
		final ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
		final ZoneRegion newZoneRegion = ZoneManager.getInstance().getRegion(newX, newY);
		
		// Mobius: Prevent moving to nonexistent regions.
		if (newZoneRegion == null)
		{
			return;
		}
		
		if (oldZoneRegion != newZoneRegion)
		{
			oldZoneRegion.removeFromZones(this);
			newZoneRegion.revalidateZones(this);
		}
		
		super.setXYZ(newX, newY, newZ);
	}
	
	public final Map<Integer, Integer> getKnownRelations()
	{
		return _knownRelations;
	}
	
	@Override
	public boolean isTargetable()
	{
		return super.isTargetable() && !isAffected(EffectFlag.UNTARGETABLE);
	}
	
	public boolean isTargetingDisabled()
	{
		return isAffected(EffectFlag.TARGETING_DISABLED);
	}
	
	public boolean cannotEscape()
	{
		return isAffected(EffectFlag.CANNOT_ESCAPE);
	}
	
	/**
	 * Sets amount of debuffs that player can avoid
	 * @param times
	 */
	public void setAbnormalShieldBlocks(int times)
	{
		_abnormalShieldBlocks.set(times);
	}
	
	/**
	 * @return the amount of debuffs that player can avoid
	 */
	public int getAbnormalShieldBlocks()
	{
		return _abnormalShieldBlocks.get();
	}
	
	/**
	 * @return the amount of debuffs that player can avoid
	 */
	public int decrementAbnormalShieldBlocks()
	{
		return _abnormalShieldBlocks.decrementAndGet();
	}
	
	public boolean hasAbnormalType(AbnormalType abnormalType)
	{
		return _effectList.hasAbnormalType(abnormalType);
	}
	
	public void addBlockActionsAllowedSkill(int skillId)
	{
		if (_blockActionsAllowedSkills == null)
		{
			synchronized (this)
			{
				if (_blockActionsAllowedSkills == null)
				{
					_blockActionsAllowedSkills = new ConcurrentHashMap<>();
				}
			}
		}
		_blockActionsAllowedSkills.computeIfAbsent(skillId, k -> new AtomicInteger()).incrementAndGet();
	}
	
	public void removeBlockActionsAllowedSkill(int skillId)
	{
		if (_blockActionsAllowedSkills != null)
		{
			_blockActionsAllowedSkills.computeIfPresent(skillId, (k, v) -> v.decrementAndGet() != 0 ? v : null);
		}
	}
	
	public boolean isBlockedActionsAllowedSkill(Skill skill)
	{
		return (_blockActionsAllowedSkills != null) && _blockActionsAllowedSkills.containsKey(skill.getId());
	}
	
	/**
	 * Initialize creature container that looks up for creatures around its owner, and notifies with onCreatureSee upon discovery.<br>
	 * @param range
	 */
	public void initSeenCreatures(int range)
	{
		initSeenCreatures(range, null);
	}
	
	/**
	 * Initialize creature container that looks up for creatures around its owner, and notifies with onCreatureSee upon discovery.<br>
	 * <i>The condition can be null</i>
	 * @param range
	 * @param condition
	 */
	public void initSeenCreatures(int range, Predicate<L2Character> condition)
	{
		if (_seenCreatures == null)
		{
			synchronized (this)
			{
				if (_seenCreatures == null)
				{
					_seenCreatures = new CreatureContainer(this, range, condition);
				}
			}
		}
	}
	
	public CreatureContainer getSeenCreatures()
	{
		return _seenCreatures;
	}
	
	public MoveType getMoveType()
	{
		if (isMoving() && _isRunning)
		{
			return MoveType.RUNNING;
		}
		else if (isMoving() && !_isRunning)
		{
			return MoveType.WALKING;
		}
		return MoveType.STANDING;
	}
	
	protected final void computeStatusUpdate(StatusUpdate su, StatusUpdateType type)
	{
		final int newValue = type.getValue(this);
		_statusUpdates.compute(type, (key, oldValue) ->
		{
			if ((oldValue == null) || (oldValue != newValue))
			{
				su.addUpdate(type, newValue);
				return newValue;
			}
			return oldValue;
		});
	}
	
	protected final void addStatusUpdateValue(StatusUpdateType type)
	{
		_statusUpdates.put(type, type.getValue(this));
	}
	
	protected void initStatusUpdateCache()
	{
		addStatusUpdateValue(StatusUpdateType.MAX_HP);
		addStatusUpdateValue(StatusUpdateType.MAX_MP);
		addStatusUpdateValue(StatusUpdateType.CUR_HP);
		addStatusUpdateValue(StatusUpdateType.CUR_MP);
	}
	
	/**
	 * Checks if the creature has basic property resist towards mesmerizing debuffs.
	 * @return {@code true}.
	 */
	public boolean hasBasicPropertyResist()
	{
		return true;
	}
	
	/**
	 * Gets the basic property resist.
	 * @param basicProperty the basic property
	 * @return the basic property resist
	 */
	public BasicPropertyResist getBasicPropertyResist(BasicProperty basicProperty)
	{
		if (_basicPropertyResists == null)
		{
			synchronized (this)
			{
				if (_basicPropertyResists == null)
				{
					_basicPropertyResists = new ConcurrentHashMap<>();
				}
			}
		}
		
		return _basicPropertyResists.computeIfAbsent(basicProperty, k -> new BasicPropertyResist());
	}
	
	public int getReputation()
	{
		return _reputation;
	}
	
	public void setReputation(int reputation)
	{
		_reputation = reputation;
	}
	
	public boolean isChargedShot(ShotType type)
	{
		return _chargedShots.contains(type);
	}
	
	/**
	 * @param type of the shot to charge
	 * @return {@code true} if there was no shot of this type charged before, {@code false} otherwise.
	 */
	public boolean chargeShot(ShotType type)
	{
		return _chargedShots.add(type);
	}
	
	/**
	 * @param type of the shot to uncharge
	 * @return {@code true} if there was a charged shot of this type, {@code false} otherwise.
	 */
	public boolean unchargeShot(ShotType type)
	{
		return _chargedShots.remove(type);
	}
	
	public void unchargeAllShots()
	{
		_chargedShots = EnumSet.noneOf(ShotType.class);
	}
	
	public void rechargeShots(boolean physical, boolean magic, boolean fish)
	{
		// Dummy method to be overriden.
	}
	
	public void setCursorKeyMovement(boolean value)
	{
		_cursorKeyMovement = value;
	}
	
	public void setCursorKeyMovementActive(boolean value)
	{
		_cursorKeyMovementActive = value;
	}
	
	public boolean isCursorKeyMovementActive()
	{
		return _cursorKeyMovementActive;
	}
	
	public List<L2ItemInstance> getFakePlayerDrops()
	{
		return _fakePlayerDrops;
	}
}
