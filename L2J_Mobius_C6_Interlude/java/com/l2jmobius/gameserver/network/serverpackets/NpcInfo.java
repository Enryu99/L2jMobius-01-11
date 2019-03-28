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

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.model.actor.Creature;
import com.l2jmobius.gameserver.model.actor.Summon;
import com.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import com.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.PetInstance;
import com.l2jmobius.gameserver.model.actor.instance.SummonInstance;

/**
 * @version $Revision: 1.7.2.4.2.9 $ $Date: 2005/04/11 10:05:54 $
 */
public class NpcInfo extends GameServerPacket
{
	private Creature _creature;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private int _idTemplate;
	private boolean _isAttackable;
	private boolean _isSummoned;
	private int _mAtkSpd;
	private int _pAtkSpd;
	private int _runSpd;
	private int _walkSpd;
	private int _swimRunSpd;
	private int _swimWalkSpd;
	private int _flRunSpd;
	private int _flWalkSpd;
	private int _flyRunSpd;
	private int _flyWalkSpd;
	private int _rhand;
	private int _lhand;
	private int _collisionHeight;
	private int _collisionRadius;
	private String _name = "";
	private String _title = "";
	
	/**
	 * Instantiates a new npc info.
	 * @param cha the cha
	 * @param attacker the attacker
	 */
	public NpcInfo(NpcInstance cha, Creature attacker)
	{
		/*
		 * if(cha.getMxcPoly() != null) { attacker.sendPacket(new MxCPolyInfo(cha)); return; }
		 */
		if (cha.getCustomNpcInstance() != null)
		{
			attacker.sendPacket(new CustomNpcInfo(cha));
			attacker.broadcastPacket(new FinishRotation(cha));
			return;
		}
		_creature = cha;
		_idTemplate = cha.getTemplate().idTemplate;
		_isAttackable = cha.isAutoAttackable(attacker);
		_rhand = cha.getRightHandItem();
		_lhand = cha.getLeftHandItem();
		_isSummoned = false;
		_collisionHeight = cha.getCollisionHeight();
		_collisionRadius = cha.getCollisionRadius();
		if (cha.getTemplate().serverSideName)
		{
			_name = cha.getTemplate().name;
		}
		
		if (Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion())
		{
			_title = Config.L2JMOD_CHAMP_TITLE;
		}
		else if (cha.getTemplate().serverSideTitle)
		{
			_title = cha.getTemplate().title;
		}
		else
		{
			_title = cha.getTitle();
		}
		
		if (Config.SHOW_NPC_LVL && (_creature instanceof MonsterInstance))
		{
			String t = "Lv " + cha.getLevel() + (cha.getAggroRange() > 0 ? "*" : "");
			if (_title != null)
			{
				t += " " + _title;
			}
			
			_title = t;
		}
		
		_x = _creature.getX();
		_y = _creature.getY();
		_z = _creature.getZ();
		_heading = _creature.getHeading();
		_mAtkSpd = _creature.getMAtkSpd();
		_pAtkSpd = _creature.getPAtkSpd();
		_runSpd = _creature.getRunSpeed();
		_walkSpd = _creature.getWalkSpeed();
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}
	
	/**
	 * Instantiates a new npc info.
	 * @param cha the cha
	 * @param attacker the attacker
	 */
	public NpcInfo(Summon cha, Creature attacker)
	{
		_creature = cha;
		_idTemplate = cha.getTemplate().idTemplate;
		_isAttackable = cha.isAutoAttackable(attacker); // (cha.getKarma() > 0);
		_rhand = 0;
		_lhand = 0;
		_isSummoned = cha.isShowSummonAnimation();
		_collisionHeight = _creature.getTemplate().collisionHeight;
		_collisionRadius = _creature.getTemplate().collisionRadius;
		if (cha.getTemplate().serverSideName || (cha instanceof PetInstance) || (cha instanceof SummonInstance))
		{
			_name = _creature.getName();
			_title = cha.getTitle();
		}
		
		_x = _creature.getX();
		_y = _creature.getY();
		_z = _creature.getZ();
		_heading = _creature.getHeading();
		_mAtkSpd = _creature.getMAtkSpd();
		_pAtkSpd = _creature.getPAtkSpd();
		_runSpd = _creature.getRunSpeed();
		_walkSpd = _creature.getWalkSpeed();
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jmobius.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		if (_creature == null)
		{
			return;
		}
		
		if (_creature instanceof Summon)
		{
			if ((((Summon) _creature).getOwner() != null) && ((Summon) _creature).getOwner().getAppearance().getInvisible())
			{
				return;
			}
		}
		writeC(0x16);
		writeD(_creature.getObjectId());
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
		writeD(_swimRunSpd/* 0x32 */); // swimspeed
		writeD(_swimWalkSpd/* 0x32 */); // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(1.1/* _activeChar.getProperMultiplier() */);
		// writeF(1/*_activeChar.getAttackSpeedMultiplier()*/);
		writeF(_pAtkSpd / 277.478340719);
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD(_rhand); // right hand weapon
		writeD(0);
		writeD(_lhand); // left hand weapon
		writeC(1); // name above char 1=true ... ??
		writeC(_creature.isRunning() ? 1 : 0);
		writeC(_creature.isInCombat() ? 1 : 0);
		writeC(_creature.isAlikeDead() ? 1 : 0);
		writeC(_isSummoned ? 2 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
		writeS(_name);
		writeS(_title);
		
		if (_creature instanceof Summon)
		{
			writeD(0x01);// Title color 0=client default
			writeD(((Summon) _creature).getPvpFlag());
			writeD(((Summon) _creature).getKarma());
		}
		else
		{
			writeD(0);
			writeD(0);
			writeD(0);
		}
		
		writeD(_creature.getAbnormalEffect()); // C2
		writeD(0000); // C2
		writeD(0000); // C2
		writeD(0000); // C2
		writeD(0000); // C2
		writeC(0000); // C2
		
		writeC(0x00); // C3 team circle 1-blue, 2-red
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD(0x00); // C4
		writeD(0x00); // C6
	}
}
