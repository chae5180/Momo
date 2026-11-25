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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.Locale;

//proc chance and effect text for enchantments, glyphs and curses, and the cursed wand effect list.
//Used by the improved magic and curses challenge. Formulas mirror each effect's proc() code.
public class ModEnchantInfo {

	public static boolean active(){
		return Dungeon.isChallenged(Challenges.IMPROVED_MAGIC);
	}

	private static String pct( float chance ){
		chance = Math.max(0, chance) * 100f;
		if (chance >= 99.95f) return "100%";
		if (chance >= 10 || Math.abs(chance - Math.round(chance)) < 0.05f) return String.format(Locale.ROOT, "%.0f%%", chance);
		return String.format(Locale.ROOT, "%.1f%%", chance);
	}

	private static String num( float val ){
		if (Math.abs(val - Math.round(val)) < 0.05f) return Integer.toString(Math.round(val));
		return String.format(Locale.ROOT, "%.1f", val);
	}

	private static int itemLevel( Item item ){
		if (item == null) return 0;
		return Math.max(0, item.levelKnown ? item.buffedLvl() : 0);
	}

	//same as Weapon.Enchantment.genericProcChanceMultiplier, but without side effects and one-off boosts
	private static float weaponMulti(){
		Hero hero = Dungeon.hero;
		if (hero == null) return 1f;
		float multi = RingOfArcana.enchantPowerMultiplier(hero);
		Berserk rage = hero.buff(Berserk.class);
		if (rage != null) multi = rage.enchantFactor(multi);
		return multi;
	}

	private static float armorMulti(){
		if (Dungeon.hero == null) return 1f;
		return Armor.Glyph.genericProcChanceMultiplier(Dungeon.hero);
	}

	private static String get( String key, Object... args ){
		return Messages.get(ModEnchantInfo.class, key, args);
	}

	private static String chanceLine( float chance ){
		return "\n" + get("chance", pct(chance));
	}

	private static String effectLine( String key, Object... args ){
		return "\n" + get("effect", get(key, args));
	}

	public static String weaponInfo( Weapon.Enchantment ench, Weapon weapon ){
		if (!active() || ench == null) return "";
		int l = itemLevel(weapon);
		float m = weaponMulti();
		String s;
		float c;

		if (ench instanceof Blazing){
			c = (l+1f)/(l+3f)*m;
			int maxBurn = Math.round((3 + Dungeon.scalingDepth()/4) * 0.67f * Math.max(1f, c));
			s = chanceLine(c) + effectLine("blazing", Math.round(0.67f*Math.max(1f, c)), maxBurn);
		} else if (ench instanceof Blocking){
			c = (l+4f)/(l+40f)*m;
			s = chanceLine(c) + effectLine("blocking", Math.round(Math.max(1f, c)*(2+l)));
		} else if (ench instanceof Blooming){
			c = (l+1f)/(l+3f)*m;
			s = chanceLine(c) + effectLine("blooming", num((1.5f + 0.1f*l)*Math.max(1f, c)));
		} else if (ench instanceof Chilling){
			c = (l+1f)/(l+4f)*m;
			s = chanceLine(c) + effectLine("chilling", num(3f*Math.max(1f, c)), num(6f*Math.max(1f, c)));
		} else if (ench instanceof Corrupting){
			c = (l+5f)/(l+20f)*m;
			s = chanceLine(c) + effectLine("corrupting");
		} else if (ench instanceof Crystal){
			s = "\n" + get("always") + effectLine("crystal");
		} else if (ench instanceof Elastic){
			c = (l+1f)/(l+5f)*m;
			s = chanceLine(c) + effectLine("elastic", Math.round(2*Math.max(1f, c)));
		} else if (ench instanceof Eldritch){
			c = (l+1f)/(l+4f)*m;
			s = chanceLine(c) + effectLine("eldritch", num(5f*Math.max(1f, c)));
		} else if (ench instanceof Grim){
			float max = (0.5f + 0.05f*l)*m;
			s = "\n" + get("grim_chance", pct(max), pct(max*0.25f)) + effectLine("grim");
		} else if (ench instanceof Kinetic){
			s = "\n" + get("always") + effectLine("kinetic");
		} else if (ench instanceof Lucky){
			c = (l+4f)/(l+40f)*m;
			s = chanceLine(c) + effectLine("lucky");
		} else if (ench instanceof Projecting){
			s = "\n" + get("always") + effectLine("projecting", Math.round(m));
		} else if (ench instanceof Shocking){
			c = 1/3f*m;
			s = chanceLine(c) + effectLine("shocking", Math.round(50*Math.max(1f, c)));
		} else if (ench instanceof Unstable){
			s = "\n" + get("always") + effectLine("unstable");
		} else if (ench instanceof Vampiric){
			Hero h = Dungeon.hero;
			float missing = h == null ? 0 : (h.HT - h.HP)/(float)h.HT;
			c = (0.05f + 0.25f*missing)*m;
			s = "\n" + get("vampiric_chance", pct(0.05f*m), pct(0.30f*m), pct(c))
					+ effectLine("vampiric", Math.round(50*Math.max(1f, c)));
		} else if (ench instanceof Venomous){
			c = 1/3f*m;
			s = chanceLine(c) + effectLine("venomous", num(Math.max(1f, c)*((l/2f)+3)));
		} else if (ench instanceof Vorpal){
			c = 1/4f*m;
			s = chanceLine(c) + effectLine("vorpal");

		//curses
		} else if (ench instanceof Annoying){
			s = chanceLine(1/20f*m) + effectLine("annoying");
		} else if (ench instanceof Dazzling){
			s = chanceLine(1/10f*m) + effectLine("dazzling");
		} else if (ench instanceof Displacing){
			s = chanceLine(1/12f*m) + effectLine("displacing");
		} else if (ench instanceof Explosive){
			s = "\n" + get("always") + effectLine("explosive", num(5*m));
		} else if (ench instanceof Friendly){
			s = chanceLine(1/10f*m) + effectLine("friendly");
		} else if (ench instanceof Polarized){
			s = chanceLine(1/2f*m) + effectLine("polarized");
		} else if (ench instanceof Pressurized){
			s = chanceLine(1/8f*m) + effectLine("pressurized");
		} else if (ench instanceof Sacrificial){
			s = chanceLine(1/10f*m) + effectLine("sacrificial");
		} else if (ench instanceof Wayward){
			s = chanceLine(1/4f*m) + effectLine("wayward");
		} else if (ench instanceof Wondrous){
			s = chanceLine(1/8f*m) + effectLine("wondrous") + cursedEffectList();
		} else {
			return "";
		}

		return "\n\n" + get("title") + s;
	}

	public static String armorInfo( Armor.Glyph glyph, Armor armor ){
		if (!active() || glyph == null) return "";
		int l = itemLevel(armor);
		float m = armorMulti();
		String s;
		float c;

		if (glyph instanceof Affection){
			c = (l+3f)/(l+20f)*m;
			s = chanceLine(c) + effectLine("affection", Math.round(10*Math.max(1f, c)));
		} else if (glyph instanceof AntiMagic){
			s = "\n" + get("always") + effectLine("antimagic", Math.round(l*m), Math.round((3 + l*1.5f)*m));
		} else if (glyph instanceof Brimstone){
			s = "\n" + get("always") + effectLine("brimstone");
		} else if (glyph instanceof Camouflage){
			s = "\n" + get("always") + effectLine("camouflage", Math.round((3 + l/2f)*m));
		} else if (glyph instanceof Entanglement){
			c = 1/4f*m;
			s = chanceLine(c) + effectLine("entanglement", Math.round((5 + 2*l)*Math.max(1f, c)));
		} else if (glyph instanceof Flow){
			s = "\n" + get("always") + effectLine("flow", num((2f + 0.5f*l)*m));
		} else if (glyph instanceof Obfuscation){
			s = "\n" + get("always") + effectLine("obfuscation", num((1 + l/3f)*m));
		} else if (glyph instanceof Potential){
			c = (l+1f)/(l+6f)*m;
			s = chanceLine(c) + effectLine("potential");
		} else if (glyph instanceof Repulsion){
			c = (l+1f)/(l+5f)*m;
			s = chanceLine(c) + effectLine("repulsion", Math.round(2*Math.max(1f, c)));
		} else if (glyph instanceof Stone){
			s = "\n" + get("always") + effectLine("stone");
		} else if (glyph instanceof Swiftness){
			s = "\n" + get("always") + effectLine("swiftness", num((1.2f + 0.04f*l)*m));
		} else if (glyph instanceof Thorns){
			c = (l+2f)/(l+12f)*m;
			s = chanceLine(c) + effectLine("thorns", Math.round((4 + l)*Math.max(1f, c)));
		} else if (glyph instanceof Viscosity){
			float p = (l+1f)/(l+6f)*m;
			s = "\n" + get("always") + effectLine("viscosity", pct(Math.min(1f, p)));

		//curses
		} else if (glyph instanceof AntiEntropy){
			s = chanceLine(1/8f*m) + effectLine("antientropy");
		} else if (glyph instanceof Bulk){
			s = "\n" + get("always") + effectLine("bulk", num(1/3f*m));
		} else if (glyph instanceof Corrosion){
			s = chanceLine(1/10f*m) + effectLine("corrosion");
		} else if (glyph instanceof Displacement){
			s = chanceLine(1/20f*m) + effectLine("displacement");
		} else if (glyph instanceof Metabolism){
			s = chanceLine(1/6f*m) + effectLine("metabolism");
		} else if (glyph instanceof Multiplicity){
			s = chanceLine(1/20f*m) + effectLine("multiplicity");
		} else if (glyph instanceof Overgrowth){
			s = chanceLine(1/20f*m) + effectLine("overgrowth");
		} else if (glyph instanceof Stench){
			s = chanceLine(1/8f*m) + effectLine("stench");
		} else {
			return "";
		}

		return "\n\n" + get("title") + s;
	}

	// ***** cursed wand effects *****

	private static final String[][] CURSED_EFFECTS = new String[][]{
			{"burnandfreeze", "spawnregrowth", "randomteleport", "randomgas", "randomareaeffect", "bubbles", "randomwand", "selfooze"},
			{"randomplant", "healthtransfer", "explosion", "lightningbolt", "geyser", "summonsheep", "levitate", "alarm"},
			{"sheeppolymorph", "curseequipment", "interfloorteleport", "summonmonsters", "fireball", "coneofcolors", "massinvuln", "petrify"},
			{"forestfire", "spawngoldenmimic", "abortretryfail", "randomtransmogrify", "heroshapeshift", "supernova", "sinkhole", "gravitychaos"}
	};
	//matches CursedWand's category chances, split evenly between the 8 effects of each category
	private static final float[] CATEGORY_CHANCES = new float[]{0.60f, 0.30f, 0.09f, 0.01f};

	public static boolean showCursedWandList(){
		return active() && SPDSettings.cursedWandList();
	}

	//the list of possible cursed wand effects, if enabled
	public static String cursedEffectList(){
		if (!showCursedWandList()) return "";
		StringBuilder sb = new StringBuilder("\n\n" + get("cw_title"));
		float positive = WondrousResin.positiveCurseEffectChance();
		if (positive > 0){
			sb.append("\n").append(get("cw_resin", pct(positive)));
		}
		for (int cat = 0; cat < CURSED_EFFECTS.length; cat++){
			float each = CATEGORY_CHANCES[cat] / CURSED_EFFECTS[cat].length;
			sb.append("\n\n_").append(get("cw_cat_" + cat, pct(CATEGORY_CHANCES[cat]), pct(each))).append("_");
			for (String key : CURSED_EFFECTS[cat]){
				sb.append("\n- ").append(get("cw_" + key));
			}
		}
		return sb.toString();
	}

	public static String cursedWandInfo(){
		if (!active()) return "";
		if (!showCursedWandList()) return "\n\n" + get("cw_hidden");
		return cursedEffectList();
	}

	//used by tests to make sure every effect is listed
	public static int cursedEffectCount(){
		int count = 0;
		for (String[] cat : CURSED_EFFECTS) count += cat.length;
		return count;
	}

}
