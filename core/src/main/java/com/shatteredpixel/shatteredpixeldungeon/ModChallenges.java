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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PhantomPiranha;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import java.util.HashSet;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
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

	public static void reset(){
		pastyVariant = SPDSettings.pastyVariant();
	}

	private static final String PASTY_VARIANT   = "mod_pasty_variant";

	public static void storeInBundle( Bundle bundle ){
		bundle.put( PASTY_VARIANT, pastyVariant );
	}

	public static void restoreFromBundle( Bundle bundle ){
		if (bundle.contains( PASTY_VARIANT )){
			pastyVariant = bundle.getInt( PASTY_VARIANT );
			if (pastyVariant < 0 || pastyVariant >= Holiday.values().length) pastyVariant = 0;
		} else {
			pastyVariant = 0;
		}
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
		return "\n\n" + Messages.get(ModChallenges.class, "food_satiety", satietyValue(food), (int)Hunger.STARVING);
	}

	// ***** Rabble / Elite Forces *****

	//whether a character is a regular enemy, affected by the rabble and elite forces challenges
	public static boolean isRegularEnemy( Char ch ){
		if (!(ch instanceof Mob) || ch instanceof NPC) return false;
		//neutral enemies (e.g. hidden mimics) count, allies don't
		if (ch.alignment == Char.Alignment.ALLY) return false;
		if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)) return false;
		//quest enemies: vault quest enemies, and anything in a quest branch (e.g. the blacksmith's mine)
		if (ch.getClass().getName().contains(".mobs.quest.")) return false;
		if (Dungeon.branch != 0) return false;
		return true;
	}

	//multiplier to health, damage, accuracy, evasion, and armor of regular enemies
	public static float mobStatFactor( Char ch ){
		if (!isRegularEnemy(ch)) return 1f;
		float factor = 1f;
		if (Dungeon.isChallenged(Challenges.RABBLE))        factor *= 0.5f;
		if (Dungeon.isChallenged(Challenges.ELITE_FORCES))  factor *= 2.3f;
		return factor;
	}

	//multiplier to the number of regular enemies on a floor
	public static float mobCountFactor(){
		float factor = 1f;
		if (Dungeon.isChallenged(Challenges.RABBLE))        factor *= 2f;
		if (Dungeon.isChallenged(Challenges.ELITE_FORCES))  factor *= 0.5f;
		return factor;
	}

	public static int scaleMobCount( int count ){
		float factor = mobCountFactor();
		if (factor == 1f || count <= 0) return count;
		return Math.max(1, Math.round(count * factor));
	}

	//multiplier to enemy spawning frequency
	public static float spawnRateFactor(){
		float factor = 1f;
		if (Dungeon.isChallenged(Challenges.RABBLE))        factor *= 1.5f;
		if (Dungeon.isChallenged(Challenges.ELITE_FORCES))  factor *= 0.5f;
		return factor;
	}

	// ***** Improved Rarity *****

	//rare variants of regular enemies. Mimics are deliberately not included
	private static final HashSet<Class<? extends Mob>> RARE_ENEMIES = new HashSet<>();
	static {
		RARE_ENEMIES.addAll(MobSpawner.RARE_ALTS.values());
		RARE_ENEMIES.add(ArmoredStatue.class);
		RARE_ENEMIES.add(PhantomPiranha.class);
		RARE_ENEMIES.add(TormentedSpirit.class);
	}

	public static final int RARE_AURA_COLOR = 0xFFDD33;

	public static boolean isRareEnemy( Char ch ){
		return ch != null && RARE_ENEMIES.contains(ch.getClass());
	}

	//rare enemies glow yellow, unless they already have a champion aura
	public static boolean showRareAura( Char ch ){
		return Dungeon.isChallenged(Challenges.IMPROVED_RARITY)
				&& isRareEnemy(ch)
				&& ch.buff(ChampionEnemy.class) == null;
	}

	// ***** Improved Chasm *****

	public static String chasmInfo( int cell ){
		if (!Dungeon.isChallenged(Challenges.IMPROVED_CHASM) || Dungeon.hero == null || Dungeon.level == null) return "";
		if (cell < 0 || cell >= Dungeon.level.length() || !Dungeon.level.pit[cell]) return "";
		Hero hero = Dungeon.hero;
		if (hero.buff(ElixirOfFeatherFall.FeatherBuff.class) != null){
			return "\n\n" + Messages.get(ModChallenges.class, "chasm_feather");
		}
		//mirrors Chasm.heroLand()
		int minDmg = hero.HP / 2;
		int maxDmg = Math.max(hero.HP / 2, hero.HT / 4);
		int bleed = Math.round(hero.HT / (6f + (6f*(hero.HP/(float)hero.HT))));
		String dmg = minDmg == maxDmg ? Integer.toString(minDmg) : minDmg + "~" + maxDmg;
		return "\n\n" + Messages.get(ModChallenges.class, "chasm_info", dmg, bleed, bleed*3);
	}

	// ***** Improved Explore *****

	//which marker (if any) to draw over a cell. Returns -1 for no marker
	public static int exploreMarker( int cell, boolean walls ){
		if (!Dungeon.isChallenged(Challenges.IMPROVED_EXPLORE)) return -1;
		Level level = Dungeon.level;
		if (level == null || level.examined == null || cell < 0 || cell >= level.length()) return -1;
		//boss floors and floor 26 have no secrets to find
		if (Dungeon.bossLevel() || Dungeon.depth == 26) return -1;
		if (level.examined[cell]) return -1;
		if (!level.visited[cell] && !level.mapped[cell]) return -1;

		//only plain floor and wall tiles get markers (including secrets, which look identical to them)
		//decorated floors (e.g. moss) and walls never get markers, secrets always look like plain floor/walls
		int terr = level.map[cell];
		boolean isWall = terr == Terrain.WALL || terr == Terrain.SECRET_DOOR;
		boolean isFloor = terr == Terrain.EMPTY || terr == Terrain.SECRET_TRAP;
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


}
