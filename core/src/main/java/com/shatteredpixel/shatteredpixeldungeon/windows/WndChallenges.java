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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.ModChallenges;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.Holiday;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

public class WndChallenges extends Window {

	private static final int WIDTH		= 120;
	private static final int TTL_HEIGHT = 16;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP        = 1;

	private boolean editable;
	private boolean scoringEditable;
	private ArrayList<ChallengeBox> boxes;
	private ArrayList<InfoButton> infos;

	private int pastyVariant;
	private PastyButton pastyButton;

	public WndChallenges( int checked, boolean editable ) {
		this( checked, editable, editable );
	}

	//scoringEditable allows the score-affecting challenges to be locked while mod challenges can still be toggled
	public WndChallenges( int checked, boolean editable, boolean scoringEditable ) {

		super();

		this.editable = editable;
		this.scoringEditable = editable && scoringEditable;
		pastyVariant = editable ? SPDSettings.pastyVariant() : ModChallenges.pastyVariant;

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 12 );
		title.hardlight( TITLE_COLOR );
		title.setPos(
				(WIDTH - title.width()) / 2,
				(TTL_HEIGHT - title.height()) / 2
		);
		PixelScene.align(title);
		add( title );

		boxes = new ArrayList<>();
		infos = new ArrayList<>();

		ScrollPane pane = new ScrollPane(new Component()){
			@Override
			public void onClick(float x, float y) {
				for (ChallengeBox cb : boxes){
					if (cb.active && cb.inside(x, y)){
						cb.click();
						return;
					}
				}
				for (InfoButton info : infos){
					if (info.inside(x, y)){
						info.click();
						return;
					}
				}
				if (pastyButton != null && pastyButton.active && pastyButton.inside(x, y)){
					pastyButton.click();
				}
			}
		};
		add(pane);
		Component content = pane.content();

		float pos = 0;
		for (int i=0; i < Challenges.NAME_IDS.length; i++) {

			final String challenge = Challenges.NAME_IDS[i];
			final int mask = Challenges.MASKS[i];
			boolean scoring = Challenges.isScoring(mask);

			//section headers
			if (i == 0 || (scoring != Challenges.isScoring(Challenges.MASKS[i-1]))){
				if (i != 0) pos += 3;
				RenderedTextBlock header = PixelScene.renderTextBlock(
						Messages.get(this, scoring
								? (editable && !this.scoringEditable ? "scoring_header_locked" : "scoring_header")
								: "mod_header"), 7 );
				header.hardlight( scoring ? TITLE_COLOR : 0x88CCFF );
				header.maxWidth(WIDTH);
				header.setPos(0, pos);
				PixelScene.align(header);
				content.add(header);
				pos = header.bottom() + 1;

				ColorBlock sep = new ColorBlock(WIDTH, 1, 0xFF222222);
				sep.y = pos;
				content.add(sep);
				pos += 2;
			}

			ChallengeBox cb = new ChallengeBox( Messages.titleCase(Messages.get(Challenges.class, challenge)) );
			cb.checked( (checked & mask) != 0 );
			cb.active = scoring ? this.scoringEditable : this.editable;
			cb.setRect( 0, pos, WIDTH-16, BTN_HEIGHT );

			content.add( cb );
			boxes.add( cb );

			if (!cb.active && editable && scoring){
				//locked, but visible
				cb.alpha(0.4f);
			}
			
			InfoButton info = new InfoButton(){
				@Override
				protected void onClick() {
					super.onClick();
					ShatteredPixelDungeon.scene().add(
							new WndMessage(Messages.get(Challenges.class, challenge+"_desc"))
					);
				}
			};
			info.setRect(cb.right(), pos, 16, BTN_HEIGHT);
			content.add(info);
			infos.add(info);
			
			pos = cb.bottom() + GAP;

			//pasty replacement selection for the improved food challenge
			if (mask == Challenges.IMPROVED_FOOD){
				pastyButton = new PastyButton();
				pastyButton.active = editable;
				pastyButton.setRect(8, pos, WIDTH-8, BTN_HEIGHT);
				content.add(pastyButton);
				pastyButton.refresh();
				pos = pastyButton.bottom() + GAP;
			}
		}

		content.setSize(WIDTH, pos);

		int maxHeight = (int)(PixelScene.uiCamera.height * 0.9f) - TTL_HEIGHT - 10;
		int paneHeight = (int)Math.min(pos, maxHeight);

		resize( WIDTH, TTL_HEIGHT + paneHeight );
		pane.setRect(0, TTL_HEIGHT, WIDTH, paneHeight);
	}

	@Override
	public void onBackPressed() {

		if (editable) {
			int value = 0;
			for (int i=0; i < boxes.size(); i++) {
				if (boxes.get( i ).checked()) {
					value |= Challenges.MASKS[i];
				}
			}
			if (!scoringEditable){
				value &= Challenges.MOD_MASK;
			}
			SPDSettings.challenges( value );
			SPDSettings.pastyVariant( pastyVariant );
		}

		super.onBackPressed();
	}

	private static class InfoButton extends IconButton {

		public InfoButton() {
			super(Icons.get(Icons.INFO));
		}

		public void click(){
			onClick();
		}

	}

	private static class ChallengeBox extends CheckBox {

		public ChallengeBox(String label) {
			super(label);
		}

		public void click(){
			onClick();
		}

	}

	private static Image pastyIcon( Holiday variant ){
		switch (variant){
			case NONE: default:         return new ItemSprite(ItemSpriteSheet.PASTY);
			case LUNAR_NEW_YEAR:        return new ItemSprite(ItemSpriteSheet.STEAMED_FISH);
			case APRIL_FOOLS:           return new ItemSprite(ItemSpriteSheet.CHOC_AMULET);
			case EASTER:                return new ItemSprite(ItemSpriteSheet.EASTER_EGG);
			case PRIDE:                 return new ItemSprite(ItemSpriteSheet.RAINBOW_POTION);
			case SHATTEREDPD_BIRTHDAY:  return new ItemSprite(ItemSpriteSheet.SHATTERED_CAKE);
			case HALLOWEEN:             return new ItemSprite(ItemSpriteSheet.PUMPKIN_PIE);
			case PD_BIRTHDAY:           return new ItemSprite(ItemSpriteSheet.VANILLA_CAKE);
			case WINTER_HOLIDAYS:       return new ItemSprite(ItemSpriteSheet.CANDY_CANE);
			case NEW_YEARS:             return new ItemSprite(ItemSpriteSheet.SPARKLING_POTION);
		}
	}

	private class PastyButton extends RedButton {

		public PastyButton() {
			super("", 6);
		}

		public void click(){
			onClick();
		}

		public void refresh(){
			Holiday variant = Holiday.values()[pastyVariant];
			text(Messages.get(WndChallenges.class, "pasty_variant", ModChallenges.pastyVariantName(variant)));
			icon(pastyIcon(variant));
			layout();
		}

		@Override
		protected void onClick() {
			super.onClick();
			String[] options = new String[ModChallenges.PASTY_VARIANTS.length];
			for (int i = 0; i < options.length; i++){
				if (ModChallenges.PASTY_VARIANTS[i] == Holiday.NONE){
					options[i] = Messages.get(WndChallenges.class, "pasty_default");
				} else {
					options[i] = ModChallenges.pastyVariantName(ModChallenges.PASTY_VARIANTS[i]);
				}
			}
			ShatteredPixelDungeon.scene().addToFront(new WndOptions(
					new ItemSprite(ItemSpriteSheet.PASTY),
					Messages.get(WndChallenges.class, "pasty_title"),
					Messages.get(WndChallenges.class, "pasty_message"),
					options){
				@Override
				protected void onSelect(int index) {
					pastyVariant = ModChallenges.PASTY_VARIANTS[index].ordinal();
					refresh();
				}

				@Override
				protected boolean hasIcon(int index) {
					return true;
				}

				@Override
				protected Image getIcon(int index) {
					return pastyIcon(ModChallenges.PASTY_VARIANTS[index]);
				}
			});
		}
	}
}
