package de.tum.cit.fop.maze.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Detects "rooms" from a loaded maze by finding connected floor regions (flood fill)
 * and turning each region into a bounding-box Room.
 *
 * This is not the original BSP room structure, but it's good enough for:
 * - enemy spawning (pick a random interior point)
 * - "room list not empty" logic
 */
public final class RoomDetector {

    private RoomDetector() {}

    public static List<Room> detectRooms(Maze maze) {
        int w = maze.getWidth();
        int h = maze.getHeight();

        boolean[][] visited = new boolean[w][h];
        List<Room> out = new ArrayList<>();

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {

                if (visited[x][y]) continue;
                if (!maze.getTile(x, y).isWalkable()) continue;

                // Flood fill this walkable region and compute its bounds
                Bounds b = floodBounds(maze, x, y, visited);

                // Filter tiny blobs (thin corridors, noise)
                // Tune these if needed.
                int bw = b.maxX - b.minX + 1;
                int bh = b.maxY - b.minY + 1;

                if (bw < 6 || bh < 6) continue; // ignore small corridor chunks

                out.add(new Room(b.minX, b.minY, bw, bh));
            }
        }

        return out;
    }

    private static final class Bounds {
        int minX, minY, maxX, maxY;
        Bounds(int x, int y) { minX = maxX = x; minY = maxY = y; }
        void include(int x, int y) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }
    }

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

            // 4-neighborhood
            tryPush(maze, x + 1, y, visited, w, h, q);
            tryPush(maze, x - 1, y, visited, w, h, q);
            tryPush(maze, x, y + 1, visited, w, h, q);
            tryPush(maze, x, y - 1, visited, w, h, q);
        }

        return b;
    }

    private static void tryPush(Maze maze, int x, int y, boolean[][] visited, int w, int h, Deque<int[]> q) {
        if (x <= 0 || y <= 0 || x >= w - 1 || y >= h - 1) return;
        if (visited[x][y]) return;
        if (!maze.getTile(x, y).isWalkable()) return;

        visited[x][y] = true;
        q.addLast(new int[]{x, y});
    }
}
