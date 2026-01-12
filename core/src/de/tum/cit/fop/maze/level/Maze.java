package de.tum.cit.fop.maze.level;

import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Maze {

    private final int width;     // tiles
    private final int height;    // tiles
    private final int tileSize;  // pixels

    // IMPORTANT: grid[x][y]
    private final Tiles[][] grid;

    private Point entryCell;
    private Point exitCell;
    private final List<Point> exitCells = new ArrayList<>();

    public Maze(int width, int height, int tileSize) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid maze size");
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;

        grid = new Tiles[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = Tiles.FLOOR;
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTileSize() { return tileSize; }

    public int worldWidth() { return width * tileSize; }
    public int worldHeight() { return height * tileSize; }

    public boolean inBounds(int x, int y) {
        return GridUtils.inBounds(x, y, width, height);
    }

    public Tiles getTile(int x, int y) {
        if (!inBounds(x, y)) return Tiles.WALL;
        return grid[x][y];
    }

    public void setTile(int x, int y, Tiles tile) {
        if (!inBounds(x, y)) return;

        grid[x][y] = tile;

        // Track entry/exit so MazeUtils.reachable() works
        if (tile == Tiles.ENTRY) {
            entryCell = new Point(x, y);
        } else if (tile == Tiles.EXIT) {
            exitCell = new Point(x, y);
            exitCells.add(new Point(x, y));
        }
    }

    public Point getEntryCell() {
        return entryCell == null ? null : new Point(entryCell);
    }

    public Point getExitCell() {
        return exitCell == null ? null : new Point(exitCell);
    }

    public List<Point> getExitCells() {
        return List.copyOf(exitCells);
    }

    public boolean isWallCell(int cx, int cy) {
        return getTile(cx, cy).isWall();
    }

    public boolean isWallWorld(float worldX, float worldY) {
        int cx = GridUtils.worldToTile(worldX, tileSize);
        int cy = GridUtils.worldToTile(worldY, tileSize);
        return isWallCell(cx, cy);
    }

    // If you still use exit-locking in Player:
    public boolean isBlockedExit(int cx, int cy, boolean hasKey) {
        return getTile(cx, cy) == Tiles.EXIT && !hasKey;
    }
}
