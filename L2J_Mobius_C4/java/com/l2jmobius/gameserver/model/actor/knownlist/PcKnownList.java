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
package com.l2jmobius.gameserver.model.actor.knownlist;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2BoatInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PetInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2StaticObjectInstance;
import com.l2jmobius.gameserver.network.serverpackets.CharInfo;
import com.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import com.l2jmobius.gameserver.network.serverpackets.DoorInfo;
import com.l2jmobius.gameserver.network.serverpackets.DoorStatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.DropItem;
import com.l2jmobius.gameserver.network.serverpackets.GetOnVehicle;
import com.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import com.l2jmobius.gameserver.network.serverpackets.PetInfo;
import com.l2jmobius.gameserver.network.serverpackets.PetItemList;
import com.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import com.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import com.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import com.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import com.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import com.l2jmobius.gameserver.network.serverpackets.SpawnItem;
import com.l2jmobius.gameserver.network.serverpackets.SpawnItemPoly;
import com.l2jmobius.gameserver.network.serverpackets.StaticObject;

public class PcKnownList extends PlayableKnownList
{
	// =========================================================
	// Data Field
	
	// =========================================================
	// Constructor
	public PcKnownList(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	// =========================================================
	// Method - Public
	/**
	 * Add a visible L2Object to L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packets needed to inform the L2PcInstance of its state and actions in progress.<BR>
	 * <BR>
	 * <B><U> object is a L2ItemInstance </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet DropItem/SpawnItem to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2DoorInstance </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packets DoorInfo and DoorStatusUpdate to the L2PcInstance</li>
	 * <li>Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2NpcInstance </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet NpcInfo to the L2PcInstance</li>
	 * <li>Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2Summon </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet NpcInfo/PetItemList (if the L2PcInstance is the owner) to the L2PcInstance</li>
	 * <li>Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2PcInstance </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet CharInfo to the L2PcInstance</li>
	 * <li>If the object has a private store, Send Server-Client Packet PrivateStoreMsgSell to the L2PcInstance</li>
	 * <li>Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * @param object The L2Object to add to _knownObjects and _knownPlayer
	 */
	@Override
	public boolean addKnownObject(L2Object object)
	{
		return addKnownObject(object, null);
	}
	
	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if (!super.addKnownObject(object, dropper))
		{
			return false;
		}
		
		if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
		{
			// if (object.getPolytype().equals("item"))
			getActiveChar().sendPacket(new SpawnItemPoly(object));
			// else if (object.getPolytype().equals("npc"))
			// sendPacket(new NpcInfoPoly(object, this));
			
		}
		else
		{
			if (object instanceof L2ItemInstance)
			{
				if (dropper != null)
				{
					getActiveChar().sendPacket(new DropItem((L2ItemInstance) object, dropper.getObjectId()));
				}
				else
				{
					getActiveChar().sendPacket(new SpawnItem((L2ItemInstance) object));
				}
			}
			else if (object instanceof L2DoorInstance)
			{
				getActiveChar().sendPacket(new DoorInfo((L2DoorInstance) object));
				getActiveChar().sendPacket(new DoorStatusUpdate((L2DoorInstance) object));
			}
			else if (object instanceof L2BoatInstance)
			{
				((L2BoatInstance) object).sendBoatInfo(getActiveChar());
			}
			else if (object instanceof L2StaticObjectInstance)
			{
				getActiveChar().sendPacket(new StaticObject((L2StaticObjectInstance) object));
			}
			else if (object instanceof L2NpcInstance)
			{
				if (Config.CHECK_KNOWN)
				{
					getActiveChar().sendMessage("Added NPC: " + ((L2NpcInstance) object).getName());
				}
				if (((L2NpcInstance) object).getRunSpeed() == 0)
				{
					getActiveChar().sendPacket(new ServerObjectInfo((L2NpcInstance) object, getActiveChar()));
				}
				else
				{
					getActiveChar().sendPacket(new NpcInfo((L2NpcInstance) object, getActiveChar()));
				}
			}
			else if (object instanceof L2Summon)
			{
				final L2Summon summon = (L2Summon) object;
				
				// Check if the L2PcInstance is the owner of the Pet
				if (getActiveChar().equals(summon.getOwner()))
				{
					getActiveChar().sendPacket(new PetInfo(summon, 0));
					summon.updateEffectIcons(true);
					if (summon instanceof L2PetInstance)
					{
						getActiveChar().sendPacket(new PetItemList((L2PetInstance) summon));
					}
				}
				else
				{
					getActiveChar().sendPacket(new NpcInfo(summon, getActiveChar(), 0));
				}
			}
			else if (object instanceof L2PcInstance)
			{
				final L2PcInstance otherPlayer = (L2PcInstance) object;
				if (otherPlayer.isInBoat())
				{
					otherPlayer.getPosition().setWorldPosition(otherPlayer.getBoat().getPosition().getWorldPosition());
					getActiveChar().sendPacket(new CharInfo(otherPlayer));
					final int relation1 = otherPlayer.getRelation(getActiveChar());
					final int relation2 = getActiveChar().getRelation(otherPlayer);
					if ((otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != null) && (otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != relation1))
					{
						getActiveChar().sendPacket(new RelationChanged(otherPlayer, relation1, getActiveChar().isAutoAttackable(otherPlayer)));
						if (otherPlayer.getPet() != null)
						{
							getActiveChar().sendPacket(new RelationChanged(otherPlayer.getPet(), relation1, getActiveChar().isAutoAttackable(otherPlayer)));
						}
					}
					
					if ((getActiveChar().getKnownList().getKnownRelations().get(otherPlayer.getObjectId()) != null) && (getActiveChar().getKnownList().getKnownRelations().get(otherPlayer.getObjectId()) != relation2))
					{
						otherPlayer.sendPacket(new RelationChanged(getActiveChar(), relation2, otherPlayer.isAutoAttackable(getActiveChar())));
						if (getActiveChar().getPet() != null)
						{
							otherPlayer.sendPacket(new RelationChanged(getActiveChar().getPet(), relation2, otherPlayer.isAutoAttackable(getActiveChar())));
						}
					}
					getActiveChar().sendPacket(new GetOnVehicle(otherPlayer.getObjectId(), otherPlayer.getBoat().getObjectId(), otherPlayer.getInBoatPosition()));
					
				}
				else
				{
					getActiveChar().sendPacket(new CharInfo(otherPlayer));
					final int relation1 = otherPlayer.getRelation(getActiveChar());
					final int relation2 = getActiveChar().getRelation(otherPlayer);
					if ((otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != null) && (otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != relation1))
					{
						getActiveChar().sendPacket(new RelationChanged(otherPlayer, relation1, getActiveChar().isAutoAttackable(otherPlayer)));
						if (otherPlayer.getPet() != null)
						{
							getActiveChar().sendPacket(new RelationChanged(otherPlayer.getPet(), relation1, getActiveChar().isAutoAttackable(otherPlayer)));
						}
					}
					
					if ((getActiveChar().getKnownList().getKnownRelations().get(otherPlayer.getObjectId()) != null) && (getActiveChar().getKnownList().getKnownRelations().get(otherPlayer.getObjectId()) != relation2))
					{
						otherPlayer.sendPacket(new RelationChanged(getActiveChar(), relation2, otherPlayer.isAutoAttackable(getActiveChar())));
						if (getActiveChar().getPet() != null)
						{
							otherPlayer.sendPacket(new RelationChanged(getActiveChar().getPet(), relation2, otherPlayer.isAutoAttackable(getActiveChar())));
						}
					}
				}
				
				if ((otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL) || (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL))
				{
					getActiveChar().sendPacket(new PrivateStoreMsgSell(otherPlayer));
				}
				else if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY)
				{
					getActiveChar().sendPacket(new PrivateStoreMsgBuy(otherPlayer));
				}
				else if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE)
				{
					getActiveChar().sendPacket(new RecipeShopMsg(otherPlayer));
				}
			}
			
			if (object instanceof L2Character)
			{
				// Update the state of the L2Character object client side by sending Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance
				final L2Character obj = (L2Character) object;
				if (obj.getAI() != null)
				{
					obj.getAI().describeStateToPlayer(getActiveChar());
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Remove a L2Object from L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packet DeleteObject to the L2PcInstance.<BR>
	 * <BR>
	 * @param object The L2Object to remove from _knownObjects and _knownPlayer
	 */
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
		{
			return false;
		}
		
		// Send Server-Client Packet DeleteObject to the L2PcInstance
		getActiveChar().sendPacket(new DeleteObject(object));
		if (Config.CHECK_KNOWN && (object instanceof L2NpcInstance))
		{
			getActiveChar().sendMessage("Removed NPC: " + ((L2NpcInstance) object).getName());
		}
		return true;
	}
	
	// =========================================================
	// Method - Private
	
	// =========================================================
	// Property - Public
	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}
	
	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		// when knownlist grows, the distance to forget should be at least
		// the same as the previous watch range, or it becomes possible that
		// extra charinfo packets are being sent (watch-forget-watch-forget)
		final int knownlistSize = getKnownObjects().size();
		if (knownlistSize <= 25)
		{
			return 4000;
		}
		if (knownlistSize <= 35)
		{
			return 3500;
		}
		if (knownlistSize <= 70)
		{
			return 2910;
		}
		return 2310;
	}
	
	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		final int knownlistSize = getKnownObjects().size();
		if (knownlistSize <= 25)
		{
			return 3400; // empty field
		}
		if (knownlistSize <= 35)
		{
			return 2900;
		}
		if (knownlistSize <= 70)
		{
			return 2300;
		}
		return 1700; // Siege, TOI, city
	}
}