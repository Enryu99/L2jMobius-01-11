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
package handlers.communityboard;

import com.l2jmobius.gameserver.cache.HtmCache;
import com.l2jmobius.gameserver.handler.CommunityBoardHandler;
import com.l2jmobius.gameserver.handler.IWriteBoardHandler;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * Memo board.
 * @author Zoey76
 */
public class MemoBoard implements IWriteBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsmemo",
		"_bbstopics"
	};
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, L2PcInstance activeChar)
	{
		CommunityBoardHandler.getInstance().addBypass(activeChar, "Memo Command", command);
		
		final String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/memo.html");
		CommunityBoardHandler.separateAndSend(html, activeChar);
		return true;
	}
	
	@Override
	public boolean writeCommunityBoardCommand(L2PcInstance activeChar, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// TODO: Implement.
		return false;
	}
}
