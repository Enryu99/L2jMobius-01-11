/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.group_template;

import java.util.HashSet;
import java.util.Set;

import ai.npc.AbstractNpcAI;

import com.l2jserver.gameserver.enums.ChatType;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.holders.MinionHolder;
import com.l2jserver.gameserver.network.NpcStringId;

/**
 * Minion Spawn Manager.
 * @author Zealar
 */
public final class MinionSpawnManager extends AbstractNpcAI
{
	private static final Set<Integer> NPC = new HashSet<>(354);
	
	static
	{
		NPC.add(35375); // Bloody Lord Nurka
		NPC.add(20376); // Varikan Brigand Leader
		NPC.add(20398); // Vrykolakas
		NPC.add(20520); // Pirate Captain Uthanka
		NPC.add(20522); // White Fang
		NPC.add(20738); // Kobold Looter Bepook
		NPC.add(20745); // Gigantiops
		NPC.add(20747); // Roxide
		NPC.add(20749); // Death Fire
		NPC.add(20751); // Snipe
		NPC.add(20753); // Dark Lord
		NPC.add(20755); // Talakin
		NPC.add(20758); // Dragon Bearer Chief
		NPC.add(20761); // Pytan
		NPC.add(20767); // Timak Orc Troop Leader
		NPC.add(20773); // Conjurer Bat Lord
		NPC.add(20939); // Tanor Silenos Warrior
		NPC.add(20941); // Tanor Silenos Chieftain
		NPC.add(20944); // Nightmare Lord
		NPC.add(20956); // Past Knight
		NPC.add(20959); // Dark Guard
		NPC.add(20963); // Bloody Lord
		NPC.add(20974); // Spiteful Soul Leader
		NPC.add(20977); // Elmoradan's Lady
		NPC.add(20980); // Hallate's Follower Mul
		NPC.add(20983); // Binder
		NPC.add(20986); // Sairon
		NPC.add(20991); // Swamp Tribe
		NPC.add(20994); // Garden Guard Leader
		NPC.add(21075); // Slaughter Bathin
		NPC.add(21078); // Magus Valac
		NPC.add(21081); // Power Angel Amon
		NPC.add(21090); // Bloody Guardian
		NPC.add(21432); // Chakram Beetle
		NPC.add(21434); // Seer of Blood
		NPC.add(22320); // Junior Watchman
		NPC.add(22321); // Junior Summoner
		NPC.add(22346); // Quarry Foreman
		NPC.add(22363); // Body Destroyer
		NPC.add(22370); // Passageway Captain
		NPC.add(22377); // Master Zelos
		NPC.add(22390); // Foundry Foreman
		NPC.add(22423); // Original Sin Warden
		NPC.add(22431); // Original Sin Warden
		NPC.add(22448); // Leodas
		NPC.add(22449); // Amaskari
		NPC.add(25001); // Greyclaw Kutus
		NPC.add(25004); // Turek Mercenary Captain
		NPC.add(25007); // Retreat Spider Cletu
		NPC.add(25010); // Furious Thieles
		NPC.add(25013); // Ghost of Peasant Leader
		NPC.add(25016); // The 3rd Underwater Guardian
		NPC.add(25020); // Breka Warlock Pastu
		NPC.add(25023); // Stakato Queen Zyrnna
		NPC.add(25026); // Ketra Commander Atis
		NPC.add(25029); // Atraiban
		NPC.add(25032); // Eva's Guardian Millenu
		NPC.add(25035); // Shilen's Messenger Cabrio
		NPC.add(25038); // Tirak
		NPC.add(25041); // Remmel
		NPC.add(25044); // Barion
		NPC.add(25047); // Karte
		NPC.add(25051); // Rahha
		NPC.add(25054); // Kernon
		NPC.add(25057); // Beacon of Blue Sky
		NPC.add(25060); // Unrequited Kael
		NPC.add(25064); // Wizard of Storm Teruk
		NPC.add(25067); // Captain of Red Flag Shaka
		NPC.add(25070); // Enchanted Forest Watcher Ruell
		NPC.add(25073); // Bloody Priest Rudelto
		NPC.add(25076); // Princess Molrang
		NPC.add(25079); // Cat's Eye Bandit
		NPC.add(25082); // Leader of Cat Gang
		NPC.add(25085); // Timak Orc Chief Ranger
		NPC.add(25089); // Soulless Wild Boar
		NPC.add(25092); // Korim
		NPC.add(25095); // Elf Renoa
		NPC.add(25099); // Rotting Tree Repiro
		NPC.add(25103); // Sorcerer Isirr
		NPC.add(25106); // Ghost of the Well Lidia
		NPC.add(25109); // Antharas Priest Cloe
		NPC.add(25112); // Beleth's Agent, Meana
		NPC.add(25115); // Icarus Sample 1
		NPC.add(25119); // Messenger of Fairy Queen Berun
		NPC.add(25122); // Refugee Applicant Leo
		NPC.add(25128); // Vuku Grand Seer Gharmash
		NPC.add(25131); // Carnage Lord Gato
		NPC.add(25134); // Leto Chief Talkin
		NPC.add(25137); // Beleth's Seer, Sephia
		NPC.add(25140); // Hekaton Prime
		NPC.add(25143); // Fire of Wrath Shuriel
		NPC.add(25146); // Serpent Demon Bifrons
		NPC.add(25149); // Zombie Lord Crowl
		NPC.add(25152); // Flame Lord Shadar
		NPC.add(25155); // Shaman King Selu
		NPC.add(25159); // Paniel the Unicorn
		NPC.add(25166); // Ikuntai
		NPC.add(25170); // Lizardmen Leader Hellion
		NPC.add(25173); // Tiger King Karuta
		NPC.add(25176); // Black Lily
		NPC.add(25179); // Guardian of the Statue of Giant Karum
		NPC.add(25182); // Demon Kuri
		NPC.add(25185); // Tasaba Patriarch Hellena
		NPC.add(25189); // Cronos's Servitor Mumu
		NPC.add(25192); // Earth Protector Panathen
		NPC.add(25199); // Water Dragon Seer Sheshark
		NPC.add(25202); // Krokian Padisha Sobekk
		NPC.add(25205); // Ocean Flame Ashakiel
		NPC.add(25208); // Water Couatle Ateka
		NPC.add(25211); // Sebek
		NPC.add(25214); // Fafurion's Page Sika
		NPC.add(25217); // Cursed Clara
		NPC.add(25220); // Death Lord Hallate
		NPC.add(25223); // Soul Collector Acheron
		NPC.add(25226); // Roaring Lord Kastor
		NPC.add(25230); // Timak Seer Ragoth
		NPC.add(25235); // Vanor Chief Kandra
		NPC.add(25238); // Abyss Brukunt
		NPC.add(25241); // Harit Hero Tamash
		NPC.add(25245); // Last Lesser Giant Glaki
		NPC.add(25249); // Menacing Palatanos
		NPC.add(25252); // Palibati Queen Themis
		NPC.add(25256); // Taik High Prefect Arak
		NPC.add(25260); // Iron Giant Totem
		NPC.add(25263); // Kernon's Faithful Servant Kelone
		NPC.add(25266); // Bloody Empress Decarbia
		NPC.add(25269); // Beast Lord Behemoth
		NPC.add(25273); // Carnamakos
		NPC.add(25277); // Lilith's Witch Marilion
		NPC.add(25283); // Lilith
		NPC.add(25286); // Anakim
		NPC.add(25290); // Daimon the White-Eyed
		NPC.add(25293); // Hesti Guardian Deity of the Hot Springs
		NPC.add(25296); // Icicle Emperor Bumbalump
		NPC.add(25299); // Ketra's Hero Hekaton
		NPC.add(25302); // Ketra's Commander Tayr
		NPC.add(25306); // Soul of Fire Nastron
		NPC.add(25309); // Varka's Hero Shadith
		NPC.add(25312); // Varka's Commander Mos
		NPC.add(25316); // Soul of Water Ashutar
		NPC.add(25319); // Ember
		NPC.add(25322); // Demon's Agent Falston
		NPC.add(25325); // Flame of Splendor Barakiel
		NPC.add(25328); // Eilhalder von Hellmann
		NPC.add(25352); // Giant Wasteland Basilisk
		NPC.add(25354); // Gargoyle Lord Sirocco
		NPC.add(25357); // Sukar Wererat Chief
		NPC.add(25360); // Tiger Hornet
		NPC.add(25362); // Tracker Leader Sharuk
		NPC.add(25366); // Kuroboros' Priest
		NPC.add(25369); // Soul Scavenger
		NPC.add(25373); // Malex Herald of Dagoniel
		NPC.add(25375); // Zombie Lord Ferkel
		NPC.add(25378); // Madness Beast
		NPC.add(25380); // Kaysha Herald of Icarus
		NPC.add(25383); // Revenant of Sir Calibus
		NPC.add(25385); // Evil Spirit Tempest
		NPC.add(25388); // Red Eye Captain Trakia
		NPC.add(25392); // Captain of Queen's Royal Guards
		NPC.add(25395); // Archon Suscepter
		NPC.add(25398); // Beleth's Eye
		NPC.add(25401); // Skyla
		NPC.add(25404); // Corsair Captain Kylon
		NPC.add(25407); // Lord Ishka
		NPC.add(25410); // Road Scavenger Leader
		NPC.add(25412); // Necrosentinel Royal Guard
		NPC.add(25415); // Nakondas
		NPC.add(25418); // Dread Avenger Kraven
		NPC.add(25420); // Orfen's Handmaiden
		NPC.add(25423); // Fairy Queen Timiniel
		NPC.add(25426); // Betrayer of Urutu Freki
		NPC.add(25429); // Mammon Collector Talos
		NPC.add(25431); // Flamestone Golem
		NPC.add(25434); // Bandit Leader Barda
		NPC.add(25438); // Thief Kelbar
		NPC.add(25441); // Evil Spirit Cyrion
		NPC.add(25444); // Enmity Ghost Ramdal
		NPC.add(25447); // Immortal Savior Mardil
		NPC.add(25450); // Cherub Galaxia
		NPC.add(25453); // Meanas Anor
		NPC.add(25456); // Mirror of Oblivion
		NPC.add(25460); // Deadman Ereve
		NPC.add(25463); // Harit Guardian Garangky
		NPC.add(25467); // Gorgolos
		NPC.add(25470); // Last Titan Utenus
		NPC.add(25473); // Grave Robber Kim
		NPC.add(25475); // Ghost Knight Kabed
		NPC.add(25478); // Shilen's Priest Hisilrome
		NPC.add(25481); // Magus Kenishee
		NPC.add(25484); // Zaken's Chief Mate Tillion
		NPC.add(25487); // Water Spirit Lian
		NPC.add(25490); // Gwindorr
		NPC.add(25493); // Eva's Spirit Niniel
		NPC.add(25496); // Fafurion's Envoy Pingolpin
		NPC.add(25498); // Fafurion's Henchman Istary
		NPC.add(25735); // Greyclaw Kutus
		NPC.add(27036); // Calpico
		NPC.add(27041); // Varangka's Messenger
		NPC.add(27062); // Tanukia
		NPC.add(27065); // Roko
		NPC.add(27068); // Murtika
		NPC.add(27093); // Delu Chief Kalkis
		NPC.add(27108); // Stenoa Gorgon Queen
		NPC.add(27110); // Shyslassys
		NPC.add(27112); // Gorr
		NPC.add(27113); // Baraham
		NPC.add(27114); // Succubus Queen
		NPC.add(27185); // Fairy Tree of Wind
		NPC.add(27186); // Fairy Tree of Star
		NPC.add(27187); // Fairy Tree of Twilight
		NPC.add(27188); // Fairy Tree of Abyss
		NPC.add(29001); // Queen Ant
	}
	
	private static final NpcStringId[] ON_ATTACK_MSG =
	{
		NpcStringId.COME_OUT_YOU_CHILDREN_OF_DARKNESS,
		NpcStringId.SHOW_YOURSELVES,
		NpcStringId.DESTROY_THE_ENEMY_MY_BROTHERS,
		NpcStringId.FORCES_OF_DARKNESS_FOLLOW_ME
	};
	
	private static final int[] ON_ATTACK_NPC =
	{
		20767, // Timak Orc Troop Leader
	};
	
	private MinionSpawnManager()
	{
		super(MinionSpawnManager.class.getSimpleName(), "ai/group_template");
		
		addSpawnId(NPC);
		addAttackId(ON_ATTACK_NPC);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		if (npc.getTemplate().getParameters().getSet().get("SummonPrivateRate") == null)
		{
			((L2MonsterInstance) npc).getMinionList().spawnMinions(npc.getTemplate().getParameters().getMinionList("Privates"));
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		if (npc.isMonster())
		{
			final L2MonsterInstance monster = (L2MonsterInstance) npc;
			if (!monster.isTeleporting())
			{
				if (getRandom(1, 100) <= npc.getTemplate().getParameters().getInt("SummonPrivateRate", 0))
				{
					for (MinionHolder is : npc.getTemplate().getParameters().getMinionList("Privates"))
					{
						addMinion((L2MonsterInstance) npc, is.getId());
					}
					broadcastNpcSay(npc, ChatType.NPC_GENERAL, ON_ATTACK_MSG[getRandom(ON_ATTACK_MSG.length)]);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	public static void main(String[] args)
	{
		new MinionSpawnManager();
	}
}