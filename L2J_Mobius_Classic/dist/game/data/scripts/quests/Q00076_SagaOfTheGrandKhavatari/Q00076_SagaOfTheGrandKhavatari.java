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
package quests.Q00076_SagaOfTheGrandKhavatari;

import com.l2jmobius.gameserver.model.Location;

import quests.AbstractSagaQuest;

/**
 * Saga of the Grand Khavatari (76)
 * @author Emperorc
 */
public class Q00076_SagaOfTheGrandKhavatari extends AbstractSagaQuest
{
	public Q00076_SagaOfTheGrandKhavatari()
	{
		super(76);
		_npc = new int[]
		{
			0, // FIXME: 31339 - Hakran
			31624,
			0, // FIXME: 31589 - Duda-Mara Totem Spirit
			0, // FIXME: 31290 - Skahi
			31637,
			31646,
			31647,
			31652,
			31654,
			31655,
			0, // FIXME: 31659 - Tablet of Vision
			0, // FIXME: 31290 - Skahi
		};
		Items = new int[]
		{
			7080,
			7539,
			7081,
			7491,
			7274,
			7305,
			7336,
			7367,
			0, // FIXME: 7398 - Resonance Amulet - 5
			0, // FIXME: 7429 - Resonance Amulet - 6
			7099,
			0
		};
		Mob = new int[]
		{
			27293,
			27226,
			27284
		};
		classid = new int[]
		{
			114
		};
		prevclass = new int[]
		{
			0x30
		};
		npcSpawnLocations = new Location[]
		{
			new Location(161719, -92823, -1893),
			new Location(124355, 82155, -2803),
			new Location(124376, 82127, -2796)
		};
		Text = new String[]
		{
			"PLAYERNAME! Pursued to here! However, I jumped out of the Banshouren boundaries! You look at the giant as the sign of power!",
			"... Oh ... good! So it was ... let's begin!",
			"I do not have the patience ..! I have been a giant force ...! Cough chatter ah ah ah!",
			"Paying homage to those who disrupt the orderly will be PLAYERNAME's death!",
			"Now, my soul freed from the shackles of the millennium, Halixia, to the back side I come ...",
			"Why do you interfere others' battles?",
			"This is a waste of time.. Say goodbye...!",
			"...That is the enemy",
			"...Goodness! PLAYERNAME you are still looking?",
			"PLAYERNAME ... Not just to whom the victory. Only personnel involved in the fighting are eligible to share in the victory.",
			"Your sword is not an ornament. Don't you think, PLAYERNAME?",
			"Goodness! I no longer sense a battle there now.",
			"let...",
			"Only engaged in the battle to bar their choice. Perhaps you should regret.",
			"The human nation was foolish to try and fight a giant's strength.",
			"Must...Retreat... Too...Strong.",
			"PLAYERNAME. Defeat...by...retaining...and...Mo...Hacker",
			"....! Fight...Defeat...It...Fight...Defeat...It..."
		};
		registerNPCs();
	}
}
