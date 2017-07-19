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

import com.l2jmobius.gameserver.model.Inventory;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * TODO Add support for Eval. Score dddddSdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddffffddddSddd rev420 dddddSdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddffffddddSdddcccddhh rev478
 * dddddSdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddffffddddSdddcccddhhddd rev551
 * @version $Revision: 1.2.2.2.2.8 $ $Date: 2005/03/27 15:29:39 $
 */
public class GMViewCharacterInfo extends L2GameServerPacket
{
	private static final String _S__04_USERINFO = "[S] 8F GMViewCharacterInfo";
	private final L2PcInstance _cha;
	private final int _runSpd, _walkSpd;
	private final float moveMultiplier;
	
	/**
	 * @param cha
	 */
	public GMViewCharacterInfo(L2PcInstance cha)
	{
		_cha = cha;
		moveMultiplier = _cha.getMovementSpeedMultiplier();
		_runSpd = (int) (_cha.getRunSpeed() / moveMultiplier);
		_walkSpd = (int) (_cha.getWalkSpeed() / moveMultiplier);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x8f);
		
		writeD(_cha.getX());
		writeD(_cha.getY());
		writeD(_cha.getZ());
		writeD(_cha.getHeading());
		writeD(_cha.getObjectId());
		writeS(_cha.getName());
		writeD(_cha.getRace().ordinal());
		writeD(_cha.getAppearance().getSex() ? 1 : 0);
		writeD(_cha.getClassId().getId());
		writeD(_cha.getLevel());
		writeD((int) _cha.getExp());
		writeD(_cha.getSTR());
		writeD(_cha.getDEX());
		writeD(_cha.getCON());
		writeD(_cha.getINT());
		writeD(_cha.getWIT());
		writeD(_cha.getMEN());
		writeD(_cha.getMaxHp());
		writeD((int) _cha.getCurrentHp());
		writeD(_cha.getMaxMp());
		writeD((int) _cha.getCurrentMp());
		writeD(_cha.getSp());
		writeD(_cha.getCurrentLoad());
		writeD(_cha.getMaxLoad());
		
		writeD(0x28); // unknown
		
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_UNDER));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
		
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
		writeD(_cha.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
		
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_UNDER));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_REAR));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_NECK));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
		
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
		writeD(_cha.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
		
		writeD(_cha.getPAtk(null));
		writeD(_cha.getPAtkSpd());
		writeD(_cha.getPDef(null));
		writeD(_cha.getEvasionRate(null));
		writeD(_cha.getAccuracy());
		writeD(_cha.getCriticalHit(null, null));
		writeD(_cha.getMAtk(null, null));
		
		writeD(_cha.getMAtkSpd());
		writeD(_cha.getPAtkSpd());
		
		writeD(_cha.getMDef(null, null));
		
		writeD(_cha.getPvpFlag()); // 0-non-pvp 1-pvp = violett name
		writeD(_cha.getKarma());
		
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd); // swimspeed
		writeD(_walkSpd); // swimspeed
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeF(moveMultiplier);
		writeF(_cha.getAttackSpeedMultiplier()); // 2.9);//
		writeF(_cha.getTemplate().collisionRadius); // scale
		writeF(_cha.getTemplate().collisionHeight); // y offset ??!? fem dwarf 4033
		writeD(_cha.getAppearance().getHairStyle());
		writeD(_cha.getAppearance().getHairColor());
		writeD(_cha.getAppearance().getFace());
		writeD(_cha.isGM() ? 0x01 : 0x00); // builder level
		
		writeS(_cha.getTitle());
		writeD(_cha.getClanId()); // pledge id
		writeD(_cha.getClanCrestId()); // pledge crest id
		writeD(_cha.getAllyId()); // ally id
		writeC(_cha.getMountType()); // ??
		writeC(_cha.getPrivateStoreType()); // ??
		writeC(_cha.hasDwarvenCraft() ? 1 : 0); // ??
		writeD(_cha.getPkKills());
		writeD(_cha.getPvpKills());
		
		writeH(_cha.getRecomLeft());
		writeH(_cha.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
		writeD(_cha.getClassId().getId());
		
		writeD(_cha.getMaxCp());
		writeD((int) _cha.getCurrentCp());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__04_USERINFO;
	}
}