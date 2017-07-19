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

import java.util.Iterator;
import java.util.Set;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.Olympiad;
import com.l2jmobius.gameserver.datatables.CharTemplateTable;
import com.l2jmobius.gameserver.datatables.ClanTable;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.instancemanager.SiegeManager;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.base.ClassId;
import com.l2jmobius.gameserver.model.base.ClassType;
import com.l2jmobius.gameserver.model.base.PlayerClass;
import com.l2jmobius.gameserver.model.base.Race;
import com.l2jmobius.gameserver.model.base.SubClass;
import com.l2jmobius.gameserver.model.entity.Castle;
import com.l2jmobius.gameserver.model.quest.QuestState;
import com.l2jmobius.gameserver.network.serverpackets.ItemList;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;

import javolution.text.TextBuilder;

/**
 * This class ...
 * @version $Revision: 1.4.2.3.2.8 $ $Date: 2005/03/29 23:15:15 $
 */
public final class L2VillageMasterInstance extends L2FolkInstance
{
	// private static Logger _log = Logger.getLogger(L2VillageMasterInstance.class.getName());
	
	/**
	 * @param objectId
	 * @param template
	 */
	public L2VillageMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		final String[] commandStr = command.split(" ");
		final String actualCommand = commandStr[0]; // Get actual command
		
		String cmdParams = "";
		
		if (commandStr.length >= 2)
		{
			cmdParams = commandStr[1];
		}
		
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			ClanTable.getInstance().createClan(player, cmdParams);
			
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			if (player.getClan() != null)
			{
				player.getClan().createAlly(player, cmdParams);
			}
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			if (player.getClan() != null)
			{
				player.getClan().dissolveAlly(player);
			}
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			levelUpClan(player, player.getClanId());
		}
		else if (command.startsWith("Subclass"))
		{
			final int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
			
			// Subclasses may not be changed while a skill is in use.
			if (player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(new SystemMessage(1295));
				return;
			}
			
			if (Olympiad.getInstance().isRegisteredInComp(player))
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
				return;
			}
			
			final TextBuilder content = new TextBuilder("<html><body>");
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			Set<PlayerClass> subsAvailable;
			
			int paramOne = 0;
			int paramTwo = 0;
			
			try
			{
				int endIndex = command.length();
				
				if (command.length() > 13)
				{
					endIndex = 13;
					paramTwo = Integer.parseInt(command.substring(13).trim());
				}
				
				paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
			}
			catch (final Exception NumberFormatException)
			{
			}
			
			switch (cmdChoice)
			{
				case 1: // Add Subclass - Initial
					// Avoid giving player an option to add a new sub class, if they have three already.
					if (player.getTotalSubClasses() == Config.ALT_MAX_SUBCLASS)
					{
						player.sendMessage("You may only add up to " + Config.ALT_MAX_SUBCLASS + " subclasses at a time.");
						return;
					}
					
					subsAvailable = getAvailableSubClasses(player);
					
					if ((subsAvailable != null) && !subsAvailable.isEmpty())
					{
						content.append("Add Subclass:<br>Which sub class do you wish to add?<br>");
						
						for (final PlayerClass subClass : subsAvailable)
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 4 " + subClass.ordinal() + "\" msg=\"1268;" + formatClassForDisplay(subClass) + "\">" + formatClassForDisplay(subClass) + "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no available subclasses at this time.");
						return;
					}
					break;
				case 2: // Change Class - Initial
					content.append("Change Subclass:<br>");
					
					final int baseClassId = player.getBaseClass();
					
					if (player.getSubClasses().isEmpty())
					{
						content.append("You can't change subclasses when you don't have a subclass to begin with.<br>" + "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 1\">Add subclass.</a>");
					}
					else
					{
						content.append("Which class would you like to switch to?<br>");
						
						if (baseClassId == player.getActiveClass())
						{
							content.append(CharTemplateTable.getClassNameById(baseClassId) + "&nbsp;<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}
						else
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 0\">" + CharTemplateTable.getClassNameById(baseClassId) + "</a>&nbsp;" + "<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}
						
						for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							final SubClass subClass = subList.next();
							final int subClassId = subClass.getClassId();
							
							if (subClassId == player.getActiveClass())
							{
								content.append(CharTemplateTable.getClassNameById(subClassId) + "<br>");
							}
							else
							{
								content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClassId) + "</a><br>");
							}
						}
					}
					break;
				case 3: // Change/Cancel Subclass - Initial
					content.append("Change Subclass:<br>Which of the following sub classes would you like to change?<br>");
					int classIndex = 1;
					
					for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
					{
						final SubClass subClass = subList.next();
						
						content.append("Sub-class " + classIndex + "<br1>");
						content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 6 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClass.getClassId()) + "</a><br>");
						
						classIndex++;
					}
					
					content.append("<br>If you change a sub class, you'll start at level 40 after the 2nd class transfer.");
					break;
				case 4: // Add Subclass - Action (Subclass 4 x[x])
					boolean allowAddition = true;
					
					/*
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice.
					 */
					
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass"))
					{
						_log.warning("Player " + player.getName() + " has performed a subclass change too fast.");
						return;
					}
					
					if (player.getLevel() < 75)
					{
						player.sendMessage("You may not add a new subclass until all your subclasses reach level 75.");
						allowAddition = false;
					}
					
					if (allowAddition)
					{
						if (!player.getSubClasses().isEmpty())
						{
							for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								final SubClass subClass = subList.next();
								
								if (subClass.getLevel() < 75)
								{
									player.sendMessage("You may not add a new subclass until all your subclasses reach level 75.");
									allowAddition = false;
									break;
								}
							}
						}
					}
					
					if (!Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
					{
						final QuestState qs = player.getQuestState("235_MimirsElixir");
						if ((qs == null) || !qs.isCompleted())
						{
							allowAddition = false;
						}
						
					}
					
					////////////////// \\\\\\\\\\\\\\\\\\
					if (allowAddition)
					{
						final String className = CharTemplateTable.getClassNameById(paramOne);
						
						if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
						{
							player.sendMessage("The subclass could not be added.");
							return;
						}
						
						player.setActiveClass(player.getTotalSubClasses());
						
						content.append("Add Subclass:<br>The sub class of <font color=\"LEVEL\">" + className + "</font> has been added.");
						player.sendPacket(new SystemMessage(1308)); // Transfer to new class.
					}
					else
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					}
					
					break;
				case 5: // Change Class - Action
					/*
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice. Note: paramOne = classIndex
					 */
					
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("change class"))
					{
						_log.warning("Player " + player.getName() + " has performed a subclass change too fast");
						return;
					}
					
					player.setActiveClass(paramOne);
					
					content.append("Change Subclass:<br>Your active sub class is now a <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getActiveClass()) + "</font>.");
					
					player.sendPacket(new SystemMessage(1270)); // Transfer completed.
					break;
				case 6: // Change/Cancel Subclass - Choice
					content.append("Please choose a sub class to change to. If the one you are looking for is not here, " + "please seek out the appropriate master for that class.<br>" + "<font color=\"LEVEL\">Warning!</font> All classes and skills for this class will be removed.<br><br>");
					
					subsAvailable = getAvailableSubClasses(player);
					
					if ((subsAvailable != null) && !subsAvailable.isEmpty())
					{
						for (final PlayerClass subClass : subsAvailable)
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 7 " + paramOne + " " + subClass.ordinal() + "\">" + formatClassForDisplay(subClass) + "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no available subclasses at this time.");
						return;
					}
					break;
				case 7: // Change Subclass - Action
					/*
					 * Warning: the information about this subclass will be removed from the subclass list even if false!
					 */
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("change class"))
					{
						_log.warning("Player " + player.getName() + " has performed a subclass change too fast");
						return;
					}
					
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.setActiveClass(paramOne);
						
						content.append("Change Subclass:<br>Your sub class has been changed to <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(paramTwo) + "</font>.");
						
						player.sendPacket(new SystemMessage(1269)); // Subclass added.
					}
					else
					{
						/*
						 * This isn't good! modifySubClass() removed subclass from memory we must update _classIndex! Else IndexOutOfBoundsException can turn up some place down the line along with other seemingly unrelated problems.
						 */
						player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
						
						player.sendMessage("The subclass could not be added, you have been reverted to your base class.");
						return;
					}
					break;
			}
			
			content.append("</body></html>");
			
			// If the content is greater than for a basic blank page,
			// then assume no external HTML file was assigned.
			if (content.length() > 26)
			{
				html.setHtml(content.toString());
			}
			
			player.sendPacket(html);
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/villagemaster/" + pom + ".htm";
	}
	
	public void dissolveClan(L2PcInstance player, int clanId)
	{
		if (Config.DEBUG)
		{
			_log.fine(player.getObjectId() + "(" + player.getName() + ") requested dissolve a clan from " + getObjectId() + "(" + getName() + ")");
		}
		
		if (!player.isClanLeader())
		{
			return;
		}
		
		final L2Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISPERSE_THE_CLANS_IN_ALLY));
			return;
		}
		
		if (clan.isAtWar() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISSOLVE_WHILE_IN_WAR));
			return;
		}
		
		if ((clan.getHasCastle() != 0) || (clan.getHasHideout() != 0))
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE));
			return;
		}
		
		for (final Castle castle : CastleManager.getInstance().getCastles())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getCastleId()))
			{
				player.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISSOLVE_CAUSE_CLAN_WILL_PARTICIPATE_IN_CASTLE_SIEGE));
				return;
			}
		}
		
		if (player.isInsideZone(L2Character.ZONE_SIEGE))
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISSOLVE_WHILE_IN_SIEGE));
			return;
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessage.DISSOLUTION_IN_PROGRESS));
			return;
		}
		
		if (clan.getRecoverPenaltyExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISSOLVE_WITHIN_XX_DAYS));
			return;
		}
		
		clan.setRecoverPenaltyExpiryTime(0);
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + (Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L)); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		
		ClanTable.getInstance().scheduleRemoveClan(clan.getClanId());
		
		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false);
		
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		
		player.sendMessage("The clan has been dissolved.");
	}
	
	public void recoverClan(L2PcInstance player, int clanId)
	{
		if (Config.DEBUG)
		{
			_log.fine(player.getObjectId() + "(" + player.getName() + ") requested recover a clan from " + getObjectId() + "(" + getName() + ")");
		}
		
		if (!player.isClanLeader())
		{
			return;
		}
		
		final L2Clan clan = player.getClan();
		
		if (clan.getDissolvingExpiryTime() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.NO_DISPERSING_REQUESTS));
			return;
		}
		
		clan.setDissolvingExpiryTime(0);
		clan.setRecoverPenaltyExpiryTime(System.currentTimeMillis() + (Config.ALT_RECOVERY_PENALTY * 86400000L)); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		player.sendMessage("Clan dissolution has been cancelled as requested.");
		
	}
	
	public void levelUpClan(L2PcInstance player, int clanId)
	{
		final L2Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		if (!player.isClanLeader())
		{
			
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED));
			return;
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS));
			return;
		}
		
		boolean increaseClanLevel = false;
		
		switch (clan.getLevel())
		{
			case 0:
			{
				// upgrade to 1
				if ((player.getSp() >= 30000) && (player.getAdena() >= 650000))
				{
					if (player.reduceAdena("ClanLvl", 650000, this, true))
					{
						player.setSp(player.getSp() - 30000);
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 1:
			{
				// upgrade to 2
				if ((player.getSp() >= 150000) && (player.getAdena() >= 2500000))
				{
					if (player.reduceAdena("ClanLvl", 2500000, this, true))
					{
						player.setSp(player.getSp() - 150000);
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 2:
			{
				// upgrade to 3
				if ((player.getSp() >= 500000) && (player.getInventory().getItemByItemId(1419) != null))
				{
					// itemid 1419 == proof of blood
					if (player.destroyItemByItemId("ClanLvl", 1419, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 500000);
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 3:
			{
				// upgrade to 4
				if ((player.getSp() >= 1400000) && (player.getInventory().getItemByItemId(3874) != null))
				{
					// itemid 3874 == proof of alliance
					if (player.destroyItemByItemId("ClanLvl", 3874, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 1400000);
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 4:
			{
				// upgrade to 5
				if ((player.getSp() >= 3500000) && (player.getInventory().getItemByItemId(3870) != null))
				{
					// itemid 3870 == proof of aspiration
					if (player.destroyItemByItemId("ClanLvl", 3870, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 3500000);
						increaseClanLevel = true;
					}
					
				}
				break;
			}
		}
		
		if (increaseClanLevel)
		{
			// the player should know that he has less sp now :p
			final StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.SP, player.getSp());
			sendPacket(su);
			
			final ItemList il = new ItemList(player, false);
			player.sendPacket(il);
			
			clan.changeLevel(clan.getLevel() + 1);
			
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessage.FAILED_TO_INCREASE_CLAN_LEVEL));
		}
	}
	
	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		int charClassId = player.getBaseClass();
		
		if (charClassId >= 88)
		{
			charClassId = ClassId.values()[charClassId].getParent().ordinal();
		}
		
		final Race npcRace = getVillageMasterRace();
		final ClassType npcTeachType = getVillageMasterTeachType();
		
		final PlayerClass currClass = PlayerClass.values()[charClassId];
		
		/**
		 * If the race of your main class is Elf or Dark Elf, you may not select each class as a subclass to the other class, and you may not select Overlord and Warsmith class as a subclass. You may not select a similar class as the subclass. The occupations classified as similar classes are as
		 * follows: Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger and Phantom Ranger Paladin, Dark Avenger, Temple Knight and Shillien Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and Shillien Elder Swordsinger and Bladedancer Sorcerer, Spellsinger and
		 * Spellhowler
		 */
		final Set<PlayerClass> availSubs = currClass.getAvailableSubclasses(player);
		
		if (availSubs != null)
		{
			for (final PlayerClass availSub : availSubs)
			{
				for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					final SubClass prevSubClass = subList.next();
					
					int subClassId = prevSubClass.getClassId();
					if (subClassId >= 88)
					{
						subClassId = ClassId.values()[subClassId].getParent().getId();
					}
					
					if ((availSub.ordinal() == subClassId) || (availSub.ordinal() == player.getBaseClass()))
					{
						availSubs.remove(PlayerClass.values()[availSub.ordinal()]);
					}
				}
				
				if (!Config.CHANGE_SUBCLASS_EVERYWHERE)
				{
					if (((npcRace == Race.Human) || (npcRace == Race.Elf)))
					{
						// If the master is human or light elf, ensure that fighter-type
						// masters only teach fighter classes, and priest-type masters
						// only teach priest classes etc.
						if (!availSub.isOfType(npcTeachType))
						{
							availSubs.remove(availSub);
						}
						else if (!availSub.isOfRace(Race.Human) && !availSub.isOfRace(Race.Elf))
						{
							// Remove any non-human or light elf classes.
							availSubs.remove(availSub);
						}
					}
					else
					{
						// If the master is not human and not light elf,
						// then remove any classes not of the same race as the master.
						if (((npcRace != Race.Human) && (npcRace != Race.Elf)) && !availSub.isOfRace(npcRace))
						{
							availSubs.remove(availSub);
						}
					}
				}
			}
		}
		
		return availSubs;
	}
	
	private final String formatClassForDisplay(PlayerClass className)
	{
		String classNameStr = className.toString();
		final char[] charArray = classNameStr.toCharArray();
		
		for (int i = 1; i < charArray.length; i++)
		{
			if (Character.isUpperCase(charArray[i]))
			{
				classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);
			}
		}
		
		return classNameStr;
	}
	
	private final Race getVillageMasterRace()
	{
		final String npcClass = getTemplate().jClass.toLowerCase();
		
		if (npcClass.indexOf("human") > -1)
		{
			return Race.Human;
		}
		if (npcClass.indexOf("darkelf") > -1)
		{
			return Race.DarkElf;
		}
		if (npcClass.indexOf("elf") > -1)
		{
			return Race.Elf;
		}
		if (npcClass.indexOf("orc") > -1)
		{
			return Race.Orc;
		}
		
		return Race.Dwarf;
	}
	
	private final ClassType getVillageMasterTeachType()
	{
		final String npcClass = getTemplate().jClass.toLowerCase();
		
		if ((npcClass.indexOf("sanctuary") > -1) || (npcClass.indexOf("clergyman") > -1) || (npcClass.indexOf("temple_master") > -1))
		{
			return ClassType.Priest;
		}
		
		if ((npcClass.indexOf("mageguild") > -1) || (npcClass.indexOf("patriarch") > -1))
		{
			return ClassType.Mystic;
		}
		
		return ClassType.Fighter;
	}
	
	private Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
}