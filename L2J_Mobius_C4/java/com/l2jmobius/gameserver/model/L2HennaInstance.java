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
package com.l2jmobius.gameserver.model;

import com.l2jmobius.gameserver.templates.L2Henna;

/**
 * This class represents a Non-Player-Character in the world. it can be a monster or a friendly character. it also uses a template to fetch some static values. the templates are hardcoded in the client, so we can rely on them.
 * @version $Revision$ $Date$
 */
public class L2HennaInstance
{
	// private static Logger _log = Logger.getLogger(L2HennaInstance.class.getName());
	
	private final L2Henna _template;
	private int _symbolId;
	private int _itemIdDye;
	private int _price;
	private int _statINT;
	private int _statSTR;
	private int _statCON;
	private int _statMEN;
	private int _statDEX;
	private int _statWIT;
	private int _amountDyeRequire;
	
	public L2HennaInstance(L2Henna template)
	{
		_template = template;
		_symbolId = _template.symbol_id;
		_itemIdDye = _template.dye;
		_amountDyeRequire = _template.amount;
		_price = _template.price;
		_statINT = _template.stat_INT;
		_statSTR = _template.stat_STR;
		_statCON = _template.stat_CON;
		_statMEN = _template.stat_MEN;
		_statDEX = _template.stat_DEX;
		_statWIT = _template.stat_WIT;
	}
	
	public String getName()
	{
		String res = "";
		if (_statINT > 0)
		{
			res = res + "INT +" + _statINT;
		}
		else if (_statSTR > 0)
		{
			res = res + "STR +" + _statSTR;
		}
		else if (_statCON > 0)
		{
			res = res + "CON +" + _statCON;
		}
		else if (_statMEN > 0)
		{
			res = res + "MEN +" + _statMEN;
		}
		else if (_statDEX > 0)
		{
			res = res + "DEX +" + _statDEX;
		}
		else if (_statWIT > 0)
		{
			res = res + "WIT +" + _statWIT;
		}
		
		if (_statINT < 0)
		{
			res = res + ", INT " + _statINT;
		}
		else if (_statSTR < 0)
		{
			res = res + ", STR " + _statSTR;
		}
		else if (_statCON < 0)
		{
			res = res + ", CON " + _statCON;
		}
		else if (_statMEN < 0)
		{
			res = res + ", MEN " + _statMEN;
		}
		else if (_statDEX < 0)
		{
			res = res + ", DEX " + _statDEX;
		}
		else if (_statWIT < 0)
		{
			res = res + ", WIT " + _statWIT;
		}
		
		return res;
	}
	
	public L2Henna getTemplate()
	{
		return _template;
	}
	
	public int getSymbolId()
	{
		return _symbolId;
	}
	
	public void setSymbolId(int SymbolId)
	{
		_symbolId = SymbolId;
	}
	
	public int getItemIdDye()
	{
		return _itemIdDye;
	}
	
	public void setItemIdDye(int ItemIdDye)
	{
		_itemIdDye = ItemIdDye;
	}
	
	public int getAmountDyeRequire()
	{
		return _amountDyeRequire;
	}
	
	public void setAmountDyeRequire(int AmountDyeRequire)
	{
		_amountDyeRequire = AmountDyeRequire;
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public void setPrice(int Price)
	{
		_price = Price;
	}
	
	public int getStatINT()
	{
		return _statINT;
	}
	
	public void setStatINT(int StatINT)
	{
		_statINT = StatINT;
	}
	
	public int getStatSTR()
	{
		return _statSTR;
	}
	
	public void setStatSTR(int StatSTR)
	{
		_statSTR = StatSTR;
	}
	
	public int getStatCON()
	{
		return _statCON;
	}
	
	public void setStatCON(int StatCON)
	{
		_statCON = StatCON;
	}
	
	public int getStatMEN()
	{
		return _statMEN;
	}
	
	public void setStatMEN(int StatMEN)
	{
		_statMEN = StatMEN;
	}
	
	public int getStatDEX()
	{
		return _statDEX;
	}
	
	public void setStatDEX(int StatDEX)
	{
		_statDEX = StatDEX;
	}
	
	public int getStatWIT()
	{
		return _statWIT;
	}
	
	public void setStatWIT(int StatWIT)
	{
		_statWIT = StatWIT;
	}
	
}
