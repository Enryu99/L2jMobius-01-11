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
package handlers.bypasshandlers;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import com.l2jmobius.commons.util.CommonUtil;
import com.l2jmobius.gameserver.cache.HtmCache;
import com.l2jmobius.gameserver.datatables.ItemTable;
import com.l2jmobius.gameserver.enums.AttributeType;
import com.l2jmobius.gameserver.handler.IBypassHandler;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Spawn;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.drops.DropListScope;
import com.l2jmobius.gameserver.model.drops.GeneralDropItem;
import com.l2jmobius.gameserver.model.drops.GroupedGeneralDropItem;
import com.l2jmobius.gameserver.model.drops.IDropItem;
import com.l2jmobius.gameserver.model.items.L2Item;
import com.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jmobius.gameserver.util.HtmlUtil;
import com.l2jmobius.gameserver.util.Util;

/**
 * @author NosBit
 */
public class NpcViewMod implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"NpcViewMod"
	};
	
	private static final int DROP_LIST_ITEMS_PER_PAGE = 10;
	
	@Override
	public boolean useBypass(String command, L2PcInstance activeChar, L2Character bypassOrigin)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (!st.hasMoreTokens())
		{
			_log.warning("Bypass[NpcViewMod] used without enough parameters.");
			return false;
		}
		
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "view":
			{
				final L2Object target;
				if (st.hasMoreElements())
				{
					try
					{
						target = L2World.getInstance().findObject(Integer.parseInt(st.nextToken()));
					}
					catch (NumberFormatException e)
					{
						return false;
					}
				}
				else
				{
					target = activeChar.getTarget();
				}
				
				final L2Npc npc = target instanceof L2Npc ? (L2Npc) target : null;
				if (npc == null)
				{
					return false;
				}
				
				sendNpcView(activeChar, npc);
				break;
			}
			case "droplist":
			{
				if (st.countTokens() < 2)
				{
					_log.warning("Bypass[NpcViewMod] used without enough parameters.");
					return false;
				}
				
				final String dropListScopeString = st.nextToken();
				try
				{
					final DropListScope dropListScope = Enum.valueOf(DropListScope.class, dropListScopeString);
					final L2Object target = L2World.getInstance().findObject(Integer.parseInt(st.nextToken()));
					final L2Npc npc = target instanceof L2Npc ? (L2Npc) target : null;
					if (npc == null)
					{
						return false;
					}
					final int page = st.hasMoreElements() ? Integer.parseInt(st.nextToken()) : 0;
					sendNpcDropList(activeChar, npc, dropListScope, page);
				}
				catch (NumberFormatException e)
				{
					return false;
				}
				catch (IllegalArgumentException e)
				{
					_log.warning("Bypass[NpcViewMod] unknown drop list scope: " + dropListScopeString);
					return false;
				}
				break;
			}
			case "skills":
			{
				final L2Object target;
				if (st.hasMoreElements())
				{
					try
					{
						target = L2World.getInstance().findObject(Integer.parseInt(st.nextToken()));
					}
					catch (NumberFormatException e)
					{
						return false;
					}
				}
				else
				{
					target = activeChar.getTarget();
				}
				
				final L2Npc npc = target instanceof L2Npc ? (L2Npc) target : null;
				if (npc == null)
				{
					return false;
				}
				
				sendNpcSkillView(activeChar, npc);
				break;
			}
			case "aggrolist":
			{
				final L2Object target;
				if (st.hasMoreElements())
				{
					try
					{
						target = L2World.getInstance().findObject(Integer.parseInt(st.nextToken()));
					}
					catch (NumberFormatException e)
					{
						return false;
					}
				}
				else
				{
					target = activeChar.getTarget();
				}
				
				final L2Npc npc = target instanceof L2Npc ? (L2Npc) target : null;
				if (npc == null)
				{
					return false;
				}
				
				sendAggroListView(activeChar, npc);
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
	
	public static void sendNpcView(L2PcInstance activeChar, L2Npc npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar.getHtmlPrefix(), "data/html/mods/NpcView/Info.htm");
		html.replace("%name%", npc.getName());
		html.replace("%hpGauge%", HtmlUtil.getHpGauge(250, (long) npc.getCurrentHp(), npc.getMaxHp(), false));
		html.replace("%mpGauge%", HtmlUtil.getMpGauge(250, (long) npc.getCurrentMp(), npc.getMaxMp(), false));
		
		final L2Spawn npcSpawn = npc.getSpawn();
		if ((npcSpawn == null) || (npcSpawn.getRespawnMinDelay() == 0))
		{
			html.replace("%respawn%", "None");
		}
		else
		{
			TimeUnit timeUnit = TimeUnit.MILLISECONDS;
			long min = Long.MAX_VALUE;
			for (TimeUnit tu : TimeUnit.values())
			{
				final long minTimeFromMillis = tu.convert(npcSpawn.getRespawnMinDelay(), TimeUnit.MILLISECONDS);
				final long maxTimeFromMillis = tu.convert(npcSpawn.getRespawnMaxDelay(), TimeUnit.MILLISECONDS);
				if ((TimeUnit.MILLISECONDS.convert(minTimeFromMillis, tu) == npcSpawn.getRespawnMinDelay()) && (TimeUnit.MILLISECONDS.convert(maxTimeFromMillis, tu) == npcSpawn.getRespawnMaxDelay()))
				{
					if (min > minTimeFromMillis)
					{
						min = minTimeFromMillis;
						timeUnit = tu;
					}
				}
			}
			final long minRespawnDelay = timeUnit.convert(npcSpawn.getRespawnMinDelay(), TimeUnit.MILLISECONDS);
			final long maxRespawnDelay = timeUnit.convert(npcSpawn.getRespawnMaxDelay(), TimeUnit.MILLISECONDS);
			final String timeUnitName = timeUnit.name().charAt(0) + timeUnit.name().toLowerCase().substring(1);
			if (npcSpawn.hasRespawnRandom())
			{
				html.replace("%respawn%", minRespawnDelay + "-" + maxRespawnDelay + " " + timeUnitName);
			}
			else
			{
				html.replace("%respawn%", minRespawnDelay + " " + timeUnitName);
			}
		}
		
		html.replace("%atktype%", CommonUtil.capitalizeFirst(npc.getAttackType().name().toLowerCase()));
		html.replace("%atkrange%", npc.getStat().getPhysicalAttackRange());
		
		html.replace("%patk%", npc.getPAtk());
		html.replace("%pdef%", npc.getPDef());
		
		html.replace("%matk%", npc.getMAtk());
		html.replace("%mdef%", npc.getMDef());
		
		html.replace("%atkspd%", npc.getPAtkSpd());
		html.replace("%castspd%", npc.getMAtkSpd());
		
		html.replace("%critrate%", npc.getStat().getCriticalHit());
		html.replace("%evasion%", npc.getEvasionRate());
		
		html.replace("%accuracy%", npc.getStat().getAccuracy());
		html.replace("%speed%", (int) npc.getStat().getMoveSpeed());
		
		html.replace("%attributeatktype%", npc.getStat().getAttackElement().name());
		html.replace("%attributeatkvalue%", npc.getStat().getAttackElementValue(npc.getStat().getAttackElement()));
		html.replace("%attributefire%", npc.getStat().getDefenseElementValue(AttributeType.FIRE));
		html.replace("%attributewater%", npc.getStat().getDefenseElementValue(AttributeType.WATER));
		html.replace("%attributewind%", npc.getStat().getDefenseElementValue(AttributeType.WIND));
		html.replace("%attributeearth%", npc.getStat().getDefenseElementValue(AttributeType.EARTH));
		html.replace("%attributedark%", npc.getStat().getDefenseElementValue(AttributeType.DARK));
		html.replace("%attributeholy%", npc.getStat().getDefenseElementValue(AttributeType.HOLY));
		
		html.replace("%dropListButtons%", getDropListButtons(npc));
		
		activeChar.sendPacket(html);
	}
	
	public static void sendNpcSkillView(L2PcInstance activeChar, L2Npc npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar.getHtmlPrefix(), "data/html/mods/NpcView/Skills.htm");
		
		final StringBuilder sb = new StringBuilder();
		
		npc.getSkills().values().forEach(s ->
		{
			sb.append("<table width=277 height=32 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
			sb.append("<tr><td width=32>");
			sb.append("<img src=\"");
			sb.append(s.getIcon());
			sb.append("\" width=32 height=32>");
			sb.append("</td><td width=110>");
			sb.append(s.getName());
			sb.append("</td>");
			sb.append("<td width=45 align=center>");
			sb.append(s.getId());
			sb.append("</td>");
			sb.append("<td width=35 align=center>");
			sb.append(s.getLevel());
			sb.append("</td></tr></table>");
		});
		
		html.replace("%skills%", sb.toString());
		html.replace("%npc_name%", npc.getName());
		html.replace("%npcId%", npc.getId());
		
		activeChar.sendPacket(html);
	}
	
	public static void sendAggroListView(L2PcInstance activeChar, L2Npc npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar.getHtmlPrefix(), "data/html/mods/NpcView/AggroList.htm");
		
		final StringBuilder sb = new StringBuilder();
		
		if (npc.isAttackable())
		{
			((L2Attackable) npc).getAggroList().values().forEach(a ->
			{
				sb.append("<table width=277 height=32 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
				sb.append("<tr><td width=110>");
				sb.append(a.getAttacker() != null ? a.getAttacker().getName() : "NULL");
				sb.append("</td>");
				sb.append("<td width=60 align=center>");
				sb.append(a.getHate());
				sb.append("</td>");
				sb.append("<td width=60 align=center>");
				sb.append(a.getDamage());
				sb.append("</td></tr></table>");
			});
		}
		
		html.replace("%aggrolist%", sb.toString());
		html.replace("%npc_name%", npc.getName());
		html.replace("%npcId%", npc.getId());
		html.replace("%objid%", npc.getObjectId());
		
		activeChar.sendPacket(html);
	}
	
	public static String getDropListButtons(L2Npc npc)
	{
		final StringBuilder sb = new StringBuilder();
		final Map<DropListScope, List<IDropItem>> dropLists = npc.getTemplate().getDropLists();
		if ((dropLists != null) && !dropLists.isEmpty() && (dropLists.containsKey(DropListScope.DEATH) || dropLists.containsKey(DropListScope.CORPSE)))
		{
			sb.append("<table width=275 cellpadding=0 cellspacing=0><tr>");
			if (dropLists.containsKey(DropListScope.DEATH))
			{
				sb.append("<td align=center><button value=\"Show Drop\" width=100 height=25 action=\"bypass NpcViewMod dropList DEATH " + npc.getObjectId() + "\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
			
			if (dropLists.containsKey(DropListScope.CORPSE))
			{
				sb.append("<td align=center><button value=\"Show Spoil\" width=100 height=25 action=\"bypass NpcViewMod dropList CORPSE " + npc.getObjectId() + "\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
			
			sb.append("</tr></table>");
		}
		return sb.toString();
	}
	
	public static void sendNpcDropList(L2PcInstance activeChar, L2Npc npc, DropListScope dropListScope, int page)
	{
		final List<IDropItem> dropList = npc.getTemplate().getDropList(dropListScope);
		if ((dropList == null) || dropList.isEmpty())
		{
			return;
		}
		
		int pages = dropList.size() / DROP_LIST_ITEMS_PER_PAGE;
		if ((DROP_LIST_ITEMS_PER_PAGE * pages) < dropList.size())
		{
			pages++;
		}
		
		final StringBuilder pagesSb = new StringBuilder();
		if (pages > 1)
		{
			pagesSb.append("<table><tr>");
			for (int i = 0; i < pages; i++)
			{
				pagesSb.append("<td align=center><button value=\"" + (i + 1) + "\" width=20 height=20 action=\"bypass NpcViewMod dropList " + dropListScope + " " + npc.getObjectId() + " " + i + "\" back=\"L2UI_CT1.Button_DF_Calculator_Down\" fore=\"L2UI_CT1.Button_DF_Calculator\"></td>");
			}
			pagesSb.append("</tr></table>");
		}
		
		if (page >= pages)
		{
			page = pages - 1;
		}
		
		final int start = page > 0 ? page * DROP_LIST_ITEMS_PER_PAGE : 0;
		
		int end = (page * DROP_LIST_ITEMS_PER_PAGE) + DROP_LIST_ITEMS_PER_PAGE;
		if (end > dropList.size())
		{
			end = dropList.size();
		}
		
		final DecimalFormat amountFormat = new DecimalFormat("#,###");
		final DecimalFormat chanceFormat = new DecimalFormat("0.00##");
		
		int leftHeight = 0;
		int rightHeight = 0;
		final StringBuilder leftSb = new StringBuilder();
		final StringBuilder rightSb = new StringBuilder();
		for (int i = start; i < end; i++)
		{
			final StringBuilder sb = new StringBuilder();
			
			int height = 64;
			final IDropItem dropItem = dropList.get(i);
			if (dropItem instanceof GeneralDropItem)
			{
				final GeneralDropItem generalDropItem = (GeneralDropItem) dropItem;
				final L2Item item = ItemTable.getInstance().getTemplate(generalDropItem.getItemId());
				sb.append("<table width=332 cellpadding=2 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
				sb.append("<tr><td width=32 valign=top>");
				sb.append("<img src=\"" + item.getIcon() + "\" width=32 height=32>");
				sb.append("</td><td fixwidth=300 align=center><font name=\"hs9\" color=\"CD9000\">");
				sb.append(item.getName());
				sb.append("</font></td></tr><tr><td width=32></td><td width=300><table width=295 cellpadding=0 cellspacing=0>");
				sb.append("<tr><td width=48 align=right valign=top><font color=\"LEVEL\">Amount:</font></td>");
				sb.append("<td width=247 align=center>");
				
				final long min = generalDropItem.getMin(npc, activeChar);
				final long max = generalDropItem.getMax(npc, activeChar);
				if (min == max)
				{
					sb.append(amountFormat.format(min));
				}
				else
				{
					sb.append(amountFormat.format(min));
					sb.append(" - ");
					sb.append(amountFormat.format(max));
				}
				
				sb.append("</td></tr><tr><td width=48 align=right valign=top><font color=\"LEVEL\">Chance:</font></td>");
				sb.append("<td width=247 align=center>");
				sb.append(chanceFormat.format(Math.min(generalDropItem.getChance(npc, activeChar), 100)));
				sb.append("%</td></tr></table></td></tr><tr><td width=32></td><td width=300>&nbsp;</td></tr></table>");
			}
			else if (dropItem instanceof GroupedGeneralDropItem)
			{
				final GroupedGeneralDropItem generalGroupedDropItem = (GroupedGeneralDropItem) dropItem;
				if (generalGroupedDropItem.getItems().size() == 1)
				{
					final GeneralDropItem generalDropItem = generalGroupedDropItem.getItems().get(0);
					final L2Item item = ItemTable.getInstance().getTemplate(generalDropItem.getItemId());
					sb.append("<table width=332 cellpadding=2 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
					sb.append("<tr><td width=32 valign=top>");
					sb.append("<img src=\"" + item.getIcon() + "\" width=32 height=32>");
					sb.append("</td><td fixwidth=300 align=center><font name=\"hs9\" color=\"CD9000\">");
					sb.append(item.getName());
					sb.append("</font></td></tr><tr><td width=32></td><td width=300><table width=295 cellpadding=0 cellspacing=0>");
					sb.append("<tr><td width=48 align=right valign=top><font color=\"LEVEL\">Amount:</font></td>");
					sb.append("<td width=247 align=center>");
					
					final long min = generalDropItem.getMin(npc, activeChar);
					final long max = generalDropItem.getMax(npc, activeChar);
					if (min == max)
					{
						sb.append(amountFormat.format(min));
					}
					else
					{
						sb.append(amountFormat.format(min));
						sb.append(" - ");
						sb.append(amountFormat.format(max));
					}
					
					sb.append("</td></tr><tr><td width=48 align=right valign=top><font color=\"LEVEL\">Chance:</font></td>");
					sb.append("<td width=247 align=center>");
					sb.append(chanceFormat.format(Math.min(generalGroupedDropItem.getChance(npc, activeChar), 100)));
					sb.append("%</td></tr></table></td></tr><tr><td width=32></td><td width=300>&nbsp;</td></tr></table>");
				}
				else
				{
					sb.append("<table width=332 cellpadding=2 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
					sb.append("<tr><td width=32 valign=top><img src=\"L2UI_CT1.ICON_DF_premiumItem\" width=32 height=32></td>");
					sb.append("<td fixwidth=300 align=center><font name=\"ScreenMessageSmall\" color=\"CD9000\">One from group</font>");
					sb.append("</td></tr><tr><td width=32></td><td width=300><table width=295 cellpadding=0 cellspacing=0><tr>");
					sb.append("<td width=48 align=right valign=top><font color=\"LEVEL\">Chance:</font></td>");
					sb.append("<td width=247 align=center>");
					sb.append(chanceFormat.format(Math.min(generalGroupedDropItem.getChance(npc, activeChar), 100)));
					sb.append("%</td></tr></table><br>");
					
					for (GeneralDropItem generalDropItem : generalGroupedDropItem.getItems())
					{
						final L2Item item = ItemTable.getInstance().getTemplate(generalDropItem.getItemId());
						sb.append("<table width=291 cellpadding=2 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
						sb.append("<tr><td width=32 valign=top>");
						String icon = item.getIcon();
						if (icon == null)
						{
							icon = "icon.etc_question_mark_i00";
						}
						sb.append("<img src=\"" + icon + "\" width=32 height=32>");
						sb.append("</td><td fixwidth=259 align=center><font name=\"hs9\" color=\"CD9000\">");
						sb.append(item.getName());
						sb.append("</font></td></tr><tr><td width=32></td><td width=259><table width=253 cellpadding=0 cellspacing=0>");
						sb.append("<tr><td width=48 align=right valign=top><font color=\"LEVEL\">Amount:</font></td><td width=205 align=center>");
						
						final long min = generalDropItem.getMin(npc, activeChar);
						final long max = generalDropItem.getMax(npc, activeChar);
						if (min == max)
						{
							sb.append(amountFormat.format(min));
						}
						else
						{
							sb.append(amountFormat.format(min));
							sb.append(" - ");
							sb.append(amountFormat.format(max));
						}
						
						sb.append("</td></tr><tr><td width=48 align=right valign=top><font color=\"LEVEL\">Chance:</font></td>");
						sb.append("<td width=205 align=center>");
						sb.append(chanceFormat.format(Math.min(generalDropItem.getChance(), 100)));
						sb.append("%</td></tr></table></td></tr><tr><td width=32></td><td width=259>&nbsp;</td></tr></table>");
						
						height += 64;
					}
					
					sb.append("</td></tr><tr><td width=32></td><td width=300>&nbsp;</td></tr></table>");
				}
			}
			
			if (leftHeight >= (rightHeight + height))
			{
				rightSb.append(sb);
				rightHeight += height;
			}
			else
			{
				leftSb.append(sb);
				leftHeight += height;
			}
		}
		
		final StringBuilder bodySb = new StringBuilder();
		bodySb.append("<table><tr>");
		bodySb.append("<td>");
		bodySb.append(leftSb.toString());
		bodySb.append("</td><td>");
		bodySb.append(rightSb.toString());
		bodySb.append("</td>");
		bodySb.append("</tr></table>");
		
		String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/mods/NpcView/DropList.htm");
		if (html == null)
		{
			_log.warning(NpcViewMod.class.getSimpleName() + ": The html file data/html/mods/NpcView/DropList.htm could not be found.");
			return;
		}
		html = html.replaceAll("%name%", npc.getName());
		html = html.replaceAll("%dropListButtons%", getDropListButtons(npc));
		html = html.replaceAll("%pages%", pagesSb.toString());
		html = html.replaceAll("%items%", bodySb.toString());
		Util.sendCBHtml(activeChar, html);
	}
}
