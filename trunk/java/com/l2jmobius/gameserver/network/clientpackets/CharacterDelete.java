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

import java.util.logging.Level;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.CharSelectInfoPackage;
import com.l2jmobius.gameserver.model.events.Containers;
import com.l2jmobius.gameserver.model.events.EventDispatcher;
import com.l2jmobius.gameserver.model.events.impl.character.player.OnPlayerDelete;
import com.l2jmobius.gameserver.network.serverpackets.CharDeleteFail;
import com.l2jmobius.gameserver.network.serverpackets.CharDeleteSuccess;
import com.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;

/**
 * This class ...
 * @version $Revision: 1.8.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class CharacterDelete extends L2GameClientPacket
{
	private static final String _C__0C_CHARACTERDELETE = "[C] 0D CharacterDelete";
	
	// cd
	private int _charSlot;
	
	@Override
	protected void readImpl()
	{
		_charSlot = readD();
	}
	
	@Override
	protected void runImpl()
	{
		// if (!getClient().getFloodProtectors().getCharacterSelect().tryPerformAction("CharacterDelete"))
		// {
		// sendPacket(new CharDeleteFail(CharDeleteFail.REASON_DELETION_FAILED));
		// return;
		// }
		
		if (Config.DEBUG)
		{
			_log.fine("deleting slot:" + _charSlot);
		}
		
		try
		{
			final byte answer = getClient().markToDeleteChar(_charSlot);
			
			switch (answer)
			{
				default:
				case -1: // Error
				{
					break;
				}
				case 0: // Success!
				{
					sendPacket(new CharDeleteSuccess());
					final CharSelectInfoPackage charInfo = getClient().getCharSelection(_charSlot);
					EventDispatcher.getInstance().notifyEvent(new OnPlayerDelete(charInfo.getObjectId(), charInfo.getName(), getClient()), Containers.Players());
					break;
				}
				case 1:
				{
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER));
					break;
				}
				case 2:
				{
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED));
					break;
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error:", e);
		}
		
		final CharSelectionInfo cl = new CharSelectionInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
	
	@Override
	public String getType()
	{
		return _C__0C_CHARACTERDELETE;
	}
}
