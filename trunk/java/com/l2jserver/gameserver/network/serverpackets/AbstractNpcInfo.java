/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TrapInstance;
import com.l2jserver.gameserver.model.skills.AbnormalVisualEffect;
import com.l2jserver.gameserver.model.zone.ZoneId;

public abstract class AbstractNpcInfo extends L2GameServerPacket
{
	protected int _x, _y, _z, _heading;
	protected int _idTemplate;
	protected boolean _isAttackable, _isSummoned;
	protected int _mAtkSpd, _pAtkSpd;
	protected final int _runSpd, _walkSpd;
	protected final int _swimRunSpd, _swimWalkSpd;
	protected final int _flyRunSpd, _flyWalkSpd;
	protected double _moveMultiplier;
	
	protected int _rhand, _lhand, _chest, _enchantEffect;
	protected double _collisionHeight, _collisionRadius;
	protected String _name = "";
	protected String _title = "";
	
	public AbstractNpcInfo(L2Character cha)
	{
		_isSummoned = cha.isShowSummonAnimation();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_heading = cha.getHeading();
		_mAtkSpd = cha.getMAtkSpd();
		_pAtkSpd = cha.getPAtkSpd();
		_moveMultiplier = cha.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(cha.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(cha.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(cha.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = cha.isFlying() ? _runSpd : 0;
		_flyWalkSpd = cha.isFlying() ? _walkSpd : 0;
	}
	
	public static class TrapInfo extends AbstractNpcInfo
	{
		private final L2TrapInstance _trap;
		
		public TrapInfo(L2TrapInstance cha, L2Character attacker)
		{
			super(cha);
			
			_trap = cha;
			_idTemplate = cha.getTemplate().getDisplayId();
			_isAttackable = cha.isAutoAttackable(attacker);
			_rhand = 0;
			_lhand = 0;
			_collisionHeight = _trap.getTemplate().getfCollisionHeight();
			_collisionRadius = _trap.getTemplate().getfCollisionRadius();
			if (cha.getTemplate().isUsingServerSideName())
			{
				_name = cha.getName();
			}
			_title = cha.getOwner() != null ? cha.getOwner().getName() : "";
		}
		
		@Override
		protected void writeImpl()
		{
			writeC(0x0c);
			writeD(_trap.getObjectId());
			writeD(_idTemplate + 1000000); // npctype id
			writeD(_isAttackable ? 1 : 0);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(0x00);
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_swimRunSpd);
			writeD(_swimWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_moveMultiplier);
			writeF(_trap.getAttackSpeedMultiplier());
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_rhand); // right hand weapon
			writeD(_chest);
			writeD(_lhand); // left hand weapon
			writeC(1); // name above char 1=true ... ??
			writeC(1);
			writeC(_trap.isInCombat() ? 1 : 0);
			writeC(_trap.isAlikeDead() ? 1 : 0);
			writeC(_isSummoned ? 2 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
			writeD(-1); // High Five NPCString ID
			writeS(_name);
			writeD(-1); // High Five NPCString ID
			writeS(_title);
			writeD(0x00); // title color 0 = client default
			
			writeD(_trap.getPvpFlag());
			writeD(_trap.getKarma());
			
			writeD(_trap.isInvisible() ? _trap.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _trap.getAbnormalVisualEffects());
			writeD(0x00); // clan id
			writeD(0x00); // crest id
			writeD(0000); // C2
			writeD(0000); // C2
			writeC(0000); // C2
			
			writeC(_trap.getTeam().getId());
			
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(0x00); // C4
			writeD(0x00); // C6
			writeD(0x00);
			writeD(0);// CT1.5 Pet form and skills
			writeC(0x01);
			writeC(0x01);
			writeD(0x00);
		}
	}
	
	/**
	 * Packet for summons.
	 */
	public static class SummonInfo extends AbstractNpcInfo
	{
		private final L2Summon _summon;
		private final int _form;
		private final int _val;
		
		public SummonInfo(L2Summon cha, L2Character attacker, int val)
		{
			super(cha);
			_summon = cha;
			_val = val;
			_form = cha.getFormId();
			
			_isAttackable = cha.isAutoAttackable(attacker);
			_rhand = cha.getWeapon();
			_lhand = 0;
			_chest = cha.getArmor();
			_enchantEffect = cha.getTemplate().getWeaponEnchant();
			_name = cha.getName();
			_title = (cha.getOwner() != null) && cha.getOwner().isOnline() ? cha.getOwner().getName() : "";
			_idTemplate = cha.getTemplate().getDisplayId();
			_collisionHeight = cha.getTemplate().getfCollisionHeight();
			_collisionRadius = cha.getTemplate().getfCollisionRadius();
			setInvisible(cha.isInvisible());
		}
		
		@Override
		protected void writeImpl()
		{
			boolean gmSeeInvis = false;
			if (isInvisible())
			{
				final L2PcInstance activeChar = getClient().getActiveChar();
				if ((activeChar != null) && activeChar.canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS))
				{
					gmSeeInvis = true;
				}
			}
			
			writeC(0x0c);
			writeD(_summon.getObjectId());
			writeD(_idTemplate + 1000000); // npctype id
			writeD(_isAttackable ? 1 : 0);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(0x00);
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_swimRunSpd);
			writeD(_swimWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_moveMultiplier);
			writeF(_summon.getAttackSpeedMultiplier());
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_rhand); // right hand weapon
			writeD(_chest);
			writeD(_lhand); // left hand weapon
			writeC(0x01); // name above char 1=true ... ??
			writeC(0x01); // always running 1=running 0=walking
			writeC(_summon.isInCombat() ? 1 : 0);
			writeC(_summon.isAlikeDead() ? 1 : 0);
			writeC(_val); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
			writeD(-1); // High Five NPCString ID
			writeS(_name);
			writeD(-1); // High Five NPCString ID
			writeS(_title);
			writeD(0x01);// Title color 0=client default
			
			writeD(_summon.getPvpFlag());
			writeD(_summon.getKarma());
			
			writeD(gmSeeInvis ? _summon.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : _summon.getAbnormalVisualEffects());
			
			writeD(0x00); // clan id
			writeD(0x00); // crest id
			writeD(0x00); // C2
			writeD(0x00); // C2
			writeC(_summon.isInsideZone(ZoneId.WATER) ? 1 : _summon.isFlying() ? 2 : 0); // C2
			
			writeC(_summon.getTeam().getId());
			
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_enchantEffect); // C4
			writeD(0x00); // C6
			writeD(0x00);
			writeD(_form); // CT1.5 Pet form and skills
			writeC(0x01);
			writeC(0x01);
			writeD(_summon.getAbnormalVisualEffectSpecial());
		}
	}
}
