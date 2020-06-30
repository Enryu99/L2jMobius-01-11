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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.datatables.sql.NpcTable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;

public class CharInfo extends GameServerPacket
{
	private static final Logger LOGGER = Logger.getLogger(CharInfo.class.getName());
	
	private final PlayerInstance _player;
	private final Inventory _inventory;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	private final int _mAtkSpd;
	private final int _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final float _moveMultiplier;
	private final float _attackSpeedMultiplier;
	
	public CharInfo(PlayerInstance player)
	{
		_player = player;
		_inventory = player.getInventory();
		_x = player.getX();
		_y = player.getY();
		_z = player.getZ();
		_heading = player.getHeading();
		_mAtkSpd = player.getMAtkSpd();
		_pAtkSpd = player.getPAtkSpd();
		_attackSpeedMultiplier = player.getAttackSpeedMultiplier();
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = Math.round(player.getWalkSpeed() / _moveMultiplier);
		_flyRunSpd = player.isFlying() ? _runSpd : 0;
		_flyWalkSpd = player.isFlying() ? _walkSpd : 0;
	}
	
	@Override
	protected final void writeImpl()
	{
		boolean isGM = false;
		
		final PlayerInstance tmp = getClient().getPlayer();
		if ((tmp != null) && tmp.isGM())
		{
			isGM = true;
		}
		
		if (!isGM && _player.getAppearance().isInvisible())
		{
			return;
		}
		
		if (_player.getPoly().isMorphed())
		{
			final NpcTemplate template = NpcTable.getInstance().getTemplate(_player.getPoly().getPolyId());
			if (template != null)
			{
				writeC(0x16);
				writeD(_player.getObjectId());
				writeD(_player.getPoly().getPolyId() + 1000000); // npctype id
				writeD(_player.getKarma() > 0 ? 1 : 0);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(0x00);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(_runSpd); // base run speed
				writeD(_walkSpd); // base walk speed
				writeD(_runSpd); // swim run speed (calculated by getter)
				writeD(_walkSpd); // swim walk speed (calculated by getter)
				writeD(_flyRunSpd); // fly run speed ?
				writeD(_flyWalkSpd); // fly walk speed ?
				writeD(_flyRunSpd);
				writeD(_flyWalkSpd);
				writeF(_moveMultiplier);
				writeF(_attackSpeedMultiplier);
				writeF(template.getCollisionRadius());
				writeF(template.getCollisionHeight());
				writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_RHAND)); // right hand weapon
				writeD(0);
				writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_LHAND)); // left hand weapon
				writeC(1); // name above char 1=true ... ??
				writeC(_player.isRunning() ? 1 : 0);
				writeC(_player.isInCombat() ? 1 : 0);
				writeC(_player.isAlikeDead() ? 1 : 0);
				
				// if(gmSeeInvis)
				// {
				writeC(0); // if the charinfo is written means receiver can see the char
				// }
				// else
				// {
				// writeC(_player.getAppearance().getInvisible() ? 1 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
				// }
				writeS(_player.getName());
				
				if (_player.getAppearance().isInvisible())
				{
					writeS("Invisible");
				}
				else
				{
					writeS(_player.getTitle());
				}
				
				writeD(0);
				writeD(0);
				writeD(0); // hmm karma ??
				
				if (_player.getAppearance().isInvisible())
				{
					writeD((_player.getAbnormalEffect() | Creature.ABNORMAL_EFFECT_STEALTH));
				}
				else
				{
					writeD(_player.getAbnormalEffect()); // C2
				}
				
				writeD(0); // C2
				writeD(0); // C2
				writeD(0); // C2
				writeD(0); // C2
				writeC(0); // C2
			}
			else
			{
				LOGGER.warning("Character " + _player.getName() + " (" + _player.getObjectId() + ") morphed in a Npc (" + _player.getPoly().getPolyId() + ") w/o template.");
			}
		}
		else
		{
			writeC(0x03);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_player.getBoat() != null ? _player.getBoat().getObjectId() : 0);
			writeD(_player.getObjectId());
			writeS(_player.getName());
			writeD(_player.getRace().ordinal());
			writeD(_player.getAppearance().isFemale() ? 1 : 0);
			
			if (_player.getClassIndex() == 0)
			{
				writeD(_player.getClassId().getId());
			}
			else
			{
				writeD(_player.getBaseClass());
			}
			
			writeD(0x00);
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(_inventory.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			
			writeD(_player.getPvpFlag());
			writeD(_player.getKarma());
			
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			
			writeD(_player.getPvpFlag());
			writeD(_player.getKarma());
			
			writeD(_runSpd); // base run speed
			writeD(_walkSpd); // base walk speed
			writeD(_runSpd); // swim run speed (calculated by getter)
			writeD(_walkSpd); // swim walk speed (calculated by getter)
			writeD(_flyRunSpd); // fly run speed ?
			writeD(_flyWalkSpd); // fly walk speed ?
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_moveMultiplier);
			writeF(_attackSpeedMultiplier);
			writeF(_player.getBaseTemplate().getCollisionRadius());
			writeF(_player.getBaseTemplate().getCollisionHeight());
			
			writeD(_player.getAppearance().getHairStyle());
			writeD(_player.getAppearance().getHairColor());
			writeD(_player.getAppearance().getFace());
			
			if (_player.getAppearance().isInvisible())
			{
				writeS("[Invisible]");
			}
			else
			{
				writeS(_player.getTitle());
			}
			
			writeD(_player.getClanId());
			writeD(_player.getClanCrestId());
			writeD(_player.getAllyId());
			writeD(_player.getAllyCrestId());
			// In UserInfo leader rights and siege flags, but here found nothing??
			// Therefore RelationChanged packet with that info is required
			writeD(0x00);
			
			writeC(_player.isSitting() ? 0 : 1); // standing = 1 sitting = 0
			writeC(_player.isRunning() ? 1 : 0); // running = 1 walking = 0
			writeC(_player.isInCombat() ? 1 : 0);
			writeC(_player.isAlikeDead() ? 1 : 0);
			
			// if(gmSeeInvis)
			// {
			writeC(0x00); // if the charinfo is written means receiver can see the char
			// }
			// else
			// {
			// writeC(_activeChar.getAppearance().getInvisible() ? 1 : 0); // invisible = 1 visible =0
			// }
			writeC(_player.getMountType()); // 1 on strider 2 on wyvern 0 no mount
			writeC(_player.getPrivateStoreType()); // 1 - sellshop
			
			writeH(_player.getCubics().size());
			for (int cubicId : _player.getCubics().keySet())
			{
				writeH(cubicId);
			}
			
			writeC(_player.isInPartyMatchRoom() ? 1 : 0);
			
			if (_player.getAppearance().isInvisible())
			{
				writeD((_player.getAbnormalEffect() | Creature.ABNORMAL_EFFECT_STEALTH));
			}
			else
			{
				writeD(_player.getAbnormalEffect());
			}
			
			writeC(_player.getRecomLeft());
			writeH(_player.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
			writeD(_player.getClassId().getId());
			
			writeD(_player.getMaxCp());
			writeD((int) _player.getCurrentCp());
			writeC(_player.isMounted() ? 0 : _player.getEnchantEffect());
			
			if (_player.getTeam() == 1)
			{
				writeC(0x01); // team circle around feet 1= Blue, 2 = red
			}
			else if (_player.getTeam() == 2)
			{
				writeC(0x02); // team circle around feet 1= Blue, 2 = red
			}
			else
			{
				writeC(0x00); // team circle around feet 1= Blue, 2 = red
			}
			
			writeD(_player.getClanCrestLargeId());
			writeC(_player.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
			writeC((_player.isHero() || (_player.isGM() && Config.GM_HERO_AURA) || _player.isPVPHero()) ? 1 : 0); // Hero Aura
			
			writeC(_player.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
			writeD(_player.getFishX());
			writeD(_player.getFishY());
			writeD(_player.getFishZ());
			
			writeD(_player.getAppearance().getNameColor());
		}
	}
}
