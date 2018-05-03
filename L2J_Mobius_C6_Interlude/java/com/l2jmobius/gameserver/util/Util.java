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
package com.l2jmobius.gameserver.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.l2jmobius.commons.concurrent.ThreadPool;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;

/**
 * General Utility functions related to game server.
 */
public final class Util
{
	private static final char[] ILLEGAL_CHARACTERS =
	{
		'/',
		'\n',
		'\r',
		'\t',
		'\0',
		'\f',
		'`',
		'?',
		'*',
		'\\',
		'<',
		'>',
		'|',
		'\"',
		':'
	};
	
	public static void handleIllegalPlayerAction(L2PcInstance actor, String message, int punishment)
	{
		ThreadPool.schedule(new IllegalPlayerAction(actor, message, punishment), 5000);
	}
	
	public static String getRelativePath(File base, File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}
	
	/**
	 * @param obj1
	 * @param obj2
	 * @return degree value of object 2 to the horizontal line with object 1 being the origin
	 */
	public static double calculateAngleFrom(L2Object obj1, L2Object obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	/**
	 * @param obj1X
	 * @param obj1Y
	 * @param obj2X
	 * @param obj2Y
	 * @return degree value of object 2 to the horizontal line with object 1 being the origin
	 */
	public static double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj1Y - obj2Y, obj1X - obj2X));
		if (angleTarget <= 0)
		{
			angleTarget += 360;
		}
		return angleTarget;
	}
	
	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2)
	{
		return calculateDistance(x1, y1, 0, x2, y2, 0, false);
	}
	
	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis)
	{
		final double dx = (double) x1 - x2;
		final double dy = (double) y1 - y2;
		
		if (includeZAxis)
		{
			final double dz = z1 - z2;
			return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		}
		return Math.sqrt((dx * dx) + (dy * dy));
	}
	
	public static double calculateDistance(L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if ((obj1 == null) || (obj2 == null))
		{
			return 1000000;
		}
		return calculateDistance(obj1.getPosition().getX(), obj1.getPosition().getY(), obj1.getPosition().getZ(), obj2.getPosition().getX(), obj2.getPosition().getY(), obj2.getPosition().getZ(), includeZAxis);
	}
	
	/**
	 * Capitalizes the first letter of a string, and returns the result.<BR>
	 * (Based on ucfirst() function of PHP)
	 * @param str
	 * @return String containing the modified string.
	 */
	public static String capitalizeFirst(String str)
	{
		str = str.trim();
		
		if ((str.length() > 0) && Character.isLetter(str.charAt(0)))
		{
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		}
		
		return str;
	}
	
	/**
	 * Capitalizes the first letter of every "word" in a string.<BR>
	 * (Based on ucwords() function of PHP)
	 * @param str
	 * @return String containing the modified string.
	 */
	public static String capitalizeWords(String str)
	{
		final char[] charArray = str.toCharArray();
		String result = "";
		
		// Capitalize the first letter in the given string!
		charArray[0] = Character.toUpperCase(charArray[0]);
		
		for (int i = 0; i < charArray.length; i++)
		{
			if (Character.isWhitespace(charArray[i]))
			{
				charArray[i + 1] = Character.toUpperCase(charArray[i + 1]);
			}
			
			result += Character.toString(charArray[i]);
		}
		
		return result;
	}
	
	// Micht: Removed this because UNUSED
	/*
	 * public static boolean checkIfInRange(int range, int x1, int y1, int x2, int y2) { return checkIfInRange(range, x1, y1, 0, x2, y2, 0, false); } public static boolean checkIfInRange(int range, int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis) { if (includeZAxis) { return
	 * ((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) + (z1 - z2)*(z1 - z2)) <= range * range; } else { return ((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2)) <= range * range; } } public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis) { if (obj1 == null || obj2
	 * == null) return false; return checkIfInRange(range, obj1.getPosition().getX(), obj1.getPosition().getY(), obj1.getPosition().getZ(), obj2.getPosition().getX(), obj2.getPosition().getY(), obj2.getPosition().getZ(), includeZAxis); }
	 */
	public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if ((obj1 == null) || (obj2 == null))
		{
			return false;
		}
		if (range == -1)
		{
			return true; // not limited
		}
		
		int rad = 0;
		if (obj1 instanceof L2Character)
		{
			rad += ((L2Character) obj1).getTemplate().collisionRadius;
		}
		if (obj2 instanceof L2Character)
		{
			rad += ((L2Character) obj2).getTemplate().collisionRadius;
		}
		
		final double dx = obj1.getX() - obj2.getX();
		final double dy = obj1.getY() - obj2.getY();
		
		if (includeZAxis)
		{
			final double dz = obj1.getZ() - obj2.getZ();
			final double d = (dx * dx) + (dy * dy) + (dz * dz);
			
			return d <= ((range * range) + (2 * range * rad) + (rad * rad));
		}
		final double d = (dx * dx) + (dy * dy);
		
		return d <= ((range * range) + (2 * range * rad) + (rad * rad));
	}
	
	public static double convertHeadingToDegree(int heading)
	{
		if (heading == 0)
		{
			return 360D;
		}
		return (9.0D * heading) / 1610.0D; // = 360.0 * (heading / 64400.0)
	}
	
	/**
	 * Returns the number of "words" in a given string.
	 * @param str
	 * @return int numWords
	 */
	public static int countWords(String str)
	{
		return str.trim().split(" ").length;
	}
	
	/**
	 * Returns a delimited string for an given array of string elements.<BR>
	 * (Based on implode() in PHP)
	 * @param strArray
	 * @param strDelim
	 * @return String implodedString
	 */
	public static String implodeString(String[] strArray, String strDelim)
	{
		String result = "";
		
		for (String strValue : strArray)
		{
			result += strValue + strDelim;
		}
		
		return result;
	}
	
	/**
	 * Returns a delimited string for an given collection of string elements.<BR>
	 * (Based on implode() in PHP)
	 * @param strCollection
	 * @param strDelim
	 * @return String implodedString
	 */
	public static String implodeString(Collection<String> strCollection, String strDelim)
	{
		return implodeString(strCollection.toArray(new String[strCollection.size()]), strDelim);
	}
	
	/**
	 * Returns the rounded value of val to specified number of digits after the decimal point.<BR>
	 * (Based on round() in PHP)
	 * @param val
	 * @param numPlaces
	 * @return float roundedVal
	 */
	public static float roundTo(float val, int numPlaces)
	{
		if (numPlaces <= 1)
		{
			return Math.round(val);
		}
		
		final float exponent = (float) Math.pow(10, numPlaces);
		
		return Math.round(val * exponent) / exponent;
	}
	
	public static boolean isAlphaNumeric(String text)
	{
		boolean result = true;
		final char[] chars = text.toCharArray();
		for (char aChar : chars)
		{
			if (!Character.isLetterOrDigit(aChar))
			{
				result = false;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Return amount of adena formatted with "," delimiter
	 * @param amount
	 * @return String formatted adena amount
	 */
	public static String formatAdena(int amount)
	{
		String s = "";
		int rem = amount % 1000;
		s = Integer.toString(rem);
		amount = (amount - rem) / 1000;
		while (amount > 0)
		{
			if (rem < 99)
			{
				s = '0' + s;
			}
			if (rem < 9)
			{
				s = '0' + s;
			}
			rem = amount % 1000;
			s = rem + "," + s;
			amount = (amount - rem) / 1000;
		}
		return s;
	}
	
	public static String reverseColor(String color)
	{
		final char[] ch1 = color.toCharArray();
		final char[] ch2 = new char[6];
		ch2[0] = ch1[4];
		ch2[1] = ch1[5];
		ch2[2] = ch1[2];
		ch2[3] = ch1[3];
		ch2[4] = ch1[0];
		ch2[5] = ch1[1];
		return new String(ch2);
	}
	
	/**
	 * converts a given time from minutes -> miliseconds
	 * @param minutesToConvert
	 * @return
	 */
	public static int convertMinutesToMiliseconds(int minutesToConvert)
	{
		return minutesToConvert * 60000;
	}
	
	public static int calculateHeadingFrom(L2Object obj1, L2Object obj2)
	{
		return calculateHeadingFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	public static int calculateHeadingFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		return (int) ((Math.atan2(obj1Y - obj2Y, obj1X - obj2X) * 10430.379999999999D) + 32768.0D);
	}
	
	public static final int calculateHeadingFrom(double dx, double dy)
	{
		double angleTarget = Math.toDegrees(Math.atan2(dy, dx));
		if (angleTarget < 0.0D)
		{
			angleTarget = 360.0D + angleTarget;
		}
		return (int) (angleTarget * 182.04444444399999D);
	}
	
	public static int calcCameraAngle(int heading)
	{
		int angle;
		// int angle;
		if (heading == 0)
		{
			angle = 360;
		}
		else
		{
			angle = (int) (heading / 182.03999999999999D);
		}
		if (angle <= 90)
		{
			angle += 90;
		}
		else if ((angle > 90) && (angle <= 180))
		{
			angle -= 90;
		}
		else if ((angle > 180) && (angle <= 270))
		{
			angle += 90;
		}
		else if ((angle > 270) && (angle <= 360))
		{
			angle -= 90;
		}
		return angle;
	}
	
	public static int calcCameraAngle(L2NpcInstance target)
	{
		return calcCameraAngle(target.getHeading());
	}
	
	public static boolean contains(int[] array, int obj)
	{
		for (int anArray : array)
		{
			if (anArray == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	public static boolean contains(Object[] array, Object obj)
	{
		for (Object anArray : array)
		{
			if (anArray == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param text - the text to check
	 * @return {@code true} if {@code text} contains only numbers, {@code false} otherwise
	 */
	public static boolean isDigit(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		for (char c : text.toCharArray())
		{
			if (!Character.isDigit(c))
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param text - the text to check
	 * @return {@code true} if {@code text} is integer, {@code false} otherwise
	 */
	public static boolean isInteger(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		try
		{
			Integer.parseInt(text);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * @param text - the text to check
	 * @return {@code true} if {@code text} is float, {@code false} otherwise
	 */
	public static boolean isFloat(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		try
		{
			Float.parseFloat(text);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * @param text - the text to check
	 * @return {@code true} if {@code text} is double, {@code false} otherwise
	 */
	public static boolean isDouble(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		try
		{
			Double.parseDouble(text);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Replaces most invalid characters for the given string with an underscore.
	 * @param str the string that may contain invalid characters
	 * @return the string with invalid character replaced by underscores
	 */
	public static String replaceIllegalCharacters(String str)
	{
		String valid = str;
		for (char c : ILLEGAL_CHARACTERS)
		{
			valid = valid.replace(c, '_');
		}
		return valid;
	}
	
	/**
	 * Verify if a file name is valid.
	 * @param name the name of the file
	 * @return {@code true} if the file name is valid, {@code false} otherwise
	 */
	public static boolean isValidFileName(String name)
	{
		final File f = new File(name);
		try
		{
			f.getCanonicalPath();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
	
	/**
	 * @param objectsSize : The overall elements size.
	 * @param pageSize : The number of elements per page.
	 * @return The number of pages, based on the number of elements and the number of elements we want per page.
	 */
	public static int countPagesNumber(int objectsSize, int pageSize)
	{
		return (objectsSize / pageSize) + ((objectsSize % pageSize) == 0 ? 0 : 1);
	}
}
