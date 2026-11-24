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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.ExploreMarkersTilemap;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.Holiday;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//shared logic and run state for the mod's (non-scoring) challenges
public class ModChallenges {

	// ***** Improved Food *****

	//chance increase for the next ration to become a pasty, each time a ration is generated
	public static final float PASTY_CHANCE_STEP = 0.2f;

	//options for what replaces pasties. NONE means a regular pasty (which still respects real holidays)
	public static final Holiday[] PASTY_VARIANTS = new Holiday[]{
			Holiday.NONE,
			Holiday.NEW_YEARS,          //sparkling potion
			Holiday.LUNAR_NEW_YEAR,     //steamed fish
			Holiday.APRIL_FOOLS,        //chocolate amulet
			Holiday.EASTER,             //easter egg
			Holiday.PRIDE,              //rainbow potion
			Holiday.SHATTEREDPD_BIRTHDAY, //green cake
			Holiday.HALLOWEEN,          //pumpkin pie
			Holiday.PD_BIRTHDAY,        //blue cake
			Holiday.WINTER_HOLIDAYS     //candy cane
	};

	//ordinal of the holiday whose food replaces pasties this run
	public static int pastyVariant = 0;
	//current chance for the next generated ration to be replaced by a pasty
	public static float pastyReplaceChance = 0;

	public static void reset(){
		pastyVariant = SPDSettings.pastyVariant();
		pastyReplaceChance = 0;
	}

	private static final String PASTY_VARIANT   = "mod_pasty_variant";
	private static final String PASTY_CHANCE    = "mod_pasty_chance";

	public static void storeInBundle( Bundle bundle ){
		bundle.put( PASTY_VARIANT, pastyVariant );
		bundle.put( PASTY_CHANCE, pastyReplaceChance );
	}

	public static void restoreFromBundle( Bundle bundle ){
		if (bundle.contains( PASTY_VARIANT )){
			pastyVariant = bundle.getInt( PASTY_VARIANT );
			if (pastyVariant < 0 || pastyVariant >= Holiday.values().length) pastyVariant = 0;
		} else {
			pastyVariant = 0;
		}
		pastyReplaceChance = bundle.getFloat( PASTY_CHANCE );
	}

	//the 'holiday' which determines what pasties look like and do
	public static Holiday pastyHoliday(){
		if (Dungeon.isChallenged(Challenges.IMPROVED_FOOD) && pastyVariant != 0){
			return Holiday.values()[pastyVariant];
		}
		return Holiday.getCurrentHoliday();
	}

	public static String pastyVariantName( Holiday variant ){
		switch (variant){
			case NONE: default:         return Messages.get(Pasty.class, "name");
			case LUNAR_NEW_YEAR:        return Messages.get(Pasty.class, "fish_name");
			case APRIL_FOOLS:           return Messages.get(Pasty.class, "amulet_name");
			case EASTER:                return Messages.get(Pasty.class, "egg_name");
			case PRIDE:                 return Messages.get(Pasty.class, "rainbow_name");
			case SHATTEREDPD_BIRTHDAY:  return Messages.get(Pasty.class, "shattered_name");
			case HALLOWEEN:             return Messages.get(Pasty.class, "pie_name");
			case PD_BIRTHDAY:           return Messages.get(Pasty.class, "vanilla_name");
			case WINTER_HOLIDAYS:       return Messages.get(Pasty.class, "cane_name");
			case NEW_YEARS:             return Messages.get(Pasty.class, "sparkling_name");
		}
	}

	//called whenever an item is generated. Rations may be swapped out for pasties
	public static Item processGeneratedFood( Item item ){
		if (item == null
				|| item.getClass() != Food.class
				|| !Dungeon.isChallenged(Challenges.IMPROVED_FOOD)){
			return item;
		}

		if (pastyReplaceChance > 0 && Random.Float() < pastyReplaceChance){
			pastyReplaceChance = 0;
			Pasty pasty = new Pasty();
			pasty.quantity(item.quantity());
			return pasty;
		}

		pastyReplaceChance = Math.min(1f, pastyReplaceChance + PASTY_CHANCE_STEP);
		return item;
	}

	//how much hunger a food item will actually restore
	public static int satietyValue( Food food ){
		float value = food.energy;
		if (food instanceof Pasty && pastyHoliday() == Holiday.LUNAR_NEW_YEAR){
			//main fish + leftover
			value = Hunger.HUNGRY + Hunger.HUNGRY/2f;
		}
		if (Dungeon.isChallenged(Challenges.NO_FOOD)){
			value /= 3f;
		}
		return Math.round(value);
	}

	public static String foodInfo( Food food ){
		if (!Dungeon.isChallenged(Challenges.IMPROVED_FOOD)) return "";
		String info = "\n\n" + Messages.get(ModChallenges.class, "food_satiety", satietyValue(food), (int)Hunger.STARVING);
		if (food.getClass() == Food.class){
			info += "\n" + pastyChanceInfo();
		}
		return info;
	}

	// ***** Improved Explore *****

	//which marker (if any) to draw over a cell. Returns -1 for no marker
	public static int exploreMarker( int cell, boolean walls ){
		if (!Dungeon.isChallenged(Challenges.IMPROVED_EXPLORE)) return -1;
		Level level = Dungeon.level;
		if (level == null || level.examined == null || cell < 0 || cell >= level.length()) return -1;
		if (level.examined[cell]) return -1;
		if (!level.visited[cell] && !level.mapped[cell]) return -1;

		//only regular floor and wall tiles get markers (including secrets that look like them)
		int terr = level.map[cell];
		boolean isWall = terr == Terrain.WALL || terr == Terrain.WALL_DECO || terr == Terrain.SECRET_DOOR;
		boolean isFloor = terr == Terrain.EMPTY || terr == Terrain.EMPTY_DECO || terr == Terrain.SECRET_TRAP;
		if (walls ? !isWall : !isFloor) return -1;

		return Dungeon.depth > 20 ? ExploreMarkersTilemap.BLUE_CROSS : ExploreMarkersTilemap.RED_CROSS;
	}

	public static void markExamined( int cell ){
		Level level = Dungeon.level;
		if (level == null || level.examined == null || cell < 0 || cell >= level.length()) return;
		if (!level.examined[cell]){
			level.examined[cell] = true;
			GameScene.updateExploreMarkers(cell);
		}
	}

	//e.g. after magic mapping, when there can no longer be any secrets on the level
	public static void markAllExamined(){
		Level level = Dungeon.level;
		if (level == null || level.examined == null) return;
		java.util.Arrays.fill(level.examined, true);
		GameScene.updateExploreMarkers();
	}

	// ***** Improved Combat *****

	//shield decay interval and progress, for the hero's shield buffs
	public static String shieldInfo( Buff buff ){
		if (!Dungeon.isChallenged(Challenges.IMPROVED_COMBAT) || buff == null || buff.target != Dungeon.hero) return "";
		if (buff instanceof Barrier){
			Barrier b = (Barrier) buff;
			float perTurn = b.decayPerTurn();
			if (perTurn <= 0){
				return "\n\n" + Messages.get(ModChallenges.class, "shield_no_decay");
			}
			return "\n\n" + Messages.get(ModChallenges.class, "shield_decay",
					String.format("%.1f", 1f/perTurn),
					Math.round(Math.max(0, Math.min(1, b.decayProgress()))*100));
		} else if (buff instanceof Berserk && ((Berserk) buff).isBerserking()){
			float perTurn = ((Berserk) buff).shieldDecayPerTurn();
			return "\n\n" + Messages.get(ModChallenges.class, "shield_decay_berserk", String.format("%.1f", perTurn));
		}
		return "";
	}

	public static String pastyChanceInfo(){
		return Messages.get(ModChallenges.class, "pasty_chance", Math.round(pastyReplaceChance*100),
				pastyVariantName(pastyHoliday()));
	}

}
