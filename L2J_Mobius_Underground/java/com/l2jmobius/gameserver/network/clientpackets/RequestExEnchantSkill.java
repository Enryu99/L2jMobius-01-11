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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.commons.network.PacketReader;
import com.l2jmobius.commons.util.Rnd;
import com.l2jmobius.gameserver.data.xml.impl.EnchantSkillGroupsData;
import com.l2jmobius.gameserver.data.xml.impl.SkillData;
import com.l2jmobius.gameserver.model.L2EnchantSkillGroup.EnchantSkillHolder;
import com.l2jmobius.gameserver.model.L2EnchantSkillLearn;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.itemcontainer.Inventory;
import com.l2jmobius.gameserver.model.items.instance.L2ItemInstance;
import com.l2jmobius.gameserver.model.skills.Skill;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.client.L2GameClient;
import com.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfo;
import com.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;
import com.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillResult;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;

/**
 * Format (ch) dd c: (id) 0xD0 h: (subid) 0x06 d: skill id d: skill lvl
 * @author -Wooden-
 */
public final class RequestExEnchantSkill implements IClientIncomingPacket
{
	private static final Logger _logEnchant = Logger.getLogger("enchant.skills");
	
	private int _type; // enchant type: 0 - normal, 1 - safe, 2 - untrain, 3 - change route, 4 - 100%
	
	private int _skillId;
	private int _skillLvl;
	private int _fullLvl;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_type = packet.readD();
		_skillId = packet.readD();
		_fullLvl = packet.readD();
		if (_fullLvl < 100)
		{
			_skillLvl = _fullLvl;
		}
		else
		{
			_skillLvl = _fullLvl >> 16;
		}
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		if ((_skillId <= 0) || (_skillLvl <= 0))
		{
			return;
		}
		
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.getClassId().level() < 3) // requires to have 3rd class quest completed
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_CORRESPONDING_FUNCTION_WHEN_COMPLETING_THE_THIRD_CLASS_CHANGE);
			return;
		}
		
		if (player.getLevel() < 76)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_ON_THIS_LEVEL_YOU_CAN_USE_THE_CORRESPONDING_FUNCTION_ON_LEVELS_HIGHER_THAN_LV_76);
			return;
		}
		
		if (!player.isAllowedToEnchantSkills())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_STATE_YOU_CAN_ENHANCE_SKILLS_WHEN_NOT_IN_BATTLE_AND_CANNOT_USE_THE_FUNCTION_WHILE_TRANSFORMED_IN_BATTLE_ON_A_MOUNT_OR_WHILE_THE_SKILL_IS_ON_COOLDOWN);
			return;
		}
		
		if (player.isSellingBuffs())
		{
			player.sendMessage("You cannot use the skill enhancing function while you selling buffs.");
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLvl);
		if (skill == null)
		{
			return;
		}
		
		final L2EnchantSkillLearn s = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(_skillId);
		if (s == null)
		{
			return;
		}
		final int _elvl = ((_skillLvl % 100) - 1) / 10;
		if (_type == 0) // enchant
		{
			final EnchantSkillHolder esd = s.getEnchantSkillHolder(_skillLvl);
			final int beforeEnchantSkillLevel = player.getSkillLevel(_skillId);
			if (beforeEnchantSkillLevel != s.getMinSkillLevel(_skillLvl))
			{
				return;
			}
			
			final int costMultiplier = EnchantSkillGroupsData.NORMAL_ENCHANT_COST_MULTIPLIER;
			final int requiredSp = esd.getSpCost() * costMultiplier;
			if (player.getSp() >= requiredSp)
			{
				final boolean usesBook = true;
				final int reqItemId;
				if (player.getClassId().level() == 3)
				{
					reqItemId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK_OLD;
				}
				else if (_elvl == 0)
				{
					reqItemId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK;
				}
				else if (_elvl == 1)
				{
					reqItemId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK_V2;
				}
				else
				{
					reqItemId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK_V3;
				}
				final L2ItemInstance spb = player.getInventory().getItemByItemId(reqItemId);
				
				if (Config.ES_SP_BOOK_NEEDED && usesBook && (spb == null))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				final int requiredAdena = esd.getAdenaCost() * costMultiplier;
				if (player.getInventory().getAdena() < requiredAdena)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				boolean check = player.getStat().removeExpAndSp(0, requiredSp, false);
				if (Config.ES_SP_BOOK_NEEDED && usesBook)
				{
					check &= player.destroyItem("Consume", spb.getObjectId(), 1, player, true);
				}
				
				check &= player.destroyItemByItemId("Consume", Inventory.ADENA_ID, requiredAdena, player, true);
				if (!check)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				// ok. Destroy ONE copy of the book
				final int rate = esd.getRate(player);
				if (Rnd.get(100) <= rate)
				{
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final LogRecord record = new LogRecord(Level.INFO, "Success");
						record.setParameters(new Object[]
						{
							player,
							skill,
							spb,
							rate
						});
						record.setLoggerName("skill");
						_logEnchant.log(record);
					}
					
					player.addSkill(skill, true);
					player.sendPacket(ExEnchantSkillResult.valueOf(true));
					
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
					sm.addSkillName(_skillId);
					player.sendPacket(sm);
					
					if (Config.DEBUG)
					{
						_log.fine("Learned skill ID: " + _skillId + " Level: " + _skillLvl + " for " + requiredSp + " SP, " + requiredAdena + " Adena.");
					}
				}
				else
				{
					if (player.getClassId().level() == 3)
					{
						player.addSkill(SkillData.getInstance().getSkill(_skillId, s.getBaseLevel()), true);
					}
					else
					{
						final int _clvl = ((((_skillLvl % 100) - 1) / 10) * 10) + ((_skillLvl / 1000) * 1000);
						player.addSkill(SkillData.getInstance().getSkill(_skillId, _clvl), true);
					}
					player.sendPacket(SystemMessageId.SKILL_ENCHANT_FAILED_THE_SKILL_WILL_BE_INITIALIZED);
					player.sendPacket(ExEnchantSkillResult.valueOf(false));
					
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final LogRecord record = new LogRecord(Level.INFO, "Fail");
						record.setParameters(new Object[]
						{
							player,
							skill,
							spb,
							rate
						});
						record.setLoggerName("skill");
						_logEnchant.log(record);
					}
				}
				
				player.sendPacket(new UserInfo(player));
				player.sendSkillList();
				final int afterEnchantSkillLevel = player.getSkillLevel(_skillId);
				player.sendPacket(new ExEnchantSkillInfo(_skillId, afterEnchantSkillLevel));
				player.sendPacket(new ExEnchantSkillInfoDetail(0, _skillId, afterEnchantSkillLevel + 1, player));
				player.updateShortCuts(_skillId, afterEnchantSkillLevel);
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
			}
		}
		else if (_type == 1) // safe enchant
		{
			final int costMultiplier = EnchantSkillGroupsData.SAFE_ENCHANT_COST_MULTIPLIER;
			final int reqItemId;
			if (player.getClassId().level() == 3)
			{
				reqItemId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK_OLD;
			}
			else if (_elvl == 0)
			{
				reqItemId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK;
			}
			else if (_elvl == 1)
			{
				reqItemId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK_V2;
			}
			else
			{
				reqItemId = EnchantSkillGroupsData.SAFE_ENCHANT_BOOK_V3;
			}
			final EnchantSkillHolder esd = s.getEnchantSkillHolder(_skillLvl);
			final int beforeEnchantSkillLevel = player.getSkillLevel(_skillId);
			if (beforeEnchantSkillLevel != s.getMinSkillLevel(_skillLvl))
			{
				return;
			}
			
			final int requiredSp = esd.getSpCost() * costMultiplier;
			final int requireditems = esd.getAdenaCost() * costMultiplier;
			final int rate = esd.getRate(player);
			
			if (player.getSp() >= requiredSp)
			{
				final L2ItemInstance spb = player.getInventory().getItemByItemId(reqItemId);
				if (spb == null)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				if (player.getInventory().getAdena() < requireditems)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				boolean check = player.getStat().removeExpAndSp(0, requiredSp, false);
				check &= player.destroyItem("Consume", spb.getObjectId(), 1, player, true);
				
				check &= player.destroyItemByItemId("Consume", Inventory.ADENA_ID, requireditems, player, true);
				
				if (!check)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				if (Rnd.get(100) <= rate)
				{
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final LogRecord record = new LogRecord(Level.INFO, "Safe Success");
						record.setParameters(new Object[]
						{
							player,
							skill,
							spb,
							rate
						});
						record.setLoggerName("skill");
						_logEnchant.log(record);
					}
					
					player.addSkill(skill, true);
					
					if (Config.DEBUG)
					{
						_log.fine("Learned skill ID: " + _skillId + " Level: " + _skillLvl + " for " + requiredSp + " SP, " + requireditems + " Adena.");
					}
					
					player.sendPacket(ExEnchantSkillResult.valueOf(true));
					
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
					sm.addSkillName(_skillId);
					player.sendPacket(sm);
				}
				else
				{
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final LogRecord record = new LogRecord(Level.INFO, "Safe Fail");
						record.setParameters(new Object[]
						{
							player,
							skill,
							spb,
							rate
						});
						record.setLoggerName("skill");
						_logEnchant.log(record);
					}
					
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SKILL_ENCHANT_FAILED_THE_SKILL_WILL_BE_INITIALIZED);
					player.sendPacket(sm);
					player.sendPacket(ExEnchantSkillResult.valueOf(false));
				}
				
				player.sendPacket(new UserInfo(player));
				player.sendSkillList();
				final int afterEnchantSkillLevel = player.getSkillLevel(_skillId);
				player.sendPacket(new ExEnchantSkillInfo(_skillId, afterEnchantSkillLevel));
				player.sendPacket(new ExEnchantSkillInfoDetail(1, _skillId, afterEnchantSkillLevel + 1, player));
				player.updateShortCuts(_skillId, afterEnchantSkillLevel);
			}
			else
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL));
			}
		}
		else if (_type == 2) // untrain
		{
			return;
		}
		else if (_type == 3) // change route
		{
			final int reqItemId;
			if (player.getClassId().level() == 3)
			{
				reqItemId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK_OLD;
			}
			else if (_elvl == 0)
			{
				reqItemId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK;
			}
			else if (_elvl == 1)
			{
				reqItemId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK_V2;
			}
			else
			{
				reqItemId = EnchantSkillGroupsData.CHANGE_ENCHANT_BOOK_V3;
			}
			
			final int beforeEnchantSkillLevel = player.getSkillLevel(_skillId);
			if (beforeEnchantSkillLevel <= 1000)
			{
				return;
			}
			
			final int currentEnchantLevel = beforeEnchantSkillLevel % 1000;
			if (currentEnchantLevel != (_skillLvl % 1000))
			{
				return;
			}
			final EnchantSkillHolder esd = s.getEnchantSkillHolder(_skillLvl);
			
			final int requiredSp = esd.getSpCost();
			final int requireditems = esd.getAdenaCost();
			
			if (player.getSp() >= requiredSp)
			{
				final L2ItemInstance spb = player.getInventory().getItemByItemId(reqItemId);
				if (Config.ES_SP_BOOK_NEEDED && (spb == null))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_SKILL_ROUTE_CHANGE);
					return;
				}
				
				if (player.getInventory().getAdena() < requireditems)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				boolean check = player.getStat().removeExpAndSp(0, requiredSp, false);
				if (Config.ES_SP_BOOK_NEEDED)
				{
					check &= player.destroyItem("Consume", spb.getObjectId(), 1, player, true);
				}
				
				check &= player.destroyItemByItemId("Consume", Inventory.ADENA_ID, requireditems, player, true);
				
				if (!check)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
				
				final int levelPenalty = Rnd.get(Math.min(4, currentEnchantLevel));
				_skillLvl -= levelPenalty;
				if ((_skillLvl % 1000) == 0)
				{
					_skillLvl = s.getBaseLevel();
				}
				
				if (Config.LOG_SKILL_ENCHANTS)
				{
					final LogRecord record = new LogRecord(Level.INFO, "Route Change");
					record.setParameters(new Object[]
					{
						player,
						skill,
						spb
					});
					record.setLoggerName("skill");
					_logEnchant.log(record);
				}
				
				player.addSkill(SkillData.getInstance().getSkill(_skillId, _skillLvl), true);
				player.sendPacket(ExEnchantSkillResult.valueOf(true));
				
				if (Config.DEBUG)
				{
					_log.fine("Learned skill ID: " + _skillId + " Level: " + _skillLvl + " for " + requiredSp + " SP, " + requireditems + " Adena.");
				}
				
				player.sendPacket(new UserInfo(player));
				
				final SystemMessage sm;
				if (levelPenalty == 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ENCHANT_SKILL_ROUTE_CHANGE_WAS_SUCCESSFUL_LV_OF_ENCHANT_SKILL_S1_WILL_REMAIN);
					sm.addSkillName(_skillId);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ENCHANT_SKILL_ROUTE_CHANGE_WAS_SUCCESSFUL_LV_OF_ENCHANT_SKILL_S1_HAS_BEEN_DECREASED_BY_S2);
					sm.addSkillName(_skillId);
					
					if (_skillLvl > 1000)
					{
						sm.addInt(_skillLvl % 1000);
					}
					else
					{
						sm.addInt(0);
					}
				}
				player.sendPacket(sm);
				player.sendSkillList();
				final int afterEnchantSkillLevel = player.getSkillLevel(_skillId);
				player.sendPacket(new ExEnchantSkillInfo(_skillId, afterEnchantSkillLevel));
				player.sendPacket(new ExEnchantSkillInfoDetail(3, _skillId, afterEnchantSkillLevel, player));
				player.updateShortCuts(_skillId, afterEnchantSkillLevel);
			}
			else
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL));
			}
		}
		else if (_type == 4) // 100% enchant
		{
			final int reqItemId;
			if (player.getClassId().level() == 3)
			{
				reqItemId = EnchantSkillGroupsData.IMMORTAL_SCROLL;
			}
			else if (_elvl == 0)
			{
				reqItemId = EnchantSkillGroupsData.IMMORTAL_SCROLL;
			}
			else if (_elvl == 1)
			{
				reqItemId = EnchantSkillGroupsData.IMMORTAL_SCROLL_V2;
			}
			else
			{
				reqItemId = EnchantSkillGroupsData.IMMORTAL_SCROLL_V3;
			}
			final int beforeEnchantSkillLevel = player.getSkillLevel(_skillId);
			if (beforeEnchantSkillLevel != s.getMinSkillLevel(_skillLvl))
			{
				return;
			}
			
			final L2ItemInstance spb = player.getInventory().getItemByItemId(reqItemId);
			if (spb == null)
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
				return;
			}
			
			player.destroyItem("Consume", spb.getObjectId(), 1, player, true);
			
			if (Config.LOG_SKILL_ENCHANTS)
			{
				final LogRecord record = new LogRecord(Level.INFO, "100% Success");
				record.setParameters(new Object[]
				{
					player,
					skill,
					spb,
					100
				});
				record.setLoggerName("skill");
				_logEnchant.log(record);
			}
			
			player.addSkill(skill, true);
			
			if (Config.DEBUG)
			{
				_log.fine("Learned skill ID: " + _skillId + " Level: " + _skillLvl + ".");
			}
			
			player.sendPacket(ExEnchantSkillResult.valueOf(true));
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
			sm.addSkillName(_skillId);
			player.sendPacket(sm);
			player.sendPacket(new UserInfo(player));
			player.sendSkillList();
			final int afterEnchantSkillLevel = player.getSkillLevel(_skillId);
			player.sendPacket(new ExEnchantSkillInfo(_skillId, afterEnchantSkillLevel));
			player.sendPacket(new ExEnchantSkillInfoDetail(1, _skillId, afterEnchantSkillLevel + 1, player));
			player.updateShortCuts(_skillId, afterEnchantSkillLevel);
		}
	}
}
