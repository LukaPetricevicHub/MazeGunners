package de.tum.cit.fop.maze.map;

import java.awt.Point;
import java.util.Random;

/**
 * Represents a rectangular room inside the maze.
 *
 * Stores position, size, and the room's type
 * (e.g. normal, spawn, exit, boss).
 */
public class Room {
    /** Room x-coordinate (top-left) in tiles. */
    private final int x, y, w, h;
    /** Room type classification. */
    private RoomType type = RoomType.NORMAL;

    /**
     * Creates a new room.
     *
     * @param x x-coordinate of the top-left corner
     * @param y y-coordinate of the top-left corner
     * @param w width of the room in tiles
     * @param h height of the room in tiles
     */
    public Room(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** @return Room x-coordinate in tiles. */
    public int x() { return x; }
    /** @return Room y-coordinate in tiles. */
    public int y() { return y; }
    /** @return Room width in tiles. */
    public int w() { return w; }
    /** @return Room height in tiles. */
    public int h() { return h; }

    /**
     * Returns the type of this room.
     *
     * @return room type
     */
    public RoomType getType() { return type; }

    /**
     * Sets the type of this room.
     *
     * @param type new room type
     */
    public void setType(RoomType type) { this.type = type; }

    /** @return Center x-coordinate in tiles. */
    public int centerX() { return x + w / 2; }
    /** @return Center y-coordinate in tiles. */
    public int centerY() { return y + h / 2; }

    /**
     * Returns the center tile of the room.
     *
     * @return Center tile point
     */
    public Point center() {
        return new Point(centerX(), centerY());
    }

    /**
     * Returns a random tile inside the room.
     *
     * The returned tile does not touch the room walls.
     * If the room is too small, the center tile is returned.
     *
     * @param rng random number generator
     * @return random interior tile position
     */
    public Point randomInterior(Random rng) {
        int ix0 = x + 1;
        int iy0 = y + 1;
        int ix1 = x + w - 2;
        int iy1 = y + h - 2;

        if (ix1 < ix0 || iy1 < iy0) {
            return center();
        }

        int rx = ix0 + rng.nextInt(ix1 - ix0 + 1);
        int ry = iy0 + rng.nextInt(iy1 - iy0 + 1);
        return new Point(rx, ry);
    }
}
