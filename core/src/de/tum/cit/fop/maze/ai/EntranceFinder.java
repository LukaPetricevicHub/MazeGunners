package de.tum.cit.fop.maze.ai;

import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.Tiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that finds entrances of a room in the maze.
 *
 * An entrance is a floor tile on the edge of a room that connects
 * to a hallway tile outside the room.
 */
public class EntranceFinder {

    /** Cardinal direction offsets for adjacency checks. */
    private static final int[][] DIRS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }
    };

    private EntranceFinder() {}

    /**
     * Finds all valid entrances of a given room.
     *
     * An entrance is detected when:
     * - the tile is on the edge of the room
     * - the tile itself is a floor tile
     * - one of its neighbors is a hallway tile (floor or door)
     *   that does not belong to any room
     *
     * @param maze the maze to scan
     * @param room the room whose entrances should be found
     * @return a list of room entrances
     */
    public static List<RoomEntrance> findEntrances(Maze maze, Room room) {
        List<RoomEntrance> entrances = new ArrayList<>();

        for (int x = room.x(); x < room.x() + room.w(); x++) {
            for (int y = room.y(); y < room.y() + room.h(); y++) {

                
                if (!isRoomEdge(room, x, y)) continue;
                if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                for (int[] d : DIRS) {
                    int nx = x + d[0];
                    int ny = y + d[1];

                    if (!maze.inBounds(nx, ny)) continue;
                    Tiles t = maze.getTile(nx, ny);
                    if (t != Tiles.FLOOR && t != Tiles.DOOR) continue;



                    if (!roomContains(room, nx, ny)
                            && !maze.isRoomTile(nx, ny)) {

                        entrances.add(new RoomEntrance(x, y, d[0], d[1]));
                    }
                }
            }
        }

        return entrances;
    }

    /**
     * Checks whether a tile is on the edge of the room.
     *
     * @param r Room reference
     * @param x Tile x-coordinate
     * @param y Tile y-coordinate
     * @return True if on the room boundary
     */
    private static boolean isRoomEdge(Room r, int x, int y) {
        return x == r.x() || x == r.x() + r.w() - 1
                || y == r.y() || y == r.y() + r.h() - 1;
    }

    /**
     * Checks whether a coordinate lies within a room.
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
}
