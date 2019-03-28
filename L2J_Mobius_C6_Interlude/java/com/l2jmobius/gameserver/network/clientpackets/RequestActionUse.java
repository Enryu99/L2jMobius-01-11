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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jmobius.gameserver.GameTimeController;
import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.instancemanager.CastleManager;
import com.l2jmobius.gameserver.model.Inventory;
import com.l2jmobius.gameserver.model.ManufactureList;
import com.l2jmobius.gameserver.model.Skill;
import com.l2jmobius.gameserver.model.WorldObject;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.Summon;
import com.l2jmobius.gameserver.model.actor.instance.DoorInstance;
import com.l2jmobius.gameserver.model.actor.instance.PetInstance;
import com.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import com.l2jmobius.gameserver.model.actor.instance.SiegeSummonInstance;
import com.l2jmobius.gameserver.model.actor.instance.StaticObjectInstance;
import com.l2jmobius.gameserver.model.actor.instance.SummonInstance;
import com.l2jmobius.gameserver.model.actor.position.Location;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.SystemMessageId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.ChairSit;
import com.l2jmobius.gameserver.network.serverpackets.RecipeShopManageList;
import com.l2jmobius.gameserver.network.serverpackets.Ride;
import com.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public final class RequestActionUse extends GameClientPacket
{
	private static Logger LOGGER = Logger.getLogger(RequestActionUse.class.getName());
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	// List of Pet Actions
	private static List<Integer> _petActions = Arrays.asList(new Integer[]
	{
		15,
		16,
		17,
		21,
		22,
		23,
		52,
		53,
		54
	});
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = readD() == 1;
		_shiftPressed = readC() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getPlayer();
		
		if (player == null)
		{
			return;
		}
		
		// dont do anything if player is dead
		if ((_actionId != 0) && player.isAlikeDead())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// don't do anything if player is confused
		if (player.isOutOfControl())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// don't do anything if player is casting and the action is not a Pet one (skills too)
		if ((_petActions.contains(_actionId) || (_actionId >= 1000)))
		{
			// LOGGER.info(activeChar.getName() + " request Pet Action use: id " + _actionId + " ctrl:" + _ctrlPressed + " shift:" + _shiftPressed);
		}
		else if (player.isCastingNow())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Summon pet = player.getPet();
		final WorldObject target = player.getTarget();
		
		switch (_actionId)
		{
			case 0:
			{
				if (player.getMountType() != 0)
				{
					break;
				}
				if ((target != null) && !player.isSitting() && (target instanceof StaticObjectInstance) && (((StaticObjectInstance) target).getType() == 1) && (CastleManager.getInstance().getCastle(target) != null) && player.isInsideRadius(target, StaticObjectInstance.INTERACTION_DISTANCE, false, false))
				{
					final ChairSit cs = new ChairSit(player, ((StaticObjectInstance) target).getStaticObjectId());
					player.sendPacket(cs);
					player.sitDown();
					player.broadcastPacket(cs);
					break;
				}
				if (player.isSitting() || player.isFakeDeath())
				{
					player.standUp();
				}
				else
				{
					player.sitDown();
				}
				break;
			}
			case 1:
			{
				if (player.isRunning())
				{
					player.setWalking();
				}
				else
				{
					player.setRunning();
				}
				break;
			}
			case 15:
			case 21: // pet follow/stop
			{
				if ((pet != null) && !pet.isMovementDisabled() && !player.isBetrayed())
				{
					pet.setFollowStatus(!pet.getFollowStatus());
				}
				break;
			}
			case 16:
			case 22: // pet attack
			{
				if ((target != null) && (pet != null) && (pet != target) && !player.isBetrayed())
				{
					if (pet.isAttackingDisabled())
					{
						if (pet.getAttackEndTime() > GameTimeController.getGameTicks())
						{
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
						else
						{
							return;
						}
					}
					if (player.isInOlympiadMode() && !player.isOlympiadStart())
					{
						// if PlayerInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
						player.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					if ((target instanceof PlayerInstance) && !player.getAccessLevel().allowPeaceAttack() && Creature.isInsidePeaceZone(pet, target))
					{
						if (!player.isInFunEvent() || !((PlayerInstance) target).isInFunEvent())
						{
							player.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
							return;
						}
					}
					if (target.isAutoAttackable(player) || _ctrlPressed)
					{
						if (target instanceof DoorInstance)
						{
							if (((DoorInstance) target).isAttackable(player) && (pet.getNpcId() != SiegeSummonInstance.SWOOP_CANNON_ID))
							{
								pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
							}
						}
						// siege golem AI doesn't support attacking other than doors at the moment
						else if (pet.getNpcId() != SiegeSummonInstance.SIEGE_GOLEM_ID)
						{
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
					}
				}
				break;
			}
			case 17:
			case 23: // pet - cancel action
			{
				if ((pet != null) && !pet.isMovementDisabled() && !player.isBetrayed())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
				}
				break;
			}
			case 19: // pet unsummon
			{
				if ((pet != null) && !player.isBetrayed())
				{
					// returns pet to control item
					if (pet.isDead())
					{
						player.sendPacket(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED);
					}
					else if (pet.isAttackingNow() || pet.isRooted())
					{
						player.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else if (pet.isInCombat() || player.isInCombat())
					{
						player.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else // if it is a pet and not a summon
					if (pet instanceof PetInstance)
					{
						final PetInstance petInst = (PetInstance) pet;
						// if the pet is more than 40% fed
						if (petInst.getCurrentFed() > (petInst.getMaxFed() * 0.40))
						{
							pet.unSummon(player);
						}
						else
						{
							player.sendPacket(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS);
						}
					}
				}
				break;
			}
			case 38: // pet mount
			{
				// mount
				if ((pet != null) && pet.isMountable() && !player.isMounted() && !player.isBetrayed())
				{
					if (player.isDead())
					{
						// A strider cannot be ridden when dead
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
						player.sendPacket(msg);
					}
					else if (pet.isDead())
					{
						// A dead strider cannot be ridden.
						final SystemMessage msg = new SystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
						player.sendPacket(msg);
					}
					else if (pet.isInCombat() || pet.isRooted())
					{
						// A strider in battle cannot be ridden
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
						player.sendPacket(msg);
					}
					else if (player.isInCombat())
					{
						// A strider cannot be ridden while in battle
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
						player.sendPacket(msg);
					}
					else if (player.isInFunEvent())
					{
						// A strider cannot be ridden while in event
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
						player.sendPacket(msg);
					}
					else if (player.isSitting()) // Like L2OFF you can mount also during movement
					{
						// A strider can be ridden only when standing
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
						player.sendPacket(msg);
					}
					else if (player.isFishing())
					{
						// You can't mount, dismount, break and drop items while fishing
						final SystemMessage msg = new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
						player.sendPacket(msg);
					}
					else if (player.isCursedWeaponEquiped())
					{
						// You can't mount, dismount, break and drop items while weilding a cursed weapon
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
						player.sendPacket(msg);
					}
					else if (!pet.isDead() && !player.isMounted())
					{
						if (!player.disarmWeapons())
						{
							return;
						}
						if (!player.getFloodProtectors().getItemPetSummon().tryPerformAction("mount"))
						{
							return;
						}
						final Ride mount = new Ride(player.getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().npcId);
						player.broadcastPacket(mount);
						player.setMountType(mount.getMountType());
						player.setMountObjectID(pet.getControlItemId());
						pet.unSummon(player);
						if ((player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) != null) || (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND) != null))
						{
							if (player.isFlying())
							{
								// Remove skill Wyvern Breath
								player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
								player.sendSkillList();
							}
							if (player.setMountType(0))
							{
								final Ride dismount = new Ride(player.getObjectId(), Ride.ACTION_DISMOUNT, 0);
								player.broadcastPacket(dismount);
								player.setMountObjectID(0);
							}
						}
					}
				}
				else if (player.isRentedPet())
				{
					player.stopRentPet();
				}
				else if (player.isMounted())
				{
					player.dismount();
				}
				break;
			}
			case 32: // Wild Hog Cannon - Mode Change
			{
				useSkill(4230);
				break;
			}
			case 36: // Soulless - Toxic Smoke
			{
				useSkill(4259);
				break;
			}
			case 37:
			{
				if (player.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Like L2OFF - You can't open Manufacture when you are in private store
				if ((player.getPrivateStoreType() == PlayerInstance.STORE_PRIVATE_BUY) || (player.getPrivateStoreType() == PlayerInstance.STORE_PRIVATE_SELL))
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Like L2OFF - You can't open Manufacture when you are sitting
				if (player.isSitting() && (player.getPrivateStoreType() != PlayerInstance.STORE_PRIVATE_MANUFACTURE))
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// You can't open Manufacture when the task is launched
				if (player.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (player.getPrivateStoreType() == PlayerInstance.STORE_PRIVATE_MANUFACTURE)
				{
					player.setPrivateStoreType(PlayerInstance.STORE_PRIVATE_NONE);
					if (player.isSitting())
					{
						player.standUp();
					}
				}
				if (player.getCreateList() == null)
				{
					player.setCreateList(new ManufactureList());
				}
				player.sendPacket(new RecipeShopManageList(player, true));
				break;
			}
			case 39: // Soulless - Parasite Burst
			{
				useSkill(4138);
				break;
			}
			case 41: // Wild Hog Cannon - Attack
			{
				useSkill(4230);
				break;
			}
			case 42: // Kai the Cat - Self Damage Shield
			{
				useSkill(4378, player);
				break;
			}
			case 43: // Unicorn Merrow - Hydro Screw
			{
				useSkill(4137);
				break;
			}
			case 44: // Big Boom - Boom Attack
			{
				useSkill(4139);
				break;
			}
			case 45: // Unicorn Boxer - Master Recharge
			{
				useSkill(4025, player);
				break;
			}
			case 46: // Mew the Cat - Mega Storm Strike
			{
				useSkill(4261);
				break;
			}
			case 47: // Silhouette - Steal Blood
			{
				useSkill(4260);
				break;
			}
			case 48: // Mechanic Golem - Mech. Cannon
			{
				useSkill(4068);
				break;
			}
			case 51:
			{
				// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
				if (player.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Like L2OFF - You can't open Manufacture when you are in private store
				if ((player.getPrivateStoreType() == PlayerInstance.STORE_PRIVATE_BUY) || (player.getPrivateStoreType() == PlayerInstance.STORE_PRIVATE_SELL))
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Like L2OFF - You can't open Manufacture when you are sitting
				if (player.isSitting() && (player.getPrivateStoreType() != PlayerInstance.STORE_PRIVATE_MANUFACTURE))
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// You can't open Manufacture when the task is launched
				if (player.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (player.getPrivateStoreType() == PlayerInstance.STORE_PRIVATE_MANUFACTURE)
				{
					player.setPrivateStoreType(PlayerInstance.STORE_PRIVATE_NONE);
					if (player.isSitting())
					{
						player.standUp();
					}
				}
				if (player.getCreateList() == null)
				{
					player.setCreateList(new ManufactureList());
				}
				player.sendPacket(new RecipeShopManageList(player, false));
				break;
			}
			case 52: // unsummon
			{
				if ((pet != null) && (pet instanceof SummonInstance))
				{
					if (pet.isInCombat() || player.isInCombat())
					{
						player.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else if (pet.isAttackingNow() || pet.isRooted())
					{
						player.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else
					{
						pet.unSummon(player);
					}
				}
				break;
			}
			case 53: // move to target
			{
				if ((target != null) && (pet != null) && (pet != target) && !pet.isMovementDisabled())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			}
			case 54: // move to target hatch/strider
			{
				if ((target != null) && (pet != null) && (pet != target) && !pet.isMovementDisabled())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			}
			case 96: // Quit Party Command Channel
			{
				LOGGER.info("98 Accessed");
				break;
			}
			case 97: // Request Party Command Channel Info
			{
				// if (!PartyCommandManager.getInstance().isPlayerInChannel(activeChar))
				// return;
				LOGGER.info("97 Accessed");
				// PartyCommandManager.getInstance().getActiveChannelInfo(activeChar);
				break;
			}
			case 1000: // Siege Golem - Siege Hammer
			{
				if (target instanceof DoorInstance)
				{
					useSkill(4079);
				}
				break;
			}
			case 1001:
			{
				break;
			}
			case 1003: // Wind Hatchling/Strider - Wild Stun
			{
				useSkill(4710); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1004: // Wind Hatchling/Strider - Wild Defense
			{
				useSkill(4711, player); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1005: // Star Hatchling/Strider - Bright Burst
			{
				useSkill(4712); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1006: // Star Hatchling/Strider - Bright Heal
			{
				useSkill(4713, player); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1007: // Cat Queen - Blessing of Queen
			{
				useSkill(4699, player);
				break;
			}
			case 1008: // Cat Queen - Gift of Queen
			{
				useSkill(4700, player);
				break;
			}
			case 1009: // Cat Queen - Cure of Queen
			{
				useSkill(4701);
				break;
			}
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
			{
				useSkill(4702, player);
				break;
			}
			case 1011: // Unicorn Seraphim - Gift of Seraphim
			{
				useSkill(4703, player);
				break;
			}
			case 1012: // Unicorn Seraphim - Cure of Seraphim
			{
				useSkill(4704);
				break;
			}
			case 1013: // Nightshade - Curse of Shade
			{
				useSkill(4705);
				break;
			}
			case 1014: // Nightshade - Mass Curse of Shade
			{
				useSkill(4706, player);
				break;
			}
			case 1015: // Nightshade - Shade Sacrifice
			{
				useSkill(4707);
				break;
			}
			case 1016: // Cursed Man - Cursed Blow
			{
				useSkill(4709);
				break;
			}
			case 1017: // Cursed Man - Cursed Strike/Stun
			{
				useSkill(4708);
				break;
			}
			case 1031: // Feline King - Slash
			{
				useSkill(5135);
				break;
			}
			case 1032: // Feline King - Spinning Slash
			{
				useSkill(5136);
				break;
			}
			case 1033: // Feline King - Grip of the Cat
			{
				useSkill(5137);
				break;
			}
			case 1034: // Magnus the Unicorn - Whiplash
			{
				useSkill(5138);
				break;
			}
			case 1035: // Magnus the Unicorn - Tridal Wave
			{
				useSkill(5139);
				break;
			}
			case 1036: // Spectral Lord - Corpse Kaboom
			{
				useSkill(5142);
				break;
			}
			case 1037: // Spectral Lord - Dicing Death
			{
				useSkill(5141);
				break;
			}
			case 1038: // Spectral Lord - Force Curse
			{
				useSkill(5140);
				break;
			}
			case 1039: // Swoop Cannon - Cannon Fodder
			{
				if (!(target instanceof DoorInstance))
				{
					useSkill(5110);
				}
				break;
			}
			case 1040: // Swoop Cannon - Big Bang
			{
				if (!(target instanceof DoorInstance))
				{
					useSkill(5111);
				}
				break;
			}
			default:
			{
				LOGGER.warning(player.getName() + ": unhandled action type " + _actionId);
			}
		}
	}
	
	/*
	 * Cast a skill for active pet/servitor. Target is specified as a parameter but can be overwrited or ignored depending on skill type.
	 */
	private void useSkill(int skillId, WorldObject target)
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Summon activeSummon = player.getPet();
		
		if (player.getPrivateStoreType() != 0)
		{
			player.sendMessage("Cannot use skills while trading");
			return;
		}
		
		if ((activeSummon != null) && !player.isBetrayed())
		{
			final Map<Integer, Skill> _skills = activeSummon.getTemplate().getSkills();
			
			if (_skills.size() == 0)
			{
				// player.sendPacket(SystemMessageId.SKILL_NOT_AVAILABLE);
				return;
			}
			
			final Skill skill = _skills.get(skillId);
			
			if (skill == null)
			{
				return;
			}
			
			activeSummon.setTarget(target);
			
			boolean force = _ctrlPressed;
			
			if (target instanceof Creature)
			{
				if (activeSummon.isInsideZone(ZoneId.PVP) && ((Creature) target).isInsideZone(ZoneId.PVP))
				{
					force = true;
				}
			}
			
			activeSummon.useMagic(skill, force, _shiftPressed);
		}
	}
	
	/*
	 * Cast a skill for active pet/servitor. Target is retrieved from owner' target, then validated by overloaded method useSkill(int, Creature).
	 */
	private void useSkill(int skillId)
	{
		final PlayerInstance player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		useSkill(skillId, player.getTarget());
	}
}
