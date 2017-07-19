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

import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.network.clientpackets.L2GameClientPacket;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class manages requests (transactions) between two L2PcInstance.
 * @author kriau
 */
public class L2Request
{
	public static final int REQUEST_TIMEOUT = 15; // in seconds
	
	protected L2PcInstance _player;
	protected L2PcInstance _partner;
	protected boolean _isRequestor;
	protected boolean _isAnswerer;
	protected L2GameClientPacket _requestPacket;
	
	public L2Request(L2PcInstance player)
	{
		_player = player;
	}
	
	protected void Clear()
	{
		_partner = null;
		_requestPacket = null;
		_isRequestor = false;
		_isAnswerer = false;
	}
	
	/**
	 * Set the L2PcInstance member of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @param partner
	 */
	private synchronized void setPartner(L2PcInstance partner)
	{
		_partner = partner;
	}
	
	/**
	 * Return the L2PcInstance member of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @return
	 */
	public L2PcInstance getPartner()
	{
		return _partner;
	}
	
	/**
	 * Set the packet incomed from requestor.<BR>
	 * <BR>
	 * @param packet
	 */
	private synchronized void setRequestPacket(L2GameClientPacket packet)
	{
		_requestPacket = packet;
	}
	
	/**
	 * Return the packet originally incomed from requestor.<BR>
	 * <BR>
	 * @return
	 */
	public L2GameClientPacket getRequestPacket()
	{
		return _requestPacket;
	}
	
	/**
	 * Checks if request can be made and in success case puts both PC on request state.<BR>
	 * <BR>
	 * @param partner
	 * @param packet
	 * @return
	 */
	public synchronized boolean setRequest(L2PcInstance partner, L2GameClientPacket packet)
	{
		
		if (partner == null)
		{
			return false;
		}
		
		if (partner.getRequest().isProcessingRequest())
		{
			SystemMessage sm = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
			sm.addString(partner.getName());
			_player.sendPacket(sm);
			sm = null;
			return false;
		}
		
		if (isProcessingRequest())
		{
			// Waiting another reply.
			_player.sendPacket(new SystemMessage(164));
			return false;
		}
		
		_partner = partner;
		_requestPacket = packet;
		setOnRequestTimer(true);
		_partner.getRequest().setPartner(_player);
		_partner.getRequest().setRequestPacket(packet);
		_partner.getRequest().setOnRequestTimer(false);
		return true;
	}
	
	private void setOnRequestTimer(boolean isRequestor)
	{
		_isRequestor = isRequestor ? true : false;
		_isAnswerer = isRequestor ? false : true;
		ThreadPoolManager.getInstance().scheduleGeneral(() -> Clear(), REQUEST_TIMEOUT * 1000);
	}
	
	/**
	 * Clears PC request state. Should be called after answer packet receive.<BR>
	 * <BR>
	 */
	public void onRequestResponse()
	{
		if (_partner != null)
		{
			_partner.getRequest().Clear();
		}
		Clear();
	}
	
	/**
	 * Return True if a transaction is in progress.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isProcessingRequest()
	{
		return _partner != null;
	}
}