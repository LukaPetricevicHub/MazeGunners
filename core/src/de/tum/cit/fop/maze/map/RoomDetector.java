package de.tum.cit.fop.maze.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Utility class that detects rooms in a maze.
 *
 * Rooms are detected by flood-filling connected walkable tiles
 * and converting each connected region into a rectangular Room.
 *
 * This does not recreate the original BSP rooms, but is sufficient
 * for gameplay logic like enemy spawning or room checks.
 */
public final class RoomDetector {

    /**
     * Private constructor to prevent instantiation.
     */
    private RoomDetector() {}

    /**
     * Detects rooms in the given maze.
     *
     * Walkable tiles are grouped using flood fill. Each connected
     * region is converted into a bounding-box Room.
     * Very small regions (e.g. corridors) are ignored.
     *
     * @param maze the maze to analyze
     * @return list of detected rooms
     */
    public static List<Room> detectRooms(Maze maze) {
        int w = maze.getWidth();
        int h = maze.getHeight();

        boolean[][] visited = new boolean[w][h];
        List<Room> out = new ArrayList<>();

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {

                if (visited[x][y]) continue;
                if (!maze.getTile(x, y).isWalkable()) continue;

                
                Bounds b = floodBounds(maze, x, y, visited);

                
                
                int bw = b.maxX - b.minX + 1;
                int bh = b.maxY - b.minY + 1;

                if (bw < 6 || bh < 6) continue; 

                out.add(new Room(b.minX, b.minY, bw, bh));
            }
        }

        return out;
    }

    /**
     * Bounds for a flood-filled region.
     */
    private static final class Bounds {
        int minX, minY, maxX, maxY;
        /**
         * Creates bounds initialized to a single point.
         *
         * @param x Tile x-coordinate
         * @param y Tile y-coordinate
         */
        Bounds(int x, int y) { minX = maxX = x; minY = maxY = y; }
        /**
         * Expands bounds to include a coordinate.
         *
         * @param x Tile x-coordinate
         * @param y Tile y-coordinate
         */
        void include(int x, int y) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }
    }

    /**
     * Flood-fills a region and computes its bounding box.
     *
     * @param maze Maze reference
     * @param sx Start x-coordinate
     * @param sy Start y-coordinate
     * @param visited Visited grid
     * @return Bounds of the connected region
     */
    private static Bounds floodBounds(Maze maze, int sx, int sy, boolean[][] visited) {
        int w = maze.getWidth();
        int h = maze.getHeight();

        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        visited[sx][sy] = true;

        Bounds b = new Bounds(sx, sy);

        while (!q.isEmpty()) {
            int[] p = q.removeFirst();
            int x = p[0], y = p[1];
            b.include(x, y);

            
            tryPush(maze, x + 1, y, visited, w, h, q);
            tryPush(maze, x - 1, y, visited, w, h, q);
            tryPush(maze, x, y + 1, visited, w, h, q);
            tryPush(maze, x, y - 1, visited, w, h, q);
        }

        return b;
    }

    /**
     * Attempts to enqueue a tile for flood fill.
     *
     * @param maze Maze reference
     * @param x Tile x-coordinate
     * @param y Tile y-coordinate
     * @param visited Visited grid
     * @param w Maze width
     * @param h Maze height
     * @param q Queue of tiles
     */
    private static void tryPush(Maze maze, int x, int y, boolean[][] visited, int w, int h, Deque<int[]> q) {
        if (x <= 0 || y <= 0 || x >= w - 1 || y >= h - 1) return;
        if (visited[x][y]) return;
        if (!maze.getTile(x, y).isWalkable()) return;

        visited[x][y] = true;
        q.addLast(new int[]{x, y});
    }
}
