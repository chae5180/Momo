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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

//builds the detailed enemy info shown by the improved combat challenge
public class ModMobInfo {

	//a random generator which always rolls the lowest (or highest) possible value
	private static class RiggedRandom extends java.util.Random {

		private final boolean max;

		public RiggedRandom( boolean max ){
			super(0);
			this.max = max;
		}

		@Override
		protected int next(int bits) {
			return max ? (int)((1L << bits) - 1) : 0;
		}

		@Override
		public int nextInt() {
			return max ? Integer.MAX_VALUE : 0;
		}

		@Override
		public int nextInt(int bound) {
			return max ? bound - 1 : 0;
		}

		@Override
		public long nextLong() {
			return max ? Long.MAX_VALUE : 0;
		}

		@Override
		public boolean nextBoolean() {
			return max;
		}

		@Override
		public float nextFloat() {
			return max ? 0.99999994f : 0f;
		}

		@Override
		public double nextDouble() {
			return max ? 0.9999999999999999 : 0;
		}

		@Override
		public synchronized double nextGaussian() {
			return 0;
		}
	}

	private interface Roll {
		int roll();
	}

	//evaluates a roll with forced min and max RNG values
	private static int[] rollRange( Roll roll ){
		int a, b;
		Random.pushGenerator(new RiggedRandom(false));
		try {
			a = roll.roll();
		} finally {
			Random.popGenerator();
		}
		Random.pushGenerator(new RiggedRandom(true));
		try {
			b = roll.roll();
		} finally {
			Random.popGenerator();
		}
		return new int[]{Math.min(a, b), Math.max(a, b)};
	}

	private static String range( int[] r ){
		return r[0] == r[1] ? Integer.toString(r[0]) : r[0] + "~" + r[1];
	}

	public static String effectName( Class c ){
		//some effects have no name of their own, we give them one
		String name = Messages.get(ModMobInfo.class, "fx_" + c.getSimpleName().toLowerCase());
		if (!name.equals(Messages.NO_TEXT_FOUND)){
			return Messages.titleCase(name);
		}

		if (Weapon.Enchantment.class.isAssignableFrom(c)){
			name = Messages.get(c, "name", Messages.get(Weapon.Enchantment.class, "enchant"));
		} else if (Armor.Glyph.class.isAssignableFrom(c)){
			name = Messages.get(c, "name", Messages.get(Armor.Glyph.class, "glyph"));
		} else {
			name = Messages.get(c, "name");
		}
		if (name == null || name.equals(Messages.NO_TEXT_FOUND) || name.isEmpty()){
			//fall back to the class name, split into words
			name = c.getSimpleName().replaceAll("([a-z])([A-Z])", "$1 $2");
		}
		return Messages.titleCase(name);
	}

	private static String effectList( HashSet<Class> effects ){
		ArrayList<String> names = new ArrayList<>();
		for (Class c : effects){
			if (c == AllyBuff.class) continue; //internal, not a real effect
			String n = effectName(c);
			if (!names.contains(n)) names.add(n);
		}
		if (names.isEmpty()) return Messages.get(ModMobInfo.class, "none");
		Collections.sort(names);
		StringBuilder sb = new StringBuilder();
		for (String n : names){
			if (sb.length() > 0) sb.append(", ");
			sb.append(n);
		}
		return sb.toString();
	}

	private static String lootName( Object loot ){
		if (loot instanceof Generator.Category){
			String key = "cat_" + ((Generator.Category) loot).name().toLowerCase();
			String name = Messages.get(ModMobInfo.class, key);
			if (name.equals(Messages.NO_TEXT_FOUND)) name = ((Generator.Category) loot).name().toLowerCase();
			return name;
		} else if (loot instanceof Class){
			return effectName((Class) loot);
		} else if (loot instanceof Item){
			return Messages.titleCase(((Item) loot).name());
		}
		return null;
	}

	private static String percent( float val ){
		val *= 100f;
		if (val >= 10 || val == Math.round(val)) return Math.round(val) + "%";
		return String.format("%.1f%%", val);
	}

	private static String skillText( int value, float factor, int infinite ){
		if (value >= infinite) return Messages.get(ModMobInfo.class, "infinite");
		return Integer.toString(Math.round(value * factor));
	}

	public static String info( final Mob mob ){
		if (!Dungeon.isChallenged(Challenges.IMPROVED_COMBAT) || mob instanceof NPC || Dungeon.hero == null){
			return "";
		}

		StringBuilder sb = new StringBuilder("\n\n");
		sb.append(Messages.get(ModMobInfo.class, "title"));

		//health
		sb.append("\n").append(Messages.get(ModMobInfo.class, "hp", mob.HP, mob.HT));
		if (mob.shielding() > 0){
			sb.append(" ").append(Messages.get(ModMobInfo.class, "shield", mob.shielding()));
		}

		//accuracy and evasion, including champion modifiers
		float accEvaFactor = 1f;
		float meleeFactor = 1f;
		float dmgTakenFactor = 1f;
		for (ChampionEnemy champ : mob.buffs(ChampionEnemy.class)){
			accEvaFactor *= champ.evasionAndAccuracyFactor();
			meleeFactor *= champ.meleeDamageFactor();
			dmgTakenFactor *= champ.damageTakenFactor();
		}
		final Char hero = Dungeon.hero;
		int acc = mob.attackSkill(hero);
		int eva = mob.defenseSkill(hero);
		sb.append("\n").append(Messages.get(ModMobInfo.class, "accuracy", skillText(acc, accEvaFactor, Char.INFINITE_ACCURACY)));
		sb.append("\n").append(Messages.get(ModMobInfo.class, "evasion", skillText(eva, accEvaFactor, Char.INFINITE_EVASION)));
		//e.g. sleeping or paralyzed enemies can't dodge, show what their evasion normally is
		if (eva < mob.defenseSkill && mob.defenseSkill < Char.INFINITE_EVASION){
			sb.append(" ").append(Messages.get(ModMobInfo.class, "evasion_normal", Math.round(mob.defenseSkill * accEvaFactor)));
		}

		//damage
		int[] dmg;
		if (mob instanceof Goo){
			dmg = ((Goo) mob).damageRangeForInfo();
		} else {
			dmg = rollRange(new Roll() {
				@Override
				public int roll() {
					return mob.damageRoll();
				}
			});
		}
		if (meleeFactor != 1f){
			dmg[0] = Math.round(dmg[0]*meleeFactor);
			dmg[1] = Math.round(dmg[1]*meleeFactor);
		}
		sb.append("\n").append(Messages.get(ModMobInfo.class, "damage", range(dmg)));

		//armor
		int[] dr = rollRange(new Roll() {
			@Override
			public int roll() {
				return mob.drRoll();
			}
		});
		sb.append("\n").append(Messages.get(ModMobInfo.class, "armor", range(dr)));

		//damage reduction (multiplicative, from champion buffs)
		if (dmgTakenFactor != 1f){
			sb.append("\n").append(Messages.get(ModMobInfo.class, "dmg_reduction", percent(1f - dmgTakenFactor)));
		} else {
			sb.append("\n").append(Messages.get(ModMobInfo.class, "dmg_reduction", "0%"));
		}

		//immunities and resistances
		sb.append("\n").append(Messages.get(ModMobInfo.class, "immunities", effectList(mob.allImmunities())));
		sb.append("\n").append(Messages.get(ModMobInfo.class, "resistances", effectList(mob.allResistances())));

		//loot
		String lootName = lootName(mob.lootObject());
		float lootChance = mob.lootChance();
		if (lootName == null && lootChance <= 0){
			sb.append("\n").append(Messages.get(ModMobInfo.class, "loot_none"));
		} else {
			if (lootName == null) lootName = Messages.get(ModMobInfo.class, "loot_special");
			sb.append("\n").append(Messages.get(ModMobInfo.class, "loot", lootName, percent(Math.min(1f, lootChance))));
			if (lootChance > 0 && Dungeon.hero.lvl > mob.maxLvl + 2){
				sb.append(" ").append(Messages.get(ModMobInfo.class, "loot_too_high"));
			}
		}

		//experience
		if (mob.EXP <= 0 || mob.maxLvl <= 0){
			sb.append("\n").append(Messages.get(ModMobInfo.class, "exp_none"));
		} else {
			sb.append("\n").append(Messages.get(ModMobInfo.class, "exp", mob.EXP, mob.maxLvl));
		}
		if (mob.EXP > 0 && mob.maxLvl > 0 && Dungeon.hero.lvl > mob.maxLvl){
			sb.append(" ").append(Messages.get(ModMobInfo.class, "exp_too_high"));
		}

		return sb.toString();
	}

}
