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

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

public class WndTitledMessage extends Window {

	protected static final int WIDTH_MIN    = 120;
	protected static final int WIDTH_MAX    = 220;
	protected static final int GAP	= 2;

	public WndTitledMessage( Image icon, String title, String message ) {
		
		this( new IconTitle( icon, title ), message );

	}
	
	public WndTitledMessage( Component titlebar, String message ) {

		super();

		int width = WIDTH_MIN;

		titlebar.setRect( 0, 0, width, 0 );
		add(titlebar);

		RenderedTextBlock text = PixelScene.renderTextBlock( 6 );
		if (!useHighlighting()) text.setHightlighting(false);
		text.text( message, width );
		text.setPos( titlebar.left(), titlebar.bottom() + 2*GAP );
		add( text );

		while (PixelScene.landscape()
				&& text.bottom() > targetHeight()
				&& width < WIDTH_MAX){
			width += 20;
			titlebar.setRect(0, 0, width, 0);
			text.setPos( titlebar.left(), titlebar.bottom() + 2*GAP );
			text.maxWidth(width);
		}

		bringToFront(titlebar);

		resize( width, (int)text.bottom() + 2 );

		makeScrollable(titlebar, text, width, 10);
	}

	//mod: very long text (e.g. detailed info from mod challenges) scrolls instead of going off screen
	private ScrollPane infoPane;

	private void makeScrollable( Component above, RenderedTextBlock text, int width, float reservedBelow ){
		float maxHeight = PixelScene.uiCamera.height * 0.85f - above.bottom() - reservedBelow;
		if (text.height() <= maxHeight) return;

		remove(text);
		resize( width, (int)(above.bottom() + 2*GAP + maxHeight + 2) );

		infoPane = new ScrollPane(new Component());
		add(infoPane);
		infoPane.setRect(0, above.bottom() + 2*GAP, width, maxHeight);
		Component content = infoPane.content();
		text.setPos(0, 0);
		content.add(text);
		content.setSize(width, text.height() + 2);
		infoPane.setRect(0, above.bottom() + 2*GAP, width, maxHeight);
	}

	@Override
	public void resize(int w, int h) {
		super.resize(w, h);
		if (infoPane != null) infoPane.setRect(infoPane.left(), infoPane.top(), infoPane.width(), infoPane.height());
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (infoPane != null) infoPane.setRect(infoPane.left(), infoPane.top(), infoPane.width(), infoPane.height());
	}


	protected boolean useHighlighting(){
		return true;
	}

	protected float targetHeight() {
		return PixelScene.MIN_HEIGHT_L - 10;
	}
}
