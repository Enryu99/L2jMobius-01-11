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

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.interfaces.IIdentifiable;
import com.l2jmobius.gameserver.network.serverpackets.AllyCrest;
import com.l2jmobius.gameserver.network.serverpackets.ExPledgeEmblem;
import com.l2jmobius.gameserver.network.serverpackets.PledgeCrest;

/**
 * @author NosBit
 */
public final class L2Crest implements IIdentifiable
{
	public enum CrestType
	{
		PLEDGE(1),
		PLEDGE_LARGE(2),
		ALLY(3);
		
		private final int _id;
		
		CrestType(int id)
		{
			_id = id;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public static CrestType getById(int id)
		{
			for (CrestType crestType : values())
			{
				if (crestType.getId() == id)
				{
					return crestType;
				}
			}
			return null;
		}
	}
	
	private final int _id;
	private final byte[] _data;
	private final CrestType _type;
	
	public L2Crest(int id, byte[] data, CrestType type)
	{
		_id = id;
		_data = data;
		_type = type;
	}
	
	@Override
	public int getId()
	{
		return _id;
	}
	
	public byte[] getData()
	{
		return _data;
	}
	
	public CrestType getType()
	{
		return _type;
	}
	
	/**
	 * Gets the client path to crest for use in html and sends the crest to {@code L2PcInstance}
	 * @param activeChar the @{code L2PcInstance} where html is send to.
	 * @return the client path to crest
	 */
	public String getClientPath(L2PcInstance activeChar)
	{
		String path = null;
		switch (getType())
		{
			case PLEDGE:
			{
				activeChar.sendPacket(new PledgeCrest(getId(), getData()));
				path = "Crest.crest_" + Config.SERVER_ID + "_" + getId();
				break;
			}
			case PLEDGE_LARGE:
			{
				final byte[] data = getData();
				if (data != null)
				{
					for (int i = 0; i <= 4; i++)
					{
						if (i < 4)
						{
							final byte[] fullChunk = new byte[14336];
							System.arraycopy(data, (14336 * i), fullChunk, 0, 14336);
							activeChar.sendPacket(new ExPledgeEmblem(getId(), fullChunk, 0, i));
						}
						else
						{
							final byte[] lastChunk = new byte[8320];
							System.arraycopy(data, (14336 * i), lastChunk, 0, 8320);
							activeChar.sendPacket(new ExPledgeEmblem(getId(), lastChunk, 0, i));
						}
					}
				}
				path = "Crest.crest_" + Config.SERVER_ID + "_" + getId() + "_l";
				break;
			}
			case ALLY:
			{
				activeChar.sendPacket(new AllyCrest(getId(), getData()));
				path = "Crest.crest_" + Config.SERVER_ID + "_" + getId();
				break;
			}
		}
		return path;
	}
}