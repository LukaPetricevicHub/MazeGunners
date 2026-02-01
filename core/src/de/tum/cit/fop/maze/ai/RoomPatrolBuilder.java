package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.Tiles;

/**
 * Builds simple two-point patrol paths inside a room near an entrance.
 *
 * The patrol points are aligned perpendicular to the entrance direction
 * and limited to a small number of tiles to avoid long or unsafe patrols.
 */
public final class RoomPatrolBuilder {

    /** Maximum steps to extend patrol points from the anchor. */
    private static final int MAX_PATROL_STEPS = 2;

    private RoomPatrolBuilder() {}

    /**
     * Creates a two-point patrol inside a room based on an entrance direction.
     *
     * @param maze Maze reference
     * @param room Room containing the patrol
     * @param e Entrance descriptor
     * @return two patrol points or {@code null} if no valid patrol is possible
     */
    public static Array<Vector2> buildPatrol(Maze maze, Room room, RoomEntrance e) {
        float ts = maze.getTileSize();
        Array<Vector2> pts = new Array<>();

        int ax = e.x() - e.dirX();
        int ay = e.y() - e.dirY();


        int tx, ty;
        if (e.dirX() != 0) {

            tx = 0;
            ty = 1;
        } else{
            tx = 1;
            ty = 0;
        }

        float axw = ax * ts;
        float ayw = ay * ts;

        if (tx == 0) {
            axw += ts * 0.5f;
        }
        else {
            ayw += ts * 0.5f;
        }

        Vector2 anchor = new Vector2(axw, ayw);

        Vector2 p1 = findFurthest(maze, room, ax, ay, tx, ty, ts);
        Vector2 p2 = findFurthest(maze, room, ax, ay, -tx, -ty, ts);


        if (p1 == null && p2 == null) return null;

        if (p1 == null) p1 = anchor;
        if (p2 == null) p2 = anchor;


        pts.add(p1);
        pts.add(p2);

        return pts;
    }

    /**
     * Checks whether a tile coordinate is inside a room.
     *
     * @param r Room reference
     * @param x Tile x-coordinate
     * @param y Tile y-coordinate
     * @return True if inside the room
     */
    private static boolean roomContains(Room r, int x, int y) {
        return x >= r.x() && x < r.x() + r.w()
                && y >= r.y() && y < r.y() + r.h();
    }

    /**
     * Walks from a start tile in one direction to find the furthest valid patrol point.
     *
     * @param maze Maze reference
     * @param room Room reference
     * @param ax Anchor tile x-coordinate
     * @param ay Anchor tile y-coordinate
     * @param dx Step direction x
     * @param dy Step direction y
     * @param ts Tile size in world units
     * @return Furthest patrol point, or null if none
     */
    private static Vector2 findFurthest(Maze maze, Room room, int ax, int ay, int dx, int dy, float ts) {
        int x = ax;
        int y = ay;
        int steps = 0;

        while (steps < MAX_PATROL_STEPS) {
            int nx = x + dx;
            int ny = y + dy;

            if (!maze.inBounds(nx, ny)) break;
            if (!maze.isRoomCell(nx, ny)) break;
            if (maze.getTile(nx, ny) != Tiles.FLOOR) break;

            x = nx;
            y = ny;
            steps++;
        }

        if (x == ax && y == ay) return null;

        float px = x * ts;
        float py = y * ts;


        if (dx == 0) {
            px += ts * 0.5f;
        }

        else {
            py += ts * 0.5f;
        }

        return new Vector2(px, py);

    }
}

