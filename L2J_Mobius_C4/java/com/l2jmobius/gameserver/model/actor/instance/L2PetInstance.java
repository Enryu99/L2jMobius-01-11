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
package com.l2jmobius.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.L2DatabaseFactory;
import com.l2jmobius.gameserver.GeoData;
import com.l2jmobius.gameserver.ThreadPoolManager;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.handler.IItemHandler;
import com.l2jmobius.gameserver.handler.ItemHandler;
import com.l2jmobius.gameserver.idfactory.IdFactory;
import com.l2jmobius.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jmobius.gameserver.model.Inventory;
import com.l2jmobius.gameserver.model.L2Character;
import com.l2jmobius.gameserver.model.L2ItemInstance;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2PetData;
import com.l2jmobius.gameserver.model.L2PetDataTable;
import com.l2jmobius.gameserver.model.L2Skill;
import com.l2jmobius.gameserver.model.L2Summon;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.PcInventory;
import com.l2jmobius.gameserver.model.PetInventory;
import com.l2jmobius.gameserver.model.actor.stat.PetStat;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.ItemList;
import com.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import com.l2jmobius.gameserver.network.serverpackets.PetItemList;
import com.l2jmobius.gameserver.network.serverpackets.PetStatusShow;
import com.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import com.l2jmobius.gameserver.network.serverpackets.StopMove;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import com.l2jmobius.gameserver.skills.Stats;
import com.l2jmobius.gameserver.taskmanager.DecayTaskManager;
import com.l2jmobius.gameserver.templates.L2Item;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;
import com.l2jmobius.gameserver.templates.L2Weapon;
import com.l2jmobius.util.Rnd;

/**
 * This class ...
 * @version $Revision: 1.15.2.10.2.16 $ $Date: 2005/04/06 16:13:40 $
 */
public class L2PetInstance extends L2Summon
{
	protected static Logger _logPet = Logger.getLogger(L2PetInstance.class.getName());
	
	private int _curFed;
	private final PetInventory _inventory;
	private final int _controlItemId;
	private boolean _respawned;
	private final boolean _mountable;
	private int _maxload;
	
	private Future<?> _feedTask;
	
	private int _weapon;
	private int _armor;
	private int _jewel;
	
	private L2PetData _data;
	
	/** The Experience before the last Death Penalty */
	private long _expBeforeDeath = 0;
	private int _curWeightPenalty = 0;
	
	public final L2PetData getPetData()
	{
		if (_data == null)
		{
			_data = L2PetDataTable.getInstance().getPetData(getTemplate().npcId, getStat().getLevel());
		}
		
		return _data;
	}
	
	public final void setPetData(L2PetData value)
	{
		_data = value;
	}
	
	/**
	 * Manage Feeding Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Feed or kill the pet depending on hunger level</li>
	 * <li>If pet has food in inventory and feed level drops below 55% then consume food from inventory</li>
	 * <li>Send a broadcastStatusUpdate packet for this L2PetInstance</li><BR>
	 * <BR>
	 */
	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if ((getOwner() == null) || (getOwner().getPet() == null) || (getOwner().getPet().getObjectId() != getObjectId()))
				{
					stopFeed();
					return;
				}
				else if (getCurrentFed() > getFeedConsume())
				{
					setCurrentFed(getCurrentFed() - getFeedConsume());
				}
				else
				{
					setCurrentFed(0);
				}
				
				final int[] foodIds = L2PetDataTable.getFoodItemId(getTemplate().npcId);
				if (foodIds[0] == 0)
				{
					return;
				}
				
				L2ItemInstance food = getInventory().getItemByItemId(foodIds[0]);
				
				// use better strider food if exists
				if (L2PetDataTable.isStrider(getNpcId()))
				{
					if (getInventory().getItemByItemId(foodIds[1]) != null)
					{
						food = getInventory().getItemByItemId(foodIds[1]);
					}
				}
				if (isRunning() && isHungry())
				{
					setWalking();
				}
				else if (!isHungry() && !isRunning())
				{
					setRunning();
				}
				
				if (isHungry() && !isDead())
				{
					if (food != null)
					{
						final IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getItemId());
						if (handler != null)
						{
							final SystemMessage sm = new SystemMessage(SystemMessage.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
							sm.addItemName(food.getItemId());
							getOwner().sendPacket(sm);
							handler.useItem(L2PetInstance.this, food);
						}
					}
					else
					{
						if (getCurrentFed() == 0)
						{
							getOwner().sendPacket(new SystemMessage(SystemMessage.YOUR_PET_IS_VERY_HUNGRY));
							if (Rnd.get(100) < 30)
							{
								stopFeed();
								getOwner().sendPacket(new SystemMessage(SystemMessage.PET_LEFT_DUE_TO_HUNGER));
								
								if (Config.DEBUG)
								{
									// _log.info("Hungry pet deleted for player :" + getOwner().getName() + " Control Item Id :" + getControlItemId());
								}
								
								deleteMe(getOwner());
							}
						}
						broadcastStatusUpdate();
					}
				}
			}
			catch (final Throwable e)
			{
				if (Config.DEBUG)
				{
					_logPet.warning("Pet [#" + getObjectId() + "] a feed task error has occurred: " + e);
				}
			}
		}
	}
	
	int getFeedConsume()
	{
		if (isAttackingNow())
		{
			return getPetData().getPetFeedBattle();
		}
		return getPetData().getPetFeedNormal();
	}
	
	public static L2PetInstance spawnPet(L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		if (L2World.getInstance().getPet(owner.getObjectId()) != null)
		{
			return null;
		}
		
		final L2PetInstance pet = restore(control, template, owner);
		
		// add the pet instance to world
		if (pet != null)
		{
			pet.setTitle(owner.getName());
			L2World.getInstance().addPet(owner.getObjectId(), pet);
		}
		
		return pet;
	}
	
	public L2PetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		super(objectId, template, owner);
		getStat();
		
		_controlItemId = control.getObjectId();
		
		// Pet's initial level is supposed to be read from DB
		// Pets start at :
		// Wolf : Level 15
		// Hatcling : Level 35
		// Sin-eaters are defaulted at the owner's level
		// Tested and confirmed on official servers
		if (template.npcId == 12564)
		{
			getStat().setLevel((byte) getOwner().getLevel());
		}
		else
		{
			getStat().setLevel(template.level);
		}
		
		_inventory = new PetInventory(this);
		_inventory.restore();
		
		final int npcId = template.npcId;
		_mountable = L2PetDataTable.isMountable(npcId);
		_maxload = getPetData().getPetMaxLoad();
	}
	
	@Override
	public PetStat getStat()
	{
		if ((super.getStat() == null) || !(super.getStat() instanceof PetStat))
		{
			setStat(new PetStat(this));
		}
		return (PetStat) super.getStat();
	}
	
	@Override
	public double getLevelMod()
	{
		return ((100.0 - 11) + getLevel()) / 100.0;
	}
	
	public boolean isRespawned()
	{
		return _respawned;
	}
	
	@Override
	public int getSummonType()
	{
		return 2;
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		final boolean isOwner = player.getObjectId() == getOwner().getObjectId();
		
		if (isOwner && (player != getOwner()))
		{
			updateRefOwner(player);
		}
		
		if (player.getTarget() != this)
		{
			if (Config.DEBUG)
			{
				_log.fine("new target selected:" + getObjectId());
			}
			
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			final MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
		}
		else
		{
			// Check if the pet is attackable (without a forced attack) and isn't dead
			if (isAutoAttackable(player) && !isOwner)
			{
				if (Config.GEODATA > 0)
				{
					if (GeoData.getInstance().canSeeTarget(player, this))
					{
						// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						player.onActionRequest();
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					player.onActionRequest();
				}
			}
			else if (!isInsideRadius(player, 150, false, false))
			{
				if (Config.GEODATA > 0)
				{
					if (GeoData.getInstance().canSeeTarget(player, this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
						player.onActionRequest();
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
					player.onActionRequest();
				}
			}
			else
			{
				if (isOwner)
				{
					player.sendPacket(new PetStatusShow(this));
				}
			}
		}
		player.sendPacket(new ActionFailed());
	}
	
	@Override
	public int getControlItemId()
	{
		return _controlItemId;
	}
	
	public L2ItemInstance getControlItem()
	{
		return getOwner().getInventory().getItemByObjectId(_controlItemId);
	}
	
	@Override
	public int getCurrentFed()
	{
		return _curFed;
	}
	
	public void setCurrentFed(int num)
	{
		_curFed = num > getMaxFed() ? getMaxFed() : num;
	}
	
	/**
	 * Returns the pet's currently equipped weapon instance (if any).
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		for (final L2ItemInstance item : getInventory().getItems())
		{
			if ((item.getLocation() == L2ItemInstance.ItemLocation.PET_EQUIP) &&
				
				(item.getItem().getBodyPart() == L2Item.SLOT_R_HAND))
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the pet's currently equipped weapon (if any).
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		final L2ItemInstance weapon = getActiveWeaponInstance();
		
		if (weapon == null)
		{
			return null;
		}
		
		return (L2Weapon) weapon.getItem();
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// temporary? unavailable
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// temporary? unavailable
		return null;
	}
	
	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		final L2ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send Pet inventory update packet
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		getOwner().sendPacket(petIU);
		
		if (sendMessage)
		{
			
			final SystemMessage sm = new SystemMessage(SystemMessage.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			getOwner().sendPacket(sm);
		}
		return true;
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		final L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		
		// Send Pet inventory update packet
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		getOwner().sendPacket(petIU);
		
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(itemId);
			getOwner().sendPacket(sm);
		}
		
		return true;
	}
	
	@Override
	protected void doPickupItem(L2Object object)
	{
		final boolean follow = getFollowStatus();
		
		if (isDead())
		{
			return;
		}
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		final StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		
		if (Config.DEBUG)
		{
			_logPet.fine("Pet pickup pos: " + object.getX() + " " + object.getY() + " " + object.getZ());
		}
		
		broadcastPacket(sm);
		
		if (!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_logPet.warning("trying to pickup wrong target." + object);
			getOwner().sendPacket(new ActionFailed());
			return;
		}
		
		final L2ItemInstance target = (L2ItemInstance) object;
		
		synchronized (target)
		{
			if (!target.isVisible())
			{
				getOwner().sendPacket(new ActionFailed());
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(getOwner()))
			{
				getOwner().sendPacket(new ActionFailed());
				final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target.getItemId());
				getOwner().sendPacket(smsg);
				return;
			}
			
			if (!_inventory.validateCapacity(target))
			{
				getOwner().sendMessage("Your pet cannot carry any more items.");
				return;
			}
			
			if (!_inventory.validateWeight(target, target.getCount()))
			{
				getOwner().sendMessage("Your pet is overweight and cannot carry any more items.");
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getOwner().getObjectId()) && !getOwner().isInLooterParty(target.getOwnerId()))
			{
				getOwner().sendPacket(new ActionFailed());
				
				if (target.getItemId() == 57)
				{
					final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addNumber(target.getCount());
					getOwner().sendPacket(smsg);
				}
				else if (target.getCount() > 1)
				{
					final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S2_S1_s);
					smsg.addItemName(target.getItemId());
					smsg.addNumber(target.getCount());
					getOwner().sendPacket(smsg);
				}
				else
				{
					final SystemMessage smsg = new SystemMessage(SystemMessage.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target.getItemId());
					getOwner().sendPacket(smsg);
				}
				return;
				
			}
			
			if ((target.getItemLootSchedule() != null) && ((target.getOwnerId() == getOwner().getObjectId()) || getOwner().isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			target.pickupMe(this);
			
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		getInventory().addItem("Pickup", target, getOwner(), this);
		// FIXME Just send the updates if possible (old way wasn't working though)
		final PetItemList iu = new PetItemList(this);
		getOwner().sendPacket(iu);
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		if (follow)
		{
			followOwner();
		}
		
	}
	
	@Override
	public void deleteMe(L2PcInstance owner)
	{
		super.deleteMe(owner);
		destroyControlItem(owner); // this should also delete the pet from the db
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer, true))
		{
			return false;
		}
		
		stopFeed();
		getOwner().sendPacket(new SystemMessage(SystemMessage.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_20_MINS));
		DecayTaskManager.getInstance().addDecayTask(this, 1200000);
		if (!isInsideZone(ZONE_PVP) && !isInsideZone(ZONE_SIEGE))
		{
			if (Config.ALT_GAME_DELEVEL)
			{
				deathPenalty();
			}
		}
		return true;
	}
	
	@Override
	public void doRevive()
	{
		
		getOwner().removeReviving();
		
		super.doRevive();
		
		// stopDecay
		DecayTaskManager.getInstance().cancelDecayTask(this);
		startFeed();
		if (!isHungry())
		{
			setRunning();
		}
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		// Restore the pet's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}
	
	/**
	 * Transfers item to another inventory
	 * @param process : String Identifier of process triggering this action
	 * @param objectId
	 * @param count : int Quantity of items to be transfered
	 * @param target
	 * @param actor : L2PcInstance Player requesting the item transfer
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2PcInstance actor, L2Object reference)
	{
		final L2ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
		final L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);
		
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		if ((oldItem.getCount() > 0) && (oldItem != newItem))
		{
			petIU.addModifiedItem(oldItem);
		}
		else
		{
			petIU.addRemovedItem(oldItem);
		}
		getOwner().sendPacket(petIU);
		
		// Send target update packet
		if (target instanceof PcInventory)
		{
			final L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			final InventoryUpdate playerUI = new InventoryUpdate();
			if (newItem.getCount() > count)
			{
				playerUI.addModifiedItem(newItem);
			}
			else
			{
				playerUI.addNewItem(newItem);
			}
			targetPlayer.sendPacket(playerUI);
			
			// Update current load as well
			final StatusUpdate playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			petIU = new PetInventoryUpdate();
			if (newItem.getCount() > count)
			{
				petIU.addRemovedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}
		
		getInventory().refreshWeight();
		return newItem;
	}
	
	@Override
	public void giveAllToOwner()
	{
		try
		{
			final Inventory petInventory = getInventory();
			final L2ItemInstance[] items = petInventory.getItems();
			for (int i = 0; (i < items.length); i++)
			{
				final L2ItemInstance giveit = items[i];
				if (((giveit.getItem().getWeight() * giveit.getCount()) + getOwner().getInventory().getTotalWeight()) < getOwner().getMaxLoad())
				{
					// If the owner can carry it give it to him/her
					giveItemToOwner(giveit);
				}
				else
				{
					// If he/she can't carry it, drop it on the floor :)
					dropItemHere(giveit);
				}
			}
		}
		catch (final Exception e)
		{
			_logPet.warning("Give all items error " + e);
		}
	}
	
	public void giveItemToOwner(L2ItemInstance item)
	{
		try
		{
			getInventory().transferItem("PetTransfer", item.getObjectId(), item.getCount(), getOwner().getInventory(), getOwner(), this);
			final PetInventoryUpdate petiu = new PetInventoryUpdate();
			final ItemList PlayerUI = new ItemList(getOwner(), false);
			petiu.addRemovedItem(item);
			getOwner().sendPacket(petiu);
			getOwner().sendPacket(PlayerUI);
		}
		catch (final Exception e)
		{
			_logPet.warning("Error while giving item to owner: " + e);
		}
	}
	
	/**
	 * Remove the Pet from DB and its associated item from the player inventory
	 * @param owner The owner from whose invenory we should delete the item
	 */
	public void destroyControlItem(L2PcInstance owner)
	{
		// remove the pet instance from world
		L2World.getInstance().removePet(owner.getObjectId());
		
		// delete from inventory
		try
		{
			final L2ItemInstance removedItem = owner.getInventory().destroyItem("PetDestroy", getControlItemId(), 1, getOwner(), this);
			if (removedItem != null)
			{
				owner.sendPacket(new SystemMessage(SystemMessage.S1_DISAPPEARED).addItemName(removedItem.getItemId()));
				
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(removedItem);
				
				owner.sendPacket(iu);
				
				final StatusUpdate su = new StatusUpdate(owner.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, owner.getCurrentLoad());
				owner.sendPacket(su);
				
				owner.broadcastUserInfo();
				
				L2World.getInstance().removeObject(removedItem);
			}
		}
		catch (final Exception e)
		{
			_logPet.warning("Error while destroying control item: " + e);
		}
		
		// pet control item no longer exists, delete the pet from the db
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
		{
			statement.setInt(1, getControlItemId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_logPet.warning("could not delete pet:" + e);
		}
	}
	
	public void dropAllItems()
	{
		try
		{
			final L2ItemInstance[] items = getInventory().getItems();
			for (int i = 0; (i < items.length); i++)
			{
				dropItemHere(items[i]);
			}
		}
		catch (final Exception e)
		{
			_logPet.warning("Pet Drop Error: " + e);
		}
	}
	
	public void dropItemHere(L2ItemInstance dropit, boolean protect)
	{
		dropit = getInventory().dropItem("Drop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);
		if (dropit != null)
		{
			if (protect)
			{
				dropit.getDropProtection().protect(getOwner());
			}
			_logPet.finer("Item id to drop: " + dropit.getItemId() + " amount: " + dropit.getCount());
			dropit.dropMe(this, getX(), getY(), getZ() + 100);
		}
	}
	
	public void dropItemHere(L2ItemInstance dropit)
	{
		dropItemHere(dropit, false);
	}
	
	/** @return Returns the mountable. */
	@Override
	public boolean isMountable()
	{
		return _mountable;
	}
	
	private static L2PetInstance restore(L2ItemInstance control, L2NpcTemplate template, L2PcInstance owner)
	{
		L2PetInstance pet;
		if (template.type.compareToIgnoreCase("L2BabyPet") == 0)
		{
			pet = new L2BabyPetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
		}
		else
		{
			pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed, weapon, armor, jewel FROM pets WHERE item_obj_id=?"))
		{
			statement.setInt(1, control.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					pet._respawned = true;
					pet.setName(rset.getString("name"));
					
					pet.getStat().setLevel(rset.getByte("level"));
					pet.getStat().setExp(rset.getLong("exp"));
					pet.getStat().setSp(rset.getInt("sp"));
					
					pet.getStatus().setCurrentHp(rset.getDouble("curHp"));
					pet.getStatus().setCurrentMp(rset.getDouble("curMp"));
					
					pet.getStatus().setCurrentCp(pet.getMaxCp());
					
					pet.setWeapon(rset.getInt("weapon"));
					pet.setArmor(rset.getInt("armor"));
					pet.setJewel(rset.getInt("jewel"));
					
					if (rset.getDouble("curHp") < 0.5)
					{
						pet.setIsDead(true);
						pet.stopHpMpRegeneration();
					}
					
					pet.setCurrentFed(rset.getInt("fed"));
				}
			}
		}
		catch (final Exception e)
		{
			_logPet.warning("could not restore pet data: " + e);
			return null;
		}
		return pet;
	}
	
	@Override
	public void store()
	{
		if (getControlItemId() == 0)
		{
			// this is a summon, not a pet, don't store anything
			return;
		}
		
		String req;
		if (!isRespawned())
		{
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,weapon,armor,jewel,item_obj_id) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,weapon=?,armor=?,jewel=? " + "WHERE item_obj_id = ?";
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(req))
		{
			statement.setString(1, getName());
			statement.setInt(2, getStat().getLevel());
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, getStat().getExp());
			statement.setInt(6, getStat().getSp());
			statement.setInt(7, getCurrentFed());
			statement.setInt(8, getWeapon());
			statement.setInt(9, getArmor());
			statement.setInt(10, getJewel());
			statement.setInt(11, getControlItemId());
			statement.executeUpdate();
			_respawned = true;
		}
		catch (final Exception e)
		{
			_logPet.warning("could not store pet data: " + e);
		}
		
		final L2ItemInstance itemInst = getControlItem();
		if ((itemInst != null) && (itemInst.getEnchantLevel() != getStat().getLevel()))
		{
			itemInst.setEnchantLevel(getStat().getLevel());
			itemInst.updateDatabase();
		}
	}
	
	public synchronized void stopFeed()
	{
		if (_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
			if (Config.DEBUG)
			{
				_logPet.fine("Pet [#" + getObjectId() + "] feed task stop");
			}
		}
	}
	
	public synchronized void startFeed()
	{
		// stop feeding task if its active
		stopFeed();
		
		if (!isDead() && (getOwner().getPet() == this))
		{
			_feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
		}
	}
	
	@Override
	public synchronized void unSummon(L2PcInstance owner)
	{
		stopFeed();
		super.unSummon(owner);
		
		if (!isDead())
		{
			L2World.getInstance().removePet(owner.getObjectId());
		}
		
	}
	
	/**
	 * Restore the specified % of experience this L2PetInstance has lost.<BR>
	 * <BR>
	 * @param restorePercent
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getStat().getExp()) * restorePercent) / 100));
			_expBeforeDeath = 0;
		}
	}
	
	private void deathPenalty()
	{
		// TODO Need Correct Penalty
		
		final int lvl = getStat().getLevel();
		final double percentLost = (-0.07 * lvl) + 6.5;
		
		// Calculate the Experience loss
		final long lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);
		
		// Get the Experience before applying penalty
		_expBeforeDeath = getStat().getExp();
		
		// Set the new Experience value of the L2PetInstance
		getStat().addExp(-lostExp);
	}
	
	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if (getNpcId() == 12564)
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
		}
		else
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
		}
	}
	
	@Override
	public long getExpForThisLevel()
	{
		return getStat().getExpForLevel(getLevel());
	}
	
	@Override
	public long getExpForNextLevel()
	{
		return getStat().getExpForLevel(getLevel() + 1);
	}
	
	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}
	
	@Override
	public int getMaxFed()
	{
		return getStat().getMaxFeed();
	}
	
	@Override
	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}
	
	@Override
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}
	
	@Override
	public int getEvasionRate(L2Character target)
	{
		return getStat().getEvasionRate(target);
	}
	
	@Override
	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}
	
	@Override
	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}
	
	@Override
	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}
	
	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}
	
	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}
	
	@Override
	public int getPAtk(L2Character target)
	{
		return getStat().getPAtk(target);
	}
	
	@Override
	public int getPDef(L2Character target)
	{
		return getStat().getPDef(target);
	}
	
	@Override
	public final int getSkillLevel(int skillId)
	{
		if ((_Skills == null) || (_Skills.get(skillId) == null))
		{
			return -1;
		}
		
		final int lvl = getLevel();
		return lvl > 70 ? 7 + ((lvl - 70) / 5) : lvl / 10;
	}
	
	public void updateRefOwner(L2PcInstance owner)
	{
		final int oldOwnerId = getOwner().getObjectId();
		
		setOwner(owner);
		L2World.getInstance().removePet(oldOwnerId);
		L2World.getInstance().addPet(oldOwnerId, this);
	}
	
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	
	public final void setMaxLoad(int maxLoad)
	{
		_maxload = maxLoad;
	}
	
	public final int getMaxLoad()
	{
		return _maxload;
	}
	
	public int getInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_PET;
	}
	
	public void refreshOverloaded()
	{
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = (long) (((getCurrentLoad() - calcStat(Stats.MAX_LOAD, 1, this, null)) * 1000) / maxLoad);
			int newWeightPenalty;
			if ((weightproc < 500) || getOwner().getDietMode())
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0)
				{
					addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
					setIsOverloaded(getCurrentLoad() >= maxLoad);
				}
				else
				{
					super.removeSkill(getKnownSkill(4270));
					setIsOverloaded(false);
				}
			}
		}
	}
	
	@Override
	public void updateAndBroadcastStatus(int val)
	{
		refreshOverloaded();
		super.updateAndBroadcastStatus(val);
	}
	
	@Override
	public final boolean isHungry()
	{
		return getCurrentFed() < (0.55 * getPetData().getPetMaxFeed());
	}
	
	@Override
	public int getPetSpeed()
	{
		return getPetData().getPetSpeed();
	}
	
	public final void setWeapon(int id)
	{
		_weapon = id;
	}
	
	public final void setArmor(int id)
	{
		_armor = id;
	}
	
	public final void setJewel(int id)
	{
		_jewel = id;
	}
	
	@Override
	public final int getWeapon()
	{
		return _weapon;
	}
	
	@Override
	public final int getArmor()
	{
		return _armor;
	}
	
	public final int getJewel()
	{
		return _jewel;
	}
	
	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss)
		{
			getOwner().sendPacket(new SystemMessage(SystemMessage.MISSED_TARGET));
			return;
		}
		
		// Prevents the double spam of system messages, if the target is the owning player.
		if (target.getObjectId() != getOwner().getObjectId())
		{
			if (pcrit || mcrit)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessage.PET_CRITICAL_HIT));
			}
			
			final SystemMessage sm = new SystemMessage(SystemMessage.PET_DID_S1_DMG);
			sm.addNumber(damage);
			getOwner().sendPacket(sm);
		}
	}
	
	@Override
	public void setName(String name)
	{
		final L2ItemInstance controlItem = getControlItem();
		if (getControlItem().getCustomType2() == (name == null ? 1 : 0))
		{
			// name not set yet
			controlItem.setCustomType2(name != null ? 1 : 0);
			controlItem.updateDatabase();
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(controlItem);
			getOwner().sendPacket(iu);
		}
		
		super.setName(name);
	}
}