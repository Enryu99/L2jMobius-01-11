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

import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.NpcTable;
import com.l2jmobius.gameserver.model.Inventory;
import com.l2jmobius.gameserver.model.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.templates.L2NpcTemplate;

/**
 * 0000: 03 32 15 00 00 44 fe 00 00 80 f1 ff ff 00 00 00 .2...D..........
 * <p>
 * 0010: 00 6b b4 c0 4a 45 00 6c 00 6c 00 61 00 6d 00 69 .k..JE.l.l.a.m.i
 * <p>
 * 0020: 00 00 00 01 00 00 00 01 00 00 00 12 00 00 00 00 ................
 * <p>
 * 0030: 00 00 00 2a 00 00 00 42 00 00 00 71 02 00 00 31 ...*...B...q...1
 * <p>
 * 0040: 00 00 00 18 00 00 00 1f 00 00 00 25 00 00 00 00 ...........%....
 * <p>
 * 0050: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f9 ................
 * <p>
 * 0060: 00 00 00 b3 01 00 00 00 00 00 00 00 00 00 00 7d ...............}
 * <p>
 * 0070: 00 00 00 5a 00 00 00 32 00 00 00 32 00 00 00 00 ...Z...2...2....
 * <p>
 * 0080: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 67 ...............g
 * <p>
 * 0090: 66 66 66 66 66 f2 3f 5f 63 97 a8 de 1a f9 3f 00 fffff.?_c.....?.
 * <p>
 * 00a0: 00 00 00 00 00 1e 40 00 00 00 00 00 00 37 40 01 .............7..
 * <p>
 * 00b0: 00 00 00 01 00 00 00 01 00 00 00 00 00 c1 0c 00 ................
 * <p>
 * 00c0: 00 00 00 00 00 00 00 00 00 01 01 00 00 00 00 00 ................
 * <p>
 * 00d0: 00 00
 * <p>
 * <p>
 * dddddSdddddddddddddddddddddddddddffffdddSdddccccccc (h)
 * <p>
 * dddddSdddddddddddddddddddddddddddffffdddSdddddccccccch dddddSddddddddddddddddddddddddddddffffdddSdddddccccccch (h) c (dchd) ddc dcc c cddd d
 * @version $Revision: 1.7.2.6.2.11 $ $Date: 2005/04/11 10:05:54 $
 */
public class CharInfo extends L2GameServerPacket
{
	private static final Logger _log = Logger.getLogger(CharInfo.class.getName());
	
	private static final String _S__03_CHARINFO = "[S] 03 CharInfo";
	private final L2PcInstance _cha;
	private final Inventory _inv;
	private final int _x, _y, _z, _heading;
	private final int _mAtkSpd, _pAtkSpd;
	private final int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd;
	
	private int _flRunSpd;
	
	private int _flWalkSpd;
	
	private int _flyRunSpd;
	
	private int _flyWalkSpd;
	private final float moveMultiplier;
	
	/**
	 * @param cha
	 */
	public CharInfo(L2PcInstance cha)
	{
		_cha = cha;
		_inv = cha.getInventory();
		_x = _cha.getX();
		_y = _cha.getY();
		_z = _cha.getZ();
		_heading = _cha.getHeading();
		_mAtkSpd = _cha.getMAtkSpd();
		_pAtkSpd = _cha.getPAtkSpd();
		moveMultiplier = _cha.getMovementSpeedMultiplier();
		_runSpd = (int) (_cha.getRunSpeed() / moveMultiplier);
		_walkSpd = (int) (_cha.getWalkSpeed() / moveMultiplier);
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
		
	}
	
	@Override
	protected final void writeImpl()
	{
		boolean gmSeeInvis = false;
		final L2PcInstance tmp = getClient().getActiveChar();
		if (tmp != null)
		{
			if (_cha.getAppearance().getInvisible())
			{
				if (tmp.isGM())
				{
					gmSeeInvis = true;
				}
				else
				{
					return;
				}
			}
			else if (_cha.isInOlympiadMode())
			{
				if (!tmp.isGM() && !tmp.isInOlympiadMode() && !tmp.inObserverMode())
				{
					return;
				}
			}
			else if (_cha.getEventTeam() > 0)
			{
				if (!tmp.isGM() && !tmp.inObserverMode() && !(tmp.getEventTeam() > 0))
				{
					return;
				}
			}
		}
		
		if (_cha.getPoly().isMorphed())
		{
			
			final L2NpcTemplate template = NpcTable.getInstance().getTemplate(_cha.getPoly().getPolyId());
			
			if (template == null)
			
			{
				
				_log.warning("Character " + _cha.getName() + " (" + _cha.getObjectId() + ") morphed in a Npc (" + _cha.getPoly().getPolyId() + ") w/o template.");
				
				return;
				
			}
			
			writeC(0x16);
			writeD(_cha.getObjectId());
			writeD(_cha.getPoly().getPolyId() + 1000000); // npctype id
			writeD(_cha.getKarma() > 0 ? 1 : 0);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(0x00);
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_swimRunSpd); // swimspeed
			writeD(_swimWalkSpd); // swimspeed
			writeD(_flRunSpd);
			writeD(_flWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_cha.getMovementSpeedMultiplier());
			writeF(_cha.getAttackSpeedMultiplier());
			writeF(template.collisionRadius);
			writeF(template.collisionHeight);
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND)); // right hand weapon
			writeD(0);
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND)); // left hand weapon
			writeC(1); // name above char 1=true ... ??
			writeC(_cha.isRunning() ? 1 : 0);
			writeC(_cha.isInCombat() ? 1 : 0);
			writeC(_cha.isAlikeDead() ? 1 : 0);
			
			if (gmSeeInvis)
			{
				writeC(0);
			}
			else
			{
				writeC(_cha.getAppearance().getInvisible() ? 1 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
			}
			
			writeS(_cha.getName());
			
			if (gmSeeInvis)
			{
				writeS("Invisible");
			}
			else
			{
				writeS(_cha.getTitle());
			}
			
			writeD(0);
			writeD(0);
			writeD(0000); // hmm karma ??
			
			writeH(_cha.getAbnormalEffect()); // C2
			writeH(0x00); // C2
			writeD(0); // C2
			writeD(0); // C2
			writeD(0); // C2
			writeD(0); // C2
			writeC(0); // C2
		}
		else
		{
			writeC(0x03);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(_cha.getObjectId());
			
			writeS(_cha.getName());
			writeD(_cha.getRace().ordinal());
			writeD(_cha.getAppearance().getSex() ? 1 : 0);
			
			if (_cha.getClassIndex() == 0)
			{
				writeD(_cha.getClassId().getId());
			}
			else
			{
				writeD(_cha.getBaseClass());
			}
			
			writeD(0); // unknown, maybe underwear?
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			
			writeD(_cha.getPvpFlag());
			writeD(_cha.getKarma());
			
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			
			writeD(_cha.getPvpFlag());
			writeD(_cha.getKarma());
			
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_swimRunSpd); // swimspeed
			writeD(_swimWalkSpd); // swimspeed
			writeD(_flRunSpd);
			writeD(_flWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_cha.getMovementSpeedMultiplier());
			writeF(_cha.getAttackSpeedMultiplier());
			
			final L2Summon pet = _cha.getPet();
			if ((_cha.getMountType() != 0) && (pet != null))
			{
				writeF(pet.getTemplate().collisionRadius);
				writeF(pet.getTemplate().collisionHeight);
			}
			else
			{
				writeF(_cha.getCollisionRadius());
				writeF(_cha.getCollisionHeight());
			}
			
			writeD(_cha.getAppearance().getHairStyle());
			writeD(_cha.getAppearance().getHairColor());
			writeD(_cha.getAppearance().getFace());
			
			if (gmSeeInvis)
			{
				writeS("Invisible");
			}
			else
			{
				writeS(_cha.getTitle());
			}
			
			writeD(_cha.getClanId());
			writeD(_cha.getClanCrestId());
			writeD(_cha.getAllyId());
			writeD(_cha.getAllyCrestId());
			// In UserInfo leader rights and siege flags, but here found nothing??
			// Therefore RelationChanged packet with that info is required
			writeD(0);
			
			writeC(_cha.isSitting() ? 0 : 1); // standing = 1 sitting = 0
			writeC(_cha.isRunning() ? 1 : 0); // running = 1 walking = 0
			writeC(_cha.isInCombat() ? 1 : 0);
			writeC(_cha.isAlikeDead() ? 1 : 0);
			
			if (gmSeeInvis)
			{
				writeC(0);
			}
			else
			{
				writeC(_cha.getAppearance().getInvisible() ? 1 : 0); // invisible = 1 visible =0
			}
			
			writeC(_cha.getMountType()); // 1 on strider 2 on wyvern 0 no mount
			writeC(_cha.getPrivateStoreType()); // 1 - sellshop
			
			writeH(_cha.getCubics().size());
			for (final int id : _cha.getCubics().keySet())
			{
				writeH(id);
			}
			
			writeC(_cha.isLookingForParty() ? 1 : 0);
			
			writeD(_cha.getAbnormalEffect());
			
			writeC(_cha.getRecomLeft()); // Changed by Thorgrim
			writeH(_cha.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
			writeD(_cha.getMountNpcId() + 1000000);
			
			writeD(_cha.getClassId().getId());
			
			writeD(0x00); // ??
			
			writeC(_cha.isMounted() ? 0 : _cha.getEnchantEffect());
			
			if (_cha.getEventTeam() > 0)
			{
				writeC(_cha.getEventTeam()); // team circle around feet 1= Blue, 2 = red
			}
			else
			{
				writeC(_cha.getAuraColor()); // team circle around feet 1= Blue, 2 = red
			}
			
			writeD(_cha.getClanCrestLargeId());
			writeC(_cha.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
			writeC((_cha.isHero() || (_cha.isGM() && Config.GM_HERO_AURA)) ? 1 : 0); // Hero Aura
			
			writeC(_cha.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
			writeD(_cha.getFishx());
			writeD(_cha.getFishy());
			writeD(_cha.getFishz());
			
			writeD(_cha.getAppearance().getNameColor());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__03_CHARINFO;
	}
}