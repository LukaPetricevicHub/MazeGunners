package de.tum.cit.fop.maze.util;

public class GridUtils {
    public static boolean inBounds(int x, int y, int width, int height){
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public static int worldToTile(float pos, int tileSize){
        return (int) (pos / tileSize);
    }

    public static int tileToWorld(int tile, int tileSize){
        return tile * tileSize;
    }


}
