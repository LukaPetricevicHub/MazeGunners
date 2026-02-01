package de.tum.cit.fop.maze.util;

/**
 * Utility methods for grid and tile coordinate conversions.
 *
 * This class provides simple helper functions for working with
 * grid-based maps and converting between world and tile coordinates.
 */
public class GridUtils {

    /**
     * Checks whether a grid coordinate is within bounds.
     */
    public static boolean inBounds(int x, int y, int width, int height){
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Converts a tile index to a world position.
     */
    public static int worldToTile(float pos, int tileSize){
        return (int) (pos / tileSize);
    }

    public static int tileToWorld(int tile, int tileSize){
        return tile * tileSize;
    }

}
