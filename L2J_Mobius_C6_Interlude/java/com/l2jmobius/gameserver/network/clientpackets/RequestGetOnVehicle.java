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
package com.l2jmobius.gameserver.network.clientpackets;

import com.l2jmobius.commons.util.Point3D;
import com.l2jmobius.gameserver.instancemanager.BoatManager;
import com.l2jmobius.gameserver.model.actor.instance.BoatInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.network.serverpackets.GetOnVehicle;

public final class RequestGetOnVehicle extends GameClientPacket
{
	private int _id;
	private int _x;
	private int _y;
	private int _z;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		
		if (player == null)
		{
			return;
		}
		
		final BoatInstance boat = BoatManager.getInstance().GetBoat(_id);
		if (boat == null)
		{
			return;
		}
		
		final GetOnVehicle Gon = new GetOnVehicle(player, boat, _x, _y, _z);
		player.setInBoatPosition(new Point3D(_x, _y, _z));
		player.getPosition().setXYZ(boat.getPosition().getX(), boat.getPosition().getY(), boat.getPosition().getZ());
		player.broadcastPacket(Gon);
		player.revalidateZone(true);
	}
}
