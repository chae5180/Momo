/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

public class Challenges {

	//Some of these internal IDs are outdated and don't represent what these challenges do
	public static final int NO_FOOD				= 1;
	public static final int NO_ARMOR			= 2;
	public static final int NO_HEALING			= 4;
	public static final int NO_HERBALISM		= 8;
	public static final int SWARM_INTELLIGENCE	= 16;
	public static final int DARKNESS			= 32;
	public static final int NO_SCROLLS		    = 64;
	public static final int CHAMPION_ENEMIES	= 128;
	public static final int STRONGER_BOSSES 	= 256;

	//mod challenges: quality of life / extra content, these do not affect score
	public static final int IMPROVED_FOOD		= 512;
	public static final int IMPROVED_CHARGE		= 1024;
	public static final int IMPROVED_COMBAT		= 2048;
	public static final int IMPROVED_EXPLORE	= 4096;
	public static final int HUGE_DUNGEON		= 8192;
	public static final int RABBLE				= 16384;
	public static final int ELITE_FORCES		= 32768;
	public static final int IMPROVED_RARITY		= 65536;
	public static final int IMPROVED_MAGIC		= 131072;
	public static final int IMPROVED_CHASM		= 262144;
	public static final int IMPROVED_RAGE		= 524288;

	public static final int MAX_VALUE           = 1048575;
	//number of challenges which affect score (used for randomizing and score multipliers)
	public static final int MAX_CHALS           = 9;

	//all challenges which affect score
	public static final int SCORING_MASK		= 511;
	//all challenges which do not affect score
	public static final int MOD_MASK			= IMPROVED_FOOD | IMPROVED_CHARGE | IMPROVED_COMBAT | IMPROVED_EXPLORE | HUGE_DUNGEON
														| RABBLE | ELITE_FORCES | IMPROVED_RARITY | IMPROVED_MAGIC | IMPROVED_CHASM | IMPROVED_RAGE;

	public static final String[] NAME_IDS = {
			"champion_enemies",
			"stronger_bosses",
			"no_food",
			"no_armor",
			"no_healing",
			"no_herbalism",
			"swarm_intelligence",
			"darkness",
			"no_scrolls",
			"improved_food",
			"improved_charge",
			"improved_combat",
			"improved_explore",
			"huge_dungeon",
			"rabble",
			"elite_forces",
			"improved_rarity",
			"improved_magic",
			"improved_chasm",
			"improved_rage"
	};

	public static final int[] MASKS = {
			CHAMPION_ENEMIES, STRONGER_BOSSES, NO_FOOD, NO_ARMOR, NO_HEALING, NO_HERBALISM, SWARM_INTELLIGENCE, DARKNESS, NO_SCROLLS,
			IMPROVED_FOOD, IMPROVED_CHARGE, IMPROVED_COMBAT, IMPROVED_EXPLORE, HUGE_DUNGEON,
			RABBLE, ELITE_FORCES, IMPROVED_RARITY, IMPROVED_MAGIC, IMPROVED_CHASM, IMPROVED_RAGE
	};

	public static boolean isScoring( int mask ){
		return (mask & SCORING_MASK) != 0;
	}

	//only counts challenges which affect score
	public static int activeChallenges(){
		return activeChallenges(Dungeon.challenges);
	}

	//only counts challenges which affect score
	public static int activeChallenges(int mask){
		int chCount = 0;
		for (int ch : Challenges.MASKS){
			if ((mask & ch & SCORING_MASK) != 0) chCount++;
		}
		return chCount;
	}

	//counts all challenges, including ones which do not affect score
	public static int allActiveChallenges(int mask){
		int chCount = 0;
		for (int ch : Challenges.MASKS){
			if ((mask & ch) != 0) chCount++;
		}
		return chCount;
	}

	public static boolean isItemBlocked( Item item ){

		if (Dungeon.isChallenged(NO_HERBALISM) && item instanceof Dewdrop){
			return true;
		}

		return false;

	}

}