package de.tum.cit.fop.maze.map;

import java.awt.Point;
import java.util.Random;

public class Room {
    private final int x, y, w, h;
    private RoomType type = RoomType.NORMAL;

    public Room(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int w() { return w; }
    public int h() { return h; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public int centerX() { return x + w / 2; }
    public int centerY() { return y + h / 2; }

    public Point center() {
        return new Point(centerX(), centerY());
    }

    /** Random tile inside the room, not touching walls */
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
