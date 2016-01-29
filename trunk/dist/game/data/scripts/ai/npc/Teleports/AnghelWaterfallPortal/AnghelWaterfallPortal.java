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
package ai.npc.Teleports.AnghelWaterfallPortal;

import com.l2jmobius.gameserver.model.Location;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.zone.L2ZoneType;

import ai.npc.AbstractNpcAI;

/**
 * Anghel Waterfall Portal teleport AI.
 * @author Mobius
 */
final class AnghelWaterfallPortal extends AbstractNpcAI
{
	private static final int ZONE_ID = 200200;
	private static final Location TELEPORT_LOC = new Location(207559, 86429, -1000);
	
	private AnghelWaterfallPortal()
	{
		super(AnghelWaterfallPortal.class.getSimpleName(), "ai/npc/Teleports");
		addEnterZoneId(ZONE_ID);
	}
	
	@Override
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		if (character.isPlayer())
		{
			character.teleToLocation(TELEPORT_LOC);
		}
		return super.onEnterZone(character, zone);
	}
	
	public static void main(String[] args)
	{
		new AnghelWaterfallPortal();
	}
}