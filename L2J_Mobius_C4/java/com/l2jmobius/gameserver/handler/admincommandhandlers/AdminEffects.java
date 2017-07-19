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
package com.l2jmobius.gameserver.handler.admincommandhandlers;

import java.util.Collection;
import java.util.StringTokenizer;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.handler.IAdminCommandHandler;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Summon;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2ChestInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.serverpackets.CharInfo;
import com.l2jmobius.gameserver.network.serverpackets.Earthquake;
import com.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jmobius.gameserver.network.serverpackets.SocialAction;
import com.l2jmobius.gameserver.network.serverpackets.StopMove;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.network.serverpackets.UserInfo;

/**
 * This class handles following admin commands: - invis / invisible = make yourself invisible - vis / visible = make yourself visible
 */
public class AdminEffects implements IAdminCommandHandler
{
	// private static Logger _log = Logger.getLogger(AdminDelete.class.getName());
	
	private static String[] _adminCommands =
	{
		"admin_invis",
		"admin_invisible",
		"admin_vis",
		"admin_visible",
		"admin_earthquake",
		"admin_bighead",
		"admin_shrinkhead",
		"admin_gmspeed",
		"admin_unpara_all",
		"admin_para_all",
		"admin_unpara",
		"admin_para",
		"admin_polyself",
		"admin_unpolyself",
		"admin_clearteams",
		"admin_setteam",
		"admin_social",
		"admin_effect"
	};
	
	private static final int REQUIRED_LEVEL = Config.GM_GODMODE;
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (!Config.ALT_PRIVILEGES_ADMIN)
		{
			if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
			{
				return false;
			}
		}
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.equals("admin_invis") || command.equals("admin_invisible"))
		{
			activeChar.getAppearance().setInvisible();
			activeChar.broadcastUserInfo();
			
			activeChar.decayMe();
			activeChar.spawnMe();
		}
		else if (command.equals("admin_vis") || command.equals("admin_visible"))
		{
			activeChar.getAppearance().setVisible();
			activeChar.broadcastUserInfo();
		}
		else if (command.startsWith("admin_earthquake"))
		{
			try
			{
				final String val1 = st.nextToken();
				final int intensity = Integer.parseInt(val1);
				final String val2 = st.nextToken();
				final int duration = Integer.parseInt(val2);
				final Earthquake eq = new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), intensity, duration);
				activeChar.broadcastPacket(eq);
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.startsWith("admin_para"))
		{
			String type = "1";
			try
			{
				type = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			
			try
			{
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					
					if (type.equals("1"))
					{
						player.startAbnormalEffect((short) 0x0400);
					}
					else
					{
						player.startAbnormalEffect((short) 0x0800);
					}
					player.setIsParalyzed(true);
					
					final StopMove sm = new StopMove(player);
					player.sendPacket(sm);
					player.broadcastPacket(sm);
				}
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.startsWith("admin_unpara"))
		{
			String type = "1";
			try
			{
				type = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			
			try
			{
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					
					if (type.equals("1"))
					{
						player.stopAbnormalEffect((short) 0x0400);
					}
					else
					{
						player.stopAbnormalEffect((short) 0x0800);
					}
					player.setIsParalyzed(false);
				}
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.equals("admin_para_all"))
		{
			try
			{
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					if (!player.isGM())
					{
						player.startAbnormalEffect((short) 0x0400);
						player.setIsParalyzed(true);
						
						final StopMove sm = new StopMove(player);
						player.sendPacket(sm);
						player.broadcastPacket(sm);
					}
				}
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.equals("admin_unpara_all"))
		{
			try
			{
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					player.stopAbnormalEffect((short) 0x0400);
					player.setIsParalyzed(false);
				}
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.startsWith("admin_bighead"))
		{
			try
			{
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					player.startAbnormalEffect((short) 0x2000);
				}
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.startsWith("admin_shrinkhead"))
		{
			try
			{
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					player.stopAbnormalEffect((short) 0x2000);
				}
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.startsWith("admin_gmspeed"))
		{
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				final boolean sendMessage = activeChar.getFirstEffect(7029) != null;
				
				if ((val == 0) && sendMessage)
				{
					activeChar.stopEffect(7029);
					final SystemMessage sm = new SystemMessage(SystemMessage.EFFECT_S1_DISAPPEARED);
					sm.addSkillName(7029);
					activeChar.sendPacket(sm);
				}
				else if ((val >= 1) && (val <= 4))
				{
					final L2Skill gmSpeedSkill = SkillTable.getInstance().getInfo(7029, val);
					gmSpeedSkill.getEffects(activeChar, activeChar);
					activeChar.sendMessage("Use Super Haste Lv." + val + ".");
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Use //gmspeed value = [0...4].");
			}
			finally
			{
				activeChar.updateEffectIcons();
			}
		}
		else if (command.startsWith("admin_polyself"))
		{
			try
			{
				final String id = st.nextToken();
				activeChar.getPoly().setPolyInfo("npc", id);
				activeChar.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false);
				final CharInfo info1 = new CharInfo(activeChar);
				activeChar.broadcastPacket(info1);
				final UserInfo info2 = new UserInfo(activeChar);
				activeChar.sendPacket(info2);
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.startsWith("admin_unpolyself"))
		{
			activeChar.getPoly().setPolyInfo(null, "1");
			activeChar.decayMe();
			activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
			final CharInfo info1 = new CharInfo(activeChar);
			activeChar.broadcastPacket(info1);
			final UserInfo info2 = new UserInfo(activeChar);
			activeChar.sendPacket(info2);
		}
		else if (command.equals("admin_clear_teams"))
		{
			try
			{
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					player.setAuraColor(0);
					player.broadcastUserInfo();
				}
			}
			catch (final Exception e)
			{
			}
		}
		else if (command.startsWith("admin_setteam"))
		{
			final String val = st.nextToken();
			final int teamVal = Integer.parseInt(val);
			final L2Object target = activeChar.getTarget();
			L2PcInstance player;
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
			
			player.setAuraColor(teamVal);
			player.broadcastUserInfo();
		}
		else if (command.startsWith("admin_social"))
		{
			try
			{
				String target = null;
				L2Object obj = activeChar.getTarget();
				if (st.countTokens() == 2)
				{
					final int social = Integer.parseInt(st.nextToken());
					target = st.nextToken();
					
					if (target != null)
					{
						final L2PcInstance player = L2World.getInstance().getPlayer(target);
						if (player != null)
						{
							if (performSocial(social, player, activeChar))
							{
								activeChar.sendMessage(player.getName() + " was affected by your request.");
							}
						}
						else
						{
							try
							{
								final int radius = Integer.parseInt(target);
								final Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
								{
									for (final L2Object object : objs)
									{
										if (activeChar.isInsideRadius(object, radius, false, false))
										{
											performSocial(social, object, activeChar);
										}
									}
								}
								activeChar.sendMessage(radius + " units radius affected by your request.");
							}
							catch (final NumberFormatException nbe)
							{
								activeChar.sendMessage("Incorrect parameter.");
							}
						}
					}
				}
				else if (st.countTokens() == 1)
				{
					final int social = Integer.parseInt(st.nextToken());
					if (obj == null)
					{
						obj = activeChar;
					}
					
					// if (obj != null)
					// {
					if (performSocial(social, obj, activeChar))
					{
						activeChar.sendMessage(obj.getName() + " was affected by your request.");
					}
					else
					{
						activeChar.sendMessage("Nothing happened.");
					}
					// }
					// else
					// {
					// activeChar.sendMessage("Incorrect target.");
					// }
				}
				else
				{
					activeChar.sendMessage("Usage: //social <social_id> [player_name|radius]");
				}
			}
			catch (final Exception e)
			{
				if (Config.DEBUG)
				{
					e.printStackTrace();
				}
			}
		}
		else if (command.startsWith("admin_effect"))
		{
			try
			{
				L2Object obj = activeChar.getTarget();
				int level = 1, hittime = 1;
				final int skill = Integer.parseInt(st.nextToken());
				
				if (st.hasMoreTokens())
				{
					level = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens())
				{
					hittime = Integer.parseInt(st.nextToken());
				}
				
				if (obj == null)
				{
					obj = activeChar;
				}
				// if (obj != null)
				// {
				if (!(obj instanceof L2Character))
				{
					activeChar.sendMessage("Incorrect target.");
				}
				else
				{
					final L2Character target = (L2Character) obj;
					target.broadcastPacket(new MagicSkillUse(target, activeChar, skill, level, hittime, 0));
					activeChar.sendMessage(obj.getName() + " performs MSU " + skill + "/" + level + " by your request.");
				}
				// }
				// else
				// {
				// activeChar.sendMessage("Incorrect target.");
				// }
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //effect skill [level | level hittime]");
			}
		}
		return true;
	}
	
	private boolean performSocial(int action, L2Object target, L2PcInstance activeChar)
	{
		try
		{
			if (target instanceof L2Character)
			{
				if ((target instanceof L2Summon) || (target instanceof L2ChestInstance))
				{
					activeChar.sendMessage("Nothing happened.");
					return false;
				}
				
				if ((target instanceof L2NpcInstance) && ((action < 1) || (action > 3)))
				{
					activeChar.sendMessage("Nothing happened.");
					return false;
				}
				
				if ((target instanceof L2PcInstance) && ((action < 2) || (action > 16)))
				{
					activeChar.sendMessage("Nothing happened.");
					return false;
				}
				
				final L2Character character = (L2Character) target;
				character.broadcastPacket(new SocialAction(target.getObjectId(), action));
			}
			else
			{
				return false;
			}
		}
		catch (final Exception e)
		{
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
	
	private boolean checkLevel(int level)
	{
		return (level >= REQUIRED_LEVEL);
	}
}