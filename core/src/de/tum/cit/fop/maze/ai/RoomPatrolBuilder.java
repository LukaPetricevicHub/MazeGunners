package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.Tiles;

public final class RoomPatrolBuilder {

    private static final int MAX_PATROL_STEPS = 2; // ← THIS fixes everything

    private RoomPatrolBuilder() {}

    public static Array<Vector2> buildPatrol(
            Maze maze,
            Room room,
            RoomEntrance e
    ) {
        float ts = maze.getTileSize();
        Array<Vector2> pts = new Array<>();

        int ax = e.x();
        int ay = e.y();

        int tx, ty;
        if (e.dirX() != 0) {
            // hall on LEFT or RIGHT → patrol UP/DOWN
            tx = 0;
            ty = 1;
        } else {
            // hall on TOP or BOTTOM → patrol LEFT/RIGHT
            tx = 1;
            ty = 0;
        }

        Vector2 anchor = new Vector2(
                ax * ts,
                ay * ts
        );



        Vector2 p1 = findFurthest(maze, room, ax, ay, tx, ty, ts);
        Vector2 p2 = findFurthest(maze, room, ax, ay, -tx, -ty, ts);


        if (p1 == null && p2 == null) return null;

        if (p1 == null) p1 = anchor;
        if (p2 == null) p2 = anchor;



        pts.add(p1);
        pts.add(p2);

        return pts;
    }

    private static boolean roomContains(Room r, int x, int y) {
        return x >= r.x() && x < r.x() + r.w()
                && y >= r.y() && y < r.y() + r.h();
    }

    private static Vector2 findFurthest(
            Maze maze,
            Room room,
            int ax,
            int ay,
            int dx,
            int dy,
            float ts
    ) {
        int x = ax;
        int y = ay;
        int steps = 0;

        while (steps < MAX_PATROL_STEPS) {
            int nx = x + dx;
            int ny = y + dy;

            if (!maze.inBounds(nx, ny)) break;
            if (!roomContains(room, nx, ny)) break;
            if (maze.getTile(nx, ny) != Tiles.FLOOR) break;

            x = nx;
            y = ny;
            steps++;
        }

        if (x == ax && y == ay) return null;

        return new Vector2(
                x * ts,
                y * ts
        );


    }
}

