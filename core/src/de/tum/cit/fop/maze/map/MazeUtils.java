package de.tum.cit.fop.maze.map;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility methods for maze logic.
 *
 * Contains helper functions for distance checks,
 * reachability tests, and random tile selection.
 */
public class MazeUtils {

    /**
     * Computes the squared distance between two points.
     *
     * Useful for comparisons without using square roots.
     *
     * @param a first point
     * @param b second point
     * @return squared distance between the points
     */
    public static int dist2(Point a, Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    /**
     * Checks whether a target tile can be reached from a start tile.
     *
     * Uses a simple breadth-first search (4-directional).
     *
     * @param maze the maze to search in
     * @param start starting position
     * @param goal target position
     * @return true if the goal is reachable, false otherwise
     */
    public static boolean reachable(Maze maze, Point start, Point goal) {
        if (start == null || goal == null) return false;
        if (!maze.inBounds(start.x, start.y)) return false;
        if (!maze.inBounds(goal.x, goal.y)) return false;
        if (!maze.getTile(start.x, start.y).isWalkable()) return false;
        if (!maze.getTile(goal.x, goal.y).isWalkable()) return false;

        boolean[][] visited = new boolean[maze.getWidth()][maze.getHeight()];
        ArrayDeque<Point> q = new ArrayDeque<>();

        q.add(start);
        visited[start.x][start.y] = true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!q.isEmpty()) {
            Point p = q.poll();
            if (p.equals(goal)) return true;

            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];

                if (!maze.inBounds(nx, ny)) continue;
                if (visited[nx][ny]) continue;
                if (!maze.getTile(nx, ny).isWalkable()) continue;

                visited[nx][ny] = true;
                q.add(new Point(nx, ny));
            }
        }
        return false;
    }

    /**
     * Picks a random walkable tile from the maze.
     *
     * Two positions can be excluded from selection.
     *
     * @param maze the maze to search
     * @param rng random number generator
     * @param forbiddenA first forbidden position (may be null)
     * @param forbiddenB second forbidden position (may be null)
     * @return a random walkable point or null if none exist
     */
    public static Point pickRandomWalkable(Maze maze, Random rng, Point forbiddenA, Point forbiddenB) {
        List<Point> candidates = new ArrayList<>();

        for (int x = 1; x < maze.getWidth() - 1; x++) {
            for (int y = 1; y < maze.getHeight() - 1; y++) {
                if (!maze.getTile(x, y).isWalkable()) continue;

                Point p = new Point(x, y);
                if (p.equals(forbiddenA)) continue;
                if (p.equals(forbiddenB)) continue;

                candidates.add(p);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(rng.nextInt(candidates.size()));
    }
}
