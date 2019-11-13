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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.datatables.SkillTable;
import org.l2jmobius.gameserver.datatables.sql.CharNameTable;
import org.l2jmobius.gameserver.datatables.sql.CharTemplateTable;
import org.l2jmobius.gameserver.datatables.sql.SkillTreeTable;
import org.l2jmobius.gameserver.datatables.xml.ExperienceData;
import org.l2jmobius.gameserver.datatables.xml.ItemTable;
import org.l2jmobius.gameserver.idfactory.IdFactory;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.model.ShortCut;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.CharCreateFail;
import org.l2jmobius.gameserver.network.serverpackets.CharCreateOk;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectInfo;
import org.l2jmobius.gameserver.util.Util;

@SuppressWarnings("unused")
public class CharacterCreate extends GameClientPacket
{
	private static Logger LOGGER = Logger.getLogger(CharacterCreate.class.getName());
	private static final Object CREATION_LOCK = new Object();
	
	private String _name;
	private byte _sex;
	private byte _hairStyle;
	private byte _hairColor;
	private byte _face;
	private int _race;
	private int _classId;
	private int _int;
	private int _str;
	private int _con;
	private int _men;
	private int _dex;
	private int _wit;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		_int = readD();
		_str = readD();
		_con = readD();
		_men = readD();
		_dex = readD();
		_wit = readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		if ((_name.length() < 3) || (_name.length() > 16) || !Util.isAlphaNumeric(_name) || !isValidName(_name))
		{
			sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
			return;
		}
		
		PlayerInstance newChar = null;
		PlayerTemplate template = null;
		
		// Since checks for duplicate names are done using SQL, lock must be held until data is written to DB as well.
		synchronized (CharNameTable.getInstance())
		{
			if ((CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT) && (Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0))
			{
				sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			else if (CharNameTable.getInstance().doesCharNameExist(_name))
			{
				sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
				return;
			}
			
			template = CharTemplateTable.getInstance().getTemplate(_classId);
			if ((template == null) || (template.classBaseLevel > 1))
			{
				sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			final int objectId = IdFactory.getInstance().getNextId();
			newChar = PlayerInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
			
			newChar.setCurrentHp(newChar.getMaxHp()); // L2Off like
			// newChar.setCurrentCp(template.baseCpMax);
			newChar.setCurrentCp(0); // L2Off like
			newChar.setCurrentMp(newChar.getMaxMp()); // L2Off like
			// newChar.setMaxLoad(template.baseLoad);
			
			// send acknowledgement
			sendPacket(new CharCreateOk()); // Success
			initNewChar(getClient(), newChar);
		}
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
	
	private void initNewChar(GameClient client, PlayerInstance newChar)
	{
		World.getInstance().storeObject(newChar);
		final PlayerTemplate template = newChar.getTemplate();
		
		// Starting Items
		if (Config.STARTING_ADENA > 0)
		{
			newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		}
		
		if (Config.STARTING_AA > 0)
		{
			newChar.addAncientAdena("Init", Config.STARTING_AA, null, false);
		}
		
		if (Config.CUSTOM_STARTER_ITEMS_ENABLED)
		{
			if (newChar.isMageClass())
			{
				for (int[] reward : Config.STARTING_CUSTOM_ITEMS_M)
				{
					if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
					{
						newChar.getInventory().addItem("Starter Items Mage", reward[0], reward[1], newChar, null);
					}
					else
					{
						for (int i = 0; i < reward[1]; ++i)
						{
							newChar.getInventory().addItem("Starter Items Mage", reward[0], 1, newChar, null);
						}
					}
				}
			}
			else
			{
				for (int[] reward : Config.STARTING_CUSTOM_ITEMS_F)
				{
					if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
					{
						newChar.getInventory().addItem("Starter Items Fighter", reward[0], reward[1], newChar, null);
					}
					else
					{
						for (int i = 0; i < reward[1]; ++i)
						{
							newChar.getInventory().addItem("Starter Items Fighter", reward[0], 1, newChar, null);
						}
					}
				}
			}
		}
		
		if (Config.SPAWN_CHAR)
		{
			newChar.setXYZInvisible(Config.SPAWN_X, Config.SPAWN_Y, Config.SPAWN_Z);
		}
		else
		{
			newChar.setXYZInvisible(template.spawnX, template.spawnY, template.spawnZ);
		}
		
		if (Config.ALLOW_CREATE_LVL)
		{
			newChar.getStat().addExp(ExperienceData.getInstance().getExpForLevel(Config.CHAR_CREATE_LVL));
		}
		
		if (Config.CHAR_TITLE)
		{
			newChar.setTitle(Config.ADD_CHAR_TITLE);
		}
		else
		{
			newChar.setTitle("");
		}
		
		if (Config.PVP_PK_TITLE)
		{
			newChar.setTitle(Config.PVP_TITLE_PREFIX + "0" + Config.PK_TITLE_PREFIX + "0 ");
		}
		
		// Shortcuts
		newChar.registerShortCut(new ShortCut(0, 0, 3, 2, -1, 1)); // Attack
		newChar.registerShortCut(new ShortCut(3, 0, 3, 5, -1, 1)); // Take
		newChar.registerShortCut(new ShortCut(10, 0, 3, 0, -1, 1)); // Sit
		
		final ItemTable itemTable = ItemTable.getInstance();
		final Item[] items = template.getItems();
		
		for (Item item2 : items)
		{
			final ItemInstance item = newChar.getInventory().addItem("Init", item2.getItemId(), 1, newChar, null);
			
			if (item.getItemId() == 5588)
			{
				newChar.registerShortCut(new ShortCut(11, 0, 1, item.getObjectId(), -1, 1)); // Tutorial Book shortcut
			}
			
			if (item.isEquipable())
			{
				if ((newChar.getActiveWeaponItem() == null) || (item.getItem().getType2() == Item.TYPE2_WEAPON))
				{
					newChar.getInventory().equipItemAndRecord(item);
				}
			}
		}
		
		final SkillLearn[] startSkills = SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId());
		
		for (SkillLearn startSkill : startSkills)
		{
			newChar.addSkill(SkillTable.getInstance().getInfo(startSkill.getId(), startSkill.getLevel()), true);
			
			if ((startSkill.getId() == 1001) || (startSkill.getId() == 1177))
			{
				newChar.registerShortCut(new ShortCut(1, 0, 2, startSkill.getId(), 1, 1));
			}
			
			if (startSkill.getId() == 1216)
			{
				newChar.registerShortCut(new ShortCut(10, 0, 2, startSkill.getId(), 1, 1));
			}
		}
		
		if (!Config.DISABLE_TUTORIAL && !Config.ALT_DEV_NO_QUESTS)
		{
			startTutorialQuest(newChar);
		}
		
		newChar.store();
		newChar.deleteMe(); // Release the world of this character and it's inventory
		
		// Before the char selection, check shutdown status
		if (GameServer.getSelectorThread().isShutdown())
		{
			client.closeNow();
			return;
		}
		
		// Send char list
		final CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
		client.getConnection().sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}
	
	public void startTutorialQuest(PlayerInstance player)
	{
		final QuestState qs1 = player.getQuestState("NewbieHelper");
		Quest q1 = null;
		if (qs1 == null)
		{
			q1 = QuestManager.getInstance().getQuest("NewbieHelper");
		}
		if (q1 != null)
		{
			q1.newQuestState(player);
		}
		
		final QuestState qs2 = player.getQuestState("Tutorial");
		Quest q2 = null;
		if (qs2 == null)
		{
			q2 = QuestManager.getInstance().getQuest("Tutorial");
		}
		if (q2 != null)
		{
			q2.newQuestState(player);
		}
	}
}