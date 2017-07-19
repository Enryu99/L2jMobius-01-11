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
package com.l2jmobius.gameserver.network.serverpackets;

/**
 * format d rev 417
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class AuthLoginFail extends L2GameServerPacket
{
	private static final String _S__12_AUTHLOGINFAIL = "[S] 14 AuthLoginFail";
	public static int NO_TEXT = 0;
	public static int SYSTEM_ERROR_LOGIN_LATER = 1;
	public static int PASSWORD_DOES_NOT_MATCH_THIS_ACCOUNT = 2;
	public static int PASSWORD_DOES_NOT_MATCH_THIS_ACCOUNT2 = 3;
	public static int ACCESS_FAILED_TRY_LATER = 4;
	public static int INCORRECT_ACCOUNT_INFO_CONTACT_CUSTOMER_SUPPORT = 5;
	public static int ACCESS_FAILED_TRY_LATER2 = 6;
	public static int ACOUNT_ALREADY_IN_USE = 7;
	public static int ACCESS_FAILED_TRY_LATER3 = 8;
	public static int ACCESS_FAILED_TRY_LATER4 = 9;
	public static int ACCESS_FAILED_TRY_LATER5 = 10;
	
	private final int _reason;
	
	/**
	 * @param reason
	 */
	public AuthLoginFail(int reason)
	{
		_reason = reason;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x14);
		writeD(_reason);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__12_AUTHLOGINFAIL;
	}
}