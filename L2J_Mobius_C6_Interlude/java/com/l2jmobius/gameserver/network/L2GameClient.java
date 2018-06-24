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
package com.l2jmobius.gameserver.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.commons.crypt.nProtect;
import com.l2jmobius.commons.database.DatabaseFactory;
import com.l2jmobius.commons.mmocore.MMOClient;
import com.l2jmobius.commons.mmocore.MMOConnection;
import com.l2jmobius.commons.mmocore.NetcoreConfig;
import com.l2jmobius.commons.mmocore.ReceivablePacket;
import com.l2jmobius.gameserver.datatables.OfflineTradeTable;
import com.l2jmobius.gameserver.datatables.SkillTable;
import com.l2jmobius.gameserver.datatables.sql.ClanTable;
import com.l2jmobius.gameserver.instancemanager.AwayManager;
import com.l2jmobius.gameserver.instancemanager.PlayerCountManager;
import com.l2jmobius.gameserver.model.CharSelectInfoPackage;
import com.l2jmobius.gameserver.model.L2Clan;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.entity.event.CTF;
import com.l2jmobius.gameserver.model.entity.event.DM;
import com.l2jmobius.gameserver.model.entity.event.L2Event;
import com.l2jmobius.gameserver.model.entity.event.TvT;
import com.l2jmobius.gameserver.model.entity.event.VIP;
import com.l2jmobius.gameserver.model.entity.olympiad.Olympiad;
import com.l2jmobius.gameserver.model.zone.ZoneId;
import com.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import com.l2jmobius.gameserver.network.serverpackets.ServerClose;
import com.l2jmobius.gameserver.thread.LoginServerThread;
import com.l2jmobius.gameserver.thread.LoginServerThread.SessionKey;
import com.l2jmobius.gameserver.util.EventData;
import com.l2jmobius.gameserver.util.FloodProtectors;

public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>> implements Runnable
{
	protected static final Logger LOGGER = Logger.getLogger(L2GameClient.class.getName());
	
	/**
	 * CONNECTED - client has just connected AUTHED - client has authed but doesn't has character attached to it yet IN_GAME - client has selected a char and is in game
	 * @author KenM
	 */
	public enum GameClientState
	{
		CONNECTED,
		AUTHED,
		IN_GAME
	}
	
	// floodprotectors
	private final FloodProtectors _floodProtectors = new FloodProtectors(this);
	
	public GameClientState _state;
	
	// Info
	public String accountName;
	public SessionKey sessionId;
	public L2PcInstance activeChar;
	private final ReentrantLock _activeCharLock = new ReentrantLock();
	
	private boolean _isAuthedGG;
	private final long _connectionStartTime;
	private final List<Integer> _charSlotMapping = new ArrayList<>();
	
	// Task
	private ScheduledFuture<?> _guardCheckTask = null;
	
	protected ScheduledFuture<?> _cleanupTask = null;
	
	private final ClientStats _stats;
	
	// Crypt
	private final GameCrypt _crypt;
	
	// Flood protection
	public long packetsNextSendTick = 0;
	
	protected boolean _closenow = true;
	private boolean _isDetached = false;
	
	protected boolean _forcedToClose = false;
	
	private final ArrayBlockingQueue<ReceivablePacket<L2GameClient>> _packetQueue;
	private final ReentrantLock _queueLock = new ReentrantLock();
	
	private long _last_received_packet_action_time = 0;
	
	public L2GameClient(MMOConnection<L2GameClient> con)
	{
		super(con);
		_state = GameClientState.CONNECTED;
		_connectionStartTime = System.currentTimeMillis();
		_crypt = new GameCrypt();
		_stats = new ClientStats();
		_packetQueue = new ArrayBlockingQueue<>(NetcoreConfig.getInstance().CLIENT_PACKET_QUEUE_SIZE);
		
		_guardCheckTask = nProtect.getInstance().startTask(this);
		ThreadPool.schedule(() ->
		{
			if (_closenow)
			{
				close(new LeaveWorld());
			}
		}, 4000);
		
	}
	
	public byte[] enableCrypt()
	{
		final byte[] key = BlowFishKeygen.getRandomKey();
		_crypt.setKey(key);
		return key;
	}
	
	public GameClientState getState()
	{
		return _state;
	}
	
	public void setState(GameClientState pState)
	{
		if (_state != pState)
		{
			_state = pState;
			_packetQueue.clear();
		}
	}
	
	public ClientStats getStats()
	{
		return _stats;
	}
	
	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}
	
	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		_closenow = false;
		_crypt.decrypt(buf.array(), buf.position(), size);
		return true;
	}
	
	@Override
	public boolean encrypt(ByteBuffer buf, int size)
	{
		_crypt.encrypt(buf.array(), buf.position(), size);
		buf.position(buf.position() + size);
		return true;
	}
	
	public L2PcInstance getActiveChar()
	{
		return activeChar;
	}
	
	public void setActiveChar(L2PcInstance pActiveChar)
	{
		activeChar = pActiveChar;
		if (activeChar != null)
		{
			L2World.getInstance().storeObject(getActiveChar());
		}
	}
	
	public ReentrantLock getActiveCharLock()
	{
		return _activeCharLock;
	}
	
	public boolean isAuthedGG()
	{
		return _isAuthedGG;
	}
	
	public void setGameGuardOk(boolean val)
	{
		_isAuthedGG = val;
	}
	
	public void setAccountName(String pAccountName)
	{
		accountName = pAccountName;
	}
	
	public String getAccountName()
	{
		return accountName;
	}
	
	public void setSessionId(SessionKey sk)
	{
		sessionId = sk;
	}
	
	public SessionKey getSessionId()
	{
		return sessionId;
	}
	
	public void sendPacket(L2GameServerPacket gsp)
	{
		if (_isDetached)
		{
			return;
		}
		
		if (getConnection() != null)
		{
			getConnection().sendPacket(gsp);
			gsp.runImpl();
		}
	}
	
	public boolean isDetached()
	{
		return _isDetached;
	}
	
	public void setDetached(boolean b)
	{
		_isDetached = b;
	}
	
	/**
	 * Method to handle character deletion
	 * @param charslot
	 * @return a byte:
	 *         <li>-1: Error: No char was found for such charslot, caught exception, etc...
	 *         <li>0: character is not member of any clan, proceed with deletion
	 *         <li>1: character is member of a clan, but not clan leader
	 *         <li>2: character is clan leader
	 */
	public byte markToDeleteChar(int charslot)
	{
		
		final int objid = getObjectIdForSlot(charslot);
		
		if (objid < 0)
		{
			return -1;
		}
		
		byte answer = -1;
		
		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT clanId from characters WHERE obj_Id=?");
			statement.setInt(1, objid);
			ResultSet rs = statement.executeQuery();
			
			rs.next();
			
			final int clanId = rs.getInt(1);
			
			answer = 0;
			
			if (clanId != 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				
				if (clan == null)
				{
					answer = 0; // jeezes!
				}
				else if (clan.getLeaderId() == objid)
				{
					answer = 2;
				}
				else
				{
					answer = 1;
				}
			}
			
			// Setting delete time
			if (answer == 0)
			{
				if (Config.DELETE_DAYS == 0)
				{
					deleteCharByObjId(objid);
				}
				else
				{
					statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_Id=?");
					statement.setLong(1, System.currentTimeMillis() + (Config.DELETE_DAYS * 86400000)); // 24*60*60*1000 = 86400000
					statement.setInt(2, objid);
					statement.execute();
					statement.close();
					rs.close();
				}
			}
			else
			{
				statement.close();
				rs.close();
			}
			
		}
		catch (Exception e)
		{
			LOGGER.warning("Data error on update delete time of char: " + e);
			answer = -1;
		}
		
		return answer;
	}
	
	public void markRestoredChar(int charslot)
	{
		// have to make sure active character must be nulled
		final int objid = getObjectIdForSlot(charslot);
		
		if (objid < 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("Data error on restoring char " + e);
		}
	}
	
	public static void deleteCharByObjId(int objid)
	{
		if (objid < 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement;
			
			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? OR friend_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM heroes WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM seven_signs WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("Data error on deleting char: " + e);
		}
	}
	
	public L2PcInstance loadCharFromDisk(int charslot)
	{
		// L2PcInstance character = L2PcInstance.load(getObjectIdForSlot(charslot));
		
		final int objId = getObjectIdForSlot(charslot);
		if (objId < 0)
		{
			return null;
		}
		
		L2PcInstance character = L2World.getInstance().getPlayer(objId);
		if (character != null)
		{
			// exploit prevention, should not happens in normal way
			
			LOGGER.warning("Attempt of double login: " + character.getName() + "(" + objId + ") " + accountName);
			
			if (character.getClient() != null)
			{
				character.getClient().closeNow();
			}
			else
			{
				character.deleteMe();
				
				try
				{
					character.store();
				}
				catch (Exception e2)
				{
					LOGGER.warning("fixme:unhandled exception " + e2);
				}
				
			}
			
			// return null;
		}
		
		character = L2PcInstance.load(objId);
		// if(character != null)
		// {
		// //restoreInventory(character);
		// //restoreSkills(character);
		// //character.restoreSkills();
		// //restoreShortCuts(character);
		// //restoreWarehouse(character);
		//
		// // preinit some values for each login
		// character.setRunning(); // running is default
		// character.standUp(); // standing is default
		//
		// character.refreshOverloaded();
		// character.refreshExpertisePenalty();
		// character.refreshMasteryPenality();
		// character.refreshMasteryWeapPenality();
		//
		// character.sendPacket(new UserInfo(character));
		// character.broadcastKarma();
		// character.setOnlineStatus(true);
		// }
		// if(character == null)
		// {
		// LOGGER.severe("could not restore in slot: " + charslot);
		// }
		
		// setCharacter(character);
		return character;
	}
	
	/**
	 * @param chars
	 */
	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping.clear();
		
		for (CharSelectInfoPackage c : chars)
		{
			final int objectId = c.getObjectId();
			
			_charSlotMapping.add(new Integer(objectId));
		}
	}
	
	public void close(L2GameServerPacket gsp)
	{
		if (getConnection() != null)
		{
			getConnection().close(gsp);
		}
	}
	
	/**
	 * @param charslot
	 * @return
	 */
	private int getObjectIdForSlot(int charslot)
	{
		if ((charslot < 0) || (charslot >= _charSlotMapping.size()))
		{
			LOGGER.warning(this + " tried to delete Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}
		
		final Integer objectId = _charSlotMapping.get(charslot);
		
		return objectId.intValue();
	}
	
	@Override
	public void onForcedDisconnection(boolean critical)
	{
		_forcedToClose = true;
		
		// the force operation will allow to not save client position to prevent again criticals and stuck
		closeNow();
	}
	
	public void stopGuardTask()
	{
		if (_guardCheckTask != null)
		{
			_guardCheckTask.cancel(true);
			_guardCheckTask = null;
		}
		
	}
	
	@Override
	public void onDisconnection()
	{
		// no long running tasks here, do it async
		try
		{
			ThreadPool.execute(new DisconnectTask());
			
		}
		catch (RejectedExecutionException e)
		{
			// server is closing
		}
	}
	
	/**
	 * Close client connection with {@link ServerClose} packet
	 */
	public void closeNow()
	{
		close(0);
	}
	
	/**
	 * Close client connection with {@link ServerClose} packet
	 * @param delay
	 */
	public void close(int delay)
	{
		
		close(ServerClose.STATIC_PACKET);
		synchronized (this)
		{
			if (_cleanupTask != null)
			{
				cancelCleanup();
			}
			_cleanupTask = ThreadPool.schedule(new CleanupTask(), delay); // delayed
		}
		stopGuardTask();
		nProtect.getInstance().closeSession(this);
	}
	
	/**
	 * Produces the best possible string representation of this client.
	 */
	@Override
	public String toString()
	{
		try
		{
			InetAddress address = getConnection().getInetAddress();
			String ip = "N/A";
			
			if (address == null)
			{
				ip = "disconnected";
			}
			else
			{
				ip = address.getHostAddress();
			}
			
			switch (_state)
			{
				case CONNECTED:
				{
					return "[IP: " + ip + "]";
				}
				case AUTHED:
				{
					return "[Account: " + accountName + " - IP: " + ip + "]";
				}
				case IN_GAME:
				{
					return "[Character: " + (activeChar == null ? "disconnected" : activeChar.getName()) + " - Account: " + accountName + " - IP: " + ip + "]";
				}
				default:
				{
					throw new IllegalStateException("Missing state on switch");
				}
			}
		}
		catch (NullPointerException e)
		{
			return "[Character read failed due to disconnect]";
		}
	}
	
	protected class CleanupTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				// // we are going to manually save the char below thus we can force the cancel
				// if (_autoSaveInDB != null)
				// _autoSaveInDB.cancel(true);
				//
				
				final L2PcInstance player = activeChar;
				if (player != null) // this should only happen on connection loss
				{
					// we store all data from players who are disconnected while in an event in order to restore it in the next login
					if (player.atEvent)
					{
						EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventKarma, player.eventPvpKills, player.eventPkKills, player.eventTitle, player.kills, player.eventSitForced);
						
						L2Event.connectionLossData.put(player.getName(), data);
					}
					else if (player._inEventCTF)
					{
						CTF.onDisconnect(player);
					}
					else if (player._inEventDM)
					{
						DM.onDisconnect(player);
					}
					else if (player._inEventTvT)
					{
						TvT.onDisconnect(player);
					}
					else if (player._inEventVIP)
					{
						VIP.onDisconnect(player);
					}
					
					if (player.isFlying())
					{
						player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
					}
					
					if (player.isAway())
					{
						AwayManager.getInstance().extraBack(player);
					}
					
					if (Olympiad.getInstance().isRegistered(player))
					{
						Olympiad.getInstance().unRegisterNoble(player);
					}
					
					// Decrease boxes number
					if (player._active_boxes != -1)
					{
						player.decreaseBoxes();
					}
					
					// prevent closing again
					player.setClient(null);
					
					player.deleteMe();
					
					try
					{
						player.store(_forcedToClose);
					}
					catch (Exception e2)
					{
					}
				}
				
				setActiveChar(null);
				setDetached(true);
			}
			catch (Exception e1)
			{
				LOGGER.warning("Error while cleanup client. " + e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(getAccountName());
			}
		}
	}
	
	protected class DisconnectTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				// // we are going to manually save the char bellow thus we can force the cancel
				// if(_autoSaveInDB != null)
				// _autoSaveInDB.cancel(true);
				
				L2PcInstance player = activeChar;
				if (player != null) // this should only happen on connection loss
				{
					// we store all data from players who are disconnected while in an event in order to restore it in the next login
					if (player.atEvent)
					{
						EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventKarma, player.eventPvpKills, player.eventPkKills, player.eventTitle, player.kills, player.eventSitForced);
						
						L2Event.connectionLossData.put(player.getName(), data);
					}
					else if (player._inEventCTF)
					{
						CTF.onDisconnect(player);
					}
					else if (player._inEventDM)
					{
						DM.onDisconnect(player);
					}
					else if (player._inEventTvT)
					{
						TvT.onDisconnect(player);
					}
					else if (player._inEventVIP)
					{
						VIP.onDisconnect(player);
					}
					
					if (player.isFlying())
					{
						player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
					}
					
					if (player.isAway())
					{
						AwayManager.getInstance().extraBack(player);
					}
					
					if (Olympiad.getInstance().isRegistered(player))
					{
						Olympiad.getInstance().unRegisterNoble(player);
					}
					
					// Decrease boxes number
					if (player._active_boxes != -1)
					{
						player.decreaseBoxes();
					}
					
					if (!player.isKicked() //
						&& !Olympiad.getInstance().isRegistered(player) //
						&& !player.isInOlympiadMode() //
						&& !player.isInFunEvent() //
						&& ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) //
							|| (player.isCrafting() && Config.OFFLINE_CRAFT_ENABLE)))
					{
						if (!Config.OFFLINE_MODE_IN_PEACE_ZONE || (Config.OFFLINE_MODE_IN_PEACE_ZONE && player.isInsideZone(ZoneId.PEACE)))
						{
							player.setOfflineMode(true);
							player.setOnlineStatus(false);
							player.leaveParty();
							player.store();
							
							if (Config.OFFLINE_MODE_SET_INVULNERABLE)
							{
								activeChar.setIsInvul(true);
							}
							if (Config.OFFLINE_SET_NAME_COLOR)
							{
								player._originalNameColorOffline = player.getAppearance().getNameColor();
								player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
								player.broadcastUserInfo();
							}
							
							if (player.getOfflineStartTime() == 0)
							{
								player.setOfflineStartTime(System.currentTimeMillis());
							}
							
							OfflineTradeTable.storeOffliner(player);
							PlayerCountManager.getInstance().incOfflineTradeCount();
							return;
						}
					}
					
					// notify the world about our disconnect
					player.deleteMe();
					
					// store operation
					try
					{
						player.store();
					}
					catch (Exception e2)
					{
					}
				}
				
				setActiveChar(null);
				setDetached(true);
			}
			catch (Exception e1)
			{
				LOGGER.warning("error while disconnecting client " + e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(getAccountName());
			}
		}
	}
	
	public FloodProtectors getFloodProtectors()
	{
		return _floodProtectors;
	}
	
	private boolean cancelCleanup()
	{
		final Future<?> task = _cleanupTask;
		if (task != null)
		{
			_cleanupTask = null;
			return task.cancel(true);
		}
		return false;
	}
	
	/**
	 * Returns false if client can receive packets. True if detached, or flood detected, or queue overflow detected and queue still not empty.
	 * @return
	 */
	public boolean dropPacket()
	{
		if (_isDetached)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Counts buffer underflow exceptions.
	 */
	public void onBufferUnderflow()
	{
		if (_stats.countUnderflowException())
		{
			LOGGER.warning("Client " + this + " - Disconnected: Too many buffer underflow exceptions.");
			closeNow();
			return;
		}
		if (_state == GameClientState.CONNECTED) // in CONNECTED state kick client immediately
		{
			LOGGER.warning("Client " + this + " - Disconnected, too many buffer underflows in non-authed state.");
			closeNow();
		}
	}
	
	/**
	 * Add packet to the queue and start worker thread if needed
	 * @param packet
	 */
	public void execute(ReceivablePacket<L2GameClient> packet)
	{
		if (_stats.countFloods())
		{
			LOGGER.warning("Client " + this + " - Disconnected, too many floods:" + _stats.longFloods + " long and " + _stats.shortFloods + " short.");
			closeNow();
			return;
		}
		
		if (!_packetQueue.offer(packet))
		{
			if (_stats.countQueueOverflow())
			{
				LOGGER.warning("Client " + this + " - Disconnected, too many queue overflows.");
				closeNow();
			}
			else
			{
				sendPacket(ActionFailed.STATIC_PACKET);
			}
			
			return;
		}
		
		if (_queueLock.isLocked())
		{
			return;
		}
		
		// save last action time
		_last_received_packet_action_time = System.currentTimeMillis();
		// LOGGER.severe("Client " + toString() + " - updated last action state "+_last_received_packet_action_time);
		
		try
		{
			if (_state == GameClientState.CONNECTED)
			{
				if (_stats.processedPackets > 3)
				{
					LOGGER.warning("Client " + this + " - Disconnected, too many packets in non-authed state.");
					closeNow();
					return;
				}
				
				ThreadPool.execute(this);
			}
			else
			{
				ThreadPool.execute(this);
			}
		}
		catch (RejectedExecutionException e)
		{
		}
	}
	
	@Override
	public void run()
	{
		if (!_queueLock.tryLock())
		{
			return;
		}
		
		try
		{
			int count = 0;
			while (true)
			{
				final ReceivablePacket<L2GameClient> packet = _packetQueue.poll();
				if (packet == null)
				{
					return;
				}
				
				if (_isDetached) // clear queue immediately after detach
				{
					_packetQueue.clear();
					return;
				}
				
				try
				{
					packet.run();
				}
				catch (Exception e)
				{
					LOGGER.warning("Exception during execution " + packet.getClass().getSimpleName() + ", client: " + this + "," + e.getMessage());
				}
				
				count++;
				if (_stats.countBurst(count))
				{
					return;
				}
			}
		}
		finally
		{
			_queueLock.unlock();
		}
	}
	
	/**
	 * @return the _forcedToClose
	 */
	public boolean is_forcedToClose()
	{
		return _forcedToClose;
	}
	
	public boolean isConnectionAlive()
	{
		// if last received packet time is higher then Config.CHECK_CONNECTION_INACTIVITY_TIME --> check connection
		if ((System.currentTimeMillis() - _last_received_packet_action_time) > Config.CHECK_CONNECTION_INACTIVITY_TIME)
		{
			
			_last_received_packet_action_time = System.currentTimeMillis();
			
			return getConnection().isConnected() && !getConnection().isClosed();
			
		}
		
		return true;
	}
}
