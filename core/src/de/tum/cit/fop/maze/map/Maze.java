package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tile-based maze structure.
 *
 * Manages tiles, rooms, entry and exit cells, collision queries,
 * and spatial information used by players, enemies, and pathfinding.
 */
public class Maze {

    /** Maze width in tiles. */
    private final int width;
    /** Maze height in tiles. */
    private final int height;
    /** Tile size in world units. */
    private final int tileSize;
    /** Tile grid for the maze. */
    private final Tiles[][] grid;

    /** Entry spawn cell. */
    private Point entryCell;
    /** Primary exit cell. */
    private Point exitCell;
    /** All exit cells. */
    private final List<Point> exitCells = new ArrayList<>();
    /** Rooms present in the maze. */
    private List<Room> rooms = List.of();
    /** Room membership mask. */
    private final boolean[][] roomMask;

    /** Enemy occupancy grid. */
    private boolean[][] enemyOccupancy;
    /** Cached enemy occupancy width. */
    private int enemyOccW = -1;
    /** Cached enemy occupancy height. */
    private int enemyOccH = -1;


    /**
     * Creates a new maze initialized entirely with wall tiles.
     *
     * @param width number of tiles horizontally
     * @param height number of tiles vertically
     * @param tileSize size of a single tile in world units
     */
    public Maze(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.roomMask = new boolean[width][height];


        grid = new Tiles[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = Tiles.WALL;
    }

    /**
     * Basic maze properties.
     *
     * @return maze dimensions and tile size
     */
    public int getWidth() { return width; }
    /** @return Maze height in tiles. */
    public int getHeight() { return height; }
    /** @return Tile size in world units. */
    public int getTileSize() { return tileSize; }

    /**
     * Checks whether the given tile coordinates lie within the maze bounds.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return true if the coordinates are inside the maze
     */
    public boolean inBounds(int x, int y) {
        return GridUtils.inBounds(x, y, width, height);
    }

    /**
     * Returns the tile at the given coordinates.
     *
     * Out-of-bounds accesses are treated as walls.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return tile at the given position, or WALL if out of bounds
     */
    public Tiles getTile(int x, int y) {
        if (!inBounds(x, y)) return Tiles.WALL;
        return grid[x][y];
    }

    /**
     * Sets a tile at the given coordinates and updates special maze state.
     *
     * Handles entry and exit bookkeeping when SPAWN or EXIT tiles are placed
     * or removed.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param tile tile type to place
     */
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

    /**
     * Entry and exit cell accessors.
     *
     * Returned points are defensive copies and may be null if not set.
     */
    /** @return Entry cell or null if unset. */
    public Point getEntryCell() {
        return entryCell == null ? null : new Point(entryCell);
    }
    /** @return Exit cell or null if unset. */
    public Point getExitCell() {
        return exitCell == null ? null : new Point(exitCell);
    }
    /** @return List of all exit cells. */
    public List<Point> getExitCells() {
        return List.copyOf(exitCells);
    }

    /**
     * Checks whether a tile cell is a wall.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return true if the tile is a wall
     */
    public boolean isWallCell(int x, int y) {
        return getTile(x, y).isWall();
    }

    /**
     * Checks whether a world-space position lies inside a wall tile.
     *
     * @param wx world X coordinate
     * @param wy world Y coordinate
     * @return true if the position is blocked by a wall
     */
    public boolean isWallWorld(float wx, float wy) {
        int cx = GridUtils.worldToTile(wx, tileSize);
        int cy = GridUtils.worldToTile(wy, tileSize);
        return isWallCell(cx, cy);
    }

    /**
     * Checks whether a rectangle intersects any wall tiles.
     *
     * @param r rectangle in world coordinates
     * @return true if the rectangle overlaps a wall
     */
    public boolean isWallRect(Rectangle r) {
        float inset = 1f;
        return isWallWorld(r.x + inset, r.y + inset)
                || isWallWorld(r.x + r.width - inset, r.y + inset)
                || isWallWorld(r.x + inset, r.y + r.height - inset)
                || isWallWorld(r.x + r.width - inset, r.y + r.height - inset);
    }

    /**
     * Determines whether an exit tile blocks movement.
     *
     * Exits block movement until the player has obtained the key.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param hasKey whether the player currently has the key
     * @param hasStoryItem whether the player has the story item
     * @return true if the exit blocks passage
     */
    public boolean isBlockedExit(int x, int y, boolean hasKey, boolean hasStoryItem) {
        return getTile(x, y) == Tiles.EXIT && !(hasKey && hasStoryItem);
    }

    /**
     * Explicit setters for entry, exit, and room data.
     *
     * Values are copied defensively.
     */
    /**
     * Sets the entry cell.
     *
     * @param p Entry tile coordinate
     */
    public void setEntryCell(Point p) {
        entryCell = (p == null) ? null : new Point(p);
    }

    /**
     * Sets the primary exit cell.
     *
     * @param p Exit tile coordinate
     */
    public void setExitCell(Point p) {
        exitCell = (p == null) ? null : new Point(p);
        if (p == null) {
            exitCells.clear();
        }
    }

    /**
     * Sets the room list for this maze.
     *
     * @param rooms Room list
     */
    public void setRooms(List<Room> rooms) {
        this.rooms = rooms != null ? rooms : List.of();
    }

    /**
     * Retrieves the list of rooms.
     *
     * @return Room list
     */
    public List<Room> getRooms() {
        return List.copyOf(rooms);
    }

    /**
     * Checks whether a tile lies inside any defined room.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return true if the tile is part of a room
     */
    public boolean isRoomTile(int x, int y) {
        for (Room r : rooms) {
            if (x >= r.x() && x < r.x() + r.w()
                    && y >= r.y() && y < r.y() + r.h()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the internal room mask for all tiles.
     */
    public void clearRoomMask() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                roomMask[x][y] = false;
            }
        }
    }

    /**
     * Marks a tile as belonging to a room in the room mask.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param isRoom whether the tile is part of a room
     */
    public void setRoomCell(int x, int y, boolean isRoom) {
        if (!inBounds(x, y)) return;
        roomMask[x][y] = isRoom;
    }

    /**
     * Checks whether a tile is marked as a room cell.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return true if the tile is part of a room
     */
    public boolean isRoomCell(int x, int y) {
        if (!inBounds(x, y)) return false;
        return roomMask[x][y];
    }

    /**
     * Updates the enemy occupancy grid based on current enemy positions.
     *
     * Marks all tiles currently occupied by enemy collision boxes,
     * allowing fast spatial queries.
     *
     * @param enemies list of active enemies
     */
    public void updateEnemyOccupancy(List<Enemy> enemies) {
        if (enemyOccupancy == null || enemyOccW != width || enemyOccH != height) {
            enemyOccupancy = new boolean[width][height];
            enemyOccW = width;
            enemyOccH = height;
        } else {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    enemyOccupancy[x][y] = false;
                }
            }
        }

        if (enemies == null) return;

        for (Enemy e : enemies) {

            if (e == null) continue;

            if (e.ignoresEnemyCollision()) continue;

            // World => tile

            Rectangle r = e.getCollisionBox();

            float inset = 4f;

            int minX = (int) ((r.x + inset) / tileSize);
            int minY = (int) ((r.y + inset) / tileSize);
            int maxX = (int) ((r.x + r.width  - inset) / tileSize);
            int maxY = (int) ((r.y + r.height - inset) / tileSize);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (inBounds(x, y)) {
                        enemyOccupancy[x][y] = true;
                    }
                }
            }

        }
    }

    /**
     * Checks whether a tile is currently occupied by an enemy.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return true if an enemy occupies the tile
     */
    public boolean isEnemyOccupied(int x, int y) {
        if (enemyOccupancy == null) return false;
        if (!inBounds(x, y)) return false;
        return enemyOccupancy[x][y];
    }

    /**
     * Checks whether a tile is a hallway tile.
     *
     * Hall tiles are floor tiles that are not part of any room.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return true if the tile is a hallway tile
     */
    public boolean isHallTile(int x, int y) {
        if (!inBounds(x, y)) return false;
        return getTile(x, y) == Tiles.FLOOR && !isRoomCell(x, y);
    }

    /**
     * Determines the orientation of a hallway tile.
     *
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @return hallway orientation, or null if undefined
     */
    public HallOrientation getHallOrientation(int x, int y) {
        if (!isHallTile(x, y)) return null;

        boolean left  = isHallTile(x - 1, y);
        boolean right = isHallTile(x + 1, y);
        boolean up    = isHallTile(x, y + 1);
        boolean down  = isHallTile(x, y - 1);

        if (left || right) return HallOrientation.LEFT_RIGHT;
        if (up || down)    return HallOrientation.UP_DOWN;

        return null;
    }


}
