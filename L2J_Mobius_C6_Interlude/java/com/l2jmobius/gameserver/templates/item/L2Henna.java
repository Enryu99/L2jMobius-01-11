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
package com.l2jmobius.gameserver.templates.item;

import com.l2jmobius.gameserver.templates.StatsSet;

/**
 * This class ...
 * @version $Revision$ $Date$
 */
public class L2Henna
{
	public final int symbolId;
	public final String symbolName;
	public final int dye;
	public final int price;
	public final int amount;
	public final int statINT;
	public final int statSTR;
	public final int statCON;
	public final int statMEM;
	public final int statDEX;
	public final int statWIT;
	
	public L2Henna(StatsSet set)
	{
		symbolId = set.getInteger("symbol_id");
		symbolName = ""; // set.getString("symbol_name");
		dye = set.getInteger("dye");
		price = set.getInteger("price");
		amount = set.getInteger("amount");
		statINT = set.getInteger("stat_INT");
		statSTR = set.getInteger("stat_STR");
		statCON = set.getInteger("stat_CON");
		statMEM = set.getInteger("stat_MEM");
		statDEX = set.getInteger("stat_DEX");
		statWIT = set.getInteger("stat_WIT");
	}
	
	public int getSymbolId()
	{
		return symbolId;
	}
	
	/**
	 * @return
	 */
	public int getDyeId()
	{
		return dye;
	}
	
	/**
	 * @return
	 */
	public int getPrice()
	{
		return price;
	}
	
	/**
	 * @return
	 */
	public int getAmountDyeRequire()
	{
		return amount;
	}
	
	/**
	 * @return
	 */
	public int getStatINT()
	{
		return statINT;
	}
	
	/**
	 * @return
	 */
	public int getStatSTR()
	{
		return statSTR;
	}
	
	/**
	 * @return
	 */
	public int getStatCON()
	{
		return statCON;
	}
	
	/**
	 * @return
	 */
	public int getStatMEM()
	{
		return statMEM;
	}
	
	/**
	 * @return
	 */
	public int getStatDEX()
	{
		return statDEX;
	}
	
	/**
	 * @return
	 */
	public int getStatWIT()
	{
		return statWIT;
	}
	/**
	 * @return
	 */
}
