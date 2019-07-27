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
package org.l2jmobius.gameserver.model;

/**
 * @version $Revision: 1.3 $ $Date: 2004/10/23 22:12:44 $
 */
public class Macro
{
	public static final int CMD_TYPE_SKILL = 1;
	public static final int CMD_TYPE_ACTION = 3;
	public static final int CMD_TYPE_SHORTCUT = 4;
	
	public int id;
	public final int icon;
	public final String name;
	public final String descr;
	public final String acronym;
	public final MacroCmd[] commands;
	
	public static class MacroCmd
	{
		public final int entry;
		public final int type;
		public final int d1; // skill_id or page for shortcuts
		public final int d2; // shortcut
		public final String cmd;
		
		public MacroCmd(int pEntry, int pType, int pD1, int pD2, String pCmd)
		{
			entry = pEntry;
			type = pType;
			d1 = pD1;
			d2 = pD2;
			cmd = pCmd;
		}
	}
	
	/**
	 * @param pId
	 * @param pIcon
	 * @param pName
	 * @param pDescr
	 * @param pAcronym
	 * @param pCommands
	 */
	public Macro(int pId, int pIcon, String pName, String pDescr, String pAcronym, MacroCmd[] pCommands)
	{
		id = pId;
		icon = pIcon;
		name = pName;
		descr = pDescr;
		acronym = pAcronym;
		commands = pCommands;
	}
}
