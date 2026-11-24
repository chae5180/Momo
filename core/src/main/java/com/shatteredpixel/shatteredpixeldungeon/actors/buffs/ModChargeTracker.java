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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;

//Tracks charges and cooldowns for the improved charge challenge.
//Notifies the player when cooldowns end and when items become fully charged, and measures artifact charge rates.
public class ModChargeTracker extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
		//act after all other buffs, so that we see the end result of each turn
		actPriority = BUFF_PRIO - 10;
	}

	//buffs which represent a cooldown, beyond those with 'Cooldown' in their class name
	private static final HashSet<Class<? extends Buff>> EXTRA_COOLDOWNS = new HashSet<>(Arrays.asList(
			Shuriken.ShurikenInstantTracker.class
	));

	public static boolean isCooldown( Buff b ){
		if (b == null || b.icon() == BuffIndicator.NONE) return false;
		if (EXTRA_COOLDOWNS.contains(b.getClass())) return true;
		return b.getClass().getSimpleName().contains("Cooldown");
	}

	public static boolean active(){
		return Dungeon.isChallenged(Challenges.IMPROVED_CHARGE);
	}

	private boolean initialized = false;

	//cooldown buffs currently on the hero, and their names
	private LinkedHashMap<Class<? extends Buff>, String> cooldowns = new LinkedHashMap<>();
	//whether each item was fully charged last turn
	private IdentityHashMap<Item, Boolean> wasFull = new IdentityHashMap<>();
	//last artifact cooldown value
	private IdentityHashMap<Artifact, Integer> lastArtiCooldown = new IdentityHashMap<>();
	//last observed charge value, and average charge per turn, for artifacts
	private IdentityHashMap<Artifact, Float> lastArtiCharge = new IdentityHashMap<>();
	private static IdentityHashMap<Artifact, Float> artiRates = new IdentityHashMap<>();
	private static IdentityHashMap<Artifact, Integer> artiSamples = new IdentityHashMap<>();

	private boolean weaponWasFull = false;

	@Override
	public boolean act() {

		if (!active() || !(target instanceof Hero)){
			detach();
			return true;
		}

		Hero hero = (Hero)target;
		boolean notify = initialized;

		//*** cooldown buffs ***
		LinkedHashMap<Class<? extends Buff>, String> current = new LinkedHashMap<>();
		for (Buff b : hero.buffs()){
			if (isCooldown(b)){
				current.put(b.getClass(), b.name());
			}
		}
		if (notify){
			for (Class<? extends Buff> cls : cooldowns.keySet()){
				if (!current.containsKey(cls)){
					GLog.p(Messages.get(ModChargeTracker.class, "cooldown_over", Messages.titleCase(cooldowns.get(cls))));
				}
			}
		}
		cooldowns = current;

		//*** wands ***
		IdentityHashMap<Item, Boolean> nowFull = new IdentityHashMap<>();
		for (Wand w : hero.belongings.getAllItems(Wand.class)){
			if (!w.curChargeKnown) continue;
			boolean full = w.curCharges >= w.maxCharges;
			if (notify && full && wasFull.containsKey(w) && !wasFull.get(w)){
				GLog.p(Messages.get(ModChargeTracker.class, "fully_charged", Messages.titleCase(w.name())));
			}
			nowFull.put(w, full);
		}

		//*** artifacts ***
		ArrayList<Artifact> artifacts = new ArrayList<>();
		if (hero.belongings.artifact instanceof Artifact) artifacts.add((Artifact) hero.belongings.artifact);
		if (hero.belongings.misc instanceof Artifact) artifacts.add((Artifact) hero.belongings.misc);

		IdentityHashMap<Artifact, Integer> nowCooldown = new IdentityHashMap<>();
		IdentityHashMap<Artifact, Float> nowCharge = new IdentityHashMap<>();
		for (Artifact a : artifacts){
			if (!a.isIdentified() || a.cursed) continue;

			int cd = a.cooldownLeft();
			if (notify && cd == 0 && lastArtiCooldown.containsKey(a) && lastArtiCooldown.get(a) > 0){
				GLog.p(Messages.get(ModChargeTracker.class, "cooldown_over", Messages.titleCase(a.name())));
			}
			nowCooldown.put(a, cd);

			if (a.chargeCapAmount() > 0){
				boolean full = a.chargeAmount() >= a.chargeCapAmount();
				if (notify && full && wasFull.containsKey(a) && !wasFull.get(a)){
					GLog.p(Messages.get(ModChargeTracker.class, "fully_charged", Messages.titleCase(a.name())));
				}
				nowFull.put(a, full);

				//measure charge rate, only while not full
				float value = a.chargeAmount() + a.partialChargeAmount();
				if (!full && lastArtiCharge.containsKey(a)){
					float delta = value - lastArtiCharge.get(a);
					//charge being spent shows up as a negative delta, ignore those turns
					if (delta >= 0){
						float rate = artiRates.containsKey(a) ? artiRates.get(a) : delta;
						int samples = artiSamples.containsKey(a) ? artiSamples.get(a) : 0;
						//running average for the first few turns, then an exponential moving average
						float weight = Math.max(1f/(samples+1), 0.05f);
						rate = rate + (delta - rate)*weight;
						artiRates.put(a, rate);
						artiSamples.put(a, samples+1);
					}
				}
				nowCharge.put(a, value);
			}
		}
		lastArtiCooldown = nowCooldown;
		lastArtiCharge = nowCharge;
		//forget measurements for artifacts which are no longer equipped
		artiRates.keySet().retainAll(artifacts);
		artiSamples.keySet().retainAll(artifacts);

		wasFull = nowFull;

		//*** duelist weapon charges ***
		MeleeWeapon.Charger charger = hero.buff(MeleeWeapon.Charger.class);
		if (charger != null){
			boolean full = charger.charges >= charger.chargeCap();
			if (notify && full && !weaponWasFull){
				GLog.p(Messages.get(ModChargeTracker.class, "weapon_charged"));
			}
			weaponWasFull = full;
		}

		initialized = true;
		spend(TICK);
		return true;
	}

	// ***** info strings *****

	private static String rateText( float perTurn ){
		if (perTurn <= 0){
			return Messages.get(ModChargeTracker.class, "rate_none");
		}
		float turns = 1f/perTurn;
		return Messages.get(ModChargeTracker.class, "rate", String.format("%.1f", perTurn*100f), String.format("%.1f", turns));
	}

	private static String progressText( float partial ){
		return Messages.get(ModChargeTracker.class, "progress", Math.round(Math.max(0, Math.min(1, partial))*100f));
	}

	public static String wandInfo( Wand wand ){
		if (!active() || !wand.curChargeKnown) return "";
		String info = "\n\n" + Messages.get(ModChargeTracker.class, "charges", wand.curCharges, wand.maxCharges);
		if (wand.curCharges >= wand.maxCharges){
			info += " " + Messages.get(ModChargeTracker.class, "full");
		} else {
			info += " " + progressText(wand.partialCharge);
			Wand.Charger charger = wand.charger();
			if (charger != null && charger.target != null){
				info += "\n" + rateText(charger.chargePerTurn());
			}
		}
		return info;
	}

	public static String artifactInfo( Artifact artifact ){
		if (!active() || !artifact.isIdentified() || artifact.cursed) return "";
		String info = "";
		if (artifact.cooldownLeft() > 0){
			info += "\n\n" + Messages.get(ModChargeTracker.class, "item_cooldown", artifact.cooldownLeft());
		}
		if (artifact.chargeCapAmount() > 0){
			info += "\n\n" + Messages.get(ModChargeTracker.class, "charges", artifact.chargeAmount(), artifact.chargeCapAmount());
			if (artifact.chargeAmount() >= artifact.chargeCapAmount()){
				info += " " + Messages.get(ModChargeTracker.class, "full");
			} else {
				info += " " + progressText(artifact.partialChargeAmount());
				if (Dungeon.hero != null && artifact.isEquipped(Dungeon.hero)){
					if (artiRates.containsKey(artifact) && artiSamples.get(artifact) >= 5){
						info += "\n" + Messages.get(ModChargeTracker.class, "measured") + " " + rateText(artiRates.get(artifact));
					} else {
						info += "\n" + Messages.get(ModChargeTracker.class, "measuring");
					}
				}
			}
		}
		return info;
	}

	public static String weaponInfo(){
		if (!active() || Dungeon.hero == null) return "";
		MeleeWeapon.Charger charger = Dungeon.hero.buff(MeleeWeapon.Charger.class);
		if (charger == null) return "";
		String info = "\n\n" + Messages.get(ModChargeTracker.class, "weapon_charges", charger.charges, charger.chargeCap());
		if (charger.charges >= charger.chargeCap()){
			info += " " + Messages.get(ModChargeTracker.class, "full");
		} else {
			info += " " + progressText(charger.partialCharge);
			info += "\n" + rateText(charger.chargePerTurn());
		}
		return info;
	}

	//remaining duration and progress of a cooldown buff
	public static String cooldownInfo( Buff b ){
		if (!active() || !isCooldown(b)) return "";
		return "\n\n" + Messages.get(ModChargeTracker.class, "buff_cooldown", String.format("%.0f", Math.ceil(b.cooldown())));
	}

}
