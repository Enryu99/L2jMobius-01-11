/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.l2jmobius.gameserver.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.templates.CharTemplate;

public class CharTemplateTable
{
	private static Logger _log = Logger.getLogger(CharTemplateTable.class.getName());
	private static CharTemplateTable _instance;
	private final Map<Integer, CharTemplate> _templates = new HashMap<>();
	
	public static CharTemplateTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new CharTemplateTable();
		}
		return _instance;
	}
	
	private CharTemplateTable()
	{
		try
		{
			final File charTemplateData = new File("data/char_templates.csv");
			if (charTemplateData.isFile() && charTemplateData.exists())
			{
				final LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(charTemplateData)));
				String line = null;
				while ((line = lnr.readLine()) != null)
				{
					if (line.trim().isEmpty() || line.startsWith("#"))
					{
						continue;
					}
					final CharTemplate ct = new CharTemplate();
					final StringTokenizer st = new StringTokenizer(line, ";");
					ct.setClassId(Integer.parseInt(st.nextToken()));
					ct.setClassName(st.nextToken());
					ct.setRaceId(Integer.parseInt(st.nextToken()));
					ct.setStr(Integer.parseInt(st.nextToken()));
					ct.setCon(Integer.parseInt(st.nextToken()));
					ct.setDex(Integer.parseInt(st.nextToken()));
					ct.setInt(Integer.parseInt(st.nextToken()));
					ct.setWit(Integer.parseInt(st.nextToken()));
					ct.setMen(Integer.parseInt(st.nextToken()));
					ct.setHp(Integer.parseInt(st.nextToken()));
					ct.setMp(Integer.parseInt(st.nextToken()));
					ct.setPatk(Integer.parseInt(st.nextToken()));
					ct.setPdef(Integer.parseInt(st.nextToken()));
					ct.setMatk(Integer.parseInt(st.nextToken()));
					ct.setMdef(Integer.parseInt(st.nextToken()));
					ct.setPspd(Integer.parseInt(st.nextToken()));
					ct.setMspd(Integer.parseInt(st.nextToken()));
					ct.setAcc(Integer.parseInt(st.nextToken()));
					ct.setCrit(Integer.parseInt(st.nextToken()));
					ct.setEvas(Integer.parseInt(st.nextToken()));
					ct.setMoveSpd(Integer.parseInt(st.nextToken()));
					ct.setLoad(Integer.parseInt(st.nextToken()));
					ct.setX(Integer.parseInt(st.nextToken()));
					ct.setY(Integer.parseInt(st.nextToken()));
					ct.setZ(Integer.parseInt(st.nextToken()));
					ct.setCanCraft(Integer.parseInt(st.nextToken()));
					ct.setMUnk1(Double.parseDouble(st.nextToken()));
					ct.setMUnk2(Double.parseDouble(st.nextToken()));
					ct.setMColR(Double.parseDouble(st.nextToken()));
					ct.setMColH(Double.parseDouble(st.nextToken()));
					ct.setFUnk1(Double.parseDouble(st.nextToken()));
					ct.setFUnk2(Double.parseDouble(st.nextToken()));
					ct.setFColR(Double.parseDouble(st.nextToken()));
					ct.setFColH(Double.parseDouble(st.nextToken()));
					while (st.hasMoreTokens())
					{
						ct.addItem(Integer.parseInt(st.nextToken()));
					}
					_templates.put(ct.getClassId(), ct);
				}
				lnr.close();
				_log.config("Loaded " + _templates.size() + " char templates.");
			}
			else
			{
				_log.warning("char_templates.csv is missing in data folder, char creation will fail.");
			}
		}
		catch (Exception e)
		{
			_log.warning("Error while loading char templates " + e);
		}
	}
	
	public CharTemplate getTemplate(int classId)
	{
		return _templates.get(classId);
	}
	
	public Collection<CharTemplate> getAllTemplates()
	{
		return _templates.values();
	}
}
