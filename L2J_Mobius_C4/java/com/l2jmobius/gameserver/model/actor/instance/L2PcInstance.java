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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import com.l2jmobius.Config;
import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.GeoData;
import com.l2jmobius.gameserver.ItemsAutoDestroy;
import com.l2jmobius.gameserver.LoginServerThread;
import com.l2jmobius.gameserver.Olympiad;
import com.l2jmobius.gameserver.RecipeController;
import com.l2jmobius.gameserver.SevenSignsFestival;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.Universe;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.ai.L2CharacterAI;
import com.l2jmobius.gameserver.ai.L2PlayerAI;
import com.l2jmobius.gameserver.ai.L2SummonAI;
import com.l2jmobius.gameserver.cache.HtmCache;
import com.l2jmobius.gameserver.cache.WarehouseCacheManager;
import com.l2jmobius.gameserver.communitybbs.BB.Forum;
import com.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.l2jmobius.gameserver.communitybbs.Manager.RegionBBSManager;
import com.l2jmobius.gameserver.datatables.CharTemplateTable;
import com.l2jmobius.gameserver.datatables.ClanTable;
import com.l2jmobius.gameserver.datatables.FishTable;
import com.l2jmobius.gameserver.datatables.GmListTable;
import com.l2jmobius.gameserver.datatables.HennaTable;
import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.datatables.MapRegionTable;
import com.l2jmobius.gameserver.datatables.NobleSkillTable;
import com.l2jmobius.gameserver.datatables.NpcTable;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.datatables.SkillTreeTable;
import com.l2jmobius.gameserver.handler.IItemHandler;
import com.l2jmobius.gameserver.handler.ItemHandler;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import com.l2jmobius.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jmobius.gameserver.instancemanager.QuestManager;
import com.l2jmobius.gameserver.instancemanager.SiegeManager;
import com.l2jmobius.gameserver.model.BlockList;
import com.l2jmobius.gameserver.model.FishData;
import com.l2jmobius.gameserver.model.Inventory;
import com.l2jmobius.gameserver.model.ItemContainer;
import com.l2jmobius.gameserver.model.L2Attackable;
import com.l2jmobius.gameserver.model.L2CharPosition;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2ClanMember;
import com.l2jmobius.gameserver.model.L2Effect;
import com.l2jmobius.gameserver.model.L2Fishing;
import com.l2jmobius.gameserver.model.L2HennaInstance;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2Macro;
import com.l2jmobius.gameserver.model.L2ManufactureList;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Party;
import com.l2jmobius.gameserver.model.L2PetData;
import com.l2jmobius.gameserver.model.L2PetDataTable;
import com.l2jmobius.gameserver.model.L2Radar;
import com.l2jmobius.gameserver.model.L2RecipeList;
import com.l2jmobius.gameserver.model.L2Request;
import com.l2jmobius.gameserver.model.L2ShortCut;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Skill.SkillTargetType;
import com.l2jmobius.gameserver.model.L2Skill.SkillType;
import com.l2jmobius.gameserver.model.L2SkillLearn;
import com.l2jmobius.gameserver.model.L2Summon;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.L2WorldRegion;
import com.l2jmobius.gameserver.model.MacroList;
import com.l2jmobius.gameserver.model.PartyMatchRoom;
import com.l2jmobius.gameserver.model.PartyMatchRoomList;
import com.l2jmobius.gameserver.model.PcFreight;
import com.l2jmobius.gameserver.model.PcInventory;
import com.l2jmobius.gameserver.model.PcWarehouse;
import com.l2jmobius.gameserver.model.PetInventory;
import com.l2jmobius.gameserver.model.ShortCuts;
import com.l2jmobius.gameserver.model.TradeList;
import com.l2jmobius.gameserver.model.actor.appearance.PcAppearance;
import com.l2jmobius.gameserver.model.actor.knownlist.PcKnownList;
import com.l2jmobius.gameserver.model.actor.stat.PcStat;
import com.l2jmobius.gameserver.model.actor.status.PcStatus;
import com.l2jmobius.gameserver.model.base.ClassId;
import com.l2jmobius.gameserver.model.base.Experience;
import com.l2jmobius.gameserver.model.base.Race;
import com.l2jmobius.gameserver.model.base.SubClass;
import com.l2jmobius.gameserver.model.entity.Castle;
import com.l2jmobius.gameserver.model.entity.Hero;
import com.l2jmobius.gameserver.model.entity.L2Event;
import com.l2jmobius.gameserver.model.entity.Siege;
import com.l2jmobius.gameserver.model.entity.TvTEvent;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.network.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import com.l2jmobius.gameserver.network.serverpackets.CharInfo;
import com.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import com.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import com.l2jmobius.gameserver.network.serverpackets.ExFishingEnd;
import com.l2jmobius.gameserver.network.serverpackets.ExFishingStart;
import com.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import com.l2jmobius.gameserver.network.serverpackets.ExOlympiadMode;
import com.l2jmobius.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import com.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jmobius.gameserver.network.serverpackets.FriendList;
import com.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.ItemList;
import com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import com.l2jmobius.gameserver.network.serverpackets.MagicEffectIcons;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillCanceld;
import com.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.NicknameChanged;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jmobius.gameserver.network.serverpackets.ObservationMode;
import com.l2jmobius.gameserver.network.serverpackets.ObservationReturn;
import com.l2jmobius.gameserver.network.serverpackets.PartySmallWindowUpdate;
import com.l2jmobius.gameserver.network.serverpackets.PartySpelled;
import com.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2jmobius.gameserver.network.serverpackets.PrivateStoreListBuy;
import com.l2jmobius.gameserver.network.serverpackets.PrivateStoreListSell;
import com.l2jmobius.gameserver.network.serverpackets.QuestList;
import com.l2jmobius.gameserver.network.serverpackets.RecipeShopSellList;
import com.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import com.l2jmobius.gameserver.network.serverpackets.Ride;
import com.l2jmobius.gameserver.network.serverpackets.SendTradeDone;
import com.l2jmobius.gameserver.network.serverpackets.ServerClose;
import com.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import com.l2jmobius.gameserver.network.serverpackets.ShortCutInit;
import com.l2jmobius.gameserver.network.serverpackets.SkillList;
import com.l2jmobius.gameserver.network.serverpackets.Snoop;
import com.l2jmobius.gameserver.network.serverpackets.SocialAction;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.StopMove;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.TargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.TargetUnselected;
import com.l2jmobius.gameserver.network.serverpackets.TradeStart;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;
import com.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import com.l2jmobius.gameserver.skills.BaseStats;
import com.l2jmobius.gameserver.skills.Formulas;
import com.l2jmobius.gameserver.skills.Stats;
import com.l2jmobius.gameserver.templates.L2Armor;
import com.l2jmobius.gameserver.templates.L2ArmorType;
import com.l2jmobius.gameserver.templates.L2Henna;
import com.l2jmobius.gameserver.templates.L2Item;
import com.l2jmobius.gameserver.templates.L2PcTemplate;
import com.l2jmobius.gameserver.templates.L2Weapon;
import com.l2jmobius.gameserver.templates.L2WeaponType;
import com.l2jmobius.gameserver.util.Broadcast;
import com.l2jmobius.gameserver.util.FloodProtectors;
import com.l2jmobius.gameserver.util.Util;
import com.l2jmobius.util.Point3D;
import com.l2jmobius.util.Rnd;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

/**
 * This class represents all player characters in the world. There is always a client-thread connected to this (except if a player-store is activated upon logout).<BR>
 * <BR>
 * @version $Revision: 1.66.2.41.2.33 $ $Date: 2005/04/11 10:06:09 $
 */
public final class L2PcInstance extends L2PlayableInstance
{
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?";
	private static final String RESTORE_SKILLS_FOR_CHAR_ALT_SUBCLASS = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? ORDER BY skill_id";
	
	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (char_obj_id,skill_id,skill_level,skill_name,class_index) VALUES (?,?,?,?,?)";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?";
	
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (char_obj_id,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_EFFECT_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type FROM character_skills_save WHERE char_obj_id=? AND class_index=? AND restore_type <= 0 ORDER BY buff_index ASC";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime FROM character_skills_save WHERE char_obj_id=? AND class_index=? AND restore_type=1 ORDER BY buff_index ASC";
	private static final String RESTORE_SKILL_SAVE_ALT_SUBCLASS = "SELECT skill_id,reuse_delay,systime FROM character_skills_save WHERE char_obj_id=? AND restore_type=1 ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=?";
	
	private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,acc,crit,evasion,mAtk,mDef,mSpd,pAtk,pDef,pSpd,runSpd,walkSpd,str,con,dex,_int,men,wit,face,hairStyle,hairColor,sex,movement_multiplier,attack_speed_multiplier,colRad,colHeight,exp,sp,karma,pvpkills,pkkills,clanid,maxload,race,classid,deletetime,cancraft,title,accesslevel,online,clan_privs,wantspeace,base_class,newbie,nobless,last_recom_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,str=?,con=?,dex=?,_int=?,men=?,wit=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,sp=?,karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,maxload=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,clan_privs=?,wantspeace=?,clan_join_expiry_time=?,clan_create_expiry_time=?,base_class=?,onlinetime=?,in_jail=?,jail_timer=?,newbie=?,nobless=?,last_recom_date=?,varka_ketra_ally=?,aio_buffer=?,char_name=? WHERE obj_Id=?";
	private static final String RESTORE_CHARACTER = "SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, clan_join_expiry_time, clan_create_expiry_time, base_class, onlinetime, in_jail, jail_timer, newbie, nobless, last_recom_date, varka_ketra_ally, aio_buffer FROM characters WHERE obj_Id=?";
	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE char_obj_id=? ORDER BY class_index ASC";
	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (char_obj_id,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE char_obj_id=? AND class_index =?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE char_obj_id=? AND class_index=?";
	
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (char_obj_id,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE char_obj_id=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?";
	
	private static final String RESTORE_CHAR_RECOMS = "SELECT char_id,target_id FROM character_recommends WHERE char_id=?";
	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (char_id,target_id) VALUES (?,?)";
	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE char_id=?";
	
	public static final int REQUEST_TIMEOUT = 15;
	
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;
	
	/** The table containing all minimum level needed for each Expertise (None, D, C, B, A, S) */
	private static final int[] EXPERTISE_LEVELS =
	{
		SkillTreeTable.getInstance().getExpertiseLevel(0), // NONE
		SkillTreeTable.getInstance().getExpertiseLevel(1), // D
		SkillTreeTable.getInstance().getExpertiseLevel(2), // C
		SkillTreeTable.getInstance().getExpertiseLevel(3), // B
		SkillTreeTable.getInstance().getExpertiseLevel(4), // A
		SkillTreeTable.getInstance().getExpertiseLevel(5), // S
	};
	
	private static final int[] COMMON_CRAFT_LEVELS =
	{
		5,
		20,
		28,
		36,
		43,
		49,
		55,
		62,
		70
	};
	
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}
		
		public L2PcInstance getPlayer()
		{
			
			return L2PcInstance.this;
			
		}
		
		public void doPickupItem(L2Object object)
		
		{
			L2PcInstance.this.doPickupItem(object);
		}
		
		public void doInteract(L2Character target)
		
		{
			L2PcInstance.this.doInteract(target);
		}
		
		@Override
		public void doAttack(L2Character target)
		
		{
			super.doAttack(target);
			
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			
			getPlayer().setRecentFakeDeath(false);
			for (final L2CubicInstance cubic : getCubics().values())
			{
				if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction(target);
				}
			}
			
		}
		
		@Override
		public void doCast(L2Skill skill)
		
		{
			
			super.doCast(skill);
			
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			
			getPlayer().setRecentFakeDeath(false);
			
			if (skill == null)
			{
				return;
			}
			
			if (!skill.isOffensive())
			{
				return;
			}
			
			final L2Object mainTarget = skill.getFirstOfTargetList(L2PcInstance.this);
			
			// the code doesn't now support multiple targets
			
			if ((mainTarget == null) || !(mainTarget instanceof L2Character))
			{
				return;
			}
			
			for (final L2CubicInstance cubic : getCubics().values())
			{
				
				if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction((L2Character) mainTarget);
				}
			}
			
		}
	}
	
	private L2GameClient _client;
	
	private PcAppearance _appearance;
	
	/** The Identifier of the L2PcInstance */
	private int _charId = 0x00030b7a;
	
	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;
	
	/** The Experience of the L2PcInstance before the last Death Penalty */
	private long _expBeforeDeath = 0;
	
	private int _charges = 0;
	
	private int _curWeightPenalty = 0;
	
	/** The Siege state of the L2PcInstance */
	private byte _siegeState = 0;
	
	private int _clanPrivileges = 0;
	
	public int _telemode = 0;
	
	/** The PvP Flag state of the L2PcInstance (0=White, 1=Purple) */
	private int _pvpFlag;
	
	/** The Karma of the L2PcInstance (if higher than 0, the name of the L2PcInstance appears in red) */
	private int _karma;
	
	/** The number of player killed during a PvP (the player killed was PvP Flagged) */
	private int _pvpKills;
	
	/** The PK counter of the L2PcInstance (= Number of non PvP Flagged player killed) */
	private int _pkKills;
	
	/** The number of recommendation obtained by the L2PcInstance */
	private int _recomHave; // how much I was recommended by others
	
	/** The number of recommendation that the L2PcInstance can give */
	private int _recomLeft; // how many recomendations I can give to others
	
	/** Date when recom points were updated last time */
	private long _lastRecomUpdate;
	
	/** List with the recomendations that I've give */
	private final List<Integer> _recomChars = new FastList<>();
	
	// Friend list
	private final List<Friend> _friendList = new FastList<>();
	
	private long _deleteTimer;
	private final PcInventory _inventory = new PcInventory(this);
	private PcWarehouse _warehouse;
	private final PcFreight _freight = new PcFreight(this);
	
	/** True if the L2PcInstance is sitting */
	private boolean _waitTypeSitting;
	
	/** True if the L2PcInstance is using the relax skill */
	protected boolean _relax;
	
	/** Novice State */
	public static byte NONE = 0;
	public static byte NEW = 1;
	public static byte OLD = 2;
	private byte _newbieState = 0;
	
	/** The table containing all Quests began by the L2PcInstance */
	private final Map<String, QuestState> _quests = new FastMap<>();
	
	/** The list containing all shortCuts of this L2PcInstance */
	private final ShortCuts _shortCuts = new ShortCuts(this);
	
	/** The list containing all macroses of this L2PcInstance */
	private final MacroList _macroses = new MacroList(this);
	
	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private L2ManufactureList _createList;
	private TradeList _sellList;
	private TradeList _buyList;
	
	/** The Private Store type of the L2PcInstance (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5) */
	private int _privatestore;
	
	private final List<L2PcInstance> _SnoopListener = new FastList<>();
	private final List<L2PcInstance> _SnoopedPlayer = new FastList<>();
	
	private ClassId _skillLearningClassId;
	
	// hennas
	private final L2HennaInstance[] _henna = new L2HennaInstance[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;
	
	/** The L2Summon of the L2PcInstance */
	private L2Summon _summon = null;
	
	// apparently, a L2PcInstance CAN have both a summon AND a tamed beast at the same time!!
	private L2TamedBeastInstance _tamedBeast = null;
	
	// client radar
	// TODO: This needs to be better intergrated and saved/loaded
	private L2Radar _radar;
	
	private L2Party _party;
	
	// Party matching
	private int _partyroom = 0;
	
	/** The Clan Identifier of the L2PcInstance */
	private int _clanId;
	
	/** The Clan object of the L2PcInstance */
	private L2Clan _clan;
	
	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;
	
	private boolean _isGm;
	private int _accessLevel;
	
	private boolean _chatBanned = false; // Chat Banned
	private ScheduledFuture<?> _chatUnbanTask = null;
	private boolean _messageRefusal = false; // message refusal mode
	private boolean _dietMode = false; // ignore weight penalty
	private boolean _tradeRefusal = false; // Trade refusal
	private boolean _exchangeRefusal = false; // Exchange refusal
	
	public boolean _exploring = false;
	
	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private L2PcInstance _activeRequester;
	private long _requestExpireTime = 0;
	private final L2Request _request = new L2Request(this);
	private L2ItemInstance _arrowItem;
	
	// Used for protection after teleport
	private long _protectEndTime = 0;
	
	// protects a char from agro mobs when getting up from fake death
	
	private long _recentFakeDeathEndTime = 0;
	
	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equiped) */
	private L2Weapon _fistsWeaponItem;
	
	private long _uptime;
	private String _accountName;
	
	private final Map<Integer, String> _chars = new FastMap<>();
	
	public byte _updateKnownCounter = 0;
	
	/** The table containing all L2RecipeList of the L2PcInstance */
	private final Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<>();
	private final Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<>();
	
	private int _mountType;
	private int _mountNpcId;
	private int _mountLevel;
	
	/** The current higher Expertise of the L2PcInstance (None=0, D=1, C=2, B=3, A=4, S=5) */
	private int _expertiseIndex; // index in EXPERTISE_LEVELS
	private int _expertisePenalty = 0;
	
	private int _lootInvitation = -1;
	
	private L2ItemInstance _activeEnchantItem = null;
	
	// Online status
	private long _onlineTime;
	private long _onlineBeginTime;
	private boolean _isOnline = false;
	
	protected boolean _inventoryDisable = false;
	
	protected Map<Integer, L2CubicInstance> _cubics = new FastMap<Integer, L2CubicInstance>().shared();
	
	/** The L2FolkInstance corresponding to the last Folk wich one the player talked. */
	private L2FolkInstance _lastFolkNpc = null;
	
	protected FastSet<Integer> _activeSoulShots = new FastSet<Integer>().shared();
	
	/** Location before entering Observer Mode */
	private int _obsX;
	private int _obsY;
	private int _obsZ;
	private boolean _observerMode = false;
	
	/** Event parameters */
	public int eventX;
	public int eventY;
	public int eventZ;
	public int eventkarma;
	public int eventpvpkills;
	public int eventpkkills;
	public String eventTitle;
	public LinkedList<String> kills = new LinkedList<>();
	public boolean eventSitForced = false;
	public boolean atEvent = false;
	
	/** new loto ticket **/
	public int _loto[] = new int[5];
	/** new race ticket **/
	public int _race[] = new int[2];
	
	private final BlockList _blockList = new BlockList();
	
	private boolean _inOfflineMode = false;
	private long _offlineShopStart = 0;
	
	private boolean _hero;
	private int _team = 0;
	private int _wantsPeace = 0;
	
	private boolean _noble = false;
	
	private boolean _inOlympiadMode = false;
	private boolean _OlympiadStart = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	public int dmgDealt = 0;
	
	/**
	 * lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks [-5,-1] varka, 0 neutral, [1,5] ketra
	 */
	private int _alliedVarkaKetra = 0;
	
	/** The list of sub-classes this character has. */
	
	private Map<Integer, SubClass> _subClasses;
	
	private final ReentrantLock _classLock = new ReentrantLock();
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	
	private long _lastAccess;
	private int _boatId;
	
	/** data for mounted pets */
	private boolean _canFeed;
	private int _controlItemId;
	private L2PetData _data;
	private int _curFeed;
	protected Future<?> _mountFeedTask;
	private ScheduledFuture<?> _dismountTask;
	
	private L2Fishing _fishCombat;
	private boolean _fishing = false;
	private int _fishx = 0;
	private int _fishy = 0;
	private int _fishz = 0;
	
	private ScheduledFuture<?> _taskRentPet;
	private ScheduledFuture<?> _taskWater;
	
	/** Boat */
	private L2BoatInstance _boat = null;
	private Point3D _inBoatPosition;
	
	/** Stored from last ValidatePosition **/
	private final Point3D _lastServerPosition = new Point3D(0, 0, 0);
	
	/** Previous coordinate sent to party in ValidatePosition **/
	private final Point3D _lastPartyPosition = new Point3D(0, 0, 0);
	
	// during fall validations will be disabled for 10 ms.
	private static final int FALLING_VALIDATION_DELAY = 10000;
	private long _fallingTimestamp = 0;
	
	/** Bypass validations */
	private final List<String> _validBypass = new FastList<>();
	private final List<String> _validBypass2 = new FastList<>();
	
	private boolean _inCrystallize;
	private boolean _inCraftMode;
	private Forum _forumMail;
	private Forum _forumMemo;
	
	/** Current skill in use */
	private SkillDat _currentSkill;
	private SkillDat _currentPetSkill;
	
	/** Skills queued because a skill is already in progress */
	private SkillDat _queuedSkill;
	
	/** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;
	
	private boolean _inJail = false;
	private long _jailTimer = 0;
	
	/** Flag to disable equipment/skills while wearing formal wear **/
	private boolean _IsWearingFormalWear = false;
	
	private int _ReviveRequested = 0;
	private double _RevivePower = 0;
	private boolean _RevivePet = false;
	
	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	
	private boolean _getGainXpSp = true;
	private boolean _isPendingSitting = false;
	private boolean _isAIOBuffer = false;
	
	/** Skill casting information (used to queue when several skills are cast in a short time) **/
	public class SkillDat
	{
		private final L2Skill _skill;
		private final boolean _ctrlPressed;
		private final boolean _shiftPressed;
		
		protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
		{
			_skill = skill;
			_ctrlPressed = ctrlPressed;
			_shiftPressed = shiftPressed;
		}
		
		public boolean isCtrlPressed()
		{
			return _ctrlPressed;
		}
		
		public boolean isShiftPressed()
		{
			return _shiftPressed;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
		
		public int getSkillId()
		{
			return (getSkill() != null) ? getSkill().getId() : -1;
		}
	}
	
	private final gatesRequest _gatesRequest = new gatesRequest();
	
	public class gatesRequest
	{
		private L2DoorInstance _target = null;
		
		public void setTarget(L2DoorInstance door)
		{
			_target = door;
			return;
		}
		
		public L2DoorInstance getDoor()
		{
			return _target;
		}
	}
	
	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create a new L2PcInstance with an account name</li>
	 * <li>Set the name, the Hair Style, the Hair Color and the Face type of the L2PcInstance</li>
	 * <li>Add the player in the characters table of the database</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the L2PcInstance
	 * @param name The name of the L2PcInstance
	 * @param hairStyle The hair style Identifier of the L2PcInstance
	 * @param hairColor The hair color Identifier of the L2PcInstance
	 * @param face The face type Identifier of the L2PcInstance
	 * @param sex
	 * @return The L2PcInstance added to the database or null
	 */
	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		// Create a new L2PcInstance with an account name
		final PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		final L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);
		
		// Set the name of the L2PcInstance
		player.setName(name);
		
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		
		// Add the player in the characters table of the database
		final boolean ok = player.createDb();
		if (!ok)
		{
			return null;
		}
		
		return player;
	}
	
	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		// Create a new L2PcInstance with an account name
		final L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);
		
		return player;
	}
	
	public String getAccountName()
	{
		if (getClient() == null)
		{
			return _accountName;
		}
		return getClient().getAccountName();
	}
	
	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}
	
	public int getRelation(L2PcInstance target)
	{
		int result = 0;
		
		if (getPvpFlag() != 0)
		{
			result |= RelationChanged.RELATION_PVP_FLAG;
		}
		
		if (getKarma() > 0)
		{
			result |= RelationChanged.RELATION_HAS_KARMA;
		}
		
		if (getClan() != null)
		{
			result |= RelationChanged.RELATION_CLAN_MEMBER;
		}
		
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		
		if ((getParty() != null) && (getParty() == target.getParty()))
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			for (int i = 0; i < getParty().getPartyMembers().size(); i++)
			{
				if (getParty().getPartyMembers().get(i) != this)
				{
					continue;
				}
				switch (i)
				{
					case 0:
						result |= RelationChanged.RELATION_PARTYLEADER; // 0x10
						break;
					case 1:
						result |= RelationChanged.RELATION_PARTY4; // 0x8
						break;
					case 2:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x7
						break;
					case 3:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2; // 0x6
						break;
					case 4:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY1; // 0x5
						break;
					case 5:
						result |= RelationChanged.RELATION_PARTY3; // 0x4
						break;
					case 6:
						result |= RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x3
						break;
					case 7:
						result |= RelationChanged.RELATION_PARTY2; // 0x2
						break;
					case 8:
						result |= RelationChanged.RELATION_PARTY1; // 0x1
						break;
				}
			}
		}
		
		if (getSiegeState() != 0)
		{
			
			result |= RelationChanged.RELATION_INSIEGE;
			
			if (getSiegeState() != target.getSiegeState())
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			
			if (getSiegeState() == 1)
			{
				result |= RelationChanged.RELATION_ATTACKER;
			}
			
		}
		
		if ((getClan() != null) && (target.getClan() != null))
		
		{
			if (target.getClan().isAtWarWith(getClan().getClanId()))
			
			{
				
				result |= RelationChanged.RELATION_1SIDED_WAR;
				
				if (getClan().isAtWarWith(target.getClan().getClanId()))
				{
					result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
				
			}
			
		}
		
		return result;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world (call restore method).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li>
	 * <li>Add the L2PcInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	public static L2PcInstance load(int objectId)
	{
		return restore(objectId);
	}
	
	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}
	
	/**
	 * Constructor of L2PcInstance (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance</li>
	 * <li>Set the name of the L2PcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the account including this L2PcInstance
	 * @param app
	 */
	private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		getStatus();
		
		super.initCharStatusUpdateValues();
		
		initPcStatusUpdateValues();
		
		_accountName = accountName;
		_appearance = app;
		
		// Create an AI
		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
		
		// Create a L2Radar object
		_radar = new L2Radar(this);
		
		if ((Hero.getInstance().getHeroes() != null) && Hero.getInstance().getHeroes().containsKey(getObjectId()))
		{
			setHero(true);
		}
		
		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		if (!Config.WAREHOUSE_CACHE)
		{
			getWarehouse();
		}
		getFreight().restore();
	}
	
	private L2PcInstance(int objectId)
	{
		super(objectId, null);
		
		getKnownList();
		getStat();
		getStatus();
		
		super.initCharStatusUpdateValues();
		
		initPcStatusUpdateValues();
	}
	
	@Override
	public final PcKnownList getKnownList()
	{
		if ((super.getKnownList() == null) || !(super.getKnownList() instanceof PcKnownList))
		{
			setKnownList(new PcKnownList(this));
		}
		return (PcKnownList) super.getKnownList();
	}
	
	@Override
	public final PcStat getStat()
	{
		if ((super.getStat() == null) || !(super.getStat() instanceof PcStat))
		{
			setStat(new PcStat(this));
		}
		return (PcStat) super.getStat();
	}
	
	@Override
	public final PcStatus getStatus()
	{
		if ((super.getStatus() == null) || !(super.getStatus() instanceof PcStatus))
		{
			setStatus(new PcStatus(this));
		}
		return (PcStatus) super.getStatus();
	}
	
	public final PcAppearance getAppearance()
	{
		return _appearance;
	}
	
	/**
	 * Return the base L2PcTemplate link to the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}
	
	/** Return the L2PcTemplate link to the L2PcInstance. */
	@Override
	public final L2PcTemplate getTemplate()
	
	{
		
		return (L2PcTemplate) super.getTemplate();
		
	}
	
	public void setTemplate(ClassId newclass)
	{
		
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}
	
	/**
	 * Return the AI of the L2PcInstance (create it if necessary).<BR>
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
					_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
				}
			}
		}
		
		return _ai;
	}
	
	/**
	 * Calculate a destination to explore the area and set the AI Intension to AI_INTENTION_MOVE_TO.<BR>
	 * <BR>
	 */
	public void explore()
	{
		if (!_exploring)
		{
			return;
		}
		
		if (getMountType() == 2)
		{
			return;
		}
		
		// Calculate the destination point (random)
		
		int x = (getX() + Rnd.nextInt(6000)) - 3000;
		int y = (getY() + Rnd.nextInt(6000)) - 3000;
		
		if (x > Universe.MAX_X)
		{
			x = Universe.MAX_X;
		}
		if (x < Universe.MIN_X)
		{
			x = Universe.MIN_X;
		}
		if (y > Universe.MAX_Y)
		{
			y = Universe.MAX_Y;
		}
		if (y < Universe.MIN_Y)
		{
			y = Universe.MIN_Y;
		}
		
		final int z = getZ();
		
		final L2CharPosition pos = new L2CharPosition(x, y, z, 0);
		
		// Set the AI Intention to AI_INTENTION_MOVE_TO
		getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
		
	}
	
	/** Return the Level of the L2PcInstance. */
	@Override
	public final int getLevel()
	{
		
		return getStat().getLevel();
	}
	
	/**
	 * Return the _newbie state of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public byte getNewbieState()
	{
		return _newbieState;
	}
	
	/**
	 * Set the _newbie state of the L2PcInstance.<BR>
	 * <BR>
	 * @param newbie The Identifier of the _newbieState state<BR>
	 *            <BR>
	 */
	public void setNewbieState(byte newbie)
	{
		_newbieState = newbie;
	}
	
	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}
	
	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}
	
	public boolean isInStoreMode()
	{
		
		return (getPrivateStoreType() > 0);
		
	}
	
	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}
	
	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}
	
	/**
	 * Manage Logout Task.<BR>
	 * <BR>
	 */
	public void logout()
	{
		logout(true);
	}
	
	/**
	 * Manage Logout Task:
	 * <li>Remove player from world <BR>
	 * <li>Save player data into DB <BR>
	 * {@link L2GameClient#saveCharToDisk(L2PcInstance, boolean)}</li> <BR>
	 * <BR>
	 * @param closeClient
	 */
	public void logout(boolean closeClient)
	{
		try
		{
			deleteMe();
			
			// Close the connection with the client
			closeNetConnection(closeClient);
			
			L2GameClient.saveCharToDisk(this, false);
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Exception on logout(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Return a table containing all Common L2RecipeList of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}
	
	/**
	 * Return a table containing all Dwarf L2RecipeList of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}
	
	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 * @param recipe The L2RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerCommonRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
		
	}
	
	/**
	 * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 * @param recipe The L2RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId(), true);
		}
	}
	
	/**
	 * @param recipeId The Identifier of the L2RecipeList to check in the player's recipe books
	 * @return <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else if (_commonRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 * @param recipeId The Identifier of the L2RecipeList to remove from the _recipebook
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, true);
		}
		else if (_commonRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, false);
		}
		else
		{
			_log.warning("Attempted to remove unknown RecipeList: " + recipeId);
		}
		
		for (final L2ShortCut sc : getAllShortCuts())
		{
			if (sc == null)
			{
				continue;
			}
			
			if ((sc.getId() == recipeId) && (sc.getType() == L2ShortCut.TYPE_RECIPE))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
	}
	
	private void insertNewRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, class_index, type) values(?,?,?,?)"))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.setInt(4, isDwarf ? 1 : 0);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Error while inserting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
	}
	
	private void deleteRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=? AND id=? AND class_index=?"))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Error while deleting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
	}
	
	/**
	 * Returns the Id for the last talked quest NPC.<BR>
	 * <BR>
	 * @return
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}
	
	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}
	
	/**
	 * Return the QuestState object corresponding to the quest name.<BR>
	 * <BR>
	 * @param quest The name of the quest
	 * @return
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}
	
	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.<BR>
	 * <BR>
	 * @param qs The QuestState to add to _quest
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}
	
	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.<BR>
	 * <BR>
	 * @param quest The name of the quest
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}
	
	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		final int len = questStateArray.length;
		final QuestState[] tmp = new QuestState[len + 1];
		for (int i = 0; i < len; i++)
		{
			tmp[i] = questStateArray[i];
		}
		tmp[len] = state;
		return tmp;
	}
	
	/**
	 * Return a table containing all Quest in progress from the table _quests.<BR>
	 * <BR>
	 * @return
	 */
	public Quest[] getAllActiveQuests()
	{
		final FastList<Quest> quests = new FastList<>();
		
		for (final QuestState qs : _quests.values())
		{
			if (qs.getQuest().getQuestIntId() >= 1999)
			{
				continue;
			}
			
			if (qs.isCompleted() && !Config.DEVELOPER)
			{
				continue;
			}
			
			if (!qs.isStarted() && !Config.DEVELOPER)
			{
				continue;
			}
			
			quests.add(qs.getQuest());
		}
		
		return quests.toArray(new Quest[quests.size()]);
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR>
	 * <BR>
	 * @param npc The Identifier of the L2Attackable attacked
	 * @return
	 */
	public QuestState[] getQuestsForAttacks(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		final Quest[] quests = npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK);
		if (quests != null)
		{
			for (final Quest quest : quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
						{
							states = new QuestState[]
							{
								getQuestState(quest.getName())
							};
						}
						else
						{
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
						}
					}
				}
			}
		}
		
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR>
	 * <BR>
	 * @param npc The Identifier of the L2Attackable killed
	 * @return
	 */
	public QuestState[] getQuestsForKills(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		final Quest[] quests = npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL);
		if (quests != null)
		{
			for (final Quest quest : quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
						{
							states = new QuestState[]
							{
								getQuestState(quest.getName())
							};
						}
						else
						{
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
						}
					}
				}
			}
		}
		
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.<BR>
	 * <BR>
	 * @param npcId The Identifier of the NPC
	 * @return
	 */
	public QuestState[] getQuestsForTalk(int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		final Quest[] quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (final Quest quest : quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
						{
							states = new QuestState[]
							{
								getQuestState(quest.getName())
							};
						}
						else
						{
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
						}
					}
				}
			}
		}
		
		// Return a table containing all QuestState to modify
		return states;
	}
	
	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if (event == null)
		{
			event = "";
		}
		
		if (!_quests.containsKey(quest))
		{
			return retval;
		}
		
		QuestState qs = getQuestState(quest);
		if ((qs == null) && (event.length() == 0))
		{
			return retval;
		}
		
		if (qs == null)
		
		{
			final Quest q = QuestManager.getInstance().getQuest(quest);
			if (q == null)
			{
				return retval;
			}
			qs = q.newQuestState(this);
		}
		
		if (qs != null)
		
		{
			if (getLastQuestNpcObject() > 0)
			{
				final L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
				
				if ((object instanceof L2NpcInstance) && isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false))
				{
					final L2NpcInstance npc = (L2NpcInstance) object;
					final QuestState[] states = getQuestsForTalk(npc.getNpcId());
					
					if (states != null)
					{
						for (final QuestState state : states)
						{
							if ((state.getQuest().getQuestIntId() == qs.getQuest().getQuestIntId()) && !qs.isCompleted())
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
								{
									showQuestWindow(quest, qs.getStateId());
								}
								
								retval = qs;
							}
						}
						
						sendPacket(new QuestList());
					}
				}
			}
		}
		
		return retval;
	}
	
	private void showQuestWindow(String questId, String stateId)
	{
		final String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
		final String content = HtmCache.getInstance().getHtm(path); // TODO path for quests html
		
		if (content != null)
		{
			if (Config.DEBUG)
			{
				_log.fine("Showing quest window for quest " + questId + " state " + stateId + " html path: " + path);
			}
			
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}
		
		sendPacket(new ActionFailed());
	}
	
	/**
	 * Return a table containing all L2ShortCut of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}
	
	/**
	 * Return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).<BR>
	 * <BR>
	 * @param slot The slot in wich the shortCuts is equiped
	 * @param page The page of shortCuts containing the slot
	 * @return
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}
	
	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts<BR>
	 * <BR>
	 * @param shortcut
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}
	
	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.<BR>
	 * <BR>
	 * @param slot
	 * @param page
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}
	
	/**
	 * Add a L2Macro to the L2PcInstance _macroses<BR>
	 * <BR>
	 * @param macro
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}
	
	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.<BR>
	 * <BR>
	 * @param id
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}
	
	/**
	 * Return all L2Macro of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}
	
	/**
	 * Set the PvP Flag of the L2PcInstance.<BR>
	 * <BR>
	 * @param pvpFlag
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = pvpFlag;
	}
	
	public int getPvpFlag()
	{
		return _pvpFlag;
	}
	
	/**
	 * Set the siege state of the L2PcInstance.<BR>
	 * <BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 * @param siegeState
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	public int getSiegeState()
	{
		return _siegeState;
	}
	
	@Override
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport)
		if (getWorldRegion() == null)
		{
			return;
		}
		
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
		
		getWorldRegion().revalidateZones(this);
		
		if (Config.ALLOW_WATER)
		{
			checkWaterState();
		}
		
	}
	
	@Override
	public void updatePvPFlag(int value)
	{
		if (getPvpFlag() == value)
		{
			return;
		}
		setPvpFlag(value);
		
		sendPacket(new UserInfo(this));
		
		// If this player has a pet update the pets pvp flag as well
		if (getPet() != null)
		{
			sendPacket(new RelationChanged(getPet(), getRelation(this), false));
		}
		
		for (final L2PcInstance target : getKnownList().getKnownPlayers().values())
		
		{
			
			target.sendPacket(new RelationChanged(this, getRelation(target), isAutoAttackable(target)));
			if (getPet() != null)
			{
				target.sendPacket(new RelationChanged(getPet(), getRelation(this), isAutoAttackable(target)));
			}
		}
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
	 * <BR>
	 * @return
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}
	
	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
	 * <BR>
	 * @return
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}
	
	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}
	
	/**
	 * Return the PK counter of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Set the PK counter of the L2PcInstance.<BR>
	 * <BR>
	 * @param pkKills
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}
	
	/**
	 * Return the _deleteTimer of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Set the _deleteTimer of the L2PcInstance.<BR>
	 * <BR>
	 * @param deleteTimer
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * Return the current weight of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	
	/**
	 * Return date of las update of recomPoints
	 * @return
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
		
	}
	
	public void setLastRecomUpdate(long date)
	
	{
		
		_lastRecomUpdate = date;
	}
	
	/**
	 * Return the number of recommendation obtained by the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommendation obtained by the L2PcInstance (Max : 255).<BR>
	 * <BR>
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
		{
			_recomHave++;
		}
	}
	
	/**
	 * Set the number of recommendation obtained by the L2PcInstance (Max : 255).<BR>
	 * <BR>
	 * @param value
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
		{
			_recomHave = 255;
		}
		else if (value < 0)
		{
			_recomHave = 0;
		}
		else
		{
			_recomHave = value;
		}
	}
	
	/**
	 * Return the number of recommendation that the L2PcInstance can give.<BR>
	 * <BR>
	 * @return
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommendation that the L2PcInstance can give.<BR>
	 * <BR>
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
		{
			_recomLeft--;
		}
	}
	
	public void giveRecom(L2PcInstance target)
	{
		
		if (Config.ALT_RECOMMEND)
		
		{
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM))
			
			{
				
				statement.setInt(1, getObjectId());
				
				statement.setInt(2, target.getObjectId());
				
				statement.execute();
				
			}
			
			catch (final Exception e)
			
			{
				
				_log.warning("could not update char recommendations:" + e);
				
			}
			
		}
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}
	
	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}
	
	/**
	 * Return the Karma of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).<BR>
	 * <BR>
	 * @param karma
	 */
	public void setKarma(int karma)
	{
		if (karma < 0)
		{
			karma = 0;
		}
		
		if ((_karma == 0) && (karma > 0))
		{
			for (final L2Object object : getKnownList().getKnownObjects().values())
			{
				if (!(object instanceof L2GuardInstance))
				{
					continue;
				}
				
				if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if ((_karma > 0) && (karma == 0))
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
			setKarmaFlag(0);
		}
		
		_karma = karma;
		broadcastKarma();
	}
	
	/**
	 * Return the max weight that the L2PcInstance can load.<BR>
	 * <BR>
	 * @return
	 */
	public int getMaxLoad()
	{
		// Weight Limit = (CON Modifier*69000) * Skills
		// Source
		final double baseLoad = Math.floor(BaseStats.CON.calcBonus(this) * 69000 * Config.ALT_WEIGHT_LIMIT);
		return (int) calcStat(Stats.MAX_LOAD, baseLoad, this, null);
	}
	
	public int getExpertisePenalty()
	{
		return _expertisePenalty;
	}
	
	public int getWeightPenalty()
	{
		if (_dietMode)
		{
			return 0;
		}
		
		return _curWeightPenalty;
	}
	
	/**
	 * Update the overloaded status of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void refreshOverloaded()
	{
		if (getMaxLoad() > 0)
		{
			final long weightproc = (long) (((getCurrentLoad() - calcStat(Stats.MAX_LOAD, 1, this, null)) * 1000) / getMaxLoad());
			int newWeightPenalty;
			if ((weightproc < 500) || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0)
				{
					super.addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
					setIsOverloaded(getCurrentLoad() >= getMaxLoad());
				}
				else
				{
					super.removeSkill(getKnownSkill(4270));
					setIsOverloaded(false);
				}
				sendPacket(new UserInfo(this));
				
				sendPacket(new EtcStatusUpdate(this));
				
				Broadcast.toKnownPlayers(this, new CharInfo(this));
			}
		}
	}
	
	public void refreshExpertisePenalty()
	{
		int newPenalty = 0;
		
		for (final L2ItemInstance item : getInventory().getItems())
		{
			if ((item != null) && item.isEquipped())
			{
				final int crystaltype = item.getItem().getCrystalType();
				
				if (crystaltype > newPenalty)
				{
					newPenalty = crystaltype;
				}
			}
		}
		
		newPenalty = newPenalty - getExpertiseIndex();
		
		if (newPenalty <= 0)
		{
			newPenalty = 0;
		}
		
		if (getExpertisePenalty() != newPenalty)
		{
			_expertisePenalty = newPenalty;
			
			if (newPenalty > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1));
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
			}
			
			sendPacket(new EtcStatusUpdate(this));
		}
	}
	
	public void checkSShotsMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
	{
		if (unequipped == null)
		{
			return;
		}
		
		// on retail auto shots are never disabled on unequip
		unequipped.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
		unequipped.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
	}
	
	/**
	 * Equip or unequip the item.
	 * <UL>
	 * <LI>If item is equipped, shots are applied if automation is on.</LI>
	 * <LI>If item is unequipped, shots are discharged.</LI>
	 * </UL>
	 * @param item The item to charge/discharge.
	 * @param abortAttack If true, the current attack will be aborted in order to equip the item.
	 */
	public void useEquippableItem(L2ItemInstance item, boolean abortAttack)
	{
		// Equip or unEquip
		L2ItemInstance[] items = null;
		final boolean isEquipped = item.isEquipped();
		SystemMessage sm = null;
		final int bodyPart = item.getItem().getBodyPart();
		
		L2ItemInstance old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		if (old == null)
		{
			old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		}
		
		checkSShotsMatch(item, old);
		
		if (isEquipped)
		{
			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessage.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item.getItemId());
			}
			else
			{
				sm = new SystemMessage(SystemMessage.S1_DISARMED);
				sm.addItemName(item.getItemId());
			}
			
			sendPacket(sm);
			
			items = getInventory().unEquipItemInBodySlotAndRecord(bodyPart);
		}
		else
		{
			L2ItemInstance tempItem = getInventory().getPaperdollItemByL2ItemId(bodyPart);
			if ((tempItem != null) && tempItem.isWear())
			{
				return;
			}
			else if ((bodyPart == L2Item.SLOT_LR_HAND) || (bodyPart == L2Item.SLOT_L_HAND) || (item.getItemId() == 6408))
			{
				// this may not remove left OR right hand equipment
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if ((tempItem != null) && tempItem.isWear())
				{
					return;
				}
				
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				if ((tempItem != null) && tempItem.isWear())
				{
					return;
				}
			}
			else if (bodyPart == L2Item.SLOT_FULL_ARMOR)
			{
				// this may not remove chest or leggins
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				if ((tempItem != null) && tempItem.isWear())
				{
					return;
				}
				
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
				if ((tempItem != null) && tempItem.isWear())
				{
					return;
				}
			}
			
			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessage.S1_S2_EQUIPPED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item.getItemId());
			}
			else
			{
				sm = new SystemMessage(SystemMessage.S1_EQUIPPED);
				sm.addItemName(item.getItemId());
			}
			sendPacket(sm);
			
			items = getInventory().equipItemAndRecord(item);
			
		}
		sm = null;
		
		refreshExpertisePenalty();
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);
		
		if (abortAttack)
		{
			abortAttack();
		}
		
		broadcastUserInfo();
	}
	
	/**
	 * Return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR>
	 * <BR>
	 * @return
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR>
	 * <BR>
	 * @param pvpKills
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}
	
	/**
	 * Return the ClassId object of the L2PcInstance contained in L2PcTemplate.<BR>
	 * <BR>
	 * @return
	 */
	public ClassId getClassId()
	{
		return getTemplate().classId;
	}
	
	/**
	 * Set the template of the L2PcInstance.<BR>
	 * <BR>
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 */
	public void setClassId(int Id)
	{
		if (!_classLock.tryLock())
		{
			return;
		}
		
		try
		{
			
			if (isSubClassActive())
			{
				getSubClasses().get(_classIndex).setClassId(Id);
			}
			
			setClassTemplate(Id);
			
			if (getClassId().level() == 3)
			{
				sendPacket(new SystemMessage(1606)); // system sound 3rd occupation
			}
			else
			{
				sendPacket(new SystemMessage(1308)); // system sound for 1st and 2nd occupation
			}
			
			if (isInParty())
			{
				getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
			}
			
			if (getClan() != null)
			{
				getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			}
			
			if (Config.AUTO_LEARN_SKILLS)
			{
				rewardSkills();
			}
		}
		
		finally
		{
			_classLock.unlock();
		}
	}
	
	/**
	 * Return the Experience of the L2PcInstance.
	 * @return
	 */
	public long getExp()
	{
		
		return getStat().getExp();
		
	}
	
	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		_activeEnchantItem = scroll;
	}
	
	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}
	
	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR>
	 * <BR>
	 * @param weaponItem The fists L2Weapon to set to the L2PcInstance
	 */
	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR>
	 * <BR>
	 * @return
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance Class (used when no weapon is equiped).<BR>
	 * <BR>
	 * @param classId
	 * @return
	 */
	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			// human fighter fists
			final L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			// human mage fists
			final L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			// elven fighter fists
			final L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			// elven mage fists
			final L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			// dark elven fighter fists
			final L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			// dark elven mage fists
			final L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			// orc fighter fists
			final L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			// orc mage fists
			final L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			// dwarven fists
			final L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon) temp;
		}
		
		return weaponItem;
	}
	
	/**
	 * Give Expertise skill of this level and remove beginner Lucky skill.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the Level of the L2PcInstance</li>
	 * <li>If L2PcInstance Level is 5, remove beginner Lucky skill</li>
	 * <li>Add the Expertise skill corresponding to its Expertise level</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR>
	 * <BR>
	 */
	public void rewardSkills()
	{
		// Get the Level of the L2PcInstance
		final int lvl = getLevel();
		
		// Calculate the current higher Expertise of the L2PcInstance
		for (int i = 0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (lvl >= EXPERTISE_LEVELS[i])
			{
				setExpertiseIndex(i);
			}
		}
		
		// Add the Expertise skill corresponding to its Expertise level
		if (getExpertiseIndex() > 0)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill, true);
		}
		
		for (int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if ((lvl >= COMMON_CRAFT_LEVELS[i]) && (getSkillLevel(1320) < (i + 1)))
			{
				final L2Skill skill = SkillTable.getInstance().getInfo(1320, (i + 1));
				addSkill(skill, true);
			}
		}
		
		// Auto-Learn skills if activated
		
		if (Config.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills();
		}
		
		// This function gets called on login, so not such a bad place to check weight
		refreshOverloaded(); // Update the overloaded status of the L2PcInstance
		refreshExpertisePenalty(); // Update the expertise status of the L2PcInstance
	}
	
	/**
	 * Give all available skills to the player.<br>
	 * <br>
	 */
	private void giveAvailableSkills()
	{
		// Check if 3rd class skills are auto-learned
		if (!Config.AUTO_LEARN_3RD_SKILLS && (getClassId().level() == 3))
		{
			return;
		}
		
		int skillCounter = 0;
		
		// Get available skills
		final L2SkillLearn[] skills = SkillTreeTable.getInstance().getMaxAvailableSkills(this, getClassId());
		for (final L2SkillLearn s : skills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if ((sk == null) || !sk.getCanLearn(getClassId()))
			{
				continue;
			}
			
			if (getSkillLevel(sk.getId()) == -1)
			{
				skillCounter++;
			}
			
			addSkill(sk, true);
		}
		
		if (skillCounter > 0)
		{
			sendMessage("You just acquired " + skillCounter + " new skills.");
		}
	}
	
	/**
	 * Set the Experience value of the L2PcInstance.
	 * @param exp
	 */
	public void setExp(long exp)
	{
		if (exp < 0)
		{
			exp = 0;
		}
		
		getStat().setExp(exp);
	}
	
	/**
	 * Return the Race object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
		{
			return getTemplate().race;
		}
		
		final L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
		return charTemp.race;
	}
	
	public L2Radar getRadar()
	{
		
		return _radar;
	}
	
	/**
	 * Return the SP amount of the L2PcInstance.
	 * @return
	 */
	public int getSp()
	
	{
		
		return getStat().getSp();
	}
	
	/**
	 * Set the SP amount of the L2PcInstance.
	 * @param sp
	 */
	public void setSp(int sp)
	
	{
		if (sp < 0)
		{
			sp = 0;
		}
		
		super.getStat().setSp(sp);
		
	}
	
	/**
	 * Return true if this L2PcInstance is a clan leader in ownership of the passed castle
	 * @param castleId
	 * @return
	 */
	public boolean isCastleLord(int castleId)
	{
		final L2Clan clan = getClan();
		
		// player has clan and is the clan leader, check the castle info
		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Return the Clan Identifier of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Return the Clan Crest Identifier of the L2PcInstance or 0.<BR>
	 * <BR>
	 * @return
	 */
	public int getClanCrestId()
	{
		if ((_clan != null) && _clan.hasCrest())
		{
			return _clan.getCrestId();
		}
		
		return 0;
	}
	
	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if ((_clan != null) && _clan.hasCrestLarge())
		{
			return _clan.getCrestLargeId();
		}
		
		return 0;
	}
	
	public long getClanJoinExpiryTime()
	
	{
		
		return _clanJoinExpiryTime;
		
	}
	
	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}
	
	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}
	
	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}
	
	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}
	
	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.<BR>
	 * <BR>
	 * @return
	 */
	public PcInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.<BR>
	 * <BR>
	 * @param objectId
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}
	
	/**
	 * Return True if the L2PcInstance is sitting.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
	
	/**
	 * Set _waitTypeSitting to given value
	 * @param state
	 */
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}
	
	/**
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)<BR>
	 * <BR>
	 */
	public void sitDown()
	{
		if (!_waitTypeSitting)
		{
			abortAttack();
			
			setIsSitting(true);
			
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			// Schedule a sit down task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}
	
	/**
	 * Sit down Task
	 */
	class SitDownTask implements Runnable
	{
		L2PcInstance _player;
		
		SitDownTask(L2PcInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			_player.setIsParalyzed(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
	
	/**
	 * Stand up Task
	 */
	class StandUpTask implements Runnable
	{
		L2PcInstance _player;
		
		StandUpTask(L2PcInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			_player.setIsSitting(false);
			if (_player.getAI().getNextIntention() != null)
			{
				_player.getAI().setIntention(_player.getAI().getNextIntention()._crtlIntention, _player.getAI().getNextIntention()._arg0, _player.getAI().getNextIntention()._arg1);
				_player.getAI().setNextIntention(null);
			}
		}
	}
	
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)<BR>
	 * <BR>
	 */
	public void standUp()
	{
		if (L2Event.active && eventSitForced)
		{
			sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up ...");
		}
		else if (isFakeDeath())
		{
			stopEffects(L2Effect.EffectType.FAKE_DEATH);
		}
		else if (_waitTypeSitting && !isInStoreMode() && !isDead())
		{
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			
			// Schedule a stand up task to wait for the animation to finish
			
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
		}
	}
	
	/**
	 * Set the value of the _relax value. Must be True if using skill Relax and False if not.
	 * @param val
	 */
	public void setRelax(boolean val)
	{
		_relax = val;
	}
	
	/**
	 * Return the PcWarehouse object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public PcWarehouse getWarehouse()
	{
		
		if (_warehouse == null)
		
		{
			
			_warehouse = new PcWarehouse(this);
			
			_warehouse.restore();
			
		}
		
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().addCacheTask(this);
		}
		
		return _warehouse;
	}
	
	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
		{
			_warehouse.deleteMe();
		}
		_warehouse = null;
	}
	
	/**
	 * Return the PcFreight object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public PcFreight getFreight()
	{
		return _freight;
	}
	
	/**
	 * Return the Identifier of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getCharId()
	{
		return _charId;
	}
	
	/**
	 * Set the Identifier of the L2PcInstance.<BR>
	 * <BR>
	 * @param charId
	 */
	public void setCharId(int charId)
	{
		_charId = charId;
	}
	
	/**
	 * Return the Adena amount of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * Return the Ancient Adena amount of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}
	
	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.EARNED_ADENA);
			sm.addNumber(count);
			sendPacket(sm);
		}
		
		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAdenaInstance());
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Reduce adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.YOU_NOT_ENOUGH_ADENA));
			}
			
			return false;
		}
		
		if (count > 0)
		{
			final L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			_inventory.reduceAdena(process, count, this, reference);
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				final SystemMessage sm = new SystemMessage(SystemMessage.DISSAPEARED_ADENA);
				sm.addNumber(count);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.EARNED_S2_S1_s);
			sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
			
			sm.addNumber(count);
			sendPacket(sm);
		}
		
		if (count > 0)
		{
			_inventory.addAncientAdena(process, count, this, reference);
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAncientAdenaInstance());
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.YOU_NOT_ENOUGH_ADENA));
			}
			
			return false;
		}
		
		if (count > 0)
		{
			final L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			_inventory.reduceAncientAdena(process, count, this, reference);
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				final SystemMessage sm = new SystemMessage(SystemMessage.DISSAPEARED_ITEM);
				sm.addNumber(count);
				sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					final SystemMessage sm = new SystemMessage(SystemMessage.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item.getItemId());
					sm.addNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					final SystemMessage sm = new SystemMessage(SystemMessage.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item.getItemId());
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessage.YOU_PICKED_UP_S1);
					sm.addItemName(item.getItemId());
					sendPacket(sm);
				}
			}
			
			// Add the item to inventory
			final L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);
			
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			// Update current load as well
			final StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
			
			// If over capacity, drop the item
			if (!isGM() && !_inventory.validateCapacity(0) && item.isDropable() && (!item.isStackable() || (item.getLastChange() != L2ItemInstance.MODIFIED)))
			{
				dropItem("InvDrop", item, null, true, true);
			}
		}
	}
	
	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						final SystemMessage sm = new SystemMessage(SystemMessage.EARNED_S2_S1_s);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessage.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						final SystemMessage sm = new SystemMessage(SystemMessage.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessage.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			
			// Add the item to inventory
			final L2ItemInstance item = _inventory.addItem(process, itemId, count, this, reference);
			
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(item);
				sendPacket(playerIU);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			// Update current load as well
			final StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
			
			// If over capacity, drop the item
			if (!isGM() && !_inventory.validateCapacity(0) && item.isDropable() && (!item.isStackable() || (item.getLastChange() != L2ItemInstance.MODIFIED)))
			{
				dropItem("InvDrop", item, null, true);
			}
		}
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param count
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItem(String process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage)
	{
		item = _inventory.destroyItem(process, item, count, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		
		final L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		return destroyItem(process, item, count, reference, sendMessage);
	}
	
	/**
	 * Destroys shots from inventory without logging and only occasional saving to database. Sends a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		final L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if ((item == null) || (item.getCount() < count))
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		
		return destroyItem(null, item, count, reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		
		final L2ItemInstance item = _inventory.getItemByItemId(itemId);
		
		if ((item == null) || (item.getCount() < count) || (_inventory.destroyItemByItemId(process, itemId, count, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.DISSAPEARED_ITEM);
			
			sm.addNumber(count);
			
			sm.addItemName(itemId);
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroy all weared items from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{
		
		// Go through all Items of the inventory
		
		for (final L2ItemInstance item : getInventory().getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			// Check if the item is a Try On item in order to remove it
			if (item.isWear())
			{
				
				if (item.isEquipped())
				{
					getInventory().unEquipItemInSlotAndRecord(item.getEquipSlot());
				}
				
				if (_inventory.destroyItem(process, item, this, reference) == null)
				{
					_log.warning("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}
				
				// Send an Unequipped Message in system window of the player for each Item
				final SystemMessage sm = new SystemMessage(0x1a1);
				sm.addItemName(item.getItemId());
				sendPacket(sm);
				
			}
		}
		
		// Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD (0x0e) information
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
		
		final ItemList il = new ItemList(getInventory().getItemList(), true);
		
		sendPacket(il);
		
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers
		broadcastUserInfo();
		
		// Sends message to client if requested
		sendMessage("Trying-on mode has ended.");
		
	}
	
	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId
	 * @param count : int Quantity of items to be transfered
	 * @param target
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference)
	{
		final L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
		{
			return null;
		}
		
		final L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);
		
		// Send target update packet
		if (target instanceof PcInventory)
		{
			final L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				final InventoryUpdate playerIU = new InventoryUpdate();
				
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
				
				targetPlayer.sendPacket(playerIU);
			}
			else
			{
				targetPlayer.sendPacket(new ItemList(targetPlayer, false));
			}
			
			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			final PetInventoryUpdate petIU = new PetInventoryUpdate();
			if (newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
			getPet().getInventory().refreshWeight();
			
		}
		
		return newItem;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @param protectItem : whether or not dropped item must be protected temporary against other players
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		item = _inventory.dropItem(process, item, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		item.dropMe(this, (getX() + Rnd.get(50)) - 25, (getY() + Rnd.get(50)) - 25, getZ() + 20);
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		
		{
			
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			{
				ItemsAutoDestroy.getInstance().addItem(item);
			}
			
		}
		
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		
		{
			
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
			
		}
		else
		{
			item.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.YOU_DROPPED_S1);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return true;
	}
	
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param x : int coordinate for drop X
	 * @param y : int coordinate for drop Y
	 * @param z : int coordinate for drop Z
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @param protectItem
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		final L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		final L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			
			return null;
		}
		
		item.dropMe(this, x, y, z);
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		
		{
			
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			
			{
				
				ItemsAutoDestroy.getInstance().addItem(item);
				
			}
			
		}
		
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		
		{
			
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
			
		}
		else
		{
			item.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.YOU_DROPPED_S1);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return item;
	}
	
	public L2ItemInstance checkItemManipulation(int objectId, int count, String action)
	{
		
		// TODO: if we remove objects that are not visible from the L2World, we'll have to remove this check
		if (L2World.getInstance().findObject(objectId) == null)
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item not available in L2World");
			return null;
		}
		
		final L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}
		
		if ((count < 0) || ((count > 1) && !item.isStackable()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		
		if (count > item.getCount())
		{
			_log.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (((getPet() != null) && (getPet().getControlItemId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			
			return null;
		}
		
		if ((getActiveEnchantItem() != null) && (getActiveEnchantItem().getObjectId() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			
			return null;
		}
		
		if (item.isWear())
		{
			return null;
		}
		
		return item;
	}
	
	/**
	 * Set _protectEndTime according settings.
	 * @param protect
	 */
	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || isSpawnProtected()))
		{
			System.out.println(getName() + ": Protection " + (protect ? "ON " + (GameTimeController.getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND)) : "OFF") + " (currently " + GameTimeController.getGameTicks() + ")");
		}
		
		_protectEndTime = protect ? GameTimeController.getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}
	
	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 * @param protect
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getGameTicks() + (Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}
	
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
	}
	
	/**
	 * Get the client owner of this char.<BR>
	 * <BR>
	 * @return
	 */
	public L2GameClient getClient()
	{
		return _client;
	}
	
	public void setClient(L2GameClient client)
	{
		_client = client;
	}
	
	/**
	 * Close the active connection with the client.<BR>
	 * <BR>
	 * @param closeClient
	 */
	public void closeNetConnection(boolean closeClient)
	{
		final L2GameClient client = _client;
		if (client != null)
		{
			if (client.isDetached())
			{
				client.cleanMe(true);
			}
			else
			{
				if (!client.getConnection().isClosed())
				{
					if (closeClient)
					{
						client.close(new LeaveWorld());
					}
					else
					{
						client.close(new ServerClose());
					}
				}
			}
		}
	}
	
	/**
	 * Manage actions when a player click on this L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions on first click on the L2PcInstance (Select it)</U> :</B><BR>
	 * <BR>
	 * <li>Set the target of the player</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li><BR>
	 * <BR>
	 * <B><U> Actions on second click on the L2PcInstance (Follow it/Attack it/Intercat with it)</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li>
	 * <li>If this L2PcInstance has a Private Store, notify the player AI with AI_INTENTION_INTERACT</li>
	 * <li>If this L2PcInstance is autoAttackable, notify the player AI with AI_INTENTION_ATTACK</li><BR>
	 * <BR>
	 * <li>If this L2PcInstance is NOT autoAttackable, notify the player AI with AI_INTENTION_FOLLOW</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action, AttackRequest</li><BR>
	 * <BR>
	 * @param player The player that start an action on this L2PcInstance
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		// Check if the L2PcInstance is confused
		if (player.isOutOfControl())
		{
			// Send a Server->Client packet ActionFailed to the player
			player.sendPacket(new ActionFailed());
			
			return;
		}
		
		// Check if the player already target this L2PcInstance
		if (player.getTarget() != this)
		{
			// Set the target of the player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the player
			// The color to display in the select window is White
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			
			if ((player != this) && !isInBoat())
			{
				player.sendPacket(new ValidateLocation(this));
			}
		}
		else
		{
			
			if ((player != this) && !isInBoat())
			{
				player.sendPacket(new ValidateLocation(this));
			}
			
			// Check if this L2PcInstance has a Private Store
			if (getPrivateStoreType() != 0)
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				// Check if this L2PcInstance is autoAttackable
				if (isAutoAttackable(player))
				
				{
					
					if (Config.GEODATA > 0)
					
					{
						
						if (GeoData.getInstance().canSeeTarget(player, this))
						
						{
							
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
							
							player.onActionRequest();
							
						}
						
					}
					
					else
					
					{
						
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						
						player.onActionRequest();
						
					}
					
				}
				else
				
				{
					
					// This Action Failed packet avoids player getting stuck when clicking three or more times
					
					player.sendPacket(new ActionFailed());
					if (Config.GEODATA > 0)
					
					{
						
						if (GeoData.getInstance().canSeeTarget(player, this))
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
						}
						
					}
					else
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
					
				}
			}
		}
	}
	
	@Override
	public void onActionShift(L2GameClient client)
	{
		// Get the L2PcInstance corresponding to the thread
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		// Check if the L2PcInstance is confused
		if (player.isOutOfControl())
		{
			// Send a Server->Client packet ActionFailed to the player
			player.sendPacket(new ActionFailed());
			
			return;
		}
		
		// Check if the player already target this L2PcInstance
		if (player.getTarget() != this)
		{
			// Set the target of the player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the player
			// The color to display in the select window is White
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		}
		
		if ((player != this) && !isInBoat())
		{
			player.sendPacket(new ValidateLocation(this));
		}
		
		// This Action Failed packet avoids player getting stuck when shift-clicking
		
		player.sendPacket(new ActionFailed());
	}
	
	/**
	 * Returns true if cp update should be done, false if not
	 * @param barPixels
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		final double currentCp = getCurrentCp();
		
		if ((currentCp <= 1.0) || (getMaxCp() < barPixels))
		{
			return true;
		}
		
		if ((currentCp <= _cpUpdateDecCheck) || (currentCp >= _cpUpdateIncCheck))
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns true if mp update should be done, false if not
	 * @param barPixels
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		final double currentMp = getCurrentMp();
		
		if ((currentMp <= 1.0) || (getMaxMp() < barPixels))
		{
			return true;
		}
		
		if ((currentMp <= _mpUpdateDecCheck) || (currentMp >= _mpUpdateIncCheck))
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance</li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		// TODO We mustn't send these informations to other players
		// Send the Server->Client packet StatusUpdate with current HP and MP to all L2PcInstance that must be informed of HP/MP updates of this L2PcInstance
		
		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		
		sendPacket(su);
		
		// Check if a party is in progress and party window update is usefull
		if (isInParty() && (needCpUpdate(352) || super.needHpUpdate(352) || needMpUpdate(352)))
		{
			
			if (Config.DEBUG)
			{
				_log.fine("Send status for party window of " + getObjectId() + "(" + getName() + ") to his party. CP: " + getCurrentCp() + " HP: " + getCurrentHp() + " MP: " + getCurrentMp());
			}
			// Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party
			final PartySmallWindowUpdate update = new PartySmallWindowUpdate(this);
			getParty().broadcastToPartyMembers(this, update);
		}
		
		if (isInOlympiadMode() && isOlympiadStart() && (needCpUpdate(352) || super.needHpUpdate(352)))
		{
			Olympiad.sendUserInfo(this);
		}
	}
	
	@Override
	public final void updateEffectIcons(boolean partyOnly)
	{
		
		// Create the main packet if needed
		MagicEffectIcons mi = null;
		
		if (!partyOnly)
		{
			mi = new MagicEffectIcons();
		}
		
		// Create the party packet if needed
		PartySpelled ps = null;
		
		if (isInParty())
		{
			ps = new PartySpelled(this);
		}
		
		// Create the olympiad spectator packet if needed
		ExOlympiadSpelledInfo os = null;
		if (isInOlympiadMode() && isOlympiadStart())
		{
			os = new ExOlympiadSpelledInfo(this);
		}
		
		if ((mi == null) && (ps == null) && (os == null))
		{
			return; // nothing to do (should not happen)
		}
		
		// Go through all effects if any
		final L2Effect[] effects = getAllEffects();
		if ((effects != null) && (effects.length > 0))
		{
			for (final L2Effect effect : effects)
			{
				if ((effect == null) || !effect.getShowIcon())
				{
					continue;
				}
				
				if (effect.getInUse())
				{
					if (mi != null)
					{
						effect.addIcon(mi);
					}
					if (ps != null)
					{
						effect.addPartySpelledIcon(ps);
					}
					if (os != null)
					{
						effect.addOlympiadSpelledIcon(os);
					}
				}
			}
			
		}
		
		// Send the packets if needed
		if (mi != null)
		{
			sendPacket(mi);
		}
		
		if (ps != null)
		{
			getParty().broadcastToPartyMembers(this, ps);
		}
		
		if (os != null)
		{
			if (Olympiad.getInstance().getSpectators(getOlympiadGameId()) != null)
			{
				for (final L2PcInstance spectator : Olympiad.getInstance().getSpectators(getOlympiadGameId()))
				{
					if (spectator == null)
					{
						continue;
					}
					
					spectator.sendPacket(os);
				}
			}
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>. In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
	 * <BR>
	 */
	public final void broadcastUserInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
		{
			_log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] 03 CharInfo");
		}
		
		Broadcast.toKnownPlayers(this, new CharInfo(this));
	}
	
	public final void broadcastTitleInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Send a Server->Client packet NicknameChanged to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
		{
			_log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] cc NicknameChanged");
		}
		
		Broadcast.toKnownPlayers(this, new NicknameChanged(this));
	}
	
	@Override
	public final void broadcastPacket(L2GameServerPacket mov)
	{
		if (!(mov instanceof CharInfo))
		{
			sendPacket(mov);
		}
		
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
		
		{
			
			player.sendPacket(mov);
			
			if (mov instanceof CharInfo)
			
			{
				
				final int relation = getRelation(player);
				
				if ((getKnownList().getKnownRelations().get(player.getObjectId()) != null) && (getKnownList().getKnownRelations().get(player.getObjectId()) != relation))
				
				{
					player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
					if (getPet() != null)
					{
						player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
					}
				}
				
			}
			
		}
		
	}
	
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		if (!(mov instanceof CharInfo))
		{
			sendPacket(mov);
		}
		
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if (isInsideRadius(player, radiusInKnownlist, false, false))
			{
				player.sendPacket(mov);
				if (mov instanceof CharInfo)
				{
					final int relation = getRelation(player);
					if ((getKnownList().getKnownRelations().get(player.getObjectId()) != null) && (getKnownList().getKnownRelations().get(player.getObjectId()) != relation))
					{
						player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
						if (getPet() != null)
						{
							player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Return the Alliance Identifier of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getAllyId()
	{
		if (_clan == null)
		{
			return 0;
		}
		return _clan.getAllyId();
	}
	
	public int getAllyCrestId()
	{
		if (getClanId() == 0)
		{
			return 0;
		}
		
		if (getClan().getAllyId() == 0)
		{
			return 0;
		}
		
		return getClan().getAllyCrestId();
	}
	
	/**
	 * Manage hit process (called by Hit Task of L2Character).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li><BR>
	 * <BR>
	 * @param target The L2Character targeted
	 * @param damage Nb of HP to reduce
	 * @param crit True if hit is critical
	 * @param miss True if hit is missed
	 * @param soulshot True if SoulShot are charged
	 * @param shld True if shield is efficient
	 */
	@Override
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
	{
		super.onHitTimer(target, damage, crit, miss, soulshot, shld);
	}
	
	/**
	 * Send a Server->Client packet StatusUpdate to the L2PcInstance.<BR>
	 * <BR>
	 */
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}
	
	/**
	 * Manage Interact Task with another L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2PcInstance</li><BR>
	 * <BR>
	 * @param target The L2Character targeted
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			final L2PcInstance temp = (L2PcInstance) target;
			sendPacket(new ActionFailed());
			
			if ((temp.getPrivateStoreType() == STORE_PRIVATE_SELL) || (temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL))
			{
				sendPacket(new PrivateStoreListSell(this, temp));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, temp));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, temp));
			}
			
		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
			{
				target.onAction(this);
			}
		}
	}
	
	/**
	 * Manage AutoLoot Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * <BR>
	 * @param target The L2ItemInstance dropped
	 * @param item
	 */
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		if (isInParty())
		{
			getParty().distributeItem(this, item, false, target);
		}
		else if (item.getItemId() == 57)
		{
			addAdena("Loot", item.getCount(), target, true);
		}
		else
		{
			addItem("Loot", item.getItemId(), item.getCount(), target, true);
		}
	}
	
	/**
	 * Manage Pickup Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet StopMove to this L2PcInstance</li>
	 * <li>Remove the L2ItemInstance from the world and send server->client GetItem packets</li>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * <BR>
	 * @param object The L2ItemInstance to pick up
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath())
		{
			return;
		}
		
		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		// Check if the L2Object to pick up is a L2ItemInstance
		if (!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warning("trying to pickup wrong target." + getTarget());
			return;
		}
		
		final L2ItemInstance target = (L2ItemInstance) object;
		
		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(new ActionFailed());
		
		// Send a Server->Client packet StopMove to this L2PcInstance
		final StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		if (Config.DEBUG)
		{
			_log.fine("pickup pos: " + target.getX() + " " + target.getY() + " " + target.getZ());
		}
		
		sendPacket(sm);
		
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(new ActionFailed());
				final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target.getItemId());
				sendPacket(smsg);
				return;
			}
			
			if (((isInParty() && (getParty().getLootDistribution() == L2Party.ITEM_LOOTER)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(new ActionFailed());
				sendPacket(new SystemMessage(SystemMessage.SLOTS_FULL));
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getObjectId()) && !isInLooterParty(target.getOwnerId()))
			
			{
				
				sendPacket(new ActionFailed());
				
				if (target.getItemId() == 57)
				
				{
					
					final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S1_ADENA);
					
					smsg.addNumber(target.getCount());
					
					sendPacket(smsg);
					
				}
				
				else if (target.getCount() > 1)
				
				{
					
					final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S2_S1_s);
					
					smsg.addItemName(target.getItemId());
					
					smsg.addNumber(target.getCount());
					
					sendPacket(smsg);
					
				}
				
				else
				
				{
					
					final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S1);
					
					smsg.addItemName(target.getItemId());
					
					sendPacket(smsg);
					
				}
				
				return;
			}
			
			if ((target.getItemLootSchedule() != null) && ((target.getOwnerId() == getObjectId()) || isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Remove the L2ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
		if ((target.getItemType() instanceof L2ArmorType) || (target.getItemType() instanceof L2WeaponType))
		{
			if (target.getEnchantLevel() > 0)
			{
				final SystemMessage msg = new SystemMessage(SystemMessage.ATTENTION_S1_PICKED_UP_S2_S3);
				msg.addString(getName());
				msg.addNumber(target.getEnchantLevel());
				msg.addItemName(target.getItemId());
				broadcastPacket(msg, 1400);
			}
			else
			{
				final SystemMessage msg = new SystemMessage(SystemMessage.ATTENTION_S1_PICKED_UP_S2);
				msg.addString(getName());
				msg.addItemName(target.getItemId());
				broadcastPacket(msg, 1400);
			}
		}
		
		// Check if a Party is in progress
		if (isInParty())
		{
			getParty().distributeItem(this, target);
		}
		else if ((target.getItemId() == 57) && (getInventory().getAdenaInstance() != null))
		{
			addAdena("Pickup", target.getCount(), null, true);
			ItemTable.getInstance().destroyItem("Pickup", target, this, null);
		}
		// Target is regular item
		else
		{
			addItem("Pickup", target, null, true);
		}
	}
	
	/**
	 * Set a target.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character</li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character</li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li><BR>
	 * <BR>
	 * @param newTarget The L2Object to target
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		// Check if the new target is visible
		if ((newTarget != null) && !newTarget.isVisible())
		{
			newTarget = null;
		}
		
		// Prevents /target exploiting
		if ((newTarget != null) && (Math.abs(newTarget.getZ() - getZ()) > 1000))
		{
			newTarget = null;
		}
		
		if (!isGM())
		{
			// Can't target and attack festival monsters if not participant
			if ((newTarget != null) && (newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant())
			{
				newTarget = null;
			}
			
			// Can't target and attack rift invaders if not in the same room
			if (isInParty() && getParty().isInDimensionalRift())
			{
				final byte riftType = getParty().getDimensionalRift().getType();
				final byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();
				if ((newTarget != null) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
				{
					newTarget = null;
				}
			}
		}
		
		// Get the current target
		final L2Object oldTarget = getTarget();
		
		if (oldTarget != null)
		{
			if ((newTarget != null) && oldTarget.equals(newTarget))
			{
				return; // no target change
			}
			
			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
			if (oldTarget instanceof L2Character)
			{
				((L2Character) oldTarget).removeStatusListener(this);
			}
		}
		
		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		if ((newTarget != null) && (newTarget instanceof L2Character))
		{
			((L2Character) newTarget).addStatusListener(this);
			final TargetSelected my = new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ());
			broadcastPacket(my);
		}
		
		if ((newTarget == null) && (getTarget() != null))
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}
	
	/**
	 * Return the active weapon instance (always equiped in the right hand).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Return the active weapon item (always equiped in the right hand).<BR>
	 * <BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		final L2ItemInstance weapon = getActiveWeaponInstance();
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}
		
		return (L2Weapon) weapon.getItem();
	}
	
	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}
	
	public L2Armor getActiveChestArmorItem()
	{
		final L2ItemInstance armor = getChestArmorInstance();
		if (armor == null)
		{
			return null;
		}
		
		return (L2Armor) armor.getItem();
	}
	
	public boolean isWearingHeavyArmor()
	{
		final L2ItemInstance armor = getChestArmorInstance();
		
		if ((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY)
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isWearingLightArmor()
	{
		final L2ItemInstance armor = getChestArmorInstance();
		
		if ((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT)
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isWearingMagicArmor()
	{
		final L2ItemInstance armor = getChestArmorInstance();
		
		if ((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC)
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isWearingFormalWear()
	{
		return _IsWearingFormalWear;
	}
	
	public void setIsWearingFormalWear(boolean value)
	{
		
		_IsWearingFormalWear = value;
	}
	
	/**
	 * Return the secondary weapon instance (always equiped in the left hand).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Return the secondary weapon item (always equiped in the left hand) or the fists weapon.<BR>
	 * <BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		final L2ItemInstance weapon = getSecondaryWeaponInstance();
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}
		
		final L2Item item = weapon.getItem();
		if (item instanceof L2Weapon)
		{
			return (L2Weapon) item;
		}
		
		return null;
	}
	
	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty</li>
	 * <li>If necessary, unsummon the Pet of the killed L2PcInstance</li>
	 * <li>Manage Karma gain for attacker and Karma loss for the killed L2PcInstance</li>
	 * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
	 * <li>Kill the L2PcInstance</li><BR>
	 * <BR>
	 * @param killer - The dead player
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2PcInstance
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (isMounted())
		{
			stopFeed();
		}
		
		if (killer != null)
		{
			final L2PcInstance pk = killer.getActingPlayer();
			if (pk != null)
			{
				if (atEvent)
				{
					pk.kills.add(getName());
				}
				
				if (pk.getEventTeam() == 1)
				{
					TvTEvent.increaseBlueKills();
				}
				else if (pk.getEventTeam() == 2)
				{
					TvTEvent.increaseRedKills();
				}
			}
			
			if (getEventTeam() > 0)
			{
				broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
				
				sendMessage("You will be respawned in 20 seconds.");
				ThreadPoolManager.getInstance().scheduleGeneral(() ->
				{
					if (!isDead() || isPendingRevive())
					{
						return;
					}
					
					setIsPendingRevive(true);
					
					if (getEventTeam() == 1)
					{
						teleToLocation(148476, 46061, -3411, true);
					}
					else if (getEventTeam() == 2)
					{
						teleToLocation(150480, 47444, -3411, true);
					}
					else
					{
						teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
				}, 20000);
			}
			else
			{
				
				// Clear resurrect xp calculation
				
				_expBeforeDeath = 0;
				
				onDieDropItem(killer); // Check if any item should be dropped
				
				if (!(isInsideZone(ZONE_PVP) && !isInsideZone(ZONE_SIEGE)))
				{
					if (Config.ALT_GAME_DELEVEL)
					{
						// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
						// NOTE: deathPenalty +- Exp will update karma
						if ((getSkillLevel(L2Skill.SKILL_LUCKY) < 0) || (getStat().getLevel() > 4))
						{
							deathPenalty(((killer instanceof L2PcInstance) && (getClan() != null) && (((L2PcInstance) killer).getClan() != null) && ((L2PcInstance) killer).getClan().isAtWarWith(getClanId())));
						}
					}
					else
					{
						onDieUpdateKarma(); // Update karma if delevel is not allowed
					}
				}
			}
		}
		
		_charges = 0;
		
		setPvpFlag(0); // Clear the pvp flag
		
		// Unsummon Cubics
		if (_cubics.size() > 0)
		
		{
			for (final L2CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			_cubics.clear();
		}
		
		if (isInParty() && getParty().isInDimensionalRift())
		{
			getParty().getDimensionalRift().memberDead(this);
		}
		
		stopRentPet();
		stopWaterTask();
		
		return true;
	}
	
	private void onDieDropItem(L2Character killer)
	{
		if (atEvent || (killer == null))
		{
			return;
		}
		
		if ((getKarma() <= 0) && (killer instanceof L2PcInstance)
			
			&& (((L2PcInstance) killer).getClan() != null) && (getClan() != null)
			
			&& (((L2PcInstance) killer).getClan().isAtWarWith(getClanId())))
		{
			return;
		}
		
		if (!isInsideZone(ZONE_PVP) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			final boolean isKillerNpc = (killer instanceof L2NpcInstance);
			final boolean hasLuckyCharm = getCharmOfLuck() && ((killer instanceof L2RaidBossInstance) || (killer instanceof L2GrandBossInstance));
			
			final int pkLimit = Config.KARMA_PK_LIMIT;
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			
			if ((getKarma() > 0) && (getPkKills() >= pkLimit))
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && (getLevel() > 4) && !hasLuckyCharm && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}
			
			int dropCount = 0;
			while ((dropPercent > 0) && (Rnd.get(100) < dropPercent) && (dropCount < dropLimit))
			{
				
				int itemDropPercent = 0;
				List<Integer> nonDroppableList = new FastList<>();
				List<Integer> nonDroppableListPet = new FastList<>();
				
				nonDroppableList = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				nonDroppableListPet = Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS;
				
				for (final L2ItemInstance itemDrop : getInventory().getItems())
				{
					if (itemDrop == null)
					{
						continue;
					}
					
					// Don't drop
					if (!itemDrop.isDropable() || (itemDrop.getItemId() == 57) || // Adena
						(itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST) || // Quest Items
						nonDroppableList.contains(itemDrop.getItemId()) || // Item listed in the non droppable item list
						nonDroppableListPet.contains(itemDrop.getItemId()) || // Item listed in the non droppable pet item list
						((getPet() != null) && (getPet().getControlItemId() == itemDrop.getItemId())))
					{
						continue;
					}
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getEquipSlot());
					}
					else
					{
						itemDropPercent = dropItem; // Item in inventory
					}
					
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);
						
						if (isKarmaDrop)
						{
							_log.warning(getName() + " has karma and dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						}
						else
						{
							_log.warning(getName() + " dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						}
						
						dropCount++;
						break;
					}
				}
			}
		}
	}
	
	private void onDieUpdateKarma()
	{
		// Karma lose for server that does not allow delevel
		if (getKarma() > 0)
		{
			// this formula seems to work relatively well:
			// baseKarma * thisLVL * (thisLVL/100)
			// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel(); // multiply by char lvl
			karmaLost *= (getLevel() / 100); // divide by 0.charLVL
			karmaLost = Math.round(karmaLost);
			if (karmaLost < 0)
			{
				karmaLost = 1;
			}
			
			// Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate packet with Karma and PvP Flag if necessary
			setKarma(getKarma() - (int) karmaLost);
		}
	}
	
	public void onKillUpdatePvPKarma(L2Character target)
	{
		if (target == null)
		{
			return;
		}
		
		if (!(target instanceof L2PlayableInstance))
		{
			return;
		}
		
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return; // Target player is null
		}
		
		if (targetPlayer == this)
		{
			return; // Target player is self
		}
		
		// If in Arena, do nothing
		if (isInsideZone(ZONE_PVP) || targetPlayer.isInsideZone(ZONE_PVP))
		{
			return;
		}
		
		// Check if it's pvp
		if ((checkIfPvP(target) && // Can pvp and
			(targetPlayer.getPvpFlag() != 0)) // Target player has pvp flag set
			|| (isInsideZone(ZONE_PVP) && // or Player is inside pvp zone and
				targetPlayer.isInsideZone(ZONE_PVP))) // Target player is inside pvp zone
		
		{
			if (target instanceof L2PcInstance)
			{
				increasePvpKills();
			}
		}
		
		else // Target player doesn't have pvp flag set
		{
			// check about wars
			if ((targetPlayer.getClan() != null) && (getClan() != null))
			{
				if (getClan().isAtWarWith(targetPlayer.getClanId()))
				{
					if (targetPlayer.getClan().isAtWarWith(getClanId()))
					{
						// 'Both way war' -> 'PvP Kill'
						if (target instanceof L2PcInstance)
						{
							increasePvpKills();
						}
						return;
					}
				}
			}
			
			// 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0) // Target player has karma
			{
				if (Config.KARMA_AWARD_PK_KILL)
				{
					if (target instanceof L2PcInstance)
					{
						increasePvpKills();
					}
				}
			}
			else if (targetPlayer.getPvpFlag() == 0)
			{
				increasePkKillsAndKarma(targetPlayer.getLevel(), target instanceof L2PcInstance);
			}
		}
		
	}
	
	/**
	 * Increase the pvp kills count and send the info to the player
	 */
	public void increasePvpKills()
	{
		// Add karma to attacker and increase its PK counter
		setPvpKills(getPvpKills() + 1);
		
		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
	}
	
	/**
	 * Increase pk count, karma and send the info to the player
	 * @param targLVL : level of the killed player
	 * @param increasePk
	 */
	public void increasePkKillsAndKarma(int targLVL, boolean increasePk)
	{
		final int baseKarma = Config.KARMA_MIN_KARMA;
		int newKarma = baseKarma;
		final int karmaLimit = Config.KARMA_MAX_KARMA;
		
		final int pkLVL = getLevel();
		final int pkPKCount = getPkKills();
		
		int lvlDiffMulti = 0;
		int pkCountMulti = 0;
		
		// Check if the attacker has a PK counter greater than 0
		if (pkPKCount > 0)
		{
			pkCountMulti = pkPKCount / 2;
		}
		else
		{
			pkCountMulti = 1;
		}
		
		if (pkCountMulti < 1)
		{
			pkCountMulti = 1;
		}
		
		// Calculate the level difference Multiplier between attacker and killed L2PcInstance
		if (pkLVL > targLVL)
		{
			lvlDiffMulti = pkLVL / targLVL;
		}
		else
		{
			lvlDiffMulti = 1;
		}
		
		if (lvlDiffMulti < 1)
		{
			lvlDiffMulti = 1;
		}
		
		// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
		newKarma *= pkCountMulti;
		newKarma *= lvlDiffMulti;
		
		// Make sure newKarma is less than karmaLimit and higher than baseKarma
		if (newKarma < baseKarma)
		{
			newKarma = baseKarma;
		}
		
		if (newKarma > karmaLimit)
		{
			newKarma = karmaLimit;
		}
		
		// Fix to prevent overflow (=> karma has a max value of 2 147 483 647)
		if (getKarma() > (Integer.MAX_VALUE - newKarma))
		{
			newKarma = Integer.MAX_VALUE - getKarma();
		}
		
		// Add karma to attacker and increase its PK counter
		
		setKarma(getKarma() + newKarma);
		if (increasePk)
		{
			setPkKills(getPkKills() + 1);
		}
		
		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
	}
	
	public int calculateKarmaLost(long exp)
	{
		// KARMA LOSS
		// When a PKer gets killed by another player or a L2MonsterInstance, it loses a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// NOTE: exp for death (if delevel is allowed) is based on the players level
		
		long expGained = Math.abs(exp);
		
		expGained /= Config.KARMA_XP_DIVIDER;
		
		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
		{
			karmaLost = Integer.MAX_VALUE;
		}
		else
		{
			karmaLost = (int) expGained;
		}
		
		if (karmaLost < Config.KARMA_LOST_BASE)
		{
			karmaLost = Config.KARMA_LOST_BASE;
		}
		
		if (karmaLost > getKarma())
		{
			karmaLost = getKarma();
		}
		
		return karmaLost;
	}
	
	public void updatePvPStatus()
	{
		
		if (isInsideZone(ZONE_PVP))
		{
			return;
		}
		
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
		
		if (getPvpFlag() == 0)
		{
			startPvPFlag();
		}
	}
	
	public void updatePvPStatus(L2Character target)
	{
		final L2PcInstance player_target = target.getActingPlayer();
		
		if (player_target == null)
		{
			return;
		}
		
		if ((!isInsideZone(ZONE_PVP) || !player_target.isInsideZone(ZONE_PVP)) && (player_target.getKarma() == 0))
		{
			if (checkIfPvP(player_target))
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			}
			
			if (getPvpFlag() == 0)
			{
				startPvPFlag();
			}
		}
	}
	
	/**
	 * Restore the specified % of experience this L2PcInstance has lost and sends a Server->Client StatusUpdate packet.<BR>
	 * <BR>
	 * @param restorePercent
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getExp()) * restorePercent) / 100));
			_expBeforeDeath = 0;
		}
	}
	
	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the Experience loss</li>
	 * <li>Set the value of _expBeforeDeath</li>
	 * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary</li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience</li><BR>
	 * <BR>
	 * @param atwar
	 */
	public void deathPenalty(boolean atwar)
	{
		// TODO Need Correct Penalty
		// Get the level of the L2PcInstance
		final int lvl = getLevel();
		
		// The death steal you some Exp
		double percentLost = (-0.07 * lvl) + 6.5;
		
		if (getKarma() > 0)
		{
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		}
		
		if (isFestivalParticipant() || atwar || isInsideZone(ZONE_SIEGE))
		{
			percentLost /= 4.0;
		}
		
		// Calculate the Experience loss
		long lostExp = 0;
		if (!atEvent)
		{
			if (lvl < Experience.MAX_LEVEL)
			{
				lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);
			}
			else
			{
				lostExp = Math.round(((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost) / 100);
			}
		}
		
		// Get the Experience before applying penalty
		_expBeforeDeath = getExp();
		
		if (Config.DEBUG)
		{
			_log.fine(getName() + " died and lost " + lostExp + " experience.");
		}
		
		// Set the new Experience value of the L2PcInstance
		getStat().addExp(-lostExp);
	}
	
	public void setPartyRoom(int id)
	{
		_partyroom = id;
	}
	
	public int getPartyRoom()
	{
		return _partyroom;
	}
	
	public boolean isInPartyMatchRoom()
	{
		return _partyroom > 0;
	}
	
	public boolean isLookingForParty()
	{
		if ((_partyroom > 0) && (_party != null))
		{
			return true;
		}
		
		final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
		if (room != null)
		{
			if (room.getOwner() == this)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Manage the increase level task of a L2PcInstance (Max MP, Max MP, recommendation, Expertise and beginner skills...).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client System Message to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL</li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance with new LEVEL, MAX_HP and MAX_MP</li>
	 * <li>Set the current HP and MP of the L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)</li>
	 * <li>Recalculate the party level</li>
	 * <li>Recalculate the number of recommendation that the L2PcInstance can give</li>
	 * <li>Give Expertise skill of this level and remove beginner Lucky skill</li><BR>
	 * <BR>
	 */
	public void increaseLevel()
	{
		// Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)
		setCurrentHpMp(getMaxHp(), getMaxMp());
		setCurrentCp(getMaxCp());
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li><BR>
	 * <BR>
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopFeed();
		clearPetData();
		storePetFood(_mountNpcId);
		stopRentPet();
		
		stopJailTask(true);
	}
	
	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR>
	 * <BR>
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.<BR>
	 * <BR>
	 * @param summon
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}
	
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}
	
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @return
	 */
	public L2Request getRequest()
	{
		return _request;
	}
	
	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @param requester
	 */
	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @return
	 */
	public L2PcInstance getActiveRequester()
	{
		if (_activeRequester != null)
		{
			if ((_activeRequester.isOnline() == 0) || _activeRequester.isRequestExpired())
			{
				_activeRequester.setLootInvitation(-1);
				_activeRequester = null;
			}
		}
		return _activeRequester;
	}
	
	/**
	 * Return True if a transaction is in progress.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isProcessingRequest()
	{
		return (getActiveRequester() != null) || (_requestExpireTime > GameTimeController.getGameTicks());
	}
	
	/**
	 * Return True if a transaction is in progress.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isProcessingTransaction()
	{
		return (getActiveRequester() != null) || (_activeTradeList != null) || (_requestExpireTime > GameTimeController.getGameTicks());
	}
	
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getGameTicks() + (REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND);
		partner.setActiveRequester(this);
	}
	
	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getGameTicks());
	}
	
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR>
	 * <BR>
	 * @param warehouse
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * Return active Warehouse.<BR>
	 * <BR>
	 * @return
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Select the TradeList to be used in next activity.<BR>
	 * <BR>
	 * @param tradeList
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * Return active TradeList.<BR>
	 * <BR>
	 * @return
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		
		final SystemMessage msg = new SystemMessage(SystemMessage.BEGIN_TRADE_WITH_S1);
		msg.addString(partner.getName());
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}
	
	public void onTradeConfirm(L2PcInstance partner)
	{
		
		final SystemMessage msg = new SystemMessage(SystemMessage.S1_CONFIRMED_TRADE);
		msg.addString(partner.getName());
		sendPacket(msg);
	}
	
	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		_activeTradeList.Lock();
		_activeTradeList = null;
		
		sendPacket(new SendTradeDone(0));
		final SystemMessage msg = new SystemMessage(SystemMessage.S1_CANCELED_TRADE);
		msg.addString(partner.getName());
		sendPacket(msg);
	}
	
	public void onTradeFinish(boolean successful)
	{
		_activeTradeList = null;
		sendPacket(new SendTradeDone(1));
		
		if (successful)
		{
			sendPacket(new SystemMessage(SystemMessage.TRADE_SUCCESSFUL));
		}
	}
	
	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		final L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
		}
		
		onTradeCancel(this);
	}
	
	/**
	 * Return the _createList object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}
	
	/**
	 * Set the _createList object of the L2PcInstance.<BR>
	 * <BR>
	 * @param x
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}
	
	/**
	 * Return the _buyList object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		
		return _sellList;
	}
	
	/**
	 * Return the _buyList object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		
		return _buyList;
	}
	
	/**
	 * Set the Private Store type of the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * <li>8 : STORE_PRIVATE_PACKAGE_SELL</li><BR>
	 * @param type
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
		
		if (Config.OFFLINE_DISCONNECT_FINISHED && (_privatestore == STORE_PRIVATE_NONE) && inOfflineMode())
		{
			logout();
		}
	}
	
	/**
	 * Return the Private Store type of the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * <li>8 : STORE_PRIVATE_PACKAGE_SELL</li><BR>
	 * @return
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}
	
	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.<BR>
	 * <BR>
	 * @param classId
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}
	
	/**
	 * Return the _skillLearningClassId object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}
	
	/**
	 * Set the _clan object, _clanId, Flag and title of the L2PcInstance.<BR>
	 * <BR>
	 * @param clan
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		
		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_activeWarehouse = null;
			return;
		}
		
		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}
		
		_clanId = clan.getClanId();
	}
	
	/**
	 * Return the _clan object of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public L2Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Return True if the L2PcInstance is the leader of its clan.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		
		return getObjectId() == getClan().getLeaderId();
	}
	
	/**
	 * Reduce the number of arrows owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).<BR>
	 * <BR>
	 */
	@Override
	protected void reduceArrowCount()
	{
		final L2ItemInstance arrows = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (arrows == null)
		{
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			sendPacket(new ItemList(this, false));
			return;
		}
		
		// Adjust item quantity
		if (arrows.getCount() > 1)
		{
			synchronized (arrows)
			{
				arrows.changeCountWithoutTrace(-1, this, null);
				arrows.setLastChange(L2ItemInstance.MODIFIED);
				
				// could do also without saving, but let's save approx 1 of 10
				if ((GameTimeController.getGameTicks() % 10) == 0)
				{
					arrows.updateDatabase();
				}
				_inventory.refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			_inventory.destroyItem("Consume", arrows, this, null);
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			
			if (Config.DEBUG)
			{
				_log.fine("removed arrows count");
			}
			
			sendPacket(new ItemList(this, false));
			return;
		}
		
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(arrows);
			sendPacket(iu);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
	}
	
	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR>
	 * <BR>
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equiped in left hand
		if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the L2ItemInstance of the arrows needed for this bow
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());
			
			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
				
				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				final ItemList il = new ItemList(this, false);
				sendPacket(il);
			}
		}
		else
		{
			// Get the L2ItemInstance of arrows equiped in left hand
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}
		
		return _arrowItem != null;
	}
	
	/**
	 * Disarm the player's weapon and shield.<BR>
	 * <BR>
	 * @return
	 */
	public boolean disarmWeapons()
	{
		// Unequip the weapon
		L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		}
		if (wpn != null)
		{
			if (wpn.isWear())
			{
				return false;
			}
			
			final L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (final L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessage.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0].getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessage.S1_DISARMED);
					sm.addItemName(unequiped[0].getItemId());
				}
				sendPacket(sm);
			}
		}
		
		// Unequip the shield
		final L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			if (sld.isWear())
			{
				return false;
			}
			
			final L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (final L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessage.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0].getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessage.S1_DISARMED);
					sm.addItemName(unequiped[0].getItemId());
				}
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Return True if the L2PcInstance use a dual weapon.<BR>
	 * <BR>
	 */
	@Override
	public boolean isUsingDualWeapon()
	{
		final L2Weapon weaponItem = getActiveWeaponItem();
		
		if (weaponItem == null)
		{
			return false;
		}
		
		if (weaponItem.getItemType() == L2WeaponType.DUAL)
		{
			return true;
		}
		else if (weaponItem.getItemType() == L2WeaponType.DUALFIST)
		{
			return true;
		}
		else if (weaponItem.getItemId() == 248)
		{
			return true;
		}
		else if (weaponItem.getItemId() == 252)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Return True if the L2PcInstance is invulnerable.<BR>
	 * <BR>
	 */
	@Override
	public boolean isInvul()
	{
		return _IsInvul || _IsTeleporting || (_protectEndTime > GameTimeController.getGameTicks());
	}
	
	/**
	 * Return True if the L2PcInstance has a Party in progress.<BR>
	 * <BR>
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Set the _party object of the L2PcInstance (without joining it).<BR>
	 * <BR>
	 * @param party
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the L2PcInstance AND join it.<BR>
	 * <BR>
	 * @param party
	 */
	public void joinParty(L2Party party)
	{
		if (party != null)
		{
			
			// First set the party otherwise this wouldn't be considered
			
			// as in a party into the L2Character.updateEffectIcons() call.
			
			_party = party;
			party.addPartyMember(this);
		}
	}
	
	/**
	 * Manage the Leave Party task of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this, true);
			_party = null;
		}
	}
	
	/**
	 * Return the _party object of the L2PcInstance.<BR>
	 * <BR>
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}
	
	/**
	 * Set the _isGm Flag of the L2PcInstance.<BR>
	 * <BR>
	 * @param status
	 */
	public void setIsGM(boolean status)
	{
		_isGm = status;
	}
	
	/**
	 * Return True if the L2PcInstance is a GM.<BR>
	 * <BR>
	 */
	@Override
	public boolean isGM()
	{
		return _isGm;
	}
	
	/**
	 * Manage a cancel cast task for the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the Intention of the AI to AI_INTENTION_IDLE</li>
	 * <li>Enable all skills (set _allSkillsDisabled to False)</li>
	 * <li>Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character (broadcast)</li><BR>
	 * <BR>
	 */
	public void cancelCastMagic()
	{
		// Set the Intention of the AI to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		// Enable all skills (set _allSkillsDisabled to False)
		enableAllSkills();
		
		// Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character (broadcast)
		final MagicSkillCanceld msc = new MagicSkillCanceld(getObjectId());
		
		// Broadcast the packet to self and known players.
		
		Broadcast.toSelfAndKnownPlayersInRadius(this, msc, 810000/* 900 */);
	}
	
	/**
	 * Set the _accessLevel of the L2PcInstance.<BR>
	 * <BR>
	 * @param level
	 */
	public void setAccessLevel(int level)
	{
		_accessLevel = level;
		
		if ((_accessLevel > 0) || Config.EVERYBODY_HAS_ADMIN_RIGHTS)
		{
			setIsGM(true);
		}
	}
	
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * Return the _accessLevel of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS && (_accessLevel <= 200))
		{
			return 200;
		}
		
		return _accessLevel;
	}
	
	@Override
	public double getLevelMod()
	{
		return ((100.0 - 11) + getLevel()) / 100.0;
	}
	
	/**
	 * Update Stats of the L2PcInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).<BR>
	 * <BR>
	 * @param broadcastType
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers (broadcast)
		
		if (broadcastType == 1)
		{
			sendPacket(new UserInfo(this));
		}
		
		if (broadcastType == 2)
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR>
	 * <BR>
	 * @param flag
	 */
	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (getPet() != null)
			{
				player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR>
	 * <BR>
	 */
	public void broadcastKarma()
	{
		final StatusUpdate su = new StatusUpdate(getObjectId());
		
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		
		sendPacket(su);
		
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (getPet() != null)
			{
				player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).<BR>
	 * <BR>
	 * @param isOnline
	 */
	public void setOnlineStatus(boolean isOnline)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		updateOnlineStatus();
	}
	
	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).<BR>
	 * <BR>
	 */
	public void updateOnlineStatus()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_Id=?"))
		{
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warning("could not set char online status:" + e);
		}
	}
	
	/**
	 * Create a new player in the characters table of the database.<BR>
	 * <BR>
	 * @return
	 */
	private boolean createDb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER))
		{
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAccuracy());
			statement.setInt(12, getCriticalHit(null, null));
			statement.setInt(13, getEvasionRate(null));
			statement.setInt(14, getMAtk(null, null));
			statement.setInt(15, getMDef(null, null));
			statement.setInt(16, getMAtkSpd());
			statement.setInt(17, getPAtk(null));
			statement.setInt(18, getPDef(null));
			statement.setInt(19, getPAtkSpd());
			statement.setInt(20, getRunSpeed());
			statement.setInt(21, getWalkSpeed());
			statement.setInt(22, getSTR());
			statement.setInt(23, getCON());
			statement.setInt(24, getDEX());
			statement.setInt(25, getINT());
			statement.setInt(26, getMEN());
			statement.setInt(27, getWIT());
			statement.setInt(28, getAppearance().getFace());
			statement.setInt(29, getAppearance().getHairStyle());
			statement.setInt(30, getAppearance().getHairColor());
			statement.setInt(31, getAppearance().getSex() ? 1 : 0);
			statement.setDouble(32, 1); // speed multiplier
			statement.setDouble(33, 1); // atk speed multiplier
			statement.setDouble(34, getCollisionRadius());
			statement.setDouble(35, getCollisionHeight());
			statement.setLong(36, getExp());
			statement.setInt(37, getSp());
			statement.setInt(38, getKarma());
			statement.setInt(39, getPvpKills());
			statement.setInt(40, getPkKills());
			statement.setInt(41, getClanId());
			statement.setInt(42, getMaxLoad());
			statement.setInt(43, getRace().ordinal());
			statement.setInt(44, getClassId().getId());
			statement.setLong(45, getDeleteTimer());
			statement.setInt(46, hasDwarvenCraft() ? 1 : 0);
			statement.setString(47, getTitle());
			statement.setInt(48, getAccessLevel());
			statement.setInt(49, isOnline());
			
			statement.setInt(50, getClanPrivileges());
			statement.setInt(51, getWantsPeace());
			statement.setInt(52, getBaseClass());
			statement.setInt(53, getNewbieState());
			
			statement.setInt(54, isNoble() ? 1 : 0);
			
			statement.setLong(55, System.currentTimeMillis());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warning("Could not insert char data: " + e);
			return false;
		}
		return true;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li>
	 * <li>Add the L2PcInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		double currentCp = 0;
		double currentHp = 0;
		double currentMp = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER))
		{
			// Retrieve the L2PcInstance from the characters table of the database
			statement.setInt(1, objectId);
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					player = L2World.getInstance().getPlayer(rset.getString("char_name"));
					if (player != null)
					{
						// exploit prevention, should not happen in normal way
						_log.severe("Attempt of double login: " + rset.getString("char_name") + "(" + objectId + ") " + rset.getString("account_name"));
						player.logout();
						return null;
					}
					
					final int activeClassId = rset.getInt("classid");
					final boolean female = rset.getInt("sex") != 0;
					final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
					final PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
					
					player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
					player.setName(rset.getString("char_name"));
					player._lastAccess = rset.getLong("lastAccess");
					
					player.getStat().setExp(rset.getLong("exp"));
					player.getStat().setLevel(rset.getByte("level"));
					player.getStat().setSp(rset.getInt("sp"));
					
					player.setClanPrivileges(rset.getInt("clan_privs"));
					player.setWantsPeace(rset.getInt("wantspeace"));
					
					player.setHeading(rset.getInt("heading"));
					
					player.setKarma(rset.getInt("karma"));
					player.setPvpKills(rset.getInt("pvpkills"));
					player.setPkKills(rset.getInt("pkkills"));
					
					player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
					
					if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
					{
						player.setClanJoinExpiryTime(0);
					}
					
					player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
					
					if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
					{
						player.setClanCreateExpiryTime(0);
					}
					
					player.setOnlineTime(rset.getLong("onlinetime"));
					
					player.setNewbieState(rset.getByte("newbie"));
					player.setNoble((rset.getInt("nobless") == 1) || Config.AUTO_NOBLE_STATUS);
					
					player.setLastRecomUpdate(rset.getLong("last_recom_date"));
					
					player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
					
					if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
					{
						player.setClanJoinExpiryTime(0);
					}
					
					player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
					
					if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
					{
						player.setClanCreateExpiryTime(0);
					}
					
					final int clanId = rset.getInt("clanid");
					if (clanId > 0)
					{
						player.setClan(ClanTable.getInstance().getClan(clanId));
					}
					
					player.setDeleteTimer(rset.getLong("deletetime"));
					
					player.setTitle(rset.getString("title"));
					player.setAccessLevel(rset.getInt("accesslevel"));
					player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
					player.setUptime(System.currentTimeMillis());
					
					currentHp = rset.getDouble("curHp");
					currentCp = rset.getDouble("curCp");
					currentMp = rset.getDouble("curMp");
					
					// Check recs
					player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));
					
					player._classIndex = 0;
					
					try
					{
						player.setBaseClass(rset.getInt("base_class"));
					}
					catch (final Exception e)
					{
						player.setBaseClass(activeClassId);
					}
					
					// Restore Subclass Data (cannot be done earlier in function)
					
					if (restoreSubClassData(player))
					{
						if (activeClassId != player.getBaseClass())
						{
							for (final SubClass subClass : player.getSubClasses().values())
							{
								if (subClass.getClassId() == activeClassId)
								{
									player._classIndex = subClass.getClassIndex();
								}
							}
						}
					}
					
					if ((player.getClassIndex() == 0) && (activeClassId != player.getBaseClass()))
					{
						// Subclass in use but doesn't exist in DB -
						// a possible restart-while-modifysubclass cheat has been attempted.
						// Switching to use base class
						player.setClassId(player.getBaseClass());
						_log.warning("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
					}
					else
					{
						player._activeClass = activeClassId;
					}
					
					player.setInJail(rset.getInt("in_jail") == 1);
					
					if (player.isInJail())
					{
						player.setJailTimer(rset.getLong("jail_timer"));
					}
					else
					{
						player.setJailTimer(0);
					}
					
					player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));
					
					player._isAIOBuffer = rset.getInt("aio_buffer") == 1;
					
					// Set the x,y,z position of the L2PcInstance and make it invisible
					player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
					
					// Retrieve the name and ID of the other characters assigned to this account.
					try (PreparedStatement stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?"))
					{
						stmt.setString(1, player._accountName);
						stmt.setInt(2, objectId);
						try (ResultSet chars = stmt.executeQuery())
						{
							while (chars.next())
							{
								final Integer charId = chars.getInt("obj_Id");
								final String charName = chars.getString("char_name");
								player._chars.put(charId, charName);
							}
						}
					}
				}
				
				if (player != null)
				{
					// Retrieve from the database all secondary data of this L2PcInstance
					// and reward expertise/lucky skills if necessary.
					player.restoreCharData();
					player.rewardSkills();
					
					if (Config.STORE_SKILL_COOLTIME)
					{
						player.restoreEffects();
					}
					
					// Restore current Cp, HP and MP values
					player.setCurrentCp(currentCp);
					player.setCurrentHp(currentHp);
					player.setCurrentMp(currentMp);
					
					if (currentHp < 0.5)
					{
						player.setIsDead(true);
						player.stopHpMpRegeneration();
					}
					
					// Restore pet if exists in the world
					player.setPet(L2World.getInstance().getPet(player.getObjectId()));
					
					if (player.getPet() != null)
					{
						player.getPet().setOwner(player);
					}
					
					// Update the overloaded status of the L2PcInstance
					player.refreshOverloaded();
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("Could not restore char data: " + e);
		}
		
		return player;
	}
	
	/**
	 * @return
	 */
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").GetChildByName(getName()));
			
			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().CreateNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").GetChildByName(getName()));
			}
		}
		
		return _forumMail;
	}
	
	/**
	 * @param forum
	 */
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}
	
	/**
	 * @return
	 */
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").GetChildByName(_accountName));
			
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().CreateNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").GetChildByName(_accountName));
			}
		}
		
		return _forumMemo;
	}
	
	/**
	 * @param forum
	 */
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}
	
	/**
	 * Restores sub-class data for the L2PcInstance, used to check the current class index for the character.
	 * @param player
	 * @return
	 */
	private static boolean restoreSubClassData(L2PcInstance player)
	{
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES))
		{
			statement.setInt(1, player.getObjectId());
			
			try (ResultSet rset = statement.executeQuery())
			
			{
				while (rset.next())
				{
					final SubClass subClass = new SubClass();
					subClass.setClassId(rset.getInt("class_id"));
					subClass.setLevel(rset.getByte("level"));
					subClass.setExp(rset.getLong("exp"));
					subClass.setSp(rset.getInt("sp"));
					subClass.setClassIndex(rset.getInt("class_index"));
					
					// Enforce the correct indexing of _subClasses against their class indexes.
					player.getSubClasses().put(subClass.getClassIndex(), subClass);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("Could not restore classes for " + player.getName() + ": " + e);
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();
		
		// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
		_macroses.restore();
		
		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();
		
		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();
		
		// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
		
		if (Config.ALT_RECOMMEND)
		{
			restoreRecom();
		}
		
		// Retrieve from the database the recipe book of this L2PcInstance.
		restoreRecipeBook(true);
		
		// Retrieve from the database all friends
		restoreFriends();
	}
	
	/**
	 * Restore recipe book data for this L2PcInstance. recipeType is the type you do not want to restore.
	 * @param loadCommon
	 */
	private void restoreRecipeBook(boolean loadCommon)
	{
		final String sql = loadCommon ? "SELECT id, type, class_index FROM character_recipebook WHERE char_id=?" : "SELECT id FROM character_recipebook WHERE char_id=? AND class_index=? AND type = 1";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(sql))
		{
			statement.setInt(1, getObjectId());
			if (!loadCommon)
			{
				statement.setInt(2, _classIndex);
			}
			
			try (ResultSet rset = statement.executeQuery())
			{
				_dwarvenRecipeBook.clear();
				
				L2RecipeList recipe;
				while (rset.next())
				{
					recipe = RecipeController.getInstance().getRecipeList(rset.getInt("id") - 1);
					if (loadCommon)
					{
						if (rset.getInt(2) == 1)
						{
							if (rset.getInt(3) == _classIndex)
							{
								registerDwarvenRecipeList(recipe, false);
							}
						}
						else
						{
							registerCommonRecipeList(recipe, false);
						}
					}
					else
					{
						registerDwarvenRecipeList(recipe, false);
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore recipe book data:" + e.getMessage(), e);
		}
	}
	
	/**
	 * Update L2PcInstance stats in the characters table of the database.<BR>
	 * <BR>
	 */
	public void store()
	{
		
		storeCharBase();
		storeCharSub();
		storeEffect(true);
	}
	
	private void storeCharBase()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER))
		{
			// Get the exp, level, and sp of base class to store in base table
			final int currentClassIndex = getClassIndex();
			_classIndex = 0;
			final long exp = getStat().getExp();
			final int level = getStat().getLevel();
			final int sp = getStat().getSp();
			_classIndex = currentClassIndex;
			
			// Update base class
			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getSTR());
			statement.setInt(9, getCON());
			statement.setInt(10, getDEX());
			statement.setInt(11, getINT());
			statement.setInt(12, getMEN());
			statement.setInt(13, getWIT());
			statement.setInt(14, getAppearance().getFace());
			statement.setInt(15, getAppearance().getHairStyle());
			statement.setInt(16, getAppearance().getHairColor());
			statement.setInt(17, getAppearance().getSex() ? 1 : 0);
			statement.setInt(18, getHeading());
			statement.setInt(19, _observerMode ? _obsX : getX());
			statement.setInt(20, _observerMode ? _obsY : getY());
			statement.setInt(21, _observerMode ? _obsZ : getZ());
			statement.setLong(22, exp);
			statement.setInt(23, sp);
			statement.setInt(24, getKarma());
			statement.setInt(25, getPvpKills());
			statement.setInt(26, getPkKills());
			statement.setInt(27, getRecomHave());
			statement.setInt(28, getRecomLeft());
			statement.setInt(29, getClanId());
			statement.setInt(30, getMaxLoad());
			statement.setInt(31, getRace().ordinal());
			statement.setInt(32, getClassId().getId());
			statement.setLong(33, getDeleteTimer());
			statement.setString(34, getTitle());
			statement.setInt(35, getAccessLevel());
			statement.setInt(36, isOnline());
			
			statement.setInt(37, getClanPrivileges());
			statement.setInt(38, getWantsPeace());
			statement.setLong(39, getClanJoinExpiryTime());
			
			statement.setLong(40, getClanCreateExpiryTime());
			statement.setInt(41, getBaseClass());
			
			long totalOnlineTime = _onlineTime;
			
			if (_onlineBeginTime > 0)
			{
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			}
			
			statement.setLong(42, totalOnlineTime);
			
			statement.setInt(43, isInJail() ? 1 : 0);
			
			statement.setLong(44, getJailTimer());
			statement.setInt(45, getNewbieState());
			
			statement.setInt(46, isNoble() ? 1 : 0);
			
			statement.setLong(47, getLastRecomUpdate());
			
			statement.setInt(48, getAllianceWithVarkaKetra());
			statement.setInt(49, isAIOBuffer() ? 1 : 0);
			
			statement.setString(50, getName());
			
			statement.setInt(51, getObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warning("Could not store char base data: " + e);
		}
	}
	
	private void storeCharSub()
	{
		if (getTotalSubClasses() <= 0)
		{
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS))
		{
			for (final SubClass subClass : getSubClasses().values())
			{
				statement.setLong(1, subClass.getExp());
				statement.setInt(2, subClass.getSp());
				statement.setInt(3, subClass.getLevel());
				statement.setInt(4, subClass.getClassId());
				statement.setInt(5, getObjectId());
				statement.setInt(6, subClass.getClassIndex());
				statement.execute();
			}
		}
		catch (final Exception e)
		{
			_log.warning("Could not store sub class data for " + getName() + ": " + e);
		}
	}
	
	private void storeEffect(boolean storeEffects)
	{
		if (!Config.STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement statement = con.prepareStatement(ADD_SKILL_SAVE))
		{
			// Delete all current stored effects for char to avoid dupe
			delete.setInt(1, getObjectId());
			delete.setInt(2, getClassIndex());
			delete.execute();
			
			int buff_index = 0;
			
			final List<Integer> storedSkills = new FastList<>();
			
			// Store all effect data along with calulated remaining
			
			// reuse delays for matching skills. 'restore_type' <= 0.
			if (storeEffects)
			{
				for (final L2Effect effect : getAllEffects())
				{
					
					if (effect == null)
					{
						continue;
					}
					
					final L2Skill skill = effect.getSkill();
					
					final int skillId = skill.getId();
					
					if (storedSkills.contains(skillId))
					{
						continue;
					}
					
					storedSkills.add(skillId);
					
					if (effect.getInUse() && !skill.isToggle())
					{
						buff_index++;
						
						statement.setInt(1, getObjectId());
						statement.setInt(2, skillId);
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, effect.getCount());
						statement.setInt(5, effect.getTime());
						
						if (_reuseTimeStamps.containsKey(skillId))
						{
							final TimeStamp t = _reuseTimeStamps.get(skillId);
							statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
							statement.setDouble(7, t.hasNotPassed() ? t.getStamp() : 0);
						}
						else
						{
							statement.setLong(6, 0);
							statement.setDouble(7, 0);
						}
						
						statement.setInt(8, effect.getPeriod() == Config.AIO_BUFF_DURATION ? -1 : 0);
						
						statement.setInt(9, getClassIndex());
						
						statement.setInt(10, buff_index);
						statement.execute();
					}
				}
			}
			
			// Store the reuse delays of remaining skills which
			
			// lost effect but still under reuse delay. 'restore_type' 1.
			
			for (final TimeStamp t : _reuseTimeStamps.values())
			{
				if (t.hasNotPassed())
				{
					final int skillId = t.getSkillId();
					
					if (storedSkills.contains(skillId))
					{
						continue;
					}
					
					storedSkills.add(skillId);
					
					buff_index++;
					
					statement.setInt(1, getObjectId());
					
					statement.setInt(2, skillId);
					
					statement.setInt(3, -1);
					
					statement.setInt(4, -1);
					
					statement.setInt(5, -1);
					
					statement.setLong(6, t.getReuse());
					
					statement.setDouble(7, t.getStamp());
					
					statement.setInt(8, 1);
					
					statement.setInt(9, getClassIndex());
					
					statement.setInt(10, buff_index);
					
					statement.execute();
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("Could not store char effect data: " + e);
		}
	}
	
	private void restoreFriends()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT character_friends.friend_id, characters.char_name, characters.online FROM character_friends, characters WHERE character_friends.char_id=? AND character_friends.friend_id = characters.obj_Id ORDER BY characters.char_name ASC"))
		
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int friendId = rset.getInt("friend_id");
					final String friendName = rset.getString("char_name");
					final int online = rset.getInt("online");
					
					final Friend friend = new Friend(friendId, friendName);
					friend.setOnline(online);
					
					getFriendList().add(friend);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("could not restore friend data:" + e);
		}
	}
	
	private void notifyFriends()
	{
		SystemMessage sm = new SystemMessage(SystemMessage.FRIEND_S1_HAS_LOGGED_IN);
		sm.addString(getName());
		
		for (final Friend friend : getFriendList())
		{
			if (friend == null)
			{
				continue;
			}
			
			// notify online friends
			if (friend.isOnline() == 1)
			{
				final L2PcInstance friendChar = L2World.getInstance().getPlayer(friend.getName());
				if (friendChar == null)
				{
					return;
				}
				
				friendChar.sendPacket(new FriendList(this));
				// friend logged in
				if (isOnline() == 1)
				{
					friendChar.sendPacket(sm);
				}
			}
		}
		sm = null;
	}
	
	/**
	 * Return True if the L2PcInstance is on line.<BR>
	 * <BR>
	 * @return
	 */
	public int isOnline()
	{
		return (_isOnline ? 1 : 0);
	}
	
	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2PcInstance are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character</li><BR>
	 * <BR>
	 * @param newSkill The L2Skill to add to the L2Character
	 * @param store
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		final L2Skill oldSkill = super.addSkill(newSkill);
		
		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}
		
		return oldSkill;
	}
	
	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the skill from the L2Character _skills</li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li><BR>
	 * <BR>
	 * @param skill The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		final L2Skill oldSkill = super.removeSkill(skill);
		if (oldSkill != null)
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR))
			{
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warning("Error could not delete skill: " + e);
			}
		}
		
		for (final L2ShortCut sc : getAllShortCuts())
		{
			if (sc == null)
			{
				continue;
			}
			
			if ((skill != null) && (sc.getId() == skill.getId()) && (sc.getType() == L2ShortCut.TYPE_SKILL))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database. <BR>
	 * <BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 * @param newSkill
	 * @param oldSkill
	 * @param newClassIndex
	 */
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;
		
		if (newClassIndex > -1)
		{
			classIndex = newClassIndex;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			if ((oldSkill != null) && (newSkill != null))
			{
				if (Config.KEEP_SUBCLASS_SKILLS)
				{
					try (PreparedStatement statement = con.prepareStatement("UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=?"))
					{
						statement.setInt(1, newSkill.getLevel());
						statement.setInt(2, oldSkill.getId());
						statement.setInt(3, getObjectId());
						statement.execute();
					}
				}
				else
				{
					try (PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL))
					{
						statement.setInt(1, newSkill.getLevel());
						statement.setInt(2, oldSkill.getId());
						statement.setInt(3, getObjectId());
						statement.setInt(4, classIndex);
						statement.execute();
					}
				}
			}
			else if (newSkill != null)
			{
				try (PreparedStatement statement = con.prepareStatement(ADD_NEW_SKILL))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, newSkill.getId());
					statement.setInt(3, newSkill.getLevel());
					statement.setString(4, newSkill.getName());
					statement.setInt(5, classIndex);
					statement.execute();
				}
			}
			else
			{
				_log.warning("could not store new skill. its NULL");
			}
		}
		catch (final Exception e)
		{
			_log.warning("Error could not store char skills: " + e);
		}
	}
	
	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.<BR>
	 * <BR>
	 */
	private void restoreSkills()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			if (Config.KEEP_SUBCLASS_SKILLS)
			{
				// Retrieve all skills of this L2PcInstance from the database
				
				try (PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR_ALT_SUBCLASS))
				{
					statement.setInt(1, getObjectId());
					try (ResultSet rset = statement.executeQuery())
					{
						// Go though the recordset of this SQL query
						while (rset.next())
						{
							final int id = rset.getInt("skill_id");
							final int level = rset.getInt("skill_level");
							
							if (id > 9000)
							{
								continue; // fake skills for base stats
							}
							
							// Create a L2Skill object for each record
							final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
							
							// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
							super.addSkill(skill);
						}
					}
				}
			}
			else
			{
				// Retrieve all skills of this L2PcInstance from the database
				try (PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, getClassIndex());
					try (ResultSet rset = statement.executeQuery())
					{
						// Go though the recordset of this SQL query
						while (rset.next())
						{
							final int id = rset.getInt("skill_id");
							final int level = rset.getInt("skill_level");
							
							if (id > 9000)
							{
								continue; // fake skills for base stats
							}
							
							// Create a L2Skill object for each record
							final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
							
							// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
							super.addSkill(skill);
							
							if (Config.SKILL_CHECK_ENABLE && !isGM())
							{
								if (!SkillTreeTable.getInstance().isSkillAllowed(this, skill))
								{
									removeSkill(skill);
									_log.warning("Removed invalid skill " + skill.getName() + " (" + skill.getId() + "/" + skill.getLevel() + ") from player " + getName() + " with class:" + getTemplate().className);
								}
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("Could not restore character skills: " + e);
		}
	}
	
	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.<BR>
	 * <BR>
	 */
	public void restoreEffects()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			
			/**
			 * Restore Type <= 0 These skill were still in effect on the character upon logout. Some of which were self casted and might still have had a long reuse delay which also is restored.
			 */
			try (PreparedStatement statement = con.prepareStatement(RESTORE_EFFECT_SAVE))
			{
				statement.setInt(1, getObjectId());
				statement.setInt(2, getClassIndex());
				
				try (ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						final int skillId = rset.getInt("skill_id");
						final int skillLvl = rset.getInt("skill_level");
						final int effectCount = rset.getInt("effect_count");
						final int effectCurTime = rset.getInt("effect_cur_time");
						
						final double reuseDelay = rset.getInt("reuse_delay");
						
						final double systime = rset.getDouble("systime");
						final byte restoreType = rset.getByte("restore_type");
						
						final double remainingTime = systime - System.currentTimeMillis();
						
						// Just incase the admin minipulated this table incorrectly :x
						
						if ((skillId == -1) || (effectCount == -1) || (effectCurTime == -1) || (reuseDelay < 0))
						{
							continue;
						}
						
						final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
						
						if (remainingTime > 10)
						
						{
							
							disableSkill(skillId, (long) reuseDelay);
							
							addTimeStamp(new TimeStamp(skillId, (long) remainingTime, (long) systime));
							
						}
						
						for (final L2Effect effect : skill.getEffects(this, this, false))
						
						{
							
							effect.setCount(effectCount);
							if (restoreType == -1)
							{
								effect.setPeriod(Config.AIO_BUFF_DURATION);
							}
							
							effect.setFirstTime(effectCurTime);
							
						}
					}
				}
			}
			
			/**
			 * Restore Type 1 The remaning skills lost effect upon logout but were still under a high reuse delay.
			 */
			if (Config.KEEP_SUBCLASS_SKILLS)
			{
				try (PreparedStatement statement = con.prepareStatement(RESTORE_SKILL_SAVE_ALT_SUBCLASS))
				{
					
					statement.setInt(1, getObjectId());
					
					try (ResultSet rset = statement.executeQuery())
					{
						
						while (rset.next())
						
						{
							
							final int skillId = rset.getInt("skill_id");
							
							final double reuseDelay = rset.getDouble("reuse_delay");
							
							final double systime = rset.getDouble("systime");
							
							final double remainingTime = systime - System.currentTimeMillis();
							
							if (remainingTime < 10)
							{
								continue;
							}
							
							disableSkill(skillId, (long) remainingTime);
							
							addTimeStamp(new TimeStamp(skillId, (long) reuseDelay, (long) systime));
							
						}
					}
				}
			}
			else
			{
				try (PreparedStatement statement = con.prepareStatement(RESTORE_SKILL_SAVE))
				{
					
					statement.setInt(1, getObjectId());
					statement.setInt(2, getClassIndex());
					
					try (ResultSet rset = statement.executeQuery())
					{
						
						while (rset.next())
						
						{
							
							final int skillId = rset.getInt("skill_id");
							
							final double reuseDelay = rset.getDouble("reuse_delay");
							
							final double systime = rset.getDouble("systime");
							
							final double remainingTime = systime - System.currentTimeMillis();
							
							if (remainingTime < 10)
							{
								continue;
							}
							
							disableSkill(skillId, (long) remainingTime);
							
							addTimeStamp(new TimeStamp(skillId, (long) reuseDelay, (long) systime));
							
						}
					}
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				statement.setInt(1, getObjectId());
				statement.setInt(2, getClassIndex());
				statement.executeUpdate();
			}
		}
		catch (final Exception e)
		{
			_log.warning("Could not restore active effect data: " + e);
		}
	}
	
	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.<BR>
	 * <BR>
	 */
	private void restoreHenna()
	{
		for (int i = 0; i < 3; i++)
		{
			_henna[i] = null;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int slot = rset.getInt("slot");
					if ((slot < 1) || (slot > 3))
					{
						continue;
					}
					
					final int symbol_id = rset.getInt("symbol_id");
					
					L2HennaInstance sym = null;
					
					if (symbol_id != 0)
					{
						final L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);
						
						if (tpl != null)
						{
							sym = new L2HennaInstance(tpl);
							_henna[slot - 1] = sym;
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("could not restore henna: " + e);
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}
	
	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.<BR>
	 * <BR>
	 */
	private void restoreRecom()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					_recomChars.add(rset.getInt("target_id"));
				}
			}
		}
		catch (final Exception e)
		{
			_log.warning("could not restore recommendations: " + e);
		}
	}
	
	/**
	 * Return the number of Henna empty slot of the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 1 + getClassId().level();
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
			{
				totalSlots--;
			}
		}
		
		if (totalSlots <= 0)
		{
			return 0;
		}
		
		return totalSlots;
	}
	
	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR>
	 * <BR>
	 * @param slot
	 * @return
	 */
	public boolean removeHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return false;
		}
		
		slot--;
		
		if (_henna[slot] == null)
		{
			return false;
		}
		
		final L2HennaInstance henna = _henna[slot];
		_henna[slot] = null;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warning("could not remove char henna: " + e);
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
		
		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));
		
		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Add the recovered dyes to the player's inventory and notify them.
		getInventory().addItem("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire() / 2, this, null);
		
		final SystemMessage sm = new SystemMessage(SystemMessage.EARNED_S2_S1_s);
		
		sm.addItemName(henna.getItemIdDye());
		
		sm.addNumber(henna.getAmountDyeRequire() / 2);
		sendPacket(sm);
		
		return true;
	}
	
	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR>
	 * <BR>
	 * @param henna
	 * @return
	 */
	public boolean addHenna(L2HennaInstance henna)
	{
		if (getHennaEmptySlots() == 0)
		{
			sendMessage("You may not draw more than 3 symbols.");
			return false;
		}
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				
				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();
				
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());
					statement.execute();
				}
				catch (final Exception e)
				{
					_log.warning("could not save char henna: " + e);
				}
				
				// Send Server->Client HennaInfo packet to this L2PcInstance
				final HennaInfo hi = new HennaInfo(this);
				sendPacket(hi);
				
				// Send Server->Client UserInfo packet to this L2PcInstance
				final UserInfo ui = new UserInfo(this);
				sendPacket(ui);
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculate Henna modifiers of this L2PcInstance.<BR>
	 * <BR>
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				continue;
			}
			
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEN();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}
		
		if (_hennaINT > 5)
		{
			_hennaINT = 5;
		}
		if (_hennaSTR > 5)
		{
			_hennaSTR = 5;
		}
		if (_hennaMEN > 5)
		{
			_hennaMEN = 5;
		}
		if (_hennaCON > 5)
		{
			_hennaCON = 5;
		}
		if (_hennaWIT > 5)
		{
			_hennaWIT = 5;
		}
		if (_hennaDEX > 5)
		{
			_hennaDEX = 5;
		}
	}
	
	/**
	 * Return the Henna of this L2PcInstance corresponding to the selected slot.<BR>
	 * <BR>
	 * @param slot
	 * @return
	 */
	public L2HennaInstance getHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return null;
		}
		
		return _henna[slot - 1];
	}
	
	/**
	 * Return the INT Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}
	
	/**
	 * Return the STR Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}
	
	/**
	 * Return the CON Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}
	
	/**
	 * Return the MEN Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}
	
	/**
	 * Return the WIT Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}
	
	/**
	 * Return the DEX Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}
	
	/**
	 * Return True if the L2PcInstance is autoAttackable.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Check if the attacker isn't the L2PcInstance Pet</li>
	 * <li>Check if the attacker is L2MonsterInstance</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same party</li>
	 * <li>Check if the L2PcInstance has Karma</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender)</li><BR>
	 * <BR>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		// Check if the attacker isn't the L2PcInstance Pet
		if ((attacker == this) || (attacker == getPet()))
		{
			return false;
		}
		
		// TODO: check for friendly mobs
		// Check if the attacker is a L2MonsterInstance
		if (attacker instanceof L2MonsterInstance)
		{
			return true;
		}
		
		// Check if the attacker is not in the same party
		if (getParty() != null)
		{
			if (getParty().getPartyMembers().contains(attacker))
			{
				return false;
			}
			
			if (attacker.getParty() != null)
			{
				if ((getParty().getCommandChannel() != null) && (attacker.getParty().getCommandChannel() != null))
				{
					if (getParty().getCommandChannel() == attacker.getParty().getCommandChannel())
					{
						return false;
					}
				}
			}
		}
		
		// Check if the attacker is in event, olympiad, and olympiad start
		if (attacker instanceof L2PcInstance)
		{
			if ((((L2PcInstance) attacker).getEventTeam() > 0) && (getEventTeam() > 0))
			{
				if (((L2PcInstance) attacker).getEventTeam() == getEventTeam())
				{
					return false;
				}
				return true;
			}
			
			if (((L2PcInstance) attacker).isInOlympiadMode())
			{
				if (isInOlympiadMode() && isOlympiadStart() && (((L2PcInstance) attacker).getOlympiadGameId() == getOlympiadGameId()))
				{
					return true;
				}
				return false;
			}
		}
		
		// Check if the attacker is not in the same clan
		if ((getClan() != null) && (attacker != null) && getClan().isMember(attacker.getObjectId()))
		{
			return false;
		}
		
		if ((attacker instanceof L2PlayableInstance) && isInsideZone(ZONE_PEACE))
		{
			return false;
		}
		
		// Check if the L2PcInstance has Karma
		if ((getKarma() > 0) || (getPvpFlag() > 0))
		{
			return true;
		}
		
		final L2PcInstance player = attacker.getActingPlayer();
		
		// Check if the attacker is a L2PcInstance
		if (player != null)
		{
			
			// Check if the L2PcInstance is in an arena or a siege area
			if (isInsideZone(ZONE_PVP) && player.isInsideZone(ZONE_PVP))
			{
				return true;
			}
			
			if (getClan() != null)
			{
				final Siege siege = SiegeManager.getInstance().getSiege(this);
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(player.getClan()) && siege.checkIsDefender(getClan()))
					{
						return false;
					}
					
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					if (siege.checkIsAttacker(player.getClan()) && siege.checkIsAttacker(getClan()))
					{
						return false;
					}
				}
				
				// Check if clan is at war
				if ((getClan() != null) && (player.getClan() != null) && (getClan().isAtWarWith(player.getClanId()) && player.getClan().isAtWarWith(getClanId()) && (getWantsPeace() == 0) && (player.getWantsPeace() == 0)))
				{
					return true;
				}
			}
		}
		else if (attacker instanceof L2SiegeGuardInstance)
		{
			if (getClan() != null)
			{
				final Siege siege = SiegeManager.getInstance().getSiege(this);
				return ((siege != null) && siege.checkIsAttacker(getClan()));
			}
		}
		
		return false;
	}
	
	/**
	 * Check if the active L2Skill can be casted.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Check if the skill isn't toggle and is offensive</li>
	 * <li>Check if the target is in the skill cast range</li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
	 * <li>Check if the caster isn't sitting</li>
	 * <li>Check if all skills are enabled and this skill is enabled</li><BR>
	 * <BR>
	 * <li>Check if the caster own the weapon needed</li><BR>
	 * <BR>
	 * <li>Check if the skill is active</li><BR>
	 * <BR>
	 * <li>Check if all casting conditions are completed</li><BR>
	 * <BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR>
	 * <BR>
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		// Check if the skill is active
		if (skill.isPassive())
		{
			// just ignore the passive skill request. why does the client send it anyway ??
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		if (isDead())
		{
			
			sendPacket(new ActionFailed());
			abortCast();
			return;
		}
		
		if (isWearingFormalWear())
		{
			sendPacket(new SystemMessage(SystemMessage.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR));
			
			sendPacket(new ActionFailed());
			abortCast();
			return;
		}
		
		if (inObserverMode())
		{
			sendPacket(new SystemMessage(SystemMessage.OBSERVERS_CANNOT_PARTICIPATE));
			sendPacket(new ActionFailed());
			abortCast();
			
			return;
		}
		
		// Check if the caster is sitting
		if (isSitting())
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessage.CANT_MOVE_SITTING));
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Get effects of the skill
			
			final L2Effect effect = getFirstEffect(skill);
			if (effect != null)
			{
				final SystemMessage sm = new SystemMessage(SystemMessage.S1_IS_ABORTED);
				sm.addString(skill.getName());
				sendPacket(sm);
				
				effect.exit();
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if the player uses "Fake Death" skill
		if (isFakeDeath())
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// ************************************* Check Casting in Progress *******************************************
		
		// If a skill is currently being used, queue this one if this is not the same
		// Note that this check is currently imperfect: getCurrentSkill() isn't always null when a skill has
		// failed to cast, or the casting is not yet in progress when this is rechecked
		if ((getCurrentSkill() != null) && isCastingNow())
		{
			// Check if new skill different from current skill in progress
			if (skill.getId() == getCurrentSkill().getSkillId())
			{
				sendPacket(new ActionFailed());
				return;
			}
			
			if (Config.DEBUG && (getQueuedSkill() != null))
			{
				_log.info(getQueuedSkill().getSkill().getName() + " is already queued for " + getName() + ".");
			}
			
			// Create a new SkillDat object and queue it in the player _queuedSkill
			if (isCastingNow())
			{
				setQueuedSkill(skill, forceUse, dontMove);
			}
			sendPacket(new ActionFailed());
			return;
		}
		
		if (getQueuedSkill() != null)
		{
			setQueuedSkill(null, false, false);
		}
		
		// ************************************* Check Target *******************************************
		
		// Create and set a L2Object containing the target of the skill
		L2Object target = null;
		
		final SkillTargetType sklTargetType = skill.getTargetType();
		final SkillType sklType = skill.getSkillType();
		
		switch (sklTargetType)
		{
			// Target the player if skill type is AURA, PARTY, CLAN or SELF
			case TARGET_AURA:
			case TARGET_AURA_UNDEAD:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_SELF:
				target = this;
				break;
			default:
				target = skill.getFirstOfTargetList(this);
				break;
		}
		
		// Check the validity of the target
		if (target == null)
		{
			
			sendPacket(new ActionFailed());
			return;
		}
		
		// ************************************* Check skill availability *******************************************
		
		// Check if a skill is disabled while attacking
		if (isSkillDisabled(skill.getId()) && (isAttackingNow() || (skill.getCastRange() < 0)))
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.S1_PREPARED_FOR_REUSE);
			sm.addSkillName(skill.getId(), skill.getLevel());
			sendPacket(sm);
			return;
		}
		
		// Check if all skills are disabled
		if (isAllSkillsDisabled())
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// ************************************* Check Consumables *******************************************
		
		// Check if the caster has enough MP
		if (getCurrentMp() < (getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)))
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_MP));
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_HP));
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the spell consummes an Item
		if (skill.getItemConsume() > 0)
		{
			// Get the L2ItemInstance consummed by the spell
			final L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enought consumed Item to cast
			if ((requiredItems == null) || (requiredItems.getCount() < skill.getItemConsume()))
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (sklType == L2Skill.SkillType.SUMMON)
				{
					final SystemMessage sm = new SystemMessage(SystemMessage.SUMMONING_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addNumber(skill.getItemConsume());
					sendPacket(sm);
					return;
					
				}
				// Send a System Message to the caster
				sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
				return;
			}
		}
		
		// ************************************* Check Casting Conditions *******************************************
		
		// Check if the caster own the weapon needed
		if (!skill.getWeaponDependancy(this))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, false))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// ************************************* Check Player State *******************************************
		
		// Abnormal effects(ex : Stun, Sleep...) are checked in L2Character useMagic()
		
		if (isFishing() && ((sklType != SkillType.PUMPING) && (sklType != SkillType.REELING) && (sklType != SkillType.FISHING)))
		{
			// Only fishing skills are available
			sendPacket(new SystemMessage(SystemMessage.ONLY_FISHING_SKILLS_NOW));
			return;
		}
		
		// ************************************* Check Skill Type *******************************************
		
		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			if ((isInsidePeaceZone(this, target)) && (getAccessLevel() < Config.GM_PEACEATTACK))
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(new SystemMessage(SystemMessage.TARGET_IN_PEACEZONE));
				sendPacket(new ActionFailed());
				return;
			}
			
			if (isInOlympiadMode() && !isOlympiadStart())
			{
				// if L2PcInstance is in Olympiad and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(new ActionFailed());
				return;
			}
			
			// Check if the target is attackable
			if (!target.isAttackable() && (getAccessLevel() < Config.GM_PEACEATTACK))
			{
				// If target is not attackable, send a Server->Client packet ActionFailed
				sendPacket(new ActionFailed());
				return;
			}
			
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse && (sklTargetType != SkillTargetType.TARGET_AURA) && (sklTargetType != SkillTargetType.TARGET_AURA_UNDEAD) && (sklTargetType != SkillTargetType.TARGET_CLAN) && (sklTargetType != SkillTargetType.TARGET_ALLY) && (sklTargetType != SkillTargetType.TARGET_PARTY) && (sklTargetType != SkillTargetType.TARGET_SELF))
			{
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
			
			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the L2PcInstance and the target
				
				if ((skill.getCastRange() > 0) && !isInsideRadius(target, skill.getCastRange() + (int) getTemplate().collisionRadius, false, false))
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessage.TARGET_TOO_FAR));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
			}
		}
		
		// Check if the skill is defensive
		if (!skill.isOffensive())
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
			if ((target instanceof L2MonsterInstance) && !forceUse
				
				&& (sklTargetType != SkillTargetType.TARGET_PET) && (sklTargetType != SkillTargetType.TARGET_AURA) && (sklTargetType != SkillTargetType.TARGET_AURA_UNDEAD) && (sklTargetType != SkillTargetType.TARGET_CLAN) && (sklTargetType != SkillTargetType.TARGET_SELF) && (sklTargetType != SkillTargetType.TARGET_PARTY) && (sklTargetType != SkillTargetType.TARGET_ALLY) && (sklTargetType != SkillTargetType.TARGET_CORPSE_MOB) && (sklTargetType != SkillTargetType.TARGET_AREA_CORPSE_MOB) && (sklType != SkillType.BEAST_FEED) && (sklType != SkillType.DELUXE_KEY_UNLOCK))
			{
				// send the action failed so that the skill doesn't go off.
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (sklType == SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
				
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if the skill is Sweep type and if conditions not apply
		if ((sklType == SkillType.SWEEP) && (target instanceof L2Attackable))
		{
			final int spoilerId = ((L2Attackable) target).getIsSpoiledBy();
			
			if (((L2Attackable) target).isDead())
			{
				if (!((L2Attackable) target).isSpoil())
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessage.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
				
				if ((getObjectId() != spoilerId) && !isInLooterParty(spoilerId))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessage.SWEEP_NOT_ALLOWED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
			}
		}
		
		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
				
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case TARGET_PARTY:
			case TARGET_ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_AURA:
			case TARGET_AURA_UNDEAD:
			case TARGET_SELF:
				break;
			default:
				if (!checkPvpSkill(target, skill) && (getAccessLevel() < Config.GM_PEACEATTACK))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
				
		}
		
		if ((sklType == SkillType.STRSIEGEASSAULT) && !SiegeManager.getInstance().checkIfOkToSummon(this, false))
		{
			sendPacket(new ActionFailed());
			abortCast();
			return;
		}
		
		// GeoData Los Check here
		if ((skill.getCastRange() > 0) && !GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(new SystemMessage(SystemMessage.CANT_SEE_TARGET));
			sendPacket(new ActionFailed());
			return;
		}
		
		// If all conditions are checked, create a new SkillDat object and set the player _currentSkill
		setCurrentSkill(skill, forceUse, dontMove);
		
		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
		
	}
	
	public boolean isInLooterParty(int looterId)
	{
		
		if (isInParty())
		{
			for (final L2PcInstance member : getParty().getPartyMembers())
			{
				if (member == null)
				{
					continue;
				}
				
				if (member.getObjectId() == looterId)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill)
	{
		return checkPvpSkill(target, skill, false);
	}
	
	public boolean checkPvpSkill(L2Object target, L2Skill skill, boolean srcIsSummon)
	{
		if (target == null)
		{
			return false;
		}
		
		final L2PcInstance targetPlayer = target.getActingPlayer();
		
		// check for PC->PC Pvp status
		if ((targetPlayer != null) && (targetPlayer != this) && // target not null and not self and
			
			!isInsideZone(ZONE_PVP) && // Pc is not in PvP zone
			!targetPlayer.isInsideZone(ZONE_PVP)) // target is not in PvP zone
		{
			
			if (skill.isPvpSkill()) // pvp skill
			
			{
				
				if ((getClan() != null) && (targetPlayer.getClan() != null))
				
				{
					
					if (getClan().isAtWarWith(targetPlayer.getClan().getClanId())
						
						&& targetPlayer.getClan().isAtWarWith(getClan().getClanId()))
					{
						return true; // in clan war player can attack whites even with sleep etc.
					}
					
					if (getClan().getClanId() == targetPlayer.getClan().getClanId())
					{
						return false;
					}
				}
				
				if ((targetPlayer.getPvpFlag() == 0) && // target's pvp flag is not set and
					
					(targetPlayer.getKarma() == 0))
				{
					return false;
				}
				
			}
			
			else if (((getCurrentSkill() != null) && !getCurrentSkill().isCtrlPressed() && skill.isOffensive() && !srcIsSummon) || ((getCurrentPetSkill() != null) && !getCurrentPetSkill().isCtrlPressed() && skill.isOffensive() && srcIsSummon))
			
			{
				if ((getClan() != null) && (targetPlayer.getClan() != null))
				{
					if (getClan().isAtWarWith(targetPlayer.getClan().getClanId()) && targetPlayer.getClan().isAtWarWith(getClan().getClanId()))
					{
						return true;
					}
				}
				
				if ((targetPlayer.getPvpFlag() == 0) && // target's pvp flag is not set and
					
					(targetPlayer.getKarma() == 0))
				{
					return false;
				}
				
			}
			
		}
		
		return true;
	}
	
	/**
	 * Reduce Item quantity of the L2PcInstance Inventory and send it a Server->Client packet InventoryUpdate.<BR>
	 * <BR>
	 */
	@Override
	public void consumeItem(int itemConsumeId, int itemCount)
	{
		if ((itemConsumeId != 0) && (itemCount != 0))
		{
			destroyItemByItemId("Consume", itemConsumeId, itemCount, null, false);
		}
	}
	
	/**
	 * Return True if the L2PcInstance is a Mage.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}
	
	public boolean isMounted()
	{
		return _mountType > 0;
	}
	
	/**
	 * Set the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern) and send a Server->Client packet InventoryUpdate to the L2PcInstance.<BR>
	 * <BR>
	 * @return
	 */
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZONE_NOLANDING))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Return the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern).<BR>
	 * <BR>
	 * @return
	 */
	public int getMountType()
	{
		return _mountType;
	}
	
	public boolean setMount(int npcId, int npcLevel, int mountType)
	{
		switch (mountType)
		{
			case 0:
				setIsFlying(false);
				setIsRiding(false);
				break; // Dismounted
			case 1:
				setIsRiding(true);
				break;
			case 2:
				setIsFlying(true);
				break; // Flying Wyvern
		}
		
		_mountType = mountType;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;
		
		return true;
	}
	
	public boolean mount(L2Summon pet)
	{
		if (!disarmWeapons())
		{
			return false;
		}
		
		for (final L2Effect e : getAllEffects())
		{
			if ((e != null) && e.getSkill().isToggle())
			{
				e.exit();
			}
		}
		
		final Ride mount = new Ride(getObjectId(), true, pet.getTemplate().npcId);
		setMount(pet.getNpcId(), pet.getLevel(), mount.getMountType());
		setMountObjectID(pet.getControlItemId());
		clearPetData();
		startFeed(pet.getNpcId());
		broadcastPacket(mount);
		
		broadcastUserInfo();
		
		pet.unSummon(this);
		
		return true;
	}
	
	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons())
		{
			return false;
		}
		
		for (final L2Effect e : getAllEffects())
		{
			if ((e != null) && e.getSkill().isToggle())
			{
				e.exit();
			}
		}
		
		final Ride mount = new Ride(getObjectId(), true, npcId);
		if (setMount(npcId, getLevel(), mount.getMountType()))
		{
			clearPetData();
			setMountObjectID(controlItemObjId);
			broadcastPacket(mount);
			broadcastUserInfo();
			if (useFood)
			{
				startFeed(npcId);
			}
			return true;
		}
		return false;
	}
	
	public boolean dismount()
	{
		final boolean wasFlying = isFlying();
		sendPacket(new SetupGauge(3, 0, 0));
		final int petId = _mountNpcId;
		
		if (setMount(0, 0, 0))
		{
			stopFeed();
			clearPetData();
			if (wasFlying)
			{
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			}
			final Ride dismount = new Ride(getObjectId(), false, 0);
			broadcastPacket(dismount);
			setMountObjectID(0);
			storePetFood(petId);
			broadcastUserInfo();
			
			return true;
		}
		return false;
	}
	
	public boolean mountPlayer(L2Summon pet)
	{
		if ((pet != null) && pet.isMountable() && !isMounted())
		{
			if (isDead())
			{
				// A strider cannot be ridden when dead
				sendPacket(new SystemMessage(SystemMessage.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD));
				sendPacket(new ActionFailed());
				return false;
			}
			else if (pet.isDead())
			{
				// A dead strider cannot be ridden.
				sendPacket(new SystemMessage(SystemMessage.DEAD_STRIDER_CANT_BE_RIDDEN));
				
				sendPacket(new ActionFailed());
				return false;
			}
			else if (pet.isInCombat() || pet.isMovementDisabled())
			{
				// A strider in battle cannot be ridden
				sendPacket(new SystemMessage(SystemMessage.STRIDER_IN_BATLLE_CANT_BE_RIDDEN));
				
				sendPacket(new ActionFailed());
				return false;
			}
			else if (isInCombat())
			{
				// A strider cannot be ridden while in battle
				sendPacket(new SystemMessage(SystemMessage.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
				
				sendPacket(new ActionFailed());
				return false;
			}
			else if (isSitting())
			{
				// A strider can be ridden only when standing
				sendPacket(new SystemMessage(SystemMessage.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING));
				
				sendPacket(new ActionFailed());
				return false;
			}
			else if (isFishing())
			{
				// You can't mount, dismount, break and drop items while fishing
				
				sendPacket(new SystemMessage(SystemMessage.CANNOT_DO_WHILE_FISHING_2));
				
				sendPacket(new ActionFailed());
				return false;
			}
			
			else if (pet.isHungry())
			{
				sendPacket(new SystemMessage(SystemMessage.HUNGRY_STRIDER_NOT_MOUNT));
				sendPacket(new ActionFailed());
				return false;
			}
			else if (getEventTeam() > 0)
			{
				sendMessage("Cannot mount while in TvT Event.");
				return false;
			}
			else if (!Util.checkIfInRange(100, this, pet, true))
			{
				sendMessage("Your pet is too far to ride it.");
				
				sendPacket(new ActionFailed());
				return false;
			}
			else if (!pet.isDead() && !isMounted())
			{
				mount(pet);
			}
		}
		else if (isRentedPet())
		{
			stopRentPet();
		}
		else if (isMounted())
		{
			if (isInCombat())
			{
				sendPacket(new ActionFailed());
				return false;
			}
			else if ((getMountType() == 2) && isInsideZone(ZONE_NOLANDING))
			{
				sendPacket(new SystemMessage(SystemMessage.NO_DISMOUNT_HERE));
				sendPacket(new ActionFailed());
				return false;
			}
			else if (isHungry())
			{
				sendPacket(new SystemMessage(SystemMessage.HUNGRY_STRIDER_NOT_MOUNT));
				sendPacket(new ActionFailed());
				return false;
			}
			else
			{
				dismount();
			}
		}
		return true;
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>. In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}
	
	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.<BR>
	 * <BR>
	 */
	public void tempInventoryDisable()
	{
		_inventoryDisable = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}
	
	/**
	 * Return True if the Inventory is disabled.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}
	
	class InventoryEnable implements Runnable
	{
		@Override
		public void run()
		{
			_inventoryDisable = false;
		}
	}
	
	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}
	
	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.<BR>
	 * <BR>
	 * @param id
	 * @param level
	 * @param givenByOther
	 */
	public void addCubic(int id, int level, boolean givenByOther)
	{
		final L2CubicInstance cubic = new L2CubicInstance(this, id, level, givenByOther);
		_cubics.put(id, cubic);
	}
	
	/**
	 * Remove a L2CubicInstance from the L2PcInstance _cubics.<BR>
	 * <BR>
	 * @param id
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}
	
	/**
	 * Return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.<BR>
	 * <BR>
	 * @param id
	 * @return
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}
	
	@Override
	public String toString()
	{
		return "player " + getName();
	}
	
	/**
	 * Return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).<BR>
	 * <BR>
	 * @return
	 */
	public int getEnchantEffect()
	{
		final L2ItemInstance wpn = getActiveWeaponInstance();
		if (wpn == null)
		{
			return 0;
		}
		
		return Math.min(127, wpn.getEnchantLevel());
	}
	
	/**
	 * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR>
	 * <BR>
	 * @param folkNpc
	 */
	public void setLastFolkNPC(L2FolkInstance folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	/**
	 * Return the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR>
	 * <BR>
	 * @return
	 */
	public L2FolkInstance getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	/**
	 * Return True if L2PcInstance is a participant in the Festival of Darkness.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}
	
	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}
	
	public void removeAutoSoulShot(int itemId)
	{
		_activeSoulShots.remove(itemId);
	}
	
	public Set<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}
	
	public void rechargeAutoSoulShot(boolean physical, boolean magic, boolean summon)
	{
		L2ItemInstance item;
		IItemHandler handler;
		
		if ((_activeSoulShots == null) || (_activeSoulShots.size() == 0))
		{
			return;
		}
		
		for (final int itemId : _activeSoulShots)
		{
			item = getInventory().getItemByItemId(itemId);
			if (item != null)
			{
				if (magic)
				{
					if (!summon)
					{
						if ((itemId == 2509) || (itemId == 2510) || (itemId == 2511) || (itemId == 2512) || (itemId == 2513) || (itemId == 2514) || (itemId == 3947) || (itemId == 3948) || (itemId == 3949) || (itemId == 3950) || (itemId == 3951) || (itemId == 3952) || (itemId == 5790))
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if ((itemId == 6646) || (itemId == 6647))
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}
				
				if (physical)
				{
					if (!summon)
					{
						if ((itemId == 1463) || (itemId == 1464) || (itemId == 1465) || (itemId == 1466) || (itemId == 1467) || (itemId == 1835) || (itemId == 5789))
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if (itemId == 6645)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}
			}
			else
			{
				removeAutoSoulShot(itemId);
			}
			
		}
	}
	
	public void disableAutoShotByCrystalType(int crystalType)
	{
		for (final int itemId : _activeSoulShots)
		{
			if (ItemTable.getInstance().getTemplate(itemId).getCrystalType() == crystalType)
			{
				disableAutoShot(itemId);
			}
		}
	}
	
	public boolean disableAutoShot(int itemId)
	{
		if (_activeSoulShots.contains(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, 0));
			
			final SystemMessage sm = new SystemMessage(SystemMessage.AUTO_USE_OF_S1_CANCELLED);
			sm.addString(ItemTable.getInstance().getTemplate(itemId).getName());
			sendPacket(sm);
			return true;
		}
		return false;
	}
	
	public void disableAutoShotsAll()
	{
		for (final int itemId : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(itemId, 0));
			final SystemMessage sm = new SystemMessage(SystemMessage.AUTO_USE_OF_S1_CANCELLED);
			sm.addString(ItemTable.getInstance().getTemplate(itemId).getName());
			sendPacket(sm);
		}
		_activeSoulShots.clear();
	}
	
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	class WarnUserTakeBreak implements Runnable
	{
		@Override
		public void run()
		{
			if (isOnline() == 1)
			{
				final SystemMessage msg = new SystemMessage(764);
				sendPacket(msg);
			}
			else
			{
				stopWarnUserTakeBreak();
			}
		}
	}
	
	class RentPetTask implements Runnable
	{
		@Override
		public void run()
		{
			stopRentPet();
		}
	}
	
	public ScheduledFuture<?> _taskforfish;
	
	class WaterTask implements Runnable
	{
		@Override
		public void run()
		{
			double reduceHp = getMaxHp() / 100;
			if (reduceHp < 1)
			{
				reduceHp = 1;
			}
			
			reduceCurrentHp(reduceHp, L2PcInstance.this, false);
			// reduced hp, because of not resting
			final SystemMessage sm = new SystemMessage(297);
			sm.addNumber((int) reduceHp);
			sendPacket(sm);
		}
	}
	
	class LookingForFishTask implements Runnable
	{
		boolean _isNoob;
		int _fishType, _fishGutsCheck, _gutsCheckTime;
		long _endTaskTime;
		
		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			
		}
		
		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= _endTaskTime)
			{
				endFishing(false);
				return;
			}
			
			if (_fishType == -1)
			{
				return;
			}
			
			if (_fishGutsCheck > Rnd.get(1000))
			{
				stopLookingForFishTask();
				startFishCombat(_isNoob);
				
			}
			
		}
	}
	
	public int getClanPrivileges()
	
	{
		return _clanPrivileges;
	}
	
	public void setClanPrivileges(int n)
	
	{
		_clanPrivileges = n;
	}
	
	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}
	
	public void enterObserverMode(int x, int y, int z)
	{
		abortCast();
		
		if (getPet() != null)
		{
			getPet().unSummon(this);
		}
		
		if (getParty() != null)
		{
			getParty().removePartyMember(this, true);
		}
		
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();
		
		setTarget(null);
		stopMove(null);
		setIsParalyzed(true);
		
		setIsInvul(true);
		getAppearance().setInvisible();
		sendPacket(new ObservationMode(x, y, z));
		getKnownList().removeAllKnownObjects();
		setXYZ(x, y, z);
		
		_observerMode = true;
		broadcastPacket(new CharInfo(this));
	}
	
	public void enterOlympiadObserverMode(int x, int y, int z, int id, boolean storeCoords)
	{
		
		_olympiadGameId = id;
		
		if (getPet() != null)
		{
			getPet().unSummon(this);
		}
		
		if (getCubics().size() > 0)
		{
			for (final L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			getCubics().clear();
		}
		
		if (getParty() != null)
		{
			getParty().removePartyMember(this, true);
		}
		
		if (isSitting())
		{
			standUp();
		}
		
		if (storeCoords)
		{
			_obsX = getX();
			_obsY = getY();
			_obsZ = getZ();
		}
		
		setTarget(null);
		
		setIsInvul(true);
		
		getAppearance().setInvisible();
		
		teleToLocation(x, y, z, false);
		
		sendPacket(new ExOlympiadMode(3));
		_observerMode = true;
		
	}
	
	public void leaveObserverMode()
	{
		setTarget(null);
		getKnownList().removeAllKnownObjects();
		setXYZ(_obsX, _obsY, _obsZ);
		setIsParalyzed(false);
		getAppearance().setVisible();
		
		setIsInvul(false);
		
		if (getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		setFalling();
		
		_observerMode = false;
		sendPacket(new ObservationReturn(this));
		broadcastPacket(new CharInfo(this));
	}
	
	public void leaveOlympiadObserverMode()
	{
		
		setTarget(null);
		sendPacket(new ExOlympiadMatchEnd());
		sendPacket(new ExOlympiadMode(0));
		
		teleToLocation(_obsX, _obsY, _obsZ, true);
		
		getAppearance().setVisible();
		
		setIsInvul(false);
		
		if (getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		_olympiadGameId = -1;
		_observerMode = false;
		
		broadcastPacket(new CharInfo(this));
		
	}
	
	public void setOlympiadSide(int i)
	{
		
		_olympiadSide = i;
		
	}
	
	public int getOlympiadSide()
	{
		return _olympiadSide;
		
	}
	
	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}
	
	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	public int getObsX()
	{
		return _obsX;
	}
	
	public int getObsY()
	{
		return _obsY;
	}
	
	public int getObsZ()
	{
		return _obsZ;
	}
	
	public boolean inObserverMode()
	{
		return _observerMode;
	}
	
	public int getTeleMode()
	{
		return _telemode;
	}
	
	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}
	
	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}
	
	public int getLoto(int i)
	{
		return _loto[i];
	}
	
	public void setRace(int i, int val)
	{
		_race[i] = val;
	}
	
	public int getRace(int i)
	{
		return _race[i];
	}
	
	public void setChatBanned(boolean isBanned)
	{
		_chatBanned = isBanned;
		
		if (isChatBanned())
		{
			sendMessage("You have been chat-banned for inappropriate language.");
		}
		else
		
		{
			sendMessage("Your chat ban has been lifted.");
			
			if (_chatUnbanTask != null)
			{
				_chatUnbanTask.cancel(false);
			}
			
			_chatUnbanTask = null;
		}
	}
	
	public boolean isChatBanned()
	{
		return _chatBanned;
	}
	
	public void setChatUnbanTask(ScheduledFuture<?> task)
	
	{
		
		_chatUnbanTask = task;
		
	}
	
	public ScheduledFuture<?> getChatUnbanTask()
	{
		
		return _chatUnbanTask;
		
	}
	
	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}
	
	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}
	
	public boolean getDietMode()
	{
		return _dietMode;
	}
	
	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}
	
	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}
	
	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}
	
	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}
	
	public BlockList getBlockList()
	{
		return _blockList;
	}
	
	public void setHero(boolean hero)
	{
		_hero = hero;
	}
	
	public void setIsInOlympiadMode(boolean b)
	
	{
		_inOlympiadMode = b;
		
	}
	
	public void setIsOlympiadStart(boolean b)
	{
		_OlympiadStart = b;
	}
	
	public boolean isHero()
	{
		return _hero;
	}
	
	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}
	
	public boolean isOlympiadStart()
	{
		return _OlympiadStart;
	}
	
	public boolean isNoble()
	{
		
		return _noble;
	}
	
	public void setNoble(boolean val)
	{
		if (val)
		{
			for (final L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				addSkill(s, false);
			}
		}
		
		else
		{
			for (final L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				removeSkill(s);
			}
		}
		
		// Update user info only if needed
		if (_noble != val)
		{
			sendPacket(new UserInfo(this));
		}
		
		_noble = val;
	}
	
	public void setEventTeam(int team)
	{
		_team = team;
	}
	
	public int getEventTeam()
	{
		return _team;
	}
	
	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}
	
	public int getWantsPeace()
	{
		return _wantsPeace;
	}
	
	public boolean isFishing()
	{
		return _fishing;
	}
	
	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}
	
	public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
	{
		// [-5,-1] varka, 0 neutral, [1,5] ketra
		_alliedVarkaKetra = sideAndLvlOfAlliance;
	}
	
	public int getAllianceWithVarkaKetra()
	{
		return _alliedVarkaKetra;
	}
	
	public boolean isAlliedWithVarka()
	{
		return (_alliedVarkaKetra < 0);
	}
	
	public boolean isAlliedWithKetra()
	{
		return (_alliedVarkaKetra > 0);
	}
	
	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<BR>
	 * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
	 * @param classId
	 * @param classIndex
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if (!_classLock.tryLock())
		{
			return false;
		}
		
		try
		{
			if ((getTotalSubClasses() == Config.ALT_MAX_SUBCLASS) || (classIndex == 0))
			{
				return false;
			}
			
			if (getSubClasses().containsKey(classIndex))
			{
				return false;
			}
			
			// Note: Never change _classIndex in any method other than setActiveClass().
			
			final SubClass newClass = new SubClass();
			newClass.setClassId(classId);
			newClass.setClassIndex(classIndex);
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS))
			{
				// Store the basic info about this new sub-class.
				statement.setInt(1, getObjectId());
				statement.setInt(2, newClass.getClassId());
				statement.setLong(3, newClass.getExp());
				statement.setInt(4, newClass.getSp());
				statement.setInt(5, newClass.getLevel());
				statement.setInt(6, newClass.getClassIndex()); // <-- Added
				statement.execute();
				
			}
			catch (final Exception e)
			{
				_log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
				return false;
			}
			
			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);
			
			if (Config.DEBUG)
			{
				_log.info(getName() + " added class ID " + classId + " as a sub class at index " + classIndex + ".");
			}
			
			final ClassId subTemplate = ClassId.values()[classId];
			final Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);
			
			if (skillTree == null)
			{
				return true;
			}
			
			final Map<Integer, L2Skill> prevSkillList = new FastMap<>();
			
			for (final L2SkillLearn skillInfo : skillTree)
			{
				if (skillInfo.getMinLevel() <= 40)
				{
					final L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
					final L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());
					
					if ((prevSkill != null) && (prevSkill.getLevel() > newSkill.getLevel()))
					{
						continue;
					}
					
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				}
			}
			
			if (Config.DEBUG)
			{
				_log.info(getName() + " was given " + getAllSkills().length + " skills for their new sub class.");
			}
			
			return true;
		}
		finally
		{
			_classLock.unlock();
		}
		
	}
	
	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
	 * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
	 * @param classIndex
	 * @param newClassId
	 * @return boolean subclassAdded
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		if (!_classLock.tryLock())
		{
			return false;
		}
		
		try
		{
			final int oldClassId = getSubClasses().get(classIndex).getClassId();
			
			if (Config.DEBUG)
			{
				_log.info(getName() + " has requested to modify sub class index " + classIndex + " from class ID " + oldClassId + " to " + newClassId + ".");
			}
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				// Remove all henna info stored for this sub-class.
				try (PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNAS))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, classIndex);
					statement.execute();
				}
				
				// Remove all shortcuts info stored for this sub-class.
				try (PreparedStatement statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, classIndex);
					statement.execute();
				}
				
				// Remove all effects info stored for this sub-class.
				try (PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, classIndex);
					statement.execute();
				}
				
				// Remove all skills info stored for this sub-class.
				try (PreparedStatement statement = con.prepareStatement(DELETE_CHAR_SKILLS))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, classIndex);
					statement.execute();
				}
				
				// Remove all basic info stored about this sub-class.
				try (PreparedStatement statement = con.prepareStatement(DELETE_CHAR_SUBCLASS))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, classIndex);
					statement.execute();
				}
			}
			catch (final Exception e)
			{
				_log.warning("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);
				
				// This must be done in order to maintain data consistency.
				getSubClasses().remove(classIndex);
				
				return false;
			}
			
			getSubClasses().remove(classIndex);
		}
		finally
		{
			_classLock.unlock();
		}
		
		return addSubClass(newClassId, classIndex);
	}
	
	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}
	
	public Map<Integer, SubClass> getSubClasses()
	{
		if (_subClasses == null)
		{
			_subClasses = new FastMap<>();
		}
		
		return _subClasses;
	}
	
	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}
	
	public int getBaseClass()
	{
		return _baseClass;
	}
	
	public int getActiveClass()
	{
		return _activeClass;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	private void setClassTemplate(int classId)
	
	{
		
		_activeClass = classId;
		
		final L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);
		if (t == null)
		
		{
			
			_log.severe("Missing template for classId: " + classId);
			
			throw new Error();
			
		}
		
		// Set the template of the L2PcInstance
		
		setTemplate(t);
		
	}
	
	/**
	 * Changes the character's class based on the given class index. <BR>
	 * <BR>
	 * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's sub-classes respectively.
	 * @param classIndex
	 * @return
	 */
	public boolean setActiveClass(int classIndex)
	{
		if (!_classLock.tryLock())
		{
			return false;
		}
		
		try
		{
			/*
			 * 1. Call store() before modifying _classIndex to avoid skill effects rollover. 2. Register the correct _classId against applied 'classIndex'.
			 */
			storeCharBase();
			storeCharSub();
			
			storeEffect(false);
			
			_reuseTimeStamps.clear();
			
			if (classIndex == 0)
			{
				setClassTemplate(getBaseClass());
			}
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getClassId());
				}
				catch (final Exception e)
				{
					_log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
					return false;
				}
			}
			
			_classIndex = classIndex;
			
			if (isInParty())
			{
				getParty().recalculatePartyLevel();
			}
			
			/*
			 * Update the character's change in class status. 1. Remove any active cubics from the player. 2. Renovate the characters table in the database with the new class info, storing also buff/effect data. 3. Remove all existing skills. 4. Restore all the learned skills for the current class
			 * from the database. 5. Restore effect/buff data for the new class. 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones. 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes. 8. Restore shortcut data
			 * related to this class. 9. Resend a class change animation effect to broadcast to all nearby players. 10.Unsummon any active servitor from the player.
			 */
			if ((getPet() != null) && (getPet() instanceof L2SummonInstance))
			{
				getPet().unSummon(this);
			}
			
			if (getCubics().size() > 0)
			
			{
				
				for (final L2CubicInstance cubic : getCubics().values())
				
				{
					cubic.stopAction();
					cubic.cancelDisappear();
				}
				
				getCubics().clear();
			}
			
			for (final L2Skill oldSkill : getAllSkills())
			{
				if (oldSkill.getSkillType() == L2Skill.SkillType.ITEM_SA)
				{
					continue;
				}
				
				super.removeSkill(oldSkill);
				
			}
			
			stopAllEffects();
			_charges = 0;
			
			restoreRecipeBook(false);
			
			restoreSkills();
			
			rewardSkills();
			// Prevents some issues when changing between subclasses that share skills
			if ((_disabledSkills != null) && !_disabledSkills.isEmpty())
			{
				_disabledSkills.clear();
			}
			restoreEffects();
			
			updateEffectIcons();
			sendPacket(new EtcStatusUpdate(this));
			
			if (isNoble())
			{
				setNoble(true);
			}
			
			if (isClanLeader() && (getClan().getLevel() > 3))
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
			
			getSkillList();
			
			// If player has quest 422: Repent Your Sins, remove it
			final QuestState st = getQuestState("422_RepentYourSins");
			if (st != null)
			{
				st.exitQuest(true);
			}
			
			for (int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}
			
			restoreHenna();
			sendPacket(new HennaInfo(this));
			
			if (getCurrentHp() > getMaxHp())
			{
				setCurrentHp(getMaxHp());
			}
			if (getCurrentMp() > getMaxMp())
			{
				setCurrentMp(getMaxMp());
			}
			if (getCurrentCp() > getMaxCp())
			{
				setCurrentCp(getMaxCp());
			}
			
			refreshOverloaded();
			refreshExpertisePenalty();
			broadcastUserInfo();
			
			// Clear resurrect xp calculation
			_expBeforeDeath = 0;
			
			_shortCuts.restore();
			sendPacket(new ShortCutInit(this));
			
			sendPacket(new ExStorageMaxCount(this));
			broadcastPacket(new SocialAction(getObjectId(), 15));
			
			return true;
		}
		finally
		{
			_classLock.unlock();
		}
	}
	
	public boolean isLocked()
	{
		return _classLock.isLocked();
	}
	
	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(false);
			_taskWarnUserTakeBreak = null;
		}
	}
	
	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(), 7200000, 7200000);
		}
	}
	
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to town before unmounting
			if (checkLandingState() && (getMountType() == 2))
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			
			if (dismount())
			{
				_taskRentPet.cancel(true);
				
				_taskRentPet = null;
				
			}
		}
	}
	
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
		{
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000, seconds * 1000);
		}
	}
	
	public boolean isRentedPet()
	{
		if (_taskRentPet != null)
		{
			return true;
		}
		
		return false;
	}
	
	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);
			_taskWater = null;
			sendPacket(new SetupGauge(2, 0));
		}
	}
	
	public void startWaterTask()
	{
		if (!isDead() && (_taskWater == null))
		{
			final int timeinwater = (int) calcStat(Stats.BREATH, 60000, this, null);
			
			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}
	
	public void checkWaterState()
	{
		if (isInsideZone(ZONE_WATER))
		{
			startWaterTask();
		}
		else
		{
			stopWaterTask();
		}
	}
	
	public void onPlayerEnter()
	{
		// notify friends about login
		notifyFriends();
		
		// send the list
		sendPacket(new FriendList(this));
		
		startWarnUserTakeBreak();
		
		// jail task
		updateJailState();
		
		if (_IsInvul)
		{
			sendMessage("Entering world in Invulnerable mode.");
		}
		if (getAppearance().getInvisible())
		{
			sendMessage("Entering world in Invisible mode.");
		}
		if (getMessageRefusal())
		{
			sendMessage("Entering world in Message Refusal mode.");
		}
		
		revalidateZone(true);
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	private void checkRecom(int recsHave, int recsLeft)
	{
		final Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);
		
		final Calendar min = Calendar.getInstance();
		
		_recomHave = recsHave;
		_recomLeft = recsLeft;
		
		if ((getStat().getLevel() < 10) || check.after(min))
		{
			return;
		}
		
		restartRecom();
	}
	
	public void restartRecom()
	{
		
		if (Config.ALT_RECOMMEND)
		{
			_recomChars.clear();
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS))
			{
				statement.setInt(1, getObjectId());
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warning("could not clear char recommendations: " + e);
			}
		}
		
		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}
		
		if (_recomHave < 0)
		{
			_recomHave = 0;
		}
		
		// If we have to update last update time, but it's now before 13, we should set it to yesterday
		
		final Calendar update = Calendar.getInstance();
		
		if (update.get(Calendar.HOUR_OF_DAY) < 13)
		{
			update.add(Calendar.DAY_OF_MONTH, -1);
		}
		
		update.set(Calendar.HOUR_OF_DAY, 13);
		
		_lastRecomUpdate = update.getTimeInMillis();
	}
	
	public int getBoatId()
	{
		return _boatId;
	}
	
	public void setBoatId(int boatId)
	{
		_boatId = boatId;
	}
	
	@Override
	public void doRevive()
	{
		super.doRevive();
		
		updateEffectIcons();
		
		sendPacket(new EtcStatusUpdate(this));
		
		_ReviveRequested = 0;
		
		_RevivePower = 0;
		
		if (isInParty() && getParty().isInDimensionalRift())
		{
			getParty().getDimensionalRift().memberRessurected(this);
		}
		
		if (isMounted())
		{
			startFeed(_mountNpcId);
		}
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		// Restore the player's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}
	
	public void ReviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if (getEventTeam() > 0)
		{
			return;
		}
		
		if (_ReviveRequested == 1)
		{
			
			if (_RevivePet == Pet)
			{
				Reviver.sendPacket(new SystemMessage(1513)); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
				{
					Reviver.sendPacket(new SystemMessage(1515)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				}
				else
				{
					Reviver.sendPacket(new SystemMessage(1511)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
				}
			}
			return;
		}
		
		if ((Pet && (getPet() != null) && getPet().isDead()) || (!Pet && isDead()))
		{
			_ReviveRequested = 1;
			_RevivePower = Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), Reviver);
			_RevivePet = Pet;
			sendPacket(new ConfirmDlg(SystemMessage.RESSURECTION_REQUEST, Reviver.getName()));
		}
		
	}
	
	public void ReviveAnswer(int answer)
	{
		if ((_ReviveRequested != 1) || (!isDead() && !_RevivePet) || (_RevivePet && (getPet() != null) && !getPet().isDead()))
		{
			return;
		}
		
		if (getEventTeam() > 0)
		{
			return;
		}
		
		if (answer == 1)
		{
			if (!_RevivePet)
			{
				if (_RevivePower != 0)
				{
					doRevive(_RevivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (getPet() != null)
			{
				if (_RevivePower != 0)
				{
					getPet().doRevive(_RevivePower);
				}
				else
				{
					getPet().doRevive();
				}
			}
		}
		_ReviveRequested = 0;
		_RevivePower = 0;
	}
	
	public boolean isReviveRequested()
	{
		return (_ReviveRequested == 1);
	}
	
	public boolean isRevivingPet()
	{
		return _RevivePet;
	}
	
	public void removeReviving()
	{
		_ReviveRequested = 0;
		_RevivePower = 0;
	}
	
	public void onActionRequest()
	{
		setProtection(false);
	}
	
	/**
	 * @param expertiseIndex The expertiseIndex to set.
	 */
	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}
	
	/**
	 * @return Returns the expertiseIndex.
	 */
	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}
	
	@Override
	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset)
	{
		if (isInBoat())
		{
			setBoat(null);
		}
		
		super.teleToLocation(x, y, z, allowRandomOffset);
	}
	
	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		
		// Force a revalidation
		revalidateZone(true);
		
		if ((Config.PLAYER_SPAWN_PROTECTION > 0) && !isInOlympiadMode())
		{
			setProtection(true);
		}
		
		// Modify the position of the pet if necessary
		if (getPet() != null)
		{
			getPet().setFollowStatus(false);
			
			getPet().teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), false);
			
			if (getPet() == null)
			{
				return;
			}
			
			((L2SummonAI) getPet().getAI()).setStartFollowController(true);
			getPet().setFollowStatus(true);
			getPet().updateAndBroadcastStatus(0);
			
		}
	}
	
	public void setLastPartyPosition(int x, int y, int z)
	{
		_lastPartyPosition.setXYZ(x, y, z);
	}
	
	public int getLastPartyPositionDistance(int x, int y, int z)
	{
		final double dx = (x - _lastPartyPosition.getX());
		final double dy = (y - _lastPartyPosition.getY());
		final double dz = (z - _lastPartyPosition.getZ());
		
		return (int) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	public Point3D getLastServerPosition()
	{
		return _lastServerPosition;
	}
	
	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x, y, z);
	}
	
	public int getLastServerDistance(int x, int y, int z)
	{
		final double dx = (x - _lastServerPosition.getX());
		final double dy = (y - _lastServerPosition.getY());
		final double dz = (z - _lastServerPosition.getZ());
		
		return (int) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		getStat().addExpAndSp(addToExp, addToSp);
	}
	
	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp);
	}
	
	@Override
	public void reduceCurrentHp(double i, L2Character attacker)
	{
		getStatus().reduceHp(i, attacker);
		
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}
	
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake)
	{
		getStatus().reduceHp(value, attacker, awake);
		
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}
	
	public void broadcastSnoop(int type, String name, String _text)
	{
		if (_SnoopListener.size() > 0)
		{
			final Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
			
			for (final L2PcInstance pci : _SnoopListener)
			{
				if (pci != null)
				{
					pci.sendPacket(sn);
				}
			}
		}
	}
	
	public void addSnooper(L2PcInstance pci)
	{
		if (!_SnoopListener.contains(pci))
		{
			_SnoopListener.add(pci);
		}
	}
	
	public void removeSnooper(L2PcInstance pci)
	{
		_SnoopListener.remove(pci);
	}
	
	public void addSnooped(L2PcInstance pci)
	{
		if (!_SnoopedPlayer.contains(pci))
		{
			_SnoopedPlayer.add(pci);
		}
	}
	
	public void removeSnooped(L2PcInstance pci)
	{
		_SnoopedPlayer.remove(pci);
	}
	
	public synchronized void addBypass(String bypass)
	{
		
		if (bypass == null)
		{
			return;
		}
		
		_validBypass.add(bypass);
		
	}
	
	public synchronized void addBypass2(String bypass)
	{
		
		if (bypass == null)
		{
			return;
		}
		
		_validBypass2.add(bypass);
	}
	
	public synchronized boolean validateBypass(String cmd)
	{
		if (!Config.BYPASS_VALIDATION)
		{
			return true;
		}
		
		for (final String bp : _validBypass)
		{
			
			if (bp == null)
			{
				continue;
			}
			
			if (bp.equals(cmd))
			{
				return true;
			}
		}
		
		for (final String bp : _validBypass2)
		{
			
			if (bp == null)
			{
				continue;
			}
			
			if (cmd.startsWith(bp))
			{
				return true;
			}
		}
		
		_log.warning("[L2PcInstance] player [" + getName() + "] sent invalid bypass '" + cmd + "', ban this player!");
		return false;
	}
	
	public boolean validateItemManipulation(int objectId, String action)
	{
		final L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (((getPet() != null) && (getPet().getControlItemId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			
			return false;
		}
		
		if ((getActiveEnchantItem() != null) && (getActiveEnchantItem().getObjectId() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			
			return false;
		}
		
		if (item.isWear())
		{
			return false;
		}
		
		return true;
	}
	
	public synchronized void clearBypass()
	{
		
		_validBypass.clear();
		
		_validBypass2.clear();
		
	}
	
	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return _boat != null;
	}
	
	/**
	 * @return
	 */
	public L2BoatInstance getBoat()
	{
		return _boat;
	}
	
	/**
	 * @param boat
	 */
	public void setBoat(L2BoatInstance boat)
	{
		if ((boat == null) && (_boat != null))
		{
			_boat.removePassenger(this);
			setInsideZone(L2Character.ZONE_PEACE, false);
		}
		else if ((_boat == null) && (boat != null))
		{
			setInsideZone(L2Character.ZONE_PEACE, true);
		}
		_boat = boat;
	}
	
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}
	
	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}
	
	/**
	 * @return
	 */
	public Point3D getInBoatPosition()
	{
		return _inBoatPosition;
	}
	
	public void setInBoatPosition(Point3D pt)
	{
		_inBoatPosition = pt;
	}
	
	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode</li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * <li>Cancel Crafting, Attak or Cast</li>
	 * <li>Remove the L2PcInstance from the world</li>
	 * <li>Stop Party and Unsummon Pet</li>
	 * <li>Update database with items in its inventory and remove them from the world</li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI</li>
	 * <li>Close the connection with the client</li><BR>
	 * <BR>
	 */
	public void deleteMe()
	{
		try
		{
			abortAttack();
			abortCast();
			stopMove(null);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Stop crafting, if in progress
		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Cancel Attack or Cast
		try
		{
			setTarget(null);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Remove from world regions zones
		final L2WorldRegion oldRegion = getWorldRegion();
		
		// Remove the L2PcInstance from the world
		
		try
		{
			decayMe();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		if (oldRegion != null)
		{
			oldRegion.removeFromZones(this);
		}
		
		// If a Party is in progress, leave it
		try
		{
			leaveParty();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		try
		{
			if (_partyroom != 0)
			{
				final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
				if (room != null)
				{
					room.deleteMember(this);
				}
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		try
		{
			if (Olympiad.getInstance().isRegisteredInComp(this))
			{
				Olympiad.getInstance().removeDisconnectedCompetitor(this);
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		try
		{
			if (TvTEvent.isEnabled)
			{
				TvTEvent.removePlayer(this);
			}
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		try
		{
			Olympiad.clearOfflineObservers(this);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
				// dead pet wasnt unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
				if (getPet() != null)
				{
					getPet().broadcastNpcInfo(0);
				}
			}
			catch (final Exception e)
			{
				_log.log(Level.SEVERE, "deletedMe()", e);
			}
		}
		
		if (getClan() != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				final L2ClanMember clanMember = getClan().getClanMember(getObjectId());
				if (clanMember != null)
				{
					clanMember.setPlayerInstance(null);
				}
			}
			catch (final Exception e)
			{
				_log.log(Level.SEVERE, "deletedMe()", e);
			}
		}
		
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			getActiveRequester().setLootInvitation(-1);
			setActiveRequester(null);
		}
		
		// If the L2PcInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch (final Exception e)
			{
				_log.log(Level.SEVERE, "deletedMe()", e);
			}
		}
		
		try
		{
			// Check if the L2PcInstance is in observer mode to set its position to its position
			// before entering in observer mode
			if (inObserverMode())
			{
				setXYZInvisible(_obsX, _obsY, _obsZ);
			}
			else if (isInBoat())
			{
				getBoat().oustPlayer(this);
			}
			
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Update database with items in its inventory and remove them from the world
		try
		{
			getInventory().deleteMe();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Update database with items in its warehouse and remove them from the world
		try
		{
			clearWarehouse();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().remCacheTask(this);
		}
		
		// Update database with items in its freight and remove them from the world
		try
		{
			getFreight().deleteMe();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		// notify friends about disconnect
		try
		{
			notifyFriends();
			getFriendList().clear();
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "deletedMe()", e);
		}
		
		if (getClanId() > 0)
		{
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		}
		
		for (final L2PcInstance player : _SnoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (final L2PcInstance player : _SnoopListener)
		{
			player.removeSnooped(this);
		}
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		
		L2World.getInstance().removeFromAllPlayers(this);
		
		// update bbs
		try
		{
			RegionBBSManager.getInstance().changeCommunityBoard();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Exception on deleteMe() changeCommunityBoard: " + e.getMessage(), e);
		}
	}
	
	private FishData _fish;
	
	public void startFishing(int _x, int _y, int _z)
	{
		stopMove(null);
		
		setIsImmobilized(true);
		_fishing = true;
		_fishx = _x;
		_fishy = _y;
		_fishz = _z;
		broadcastUserInfo();
		// Starts fishing
		final int lvl = getRandomFishLvl();
		final int group = getRandomGroup();
		final int type = getRandomFishType(group);
		
		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if ((fishs == null) || (fishs.size() == 0))
		{
			sendMessage("Error - Fishes are not defined.");
			endFishing(false);
			return;
		}
		
		final int check = Rnd.get(fishs.size());
		_fish = fishs.get(check);
		fishs.clear();
		fishs = null;
		sendPacket(new SystemMessage(SystemMessage.CAST_LINE_AND_START_FISHING));
		broadcastPacket(new ExFishingStart(this, _fish.getType(), _x, _y, _z));
		startLookingForFishTask();
	}
	
	public void stopLookingForFishTask()
	{
		if (_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}
	
	public void startLookingForFishTask()
	{
		if (!isDead() && (_taskforfish == null))
		{
			int checkDelay = 0;
			
			boolean isNoob = false;
			
			if (_lure != null)
			{
				final int lureid = _lure.getItemId();
				
				isNoob = _fish.getGroup() == 0;
				
				if ((lureid == 6519) || (lureid == 6522) || (lureid == 6525))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.33)));
				}
				else if ((lureid == 6520) || (lureid == 6523) || (lureid == 6526) || ((lureid >= 7610) && (lureid <= 7613)) || ((lureid >= 7807) && (lureid <= 7809)))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.00)));
				}
				else if ((lureid == 6521) || (lureid == 6524) || (lureid == 6527))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (0.66)));
				}
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob), 10000, checkDelay);
		}
	}
	
	private int getRandomGroup()
	{
		switch (_lure.getItemId())
		
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
				return 0;
			default:
				return 1;
		}
	}
	
	private int getRandomFishType(int group)
	{
		final int check = Rnd.get(100);
		int type = 1;
		switch (group)
		
		{
			case 0:
				switch (_lure.getItemId())
				
				{
					case 7807:
						if (check <= 54)
						{
							type = 5;
						}
						else if (check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					case 7808:
						if (check <= 54)
						{
							type = 4;
						}
						else if (check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					case 7809:
						if (check <= 54)
						{
							type = 6;
						}
						else if (check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
				}
				break;
			case 1:
				switch (_lure.getItemId())
				
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519:
					
					case 6520:
					
					case 6521:
						if (check <= 54)
						{
							type = 1;
						}
						else if (check <= 74)
						{
							type = 0;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6522:
					
					case 6523:
					
					case 6524:
						if (check <= 54)
						{
							type = 0;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6525:
					
					case 6526:
					
					case 6527:
						if (check <= 55)
						{
							type = 2;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
				}
		}
		return type;
	}
	
	private int getRandomFishLvl()
	{
		final int skilllvl = getSkillLevel(1315);
		if (skilllvl <= 0)
		{
			return 1;
		}
		
		int randomlvl;
		final int check = Rnd.get(100);
		
		if (check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			
			randomlvl = skilllvl + 1;
			if (randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		
		return randomlvl;
	}
	
	public void startFishCombat(boolean isNoob)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob);
	}
	
	public void endFishing(boolean win)
	{
		
		final ExFishingEnd efe = new ExFishingEnd(win, this);
		broadcastPacket(efe);
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		broadcastUserInfo();
		if (_fishCombat == null)
		{
			sendPacket(new SystemMessage(SystemMessage.BAIT_LOST_FISH_GOT_AWAY));
		}
		_fishCombat = null;
		_lure = null;
		// Ends fishing
		sendPacket(new SystemMessage(SystemMessage.REEL_LINE_AND_STOP_FISHING));
		setIsImmobilized(false);
		stopLookingForFishTask();
	}
	
	public L2Fishing getFishCombat()
	{
		return _fishCombat;
	}
	
	public int getFishx()
	{
		return _fishx;
	}
	
	public int getFishy()
	{
		return _fishy;
	}
	
	public int getFishz()
	{
		return _fishz;
	}
	
	public void setLure(L2ItemInstance lure)
	{
		_lure = lure;
	}
	
	public L2ItemInstance getLure()
	{
		return _lure;
	}
	
	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
		{
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		}
		else if (getRace() == Race.Dwarf)
		{
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		}
		else
		{
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
		
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
		
		return ivlim;
		
	}
	
	public int getWareHouseLimit()
	
	{
		
		int whlim;
		
		if (getRace() == Race.Dwarf)
		{
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		}
		else
		{
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		}
		
		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
		
		return whlim;
	}
	
	public int getPrivateSellStoreLimit()
	
	{
		
		int pslim;
		
		if (getRace() == Race.Dwarf)
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}
		else
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		
		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
		
		return pslim;
		
	}
	
	public int getPrivateBuyStoreLimit()
	
	{
		
		int pblim;
		
		if (getRace() == Race.Dwarf)
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}
		else
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
		
		return pblim;
		
	}
	
	public int getFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}
	
	public int getDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_LIMIT;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		return recdlim;
	}
	
	public int getCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_LIMIT;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
		return recclim;
	}
	
	public int getMountNpcId()
	{
		return _mountNpcId;
	}
	
	public int getMountLevel()
	{
		return _mountLevel;
	}
	
	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}
	
	public int getMountObjectID()
	{
		return _mountObjectID;
	}
	
	private L2ItemInstance _lure = null;
	
	/**
	 * Get the current skill in use or return null.<BR>
	 * <BR>
	 * @return
	 */
	public SkillDat getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentSkill.<BR>
	 * <BR>
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			if (Config.DEBUG)
			{
				_log.info("Setting current skill: NULL for " + getName() + ".");
			}
			
			_currentSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.info("Setting current skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");
		}
		
		_currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * Get the current pet skill in use or return null.<BR>
	 * <BR>
	 * @return
	 */
	public SkillDat getCurrentPetSkill()
	{
		return _currentPetSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentPetSkill.<BR>
	 * <BR>
	 * @param currentPetSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentPetSkill(L2Skill currentPetSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentPetSkill == null)
		{
			if (Config.DEBUG)
			{
				_log.info("Setting current pet skill: NULL for " + getName() + ".");
			}
			
			_currentPetSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.info("Setting current pet skill: " + currentPetSkill.getName() + " (ID: " + currentPetSkill.getId() + ") for " + getName() + ".");
		}
		
		_currentPetSkill = new SkillDat(currentPetSkill, ctrlPressed, shiftPressed);
	}
	
	public SkillDat getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Create a new SkillDat object and queue it in the player _queuedSkill.<BR>
	 * <BR>
	 * @param queuedSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			if (Config.DEBUG)
			{
				_log.info("Setting queued skill: NULL for " + getName() + ".");
			}
			
			_queuedSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.info("Setting queued skill: " + queuedSkill.getName() + " (ID: " + queuedSkill.getId() + ") for " + getName() + ".");
		}
		
		_queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
	}
	
	public boolean isInJail()
	{
		return _inJail;
	}
	
	public void setInJail(boolean state)
	{
		_inJail = state;
	}
	
	public void setInJail(boolean state, long delayInMinutes)
	{
		_inJail = state;
		_jailTimer = 0;
		// Remove the task if any
		stopJailTask(false);
		
		if (_inJail)
		{
			if (delayInMinutes > 0)
			{
				_jailTimer = delayInMinutes * 60000; // in millisec
				
				// start the countdown
				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage("You have been jailed for " + delayInMinutes + " minutes.");
			}
			
			if (Olympiad.getInstance().isRegisteredInComp(this))
			{
				Olympiad.getInstance().removeDisconnectedCompetitor(this);
			}
			
			if (TvTEvent.isEnabled)
			{
				TvTEvent.removePlayer(this);
			}
			
			// Open a Html message to inform the player
			final NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			final String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm");
			if (jailInfos != null)
			{
				htmlMsg.setHtml(jailInfos);
			}
			else
			{
				htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
			}
			sendPacket(htmlMsg);
			
			if (inOfflineMode())
			{
				logout();
			}
			else
			{
				teleToLocation(-114356, -249645, -2984, false); // Jail
			}
		}
		else
		{
			// Open a Html message to inform the player
			final NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			final String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm");
			if (jailInfos != null)
			{
				htmlMsg.setHtml(jailInfos);
			}
			else
			{
				htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
			}
			sendPacket(htmlMsg);
			
			teleToLocation(17836, 170178, -3507, true); // Floran
		}
		
		// store in database
		storeCharBase();
	}
	
	public long getJailTimer()
	{
		return _jailTimer;
	}
	
	public void setJailTimer(long time)
	{
		_jailTimer = time;
	}
	
	private void updateJailState()
	{
		if (isInJail())
		{
			// If jail time is elapsed, free the player
			if (_jailTimer > 0)
			{
				// restart the countdown
				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage("You are still in jail for " + Math.round(_jailTimer / 60000) + " minutes.");
			}
			
			// If player escaped, put him back in jail
			if (!isInsideZone(ZONE_JAIL))
			{
				teleToLocation(-114356, -249645, -2984, false);
			}
		}
	}
	
	public void stopJailTask(boolean save)
	{
		if (_jailTask != null)
		{
			if (save)
			{
				long delay = _jailTask.getDelay(TimeUnit.MILLISECONDS);
				if (delay < 0)
				{
					delay = 0;
				}
				setJailTimer(delay);
			}
			_jailTask.cancel(false);
			_jailTask = null;
		}
	}
	
	private ScheduledFuture<?> _jailTask;
	
	private class JailTask implements Runnable
	{
		L2PcInstance _player;
		@SuppressWarnings("unused")
		protected long _startedAt;
		
		protected JailTask(L2PcInstance player)
		{
			_player = player;
			_startedAt = System.currentTimeMillis();
		}
		
		@Override
		public void run()
		{
			_player.setInJail(false, 0);
		}
	}
	
	private final FastMap<Integer, TimeStamp> _reuseTimeStamps = new FastMap<Integer, TimeStamp>().shared();
	
	public Collection<TimeStamp> getReuseTimeStamps()
	{
		return _reuseTimeStamps.values();
	}
	
	/**
	 * Simple class containing all neccessary information to maintain valid timestamps and reuse for skills upon relog. Filter this carefully as it becomes redundant to store reuse for small delays.
	 * @author Yesod
	 */
	public class TimeStamp
	{
		private final int skillId;
		private final long reuse;
		private final long stamp;
		
		public TimeStamp(int _skillId, long _reuse)
		{
			skillId = _skillId;
			reuse = _reuse;
			stamp = System.currentTimeMillis() + reuse;
		}
		
		public TimeStamp(int _skillId, long _reuse, long _systime)
		{
			skillId = _skillId;
			reuse = _reuse;
			stamp = _systime;
		}
		
		public long getStamp()
		{
			return stamp;
		}
		
		public int getSkillId()
		{
			return skillId;
		}
		
		public long getReuse()
		{
			return reuse;
		}
		
		public long getRemaining()
		{
			
			return Math.max(stamp - System.currentTimeMillis(), 0);
		}
		
		/*
		 * Check if the reuse delay has passed and if it has not then update the stored reuse time according to what is currently remaining on the delay.
		 */
		public boolean hasNotPassed()
		{
			if (System.currentTimeMillis() < stamp)
			{
				return true;
			}
			
			return false;
		}
	}
	
	/**
	 * Index according to skill id the current timestamp of use.
	 */
	@Override
	public void addTimeStamp(int s, int r)
	{
		_reuseTimeStamps.put(s, new TimeStamp(s, r));
	}
	
	/**
	 * Index according to skill this TimeStamp instance for restoration purposes only.
	 * @param t
	 */
	private void addTimeStamp(TimeStamp t)
	{
		_reuseTimeStamps.put(t.getSkillId(), t);
	}
	
	/**
	 * Index according to skill id the current timestamp of use.
	 */
	@Override
	public void removeTimeStamp(int s)
	{
		_reuseTimeStamps.remove(s);
	}
	
	public int getCharges()
	{
		return _charges;
	}
	
	public void addCharge(int number)
	{
		_charges += number;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public void getSkillList()
	{
		final SkillList sl = new SkillList();
		for (final L2Skill s : getAllSkills())
		{
			if (s == null)
			{
				continue;
			}
			if (s.getId() > 9000)
			{
				continue; // Fake skills to change base stats
			}
			sl.addSkill(s.getId(), s.getLevel(), s.isPassive());
		}
		sendPacket(sl);
		
	}
	
	public boolean isSpawnProtected()
	{
		return (_protectEndTime > 0);
	}
	
	public boolean inOfflineMode()
	{
		return _inOfflineMode;
	}
	
	public void setInOfflineMode()
	{
		_inOfflineMode = true;
		
		if (getClan() != null)
		{
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		}
		
		leaveParty();
		
		if (getPet() != null)
		{
			getPet().unSummon(this);
		}
		
		stopWarnUserTakeBreak();
	}
	
	public long getOfflineStartTime()
	{
		return _offlineShopStart;
	}
	
	public void setOfflineStartTime(long time)
	{
		_offlineShopStart = time;
	}
	
	@Override
	public L2PcInstance getActingPlayer()
	{
		return this;
	}
	
	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		// Check if hit is missed
		if (miss)
		{
			sendPacket(new SystemMessage(SystemMessage.MISSED_TARGET));
			return;
		}
		
		// Check if hit is critical
		if (pcrit)
		{
			sendPacket(new SystemMessage(SystemMessage.CRITICAL_HIT));
		}
		if (mcrit)
		{
			sendPacket(new SystemMessage(SystemMessage.CRITICAL_HIT_MAGIC));
		}
		
		if (isInOlympiadMode() && (target instanceof L2PcInstance) && ((L2PcInstance) target).isInOlympiadMode() && (((L2PcInstance) target).getOlympiadGameId() == getOlympiadGameId()))
		{
			dmgDealt += damage;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessage.YOU_DID_S1_DMG);
		sm.addNumber(damage);
		sendPacket(sm);
	}
	
	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if (!isMounted())
				{
					stopFeed();
					return;
				}
				
				if (getCurrentFeed() > getFeedConsume())
				{
					setCurrentFeed(getCurrentFeed() - getFeedConsume());
				}
				else
				{
					// go back to pet control item, or simply said, unsummon it
					setCurrentFeed(0);
					stopFeed();
					dismount();
					sendPacket(new SystemMessage(SystemMessage.OUT_OF_FEED_MOUNT_CANCELED));
				}
				
				final int[] foodIds = L2PetDataTable.getFoodItemId(getMountNpcId());
				if (foodIds[0] == 0)
				{
					return;
				}
				
				L2ItemInstance food = getInventory().getItemByItemId(foodIds[0]);
				
				// use better strider food if exists
				if (L2PetDataTable.isStrider(getMountNpcId()))
				{
					if (getInventory().getItemByItemId(foodIds[1]) != null)
					{
						food = getInventory().getItemByItemId(foodIds[1]);
					}
				}
				
				if ((food != null) && isHungry())
				{
					final IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getItemId());
					if (handler != null)
					{
						handler.useItem(L2PcInstance.this, food);
						final SystemMessage sm = new SystemMessage(SystemMessage.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
						sm.addItemName(food.getItemId());
						sendPacket(sm);
					}
				}
			}
			catch (final Exception e)
			{
				// _log.log(Level.SEVERE, "Mounted Pet [NpcId: " + getMountNpcId() + "] a feed task error has occurred", e);
			}
		}
	}
	
	protected synchronized void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
		{
			return;
		}
		
		if (getPet() != null)
		{
			setCurrentFeed(((L2PetInstance) getPet()).getCurrentFed());
			_controlItemId = getPet().getControlItemId();
			sendPacket(new SetupGauge(3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
			}
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			sendPacket(new SetupGauge(3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
			}
		}
	}
	
	protected synchronized void stopFeed()
	{
		if (_mountFeedTask != null)
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
			if (Config.DEBUG)
			{
				_log.fine("Pet [#" + _mountNpcId + "] feed task stop");
			}
		}
	}
	
	protected final void clearPetData()
	{
		_data = null;
	}
	
	protected final L2PetData getPetData(int npcId)
	{
		if ((_data == null) && (getPet() != null))
		{
			_data = L2PetDataTable.getInstance().getPetData(getPet().getNpcId(), getPet().getLevel());
		}
		else if ((_data == null) && (npcId > 0))
		{
			_data = L2PetDataTable.getInstance().getPetData(npcId, getLevel());
		}
		
		return _data;
	}
	
	public int getCurrentFeed()
	{
		return _curFeed;
	}
	
	protected int getFeedConsume()
	{
		// if pet is attacking
		if (isAttackingNow())
		{
			return getPetData(_mountNpcId).getPetFeedBattle();
		}
		return getPetData(_mountNpcId).getPetFeedNormal();
	}
	
	public void setCurrentFeed(int num)
	{
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		sendPacket(new SetupGauge(3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
	}
	
	protected int getMaxFeed()
	{
		return getPetData(_mountNpcId).getPetMaxFeed();
	}
	
	protected boolean isHungry()
	{
		return _canFeed ? (getCurrentFeed() < (0.55 * getPetData(getMountNpcId()).getPetMaxFeed())) : false;
	}
	
	public class dismount implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				dismount();
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void enteredNoLanding()
	{
		_dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new L2PcInstance.dismount(), 5000);
	}
	
	public void exitedNoLanding()
	{
		if (_dismountTask != null)
		{
			_dismountTask.cancel(true);
			_dismountTask = null;
		}
	}
	
	public void storePetFood(int petId)
	{
		if ((_controlItemId != 0) && (petId != 0))
		{
			String req;
			req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(req))
			{
				statement.setInt(1, getCurrentFeed());
				statement.setInt(2, _controlItemId);
				statement.executeUpdate();
				_controlItemId = 0;
			}
			catch (final Exception e)
			{
				_log.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
			}
		}
	}
	
	/**
	 * Return true if character falling now On the start of fall return false for correct coord sync !
	 * @param z
	 * @return
	 */
	public final boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isInsideZone(ZONE_WATER))
		{
			return false;
		}
		
		if (System.currentTimeMillis() < _fallingTimestamp)
		{
			return true;
		}
		
		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getFallHeight())
		{
			return false;
		}
		
		final int damage = (int) Formulas.getInstance().calcFallDam(this, deltaZ);
		if (damage > 0)
		{
			reduceCurrentHp(Math.min(damage, getCurrentHp() - 1), null, false);
			final SystemMessage sm = new SystemMessage(SystemMessage.FALL_DAMAGE_S1);
			sm.addNumber(damage);
			sendPacket(sm);
		}
		setFalling();
		
		return false;
	}
	
	public void gatesRequest(L2DoorInstance door)
	{
		_gatesRequest.setTarget(door);
	}
	
	public void gatesAnswer(int answer, int type)
	{
		if (_gatesRequest.getDoor() == null)
		{
			return;
		}
		if ((answer == 1) && (getTarget() == _gatesRequest.getDoor()) && (type == 1))
		{
			_gatesRequest.getDoor().openMe();
		}
		else if ((answer == 1) && (getTarget() == _gatesRequest.getDoor()) && (type == 0))
		{
			_gatesRequest.getDoor().closeMe();
		}
		_gatesRequest.setTarget(null);
	}
	
	public final void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
	}
	
	public void setGainXpSp(boolean XpSp)
	{
		_getGainXpSp = XpSp;
	}
	
	public boolean getGainXpSp()
	{
		return _getGainXpSp;
	}
	
	public boolean isPendingSitting()
	{
		return _isPendingSitting;
	}
	
	public void setIsPendingSitting(boolean sit)
	{
		_isPendingSitting = sit;
	}
	
	public int getLootInvitation()
	{
		return _lootInvitation;
	}
	
	public void setLootInvitation(int loot)
	{
		_lootInvitation = loot;
	}
	
	public double getCollisionRadius()
	{
		if (getAppearance().getSex())
		{
			return getBaseTemplate().collisionRadius_female;
		}
		return getBaseTemplate().collisionRadius;
	}
	
	public double getCollisionHeight()
	{
		if (getAppearance().getSex())
		{
			return getBaseTemplate().collisionHeight_female;
		}
		return getBaseTemplate().collisionHeight;
	}
	
	@Override
	public boolean isAIOBuffer()
	{
		return _isAIOBuffer;
	}
	
	public FloodProtectors getFloodProtectors()
	{
		return _client.getFloodProtectors();
	}
	
	public static class Friend
	{
		private final int _objectId;
		private String _name;
		private int _online;
		
		public Friend(int objectId, String name)
		{
			_objectId = objectId;
			_name = name;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public void setName(String name)
		{
			_name = name;
		}
		
		public int isOnline()
		{
			return _online;
		}
		
		public void setOnline(int online)
		{
			_online = online;
		}
	}
	
	public List<Friend> getFriendList()
	{
		return _friendList;
	}
	
	public Friend getFriend(String name)
	{
		for (final Friend friend : getFriendList())
		{
			if (friend == null)
			{
				continue;
			}
			
			if (friend.getName().equalsIgnoreCase(name))
			{
				return friend;
			}
		}
		return null;
	}
	
	/**
	 * Check for Olympiad restrictions
	 * @return
	 */
	public boolean checkOlympiadConditions()
	{
		if (!isNoble())
		{
			
			sendPacket(new SystemMessage(SystemMessage.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD));
			return false;
		}
		
		if ((getInventoryLimit() * 0.8) <= getInventory().getSize())
		{
			
			sendPacket(new SystemMessage(SystemMessage.SINCE_80_PERCENT_OR_MORE_OF_YOUR_INVENTORY_SLOTS_ARE_FULL_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD));
			return false;
		}
		
		if (isFestivalParticipant())
		{
			sendMessage("Festival participants cannot register to the Grand Olympiad games.");
			return false;
		}
		
		if (isDead())
		{
			return false;
		}
		
		if (getBaseClass() != getClassId().getId())
		{
			sendPacket(new SystemMessage(SystemMessage.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER));
			return false;
		}
		
		if ((getEventTeam() > 0) || TvTEvent.isRegistered(this))
		
		{
			sendMessage("TvT Participants cannot register to the Grand Olympiad Games.");
			return false;
		}
		
		return true;
	}
}