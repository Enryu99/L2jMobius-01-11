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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.knownlist.GuardNoHTMLKnownList;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

/**
 * This class manages all Guards in the world. It inherits all methods from Attackable and adds some more such as tracking PK and aggressive MonsterInstance.<br>
 * <br>
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/06 16:13:40 $
 */
public class GuardNoHTMLInstance extends Attackable
{
	private static final int RETURN_INTERVAL = 60000;
	private int _homeX;
	private int _homeY;
	private int _homeZ;
	
	public class ReturnTask implements Runnable
	{
		@Override
		public void run()
		{
			if (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			{
				returnHome();
			}
		}
	}
	
	/**
	 * Constructor of GuardInstance (use Creature and NpcInstance constructor).<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <li>Call the Creature constructor to set the _template of the GuardInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the GuardInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li>
	 * @param objectId Identifier of the object to initialized
	 * @param template the template
	 */
	public GuardNoHTMLInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		ThreadPool.scheduleAtFixedRate(new ReturnTask(), RETURN_INTERVAL, RETURN_INTERVAL + Rnd.get(60000));
	}
	
	@Override
	public GuardNoHTMLKnownList getKnownList()
	{
		if (!(super.getKnownList() instanceof GuardNoHTMLKnownList))
		{
			setKnownList(new GuardNoHTMLKnownList(this));
		}
		return (GuardNoHTMLKnownList) super.getKnownList();
	}
	
	/**
	 * Return true if hte attacker is a MonsterInstance.
	 * @param attacker the attacker
	 * @return true, if is auto attackable
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return attacker instanceof MonsterInstance;
	}
	
	/**
	 * Set home location of the GuardInstance.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Guard will always try to return to this location after it has killed all PK's in range
	 */
	public void getHomeLocation()
	{
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
	}
	
	/**
	 * Gets the home x.
	 * @return the home x
	 */
	public int getHomeX()
	{
		return _homeX;
	}
	
	/**
	 * Notify the GuardInstance to return to its home location (AI_INTENTION_MOVE_TO) and clear its _aggroList.
	 */
	public void returnHome()
	{
		if (!isInsideRadius2D(_homeX, _homeY, _homeZ, 150))
		{
			clearAggroList();
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_homeX, _homeY, _homeZ, 0));
		}
	}
	
	/**
	 * Set the home location of its GuardInstance.
	 */
	@Override
	public void onSpawn()
	{
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		final WorldRegion region = World.getInstance().getRegion(getX(), getY());
		if ((region != null) && !region.isActive())
		{
			((AttackableAI) getAI()).stopAITask();
		}
	}
	
	/**
	 * Manage actions when a player click on the GuardInstance.<br>
	 * <br>
	 * <b><u>Actions on first click on the GuardInstance (Select it)</u>:</b><br>
	 * <li>Set the GuardInstance as target of the PlayerInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the PlayerInstance player (display the select window)</li>
	 * <li>Set the PlayerInstance Intention to AI_INTENTION_IDLE</li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the GuardInstance position and heading on the client</li><br>
	 * <br>
	 * <b><u>Actions on second click on the GuardInstance (Attack it/Interact with it)</u>:</b><br>
	 * <li>If PlayerInstance is in the _aggroList of the GuardInstance, set the PlayerInstance Intention to AI_INTENTION_ATTACK</li>
	 * <li>If PlayerInstance is NOT in the _aggroList of the GuardInstance, set the PlayerInstance Intention to AI_INTENTION_INTERACT (after a distance verification) and show message</li><br>
	 * <br>
	 * <b><u>Example of use</u>:</b><br>
	 * <li>Client packet : Action, AttackRequest</li>
	 * @param player The PlayerInstance that start an action on the GuardInstance
	 */
	@Override
	public void onAction(PlayerInstance player)
	{
		// Check if the PlayerInstance already target the GuardInstance
		if (getObjectId() != player.getTargetId())
		{
			// Set the PlayerInstance Intention to AI_INTENTION_IDLE
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
			// Set the target of the PlayerInstance player
			player.setTarget(this);
			// Send a Server->Client packet MyTargetSelected to the PlayerInstance player The color to display in the select window is White
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			// Send a Server->Client packet ValidateLocation to correct the NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else if (containsTarget(player)) // Check if the PlayerInstance is in the _aggroList of the GuardInstance
		{
			// Set the PlayerInstance Intention to AI_INTENTION_ATTACK
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
		}
		else // Calculate the distance between the PlayerInstance and the NpcInstance
		if (!isInsideRadius2D(player, INTERACTION_DISTANCE))
		{
			// Set the PlayerInstance Intention to AI_INTENTION_INTERACT
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
		}
		else
		{
			// Send a Server->Client ActionFailed to the PlayerInstance in order to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
			// Set the PlayerInstance Intention to AI_INTENTION_IDLE
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, null);
		}
	}
}
