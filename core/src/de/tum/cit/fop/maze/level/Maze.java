package de.tum.cit.fop.maze.level;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tum.cit.fop.maze.util.GridUtils;

public class Maze {
    private final int width;
    private final int height;
    private final int tileSize;
    private final int[][] grid;

    private Point entryCell;
    private Point exitCell;

    private final List<Point> exitCells = new ArrayList<>();
    private final Set<Point> keyCells = new HashSet<>();


    public Maze(int width, int height, int tileSize) {
        if (width <= 0 || height <= 0){ throw new IllegalArgumentException("Maze width/height must be > 0");}
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.grid = new int[height][width];
    }

    public int worldWidth(){
        return width * tileSize;
    }

    public int worldHeight(){
        return height * tileSize;
    }

    public int getTile(int cx, int cy){
        if(!GridUtils.inBounds(cx, cy, width, height)){
            return Tiles.Wall;
        }
        return grid[cy][cx];
    }

    public void setTile(int cx, int cy, int value) {
        if (GridUtils.inBounds(cx, cy, width, height)) {
            grid[cy][cx] = value;

            if (value == Tiles.Exit) {
                exitCells.add(new Point(cx, cy));
            }
        }
    }



    public List<Point> getExitCells() {
        return List.copyOf(exitCells);
    }


    public void addKey(Point p) {
        if (p != null) {
            keyCells.add(new Point(p));
        }
    }

    public boolean hasKeyAt(int cx, int cy) {
        return keyCells.contains(new Point(cx, cy));
    }

    public void removeKeyAt(int cx, int cy) {
        keyCells.remove(new Point(cx, cy));
    }

    public Set<Point> getKeyCells() {
        return Set.copyOf(keyCells);
    }

    public int[][] getGridReadOnly(){
        return grid;
    }
    public boolean isWallCell(int cx, int cy){
        return Tiles.isWall(getTile(cx, cy));
    }
    public boolean isWallWorld(float worldX, float worldY){
        int cx = GridUtils.worldToTile(worldX, tileSize);
        int cy = GridUtils.worldToTile(worldY, tileSize);
        return isWallCell(cx, cy);
    }

    public boolean isBlockedExit(int cx, int cy, boolean hasKey) {
        int tile = getTile(cx, cy);
        return tile == Tiles.Exit && !hasKey;
    }


    public Point getEntryCell(){
        return entryCell == null ? null : new Point(entryCell);
    }

    public Point getExitCell(){
        return exitCell == null ? null : new Point(exitCell);
    }

    public void setEntryCell(Point p){
        entryCell = (p == null ? null : new Point(p));
    }

    public void setExitCell(Point p){
        exitCell = (p == null ? null : new Point(p));
    }


    public Point findFirstTile(int tileId){
        for(int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                if(grid[y][x] == tileId){
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTileSize() {
        return tileSize;
    }

}
