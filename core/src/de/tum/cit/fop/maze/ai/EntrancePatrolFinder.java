package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public final class EntrancePatrolFinder {

    private EntrancePatrolFinder() {}


    public static List<EntrancePatrol> findAllEntrancePatrols(Maze maze) {
        List<EntrancePatrol> patrols = new ArrayList<>();
        boolean[][] usedDoor = new boolean[maze.getWidth()][maze.getHeight()];

        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {

                if (maze.getTile(x, y) != Tiles.DOOR)
                    continue;

                if (usedDoor[x][y])
                    continue;

                List<Point> doorway = collectExactTwoTileDoorway(maze, x, y, usedDoor);
                if (doorway == null)
                    continue;

                EntrancePatrol patrol = buildExactTwoPointPatrol(maze, doorway);
                if (patrol != null) {
                    patrols.add(patrol);
                }
            }
        }

        return patrols;
    }


    private static List<Point> collectExactTwoTileDoorway(
            Maze maze,
            int sx,
            int sy,
            boolean[][] used
    ) {
        List<Point> tiles = new ArrayList<>(2);
        tiles.add(new Point(sx, sy));

        // Look for the second tile (must be directly adjacent)
        Point second = null;

        if (maze.inBounds(sx + 1, sy) && maze.getTile(sx + 1, sy) == Tiles.DOOR)
            second = new Point(sx + 1, sy);
        else if (maze.inBounds(sx - 1, sy) && maze.getTile(sx - 1, sy) == Tiles.DOOR)
            second = new Point(sx - 1, sy);
        else if (maze.inBounds(sx, sy + 1) && maze.getTile(sx, sy + 1) == Tiles.DOOR)
            second = new Point(sx, sy + 1);
        else if (maze.inBounds(sx, sy - 1) && maze.getTile(sx, sy - 1) == Tiles.DOOR)
            second = new Point(sx, sy - 1);

        if (second == null)
            return null;

        tiles.add(second);

        used[sx][sy] = true;
        used[second.x][second.y] = true;

        return tiles;
    }


    private static EntrancePatrol buildExactTwoPointPatrol(
            Maze maze,
            List<Point> doorTiles
    ) {
        Point d0 = doorTiles.get(0);
        Point d1 = doorTiles.get(1);

        boolean horizontalDoor = (d0.y == d1.y);
        boolean verticalDoor   = (d0.x == d1.x);

        if (!horizontalDoor && !verticalDoor)
            return null;

        // Determine which side is the ROOM
        int roomDx = 0;
        int roomDy = 0;

        if (horizontalDoor) {
            // hallway LEFT/RIGHT → room is UP or DOWN
            if (isRoomTile(maze, d0.x, d0.y - 1)) roomDy = -1;
            else if (isRoomTile(maze, d0.x, d0.y + 1)) roomDy = 1;
            else return null;
        } else {
            // hallway TOP/BOTTOM → room is LEFT or RIGHT
            if (isRoomTile(maze, d0.x - 1, d0.y)) roomDx = -1;
            else if (isRoomTile(maze, d0.x + 1, d0.y)) roomDx = 1;
            else return null;
        }

        Array<Vector2> points = new Array<>(2);
        float ts = maze.getTileSize();

        if (horizontalDoor) {
            int rx = d0.x;
            int ry = d0.y + roomDy;

            addPointIfValid(maze, points, rx - 1, ry, ts);
            addPointIfValid(maze, points, rx + 1, ry, ts);
        } else {
            int rx = d0.x + roomDx;
            int ry = d0.y;

            addPointIfValid(maze, points, rx, ry - 1, ts);
            addPointIfValid(maze, points, rx, ry + 1, ts);
        }

        return (points.size == 2) ? new EntrancePatrol(points) : null;
    }



    private static boolean isRoomTile(Maze maze, int x, int y) {
        return maze.inBounds(x, y) && maze.getTile(x, y) == Tiles.FLOOR;
    }

    private static void addPointIfValid(
            Maze maze,
            Array<Vector2> points,
            int x,
            int y,
            float ts
    ) {
        if (!maze.inBounds(x, y)) return;
        if (maze.getTile(x, y) != Tiles.FLOOR) return;

        points.add(new Vector2(
                (x + 0.5f) * ts,
                (y + 0.5f) * ts
        ));
    }
}
