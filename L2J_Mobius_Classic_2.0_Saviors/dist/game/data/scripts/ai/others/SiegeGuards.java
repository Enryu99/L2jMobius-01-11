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
package ai.others;

import com.l2jmobius.gameserver.ai.CtrlIntention;
import com.l2jmobius.gameserver.geoengine.GeoEngine;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.L2Attackable;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.L2Npc;
import com.l2jmobius.gameserver.model.actor.L2Summon;
import com.l2jmobius.gameserver.model.actor.instance.L2PcInstance;
import com.l2jmobius.gameserver.model.entity.Castle;
import com.l2jmobius.gameserver.model.entity.Fort;
import com.l2jmobius.gameserver.model.items.type.WeaponType;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class SiegeGuards extends AbstractNpcAI
{
	//@formatter:off
	// NPCs
	private static final int[] CASTLE_GUARDS = 
	{
		35064, 35065, 35066, 35067, 35068, 35069, 35071, 35072, 35079, 35080, 35081, 35082, 35083, 35084, 35085, // Gludio
		35106, 35107, 35108, 35109, 35110, 35111, 35113, 35114, 35121, 35122, 35123,35124, 35125, 35126, 35127, // Dion
		35150, 35151, 35152, 35153, 35155, 35156, 35163, 35164, 35165, 35166, 35167, 35168, 35169, // Giran
		35192, 35193, 35194, 35195, 35197, 35198, 35205, 35206, 35207, 35208, 35209, 35210, 35211, // Oren
		35234, 35239, 35240, 35248, 35249, 35250, 35251, 35252, 35253, 35254, // Aden
		35280, 35281, 35282, 35283, 35284, 35285, 35287, 35288, 35295, 35296, 35297, 35298, 35299, 35300, 35301, // Innadril
		35324, 35325, 35326, 35327, 35328, 35330, 35339, 35340, 35341, 35343, 35350, 35351, // Goddard
		35475, 35477, 35480, 35484, 35486, 35487, 35488, 35489, 35490, // Rune
		35516, 35517, 35518, 35519, 35520, 35522, 35531, 35532, 35533, 35535, 35542, 35543, // Schuttgart
	};
	private static final int[] MERCENARIES =
	{
		35015, 35016, 35017, 35018, 35019, 35025, 35026, 35027, 35028, 35029, 35035, 35036, 35037, 35038, 35039, 35045, 35046, 35047, 35048, 35049, 35055, 35056, 35057, 35058, 35059, 35060, 35061
	};
	private static final int[] STATIONARY_MERCENARIES =
	{
		35010, 35011, 35012, 35013, 35014, 35020, 35021, 35022, 35023, 35024, 35030, 35031, 35032, 35033, 35034, 35040, 35041, 35042, 35043, 35044, 35050, 35051, 35052, 35053, 35054, 35092, 35093, 35094,
		35134, 35135, 35136, 35176, 35177, 35178, 35218, 35219, 35220, 35261, 35262, 35263, 35264, 35265, 35308, 35309, 35310, 35352, 35353, 35354, 35497, 35498, 35499, 35500, 35501, 35544, 35545, 35546
	};
	//@formatter:on
	
	public SiegeGuards()
	{
		addAttackId(CASTLE_GUARDS);
		addAttackId(MERCENARIES);
		addAttackId(STATIONARY_MERCENARIES);
		addSpawnId(CASTLE_GUARDS);
		addSpawnId(MERCENARIES);
		addSpawnId(STATIONARY_MERCENARIES);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equals("AGGRO_CHECK"))
		{
			if ((npc != null) && !npc.isDead())
			{
				final L2Object target = npc.getTarget();
				if (!npc.isInCombat() || (target == null) || (npc.calculateDistance(target, false, false) > npc.getAggroRange()) || target.isInvul())
				{
					for (L2Character nearby : L2World.getInstance().getVisibleObjects(npc, L2Character.class, npc.getAggroRange()))
					{
						if (nearby.isPlayable() && GeoEngine.getInstance().canSeeTarget(npc, nearby))
						{
							final L2Summon summon = nearby.isSummon() ? (L2Summon) nearby : null;
							final L2PcInstance pl = summon == null ? (L2PcInstance) nearby : summon.getOwner();
							if (((pl.getSiegeState() != 2) || pl.isRegisteredOnThisSiegeField(npc.getScriptValue())) && ((pl.getSiegeState() != 0) || (npc.getAI().getIntention() != CtrlIntention.AI_INTENTION_IDLE)))
							{
								if (!pl.isInvisible() && !pl.isInvul()) // skip invisible players
								{
									addAttackPlayerDesire(npc, pl);
								}
							}
						}
					}
				}
				startQuestTimer("AGGRO_CHECK", 2000, npc, null);
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		if ((attacker.getSiegeState() == 2) && !attacker.isRegisteredOnThisSiegeField(npc.getScriptValue()))
		{
			((L2Attackable) npc).stopHating(attacker);
			return null;
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		npc.setRandomWalking(false);
		if ((npc.getTemplate().getBaseAttackType() != WeaponType.SWORD) && (npc.getTemplate().getBaseAttackType() != WeaponType.POLE))
		{
			npc.setIsImmobilized(true);
		}
		final Castle castle = npc.getCastle();
		final Fort fortress = npc.getFort();
		npc.setScriptValue(fortress != null ? fortress.getResidenceId() : (castle != null ? castle.getResidenceId() : 0));
		startQuestTimer("AGGRO_CHECK", 2000, npc, null);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new SiegeGuards();
	}
}
