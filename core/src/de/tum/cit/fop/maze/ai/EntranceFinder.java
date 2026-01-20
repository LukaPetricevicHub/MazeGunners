package de.tum.cit.fop.maze.ai;

import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.Tiles;

import java.util.ArrayList;
import java.util.List;

public class EntranceFinder {

    private static final int[][] DIRS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }
    };

    private EntranceFinder() {}

    public static List<RoomEntrance> findEntrances(Maze maze, Room room) {
        List<RoomEntrance> entrances = new ArrayList<>();

        for (int x = room.x(); x < room.x() + room.w(); x++) {
            for (int y = room.y(); y < room.y() + room.h(); y++) {

                // Only consider ROOM EDGE tiles
                if (!isRoomEdge(room, x, y)) continue;
                if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                for (int[] d : DIRS) {
                    int nx = x + d[0];
                    int ny = y + d[1];

                    if (!maze.inBounds(nx, ny)) continue;
                    Tiles t = maze.getTile(nx, ny);
                    if (t != Tiles.FLOOR && t != Tiles.DOOR) continue;


                    // ✅ HALL = floor tile that is NOT part of THIS room
                    // AND not part of ANY other room
                    if (!roomContains(room, nx, ny)
                            && !maze.isRoomTile(nx, ny)) {

                        entrances.add(new RoomEntrance(x, y, d[0], d[1]));
                    }
                }
            }
        }

        return entrances;
    }

    private static boolean isRoomEdge(Room r, int x, int y) {
        return x == r.x() || x == r.x() + r.w() - 1
                || y == r.y() || y == r.y() + r.h() - 1;
    }

    private static boolean roomContains(Room r, int x, int y) {
        return x >= r.x() && x < r.x() + r.w()
                && y >= r.y() && y < r.y() + r.h();
    }
}

