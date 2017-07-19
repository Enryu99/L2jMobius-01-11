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
package com.l2jmobius.gameserver.model.actor.appearance;

public class PcAppearance
{
	// =========================================================
	// Data Field
	private byte _Face;
	private byte _HairColor;
	private byte _HairStyle;
	
	private boolean _Sex; // Female true(1)
	
	/** true if the player is invisible */
	private boolean _invisible = false;
	/** The hexadecimal Color of players name (white is 0xFFFFFF) */
	private int _nameColor = 0xFFFFFF;
	/** The hexadecimal Color of players name (white is 0xFFFFFF) */
	private int _titleColor = 0xFFFF77;
	
	// =========================================================
	// Constructor
	public PcAppearance(byte Face, byte HColor, byte HStyle, boolean Sex)
	{
		_Face = Face;
		_HairColor = HColor;
		_HairStyle = HStyle;
		_Sex = Sex;
	}
	
	// =========================================================
	// Property - Public
	public final byte getFace()
	{
		return _Face;
	}
	
	public final void setFace(int value)
	{
		_Face = (byte) value;
	}
	
	public final byte getHairColor()
	{
		return _HairColor;
	}
	
	public final void setHairColor(int value)
	{
		_HairColor = (byte) value;
	}
	
	public final byte getHairStyle()
	{
		return _HairStyle;
	}
	
	public final void setHairStyle(int value)
	{
		_HairStyle = (byte) value;
	}
	
	/**
	 * @return true if char is female
	 */
	public final boolean getSex()
	{
		return _Sex;
	}
	
	/**
	 * @param isfemale
	 */
	public final void setSex(boolean isfemale)
	{
		_Sex = isfemale;
	}
	
	public void setInvisible()
	{
		_invisible = true;
	}
	
	public void setVisible()
	{
		_invisible = false;
	}
	
	public boolean getInvisible()
	{
		return _invisible;
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	public void setNameColor(int nameColor)
	{
		_nameColor = nameColor;
	}
	
	public void setNameColor(int red, int green, int blue)
	{
		_nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	public void setTitleColor(int titleColor)
	{
		_titleColor = titleColor;
	}
	
	public void setTitleColor(int red, int green, int blue)
	{
		_titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
}