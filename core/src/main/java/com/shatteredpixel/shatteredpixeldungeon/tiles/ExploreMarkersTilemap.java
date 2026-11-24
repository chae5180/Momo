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

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ModChallenges;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;

//draws small crosses over regular floor and wall tiles which haven't been stepped on or searched
// used by the improved explore challenge. One instance handles floors, another handles walls
public class ExploreMarkersTilemap extends Tilemap {

	public static final int SIZE = 16;

	private static final String TEXTURE_KEY = "mod_explore_markers_v2";

	//colors, as returned by ModChallenges.exploreMarker
	public static final int RED_CROSS   = 0;
	public static final int BLUE_CROSS  = 1;

	//marker placement variants, so the cross stays visible around raised walls
	private static final int FLOOR_CENTER   = 0;
	private static final int FLOOR_HIGH     = 1; //floor with a wall below it, which overlaps the bottom of the tile
	private static final int WALL_CENTER    = 2; //wall face, visible from below
	private static final int WALL_LEFT      = 3; //side wall, visible from the left
	private static final int WALL_RIGHT     = 4; //side wall, visible from the right
	private static final int WALL_TOP       = 5; //bottom wall, visible from above
	private static final int VARIANTS       = 6;

	private final boolean walls;

	public ExploreMarkersTilemap( boolean walls ) {
		super( texture(), new TextureFilm( texture(), SIZE, SIZE ) );
		this.walls = walls;
		map( new int[Dungeon.level.length()], Dungeon.level.width() );
	}

	private static SmartTexture texture(){
		boolean fresh = !TextureCache.contains(TEXTURE_KEY);
		SmartTexture tx = TextureCache.create(TEXTURE_KEY, SIZE*VARIANTS, SIZE*2);
		if (fresh){
			Pixmap pm = tx.bitmap;
			pm.setBlending(Pixmap.Blending.None);
			pm.setColor(0);
			pm.fill();
			int[] colors = new int[]{0xFF3030D8, 0x40A0FFD8}; //RGBA
			for (int c = 0; c < 2; c++){
				int y = c*SIZE;
				drawCross(pm, FLOOR_CENTER*SIZE + 6,  y + 6, colors[c]);
				drawCross(pm, FLOOR_HIGH*SIZE + 6,    y + 3, colors[c]);
				drawCross(pm, WALL_CENTER*SIZE + 6,   y + 6, colors[c]);
				drawCross(pm, WALL_LEFT*SIZE + 0,     y + 6, colors[c]);
				drawCross(pm, WALL_RIGHT*SIZE + 11,   y + 6, colors[c]);
				drawCross(pm, WALL_TOP*SIZE + 6,      y + 0, colors[c]);
			}
			tx.filter(Texture.NEAREST, Texture.NEAREST);
			tx.bitmap(pm);
		}
		return tx;
	}

	//draws a small 5x5 cross with its top-left at x,y, with a subtle shadow below it. Colors are RGBA
	private static void drawCross( Pixmap pm, int x, int y, int color ){
		int shadow = 0x00000080;
		for (int i = 0; i < 5; i++){
			pm.drawPixel(x + i, y + 1 + i, shadow);
			pm.drawPixel(x + 4 - i, y + 1 + i, shadow);
		}
		for (int i = 0; i < 5; i++){
			pm.drawPixel(x + i, y + i, color);
			pm.drawPixel(x + 4 - i, y + i, color);
		}
	}

	private boolean wallAt( int cell ){
		return cell < 0 || cell >= data.length || DungeonTileSheet.wallStitcheable(Dungeon.level.map[cell]);
	}

	private int variant( int cell ){
		int w = mapWidth;
		if (!walls){
			return wallAt(cell + w) ? FLOOR_HIGH : FLOOR_CENTER;
		}
		int x = cell % w;
		if (!wallAt(cell + w))              return WALL_CENTER;
		if (x > 0 && !wallAt(cell - 1))     return WALL_LEFT;
		if (x < w-1 && !wallAt(cell + 1))   return WALL_RIGHT;
		if (!wallAt(cell - w))              return WALL_TOP;
		return WALL_CENTER;
	}

	private void refreshCell( int cell ){
		if (data != null && cell >= 0 && cell < data.length) {
			int color = ModChallenges.exploreMarker(cell, walls);
			data[cell] = color < 0 ? -1 : variant(cell) + color*VARIANTS;
		}
	}

	@Override
	public synchronized void updateMap() {
		if (data != null) {
			for (int i = 0; i < data.length; i++) {
				refreshCell(i);
			}
		}
		super.updateMap();
	}

	@Override
	public synchronized void updateMapCell( int cell ) {
		refreshCell(cell);
		super.updateMapCell(cell);
		//a change to this cell may change how neighbouring markers are placed
		if (data != null){
			for (int n : new int[]{cell-1, cell+1, cell-mapWidth, cell+mapWidth}){
				if (n >= 0 && n < data.length){
					refreshCell(n);
					super.updateMapCell(n);
				}
			}
		}
	}

	//updates an inclusive rectangle of cells
	public synchronized void updateArea( int left, int top, int right, int bottom ){
		for (int y = top; y <= bottom; y++){
			for (int x = left; x <= right; x++){
				int cell = x + y*mapWidth;
				refreshCell(cell);
				super.updateMapCell(cell);
			}
		}
	}

}
