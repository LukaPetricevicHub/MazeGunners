package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class WallSet {
    public final Texture texture;

    // Corners
    public final TextureRegion cornerTL;
    public final TextureRegion cornerTR;
    public final TextureRegion cornerBL;
    public final TextureRegion cornerBR;

    // Straights
    public final TextureRegion edgeTop;
    public final TextureRegion edgeBottom;
    public final TextureRegion edgeLeft;
    public final TextureRegion edgeRight;

    // Fallback/inside fill (optional)
    public final TextureRegion fill;

    /**
     * Assumes the wall tiles are on a spritesheet grid.
     * Change the (col,row) mapping to match your tileset layout.
     */
    public WallSet(Texture texture, int tileSize) {
        this.texture = texture;
        TextureRegion[][] t = TextureRegion.split(texture, tileSize, tileSize);

        // >>> YOU MUST MAP THESE TO YOUR TILESET <<<
        // Example mapping (row 0 = top row):
        cornerTL  = t[0][0];
        edgeTop   = t[0][1];
        cornerTR  = t[0][2];

        edgeLeft  = t[1][0];
        fill      = t[1][1];
        edgeRight = t[1][2];

        cornerBL  = t[2][0];
        edgeBottom= t[2][1];
        cornerBR  = t[2][2];
    }

    public void dispose() {
        texture.dispose();
    }
}
