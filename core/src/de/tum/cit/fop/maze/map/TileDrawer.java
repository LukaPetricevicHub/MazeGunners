package de.tum.cit.fop.maze.map;

/**
 * Callback interface for drawing a tile at a world position.
 */
public interface TileDrawer {
    /**
     * Draws a tile.
     *
     * @param tileId Numeric tile id
     * @param cellX Tile x-coordinate
     * @param cellY Tile y-coordinate
     * @param worldX World x-coordinate
     * @param worldY World y-coordinate
     * @param tileSize Tile size in world units
     */
    void drawTile(int tileId, int cellX, int cellY, int worldX, int worldY, int tileSize);
}

