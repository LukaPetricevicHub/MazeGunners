package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Maze {

    private final int width;
    private final int height;
    private final int tileSize;
    private final Tiles[][] grid;

    private Point entryCell;
    private Point exitCell;
    private final List<Point> exitCells = new ArrayList<>();
    private List<Room> rooms = List.of();

    public Maze(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;

        grid = new Tiles[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = Tiles.WALL;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTileSize() { return tileSize; }

    public boolean inBounds(int x, int y) {
        return GridUtils.inBounds(x, y, width, height);
    }

    public Tiles getTile(int x, int y) {
        if (!inBounds(x, y)) return Tiles.WALL;
        return grid[x][y];
    }

    public void setTile(int x, int y, Tiles tile) {
        if (!inBounds(x, y) || tile == null) return;

        Tiles old = grid[x][y];

        // Remove old special state
        if (old == Tiles.SPAWN && entryCell != null &&
                entryCell.x == x && entryCell.y == y) {
            entryCell = null;
        }

        if (old == Tiles.EXIT) {
            exitCells.removeIf(p -> p.x == x && p.y == y);
            if (exitCell != null && exitCell.x == x && exitCell.y == y) {
                exitCell = exitCells.isEmpty() ? null : new Point(exitCells.get(0));
            }
        }

        grid[x][y] = tile;

        // Add new special state
        if (tile == Tiles.SPAWN) {
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

    public boolean isWallCell(int x, int y) {
        return getTile(x, y).isWall();
    }

    public boolean isWallWorld(float wx, float wy) {
        int cx = GridUtils.worldToTile(wx, tileSize);
        int cy = GridUtils.worldToTile(wy, tileSize);
        return isWallCell(cx, cy);
    }

    public boolean isWallRect(Rectangle r) {
        float inset = 1f;
        return isWallWorld(r.x + inset, r.y + inset)
                || isWallWorld(r.x + r.width - inset, r.y + inset)
                || isWallWorld(r.x + inset, r.y + r.height - inset)
                || isWallWorld(r.x + r.width - inset, r.y + r.height - inset);
    }

    public boolean isBlockedExit(int x, int y, boolean hasKey) {
        return getTile(x, y) == Tiles.EXIT && !hasKey;
    }

    // Explicit setters (useful when generator wants to clear tracked cells)
    public void setEntryCell(Point p) {
        entryCell = (p == null) ? null : new Point(p);
    }

    public void setExitCell(Point p) {
        exitCell = (p == null) ? null : new Point(p);
        if (p == null) {
            exitCells.clear();
        }
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms != null ? rooms : List.of();
    }

    public boolean isRoomTile(int x, int y) {
        for (Room r : rooms) {
            if (x >= r.x() && x < r.x() + r.w()
                    && y >= r.y() && y < r.y() + r.h()) {
                return true;
            }
        }
        return false;
    }
}
