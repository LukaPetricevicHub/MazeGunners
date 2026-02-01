package de.tum.cit.fop.maze.util;

/**
 * Utility methods for grid and tile coordinate conversions.
 *
 * This class provides simple helper functions for working with
 * grid-based maps and converting between world and tile coordinates.
 */
/**
 * Utility methods for grid and tile coordinate conversions.
 */
public class GridUtils {

    /**
     * Checks whether a grid coordinate is within bounds.
     *
     * @param x x-coordinate to check
     * @param y y-coordinate to check
     * @param width grid width
     * @param height grid height
     * @return true if the coordinate lies within bounds
     */
    public static boolean inBounds(int x, int y, int width, int height){
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Converts a tile index to a world position.
     *
     * @param pos world position
     * @param tileSize tile size in world units
     * @return tile index
     */
    public static int worldToTile(float pos, int tileSize){
        return (int) (pos / tileSize);
    }

    /**
     * Converts a tile index to a world position.
     *
     * @param tile tile index
     * @param tileSize tile size in world units
     * @return world position
     */
    public static int tileToWorld(int tile, int tileSize){
        return tile * tileSize;
    }

}
