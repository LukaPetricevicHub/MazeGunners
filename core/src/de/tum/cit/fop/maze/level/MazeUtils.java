package de.tum.cit.fop.maze.level;

import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.*;


public class MazeUtils {

    public static boolean inBounds(int x, int y, int w, int h) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    public static int dist2(Point a, Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    public static boolean reachable(Maze maze, Point start, Point goal) {
        if (maze == null || start == null || goal == null) {
            return false;
        }

        int w = maze.getWidth();
        int h = maze.getHeight();

        boolean[][] visited = new boolean[h][w];
        ArrayDeque<Point> queue = new ArrayDeque<>();

        queue.add(start);
        visited[start.y][start.x] = true;

        int[][] dirs = {{ 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }};

        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();

            if (p.equals(goal)) {
                return true;
            }

            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];

                if (!inBounds(nx, ny, w, h)) continue;
                if (visited[ny][nx]) continue;
                if (!Tiles.isWalkable(maze.getTile(nx, ny))) continue;

                visited[ny][nx] = true;
                queue.addLast(new Point(nx, ny));
            }
        }

        return false;
    }

    public static Point pickFarWalkable(Maze maze, Random rng, Point from) {
        if (maze == null || rng == null) {
            return null;
        }

        List<Point> walkable = new ArrayList<>();

        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (Tiles.isWalkable(maze.getTile(x, y))) {
                    walkable.add(new Point(x, y));
                }
            }
        }

        if (walkable.isEmpty()) {
            return null;
        }

        if (from != null) {
            walkable.sort((a, b) ->
                    Integer.compare(dist2(b, from), dist2(a, from))
            );
        }

        int top = Math.min(40, walkable.size());
        return walkable.get(rng.nextInt(top));
    }

    public static Point pickRandomWalkable(
            Maze maze,
            Random rng,
            Point forbiddenA,
            Point forbiddenB
    ) {
        List<Point> candidates = new ArrayList<>();

        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {

                if (!Tiles.isWalkable(maze.getTile(x, y))) continue;

                Point p = new Point(x, y);

                if (p.equals(forbiddenA)) continue;
                if (p.equals(forbiddenB)) continue;

                candidates.add(p);
            }
        }

        return candidates.isEmpty()
                ? null
                : candidates.get(rng.nextInt(candidates.size()));
    }

}

