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

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.data.xml.impl.PlayerTemplateData;
import com.l2jmobius.gameserver.model.base.ClassId;
import com.l2jmobius.gameserver.network.serverpackets.NewCharacterSuccess;

/**
 * @author Zoey76
 */
public final class NewCharacter extends L2GameClientPacket
{
	private static final String _C__13_NEWCHARACTER = "[C] 13 NewCharacter";
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
		{
			_log.fine(_C__13_NEWCHARACTER);
		}
		
		final NewCharacterSuccess ct = new NewCharacterSuccess();
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.FIGHTER)); // Human Figther
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.MAGE)); // Human Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.ELVEN_FIGHTER)); // Elven Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.ELVEN_MAGE)); // Elven Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.DARK_FIGHTER)); // Dark Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.DARK_MAGE)); // Dark Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.ORC_FIGHTER)); // Orc Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.ORC_MAGE)); // Orc Mystic
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.DWARVEN_FIGHTER)); // Dwarf Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.MALE_SOLDIER)); // Male Kamael Soldier
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.FEMALE_SOLDIER)); // Female Kamael Soldier
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.ERTHEIA_FIGHTER)); // Ertheia Fighter
		ct.addChar(PlayerTemplateData.getInstance().getTemplate(ClassId.ERTHEIA_WIZARD)); // Ertheia Wizard
		sendPacket(ct);
	}
	
	@Override
	public String getType()
	{
		return _C__13_NEWCHARACTER;
	}
}
