package de.tum.cit.fop.maze.obstacles;

public enum PropType {

    /*
     * Sprite crop coordinates in pixels (top-left origin in your PNG):
     *   xPx, yTopPx, wPx, hPx
     *
     * Footprint in MAZE TILES (tilesW, tilesH):
     *   - this is what placement & collision use
     *   - tune these to match your art / desired blocking size
     */

    CRATE (227,  175, 347, 431, 1, 1, true),
    BARREL(867,  185, 340, 427, 1, 1, true),
    CHAIR (1355, 187, 408, 462, 1, 1, true),

    // bigger props
    SHELF (227,  651, 609, 609, 2, 2, true),
    TABLE (1196, 651, 663, 705, 2, 2, true);

    public final int xPx;
    public final int yTopPx;     // top-left origin
    public final int wPx;
    public final int hPx;

    public final int tilesW;
    public final int tilesH;

    public final boolean blocksMovement;

    PropType(int xPx, int yTopPx, int wPx, int hPx, int tilesW, int tilesH, boolean blocksMovement) {
        this.xPx = xPx;
        this.yTopPx = yTopPx;
        this.wPx = wPx;
        this.hPx = hPx;
        this.tilesW = tilesW;
        this.tilesH = tilesH;
        this.blocksMovement = blocksMovement;
    }
}
