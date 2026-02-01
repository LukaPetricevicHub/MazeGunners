package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that finds patrol routes for enemies guarding door entrances.
 *
 * An entrance patrol consists of exactly two world-space points located
 * just inside a room next to a two-tile-wide door. These patrols are used
 * to make enemies guard doorways realistically.
 */
public final class EntrancePatrolFinder {

    private EntrancePatrolFinder() {}

    /**
     * Scans the entire maze and finds all valid entrance patrols.
     *
     * Only doors consisting of exactly two adjacent DOOR tiles are considered.
     * Each door produces at most one patrol.
     *
     * @param maze the maze to scan
     * @return list of entrance patrols
     */
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

    /**
     * Collects exactly two adjacent DOOR tiles starting from a given position.
     *
     * The doorway is only valid if it consists of exactly two directly adjacent
     * tiles (horizontal or vertical). Each tile is marked as used to prevent
     * duplicate patrols.
     *
     * @param maze the maze
     * @param sx starting x tile
     * @param sy starting y tile
     * @param used bookkeeping array for used door tiles
     * @return list of two door tiles, or null if invalid
     */
    private static List<Point> collectExactTwoTileDoorway(Maze maze, int sx, int sy, boolean[][] used) {
        List<Point> tiles = new ArrayList<>(2);
        tiles.add(new Point(sx, sy));

        
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

    /**
     * Builds a two-point patrol just inside the room next to a doorway.
     *
     * The method determines which side of the door belongs to the room and
     * places two patrol points one tile inside that room, parallel to the door.
     *
     * @param maze the maze
     * @param doorTiles exactly two adjacent door tiles
     * @return entrance patrol or null if no valid patrol can be built
     */
    private static EntrancePatrol buildExactTwoPointPatrol(Maze maze, List<Point> doorTiles) {
        Point d0 = doorTiles.get(0);
        Point d1 = doorTiles.get(1);

        boolean horizontalDoor = (d0.y == d1.y);
        boolean verticalDoor   = (d0.x == d1.x);

        if (!horizontalDoor && !verticalDoor)
            return null;


        int roomDx = 0;
        int roomDy = 0;

        if (horizontalDoor) {
            if (isRoomTile(maze, d0.x, d0.y - 1)) {
                roomDy = -1;
            } else if(isRoomTile(maze, d0.x, d0.y + 1)) {
                roomDy = 1;
            } else
                return null;
        } else {
            if (isRoomTile(maze, d0.x - 1, d0.y)) roomDx = -1;
            else if(isRoomTile(maze, d0.x + 1, d0.y)) roomDx = 1;
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

    /**
     * Checks whether a tile is a valid room interior tile.
     *
     * @param maze Maze reference
     * @param x Tile x-coordinate
     * @param y Tile y-coordinate
     * @return True if the tile is a room interior floor
     */
    private static boolean isRoomTile(Maze maze, int x, int y) {
        return maze.inBounds(x, y) && maze.getTile(x, y) == Tiles.FLOOR;
    }

    /**
     * Adds a world-space patrol point if the tile is a valid floor tile.
     *
     * @param maze Maze reference
     * @param points Output list of points
     * @param x Tile x-coordinate
     * @param y Tile y-coordinate
     * @param ts Tile size in world units
     */
    private static void addPointIfValid(Maze maze, Array<Vector2> points, int x, int y, float ts) {
        if (!maze.inBounds(x, y)) return;
        if (maze.getTile(x, y) != Tiles.FLOOR) return;

        points.add(new Vector2(
                (x + 0.5f) * ts,
                (y + 0.5f) * ts
        ));
    }
}
