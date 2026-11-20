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

package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

// [Momo mod] A homemade cookie: fills you up a little and heals 25% of max HP over a few turns.
public class MomoCookie extends Food {

	{
		image = ItemSpriteSheet.PASTY;
		energy = Hunger.HUNGRY/2f;
	}

	@Override
	protected void satisfy( Hero hero ) {
		super.satisfy( hero );

		//respects the "Pharmacophobia" (no healing) challenge
		if (Dungeon.isChallenged(Challenges.NO_HEALING)) return;

		int heal = Math.max(1, Math.round(hero.HT * 0.25f));
		Buff.affect(hero, Healing.class).setHeal(heal, 0.25f, 0);
		GLog.p( Messages.get(this, "heal") );
	}

	@Override
	public int value() {
		return 15 * quantity;
	}
}
