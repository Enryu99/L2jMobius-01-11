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
package com.l2jmobius.gameserver.skills;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.base.Race;
import com.l2jmobius.gameserver.skills.conditions.Condition;
import com.l2jmobius.gameserver.skills.conditions.ConditionElementSeed;
import com.l2jmobius.gameserver.skills.conditions.ConditionGameChance;
import com.l2jmobius.gameserver.skills.conditions.ConditionGameTime;
import com.l2jmobius.gameserver.skills.conditions.ConditionGameTime.CheckGameTime;
import com.l2jmobius.gameserver.skills.conditions.ConditionLogicAnd;
import com.l2jmobius.gameserver.skills.conditions.ConditionLogicNot;
import com.l2jmobius.gameserver.skills.conditions.ConditionLogicOr;
import com.l2jmobius.gameserver.skills.conditions.ConditionPlayerHp;
import com.l2jmobius.gameserver.skills.conditions.ConditionPlayerHpPercentage;
import com.l2jmobius.gameserver.skills.conditions.ConditionPlayerLevel;
import com.l2jmobius.gameserver.skills.conditions.ConditionPlayerRace;
import com.l2jmobius.gameserver.skills.conditions.ConditionPlayerState;
import com.l2jmobius.gameserver.skills.conditions.ConditionPlayerState.CheckPlayerState;
import com.l2jmobius.gameserver.skills.conditions.ConditionSkillStats;
import com.l2jmobius.gameserver.skills.conditions.ConditionSlotItemId;
import com.l2jmobius.gameserver.skills.conditions.ConditionTargetAggro;
import com.l2jmobius.gameserver.skills.conditions.ConditionTargetLevel;
import com.l2jmobius.gameserver.skills.conditions.ConditionTargetUsesWeaponKind;
import com.l2jmobius.gameserver.skills.conditions.ConditionUsingItemType;
import com.l2jmobius.gameserver.skills.conditions.ConditionUsingSkill;
import com.l2jmobius.gameserver.skills.conditions.ConditionWithSkill;
import com.l2jmobius.gameserver.skills.effects.EffectTemplate;
import com.l2jmobius.gameserver.skills.funcs.FuncTemplate;
import com.l2jmobius.gameserver.skills.funcs.Lambda;
import com.l2jmobius.gameserver.skills.funcs.LambdaCalc;
import com.l2jmobius.gameserver.skills.funcs.LambdaConst;
import com.l2jmobius.gameserver.skills.funcs.LambdaStats;
import com.l2jmobius.gameserver.templates.L2ArmorType;
import com.l2jmobius.gameserver.templates.L2Item;
import com.l2jmobius.gameserver.templates.L2Weapon;
import com.l2jmobius.gameserver.templates.L2WeaponType;
import com.l2jmobius.gameserver.templates.StatsSet;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author mkizub TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code Templates
 */
abstract class DocumentBase
{
	static Logger _log = Logger.getLogger(DocumentBase.class.getName());
	
	private final File file;
	protected Map<String, Number[]> tables;
	
	DocumentBase(File pFile)
	{
		file = pFile;
		tables = new FastMap<>();
	}
	
	Document parse()
	{
		Document doc;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "Error loading file " + file, e);
			return null;
		}
		try
		{
			parseDocument(doc);
		}
		catch (final Exception e)
		{
			_log.log(Level.SEVERE, "Error in file " + file, e);
			return null;
		}
		return doc;
	}
	
	protected abstract void parseDocument(Document doc);
	
	protected abstract StatsSet getStatsSet();
	
	protected abstract Number getTableValue(String name);
	
	protected abstract Number getTableValue(String name, int idx);
	
	protected void resetTable()
	{
		tables = new FastMap<>();
	}
	
	protected void setTable(String name, Number[] table)
	{
		tables.put(name, table);
	}
	
	protected void parseTemplate(Node n, Object template)
	{
		Condition condition = null;
		n = n.getFirstChild();
		if (n == null)
		{
			return;
		}
		if ("cond".equalsIgnoreCase(n.getNodeName()))
		{
			condition = parseCondition(n.getFirstChild(), template);
			final Node msg = n.getAttributes().getNamedItem("msg");
			
			if ((condition != null) && (msg != null))
			{
				condition.setMessage(msg.getNodeValue());
			}
			
			n = n.getNextSibling();
		}
		for (; n != null; n = n.getNextSibling())
		{
			if ("add".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Add", condition);
			}
			else if ("sub".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Sub", condition);
			}
			else if ("mul".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Mul", condition);
			}
			else if ("div".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Div", condition);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Set", condition);
			}
			else if ("enchant".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Enchant", condition);
			}
			else if ("skill".equalsIgnoreCase(n.getNodeName()))
			{
				attachSkill(n, template, condition);
			}
			else if ("effect".equalsIgnoreCase(n.getNodeName()))
			{
				if (template instanceof EffectTemplate)
				{
					throw new RuntimeException("Nested effects");
				}
				attachEffect(n, template, condition);
			}
		}
	}
	
	protected void attachFunc(Node n, Object template, String name, Condition attachCond)
	{
		final Stats stat = Stats.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
		final String order = n.getAttributes().getNamedItem("order").getNodeValue();
		final Lambda lambda = getLambda(n, template);
		final int ord = getNumber(order, template).intValue();
		
		final Condition applyCond = parseCondition(n.getFirstChild(), template);
		final FuncTemplate ft = new FuncTemplate(attachCond, applyCond, name, stat, ord, lambda);
		if (template instanceof L2Item)
		{
			((L2Item) template).attach(ft);
		}
		else if (template instanceof L2Skill)
		{
			((L2Skill) template).attach(ft);
		}
		else if (template instanceof EffectTemplate)
		{
			((EffectTemplate) template).attach(ft);
		}
	}
	
	protected void attachLambdaFunc(Node n, Object template, LambdaCalc calc)
	{
		String name = n.getNodeName();
		final TextBuilder sb = new TextBuilder(name);
		sb.setCharAt(0, Character.toUpperCase(name.charAt(0)));
		name = sb.toString();
		final Lambda lambda = getLambda(n, template);
		final FuncTemplate ft = new FuncTemplate(null, null, name, null, calc._funcs.length, lambda);
		calc.addFunc(ft.getFunc(new Env(), calc));
	}
	
	protected void attachEffect(Node n, Object template, Condition attachCond)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final String name = attrs.getNamedItem("name").getNodeValue();
		int time, count = 1;
		if (attrs.getNamedItem("count") != null)
		{
			count = getNumber(attrs.getNamedItem("count").getNodeValue(), template).intValue();
		}
		if (attrs.getNamedItem("time") != null)
		{
			time = getNumber(attrs.getNamedItem("time").getNodeValue(), template).intValue();
			if (Config.ENABLE_MODIFY_SKILL_DURATION)
			{
				if (Config.SKILL_DURATION_LIST.containsKey(((L2Skill) template).getId()))
				{
					if ((((L2Skill) template).getLevel() >= 100) && (((L2Skill) template).getLevel() < 140))
					{
						time += Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
					}
					else
					{
						time = Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
					}
					
					if (Config.DEBUG)
					{
						_log.info("*** Skill " + ((L2Skill) template).getName() + " (" + ((L2Skill) template).getLevel() + ") changed duration to " + time + " seconds.");
					}
				}
			}
		}
		else
		{
			time = ((L2Skill) template).getBuffDuration() / 1000 / count;
		}
		
		boolean self = false;
		if (attrs.getNamedItem("self") != null)
		{
			if (getNumber(attrs.getNamedItem("self").getNodeValue(), template).intValue() == 1)
			{
				self = true;
			}
		}
		
		boolean icon = true;
		if (attrs.getNamedItem("noicon") != null)
		{
			if (getNumber(attrs.getNamedItem("noicon").getNodeValue(), template).intValue() == 1)
			{
				icon = false;
			}
		}
		
		final Lambda lambda = getLambda(n, template);
		final Condition applayCond = parseCondition(n.getFirstChild(), template);
		short abnormal = 0;
		if (attrs.getNamedItem("abnormal") != null)
		{
			final String abn = attrs.getNamedItem("abnormal").getNodeValue();
			if (abn.equals("poison"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_POISON;
			}
			if (abn.equals("bleeding"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_BLEEDING;
			}
			if (abn.equals("flame"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_FLAME;
			}
			if (abn.equals("bighead"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_BIG_HEAD;
			}
		}
		float stackOrder = 0;
		String stackType = "none";
		if (attrs.getNamedItem("stackType") != null)
		{
			stackType = attrs.getNamedItem("stackType").getNodeValue();
		}
		if (attrs.getNamedItem("stackOrder") != null)
		{
			stackOrder = getNumber(attrs.getNamedItem("stackOrder").getNodeValue(), template).floatValue();
		}
		final EffectTemplate lt = new EffectTemplate(attachCond, applayCond, name, lambda, count, time, abnormal, stackType, stackOrder, icon);
		parseTemplate(n, lt);
		if (template instanceof L2Item)
		{
			((L2Item) template).attach(lt);
		}
		else if ((template instanceof L2Skill) && !self)
		{
			((L2Skill) template).attach(lt);
		}
		else if ((template instanceof L2Skill) && self)
		{
			((L2Skill) template).attachSelf(lt);
		}
	}
	
	protected void attachSkill(Node n, Object template, Condition attachCond)
	{
		final NamedNodeMap attrs = n.getAttributes();
		int id = 0, lvl = 1;
		if (attrs.getNamedItem("id") != null)
		{
			id = getNumber(attrs.getNamedItem("id").getNodeValue(), template).intValue();
		}
		if (attrs.getNamedItem("lvl") != null)
		{
			lvl = getNumber(attrs.getNamedItem("lvl").getNodeValue(), template).intValue();
		}
		
		final L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
		if (attrs.getNamedItem("chance") != null)
		{
			if ((template instanceof L2Weapon) || (template instanceof L2Item))
			{
				skill.attach(new ConditionGameChance(getNumber(attrs.getNamedItem("chance").getNodeValue(), template).intValue()), true);
			}
			else
			{
				skill.attach(new ConditionGameChance(getNumber(attrs.getNamedItem("chance").getNodeValue(), template).intValue()), false);
			}
		}
		if (template instanceof L2Weapon)
		{
			if ((attrs.getNamedItem("onUse") != null) || ((attrs.getNamedItem("onCrit") == null) && (attrs.getNamedItem("onCast") == null)))
			{
				((L2Weapon) template).attach(skill); // Attach as skill triggered on use
			}
			if (attrs.getNamedItem("onCrit") != null)
			{
				((L2Weapon) template).attachOnCrit(skill); // Attach as skill triggered on critical hit
			}
			if (attrs.getNamedItem("onCast") != null)
			{
				((L2Weapon) template).attachOnCast(skill); // Attach as skill triggered on cast
			}
		}
		else if (template instanceof L2Item)
		{
			((L2Item) template).attach(skill); // Attach as skill triggered on use
		}
	}
	
	protected Condition parseCondition(Node n, Object template)
	{
		while ((n != null) && (n.getNodeType() != Node.ELEMENT_NODE))
		{
			n = n.getNextSibling();
		}
		if (n == null)
		{
			return null;
		}
		if ("and".equalsIgnoreCase(n.getNodeName()))
		{
			return parseLogicAnd(n, template);
		}
		if ("or".equalsIgnoreCase(n.getNodeName()))
		{
			return parseLogicOr(n, template);
		}
		if ("not".equalsIgnoreCase(n.getNodeName()))
		{
			return parseLogicNot(n, template);
		}
		if ("player".equalsIgnoreCase(n.getNodeName()))
		{
			return parsePlayerCondition(n);
		}
		if ("target".equalsIgnoreCase(n.getNodeName()))
		{
			return parseTargetCondition(n, template);
		}
		if ("skill".equalsIgnoreCase(n.getNodeName()))
		{
			return parseSkillCondition(n);
		}
		if ("using".equalsIgnoreCase(n.getNodeName()))
		{
			return parseUsingCondition(n);
		}
		if ("game".equalsIgnoreCase(n.getNodeName()))
		{
			return parseGameCondition(n);
		}
		return null;
	}
	
	protected Condition parseLogicAnd(Node n, Object template)
	{
		final ConditionLogicAnd cond = new ConditionLogicAnd();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		if ((cond._conditions == null) || (cond._conditions.length == 0))
		{
			_log.severe("Empty <and> condition in " + file);
		}
		return cond;
	}
	
	protected Condition parseLogicOr(Node n, Object template)
	{
		final ConditionLogicOr cond = new ConditionLogicOr();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		if ((cond._conditions == null) || (cond._conditions.length == 0))
		{
			_log.severe("Empty <or> condition in " + file);
		}
		return cond;
	}
	
	protected Condition parseLogicNot(Node n, Object template)
	{
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				return new ConditionLogicNot(parseCondition(n, template));
			}
		}
		_log.severe("Empty <not> condition in " + file);
		return null;
	}
	
	protected Condition parsePlayerCondition(Node n)
	{
		Condition cond = null;
		final int[] ElementSeeds = new int[5];
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("race".equalsIgnoreCase(a.getNodeName()))
			{
				final Race race = Race.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerRace(race));
			}
			else if ("level".equalsIgnoreCase(a.getNodeName()))
			{
				final int lvl = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerLevel(lvl));
			}
			else if ("resting".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RESTING, val));
			}
			else if ("flying".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.FLYING, val));
			}
			else if ("moving".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.MOVING, val));
			}
			else if ("running".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RUNNING, val));
			}
			else if ("behind".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.BEHIND, val));
			}
			else if ("front".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.FRONT, val));
			}
			else if ("hp".equalsIgnoreCase(a.getNodeName()))
			{
				final int hp = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionPlayerHp(hp));
			}
			else if ("hprate".equalsIgnoreCase(a.getNodeName()))
			{
				final double rate = getNumber(a.getNodeValue(), null).doubleValue();
				cond = joinAnd(cond, new ConditionPlayerHpPercentage(rate));
			}
			else if ("seed_fire".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[0] = getNumber(a.getNodeValue(), null).intValue();
			}
			else if ("seed_water".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[1] = getNumber(a.getNodeValue(), null).intValue();
			}
			else if ("seed_wind".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[2] = getNumber(a.getNodeValue(), null).intValue();
			}
			else if ("seed_various".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[3] = getNumber(a.getNodeValue(), null).intValue();
			}
			else if ("seed_any".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[4] = getNumber(a.getNodeValue(), null).intValue();
			}
		}
		
		// Elemental seed condition processing
		for (final int elementSeed : ElementSeeds)
		{
			if (elementSeed > 0)
			{
				cond = joinAnd(cond, new ConditionElementSeed(ElementSeeds));
				break;
			}
		}
		
		if (cond == null)
		{
			_log.severe("Unrecognized <player> condition in " + file);
		}
		return cond;
	}
	
	protected Condition parseTargetCondition(Node n, Object template)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("aggro".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetAggro(val));
			}
			else if ("level".equalsIgnoreCase(a.getNodeName()))
			{
				final int lvl = getNumber(a.getNodeValue(), template).intValue();
				cond = joinAnd(cond, new ConditionTargetLevel(lvl));
			}
			else if ("using".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					for (final L2WeaponType wt : L2WeaponType.values())
					{
						if (wt.toString().equals(item))
						{
							mask |= wt.mask();
							break;
						}
					}
					for (final L2ArmorType at : L2ArmorType.values())
					{
						if (at.toString().equals(item))
						{
							mask |= at.mask();
							break;
						}
					}
				}
				cond = joinAnd(cond, new ConditionTargetUsesWeaponKind(mask));
			}
		}
		if (cond == null)
		{
			_log.severe("Unrecognized <target> condition in " + file);
		}
		return cond;
	}
	
	protected Condition parseSkillCondition(Node n)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final Stats stat = Stats.valueOfXml(attrs.getNamedItem("stat").getNodeValue());
		return new ConditionSkillStats(stat);
	}
	
	protected Condition parseUsingCondition(Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("kind".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					for (final L2WeaponType wt : L2WeaponType.values())
					{
						if (wt.toString().equals(item))
						{
							mask |= wt.mask();
							break;
						}
					}
					for (final L2ArmorType at : L2ArmorType.values())
					{
						if (at.toString().equals(item))
						{
							mask |= at.mask();
							break;
						}
					}
				}
				cond = joinAnd(cond, new ConditionUsingItemType(mask));
			}
			else if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				final int id = Integer.parseInt(a.getNodeValue());
				cond = joinAnd(cond, new ConditionUsingSkill(id));
			}
			else if ("slotitem".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
				final int id = Integer.parseInt(st.nextToken().trim());
				final int slot = Integer.parseInt(st.nextToken().trim());
				int enchant = 0;
				if (st.hasMoreTokens())
				{
					enchant = Integer.parseInt(st.nextToken().trim());
				}
				cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
			}
		}
		if (cond == null)
		{
			_log.severe("Unrecognized <using> condition in " + file);
		}
		return cond;
	}
	
	protected Condition parseGameCondition(Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionWithSkill(val));
			}
			if ("night".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionGameTime(CheckGameTime.NIGHT, val));
			}
			if ("chance".equalsIgnoreCase(a.getNodeName()))
			{
				final int val = getNumber(a.getNodeValue(), null).intValue();
				cond = joinAnd(cond, new ConditionGameChance(val));
			}
		}
		if (cond == null)
		{
			_log.severe("Unrecognized <game> condition in " + file);
		}
		return cond;
	}
	
	protected void parseTable(Node n)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final String name = attrs.getNamedItem("name").getNodeValue();
		if (name.charAt(0) != '#')
		{
			throw new IllegalArgumentException("Table name must start with #");
		}
		final StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
		final List<String> array = new FastList<>();
		while (data.hasMoreTokens())
		{
			array.add(data.nextToken());
		}
		final Number[] res = new Number[array.size()];
		for (int i = 0; i < array.size(); i++)
		{
			res[i] = getNumber(array.get(i), null);
		}
		setTable(name, res);
	}
	
	protected void parseBeanSet(Node n, StatsSet set, Integer level)
	{
		final String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		final String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		final char ch = value.length() == 0 ? ' ' : value.charAt(0);
		if ((ch == '#') || (ch == '-') || Character.isDigit(ch))
		{
			set.set(name, String.valueOf(getNumber(value, level)));
		}
		else
		{
			set.set(name, value);
		}
	}
	
	protected Lambda getLambda(Node n, Object template)
	{
		final Node nval = n.getAttributes().getNamedItem("val");
		if (nval != null)
		{
			final String val = nval.getNodeValue();
			if (val.charAt(0) == '#')
			{ // table by level
				return new LambdaConst(getTableValue(val).doubleValue());
			}
			else if (val.charAt(0) == '$')
			{
				if (val.equalsIgnoreCase("$player_level"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_LEVEL);
				}
				if (val.equalsIgnoreCase("$target_level"))
				{
					return new LambdaStats(LambdaStats.StatsType.TARGET_LEVEL);
				}
				if (val.equalsIgnoreCase("$player_max_hp"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_HP);
				}
				if (val.equalsIgnoreCase("$player_max_mp"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_MP);
				}
				// try to find value out of item fields
				final StatsSet set = getStatsSet();
				final String field = set.getString(val.substring(1));
				if (field != null)
				{
					return new LambdaConst(getNumber(field, template).doubleValue());
				}
				// failed
				throw new IllegalArgumentException("Unknown value " + val);
			}
			else
			{
				return new LambdaConst(Double.parseDouble(val));
			}
		}
		final LambdaCalc calc = new LambdaCalc();
		n = n.getFirstChild();
		while ((n != null) && (n.getNodeType() != Node.ELEMENT_NODE))
		{
			n = n.getNextSibling();
		}
		if ((n == null) || !"val".equals(n.getNodeName()))
		{
			throw new IllegalArgumentException("Value not specified");
		}
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			attachLambdaFunc(n, template, calc);
		}
		return calc;
	}
	
	protected Number getNumber(String value, Object template)
	{
		if (value.charAt(0) == '#')
		{// table by level
			if (template instanceof L2Skill)
			{
				return getTableValue(value);
			}
			else if (template instanceof Integer)
			{
				return getTableValue(value, ((Integer) template).intValue());
			}
			else
			{
				throw new IllegalStateException();
			}
		}
		if (value.indexOf('.') == -1)
		{
			int radix = 10;
			if ((value.length() > 2) && value.substring(0, 2).equalsIgnoreCase("0x"))
			{
				value = value.substring(2);
				radix = 16;
			}
			return Integer.valueOf(value, radix);
		}
		return Double.valueOf(value);
	}
	
	protected Condition joinAnd(Condition cond, Condition c)
	{
		if (cond == null)
		{
			return c;
		}
		if (cond instanceof ConditionLogicAnd)
		{
			((ConditionLogicAnd) cond).add(c);
			return cond;
		}
		final ConditionLogicAnd and = new ConditionLogicAnd();
		and.add(cond);
		and.add(c);
		return and;
	}
}