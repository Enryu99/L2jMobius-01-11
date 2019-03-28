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
package com.l2jmobius.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.zone.type.OlympiadStadiumZone;

public class OlympiadStadiaManager
{
	protected static Logger LOGGER = Logger.getLogger(OlympiadStadiaManager.class.getName());
	
	private static OlympiadStadiaManager _instance;
	
	public static final OlympiadStadiaManager getInstance()
	{
		if (_instance == null)
		{
			LOGGER.info("Initializing OlympiadStadiaManager");
			_instance = new OlympiadStadiaManager();
		}
		return _instance;
	}
	
	private List<OlympiadStadiumZone> _olympiadStadias;
	
	public OlympiadStadiaManager()
	{
	}
	
	public void addStadium(OlympiadStadiumZone arena)
	{
		if (_olympiadStadias == null)
		{
			_olympiadStadias = new ArrayList<>();
		}
		
		_olympiadStadias.add(arena);
	}
	
	public final OlympiadStadiumZone getStadium(Creature creature)
	{
		for (OlympiadStadiumZone temp : _olympiadStadias)
		{
			if (temp.isCharacterInZone(creature))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public final OlympiadStadiumZone getStadiumByLoc(int x, int y, int z)
	{
		if (_olympiadStadias != null)
		{
			for (OlympiadStadiumZone temp : _olympiadStadias)
			{
				if (temp.isInsideZone(x, y, z))
				{
					return temp;
				}
			}
		}
		
		return null;
	}
}
