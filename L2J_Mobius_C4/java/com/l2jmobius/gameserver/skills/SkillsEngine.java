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
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.Item;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.templates.L2Armor;
import com.l2jmobius.gameserver.templates.L2EtcItem;
import com.l2jmobius.gameserver.templates.L2EtcItemType;
import com.l2jmobius.gameserver.templates.L2Item;
import com.l2jmobius.gameserver.templates.L2Weapon;

import javolution.util.FastList;

/**
 * @author mkizub TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code Templates
 */
public class SkillsEngine
{
	protected static Logger _log = Logger.getLogger(SkillsEngine.class.getName());
	
	private static final SkillsEngine _instance = new SkillsEngine();
	
	private final List<File> _armorFiles = new FastList<>();
	private final List<File> _weaponFiles = new FastList<>();
	private final List<File> _etcitemFiles = new FastList<>();
	private final List<File> _skillFiles = new FastList<>();
	
	public static SkillsEngine getInstance()
	{
		return _instance;
	}
	
	private SkillsEngine()
	{
		hashFiles("data/stats/armor", _armorFiles);
		hashFiles("data/stats/weapon", _weaponFiles);
		hashFiles("data/stats/skills", _skillFiles);
	}
	
	private void hashFiles(String dirname, List<File> hash)
	{
		final File dir = new File(Config.DATAPACK_ROOT, dirname);
		
		if (!dir.exists())
		{
			_log.config("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		
		final File[] files = dir.listFiles();
		for (final File f : files)
		{
			if (f.getName().endsWith(".xml"))
			{
				if (!f.getName().startsWith("custom"))
				{
					hash.add(f);
				}
			}
		}
		final File customfile = new File(Config.DATAPACK_ROOT, dirname + "/custom.xml");
		if (customfile.exists())
		{
			hash.add(customfile);
		}
	}
	
	public List<L2Skill> loadSkills(File file)
	{
		if (file == null)
		{
			_log.config("Skill file not found.");
			return null;
		}
		final DocumentSkill doc = new DocumentSkill(file);
		doc.parse();
		return doc.getSkills();
	}
	
	public void loadAllSkills(Map<Integer, L2Skill> allSkills)
	{
		int count = 0;
		for (final File file : _skillFiles)
		{
			final List<L2Skill> s = loadSkills(file);
			if (s == null)
			{
				continue;
			}
			
			for (final L2Skill skill : s)
			{
				allSkills.put(SkillTable.getSkillHashCode(skill), skill);
				count++;
			}
		}
		_log.config("SkillsEngine: Loaded " + count + " Skill templates from XML files.");
	}
	
	public List<L2Armor> loadArmors(Map<Integer, Item> armorData)
	{
		final List<L2Armor> list = new FastList<>();
		for (final L2Item item : loadData(armorData, _armorFiles))
		{
			list.add((L2Armor) item);
		}
		
		return list;
	}
	
	public List<L2Weapon> loadWeapons(Map<Integer, Item> weaponData)
	{
		final List<L2Weapon> list = new FastList<>();
		for (final L2Item item : loadData(weaponData, _weaponFiles))
		{
			list.add((L2Weapon) item);
		}
		
		return list;
	}
	
	public List<L2EtcItem> loadItems(Map<Integer, Item> itemData)
	{
		final List<L2EtcItem> list = new FastList<>();
		for (final L2Item item : loadData(itemData, _etcitemFiles))
		{
			list.add((L2EtcItem) item);
		}
		
		if (list.size() == 0)
		{
			for (final Item item : itemData.values())
			{
				list.add(new L2EtcItem((L2EtcItemType) item.type, item.set));
			}
			
		}
		return list;
	}
	
	public List<L2Item> loadData(Map<Integer, Item> itemData, List<File> files)
	{
		final List<L2Item> list = new FastList<>();
		for (final File f : files)
		{
			final DocumentItem document = new DocumentItem(itemData, f);
			document.parse();
			list.addAll(document.getItemList());
		}
		return list;
	}
}