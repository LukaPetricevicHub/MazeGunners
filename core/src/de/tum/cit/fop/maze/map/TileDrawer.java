package de.tum.cit.fop.maze.map;

public interface TileDrawer {
    void drawTile(int tileId, int cellX, int cellY, int worldX, int worldY, int tileSize);
}

