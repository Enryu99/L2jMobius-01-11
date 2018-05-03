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
package com.l2jmobius.gameserver.network.clientpackets;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.l2jmobius.Config;
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.commons.crypt.nProtect;
import com.l2jmobius.commons.crypt.nProtect.RestrictionType;
import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.communitybbs.Manager.MailBBSManager;
import com.l2jmobius.gameserver.datatables.GmListTable;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.datatables.csv.MapRegionTable;
import com.l2jmobius.gameserver.datatables.sql.AdminCommandAccessRights;
import com.l2jmobius.gameserver.handler.custom.CustomWorldHandler;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.ClanHallManager;
import com.l2jmobius.gameserver.instancemanager.CoupleManager;
import com.l2jmobius.gameserver.instancemanager.CrownManager;
import com.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import com.l2jmobius.gameserver.instancemanager.FortSiegeManager;
import com.l2jmobius.gameserver.instancemanager.PetitionManager;
import com.l2jmobius.gameserver.instancemanager.SiegeManager;
import com.l2jmobius.gameserver.model.Inventory;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2Effect;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2ClassMasterInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.base.ClassLevel;
import com.l2jmobius.gameserver.model.base.PlayerClass;
import com.l2jmobius.gameserver.model.entity.Announcements;
import com.l2jmobius.gameserver.model.entity.ClanHall;
import com.l2jmobius.gameserver.model.entity.Hero;
import com.l2jmobius.gameserver.model.entity.Wedding;
import com.l2jmobius.gameserver.model.entity.event.CTF;
import com.l2jmobius.gameserver.model.entity.event.DM;
import com.l2jmobius.gameserver.model.entity.event.L2Event;
import com.l2jmobius.gameserver.model.entity.event.TvT;
import com.l2jmobius.gameserver.model.entity.olympiad.Olympiad;
import com.l2jmobius.gameserver.model.entity.sevensigns.SevenSigns;
import com.l2jmobius.gameserver.model.entity.siege.Castle;
import com.l2jmobius.gameserver.model.entity.siege.FortSiege;
import com.l2jmobius.gameserver.model.entity.siege.Siege;
import com.l2jmobius.gameserver.model.quest.Quest;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.Disconnection;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ClientSetTime;
import com.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import com.l2jmobius.gameserver.network.serverpackets.Die;
import com.l2jmobius.gameserver.network.serverpackets.Earthquake;
import com.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.ExMailArrived;
import com.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jmobius.gameserver.network.serverpackets.FriendList;
import com.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import com.l2jmobius.gameserver.network.serverpackets.ItemList;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jmobius.gameserver.network.serverpackets.PlaySound;
import com.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import com.l2jmobius.gameserver.network.serverpackets.PledgeStatusChanged;
import com.l2jmobius.gameserver.network.serverpackets.QuestList;
import com.l2jmobius.gameserver.network.serverpackets.ShortCutInit;
import com.l2jmobius.gameserver.network.serverpackets.SignsSky;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;
import com.l2jmobius.gameserver.thread.TaskPriority;
import com.l2jmobius.gameserver.util.BuilderUtil;
import com.l2jmobius.gameserver.util.Util;

/**
 * Enter World Packet Handler
 */
public class EnterWorld extends L2GameClientPacket
{
	private static Logger LOGGER = Logger.getLogger(EnterWorld.class.getName());
	
	private final SimpleDateFormat fmt = new SimpleDateFormat("H:mm.");
	private long _daysleft;
	SimpleDateFormat df = new SimpleDateFormat("dd MM yyyy");
	
	public TaskPriority getPriority()
	{
		return TaskPriority.PR_URGENT;
	}
	
	@Override
	protected void readImpl()
	{
		// this is just a trigger packet. it has no content
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			LOGGER.warning("EnterWorld failed! activeChar is null...");
			getClient().closeNow();
			return;
		}
		
		// Set lock at login
		activeChar.setLocked(true);
		
		// Register in flood protector
		// FloodProtector.getInstance().registerNewPlayer(activeChar.getObjectId());
		
		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
		{
			if (Config.DEBUG)
			{
				LOGGER.warning("DEBUG " + getType() + ": User already exist in OID map! User " + activeChar.getName() + " is character clone");
				// activeChar.closeNetConnection(); // Do nothing?
			}
		}
		
		if (!activeChar.isGM() && !activeChar.isDonator() && Config.CHECK_NAME_ON_LOGIN)
		{
			if ((activeChar.getName().length() < 3) || (activeChar.getName().length() > 16) || !Util.isAlphaNumeric(activeChar.getName()) || !isValidName(activeChar.getName()))
			{
				LOGGER.warning("Charname: " + activeChar.getName() + " is invalid. EnterWorld failed.");
				getClient().closeNow();
				return;
			}
		}
		
		// Set online status
		activeChar.setOnlineStatus(true);
		
		activeChar.setRunning(); // running is default
		activeChar.standUp(); // standing is default
		
		activeChar.broadcastKarma(); // include UserInfo
		
		// Engage and notify Partner
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
		}
		
		EnterGM(activeChar);
		
		Quest.playerEnter(activeChar);
		activeChar.sendPacket(new QuestList());
		
		if (Config.ENABLE_COMMUNITY_BOARD)
		{
			// Unread mails make a popup appears.
			if (MailBBSManager.getInstance().checkUnreadMail(activeChar) > 0)
			{
				activeChar.sendPacket(SystemMessageId.NEW_MAIL);
				activeChar.sendPacket(new PlaySound("systemmsg_e.1233"));
				activeChar.sendPacket(ExMailArrived.STATIC_PACKET);
			}
			
			// Clan notice, if active.
			if ((activeChar.getClan() != null) && activeChar.getClan().isNoticeEnabled())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setFile("data/html/clan_notice.htm");
				html.replace("%clan_name%", activeChar.getClan().getName());
				html.replace("%notice_text%", activeChar.getClan().getNotice().replaceAll("\r\n", "<br>").replaceAll("action", "").replaceAll("bypass", ""));
				sendPacket(html);
			}
		}
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			activeChar.setProtection(true);
		}
		
		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		// Mobius: Set player heading.
		// activeChar.sendPacket(new StopRotation(activeChar, activeChar.getHeading(), 10000000));
		
		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			sendPacket(new SignsSky());
		}
		
		// Buff and Status icons
		if (Config.STORE_SKILL_COOLTIME)
		{
			activeChar.restoreEffects();
		}
		
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		
		final L2Effect[] effects = activeChar.getAllEffects();
		
		if (effects != null)
		{
			for (L2Effect e : effects)
			{
				if (e.getEffectType() == L2Effect.EffectType.HEAL_OVER_TIME)
				{
					activeChar.stopEffects(L2Effect.EffectType.HEAL_OVER_TIME);
					activeChar.removeEffect(e);
				}
				if (e.getEffectType() == L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME)
				{
					activeChar.stopEffects(L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME);
					activeChar.removeEffect(e);
				}
			}
		}
		
		// Apply augmentation boni for equipped items
		for (L2ItemInstance temp : activeChar.getInventory().getAugmentedItems())
		{
			if ((temp != null) && temp.isEquipped())
			{
				temp.getAugmentation().applyBoni(activeChar);
			}
		}
		
		// Remove Demonic Weapon if character is not Cursed Weapon Equipped
		if ((activeChar.getInventory().getItemByItemId(8190) != null) && (activeChar.isCursedWeaponEquipped() == false))
		{
			activeChar.destroyItem("Zariche", activeChar.getInventory().getItemByItemId(8190), null, true);
		}
		if ((activeChar.getInventory().getItemByItemId(8689) != null) && (activeChar.isCursedWeaponEquipped() == false))
		{
			activeChar.destroyItem("Akamanah", activeChar.getInventory().getItemByItemId(8689), null, true);
		}
		
		// Apply death penalty
		activeChar.restoreDeathPenaltyBuffLevel();
		
		if (L2Event.active && L2Event.connectionLossData.containsKey(activeChar.getName()) && L2Event.isOnEvent(activeChar))
		{
			L2Event.restoreChar(activeChar);
		}
		else if (L2Event.connectionLossData.containsKey(activeChar.getName()))
		{
			L2Event.restoreAndTeleChar(activeChar);
		}
		
		// SECURE FIX - Anti Overenchant Cheat!!
		if (Config.MAX_ITEM_ENCHANT_KICK > 0)
		{
			for (L2ItemInstance i : activeChar.getInventory().getItems())
			{
				if (!activeChar.isGM())
				{
					if (i.isEquipable())
					{
						if (i.getEnchantLevel() > Config.MAX_ITEM_ENCHANT_KICK)
						{
							// Delete Item Over enchanted
							activeChar.getInventory().destroyItem(null, i, activeChar, null);
							// Message to Player
							activeChar.sendMessage("[Server]: You have over enchanted items you will be kicked from server!");
							activeChar.sendMessage("[Server]: Respect our server rules.");
							// Message with screen
							sendPacket(new ExShowScreenMessage(" You have an over enchanted item, you will be kicked from server! ", 6000));
							// Punishment e LOGGER in audit
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has Overenchanted  item! Kicked! ", Config.DEFAULT_PUNISH);
							// Logger in console
							LOGGER.info("#### ATTENTION ####");
							LOGGER.info(i + " item has been removed from " + activeChar);
						}
					}
				}
			}
		}
		
		// Restores custom status
		activeChar.restoreCustomStatus();
		
		ColorSystem(activeChar);
		
		// Expand Skill
		final ExStorageMaxCount esmc = new ExStorageMaxCount(activeChar);
		activeChar.sendPacket(esmc);
		
		activeChar.getMacroses().sendUpdate();
		
		// Send packets info
		sendPacket(new ClientSetTime()); // SetClientTime
		sendPacket(new UserInfo(activeChar));
		sendPacket(new HennaInfo(activeChar));
		sendPacket(new FriendList(activeChar));
		sendPacket(new ItemList(activeChar, false));
		sendPacket(new ShortCutInit(activeChar));
		
		// Reload inventory to give SA skill
		activeChar.getInventory().reloadEquippedItems();
		
		// Welcome to Lineage II
		activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		Announcements.getInstance().showAnnouncements(activeChar);
		
		loadTutorial(activeChar);
		
		// Check for crowns
		CrownManager.getInstance().checkCrowns(activeChar);
		
		// Check player skills
		if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
		{
			activeChar.checkAllowedSkills();
		}
		
		PetitionManager.getInstance().checkPetitionMessages(activeChar);
		
		// Send user info again .. just like the real client
		// sendPacket(ui);
		
		if ((activeChar.getClanId() != 0) && (activeChar.getClan() != null))
		{
			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			sendPacket(new PledgeStatusChanged(activeChar.getClan()));
		}
		
		if (activeChar.isAlikeDead())
		{
			sendPacket(new Die(activeChar)); // No broadcast needed since the player will already spawn dead to others
		}
		
		if (Config.ALLOW_WATER)
		{
			activeChar.checkWaterState();
		}
		
		if ((Hero.getInstance().getHeroes() != null) && Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
		{
			activeChar.setHero(true);
		}
		
		setPledgeClass(activeChar);
		
		notifyClanMembers(activeChar);
		notifySponsorOrApprentice(activeChar);
		
		activeChar.setTarget(activeChar);
		
		activeChar.onPlayerEnter();
		
		if (Config.PCB_ENABLE)
		{
			activeChar.showPcBangWindow();
		}
		
		if (Config.ANNOUNCE_CASTLE_LORDS)
		{
			notifyCastleOwner(activeChar);
		}
		
		if (Olympiad.getInstance().playerInStadia(activeChar))
		{
			activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			activeChar.sendMessage("You have been teleported to the nearest town due to you being in an Olympiad Stadium");
		}
		
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
		{
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
		}
		
		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		}
		
		if (activeChar.getClan() != null)
		{
			activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));
			
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 1);
					break;
				}
				else if (siege.checkIsDefender(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 2);
					break;
				}
			}
			
			for (FortSiege fortsiege : FortSiegeManager.getInstance().getSieges())
			{
				if (!fortsiege.getIsInProgress())
				{
					continue;
				}
				
				if (fortsiege.checkIsAttacker(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 1);
					break;
				}
				else if (fortsiege.checkIsDefender(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 2);
					break;
				}
			}
			
			// Add message at connexion if clanHall not paid. Possibly this is custom...
			final ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
			
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
				{
					activeChar.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
				}
			}
		}
		
		if (!activeChar.isGM() && (activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
		{
			// Attacker or spectator logging in to a siege zone. Actually should be checked for inside castle only?
			activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			activeChar.sendMessage("You have been teleported to the nearest town due to you being in siege zone");
		}
		
		CustomWorldHandler.getInstance().enterWorld(activeChar);
		
		if (TvT._savePlayers.contains(activeChar.getName()))
		{
			TvT.addDisconnectedPlayer(activeChar);
		}
		
		if (CTF._savePlayers.contains(activeChar.getName()))
		{
			CTF.addDisconnectedPlayer(activeChar);
		}
		
		if (DM._savePlayers.contains(activeChar.getName()))
		{
			DM.addDisconnectedPlayer(activeChar);
		}
		
		// Means that it's not ok multiBox situation, so logout
		if (!activeChar.checkMultiBox())
		{
			activeChar.sendMessage("I'm sorry, but multibox is not allowed here.");
			activeChar.logout();
		}
		
		Hellows(activeChar);
		
		if (Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS)
		{
			final L2ClassMasterInstance master_instance = L2ClassMasterInstance.getInstance();
			
			if (master_instance != null)
			{
				final ClassLevel lvlnow = PlayerClass.values()[activeChar.getClassId().getId()].getLevel();
				
				if ((activeChar.getLevel() >= 20) && (lvlnow == ClassLevel.First))
				{
					L2ClassMasterInstance.getInstance().onAction(activeChar);
				}
				else if ((activeChar.getLevel() >= 40) && (lvlnow == ClassLevel.Second))
				{
					L2ClassMasterInstance.getInstance().onAction(activeChar);
				}
				else if ((activeChar.getLevel() >= 76) && (lvlnow == ClassLevel.Third))
				{
					L2ClassMasterInstance.getInstance().onAction(activeChar);
				}
			}
			else
			{
				LOGGER.info("Attention: Remote ClassMaster is Enabled, but not inserted into DataBase. Remember to install 31288 Custom_Npc...");
			}
		}
		
		// Apply night/day bonus on skill Shadow Sense
		if (activeChar.getRace().ordinal() == 2)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
			if ((skill != null) && (activeChar.getSkillLevel(294) == 1))
			{
				if (GameTimeController.getInstance().isNowNight())
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.NIGHT_EFFECT_APPLIES);
					sm.addSkillName(294);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DAY_EFFECT_DISAPPEARS);
					sm.addSkillName(294);
					sendPacket(sm);
				}
			}
		}
		
		if (!nProtect.getInstance().checkRestriction(activeChar, RestrictionType.RESTRICT_ENTER))
		{
			activeChar.setIsImobilised(true);
			activeChar.disableAllSkills();
			ThreadPool.schedule(new Disconnection(activeChar), 20000);
		}
		
		// Elrokian Trap like L2OFF
		final L2ItemInstance rhand = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if ((rhand != null) && (rhand.getItemId() == 8763))
		{
			activeChar.addSkill(SkillTable.getInstance().getInfo(3626, 1));
			activeChar.addSkill(SkillTable.getInstance().getInfo(3627, 1));
			activeChar.addSkill(SkillTable.getInstance().getInfo(3628, 1));
		}
		else
		{
			activeChar.removeSkill(3626, true);
			activeChar.removeSkill(3627, true);
			activeChar.removeSkill(3628, true);
		}
		
		// If it's a Beta server all players got GM SPEED skill for better testing
		if (Config.BETASERVER)
		{
			activeChar.addSkill(SkillTable.getInstance().getInfo(7029, 4), true);
			activeChar.sendMessage("Server is on Beta mode. Skill Gm Haste 4 added for better testing.");
		}
		
		// Send all skills to char
		activeChar.sendSkillList();
		
		// Close lock at login
		activeChar.setLocked(false);
	}
	
	private boolean isValidName(String text)
	{
		boolean result = true;
		final String test = text;
		Pattern pattern;
		
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch (PatternSyntaxException e) // case of illegal pattern
		{
			LOGGER.warning("ERROR " + getType() + ": Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		
		final Matcher regexp = pattern.matcher(test);
		if (!regexp.matches())
		{
			result = false;
		}
		
		return result;
	}
	
	private void EnterGM(L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			gmStartupProcess:
			{
				if (Config.GM_STARTUP_BUILDER_HIDE && AdminCommandAccessRights.getInstance().hasAccess("admin_hide", activeChar.getAccessLevel()))
				{
					activeChar.setInRefusalMode(true);
					activeChar.setIsInvul(true);
					activeChar.getAppearance().setInvisible();
					
					BuilderUtil.sendSysMessage(activeChar, "hide is default for builder.");
					BuilderUtil.sendSysMessage(activeChar, "FriendAddOff is default for builder.");
					BuilderUtil.sendSysMessage(activeChar, "whisperoff is default for builder.");
					
					// It isn't recommend to use the below custom L2J GMStartup functions together with retail-like GMStartupBuilderHide, so breaking the process at that stage.
					break gmStartupProcess;
				}
				
				if (Config.GM_STARTUP_INVULNERABLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invul", activeChar.getAccessLevel()))
				{
					activeChar.setIsInvul(true);
				}
				
				if (Config.GM_STARTUP_INVISIBLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invisible", activeChar.getAccessLevel()))
				{
					activeChar.getAppearance().setInvisible();
				}
				
				if (Config.GM_STARTUP_SILENCE && AdminCommandAccessRights.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel()))
				{
					activeChar.setInRefusalMode(true);
				}
				
				if (Config.GM_STARTUP_AUTO_LIST && AdminCommandAccessRights.getInstance().hasAccess("admin_gmliston", activeChar.getAccessLevel()))
				{
					GmListTable.getInstance().addGm(activeChar, false);
				}
				else
				{
					GmListTable.getInstance().addGm(activeChar, true);
				}
			}
			
			if (Config.GM_SPECIAL_EFFECT)
			{
				activeChar.broadcastPacket(new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 50, 4));
			}
			
			if (Config.SHOW_GM_LOGIN)
			{
				Announcements.getInstance().announceToAll("GM " + activeChar.getName() + " has logged on.");
			}
			
			if (Config.MASTERACCESS_NAME_COLOR_ENABLED)
			{
				activeChar.getAppearance().setNameColor(Config.MASTERACCESS_NAME_COLOR);
			}
			
			if (Config.MASTERACCESS_TITLE_COLOR_ENABLED)
			{
				activeChar.getAppearance().setTitleColor(Config.MASTERACCESS_TITLE_COLOR);
			}
		}
	}
	
	private void Hellows(L2PcInstance activeChar)
	{
		if (Config.ALT_SERVER_NAME_ENABLED)
		{
			activeChar.sendMessage("Welcome to " + Config.ALT_Server_Name);
		}
		
		if (Config.ONLINE_PLAYERS_ON_LOGIN)
		{
			activeChar.sendMessage("There are " + L2World.getInstance().getAllPlayers().size() + " players online.");
		}
		
		if (activeChar.getFirstLog() && Config.NEW_PLAYER_EFFECT)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(2025, 1);
			if (skill != null)
			{
				final MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2025, 1, 1, 0);
				activeChar.sendPacket(MSU);
				activeChar.broadcastPacket(MSU);
				activeChar.useMagic(skill, false, false);
			}
			activeChar.setFirstLog(false);
			activeChar.updateFirstLog();
		}
		
		if (Config.WELCOME_HTM && isValidName(activeChar.getName()))
		{
			final String Welcome_Path = "data/html/welcome.htm";
			final File mainText = new File(Config.DATAPACK_ROOT, Welcome_Path);
			if (mainText.exists())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(Welcome_Path);
				html.replace("%name%", activeChar.getName());
				sendPacket(html);
			}
		}
		
		if (Config.PM_MESSAGE_ON_START)
		{
			activeChar.sendPacket(new CreatureSay(2, Say2.HERO_VOICE, Config.PM_TEXT1, Config.PM_SERVER_NAME));
			activeChar.sendPacket(new CreatureSay(15, Say2.PARTYROOM_COMMANDER, activeChar.getName(), Config.PM_TEXT2));
		}
		
		if (Config.SERVER_TIME_ON_START)
		{
			activeChar.sendMessage("SVR time is " + fmt.format(new Date(System.currentTimeMillis())));
		}
	}
	
	private void ColorSystem(L2PcInstance activeChar)
	{
		// Color System checks - Start
		// Check if the custom PvP and PK color systems are enabled and if so check the character's counters
		// and apply any color changes that must be done. Thankz Kidzor
		/** KidZor: Ammount 1 **/
		if ((activeChar.getPvpKills() >= Config.PVP_AMOUNT1) && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if ((activeChar.getPkKills() >= Config.PK_AMOUNT1) && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		/** KidZor: Ammount 2 **/
		if ((activeChar.getPvpKills() >= Config.PVP_AMOUNT2) && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if ((activeChar.getPkKills() >= Config.PK_AMOUNT2) && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		/** KidZor: Ammount 3 **/
		if ((activeChar.getPvpKills() >= Config.PVP_AMOUNT3) && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if ((activeChar.getPkKills() >= Config.PK_AMOUNT3) && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		/** KidZor: Ammount 4 **/
		if ((activeChar.getPvpKills() >= Config.PVP_AMOUNT4) && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if ((activeChar.getPkKills() >= Config.PK_AMOUNT4) && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
		}
		
		/** KidZor: Ammount 5 **/
		if ((activeChar.getPvpKills() >= Config.PVP_AMOUNT5) && Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor(activeChar.getPvpKills());
		}
		if ((activeChar.getPkKills() >= Config.PK_AMOUNT5) && Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor(activeChar.getPkKills());
			// Color System checks - End
		}
		
		// Apply color settings to clan leader when entering
		if ((activeChar.getClan() != null) && activeChar.isClanLeader() && Config.CLAN_LEADER_COLOR_ENABLED && (activeChar.getClan().getLevel() >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL))
		{
			if (Config.CLAN_LEADER_COLORED == 1)
			{
				activeChar.getAppearance().setNameColor(Config.CLAN_LEADER_COLOR);
			}
			else
			{
				activeChar.getAppearance().setTitleColor(Config.CLAN_LEADER_COLOR);
			}
		}
		
		if (Config.ALLOW_AIO_NCOLOR && activeChar.isAio())
		{
			activeChar.getAppearance().setNameColor(Config.AIO_NCOLOR);
		}
		
		if (Config.ALLOW_AIO_TCOLOR && activeChar.isAio())
		{
			activeChar.getAppearance().setTitleColor(Config.AIO_TCOLOR);
		}
		
		if (activeChar.isAio())
		{
			onEnterAio(activeChar);
		}
		
		activeChar.updateNameTitleColor();
		
		sendPacket(new UserInfo(activeChar));
		sendPacket(new HennaInfo(activeChar));
		sendPacket(new FriendList(activeChar));
		sendPacket(new ItemList(activeChar, false));
		sendPacket(new ShortCutInit(activeChar));
		activeChar.broadcastUserInfo();
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
	}
	
	private void onEnterAio(L2PcInstance activeChar)
	{
		final long now = Calendar.getInstance().getTimeInMillis();
		final long endDay = activeChar.getAioEndTime();
		
		if (now > endDay)
		{
			activeChar.setAio(false);
			activeChar.setAioEndTime(0);
			activeChar.lostAioSkills();
			activeChar.sendMessage("[Aio System]: Removed your Aio stats... period ends.");
		}
		else
		{
			final Date dt = new Date(endDay);
			_daysleft = (endDay - now) / 86400000;
			if (_daysleft > 30)
			{
				activeChar.sendMessage("[Aio System]: Aio period ends in " + df.format(dt) + ". enjoy the Game.");
			}
			else if (_daysleft > 0)
			{
				activeChar.sendMessage("[Aio System]: Left " + (int) _daysleft + " for Aio period ends.");
			}
			else if (_daysleft < 1)
			{
				final long hour = (endDay - now) / 3600000;
				activeChar.sendMessage("[Aio System]: Left " + (int) hour + " hours to Aio period ends.");
			}
		}
	}
	
	private void engage(L2PcInstance cha)
	{
		final int _chaid = cha.getObjectId();
		
		for (Wedding cl : CoupleManager.getInstance().getCouples())
		{
			if ((cl.getPlayer1Id() == _chaid) || (cl.getPlayer2Id() == _chaid))
			{
				if (cl.getMaried())
				{
					cha.setMarried(true);
					cha.setmarriedType(cl.getType());
				}
				
				cha.setCoupleId(cl.getId());
				
				if (cl.getPlayer1Id() == _chaid)
				{
					cha.setPartnerId(cl.getPlayer2Id());
				}
				else
				{
					cha.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}
	
	private void notifyPartner(L2PcInstance cha, int partnerId)
	{
		if (cha.getPartnerId() != 0)
		{
			L2PcInstance partner = null;
			
			if (L2World.getInstance().findObject(cha.getPartnerId()) instanceof L2PcInstance)
			{
				partner = (L2PcInstance) L2World.getInstance().findObject(cha.getPartnerId());
			}
			
			if (partner != null)
			{
				partner.sendMessage("Your partner has logged in");
			}
		}
	}
	
	private void notifyClanMembers(L2PcInstance activeChar)
	{
		final L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			clan.broadcastToOtherOnlineMembers(new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addString(activeChar.getName()), activeChar);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
		}
	}
	
	private void notifySponsorOrApprentice(L2PcInstance activeChar)
	{
		if (activeChar.getSponsor() != 0)
		{
			final L2PcInstance sponsor = (L2PcInstance) L2World.getInstance().findObject(activeChar.getSponsor());
			if (sponsor != null)
			{
				sponsor.sendPacket(new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addString(activeChar.getName()));
			}
		}
		else if (activeChar.getApprentice() != 0)
		{
			final L2PcInstance apprentice = (L2PcInstance) L2World.getInstance().findObject(activeChar.getApprentice());
			if (apprentice != null)
			{
				apprentice.sendPacket(new SystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addString(activeChar.getName()));
			}
		}
	}
	
	private void loadTutorial(L2PcInstance player)
	{
		final QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
		{
			qs.getQuest().notifyEvent("UC", null, player);
		}
	}
	
	private void setPledgeClass(L2PcInstance activeChar)
	{
		int pledgeClass = 0;
		
		if (activeChar.getClan() != null)
		{
			pledgeClass = activeChar.getClan().getClanMember(activeChar.getObjectId()).calculatePledgeClass(activeChar);
		}
		
		if (activeChar.isNoble() && (pledgeClass < 5))
		{
			pledgeClass = 5;
		}
		
		if (activeChar.isHero())
		{
			pledgeClass = 8;
		}
		
		activeChar.setPledgeClass(pledgeClass);
	}
	
	private void notifyCastleOwner(L2PcInstance activeChar)
	{
		final L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			if (clan.getHasCastle() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleById(clan.getHasCastle());
				if ((castle != null) && (activeChar.getObjectId() == clan.getLeaderId()))
				{
					Announcements.getInstance().announceToAll("Lord " + activeChar.getName() + " Ruler Of " + castle.getName() + " Castle is now Online!");
				}
			}
		}
	}
}